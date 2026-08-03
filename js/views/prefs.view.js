// ============================================================
//  prefs.view.js — Écran Préférences
// ============================================================

const PrefsView = (() => {

  async function render() {
    const userId = Auth.getCurrentUserId();
    const prefs = await PrefsModel.get(userId);
    const storages = await StorageModel.getAll(userId);
    const user = await DB.getOne('users', userId);

    document.getElementById('prefs-fullscreen-title').textContent = i18n.t('prefs_title');

    // ── Général ──
    document.getElementById('pref-section-general').textContent = i18n.t('pref_section_general');

    // Expiry days slider
    document.getElementById('pref-expiry-label').textContent = i18n.t('pref_expiry_days');
    const slider = document.getElementById('pref-expiry-slider');
    const sliderVal = document.getElementById('pref-expiry-value');
    slider.value = prefs.expiryWarningDays;
    sliderVal.textContent = prefs.expiryWarningDays + ' ' + i18n.t('expiring_days_unit');

    // Theme
    document.getElementById('pref-theme-label').textContent = i18n.t('pref_theme');
    const themeBtn = document.getElementById('btn-pref-theme');
    themeBtn.textContent = prefs.theme === 'dark' ? i18n.t('pref_theme_light') : i18n.t('pref_theme_dark');
    themeBtn.dataset.current = prefs.theme;

    // Language
    document.getElementById('pref-lang-label').textContent = i18n.t('pref_lang');
    document.getElementById('pref-lang-select').value = i18n.lang;

    document.getElementById('pref-date-format-label').textContent = i18n.t('pref_date_format');
    document.getElementById('btn-pref-date-european').classList.toggle('active', prefs.dateFormat === 'european');
    document.getElementById('btn-pref-date-american').classList.toggle('active', prefs.dateFormat === 'american');
    document.getElementById('btn-pref-date-european').textContent = i18n.t('pref_date_format_european');
    document.getElementById('btn-pref-date-american').textContent = i18n.t('pref_date_format_american');

    // Default unit
    document.getElementById('pref-unit-label').textContent = i18n.t('pref_default_unit');
    const unitSel = document.getElementById('pref-unit-select');
    unitSel.innerHTML = i18n.units().map(u =>
      `<option value="${u}" ${prefs.defaultUnit === u ? 'selected' : ''}>${u}</option>`
    ).join('');

    // Default storage
    document.getElementById('pref-storage-label').textContent = i18n.t('pref_default_storage');
    const storageSel = document.getElementById('pref-storage-select');
    storageSel.innerHTML = storages.map(s =>
      `<option value="${s.id}" ${prefs.defaultStorageId === s.id ? 'selected' : ''}>${s.icon} ${escHtml(s.name)}</option>`
    ).join('');

    // Notifications toggle
    document.getElementById('pref-notif-label').textContent = i18n.t('pref_notifications');
    document.getElementById('pref-notif-sub').textContent = i18n.t('pref_notif_time');
    const notifToggle = document.getElementById('pref-notif-toggle');
    notifToggle.classList.toggle('on', prefs.notificationsEnabled);
    document.getElementById('pref-notif-time').value = prefs.notificationTime || '08:00';
    document.getElementById('pref-notif-time-row').style.display = prefs.notificationsEnabled ? 'flex' : 'none';

    // ── Account ──
    document.getElementById('pref-section-account').textContent = i18n.t('pref_section_account');
    document.getElementById('pref-email-display').textContent = Auth.getCurrentEmail();
    document.getElementById('pref-logout-btn').textContent = i18n.t('logout');
    document.getElementById('pref-account-label').textContent = i18n.t('account');

    // ── Data ──
    document.getElementById('pref-section-data').textContent = i18n.t('pref_section_data');
    document.getElementById('btn-pref-export').textContent = i18n.t('pref_export');
    document.getElementById('btn-pref-reset').textContent = i18n.t('pref_reset');

    // Sync GUID
    const guidInput = document.getElementById('pref-sync-guid');
    if (guidInput) {
      guidInput.value = Auth.getCurrentSyncChannel() || '';
    }
  }

  function open() {
    render();
    Modal.openFullscreen('modal-prefs');
  }

  function close() {
    Modal.closeFullscreen('modal-prefs');
  }

  function init() {
    document.getElementById('btn-prefs-close').addEventListener('click', close);

    // Slider
    document.getElementById('pref-expiry-slider').addEventListener('input', async e => {
      const val = parseInt(e.target.value);
      document.getElementById('pref-expiry-value').textContent = val + ' ' + i18n.t('expiring_days_unit');
      await PrefsModel.set(Auth.getCurrentUserId(), { expiryWarningDays: val });
      ExpiringView.render();
    });

    // Theme
    document.getElementById('btn-pref-theme').addEventListener('click', async () => {
      const userId = Auth.getCurrentUserId();
      const prefs = await PrefsModel.get(userId);
      const newTheme = prefs.theme === 'dark' ? 'light' : 'dark';
      await PrefsModel.set(userId, { theme: newTheme });
      await PrefsModel.applyTheme(newTheme);
      render();
    });

    // Language
    document.getElementById('pref-lang-select').addEventListener('change', async e => {
      const lang = e.target.value;
      i18n.setLang(lang);
      await PrefsModel.set(Auth.getCurrentUserId(), { lang });
      App.applyLanguage();
      render();
    });

    document.getElementById('btn-pref-date-european').addEventListener('click', async () => {
      const userId = Auth.getCurrentUserId();
      await PrefsModel.set(userId, { dateFormat: 'european' });
      App.dateFormat = 'european';
      render();
      if (document.getElementById('view-expiring').classList.contains('active')) await ExpiringView.render();
      if (document.getElementById('view-all').classList.contains('active')) await AllItemsView.render();
    });
    document.getElementById('btn-pref-date-american').addEventListener('click', async () => {
      const userId = Auth.getCurrentUserId();
      await PrefsModel.set(userId, { dateFormat: 'american' });
      App.dateFormat = 'american';
      render();
      if (document.getElementById('view-expiring').classList.contains('active')) await ExpiringView.render();
      if (document.getElementById('view-all').classList.contains('active')) await AllItemsView.render();
    });

    // Unit
    document.getElementById('pref-unit-select').addEventListener('change', async e => {
      await PrefsModel.set(Auth.getCurrentUserId(), { defaultUnit: e.target.value });
    });

    // Storage
    document.getElementById('pref-storage-select').addEventListener('change', async e => {
      await PrefsModel.set(Auth.getCurrentUserId(), { defaultStorageId: e.target.value });
    });

    // Notifications
    document.getElementById('pref-notif-toggle').addEventListener('click', async () => {
      const userId = Auth.getCurrentUserId();
      const prefs = await PrefsModel.get(userId);
      const newVal = !prefs.notificationsEnabled;
      if (newVal && Notification.permission === 'default') {
        const perm = await Notification.requestPermission();
        if (perm !== 'granted') { Toast.error(i18n.lang === 'fr' ? 'Permission refusée' : 'Permission denied'); return; }
      }
      await PrefsModel.set(userId, { notificationsEnabled: newVal });
      render();
    });
    document.getElementById('pref-notif-time').addEventListener('change', async e => {
      await PrefsModel.set(Auth.getCurrentUserId(), { notificationTime: e.target.value });
    });

    // Logout
    document.getElementById('pref-logout-btn').addEventListener('click', () => {
      Auth.logout();
      close();
      App.showLogin();
    });

    // Export
    document.getElementById('btn-pref-export').addEventListener('click', async () => {
      const userId = Auth.getCurrentUserId();
      const data = await DB.exportUserData(userId);
      const json = JSON.stringify(data, null, 2);
      const blob = new Blob([json], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = `mystockmanager-export-${new Date().toISOString().slice(0,10)}.json`;
      a.click();
      URL.revokeObjectURL(url);
      Toast.success(i18n.t('pref_export_done'));
    });

    // Reset
    document.getElementById('btn-pref-reset').addEventListener('click', () => {
      document.getElementById('confirm-title').textContent = i18n.t('pref_reset');
      document.getElementById('confirm-sub').textContent = i18n.t('pref_reset_confirm');
      document.getElementById('btn-confirm-yes').textContent = i18n.t('yes_delete');
      document.getElementById('btn-confirm-no').textContent = i18n.t('cancel');

      document.getElementById('btn-confirm-yes').onclick = async () => {
        const userId = Auth.getCurrentUserId();
        if (userId) {
          await DB.deleteAllUserData(userId);
          Modal.close('modal-confirm');
          close();
          Toast.success(i18n.t('pref_reset_done'));
          Auth.logout();
          App.showLogin();
        }
      };
      document.getElementById('btn-confirm-no').onclick = () => Modal.close('modal-confirm');
      Modal.open('modal-confirm');
    });

    // Copy GUID
    document.getElementById('btn-copy-guid')?.addEventListener('click', () => {
      const guid = document.getElementById('pref-sync-guid').value;
      if (guid) {
        navigator.clipboard.writeText(guid);
        Toast.success(i18n.t('shopping_copied'));
      }
    });

    // Join Channel
    document.getElementById('btn-join-channel')?.addEventListener('click', () => {
      const newGuid = prompt(i18n.lang === 'fr' ? 'Entrez le GUID du canal à rejoindre :' : 'Enter the channel GUID to join:');
      if (newGuid && newGuid.trim().length > 30) {
        joinChannel(newGuid.trim());
      }
    });

    // Show QR
    document.getElementById('btn-show-qr')?.addEventListener('click', () => {
      const guid = document.getElementById('pref-sync-guid').value;
      if (!guid) return;

      Modal.open('modal-qr-display');
      const canvas = document.getElementById('qr-canvas');
      QRCode.toCanvas(canvas, guid, { width: 256, margin: 1 }, error => {
        if (error) console.error(error);
      });
      document.getElementById('qr-guid-text').textContent = guid;
    });

    // Scan QR
    let html5QrCode = null;
    document.getElementById('btn-scan-qr')?.addEventListener('click', async () => {
      Modal.open('modal-qr-scanner');
      html5QrCode = new Html5Qrcode("reader");
      const config = { fps: 10, qrbox: { width: 250, height: 250 } };

      try {
        await html5QrCode.start({ facingMode: "environment" }, config, (decodedText) => {
          html5QrCode.stop().then(() => {
            Modal.close('modal-qr-scanner');
            if (decodedText && decodedText.length > 30) {
              joinChannel(decodedText.trim());
            }
          });
        });
      } catch (err) {
        console.error("Camera error:", err);
        Toast.error(i18n.lang === 'fr' ? "Erreur caméra" : "Camera error");
        Modal.close('modal-qr-scanner');
      }
    });

    document.getElementById('btn-close-scanner')?.addEventListener('click', () => {
      if (html5QrCode) {
        html5QrCode.stop().finally(() => {
          Modal.close('modal-qr-scanner');
        });
      } else {
        Modal.close('modal-qr-scanner');
      }
    });
  }

  function joinChannel(guid) {
    if (confirm(i18n.lang === 'fr' ? 'Rejoindre ce canal synchronisera vos données avec ses membres. Continuer ?' : 'Joining this channel will sync your data with its members. Continue?')) {
      Auth.updateSyncChannel(Auth.getCurrentUserId(), guid).then(() => {
        Toast.success(i18n.lang === 'fr' ? 'Canal mis à jour. Redémarrage...' : 'Channel updated. Restarting...');
        setTimeout(() => location.reload(), 1500);
      });
    }
  }

  return { init, render, open };
})();
