// ============================================================
//  sync.js — Connecteur de synchronisation (Yjs + WebSocket)
// ============================================================

const SyncConnector = (() => {
  let _doc = null;
  let _provider = null;
  let _indexeddb = null;

  /**
   * Initialise le document Yjs et la connexion WebSocket
   * @param {string} channelGuid - Le GUID unique du canal
   */
  async function init(channelGuid) {
    if (!channelGuid) return;
    if (_doc) await destroy();

    console.log('Sync: Initialisation du canal', channelGuid);

    // 1. Création du document Yjs
    // Avec les versions UMD/Globales, Y est directement disponible
    if (typeof Y === 'undefined') {
      console.error('Sync: Yjs library not found!');
      return;
    }
    _doc = new Y.Doc();

    // 2. Persistance locale
    // Dans les versions globales, les noms peuvent varier (YIndexeddb ou Y.IndexeddbPersistence)
    const YIDB = typeof YIndexeddbPersistence !== 'undefined' ? YIndexeddbPersistence : (typeof Y.IndexeddbPersistence !== 'undefined' ? Y.IndexeddbPersistence : null);
    if (YIDB) {
      _indexeddb = new YIDB('msm-sync-' + channelGuid, _doc);
      _indexeddb.on('synced', () => {
        console.log('Sync: Données locales chargées (Local)');
        window.dispatchEvent(new CustomEvent('sync:local-ready'));
      });
    }

    // 3. Connexion au serveur centralisé WebSocket
    const WSProvider = typeof WebsocketProvider !== 'undefined' ? WebsocketProvider : (typeof Y.WebsocketProvider !== 'undefined' ? Y.WebsocketProvider : null);

    if (!WSProvider) {
      console.error('Sync: WebSocketProvider not found!');
      return;
    }

    _provider = new WSProvider(
      "wss://sync.noshi.be",
      channelGuid,
      _doc
    );

    _provider.on('status', event => {
      console.log('Sync WebSocket Status:', event.status);
    });

    // 4. Branchement automatique des collections vers IndexedDB
    const collections = ['items', 'storages', 'shops', 'shopping_list', 'preferences'];
    collections.forEach(name => {
      const map = _doc.getMap(name);
      map.observe(async event => {
        // Cette fonction est appelée quand la map change (localement ou via pair)
        const userId = Auth.getCurrentUserId();
        if (!userId) return;

        for (const [key, change] of event.changes.keys) {
          if (change.action === 'add' || change.action === 'update') {
            const data = map.get(key);
            if (data) {
              // On s'assure que le userId est le nôtre pour les données reçues
              // (ou on garde celui du créateur si on veut du multi-utilisateur réel)
              await DB.put(name, data);
            }
          } else if (change.action === 'delete') {
            await DB.del(name, key);
          }
        }
        // Rafraîchir l'UI
        window.dispatchEvent(new CustomEvent('sync:data-changed', { detail: { collection: name } }));
      });
    });

    // 5. Gestion de la propagation des mises à jour de l'app (Migrations)
    _doc.on('update', (update, origin) => {
      // Si l'origine n'est pas locale, cela signifie qu'on a reçu un patch d'un pair
      if (origin !== _provider && origin !== null) {
        console.log('Sync: Mise à jour reçue des pairs');
      }
    });
  }

  async function destroy() {
    if (_provider) _provider.destroy();
    if (_indexeddb) _indexeddb.destroy();
    if (_doc) _doc.destroy();
    _doc = null;
    _provider = null;
    _indexeddb = null;
  }

  // ── Accesseurs pour les données partagées ──────────────────

  /**
   * Retourne une Y.Map partagée pour un type de données
   * @param {string} name - 'items', 'storages', 'shops', etc.
   */
  function getCollection(name) {
    if (!_doc) throw new Error('Sync not initialized');
    return _doc.getMap(name);
  }

  /**
   * Observe les changements sur une collection
   */
  function observe(name, callback) {
    const map = getCollection(name);
    map.observe(event => callback(event));
  }

  function isConnected() {
    return _provider && _provider.wsconnected;
  }

  return { init, destroy, getCollection, observe, isConnected };
})();
