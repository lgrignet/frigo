// ============================================================
//  app.js — Contrôleur principal MyStockManager
// ============================================================

// ── Utilitaires globaux ─────────────────────────────────────
function escHtml(str) {
  return String(str || '')
    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

const BLOCKED_INPUT_CHARS = /[<>"'`]/g;

function sanitizeInputValue(value) {
  return String(value || '')
    .replace(BLOCKED_INPUT_CHARS, '')
    .replace(/[\u0000-\u001F\u007F]/g, '');
}

function bindInputSecurityGuards() {
  const selector = 'input[type="text"], input[type="email"], input[type="password"], textarea';
  document.querySelectorAll(selector).forEach(input => {
    input.addEventListener('input', () => {
      const clean = sanitizeInputValue(input.value);
      if (clean !== input.value) input.value = clean;
    });
  });
}

function pad(num) {
  return String(num).padStart(2, '0');
}

function parseDateString(dateStr, format = 'european') {
  if (!dateStr) return null;
  const raw = String(dateStr).trim();

  let date = null;
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
    date = new Date(raw);
  } else if (/^(\d{1,2})[\/\-](\d{1,2})[\/\-](\d{4})$/.test(raw)) {
    const parts = raw.split(/[\/\-]/);
    const year = parts[2];
    const day = format === 'american' ? parts[1] : parts[0];
    const month = format === 'american' ? parts[0] : parts[1];
    date = new Date(`${year}-${pad(month)}-${pad(day)}`);
  } else {
    date = new Date(raw);
  }

  return Number.isNaN(date?.getTime()) ? null : date;
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const date = parseDateString(dateStr, App?.dateFormat || 'european');
  if (!date) return '';
  if (App?.dateFormat === 'american') {
    return `${pad(date.getMonth() + 1)}/${pad(date.getDate())}/${date.getFullYear()}`;
  }
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;
}

function parseExpiryDate(value, format = 'european') {
  const date = parseDateString(value, format);
  return date ? date.toISOString().slice(0, 10) : null;
}

function isAndroidNativeRuntime() {
  return !!window.AndroidAdsBridge;
}

async function disableServiceWorkerForAndroidRuntime() {
  if (!('serviceWorker' in navigator)) return;
  const registrations = await navigator.serviceWorker.getRegistrations();
  await Promise.all(registrations.map(reg => reg.unregister()));
  if ('caches' in window) {
    const keys = await caches.keys();
    await Promise.all(keys.map(key => caches.delete(key)));
  }
}

const App = (() => {
  const VIEWS = ['expiring', 'all', 'shopping', 'storages'];
  let currentView = 'expiring';
  let dateFormat = 'european';

  // ── Boot ────────────────────────────────────────────────────
  async function boot() {
    // Apply saved theme immediately (before any render)
    const theme = PrefsModel.getStoredTheme();
    await PrefsModel.applyTheme(theme);

    // Initialize DB
    await DB.open();

    // Init components
    Modal.init();
    ItemForm.init();
    LoginView.init();

    // Check session
    if (Auth.isLoggedIn()) {
      await showMain();
    } else {
      showLogin();
    }

    // Service worker:
    // - enabled for web/PWA
    // - disabled in Android native runtime (WebView wrapper) to avoid stale cache
    if (isAndroidNativeRuntime()) {
      disableServiceWorkerForAndroidRuntime().catch(() => {});
    } else if ('serviceWorker' in navigator) {
      navigator.serviceWorker.register('./sw.js').catch(() => {});
    }
  }

  // ── Login screen ────────────────────────────────────────────
  function showLogin() {
    document.getElementById('screen-login').classList.add('active');
    document.getElementById('screen-main').classList.remove('active');
    LoginView.render();
    if (typeof AdBanner !== 'undefined' && AdBanner.update) {
      AdBanner.update();
    }
  }

  async function onLoginSuccess() {
    document.getElementById('screen-login').classList.remove('active');
    await showMain();
  }

  async function showMain() {
    document.getElementById('screen-main').classList.add('active');

    // Load prefs for language
    const userId = Auth.getCurrentUserId();
    const syncChannel = Auth.getCurrentSyncChannel();

    // Initialisation de la synchronisation P2P
    if (syncChannel) {
      SyncConnector.init(syncChannel).catch(err => console.error('Sync Init Error:', err));
    }

    const prefs = await PrefsModel.get(userId);
    if (prefs.lang && prefs.lang !== i18n.lang) {
      i18n.setLang(prefs.lang);
    }
    dateFormat = prefs.dateFormat || 'european';
    await PrefsModel.applyTheme(prefs.theme || 'dark');

    // Ad banner
    await AdBanner.update();

    // Apply translations to nav
    applyLanguage();

    // Init views
    ExpiringView.init();
    AllItemsView.init();
    ShoppingView.init();
    StoragesView.init();
    RecipesView.init();
    PrefsView.init();

    // Navigate to expiring view
    navigateTo('expiring');
  }

  // ── Navigation ──────────────────────────────────────────────
  function navigateTo(view) {
    if (!VIEWS.includes(view)) return;
    currentView = view;

    VIEWS.forEach(v => {
      document.getElementById(`view-${v}`).classList.toggle('active', v === view);
    });
    document.querySelectorAll('.nav-tab').forEach(tab => {
      tab.classList.toggle('active', tab.dataset.view === view);
    });

    // Show/hide FAB
    document.getElementById('fab-main').style.display =
      (view === 'expiring' || view === 'all' || view === 'shopping') ? 'flex' : 'none';

    // Re-render
    if (view === 'expiring') ExpiringView.render();
    if (view === 'all')      AllItemsView.render();
    if (view === 'shopping') ShoppingView.render();
    if (view === 'storages') StoragesView.render();
  }

  // ── Language application ────────────────────────────────────
  function applyLanguage() {
    const tabs = ['expiring', 'all', 'shopping', 'storages'];
    const keys = ['nav_expiring', 'nav_all', 'nav_shopping', 'nav_storages'];
    tabs.forEach((v, i) => {
      const el = document.getElementById(`nav-label-${v}`);
      if (el) el.textContent = i18n.t(keys[i]);
    });
    document.getElementById('header-app-title').textContent = i18n.t('app_name');
  }

  // ── Alert badge updates ─────────────────────────────────────
  async function updateBadges() {
    const userId = Auth.getCurrentUserId();
    if (!userId) return;
    const prefs = await PrefsModel.get(userId);
    const expiring = await ItemModel.getExpiring(userId, prefs.expiryWarningDays);
    const badge = document.getElementById('nav-expiring-badge');
    badge.textContent = expiring.length > 0 ? expiring.length : '';
    badge.style.display = expiring.length > 0 ? 'flex' : 'none';
  }

  return { boot, showLogin, showMain, onLoginSuccess, navigateTo, applyLanguage, updateBadges, get dateFormat() { return dateFormat; }, set dateFormat(value) { dateFormat = value; } };
})();

// ── Affichage d'erreur de boot ──────────────────────────────
function showBootError(msg) {
  const div = document.createElement('div');
  div.style.cssText = 'position:fixed;top:0;left:0;right:0;background:#c0392b;color:#fff;padding:16px;font-size:13px;font-family:monospace;z-index:99999;white-space:pre-wrap;word-break:break-all;max-height:50vh;overflow:auto';
  div.textContent = '⚠️ BOOT ERROR:\n' + msg;
  document.body.appendChild(div);
}

// ── Main event listeners ────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
  bindInputSecurityGuards();

  // Nav tabs
  document.querySelectorAll('.nav-tab').forEach(tab => {
    tab.addEventListener('click', () => App.navigateTo(tab.dataset.view));
  });

  // Header buttons
  document.getElementById('btn-open-prefs').addEventListener('click', () => PrefsView.open());
  document.getElementById('btn-open-recipes').addEventListener('click', () => RecipesView.open());

  // FAB
  document.getElementById('fab-main').addEventListener('click', async () => {
    const userId = Auth.getCurrentUserId();
    const prefs = await PrefsModel.get(userId);
    const activeView = document.querySelector('.nav-tab.active')?.dataset.view;
    const forShopping = activeView === 'shopping';
    ItemForm.open(null, prefs.defaultStorageId, async () => {
      await App.updateBadges();
      if (document.getElementById('view-expiring').classList.contains('active')) ExpiringView.render();
      if (document.getElementById('view-all').classList.contains('active')) AllItemsView.render();
      if (document.getElementById('view-shopping').classList.contains('active')) await ShoppingView.render();
    }, forShopping);
  });

  // Boot app avec gestion d'erreur visible
  try {
    await App.boot();
  } catch (err) {
    showBootError(err?.stack || err?.message || String(err));
  }
});
