// ============================================================
//  storages.view.js — Onglet "Rangements"
// ============================================================

const StoragesView = (() => {

  async function render() {
    const userId = Auth.getCurrentUserId();
    const storages = await StorageModel.getAll(userId);

    document.getElementById('storages-view-title').textContent = i18n.t('storages_title');
    document.getElementById('btn-add-storage').title = i18n.t('storages_add');

    const grid = document.getElementById('storages-grid');
    const empty = document.getElementById('storages-empty');

    if (storages.length === 0) {
      grid.innerHTML = '';
      empty.style.display = 'flex';
      return;
    }
    empty.style.display = 'none';

    // Count items per storage
    const counts = {};
    for (const s of storages) {
      counts[s.id] = await StorageModel.getItemCount(s.id, userId);
    }

    grid.innerHTML = storages.map(s => `
      <div class="storage-card card" data-id="${s.id}">
        <div class="storage-icon">${s.icon}</div>
        <div class="storage-name">${escHtml(s.name)}</div>
        <div class="storage-count">${counts[s.id]} ${i18n.t('items_count') || 'produit(s)'}</div>
        <div class="storage-type-badge">${i18n.storageTypes().find(t => t.id === s.type)?.label || s.type}</div>
      </div>`).join('');

    grid.querySelectorAll('.storage-card').forEach(card => {
      card.addEventListener('click', () => openStorageDetail(card.dataset.id, storages));
    });
  }

  async function openStorageDetail(id, storages) {
    const storage = storages.find(s => s.id === id);
    if (!storage) return;
    const userId = Auth.getCurrentUserId();
    const items  = await ItemModel.getByStorage(userId, id);

    document.getElementById('storage-detail-title').textContent = `${storage.icon} ${storage.name}`;
    document.getElementById('btn-storage-edit').onclick = () => {
      Modal.close('modal-storage-detail');
      openStorageForm(id);
    };
    document.getElementById('btn-storage-delete').onclick = async () => {
      try {
        await StorageModel.remove(id, userId);
        Modal.close('modal-storage-detail');
        render();
        Toast.success(i18n.lang === 'fr' ? 'Rangement supprimé' : 'Storage deleted');
      } catch (e) {
        if (e.message === 'STORAGE_NOT_EMPTY') Toast.error(i18n.t('storage_delete_error'));
      }
    };

    const itemsList = document.getElementById('storage-detail-items');
    if (items.length === 0) {
      itemsList.innerHTML = `<div class="shopping-empty-section" style="padding:20px;text-align:center;color:var(--text-muted)">${i18n.lang === 'fr' ? 'Aucun produit' : 'No items'}</div>`;
    } else {
      itemsList.innerHTML = items.map(item => ItemCard.render(item, storages)).join('');
      ItemCard.bindEvents(itemsList, itemId => {
        Modal.close('modal-storage-detail');
        ItemForm.open(itemId, null, () => { render(); Modal.open('modal-storage-detail'); });
      });
    }
    Modal.open('modal-storage-detail');
  }

  function openStorageForm(id = null) {
    const storage = id ? null : null;
    document.getElementById('storage-form-title').textContent = i18n.t(id ? 'edit' : 'storages_add');
    document.getElementById('storage-name-input').value = '';
    document.getElementById('storage-icon-select').value = '📦';
    document.getElementById('label-storage-name').textContent = i18n.t('storage_name');
    document.getElementById('label-storage-icon').textContent = i18n.t('storage_icon');
    document.getElementById('label-storage-type').textContent = i18n.t('storage_type');

    const typeSel = document.getElementById('storage-type-select');
    typeSel.innerHTML = i18n.storageTypes().map(t =>
      `<option value="${t.id}">${t.label}</option>`
    ).join('');

    if (id) {
      StorageModel.getById(id).then(s => {
        if (!s) return;
        document.getElementById('storage-name-input').value = s.name;
        document.getElementById('storage-icon-select').value = s.icon;
        document.getElementById('storage-type-select').value = s.type;
      });
    }

    document.getElementById('btn-storage-form-save').onclick = async () => {
      const name = document.getElementById('storage-name-input').value.trim();
      if (!name) { Toast.error(i18n.lang === 'fr' ? 'Nom requis' : 'Name required'); return; }
      const data = {
        name,
        icon: document.getElementById('storage-icon-select').value,
        type: document.getElementById('storage-type-select').value,
      };
      const userId = Auth.getCurrentUserId();
      if (id) await StorageModel.update(id, data);
      else    await StorageModel.create(userId, data);
      Modal.close('modal-storage-form');
      render();
      Toast.success(i18n.lang === 'fr' ? 'Rangement enregistré' : 'Storage saved');
    };
    document.getElementById('btn-storage-form-cancel').onclick = () => Modal.close('modal-storage-form');
    Modal.open('modal-storage-form');
  }

  function init() {
    document.getElementById('btn-add-storage').addEventListener('click', () => openStorageForm());
    render();
  }

  return { init, render };
})();
