// ============================================================
//  storage.model.js — CRUD Rangements
// ============================================================

const StorageModel = (() => {

  function uid() { return crypto.randomUUID(); }

  async function getAll(userId) {
    return DB.getUserStorages(userId);
  }

  async function getById(id) {
    return DB.getOne('storages', id);
  }

  async function getByName(userId, name) {
    const all = await getAll(userId);
    const normalized = String(name || '').trim().toLowerCase();
    return all.find(s => String(s.name || '').trim().toLowerCase() === normalized) || null;
  }

  async function create(userId, data) {
    const storage = {
      id: uid(),
      userId,
      name: data.name?.trim(),
      icon: data.icon || '📦',
      type: data.type || 'dry', // cold / dry / frozen
      isDefault: data.isDefault || false,
      createdAt: new Date().toISOString(),
    };
    await DB.put('storages', storage);
    return storage;
  }

  async function update(id, data) {
    const existing = await getById(id);
    if (!existing) throw new Error('Storage not found');
    const updated = { ...existing, ...data, id };
    await DB.put('storages', updated);
    return updated;
  }

  async function remove(id, userId) {
    // Check if storage has items
    const items = await ItemModel.getByStorage(userId, id);
    if (items.length > 0) throw new Error('STORAGE_NOT_EMPTY');
    await DB.del('storages', id);
  }

  async function getItemCount(id, userId) {
    const items = await ItemModel.getByStorage(userId, id);
    return items.length;
  }

  async function getDefaultStorage(userId) {
    const all = await getAll(userId);
    return all.find(s => s.isDefault) || all[0] || null;
  }

  const TYPE_ICONS = { cold: '🌡️', frozen: '❄️', dry: '🌿' };
  function typeIcon(type) { return TYPE_ICONS[type] || '📦'; }

  return {
    getAll, getById, getByName, create, update, remove,
    getItemCount, getDefaultStorage, typeIcon,
  };
})();
