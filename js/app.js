// ============================================================
//  app.js — Contrôleur principal MyStockManager
// ============================================================

// ── Utilitaires globaux ─────────────────────────────────────
function escHtml(str) {
  return String(str || '')
    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleDateString(i18n.lang === 'fr' ? 'fr-BE' : 'en-GB', {
    day: '2-digit', month: 'short', year: 'numeric'
  });
}

const App = (() => {
  const VIEWS = ['expiring', 'all', 'shopping', 'storages'];
  let currentView = 'expiring';

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

    // Service worker (chemin relatif pour GitHub Pages)
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.register('./sw.js').catch(() => {});
    }
  }

  // ── Login screen ────────────────────────────────────────────
  function showLogin() {
    document.getElementById('screen-login').classList.add('active');
    document.getElementById('screen-main').classList.remove('active');
    LoginView.render();
  }

  async function onLoginSuccess() {
    document.getElementById('screen-login').classList.remove('active');
    await showMain();
  }

  async function showMain() {
    document.getElementById('screen-main').classList.add('active');

    // Load prefs for language
    const userId = Auth.getCurrentUserId();
    const prefs = await PrefsModel.get(userId);
    if (prefs.lang && prefs.lang !== i18n.lang) {
      i18n.setLang(prefs.lang);
    }
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
      (view === 'expiring' || view === 'all') ? 'flex' : 'none';

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

  return { boot, showLogin, showMain, onLoginSuccess, navigateTo, applyLanguage, updateBadges };
})();

// ── Main event listeners ────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
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
    ItemForm.open(null, prefs.defaultStorageId, async () => {
      await App.updateBadges();
      // Re-render current view
      if (document.getElementById('view-expiring').classList.contains('active')) ExpiringView.render();
      if (document.getElementById('view-all').classList.contains('active')) AllItemsView.render();
    });
  });

  // Boot app
  await App.boot();
});
