// ============================================================
//  expiring.view.js — Onglet "Bientôt périmé"
// ============================================================

const ExpiringView = (() => {

  async function render() {
    const userId = Auth.getCurrentUserId();
    const prefs = await PrefsModel.get(userId);
    const days = prefs.expiryWarningDays || 7;
    const items = await ItemModel.getExpiring(userId, days);
    const storages = await StorageModel.getAll(userId);

    // Header
    document.getElementById('expiring-title').textContent = i18n.t('expiring_title');
    document.getElementById('expiring-days-value').textContent = days;
    document.getElementById('expiring-days-label').textContent = i18n.t('expiring_days_unit');

    const list = document.getElementById('expiring-list');
    const empty = document.getElementById('expiring-empty');

    if (items.length === 0) {
      list.innerHTML = '';
      empty.style.display = 'flex';
      document.getElementById('expiring-empty-title').textContent = i18n.t('expiring_empty');
      document.getElementById('expiring-empty-sub').textContent = i18n.t('expiring_empty_sub');
    } else {
      empty.style.display = 'none';
      list.innerHTML = items.map(item => ItemCard.render(item, storages)).join('');
      ItemCard.bindEvents(list, id => ItemForm.open(id, null, () => render()), async (id, action) => {
        await ItemCard.changeQuantity(id, action);
        render();
      });
    }

    if (typeof App !== 'undefined' && typeof App.updateBadges === 'function') {
      await App.updateBadges();
    }
  }

  function init() {
    render();
  }

  return { init, render };
})();
