// ============================================================
//  ad-banner.js — Bannière publicitaire placeholder
// ============================================================

const AdBanner = {
  async update() {
    const userId = Auth.getCurrentUserId();
    const banner = document.getElementById('ad-banner');
    const app    = document.getElementById('app');
    if (!banner) return;

    if (!userId) { banner.style.display = 'none'; app.classList.remove('ad-active'); return; }

    const prefs = await PrefsModel.get(userId);
    if (prefs.isPremium || !prefs.adsEnabled) {
      banner.style.display = 'none';
      app.classList.remove('ad-active');
    } else {
      banner.style.display = 'flex';
      app.classList.add('ad-active');
      banner.textContent = i18n.t('ad_placeholder');
    }
  }
};
