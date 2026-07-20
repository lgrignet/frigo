// ============================================================
//  shop.model.js — CRUD Magasins
// ============================================================

const ShopModel = (() => {

  const UNDEFINED_KEY = '__undefined__';

  function uid() { return crypto.randomUUID(); }

  async function getAll(userId) {
    return DB.getUserShops(userId);
  }

  async function getById(id) {
    return DB.getOne('shops', id);
  }

  async function create(userId, data) {
    const shop = {
      id: uid(),
      userId,
      name: data.name?.trim(),
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    await DB.put('shops', shop);
    return shop;
  }

  async function update(id, data) {
    const existing = await getById(id);
    if (!existing) throw new Error('Shop not found');
    const updated = {
      ...existing,
      name: data.name?.trim() || existing.name,
      updatedAt: new Date().toISOString(),
    };
    await DB.put('shops', updated);
    return updated;
  }

  async function remove(id, userId) {
    await DB.del('shops', id);

    const [items, shopping] = await Promise.all([
      ItemModel.getAll(userId),
      ShoppingModel.getAll(userId),
    ]);

    for (const item of items) {
      if (item.shopId === id) {
        await ItemModel.update(item.id, { shopId: null });
      }
    }

    for (const entry of shopping) {
      if (entry.shopId === id) {
        await DB.put('shopping_list', { ...entry, shopId: null });
      }
    }
  }

  async function getItemCount(id, userId) {
    const items = await ItemModel.getAll(userId);
    return items.filter(item => item.shopId === id).length;
  }

  async function getByName(userId, name) {
    const all = await getAll(userId);
    const normalized = String(name || '').trim().toLowerCase();
    return all.find(s => String(s.name || '').trim().toLowerCase() === normalized) || null;
  }

  function normalizeShopId(value) {
    return value && value !== UNDEFINED_KEY ? value : null;
  }

  function toFilterValue(shopId) {
    return shopId || UNDEFINED_KEY;
  }

  return {
    UNDEFINED_KEY,
    getAll, getById, create, update, remove, getItemCount, getByName,
    normalizeShopId, toFilterValue,
  };
})();
