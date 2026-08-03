// ============================================================
//  item.model.js — CRUD Items
// ============================================================

const ItemModel = (() => {

  function uid() { return crypto.randomUUID(); }

  // ── Get ────────────────────────────────────────────────────
  async function getAll(userId) {
    return DB.getUserItems(userId);
  }

  async function getById(id) {
    return DB.getOne('items', id);
  }

  async function getByStorage(userId, storageId) {
    const all = await DB.getUserItems(userId);
    return all.filter(i => i.storageId === storageId);
  }

  async function getByShop(userId, shopId) {
    const all = await DB.getUserItems(userId);
    if (!shopId) return all.filter(i => !i.shopId);
    return all.filter(i => i.shopId === shopId);
  }

  async function getExpiring(userId, days) {
    const all = await DB.getUserItems(userId);
    const today = new Date(); today.setHours(0,0,0,0);
    const limit = new Date(today); limit.setDate(limit.getDate() + days);
    return all
      .filter(i => i.expiryDate)
      .filter(i => {
        const d = parseDateString(i.expiryDate, App?.dateFormat || 'european');
        if (!d) return false;
        d.setHours(0,0,0,0);
        return d <= limit;
      })
      .sort((a, b) => {
        const da = parseDateString(a.expiryDate, App?.dateFormat || 'european');
        const db = parseDateString(b.expiryDate, App?.dateFormat || 'european');
        if (!da && !db) return 0;
        if (!da) return 1;
        if (!db) return -1;
        return da - db;
      });
  }

  async function getLowStock(userId) {
    const all = await DB.getUserItems(userId);
    return all.filter(i => i.restockThreshold > 0 && i.quantity <= i.restockThreshold);
  }

  // ── Create ─────────────────────────────────────────────────
  async function create(userId, data) {
    const item = {
      id: uid(),
      userId,
      name: data.name?.trim(),
      quantity: parseFloat(data.quantity) || 0,
      unit: data.unit || 'pièce(s)',
      expiryDate: data.expiryDate || null,
      storageId: data.storageId,
      shopId: data.shopId || null,
      photo: data.photo || null,
      restockThreshold: parseInt(data.restockThreshold) || 0,
      restockBuyQuantity: parseFloat(data.restockBuyQuantity) || 1,
      notes: data.notes?.trim() || '',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    await DB.put('items', item);

    // Propagation P2P via Yjs
    try {
      const itemsMap = SyncConnector.getCollection('items');
      itemsMap.set(item.id, item);
    } catch (e) { console.warn('Sync: non disponible au moment de la création'); }

    return item;
  }

  // ── Update ─────────────────────────────────────────────────
  async function update(id, data) {
    const existing = await getById(id);
    if (!existing) throw new Error('Item not found');
    const updated = {
      ...existing,
      ...data,
      id, // preserve id
      updatedAt: new Date().toISOString(),
    };
    await DB.put('items', updated);

    // Propagation P2P via Yjs
    try {
      const itemsMap = SyncConnector.getCollection('items');
      itemsMap.set(id, updated);
    } catch (e) { console.warn('Sync: non disponible au moment de la mise à jour'); }

    return updated;
  }

  // ── Delete ─────────────────────────────────────────────────
  async function remove(id) {
    await DB.del('items', id);

    // Propagation P2P via Yjs (suppression)
    try {
      const itemsMap = SyncConnector.getCollection('items');
      itemsMap.delete(id);
    } catch (e) { console.warn('Sync: non disponible au moment de la suppression'); }

    // Also remove related shopping entries (auto)
    const session = Auth.getSession();
    if (session) {
      const shopping = await DB.getUserShoppingList(session.userId);
      const toRemove = shopping.filter(s => s.itemId === id);
      for (const s of toRemove) await DB.del('shopping_list', s.id);
    }
  }

  // ── Duplicate ──────────────────────────────────────────────
  async function duplicate(id) {
    const original = await getById(id);
    if (!original) throw new Error('Item not found');
    const copy = {
      ...original,
      id: uid(),
      name: original.name + ' (copie)',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    await DB.put('items', copy);
    return copy;
  }

  // ── Move to another storage ────────────────────────────────
  async function moveToStorage(id, storageId) {
    return update(id, { storageId });
  }

  // ── Expiry helpers ─────────────────────────────────────────
  function getDiffDays(expiryDate) {
    if (!expiryDate) return null;
    const today = new Date(); today.setHours(0,0,0,0);
    const exp = parseDateString(expiryDate, App?.dateFormat || 'european');
    if (!exp) return null;
    exp.setHours(0,0,0,0);
    return Math.round((exp - today) / 86400000);
  }

  function getExpiryStatus(expiryDate) {
    const diff = getDiffDays(expiryDate);
    if (diff === null) return 'none';
    if (diff < 0)  return 'expired';
    if (diff === 0) return 'today';
    if (diff <= 3) return 'soon';
    if (diff <= 7) return 'week';
    return 'ok';
  }

  function isLowStock(item) {
    return item.restockThreshold > 0 && item.quantity <= item.restockThreshold;
  }

  // ── Sort ───────────────────────────────────────────────────
  function sort(items, by = 'expiryDate') {
    return [...items].sort((a, b) => {
      switch (by) {
        case 'expiryDate': {
          if (!a.expiryDate && !b.expiryDate) return 0;
          if (!a.expiryDate) return 1;
          if (!b.expiryDate) return -1;
          const da = parseDateString(a.expiryDate, App?.dateFormat || 'european');
          const db = parseDateString(b.expiryDate, App?.dateFormat || 'european');
          if (!da && !db) return 0;
          if (!da) return 1;
          if (!db) return -1;
          return da - db;
        }
        case 'name':       return a.name.localeCompare(b.name);
        case 'quantity':   return a.quantity - b.quantity;
        case 'createdAt':  return new Date(b.createdAt) - new Date(a.createdAt);
        case 'storage':    return (a.storageId || '').localeCompare(b.storageId || '');
        default: return 0;
      }
    });
  }

  return {
    getAll, getById, getByStorage, getByShop, getExpiring, getLowStock,
    create, update, remove, duplicate, moveToStorage,
    getDiffDays, getExpiryStatus, isLowStock, sort,
  };
})();
