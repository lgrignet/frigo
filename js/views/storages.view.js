// ============================================================
//  storages.view.js — Onglet "Rangements" + "Magasins"
// ============================================================

const StoragesView = (() => {

  async function render() {
    const userId = Auth.getCurrentUserId();
    const [storages, shops] = await Promise.all([
      StorageModel.getAll(userId),
      ShopModel.getAll(userId),
    ]);

    document.getElementById('storages-view-title').textContent = i18n.t('storages_title');
    document.getElementById('btn-add-storage').innerHTML = `+ ${i18n.t('add')}`;
    document.getElementById('btn-add-storage').title = i18n.t('storages_add');
    document.getElementById('shops-view-title').textContent = i18n.t('shops_title');
    document.getElementById('btn-add-shop').innerHTML = `+ ${i18n.t('add')}`;
    document.getElementById('btn-add-shop').title = i18n.t('shops_add');
    document.getElementById('shops-empty-title').textContent = i18n.t('shops_empty');

    await renderStoragesGrid(storages, userId);
    await renderShopsGrid(shops, userId);
  }

  async function renderStoragesGrid(storages, userId) {
    const grid = document.getElementById('storages-grid');
    const empty = document.getElementById('storages-empty');

    if (storages.length === 0) {
      grid.innerHTML = '';
      empty.style.display = 'flex';
      return;
    }
    empty.style.display = 'none';

    const counts = {};
    for (const storage of storages) {
      counts[storage.id] = await StorageModel.getItemCount(storage.id, userId);
    }

    grid.innerHTML = storages.map(storage => `
      <div class="storage-card card" data-id="${storage.id}">
        <div class="storage-icon">${storage.icon}</div>
        <div class="storage-name">${escHtml(storage.name)}</div>
        <div class="storage-count">${counts[storage.id]} ${i18n.t('items_count')}</div>
        <div class="storage-type-badge">${i18n.storageTypes().find(t => t.id === storage.type)?.label || storage.type}</div>
      </div>`).join('');

    grid.querySelectorAll('.storage-card').forEach(card => {
      card.addEventListener('click', () => openStorageDetail(card.dataset.id, storages));
    });
  }

  async function renderShopsGrid(shops, userId) {
    const grid = document.getElementById('shops-grid');
    const empty = document.getElementById('shops-empty');

    if (shops.length === 0) {
      grid.innerHTML = '';
      empty.style.display = 'flex';
      return;
    }
    empty.style.display = 'none';

    const counts = {};
    for (const shop of shops) {
      counts[shop.id] = await ShopModel.getItemCount(shop.id, userId);
    }

    const sortedShops = [...shops].sort((a, b) =>
      String(a.name || '').localeCompare(String(b.name || ''), i18n.lang || 'fr', { sensitivity: 'base' })
    );

    grid.innerHTML = sortedShops.map(shop => `
      <div class="storage-card card" data-shop-id="${shop.id}">
        <div class="storage-icon">🏬</div>
        <div class="storage-name">${escHtml(shop.name)}</div>
        <div class="storage-count">${counts[shop.id]} ${i18n.t('items_count')}</div>
      </div>`).join('');

    grid.querySelectorAll('.storage-card[data-shop-id]').forEach(card => {
      card.addEventListener('click', () => openShopDetail(card.dataset.shopId, sortedShops));
    });
  }

  async function openStorageDetail(id, storages) {
    const storage = storages.find(s => s.id === id);
    if (!storage) return;

    const userId = Auth.getCurrentUserId();
    const [items, shops] = await Promise.all([
      ItemModel.getByStorage(userId, id),
      ShopModel.getAll(userId),
    ]);

    document.getElementById('storage-detail-title').textContent = `${storage.icon} ${storage.name}`;
    document.getElementById('btn-storage-edit').innerHTML = `✏️ ${i18n.t('edit')}`;
    document.getElementById('btn-storage-close').textContent = i18n.t('close');
    document.getElementById('btn-storage-edit').onclick = () => {
      Modal.close('modal-storage-detail');
      openStorageForm(id);
    };
    document.getElementById('btn-storage-delete').onclick = async () => {
      try {
        await StorageModel.remove(id, userId);
        Modal.close('modal-storage-detail');
        await render();
        Toast.success(i18n.t('storage_deleted'));
      } catch (e) {
        if (e.message === 'STORAGE_NOT_EMPTY') Toast.error(i18n.t('storage_delete_error'));
      }
    };

    const itemsList = document.getElementById('storage-detail-items');
    if (items.length === 0) {
      itemsList.innerHTML = `<div class="shopping-empty-section" style="padding:20px;text-align:center;color:var(--text-muted)">${i18n.t('all_empty')}</div>`;
    } else {
      itemsList.innerHTML = items.map(item => ItemCard.render(item, storages, shops)).join('');
      ItemCard.bindEvents(itemsList, itemId => {
        Modal.close('modal-storage-detail');
        ItemForm.open(itemId, null, async () => { await render(); Modal.open('modal-storage-detail'); });
      }, async (itemId, action) => {
        await ItemCard.changeQuantity(itemId, action);
        await render();
        await openStorageDetail(id, storages);
      });
    }
    Modal.open('modal-storage-detail');
  }

  async function openShopDetail(id, shops) {
    const shop = shops.find(s => s.id === id);
    if (!shop) return;

    const userId = Auth.getCurrentUserId();
    const [items, storages] = await Promise.all([
      ItemModel.getByShop(userId, id),
      StorageModel.getAll(userId),
    ]);

    document.getElementById('shop-detail-title').textContent = `🏬 ${shop.name}`;
    document.getElementById('btn-shop-edit').innerHTML = `✏️ ${i18n.t('edit')}`;
    document.getElementById('btn-shop-close').textContent = i18n.t('close');
    document.getElementById('btn-shop-edit').onclick = () => {
      Modal.close('modal-shop-detail');
      openShopForm(id);
    };
    document.getElementById('btn-shop-delete').onclick = async () => {
      await ShopModel.remove(id, userId);
      Modal.close('modal-shop-detail');
      await ShoppingModel.syncAutoRestock(userId);
      await render();
      await ShoppingView.render();
      Toast.success(i18n.t('shop_deleted'));
    };

    const itemsList = document.getElementById('shop-detail-items');
    if (items.length === 0) {
      itemsList.innerHTML = `<div class="shopping-empty-section" style="padding:20px;text-align:center;color:var(--text-muted)">${i18n.t('all_empty')}</div>`;
    } else {
      itemsList.innerHTML = items.map(item => ItemCard.render(item, storages, shops)).join('');
      ItemCard.bindEvents(itemsList, itemId => {
        Modal.close('modal-shop-detail');
        ItemForm.open(itemId, null, async () => { await render(); Modal.open('modal-shop-detail'); });
      }, async (itemId, action) => {
        await ItemCard.changeQuantity(itemId, action);
        await render();
        await openShopDetail(id, shops);
      });
    }
    Modal.open('modal-shop-detail');
  }

  function openStorageForm(id = null) {
    document.getElementById('storage-form-title').textContent = i18n.t(id ? 'edit' : 'storages_add');
    document.getElementById('storage-name-input').value = '';
    document.getElementById('storage-icon-select').value = '📦';
    document.getElementById('label-storage-name').textContent = i18n.t('storage_name');
    document.getElementById('label-storage-icon').textContent = i18n.t('storage_icon');
    document.getElementById('label-storage-type').textContent = i18n.t('storage_type');

    const typeSel = document.getElementById('storage-type-select');
    typeSel.innerHTML = i18n.storageTypes().map(type =>
      `<option value="${type.id}">${type.label}</option>`
    ).join('');

    if (id) {
      StorageModel.getById(id).then(storage => {
        if (!storage) return;
        document.getElementById('storage-name-input').value = storage.name;
        document.getElementById('storage-icon-select').value = storage.icon;
        document.getElementById('storage-type-select').value = storage.type;
      });
    }

    document.getElementById('btn-storage-form-save').textContent = i18n.t('save');
    document.getElementById('btn-storage-form-cancel').textContent = i18n.t('cancel');
    document.getElementById('btn-storage-form-save').onclick = async () => {
      const name = sanitizeInputValue(document.getElementById('storage-name-input').value).trim();
      if (!name) { Toast.error(i18n.t('name_required')); return; }

      const data = {
        name,
        icon: document.getElementById('storage-icon-select').value,
        type: document.getElementById('storage-type-select').value,
      };
      const userId = Auth.getCurrentUserId();
      if (id) await StorageModel.update(id, data);
      else await StorageModel.create(userId, data);

      Modal.close('modal-storage-form');
      await render();
      Toast.success(i18n.t('storage_saved'));
    };

    document.getElementById('btn-storage-form-cancel').onclick = () => Modal.close('modal-storage-form');
    Modal.open('modal-storage-form');
  }

  function openShopForm(id = null) {
    document.getElementById('shop-form-title').textContent = i18n.t(id ? 'edit' : 'shops_add');
    document.getElementById('label-shop-name').textContent = i18n.t('shop_name');
    document.getElementById('shop-name-input').value = '';

    if (id) {
      ShopModel.getById(id).then(shop => {
        if (!shop) return;
        document.getElementById('shop-name-input').value = shop.name;
      });
    }

    document.getElementById('btn-shop-form-save').textContent = i18n.t('save');
    document.getElementById('btn-shop-form-cancel').textContent = i18n.t('cancel');
    document.getElementById('btn-shop-form-save').onclick = async () => {
      const name = sanitizeInputValue(document.getElementById('shop-name-input').value).trim();
      if (!name) {
        Toast.error(i18n.t('shop_name_required'));
        return;
      }

      const userId = Auth.getCurrentUserId();
      if (id) {
        await ShopModel.update(id, { name });
      } else {
        await ShopModel.create(userId, { name });
      }

      await ShoppingModel.syncAutoRestock(userId);
      Modal.close('modal-shop-form');
      await render();
      await ShoppingView.render();
      Toast.success(i18n.t('shop_saved'));
    };

    document.getElementById('btn-shop-form-cancel').onclick = () => Modal.close('modal-shop-form');
    Modal.open('modal-shop-form');
  }

  function init() {
    document.getElementById('btn-add-storage').addEventListener('click', () => openStorageForm());
    document.getElementById('btn-add-shop').addEventListener('click', () => openShopForm());
    render();
  }

  return { init, render };
})();
