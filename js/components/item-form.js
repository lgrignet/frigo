// ============================================================
//  item-form.js — Formulaire ajout / modification d'item
// ============================================================

const ItemForm = (() => {
  let editingId = null;
  let capturedPhoto = null;
  let onSaveCallback = null;
  let addToShoppingList = false;

  function open(id = null, defaultStorageId = null, callback = null, forShopping = false) {
    editingId = id;
    capturedPhoto = null;
    onSaveCallback = callback;
    addToShoppingList = forShopping;
    const item = id ? null : null; // will fetch async
    populateForm(id, defaultStorageId);
    Modal.open('modal-item-form');
  }

  async function populateForm(id, defaultStorageId) {
    const userId = Auth.getCurrentUserId();
    const item = id ? await ItemModel.getById(id) : null;
    const storages = await StorageModel.getAll(userId);
    const prefs = await PrefsModel.get(userId);

    // Title
    document.getElementById('form-title').textContent = i18n.t(id ? 'item_edit' : 'item_add');

    // Fields
    document.getElementById('field-name').value = item?.name || '';
    document.getElementById('field-qty').value = item?.quantity ?? 1;
    document.getElementById('field-notes').value = item?.notes || '';
    document.getElementById('field-expiry').value = item?.expiryDate || '';
    document.getElementById('field-restock').value = item?.restockThreshold ?? 0;

    // Photo
    capturedPhoto = item?.photo || null;
    updatePhotoPreview();

    // Unit select
    const unitSel = document.getElementById('field-unit');
    const units = i18n.units();
    unitSel.innerHTML = units.map(u =>
      `<option value="${u}" ${(item?.unit || prefs.defaultUnit) === u ? 'selected' : ''}>${u}</option>`
    ).join('');

    // Storage select
    const storageSel = document.getElementById('field-storage');
    const defaultSId = item?.storageId || defaultStorageId || prefs.defaultStorageId || storages[0]?.id;
    storageSel.innerHTML = storages.map(s =>
      `<option value="${s.id}" ${defaultSId === s.id ? 'selected' : ''}>${s.icon} ${s.name}</option>`
    ).join('');

    // Labels
    document.getElementById('form-title').textContent = i18n.t(id ? 'item_edit' : 'item_add');
    document.getElementById('label-name').textContent = i18n.t('item_name');
    document.getElementById('label-qty').textContent = i18n.t('item_quantity');
    document.getElementById('label-unit').textContent = i18n.t('item_unit');
    document.getElementById('label-expiry').textContent = i18n.t('item_expiry');
    document.getElementById('label-storage').textContent = i18n.t('item_storage');
    document.getElementById('label-restock').textContent = i18n.t('item_restock');
    document.getElementById('label-notes').textContent = i18n.t('item_notes');
    document.getElementById('label-photo').textContent = i18n.t('item_photo');
    document.getElementById('field-notes').placeholder = i18n.t('item_notes_placeholder');
    document.getElementById('btn-form-save').textContent = i18n.t('item_save');
    document.getElementById('btn-form-cancel').textContent = i18n.t('item_cancel');
    document.getElementById('btn-take-photo').textContent = i18n.t('take_photo');
    document.getElementById('btn-choose-photo').textContent = i18n.t('choose_photo');
    document.getElementById('btn-remove-photo').textContent = i18n.t('remove_photo');
    document.getElementById('label-restock-help').textContent = i18n.t('item_restock_help');

    // Delete button
    const btnDelete = document.getElementById('btn-form-delete');
    if (id) {
      btnDelete.style.display = 'flex';
      btnDelete.textContent = i18n.t('item_delete');
    } else {
      btnDelete.style.display = 'none';
    }
  }

  function updatePhotoPreview() {
    const preview = document.getElementById('photo-preview');
    const btnRemove = document.getElementById('btn-remove-photo');
    if (capturedPhoto) {
      preview.style.backgroundImage = `url('${capturedPhoto}')`;
      preview.classList.add('has-photo');
      preview.textContent = '';
      btnRemove.style.display = 'flex';
    } else {
      preview.style.backgroundImage = '';
      preview.classList.remove('has-photo');
      preview.textContent = '📷';
      btnRemove.style.display = 'none';
    }
  }

  function handlePhotoFile(file) {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = e => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const maxW = 400;
        const ratio = Math.min(maxW / img.width, maxW / img.height, 1);
        canvas.width = img.width * ratio;
        canvas.height = img.height * ratio;
        canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height);
        capturedPhoto = canvas.toDataURL('image/jpeg', 0.78);
        updatePhotoPreview();
      };
      img.src = e.target.result;
    };
    reader.readAsDataURL(file);
  }

  async function save() {
    const userId = Auth.getCurrentUserId();
    const name = document.getElementById('field-name').value.trim();
    if (!name) {
      document.getElementById('field-name').classList.add('error');
      document.getElementById('field-name').focus();
      Toast.error(i18n.t('item_name_required'));
      return;
    }
    document.getElementById('field-name').classList.remove('error');

    const expiryRaw = document.getElementById('field-expiry').value || null;
    const prefs = await PrefsModel.get(userId);
    const data = {
      name,
      quantity: parseFloat(document.getElementById('field-qty').value) || 0,
      unit: document.getElementById('field-unit').value,
      expiryDate: expiryRaw ? parseExpiryDate(expiryRaw, prefs.dateFormat || 'european') : null,
      storageId: document.getElementById('field-storage').value,
      restockThreshold: parseInt(document.getElementById('field-restock').value) || 0,
      notes: document.getElementById('field-notes').value.trim(),
      photo: capturedPhoto,
    };

    if (editingId) {
      await ItemModel.update(editingId, data);
    } else if (addToShoppingList) {
      await ShoppingModel.add(userId, { name, quantity: data.quantity, unit: data.unit, source: 'manual' });
    } else {
      await ItemModel.create(userId, data);
    }

    // Sync shopping list
    if (!addToShoppingList) {
      await ShoppingModel.syncAutoRestock(userId);
    }
    if (typeof App !== 'undefined' && typeof App.updateBadges === 'function') {
      await App.updateBadges();
    }

    Toast.success(i18n.t('item_saved'));
    Modal.close('modal-item-form');
    if (onSaveCallback) onSaveCallback();
  }

  async function deleteItem() {
    if (!editingId) return;
    Modal.close('modal-item-form');
    // Show confirm
    document.getElementById('confirm-title').textContent = i18n.t('item_delete_confirm');
    document.getElementById('confirm-sub').textContent = i18n.t('item_delete_sub');
    document.getElementById('btn-confirm-yes').textContent = i18n.t('yes_delete');
    document.getElementById('btn-confirm-no').textContent = i18n.t('cancel');
    document.getElementById('btn-confirm-yes').onclick = async () => {
      await ItemModel.remove(editingId);
      await ShoppingModel.syncAutoRestock(Auth.getCurrentUserId());
      if (typeof App !== 'undefined' && typeof App.updateBadges === 'function') {
        await App.updateBadges();
      }
      Modal.close('modal-confirm');
      Toast.success(i18n.t('item_deleted'));
      if (onSaveCallback) onSaveCallback();
    };
    document.getElementById('btn-confirm-no').onclick = () => Modal.close('modal-confirm');
    Modal.open('modal-confirm');
  }

  function init() {
    document.getElementById('btn-form-save').addEventListener('click', save);
    document.getElementById('btn-form-cancel').addEventListener('click', () => Modal.close('modal-item-form'));
    document.getElementById('btn-form-delete').addEventListener('click', deleteItem);
    document.getElementById('item-form').addEventListener('submit', e => { e.preventDefault(); save(); });

    document.getElementById('btn-take-photo').addEventListener('click', () => {
      const inp = document.getElementById('photo-input');
      inp.setAttribute('capture', 'environment');
      inp.click();
    });
    document.getElementById('btn-choose-photo').addEventListener('click', () => {
      const inp = document.getElementById('photo-input');
      inp.removeAttribute('capture');
      inp.click();
    });
    document.getElementById('btn-remove-photo').addEventListener('click', () => {
      capturedPhoto = null;
      document.getElementById('photo-input').value = '';
      updatePhotoPreview();
    });
    document.getElementById('photo-input').addEventListener('change', e => handlePhotoFile(e.target.files[0]));
  }

  return { open, init };
})();
