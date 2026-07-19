// ============================================================
//  sw.js — Service Worker (cache-first pour PWA offline)
// ============================================================

const CACHE_NAME = 'frigo-v1';
const PRECACHE = [
  './',
  './index.html',
  './css/style.css',
  './css/themes.css',
  './js/i18n.js',
  './js/app.js',
  './js/core/crypto.js',
  './js/core/db.js',
  './js/core/auth.js',
  './js/models/item.model.js',
  './js/models/storage.model.js',
  './js/models/shopping.model.js',
  './js/models/prefs.model.js',
  './js/components/modal.js',
  './js/components/toast.js',
  './js/components/item-card.js',
  './js/components/item-form.js',
  './js/components/ad-banner.js',
  './js/views/login.view.js',
  './js/views/expiring.view.js',
  './js/views/all-items.view.js',
  './js/views/shopping.view.js',
  './js/views/storages.view.js',
  './js/views/recipes.view.js',
  './js/views/prefs.view.js',
  './manifest.json',
  './icons/icon-192.png',
  './icons/icon-512.png',
];

// Install: precache all assets
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(PRECACHE))
      .then(() => self.skipWaiting())
  );
});

// Activate: clean old caches
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

// Fetch: cache-first for app assets, network-first for API
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);

  // TheMealDB API: network only (don't cache API responses)
  if (url.hostname === 'www.themealdb.com') {
    event.respondWith(fetch(event.request).catch(() => new Response('[]')));
    return;
  }

  // App assets: cache-first
  event.respondWith(
    caches.match(event.request).then(cached => {
      if (cached) return cached;
      return fetch(event.request).then(response => {
        // Cache successful GET requests
        if (event.request.method === 'GET' && response.status === 200) {
          const clone = response.clone();
          caches.open(CACHE_NAME).then(cache => cache.put(event.request, clone));
        }
        return response;
      }).catch(() => {
        // Offline fallback: return index.html for navigation requests
        if (event.request.mode === 'navigate') {
          return caches.match('/index.html');
        }
      });
    })
  );
});
