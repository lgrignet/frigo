// ============================================================
//  alerts.js — Vue Alertes (péremptions + réassort)
// ============================================================

const AlertsModule = (() => {

  function render() {
    renderExpiryAlerts();
    renderRestockAlerts();
    updateAlertHeader();
  }

  function updateAlertHeader() {
    document.getElementById('alerts-view-title').textContent = i18n.t('alerts_title');
    document.getElementById('alerts-expiry-title').textContent = i18n.t('alerts_expiry_title');
    document.getElementById('alerts-restock-title').textContent = i18n.t('alerts_restock_title');
  }

  function renderExpiryAlerts() {
    const container = document.getElementById('alerts-expiry-list');
    const items = Storage.getExpiringItems(7);

    if (items.length === 0) {
      container.innerHTML = `<div class="alert-empty-section">${i18n.lang === 'fr' ? 'Aucun aliment ne périme bientôt 🎉' : 'No items expiring soon 🎉'}</div>`;
      return;
    }

    container.innerHTML = items.map(item => {
      const diffDays = Storage.getDiffDays(item.expiryDate);
      const status = Storage.getExpiryStatus(item.expiryDate);
      const catIcon = i18n.getCategoryIcon(item.category);

      let timeText = '';
      let badgeClass = '';
      if (diffDays < 0) {
        timeText = `${i18n.t('expired_since')} ${Math.abs(diffDays)} ${Math.abs(diffDays) === 1 ? i18n.t('day') : i18n.t('days_plural')}`;
        badgeClass = 'badge-expired';
      } else if (diffDays === 0) {
        timeText = i18n.t('expires_today');
        badgeClass = 'badge-soon';
      } else {
        timeText = `${i18n.t('expires_in')} ${diffDays} ${diffDays === 1 ? i18n.t('day') : i18n.t('days_plural')}`;
        badgeClass = diffDays <= 3 ? 'badge-soon' : 'badge-week';
      }

      const photoHTML = item.photo
        ? `<div class="alert-item-photo" style="background-image:url('${item.photo}')"></div>`
        : `<div class="alert-item-photo alert-item-photo-placeholder">${catIcon}</div>`;

      return `
        <div class="alert-item ${status === 'expired' ? 'alert-item-expired' : ''}" data-id="${item.id}">
          ${photoHTML}
          <div class="alert-item-info">
            <span class="alert-item-name">${escapeHtml(item.name)}</span>
            <span class="alert-item-qty">${item.quantity} ${item.unit || ''}</span>
          </div>
          <div class="alert-item-right">
            <span class="alert-expiry-date">${formatDate(item.expiryDate)}</span>
            <span class="badge ${badgeClass}">${timeText}</span>
          </div>
        </div>`;
    }).join('');

    container.querySelectorAll('.alert-item').forEach(el => {
      el.addEventListener('click', () => ItemsModule.openForm(el.dataset.id));
    });
  }

  function renderRestockAlerts() {
    const container = document.getElementById('alerts-restock-list');
    const items = Storage.getLowStockItems();

    if (items.length === 0) {
      container.innerHTML = `<div class="alert-empty-section">${i18n.lang === 'fr' ? 'Tous les stocks sont OK 👍' : 'All stocks are OK 👍'}</div>`;
      return;
    }

    container.innerHTML = items.map(item => {
      const catIcon = i18n.getCategoryIcon(item.category);
      const needed = Math.max(0, item.restockThreshold - item.quantity + 1);

      const photoHTML = item.photo
        ? `<div class="alert-item-photo" style="background-image:url('${item.photo}')"></div>`
        : `<div class="alert-item-photo alert-item-photo-placeholder">${catIcon}</div>`;

      return `
        <div class="alert-item alert-item-restock" data-id="${item.id}">
          ${photoHTML}
          <div class="alert-item-info">
            <span class="alert-item-name">${escapeHtml(item.name)}</span>
            <span class="alert-item-qty">${item.quantity} / ${item.restockThreshold} ${item.unit || ''}</span>
          </div>
          <div class="alert-item-right">
            <span class="badge badge-restock">+ ${needed} ${i18n.t('restock_qty')}</span>
            <button class="btn-add-to-shopping" data-id="${item.id}" title="${i18n.t('shopping_manual_add')}">
              🛒
            </button>
          </div>
        </div>`;
    }).join('');

    // Add to shopping
    container.querySelectorAll('.btn-add-to-shopping').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const item = Storage.getItem(btn.dataset.id);
        if (!item) return;
        const needed = Math.max(1, item.restockThreshold - item.quantity + 1);
        // Check if already in list
        const existing = Storage.getShoppingList().find(
          s => s.name === item.name && s.source === 'auto'
        );
        if (!existing) {
          Storage.addShoppingItem({
            name: item.name,
            quantity: needed,
            unit: item.unit,
            source: 'auto'
          });
          ShoppingModule.render();
          showToast(`🛒 ${item.name} ${i18n.lang === 'fr' ? 'ajouté aux courses' : 'added to shopping'}`);
        } else {
          showToast(i18n.lang === 'fr' ? 'Déjà dans la liste' : 'Already in list');
        }
      });
    });

    container.querySelectorAll('.alert-item').forEach(el => {
      el.addEventListener('click', () => ItemsModule.openForm(el.dataset.id));
    });
  }

  function init() {
    render();
  }

  return { init, render };
})();
