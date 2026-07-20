// ============================================================
//  shopping.model.js — Liste de courses
// ============================================================

const ShoppingModel = (() => {

  function uid() { return crypto.randomUUID(); }

  async function getAll(userId) {
    return DB.getUserShoppingList(userId);
  }

  async function add(userId, data) {
    const entry = {
      id: uid(),
      userId,
      name: data.name?.trim(),
      quantity: data.quantity || '',
      unit: data.unit || '',
      source: data.source || 'manual', // 'auto', 'manual', 'recipe'
      itemId: data.itemId || null,      // linked fridge item if auto
      shopId: data.shopId || null,
      checked: false,
      targetStorageId: data.targetStorageId || null,
      expiryDate: data.expiryDate || null,
      notes: data.notes || '',
      addedAt: new Date().toISOString(),
    };
    await DB.put('shopping_list', entry);
    return entry;
  }

  async function toggle(id) {
    const item = await DB.getOne('shopping_list', id);
    if (!item) return;
    item.checked = !item.checked;
    await DB.put('shopping_list', item);
    return item;
  }
 
  async function changeQuantity(id, delta) {
    const item = await DB.getOne('shopping_list', id);
    if (!item) return;
    const current = parseFloat(item.quantity) || 0;
    const next = Math.max(0, current + delta);
    if (next === current) return item;
    item.quantity = next;
    await DB.put('shopping_list', item);
    return item;
  }
 
  async function setTargetStorage(id, storageId) {
    const item = await DB.getOne('shopping_list', id);
    if (!item) return;
    item.targetStorageId = storageId;
    await DB.put('shopping_list', item);
    return item;
  }

  async function remove(id) {
    await DB.del('shopping_list', id);
  }

  async function clearChecked(userId) {
    const all = await getAll(userId);
    for (const item of all.filter(i => i.checked)) {
      await DB.del('shopping_list', item.id);
    }
  }

  async function clearAll(userId) {
    const all = await getAll(userId);
    for (const item of all) await DB.del('shopping_list', item.id);
  }

  // Sync auto-restock: add items below threshold not already in list
  async function syncAutoRestock(userId) {
    const lowStock = await ItemModel.getLowStock(userId);
    const existing = await getAll(userId);
    const existingAuto = existing.filter(i => i.source === 'auto');

    for (const item of lowStock) {
      const needed = Math.max(1, parseFloat(item.restockBuyQuantity) || 1);
      const existingEntry = existingAuto.find(s => s.itemId === item.id);
      if (!existingEntry) {
        await add(userId, {
          name: item.name,
          quantity: needed,
          unit: item.unit,
          source: 'auto',
          itemId: item.id,
          shopId: item.shopId || null,
        });
      } else {
        const updatedEntry = {
          ...existingEntry,
          name: item.name,
          quantity: needed,
          unit: item.unit,
          shopId: item.shopId || null,
        };
        await DB.put('shopping_list', updatedEntry);
      }
    }

    // Remove auto items whose fridge item is no longer low stock
    const lowIds = new Set(lowStock.map(i => i.id));
    for (const s of existingAuto) {
      if (s.itemId && !lowIds.has(s.itemId)) {
        await DB.del('shopping_list', s.id);
      }
    }
  }

  // When a shopping item is added to storage → update or create fridge item
  async function markAsBought(shoppingId, userId, storageId = null) {
    const entry = await DB.getOne('shopping_list', shoppingId);
    if (!entry) return;
    const targetStorageId = entry.targetStorageId || storageId || null;

    if (entry.itemId) {
      // Update existing item quantity
      const fridgeItem = await ItemModel.getById(entry.itemId);
      if (fridgeItem) {
        const added = parseFloat(entry.quantity) || 1;
        await ItemModel.update(entry.itemId, { quantity: fridgeItem.quantity + added });
      }
    } else if (targetStorageId) {
      // Create new fridge item
      await ItemModel.create(userId, {
        name: entry.name,
        quantity: parseFloat(entry.quantity) || 1,
        unit: entry.unit,
        storageId: targetStorageId,
        shopId: entry.shopId || null,
        expiryDate: entry.expiryDate || null,
        notes: entry.notes || '',
      });
    }

    await DB.del('shopping_list', shoppingId);
    return true;
  }

  async function markManyAsBought(shoppingIds, userId, storageId = null) {
    for (const shoppingId of shoppingIds) {
      await markAsBought(shoppingId, userId, storageId);
    }
  }

  async function copyToClipboard(userId) {
    const [all, shops] = await Promise.all([
      getAll(userId),
      ShopModel.getAll(userId),
    ]);
    const shopNames = new Map(shops.map(shop => [shop.id, shop.name]));
    const grouped = {};
    all
      .filter(i => !i.checked)
      .forEach(item => {
        const key = item.shopId || ShopModel.UNDEFINED_KEY;
        if (!grouped[key]) grouped[key] = [];
        grouped[key].push(item);
      });

    const sortedKeys = Object.keys(grouped).sort((a, b) => {
      if (a === ShopModel.UNDEFINED_KEY) return 1;
      if (b === ShopModel.UNDEFINED_KEY) return -1;
      return String(shopNames.get(a) || '').localeCompare(String(shopNames.get(b) || ''), i18n.lang || 'fr', { sensitivity: 'base' });
    });

    const lines = [];
    for (const key of sortedKeys) {
      const label = key === ShopModel.UNDEFINED_KEY
        ? i18n.t('shopping_store_undefined')
        : (shopNames.get(key) || i18n.t('shopping_store_undefined'));
      lines.push(`=== ${label} ===`);
      grouped[key].forEach(item => {
        lines.push(`• ${item.name}${item.quantity ? ` (${item.quantity}${item.unit ? ' ' + item.unit : ''})` : ''}`);
      });
      lines.push('');
    }

    if (lines.length === 0) return false;
    await navigator.clipboard.writeText(lines.join('\n').trim());
    return true;
  }

  return {
    getAll, add, toggle, changeQuantity, setTargetStorage, remove,
    clearChecked, clearAll, syncAutoRestock,
    markAsBought, markManyAsBought, copyToClipboard,
  };
})();
