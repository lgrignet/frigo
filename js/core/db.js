// ============================================================
//  db.js — IndexedDB schema, migrations, CRUD helpers
// ============================================================

const DB = (() => {
  const DB_NAME = 'mystockmanager';
  const DB_VERSION = 2;
  let _db = null;

  // ── Open / migrate ─────────────────────────────────────────
  async function open() {
    if (_db) return _db;
    return new Promise((resolve, reject) => {
      const req = indexedDB.open(DB_NAME, DB_VERSION);

      req.onupgradeneeded = (event) => {
        const db = event.target.result;

        // users
        if (!db.objectStoreNames.contains('users')) {
          const users = db.createObjectStore('users', { keyPath: 'id', autoIncrement: true });
          users.createIndex('email', 'email', { unique: true });
        }

        // storages (rangements)
        if (!db.objectStoreNames.contains('storages')) {
          const storages = db.createObjectStore('storages', { keyPath: 'id' });
          storages.createIndex('userId', 'userId', { unique: false });
        }

        // shops (magasins)
        if (!db.objectStoreNames.contains('shops')) {
          const shops = db.createObjectStore('shops', { keyPath: 'id' });
          shops.createIndex('userId', 'userId', { unique: false });
        }

        // items
        if (!db.objectStoreNames.contains('items')) {
          const items = db.createObjectStore('items', { keyPath: 'id' });
          items.createIndex('userId', 'userId', { unique: false });
          items.createIndex('storageId', 'storageId', { unique: false });
          items.createIndex('shopId', 'shopId', { unique: false });
          items.createIndex('expiryDate', 'expiryDate', { unique: false });
        } else {
          const tx = event.target.transaction;
          const items = tx.objectStore('items');
          if (!items.indexNames.contains('shopId')) {
            items.createIndex('shopId', 'shopId', { unique: false });
          }
        }

        // shopping_list
        if (!db.objectStoreNames.contains('shopping_list')) {
          const shopping = db.createObjectStore('shopping_list', { keyPath: 'id' });
          shopping.createIndex('userId', 'userId', { unique: false });
          shopping.createIndex('shopId', 'shopId', { unique: false });
        } else {
          const tx = event.target.transaction;
          const shopping = tx.objectStore('shopping_list');
          if (!shopping.indexNames.contains('shopId')) {
            shopping.createIndex('shopId', 'shopId', { unique: false });
          }
        }

        // preferences
        if (!db.objectStoreNames.contains('preferences')) {
          db.createObjectStore('preferences', { keyPath: 'userId' });
        }
      };

      req.onsuccess = (e) => { _db = e.target.result; resolve(_db); };
      req.onerror   = (e) => reject(e.target.error);
    });
  }

  // ── Generic helpers ────────────────────────────────────────
  async function getAll(storeName, indexName = null, indexValue = null) {
    const db = await open();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(storeName, 'readonly');
      const store = tx.objectStore(storeName);
      let req;
      if (indexName && indexValue !== null) {
        req = store.index(indexName).getAll(indexValue);
      } else {
        req = store.getAll();
      }
      req.onsuccess = (e) => resolve(e.target.result);
      req.onerror   = (e) => reject(e.target.error);
    });
  }

  async function getOne(storeName, key) {
    const db = await open();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(storeName, 'readonly');
      const req = tx.objectStore(storeName).get(key);
      req.onsuccess = (e) => resolve(e.target.result);
      req.onerror   = (e) => reject(e.target.error);
    });
  }

  async function put(storeName, record) {
    const db = await open();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(storeName, 'readwrite');
      const req = tx.objectStore(storeName).put(record);
      req.onsuccess = (e) => resolve(e.target.result);
      req.onerror   = (e) => reject(e.target.error);
    });
  }

  async function del(storeName, key) {
    const db = await open();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(storeName, 'readwrite');
      const req = tx.objectStore(storeName).delete(key);
      req.onsuccess = () => resolve();
      req.onerror   = (e) => reject(e.target.error);
    });
  }

  async function getByIndex(storeName, indexName, value) {
    return getAll(storeName, indexName, value);
  }

  async function countByIndex(storeName, indexName, value) {
    const db = await open();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(storeName, 'readonly');
      const req = tx.objectStore(storeName).index(indexName).count(value);
      req.onsuccess = (e) => resolve(e.target.result);
      req.onerror   = (e) => reject(e.target.error);
    });
  }

  // ── User-specific helpers ──────────────────────────────────
  async function getUserItems(userId) {
    return getAll('items', 'userId', userId);
  }

  async function getUserStorages(userId) {
    return getAll('storages', 'userId', userId);
  }

  async function getUserShoppingList(userId) {
    return getAll('shopping_list', 'userId', userId);
  }

  async function getUserShops(userId) {
    return getAll('shops', 'userId', userId);
  }

  async function getUserPrefs(userId) {
    return getOne('preferences', userId);
  }

  // ── Export / Import ────────────────────────────────────────
  async function exportUserData(userId) {
    const [items, storages, shops, shopping, prefs] = await Promise.all([
      getUserItems(userId),
      getUserStorages(userId),
      getUserShops(userId),
      getUserShoppingList(userId),
      getUserPrefs(userId),
    ]);
    return { exportedAt: new Date().toISOString(), items, storages, shops, shopping, prefs };
  }

  async function importUserData(userId, data) {
    // Import storages first, then items
    for (const s of (data.storages || [])) {
      s.userId = userId;
      await put('storages', s);
    }
    for (const s of (data.shops || [])) {
      s.userId = userId;
      await put('shops', s);
    }
    for (const item of (data.items || [])) {
      item.userId = userId;
      await put('items', item);
    }
    for (const s of (data.shopping || [])) {
      s.userId = userId;
      await put('shopping_list', s);
    }
    if (data.prefs) {
      data.prefs.userId = userId;
      await put('preferences', data.prefs);
    }
  }

  // ── Reset user data ────────────────────────────────────────
  async function deleteAllUserData(userId) {
    const stores = ['items', 'storages', 'shops', 'shopping_list'];
    for (const storeName of stores) {
      const items = await getAll(storeName, 'userId', userId);
      for (const item of items) await del(storeName, item.id);
    }
    await del('preferences', userId);
  }

  return {
    open, getAll, getOne, put, del,
    getByIndex, countByIndex,
    getUserItems, getUserStorages, getUserShops, getUserShoppingList, getUserPrefs,
    exportUserData, importUserData, deleteAllUserData,
  };
})();
