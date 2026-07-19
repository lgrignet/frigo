// ============================================================
//  item-card.js — Composant carte item réutilisable
// ============================================================

const ItemCard = {

  // Renders an item card HTML string
  render(item, storages = [], options = {}) {
    const status = ItemModel.getExpiryStatus(item.expiryDate);
    const diffDays = ItemModel.getDiffDays(item.expiryDate);
    const lowStock = ItemModel.isLowStock(item);
    const storage = storages.find(s => s.id === item.storageId);

    // Status badge
    let badge = '';
    if (status === 'expired') {
      badge = `<span class="badge badge-expired">${i18n.t('expired')}</span>`;
    } else if (status === 'today') {
      badge = `<span class="badge badge-today">${i18n.t('expires_today')}</span>`;
    } else if (status === 'soon') {
      badge = `<span class="badge badge-soon">${i18n.t('expires_in')} ${diffDays}${i18n.t('days')}</span>`;
    } else if (status === 'week') {
      badge = `<span class="badge badge-week">${i18n.t('expires_in')} ${diffDays}${i18n.t('days')}</span>`;
    }
    if (lowStock) {
      badge += `<span class="badge badge-restock">${i18n.t('low_stock')}</span>`;
    }

    // Expiry text
    let expiryText = '';
    if (item.expiryDate) {
      if (diffDays < 0) {
        expiryText = `<span style="color:var(--danger)">${i18n.t('expired_since')} ${Math.abs(diffDays)}${i18n.t('days')}</span>`;
      } else if (diffDays === 0) {
        expiryText = `<span style="color:var(--danger)">${i18n.t('expires_today')}</span>`;
      } else {
        expiryText = formatDate(item.expiryDate);
      }
    } else {
      expiryText = `<span style="color:var(--text-muted)">${i18n.t('no_expiry')}</span>`;
    }

    const photoHTML = item.photo
      ? `<div class="item-photo" style="background-image:url('${item.photo}')"></div>`
      : `<div class="item-photo item-photo-placeholder">${storage?.icon || '📦'}</div>`;

    const storageTag = storage
      ? `<span class="item-storage-tag">${storage.icon} ${escHtml(storage.name)}</span>`
      : '';

    return `
      <div class="card item-card" data-id="${item.id}" style="margin-bottom:8px">
        <div class="item-card-indicator ${status}"></div>
        ${photoHTML}
        <div class="item-card-body">
          <div class="item-card-header">
            <span class="item-card-name">${escHtml(item.name)}</span>
            <div style="display:flex;gap:4px;flex-wrap:wrap;justify-content:flex-end;flex-shrink:0">${badge}</div>
          </div>
          <div class="item-card-meta">
            <span class="item-qty">${item.quantity} ${escHtml(item.unit || '')}</span>
            <span>${expiryText}</span>
            ${storageTag}
          </div>
          ${item.notes ? `<div class="item-notes">📝 ${escHtml(item.notes)}</div>` : ''}
        </div>
      </div>`;
  },

  bindEvents(container, onTap) {
    container.querySelectorAll('.item-card').forEach(card => {
      card.addEventListener('click', () => onTap(card.dataset.id));
    });
  },
};
