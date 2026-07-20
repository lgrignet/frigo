// ============================================================
//  all-items.view.js — Onglet "Tous les produits"
// ============================================================

const AllItemsView = (() => {

  let currentFilter = 'all';
  let currentSort = 'expiryDate';
  let searchQuery = '';

  async function render() {
    const userId = Auth.getCurrentUserId();
    let items = await ItemModel.getAll(userId);
    const [storages, shops] = await Promise.all([
      StorageModel.getAll(userId),
      ShopModel.getAll(userId),
    ]);

    // Update filter chips with storages
    renderFilters(storages);

    // Filter
    if (currentFilter !== 'all') {
      items = items.filter(i => i.storageId === currentFilter);
    }
    // Search
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      items = items.filter(i =>
        i.name.toLowerCase().includes(q) ||
        (i.notes || '').toLowerCase().includes(q)
      );
    }
    // Sort
    items = ItemModel.sort(items, currentSort);

    // Header
    document.getElementById('all-title').textContent = i18n.t('all_title');
    document.getElementById('all-sort-label').textContent = i18n.t('sort_by');
    const sortSel = document.getElementById('all-sort-select');
    sortSel.innerHTML = [
      ['expiryDate', i18n.t('sort_expiry')],
      ['name',       i18n.t('sort_name')],
      ['storage',    i18n.t('sort_storage')],
      ['quantity',   i18n.t('sort_quantity')],
      ['createdAt',  i18n.t('sort_added')],
    ].map(([v, l]) => `<option value="${v}" ${currentSort === v ? 'selected' : ''}>${l}</option>`).join('');

    // Search placeholder
    document.getElementById('all-search').placeholder = i18n.t('item_name');

    const list = document.getElementById('all-list');
    const empty = document.getElementById('all-empty');

    if (items.length === 0) {
      list.innerHTML = '';
      empty.style.display = 'flex';
      document.getElementById('all-empty-title').textContent = i18n.t('all_empty');
      document.getElementById('all-empty-sub').textContent = i18n.t('all_empty_sub');
    } else {
      empty.style.display = 'none';
      list.innerHTML = items.map(item => ItemCard.render(item, storages, shops)).join('');
      ItemCard.bindEvents(list, id => openItemActions(id, storages), async (id, action) => {
        await ItemCard.changeQuantity(id, action);
        render();
      });
    }
  }

  function renderFilters(storages) {
    const bar = document.getElementById('all-filters');
    const allChip = `<button class="filter-chip ${currentFilter === 'all' ? 'active' : ''}" data-filter="all">${i18n.t('filter_all')}</button>`;
    const sortedStorages = [...storages].sort((a, b) => {
      const aIsToStore = String(a.name || '').trim().toLowerCase() === 'a ranger';
      const bIsToStore = String(b.name || '').trim().toLowerCase() === 'a ranger';
      if (aIsToStore && !bIsToStore) return -1;
      if (!aIsToStore && bIsToStore) return 1;
      return String(a.name || '').localeCompare(String(b.name || ''), i18n.lang || 'fr', { sensitivity: 'base' });
    });
    const storageChips = sortedStorages.map(s =>
      `<button class="filter-chip ${currentFilter === s.id ? 'active' : ''}" data-filter="${s.id}">${s.icon} ${escHtml(s.name)}</button>`
    ).join('');
    bar.innerHTML = allChip + storageChips;
    bar.querySelectorAll('.filter-chip').forEach(btn => {
      btn.addEventListener('click', () => {
        currentFilter = btn.dataset.filter;
        render();
      });
    });
  }

  async function openItemActions(id, storages) {
    const item = await ItemModel.getById(id);
    if (!item) return;

    // Show action sheet
    document.getElementById('action-item-name').textContent = item.name;
    document.getElementById('btn-action-edit').textContent = i18n.t('edit');
    document.getElementById('btn-action-duplicate').textContent = i18n.t('item_duplicate');
    document.getElementById('btn-action-move').textContent = i18n.t('item_move');
    document.getElementById('btn-action-delete').textContent = i18n.t('delete');

    document.getElementById('btn-action-edit').onclick = () => {
      Modal.close('modal-item-actions');
      ItemForm.open(id, null, () => render());
    };
    document.getElementById('btn-action-duplicate').onclick = async () => {
      await ItemModel.duplicate(id);
      Modal.close('modal-item-actions');
      Toast.success(i18n.t('item_duplicated'));
      render();
    };
    document.getElementById('btn-action-move').onclick = () => {
      Modal.close('modal-item-actions');
      openMoveDialog(id, storages);
    };
    document.getElementById('btn-action-delete').onclick = () => {
      Modal.close('modal-item-actions');
      document.getElementById('confirm-title').textContent = i18n.t('item_delete_confirm');
      document.getElementById('confirm-sub').textContent = i18n.t('item_delete_sub');
      document.getElementById('btn-confirm-yes').textContent = i18n.t('yes_delete');
      document.getElementById('btn-confirm-no').textContent = i18n.t('cancel');
      document.getElementById('btn-confirm-yes').onclick = async () => {
        await ItemModel.remove(id);
        await ShoppingModel.syncAutoRestock(Auth.getCurrentUserId());
        Modal.close('modal-confirm');
        Toast.success(i18n.t('item_deleted'));
        render();
      };
      document.getElementById('btn-confirm-no').onclick = () => Modal.close('modal-confirm');
      Modal.open('modal-confirm');
    };
    Modal.open('modal-item-actions');
  }

  async function openMoveDialog(id, storages) {
    const moveList = document.getElementById('move-storage-list');
    const item = await ItemModel.getById(id);
    moveList.innerHTML = storages
      .filter(s => s.id !== item.storageId)
      .map(s => `
        <button class="prefs-item" data-sid="${s.id}">
          <span class="prefs-item-icon">${s.icon}</span>
          <span class="prefs-item-info">
            <span class="prefs-item-label">${escHtml(s.name)}</span>
          </span>
        </button>`).join('');
    moveList.querySelectorAll('button').forEach(btn => {
      btn.addEventListener('click', async () => {
        await ItemModel.moveToStorage(id, btn.dataset.sid);
        await ShoppingModel.syncAutoRestock(Auth.getCurrentUserId());
        Modal.close('modal-move');
        Toast.success(i18n.t('item_moved'));
        render();
      });
    });
    document.getElementById('modal-move-title').textContent = i18n.t('item_move');
    Modal.open('modal-move');
  }

  function init() {
    document.getElementById('all-search').addEventListener('input', e => {
      searchQuery = e.target.value;
      render();
    });
    document.getElementById('all-sort-select').addEventListener('change', e => {
      currentSort = e.target.value;
      render();
    });
    render();
  }

  return { init, render };
})();
