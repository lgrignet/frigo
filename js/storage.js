// ============================================================
//  storage.js — Couche de persistance localStorage
// ============================================================

const STORAGE_KEYS = {
  ITEMS: 'frigo_items',
  SHOPPING: 'frigo_shopping',
  SETTINGS: 'frigo_settings',
};

const Storage = {
  // ── Items ──────────────────────────────────────────────────
  getItems() {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEYS.ITEMS)) || [];
    } catch { return []; }
  },

  saveItems(items) {
    localStorage.setItem(STORAGE_KEYS.ITEMS, JSON.stringify(items));
  },

  addItem(item) {
    const items = this.getItems();
    item.id = item.id || crypto.randomUUID();
    item.createdAt = new Date().toISOString();
    item.updatedAt = new Date().toISOString();
    items.push(item);
    this.saveItems(items);
    return item;
  },

  updateItem(id, updates) {
    const items = this.getItems();
    const idx = items.findIndex(i => i.id === id);
    if (idx === -1) return null;
    items[idx] = { ...items[idx], ...updates, updatedAt: new Date().toISOString() };
    this.saveItems(items);
    return items[idx];
  },

  deleteItem(id) {
    const items = this.getItems().filter(i => i.id !== id);
    this.saveItems(items);
  },

  getItem(id) {
    return this.getItems().find(i => i.id === id) || null;
  },

  // ── Shopping list ──────────────────────────────────────────
  getShoppingList() {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEYS.SHOPPING)) || [];
    } catch { return []; }
  },

  saveShoppingList(list) {
    localStorage.setItem(STORAGE_KEYS.SHOPPING, JSON.stringify(list));
  },

  addShoppingItem(item) {
    const list = this.getShoppingList();
    const entry = {
      id: crypto.randomUUID(),
      name: item.name,
      quantity: item.quantity || '',
      unit: item.unit || '',
      source: item.source || 'manual', // 'auto', 'manual', 'recipe'
      checked: false,
      addedAt: new Date().toISOString(),
    };
    list.push(entry);
    this.saveShoppingList(list);
    return entry;
  },

  toggleShoppingItem(id) {
    const list = this.getShoppingList();
    const item = list.find(i => i.id === id);
    if (item) item.checked = !item.checked;
    this.saveShoppingList(list);
  },

  removeShoppingItem(id) {
    this.saveShoppingList(this.getShoppingList().filter(i => i.id !== id));
  },

  clearCheckedShopping() {
    this.saveShoppingList(this.getShoppingList().filter(i => !i.checked));
  },

  clearShoppingList() {
    this.saveShoppingList([]);
  },

  // ── Helpers ────────────────────────────────────────────────
  getExpiryStatus(expiryDate) {
    if (!expiryDate) return 'none';
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const expiry = new Date(expiryDate);
    expiry.setHours(0, 0, 0, 0);
    const diffDays = Math.round((expiry - today) / (1000 * 60 * 60 * 24));
    if (diffDays < 0)  return 'expired';
    if (diffDays === 0) return 'today';
    if (diffDays <= 3) return 'soon';
    if (diffDays <= 7) return 'week';
    return 'ok';
  },

  getDiffDays(expiryDate) {
    if (!expiryDate) return null;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const expiry = new Date(expiryDate);
    expiry.setHours(0, 0, 0, 0);
    return Math.round((expiry - today) / (1000 * 60 * 60 * 24));
  },

  isLowStock(item) {
    return item.restockThreshold > 0 && item.quantity <= item.restockThreshold;
  },

  getExpiringItems(days = 7) {
    return this.getItems().filter(item => {
      if (!item.expiryDate) return false;
      const diff = this.getDiffDays(item.expiryDate);
      return diff !== null && diff <= days;
    }).sort((a, b) => new Date(a.expiryDate) - new Date(b.expiryDate));
  },

  getLowStockItems() {
    return this.getItems().filter(item => this.isLowStock(item));
  },
};
