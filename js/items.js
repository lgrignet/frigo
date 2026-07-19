// ============================================================
//  items.js — Vue Frigo + CRUD items
// ============================================================

const ItemsModule = (() => {

  let currentFilter = 'all';
  let searchQuery = '';
  let editingId = null;
  let capturedPhotoData = null;

  // ── Render fridge view ─────────────────────────────────────
  function render() {
    renderFilterBadges();
    renderItemGrid();
    updateNavBadge();
  }

  function renderFilterBadges() {
    const items = Storage.getItems();
    const expiringCount = items.filter(i => {
      const s = Storage.getExpiryStatus(i.expiryDate);
      return s === 'expired' || s === 'today' || s === 'soon' || s === 'week';
    }).length;
    const restockCount = items.filter(i => Storage.isLowStock(i)).length;

    document.getElementById('filter-expiring-count').textContent = expiringCount || '';
    document.getElementById('filter-restock-count').textContent = restockCount || '';

    document.getElementById('fridge-item-count').textContent =
      `${items.length} ${i18n.t('items_count')}`;

    // Update filter button labels
    document.getElementById('btn-filter-all').querySelector('.filter-label').textContent = i18n.t('filter_all');
    document.getElementById('btn-filter-expiring').querySelector('.filter-label').textContent = i18n.t('filter_expiring');
    document.getElementById('btn-filter-restock').querySelector('.filter-label').textContent = i18n.t('filter_restock');
  }

  function getFilteredItems() {
    let items = Storage.getItems();
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      items = items.filter(i => i.name.toLowerCase().includes(q) ||
        (i.category && i18n.getCategoryLabel(i.category).toLowerCase().includes(q)));
    }
    if (currentFilter === 'expiring') {
      items = items.filter(i => {
        const s = Storage.getExpiryStatus(i.expiryDate);
        return s === 'expired' || s === 'today' || s === 'soon' || s === 'week';
      });
    } else if (currentFilter === 'restock') {
      items = items.filter(i => Storage.isLowStock(i));
    }
    return items;
  }

  function renderItemGrid() {
    const grid = document.getElementById('item-grid');
    const emptyState = document.getElementById('fridge-empty');
    const items = getFilteredItems();

    if (items.length === 0) {
      grid.innerHTML = '';
      emptyState.style.display = 'flex';
      return;
    }
    emptyState.style.display = 'none';

    grid.innerHTML = items.map(item => renderItemCard(item)).join('');

    // Bind events
    grid.querySelectorAll('.item-card').forEach(card => {
      card.addEventListener('click', () => openDetail(card.dataset.id));
    });
  }

  function renderItemCard(item) {
    const status = Storage.getExpiryStatus(item.expiryDate);
    const lowStock = Storage.isLowStock(item);
    const diffDays = Storage.getDiffDays(item.expiryDate);
    const catIcon = i18n.getCategoryIcon(item.category);

    let statusClass = '';
    let statusBadge = '';
    if (status === 'expired') {
      statusClass = 'status-expired';
      statusBadge = `<span class="badge badge-expired">${i18n.t('status_expired')}</span>`;
    } else if (status === 'today' || status === 'soon') {
      statusClass = 'status-soon';
      statusBadge = `<span class="badge badge-soon">${i18n.t('status_expiring_soon')}</span>`;
    } else if (status === 'week') {
      statusClass = 'status-week';
      statusBadge = `<span class="badge badge-week">${i18n.t('status_expiring_week')}</span>`;
    }
    if (lowStock) {
      statusBadge += `<span class="badge badge-restock">${i18n.t('status_low_stock')}</span>`;
    }

    const photoHTML = item.photo
      ? `<div class="item-photo" style="background-image:url('${item.photo}')"></div>`
      : `<div class="item-photo item-photo-placeholder">${catIcon}</div>`;

    let expiryText = '';
    if (item.expiryDate) {
      if (diffDays < 0) {
        expiryText = `<span class="expiry-text text-expired">⚠ ${i18n.t('expired_since')} ${Math.abs(diffDays)}${i18n.t('days')}</span>`;
      } else if (diffDays === 0) {
        expiryText = `<span class="expiry-text text-soon">⏰ ${i18n.t('expires_today')}</span>`;
      } else {
        expiryText = `<span class="expiry-text">${formatDate(item.expiryDate)}</span>`;
      }
    }

    return `
      <div class="item-card ${statusClass}" data-id="${item.id}">
        ${photoHTML}
        <div class="item-card-body">
          <div class="item-card-header">
            <h3 class="item-name">${escapeHtml(item.name)}</h3>
            <div class="item-badges">${statusBadge}</div>
          </div>
          <div class="item-meta">
            <span class="item-qty">${item.quantity} ${escapeHtml(item.unit || '')}</span>
            ${expiryText}
          </div>
        </div>
        <div class="item-card-indicator ${statusClass}"></div>
      </div>`;
  }

  // ── Detail modal ────────────────────────────────────────────
  function openDetail(id) {
    const item = Storage.getItem(id);
    if (!item) return;
    const status = Storage.getExpiryStatus(item.expiryDate);
    const diffDays = Storage.getDiffDays(item.expiryDate);
    const catIcon = i18n.getCategoryIcon(item.category);
    const catLabel = i18n.getCategoryLabel(item.category);

    let expiryText = i18n.t('no_expiry');
    let expiryClass = '';
    if (item.expiryDate) {
      if (diffDays < 0) {
        expiryText = `${i18n.t('expired_since')} ${Math.abs(diffDays)} ${i18n.t('days_plural')}`;
        expiryClass = 'text-expired';
      } else if (diffDays === 0) {
        expiryText = i18n.t('expires_today');
        expiryClass = 'text-soon';
      } else {
        expiryText = `${formatDate(item.expiryDate)} (${i18n.t('expires_in')} ${diffDays}${i18n.t('days')})`;
      }
    }

    document.getElementById('detail-photo').innerHTML = item.photo
      ? `<img src="${item.photo}" alt="${escapeHtml(item.name)}">`
      : `<div class="detail-photo-placeholder">${catIcon}</div>`;

    document.getElementById('detail-name').textContent = item.name;
    document.getElementById('detail-category').textContent = `${catLabel}`;
    document.getElementById('detail-qty').textContent = `${item.quantity} ${item.unit || ''}`;
    document.getElementById('detail-expiry').innerHTML = `<span class="${expiryClass}">${expiryText}</span>`;
    document.getElementById('detail-restock').textContent = item.restockThreshold > 0
      ? `≤ ${item.restockThreshold} ${item.unit || ''}`
      : '—';

    const lowStock = Storage.isLowStock(item);
    document.getElementById('detail-restock-alert').style.display = lowStock ? 'flex' : 'none';

    document.getElementById('btn-detail-edit').onclick = () => { closeDetail(); openForm(id); };
    document.getElementById('btn-detail-delete').onclick = () => confirmDelete(id);

    openModal('modal-detail');
  }

  function closeDetail() {
    closeModal('modal-detail');
  }

  // ── Delete confirm ──────────────────────────────────────────
  function confirmDelete(id) {
    closeDetail();
    document.getElementById('modal-delete-title').textContent = i18n.t('item_delete_confirm');
    document.getElementById('modal-delete-sub').textContent = i18n.t('item_delete_sub');
    document.getElementById('btn-delete-yes').textContent = i18n.t('item_delete_yes');
    document.getElementById('btn-delete-no').textContent = i18n.t('item_delete_no');
    document.getElementById('btn-delete-yes').onclick = () => {
      Storage.deleteItem(id);
      closeModal('modal-delete');
      render();
      AlertsModule.render();
      showToast('🗑️ ' + (i18n.lang === 'fr' ? 'Aliment supprimé' : 'Item deleted'));
    };
    document.getElementById('btn-delete-no').onclick = () => closeModal('modal-delete');
    openModal('modal-delete');
  }

  // ── Form (add / edit) ───────────────────────────────────────
  function openForm(id = null) {
    editingId = id;
    capturedPhotoData = null;
    const item = id ? Storage.getItem(id) : null;

    document.getElementById('form-title').textContent = i18n.t(id ? 'item_edit' : 'item_add');
    document.getElementById('field-name').value = item?.name || '';
    document.getElementById('field-qty').value = item?.quantity || 1;
    document.getElementById('field-expiry').value = item?.expiryDate || '';
    document.getElementById('field-restock').value = item?.restockThreshold ?? 0;

    // Photo
    capturedPhotoData = item?.photo || null;
    updatePhotoPreview();

    // Category select
    const catSel = document.getElementById('field-category');
    catSel.innerHTML = i18n.categories().map(c =>
      `<option value="${c.id}" ${item?.category === c.id ? 'selected' : ''}>${c.icon} ${c.label}</option>`
    ).join('');

    // Unit select
    const unitSel = document.getElementById('field-unit');
    unitSel.innerHTML = i18n.units().map(u =>
      `<option value="${u}" ${item?.unit === u ? 'selected' : ''}>${u}</option>`
    ).join('');
    if (item?.unit) unitSel.value = item.unit;

    // Labels
    document.getElementById('label-name').textContent = i18n.t('item_name');
    document.getElementById('label-qty').textContent = i18n.t('item_quantity');
    document.getElementById('label-unit').textContent = i18n.t('item_unit');
    document.getElementById('label-expiry').textContent = i18n.t('item_expiry');
    document.getElementById('label-category').textContent = i18n.t('item_category');
    document.getElementById('label-restock').textContent = i18n.t('item_restock');
    document.getElementById('label-restock-help').textContent = i18n.t('item_restock_help');
    document.getElementById('label-photo').textContent = i18n.t('item_photo');
    document.getElementById('btn-save').textContent = i18n.t('item_save');
    document.getElementById('btn-cancel-form').textContent = i18n.t('item_cancel');
    document.getElementById('btn-take-photo').title = i18n.t('take_photo');
    document.getElementById('btn-choose-photo').title = i18n.t('choose_photo');

    openModal('modal-form');
    document.getElementById('field-name').focus();
  }

  function updatePhotoPreview() {
    const preview = document.getElementById('photo-preview');
    const btnRemove = document.getElementById('btn-remove-photo');
    if (capturedPhotoData) {
      preview.style.backgroundImage = `url('${capturedPhotoData}')`;
      preview.classList.add('has-photo');
      btnRemove.style.display = 'flex';
    } else {
      preview.style.backgroundImage = '';
      preview.classList.remove('has-photo');
      btnRemove.style.display = 'none';
    }
  }

  function saveForm() {
    const name = document.getElementById('field-name').value.trim();
    if (!name) {
      document.getElementById('field-name').classList.add('field-error');
      document.getElementById('field-name').focus();
      return;
    }
    document.getElementById('field-name').classList.remove('field-error');

    const data = {
      name,
      quantity: parseFloat(document.getElementById('field-qty').value) || 0,
      unit: document.getElementById('field-unit').value,
      expiryDate: document.getElementById('field-expiry').value || null,
      category: document.getElementById('field-category').value,
      restockThreshold: parseInt(document.getElementById('field-restock').value) || 0,
      photo: capturedPhotoData || null,
    };

    if (editingId) {
      Storage.updateItem(editingId, data);
      showToast('✓ ' + (i18n.lang === 'fr' ? 'Aliment mis à jour' : 'Item updated'));
    } else {
      Storage.addItem(data);
      showToast('✓ ' + (i18n.lang === 'fr' ? 'Aliment ajouté' : 'Item added'));
    }

    closeModal('modal-form');
    render();
    AlertsModule.render();
    ShoppingModule.render();
  }

  // ── Photo capture ───────────────────────────────────────────
  function handlePhotoCapture(input) {
    const file = input.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (e) => {
      // Resize to max 400px wide to save localStorage space
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const maxW = 400;
        const ratio = Math.min(maxW / img.width, maxW / img.height, 1);
        canvas.width = img.width * ratio;
        canvas.height = img.height * ratio;
        canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height);
        capturedPhotoData = canvas.toDataURL('image/jpeg', 0.8);
        updatePhotoPreview();
      };
      img.src = e.target.result;
    };
    reader.readAsDataURL(file);
  }

  // ── Filter ──────────────────────────────────────────────────
  function setFilter(filter) {
    currentFilter = filter;
    document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
    document.getElementById(`btn-filter-${filter}`).classList.add('active');
    renderItemGrid();
  }

  // ── Nav badge ───────────────────────────────────────────────
  function updateNavBadge() {
    const alertCount = Storage.getExpiringItems(7).length;
    const badge = document.getElementById('nav-alerts-badge');
    badge.textContent = alertCount > 0 ? alertCount : '';
    badge.style.display = alertCount > 0 ? 'flex' : 'none';
  }

  // ── Init ────────────────────────────────────────────────────
  function init() {
    // Filter buttons
    document.getElementById('btn-filter-all').addEventListener('click', () => setFilter('all'));
    document.getElementById('btn-filter-expiring').addEventListener('click', () => setFilter('expiring'));
    document.getElementById('btn-filter-restock').addEventListener('click', () => setFilter('restock'));

    // Search
    document.getElementById('fridge-search').addEventListener('input', (e) => {
      searchQuery = e.target.value;
      renderItemGrid();
    });
    document.getElementById('fridge-search').placeholder = i18n.t('search_placeholder');

    // FAB
    document.getElementById('fab-add').addEventListener('click', () => openForm());

    // Form events
    document.getElementById('btn-save').addEventListener('click', saveForm);
    document.getElementById('btn-cancel-form').addEventListener('click', () => closeModal('modal-form'));
    document.getElementById('item-form').addEventListener('submit', (e) => { e.preventDefault(); saveForm(); });

    // Photo buttons
    document.getElementById('btn-take-photo').addEventListener('click', () => {
      const input = document.getElementById('photo-capture');
      input.setAttribute('capture', 'environment');
      input.click();
    });
    document.getElementById('btn-choose-photo').addEventListener('click', () => {
      const input = document.getElementById('photo-capture');
      input.removeAttribute('capture');
      input.click();
    });
    document.getElementById('btn-remove-photo').addEventListener('click', () => {
      capturedPhotoData = null;
      document.getElementById('photo-capture').value = '';
      updatePhotoPreview();
    });
    document.getElementById('photo-capture').addEventListener('change', (e) => handlePhotoCapture(e.target));

    // Detail modal close
    document.getElementById('btn-detail-close').addEventListener('click', closeDetail);

    render();
  }

  return { init, render, openForm, updateNavBadge };
})();
