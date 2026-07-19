// ============================================================
//  ad-banner.js — Bannière publicitaire placeholder
// ============================================================

const AdBanner = {
  async update() {
    const userId = Auth.getCurrentUserId();
    const banner = document.getElementById('ad-banner');
    if (!banner) return;

    if (!userId) { banner.style.display = 'none'; return; }

    const prefs = await PrefsModel.get(userId);
    if (prefs.isPremium || !prefs.adsEnabled) {
      banner.style.display = 'none';
    } else {
      banner.style.display = 'flex';
      banner.textContent = i18n.t('ad_placeholder');
    }
  }
};
