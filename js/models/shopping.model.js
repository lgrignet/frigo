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
      checked: false,
      targetStorageId: data.targetStorageId || null,
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
      const alreadyInList = existingAuto.some(s => s.itemId === item.id);
      if (!alreadyInList) {
        const needed = Math.max(1, item.restockThreshold - item.quantity + 1);
        await add(userId, {
          name: item.name,
          quantity: needed,
          unit: item.unit,
          source: 'auto',
          itemId: item.id,
        });
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

  // When a shopping item is checked as bought → update or create fridge item
  async function markAsBought(shoppingId, userId, storageId) {
    const entry = await DB.getOne('shopping_list', shoppingId);
    if (!entry) return;

    if (entry.itemId) {
      // Update existing item quantity
      const fridgeItem = await ItemModel.getById(entry.itemId);
      if (fridgeItem) {
        const added = parseFloat(entry.quantity) || 1;
        await ItemModel.update(entry.itemId, { quantity: fridgeItem.quantity + added });
      }
    } else if (storageId) {
      // Create new fridge item
      await ItemModel.create(userId, {
        name: entry.name,
        quantity: parseFloat(entry.quantity) || 1,
        unit: entry.unit,
        storageId,
      });
    }

    await toggle(shoppingId);
  }

  async function copyToClipboard(userId) {
    const all = await getAll(userId);
    const lines = all
      .filter(i => !i.checked)
      .map(i => `• ${i.name}${i.quantity ? ` (${i.quantity}${i.unit ? ' ' + i.unit : ''})` : ''}`);
    if (lines.length === 0) return false;
    await navigator.clipboard.writeText(lines.join('\n'));
    return true;
  }

  return {
    getAll, add, toggle, setTargetStorage, remove,
    clearChecked, clearAll, syncAutoRestock,
    markAsBought, copyToClipboard,
  };
})();
