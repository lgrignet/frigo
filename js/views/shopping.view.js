// ============================================================
//  shopping.view.js — Onglet "Liste de courses"
// ============================================================

const ShoppingView = (() => {

  async function render() {
    const userId = Auth.getCurrentUserId();

    // Sync auto-restock first
    await ShoppingModel.syncAutoRestock(userId);

    const list = await ShoppingModel.getAll(userId);
    const storages = await StorageModel.getAll(userId);

    // Header labels
    document.getElementById('shopping-view-title').textContent = i18n.t('shopping_title');
    document.getElementById('shopping-input').placeholder = i18n.t('shopping_placeholder');
    document.getElementById('btn-shopping-add').textContent = i18n.t('shopping_add_manual');
    document.getElementById('btn-shopping-copy').textContent = i18n.t('shopping_copy');
    document.getElementById('btn-shopping-clear').textContent = i18n.t('shopping_clear_checked');

    const autoItems   = list.filter(i => i.source === 'auto');
    const manualItems = list.filter(i => i.source === 'manual');
    const recipeItems = list.filter(i => i.source === 'recipe');

    // Sections
    renderSection('shopping-auto-list',   'shopping-auto-title',   autoItems,   i18n.t('shopping_section_auto'),   storages, userId);
    renderSection('shopping-manual-list', 'shopping-manual-title', manualItems, i18n.t('shopping_section_manual'), storages, userId);
    renderSection('shopping-recipe-list', 'shopping-recipe-title', recipeItems, i18n.t('shopping_section_recipe'), storages, userId);

    // Hide recipe section if empty
    document.getElementById('shopping-recipe-section').style.display = recipeItems.length > 0 ? 'block' : 'none';

    // Badge
    updateBadge(list);

    const emptyAll = list.length === 0;
    document.getElementById('shopping-empty').style.display = emptyAll ? 'flex' : 'none';
    document.getElementById('shopping-empty-title').textContent = i18n.t('shopping_empty');
    document.getElementById('shopping-empty-sub').textContent = i18n.t('shopping_empty_sub');
  }

  function renderSection(listId, titleId, items, title, storages, userId) {
    document.getElementById(titleId).textContent = title;
    const container = document.getElementById(listId);
    if (items.length === 0) { container.innerHTML = ''; return; }

    container.innerHTML = items.map(item => `
      <div class="shopping-item ${item.checked ? 'checked' : ''}" data-id="${item.id}">
        <div class="shopping-check ${item.checked ? 'checked' : ''}" data-id="${item.id}">
          ${item.checked ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>' : ''}
        </div>
        <div style="flex:1;min-width:0">
          <div class="shopping-item-name ${item.checked ? 'line-through' : ''}">${escHtml(item.name)}</div>
          ${item.quantity ? `<div class="shopping-item-qty">${item.quantity} ${item.unit || ''}</div>` : ''}
        </div>
        ${item.checked ? `
          <button class="btn-sm btn-secondary shopping-to-storage" data-id="${item.id}" style="font-size:11px;padding:5px 8px">
            🏪 ${i18n.t('shopping_add_to_storage')}
          </button>` : ''}
        <button class="shopping-btn-remove" data-id="${item.id}">✕</button>
      </div>
    `).join('');

    // Events
    container.querySelectorAll('.shopping-check').forEach(btn => {
      btn.addEventListener('click', async () => {
        await ShoppingModel.toggle(btn.dataset.id);
        render();
      });
    });
    container.querySelectorAll('.shopping-to-storage').forEach(btn => {
      btn.addEventListener('click', () => openAddToStorage(btn.dataset.id, storages, userId));
    });
    container.querySelectorAll('.shopping-btn-remove').forEach(btn => {
      btn.addEventListener('click', async () => {
        await ShoppingModel.remove(btn.dataset.id);
        render();
      });
    });
  }

  function openAddToStorage(shoppingId, storages, userId) {
    const moveList = document.getElementById('move-storage-list');
    document.getElementById('modal-move-title').textContent = i18n.t('shopping_add_to_storage');
    moveList.innerHTML = storages.map(s => `
      <button class="prefs-item" data-sid="${s.id}">
        <span class="prefs-item-icon">${s.icon}</span>
        <span class="prefs-item-info">
          <span class="prefs-item-label">${escHtml(s.name)}</span>
        </span>
      </button>`).join('');
    moveList.querySelectorAll('button').forEach(btn => {
      btn.addEventListener('click', async () => {
        await ShoppingModel.markAsBought(shoppingId, userId, btn.dataset.sid);
        await ShoppingModel.syncAutoRestock(userId);
        Modal.close('modal-move');
        Toast.success('✓ ' + (i18n.lang === 'fr' ? 'Ajouté au rangement' : 'Added to storage'));
        render();
        ExpiringView.render();
        AllItemsView.render();
      });
    });
    Modal.open('modal-move');
  }

  function updateBadge(list) {
    const unchecked = list.filter(i => !i.checked).length;
    const badge = document.getElementById('nav-shopping-badge');
    badge.textContent = unchecked > 0 ? unchecked : '';
    badge.style.display = unchecked > 0 ? 'flex' : 'none';
  }

  async function addManual() {
    const input = document.getElementById('shopping-input');
    const name = input.value.trim();
    if (!name) return;
    await ShoppingModel.add(Auth.getCurrentUserId(), { name, source: 'manual' });
    input.value = '';
    render();
  }

  function init() {
    document.getElementById('shopping-input').addEventListener('keydown', e => { if (e.key === 'Enter') addManual(); });
    document.getElementById('btn-shopping-add').addEventListener('click', addManual);
    document.getElementById('btn-shopping-copy').addEventListener('click', async () => {
      const ok = await ShoppingModel.copyToClipboard(Auth.getCurrentUserId());
      Toast.success(ok ? i18n.t('shopping_copied') : (i18n.lang === 'fr' ? 'Liste vide' : 'Empty list'));
    });
    document.getElementById('btn-shopping-clear').addEventListener('click', async () => {
      await ShoppingModel.clearChecked(Auth.getCurrentUserId());
      render();
    });
    render();
  }

  return { init, render };
})();
