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
    document.getElementById('btn-lang-fr').classList.toggle('active', i18n.lang === 'fr');
    document.getElementById('btn-lang-en').classList.toggle('active', i18n.lang === 'en');
    document.getElementById('btn-lang-fr').textContent = i18n.t('pref_lang_fr');
    document.getElementById('btn-lang-en').textContent = i18n.t('pref_lang_en');

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

    // ── Premium ──
    document.getElementById('pref-section-premium').textContent = i18n.t('pref_section_premium');
    renderPremium(prefs);

    // ── Data ──
    document.getElementById('pref-section-data').textContent = i18n.t('pref_section_data');
    document.getElementById('btn-pref-export').textContent = i18n.t('pref_export');
    document.getElementById('btn-pref-reset').textContent = i18n.t('pref_reset');
  }

  function renderPremium(prefs) {
    const section = document.getElementById('premium-section');
    section.innerHTML = `
      <div class="premium-banner">
        <div class="premium-banner-icon">🌟</div>
        <div class="premium-banner-text">
          <div class="premium-banner-title">${i18n.t('premium_title')}</div>
          <div class="premium-banner-sub">${i18n.t('premium_sub')}</div>
        </div>
      </div>
      <div style="padding:0 16px 12px">
        ${['premium_feature_1','premium_feature_2','premium_feature_3','premium_feature_4','premium_feature_5']
          .map(k => `<div style="font-size:13px;color:var(--text-secondary);padding:4px 0">${i18n.t(k)}</div>`).join('')}
      </div>
      <div style="padding:0 16px 16px">
        ${prefs.isPremium
          ? `<div class="btn btn-secondary" style="cursor:default">${i18n.t('premium_already')}</div>`
          : `<button class="btn btn-premium" id="btn-activate-premium">${i18n.t('premium_activate')}</button>`
        }
      </div>`;

    if (!prefs.isPremium) {
      document.getElementById('btn-activate-premium').addEventListener('click', async () => {
        const userId = Auth.getCurrentUserId();
        await Auth.activatePremium(userId);
        await PrefsModel.applyTheme(prefs.theme);
        Toast.success('🌟 ' + (i18n.lang === 'fr' ? 'Premium activé !' : 'Premium activated!'));
        await AdBanner.update();
        render();
      });
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
    document.getElementById('btn-lang-fr').addEventListener('click', async () => {
      i18n.setLang('fr');
      await PrefsModel.set(Auth.getCurrentUserId(), { lang: 'fr' });
      App.applyLanguage();
      render();
    });
    document.getElementById('btn-lang-en').addEventListener('click', async () => {
      i18n.setLang('en');
      await PrefsModel.set(Auth.getCurrentUserId(), { lang: 'en' });
      App.applyLanguage();
      render();
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
        await DB.deleteAllUserData(Auth.getCurrentUserId());
        Modal.close('modal-confirm');
        close();
        Toast.success(i18n.t('pref_reset_done'));
        App.showLogin();
        Auth.logout();
      };
      document.getElementById('btn-confirm-no').onclick = () => Modal.close('modal-confirm');
      Modal.open('modal-confirm');
    });
  }

  return { init, render, open };
})();
