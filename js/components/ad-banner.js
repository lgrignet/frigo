// ============================================================
//  ad-banner.js — Bannière publicitaire placeholder
// ============================================================

const AdBanner = {
  notifyNativeAdVisibility(enabled) {
    try {
      if (window.AndroidAdsBridge && typeof window.AndroidAdsBridge.setAdsEnabled === 'function') {
        window.AndroidAdsBridge.setAdsEnabled(Boolean(enabled));
      }
    } catch (error) {
      // Ignore bridge errors.
    }
  },

  async update() {
    const userId = Auth.getCurrentUserId();
    if (!userId) {
      AdBanner.notifyNativeAdVisibility(false);
      return;
    }

    const prefs = await PrefsModel.get(userId);
    AdBanner.notifyNativeAdVisibility(!(prefs.isPremium || !prefs.adsEnabled));
  }
};
