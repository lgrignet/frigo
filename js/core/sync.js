// ============================================================
//  sync.js — Connecteur de synchronisation P2P (Yjs + WebRTC)
// ============================================================

const SyncConnector = (() => {
  let _doc = null;
  let _provider = null;
  let _indexeddb = null;

  /**
   * Initialise le document Yjs et la connexion WebRTC
   * @param {string} channelGuid - Le GUID unique du canal
   */
  async function init(channelGuid) {
    if (!channelGuid) return;
    if (_doc) await destroy();

    console.log('Sync: Initialisation du canal', channelGuid);

    // 1. Création du document Yjs
    _doc = new Y.Doc();

    // 2. Persistance locale avec IndexedDB (Offline-first)
    // y-indexeddb permet de sauvegarder l'état CRDT localement entre les sessions
    _indexeddb = new YIndexeddbPersistence('msm-sync-' + channelGuid, _doc);

    _indexeddb.on('synced', () => {
      console.log('Sync: Données locales chargées depuis IndexedDB');
      // Déclencher un événement global pour rafraîchir l'UI si nécessaire
      window.dispatchEvent(new CustomEvent('sync:local-ready'));
    });

    // 3. Connexion WebRTC pour la synchro P2P
    // On utilise le GUID comme nom de room.
    // NOTE: Pour la production, remplacez signaling par votre propre serveur WebSocket.
    _provider = new YWebrtcProvider('msm-room-' + channelGuid, _doc, {
      signaling: ['wss://signaling.yjs.dev'], // Serveur de signalisation public pour le test
      password: null // Optionnel: ajouter un mot de passe de canal
    });

    _provider.on('status', event => {
      console.log('Sync WebRTC Status:', event.status); // 'connected' or 'disconnected'
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
    return _provider && _provider.connected;
  }

  return { init, destroy, getCollection, observe, isConnected };
})();
