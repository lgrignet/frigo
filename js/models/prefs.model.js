// ============================================================
//  prefs.model.js — Préférences utilisateur
// ============================================================

const PrefsModel = (() => {

  const DEFAULTS = {
    expiryWarningDays: 7,
    notificationsEnabled: false,
    notificationTime: '08:00',
    defaultUnit: 'pièce(s)',
    defaultStorageId: null,
    theme: 'dark',
    dateFormat: 'european',
    adsEnabled: true,
    lang: 'fr',
  };

  async function get(userId) {
    const prefs = await DB.getUserPrefs(userId);
    return { ...DEFAULTS, ...(prefs || {}), userId };
  }

  async function set(userId, updates) {
    const current = await get(userId);
    const updated = { ...current, ...updates, userId };
    await DB.put('preferences', updated);

    // Propagation P2P via Yjs
    try {
      const map = SyncConnector.getCollection('preferences');
      map.set(String(userId), updated);
    } catch (e) { console.warn('Sync Prefs Error:', e); }

    return updated;
  }

  async function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('msm_theme', theme);
  }

  function getStoredTheme() {
    return localStorage.getItem('msm_theme') || 'dark';
  }

  return { get, set, applyTheme, getStoredTheme, DEFAULTS };
})();
