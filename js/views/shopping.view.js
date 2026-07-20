// ============================================================
//  shopping.view.js — Onglet "Liste de courses"
// ============================================================

const ShoppingView = (() => {
  let searchQuery = '';
  let currentShopFilter = 'all';
  let isAddingToStorage = false;

  async function render() {
    const userId = Auth.getCurrentUserId();

    await ShoppingModel.syncAutoRestock(userId);

    const [list, shops] = await Promise.all([
      ShoppingModel.getAll(userId),
      ShopModel.getAll(userId),
    ]);
    const shopsMap = new Map(shops.map(shop => [shop.id, shop]));

    const query = searchQuery.trim().toLowerCase();
    let filtered = query
      ? list.filter(i => i.name?.toLowerCase().includes(query))
      : list;

    filtered = filterByShop(filtered);

    // Header labels
    document.getElementById('shopping-view-title').textContent = i18n.t('shopping_title');
    const shoppingInput = document.getElementById('shopping-input');
    shoppingInput.placeholder = i18n.t('shopping_placeholder');
    shoppingInput.value = searchQuery;
    document.getElementById('btn-shopping-copy').textContent = i18n.t('shopping_copy');
    document.getElementById('btn-shopping-clear').textContent = i18n.t('shopping_clear_checked');

    renderStoreFilters(shops);

    const restockItems = filtered.filter(i => i.source === 'auto' || i.source === 'manual');
    const recipeItems = filtered.filter(i => i.source === 'recipe');

    renderSection('shopping-auto-list', 'shopping-auto-title', restockItems, i18n.t('shopping_section_restock'), userId, shopsMap);
    renderSection('shopping-recipe-list', 'shopping-recipe-title', recipeItems, i18n.t('shopping_section_recipe'), userId, shopsMap);

    document.getElementById('shopping-recipe-section').style.display = recipeItems.length > 0 ? 'block' : 'none';

    updateBadge(list);

    const emptyAll = filtered.length === 0;
    document.getElementById('shopping-empty').style.display = emptyAll ? 'flex' : 'none';
    document.getElementById('shopping-empty-title').textContent = i18n.t('shopping_empty');
    document.getElementById('shopping-empty-sub').textContent = i18n.t('shopping_empty_sub');
  }

  function filterByShop(items) {
    if (currentShopFilter === 'all') return items;
    if (currentShopFilter === ShopModel.UNDEFINED_KEY) {
      return items.filter(item => !item.shopId);
    }
    return items.filter(item => item.shopId === currentShopFilter);
  }

  function renderStoreFilters(shops) {
    const filters = document.getElementById('shopping-store-filters');
    const sortedShops = [...shops].sort((a, b) =>
      String(a.name || '').localeCompare(String(b.name || ''), i18n.lang || 'fr', { sensitivity: 'base' })
    );
    const validFilters = new Set(['all', ShopModel.UNDEFINED_KEY, ...sortedShops.map(shop => shop.id)]);
    if (!validFilters.has(currentShopFilter)) {
      currentShopFilter = 'all';
    }

    const chips = [
      `<button class="filter-chip ${currentShopFilter === 'all' ? 'active' : ''}" data-shop-filter="all">${i18n.t('shopping_filter_all_stores')}</button>`,
      ...sortedShops.map(shop =>
        `<button class="filter-chip ${currentShopFilter === shop.id ? 'active' : ''}" data-shop-filter="${shop.id}">🏬 ${escHtml(shop.name)}</button>`
      ),
      `<button class="filter-chip ${currentShopFilter === ShopModel.UNDEFINED_KEY ? 'active' : ''}" data-shop-filter="${ShopModel.UNDEFINED_KEY}">🏬 ${i18n.t('shopping_store_undefined')}</button>`,
    ];

    filters.innerHTML = chips.join('');
    filters.querySelectorAll('[data-shop-filter]').forEach(btn => {
      btn.addEventListener('click', () => {
        currentShopFilter = btn.dataset.shopFilter;
        render();
      });
    });
  }

  function getShopGroup(item, shopsMap) {
    const shop = item.shopId ? shopsMap.get(item.shopId) : null;
    return {
      key: item.shopId || ShopModel.UNDEFINED_KEY,
      label: shop?.name || i18n.t('shopping_store_undefined'),
    };
  }

  function renderSection(listId, titleId, items, title, userId, shopsMap) {
    document.getElementById(titleId).textContent = title;
    const container = document.getElementById(listId);
    if (items.length === 0) { container.innerHTML = ''; return; }

    const grouped = {};
    for (const item of items) {
      const group = getShopGroup(item, shopsMap);
      if (!grouped[group.key]) grouped[group.key] = { label: group.label, items: [] };
      grouped[group.key].items.push(item);
    }

    const sortedGroupKeys = Object.keys(grouped).sort((a, b) => {
      if (a === ShopModel.UNDEFINED_KEY) return 1;
      if (b === ShopModel.UNDEFINED_KEY) return -1;
      return grouped[a].label.localeCompare(grouped[b].label, i18n.lang || 'fr', { sensitivity: 'base' });
    });

    const html = sortedGroupKeys.map(groupKey => {
      const group = grouped[groupKey];
      const itemsHtml = group.items.map(item => {
        const quantityLabel = item.quantity !== '' && item.quantity !== null
          ? `<div class="shopping-item-qty">${item.quantity} ${item.unit || ''}</div>`
          : `<div class="shopping-item-qty">0 ${item.unit || ''}</div>`;
        const sourceBadge = item.source === 'manual'
          ? `<span class="shopping-source-badge">${i18n.t('shopping_badge_new')}</span>`
          : '';
        return `
        <div class="shopping-item ${item.checked ? 'checked' : ''}" data-id="${item.id}">
          <div class="shopping-check ${item.checked ? 'checked' : ''}" data-id="${item.id}">
            ${item.checked ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>' : ''}
          </div>
          <div style="flex:1;min-width:0">
            <div class="shopping-item-name ${item.checked ? 'line-through' : ''}">${escHtml(item.name)}${sourceBadge}</div>
            <div class="shopping-qty-controls">
              <button class="shopping-qty-btn" type="button" data-id="${item.id}" data-action="decrement">−</button>
              ${quantityLabel}
              <button class="shopping-qty-btn" type="button" data-id="${item.id}" data-action="increment">+</button>
            </div>
          </div>
          <button class="btn-sm btn-secondary shopping-to-storage ${isAddingToStorage ? 'is-busy' : ''}" data-id="${item.id}" style="font-size:11px;padding:5px 8px" ${isAddingToStorage ? 'disabled' : ''}>
            🏪 ${i18n.t('shopping_add_to_storage')}
          </button>
          <button class="shopping-btn-remove" data-id="${item.id}">✕</button>
        </div>`;
      }).join('');

      return `
        <div class="shopping-shop-group">
          <div class="shopping-shop-group-title">🏬 ${escHtml(group.label)}</div>
          <div class="shopping-shop-group-list">${itemsHtml}</div>
        </div>
      `;
    }).join('');

    container.innerHTML = html;

    container.querySelectorAll('.shopping-check').forEach(btn => {
      btn.addEventListener('click', async () => {
        await ShoppingModel.toggle(btn.dataset.id);
        render();
      });
    });
    container.querySelectorAll('.shopping-qty-btn').forEach(btn => {
      btn.addEventListener('click', async event => {
        event.stopPropagation();
        const delta = btn.dataset.action === 'increment' ? 1 : -1;
        await ShoppingModel.changeQuantity(btn.dataset.id, delta);
        render();
      });
    });
    container.querySelectorAll('.shopping-to-storage').forEach(btn => {
      btn.addEventListener('click', () => addToStorage(btn.dataset.id, userId));
    });
    container.querySelectorAll('.shopping-btn-remove').forEach(btn => {
      btn.addEventListener('click', async () => {
        await ShoppingModel.remove(btn.dataset.id);
        render();
      });
    });
  }

  async function getOrCreateTemporaryStorage(userId) {
    const existing = await StorageModel.getByName(userId, 'A ranger');
    if (existing) return existing;
    return StorageModel.create(userId, {
      name: 'A ranger',
      icon: '📦',
      type: 'dry',
      isDefault: false,
    });
  }

  async function addToStorage(shoppingId, userId) {
    if (isAddingToStorage) return;
    isAddingToStorage = true;
    setStorageButtonsBusy(true);

    try {
      const all = await ShoppingModel.getAll(userId);
      const selected = all.filter(i => i.checked);
      const idsToProcess = selected.length > 1
        ? selected.map(i => i.id)
        : [shoppingId];

      const temporaryStorage = await getOrCreateTemporaryStorage(userId);
      await ShoppingModel.markManyAsBought(idsToProcess, userId, temporaryStorage.id);
      await ShoppingModel.syncAutoRestock(userId);

      const count = idsToProcess.length;
      Toast.success(
        count > 1
          ? `✓ ${count} ${(i18n.lang === 'fr' ? 'articles ajoutés au rangement' : 'items added to storage')}`
          : '✓ ' + (i18n.lang === 'fr' ? 'Ajouté au rangement' : 'Added to storage')
      );
      await render();
      await ExpiringView.render();
      await AllItemsView.render();
    } finally {
      isAddingToStorage = false;
      setStorageButtonsBusy(false);
    }
  }

  function setStorageButtonsBusy(isBusy) {
    document.querySelectorAll('.shopping-to-storage').forEach(btn => {
      btn.disabled = isBusy;
      btn.classList.toggle('is-busy', isBusy);
    });
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

  let initialized = false;

  function init() {
    if (initialized) return;
    initialized = true;

    const shoppingInput = document.getElementById('shopping-input');
    shoppingInput.addEventListener('input', e => {
      searchQuery = e.target.value || '';
      render();
    });
    shoppingInput.addEventListener('keydown', e => {
      if (e.key === 'Enter') e.preventDefault();
    });
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

  return { init, render, addManual };
})();
