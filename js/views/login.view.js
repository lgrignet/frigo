// ============================================================
//  login.view.js — Écran de connexion / inscription
// ============================================================

const LoginView = (() => {

  let currentTab = 'login'; // 'login' | 'register' | 'recover'

  function render() {
    document.getElementById('login-title').textContent = i18n.t('app_name');
    document.getElementById('login-tagline').textContent = i18n.t('app_tagline');
    document.getElementById('login-tab-login').textContent = i18n.t('login');
    document.getElementById('login-tab-register').textContent = i18n.t('register');
    showTab('login');
  }

  function showTab(tab) {
    currentTab = tab;
    document.querySelectorAll('.login-tab').forEach(t => t.classList.remove('active'));
    document.getElementById(`login-tab-${tab}`)?.classList.add('active');
    document.querySelectorAll('.login-form-section').forEach(s => s.classList.remove('active'));
    document.getElementById(`section-${tab}`)?.classList.add('active');
    document.getElementById('login-error').textContent = '';

    // Update labels
    if (tab === 'login') {
      document.getElementById('label-login-email').textContent = i18n.t('email');
      document.getElementById('label-login-pass').textContent = i18n.t('password');
      document.getElementById('btn-login').textContent = i18n.t('login_btn');
      document.getElementById('link-forgot').textContent = i18n.t('forgot_password');
    } else if (tab === 'register') {
      document.getElementById('label-reg-email').textContent = i18n.t('email');
      document.getElementById('label-reg-pass').textContent = i18n.t('password');
      document.getElementById('label-reg-pass2').textContent = i18n.t('password_confirm');
      document.getElementById('btn-register').textContent = i18n.t('register_btn');
    } else if (tab === 'recover') {
      document.getElementById('label-rec-email').textContent = i18n.t('email');
      document.getElementById('label-rec-code').textContent = i18n.t('recovery_code_input');
      document.getElementById('label-rec-newpass').textContent = i18n.t('new_password');
      document.getElementById('btn-recover').textContent = i18n.t('recover_btn');
      document.getElementById('link-back-login').textContent = i18n.t('back_to_login');
    }
  }

  function showError(msg) {
    const el = document.getElementById('login-error');
    el.textContent = msg;
    el.style.display = 'block';
  }

  function clearError() {
    const el = document.getElementById('login-error');
    el.textContent = '';
  }

  // ── Login ────────────────────────────────────────────────────
  async function doLogin() {
    clearError();
    const email = document.getElementById('login-email').value.trim();
    const pass  = document.getElementById('login-pass').value;
    if (!email || !pass) { showError(i18n.t('err_missing_fields')); return; }

    const btn = document.getElementById('btn-login');
    btn.disabled = true; btn.textContent = i18n.t('loading');

    try {
      await Auth.login(email, pass);
      App.onLoginSuccess();
    } catch (e) {
      if (e.message === 'INVALID_CREDENTIALS') showError(i18n.t('err_invalid_creds'));
      else showError(e.message);
    } finally {
      btn.disabled = false; btn.textContent = i18n.t('login_btn');
    }
  }

  // ── Register ─────────────────────────────────────────────────
  async function doRegister() {
    clearError();
    const email = document.getElementById('reg-email').value.trim();
    const pass  = document.getElementById('reg-pass').value;
    const pass2 = document.getElementById('reg-pass2').value;

    if (!email || !pass || !pass2) { showError(i18n.t('err_missing_fields')); return; }
    if (pass !== pass2) { showError(i18n.t('err_password_mismatch')); return; }

    const btn = document.getElementById('btn-register');
    btn.disabled = true; btn.textContent = i18n.t('loading');

    try {
      const { userId, recoveryCode } = await Auth.register(email, pass);
      showRecoveryCode(recoveryCode);
    } catch (e) {
      if (e.message === 'EMAIL_EXISTS')        showError(i18n.t('err_email_exists'));
      else if (e.message === 'PASSWORD_TOO_SHORT') showError(i18n.t('err_password_short'));
      else showError(e.message);
    } finally {
      btn.disabled = false; btn.textContent = i18n.t('register_btn');
    }
  }

  function showRecoveryCode(code) {
    // Show recovery code modal before entering the app
    document.getElementById('recovery-code-display').textContent = code;
    document.getElementById('recovery-warn-text').textContent = i18n.t('recovery_code_warn');
    document.getElementById('btn-noted-code').textContent = i18n.t('i_noted_code');
    Modal.open('modal-recovery-code');
  }

  // ── Recover ──────────────────────────────────────────────────
  async function doRecover() {
    clearError();
    const email   = document.getElementById('rec-email').value.trim();
    const code    = document.getElementById('rec-code').value.trim().toUpperCase();
    const newPass = document.getElementById('rec-newpass').value;

    if (!email || !code || !newPass) { showError(i18n.t('err_missing_fields')); return; }

    const btn = document.getElementById('btn-recover');
    btn.disabled = true; btn.textContent = i18n.t('loading');

    try {
      const { newRecoveryCode } = await Auth.recoverWithCode(email, code, newPass);
      showRecoveryCode(newRecoveryCode);
    } catch (e) {
      if (e.message === 'USER_NOT_FOUND')       showError(i18n.t('err_user_not_found'));
      else if (e.message === 'INVALID_RECOVERY_CODE') showError(i18n.t('err_invalid_recovery'));
      else if (e.message === 'PASSWORD_TOO_SHORT')    showError(i18n.t('err_password_short'));
      else showError(e.message);
    } finally {
      btn.disabled = false; btn.textContent = i18n.t('recover_btn');
    }
  }

  function init() {
    document.getElementById('login-tab-login').addEventListener('click', () => showTab('login'));
    document.getElementById('login-tab-register').addEventListener('click', () => showTab('register'));
    document.getElementById('link-forgot').addEventListener('click', () => showTab('recover'));
    document.getElementById('link-back-login').addEventListener('click', () => showTab('login'));

    document.getElementById('btn-login').addEventListener('click', doLogin);
    document.getElementById('btn-register').addEventListener('click', doRegister);
    document.getElementById('btn-recover').addEventListener('click', doRecover);

    // Enter key
    ['login-email','login-pass'].forEach(id => {
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') doLogin(); });
    });
    ['reg-email','reg-pass','reg-pass2'].forEach(id => {
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') doRegister(); });
    });

    // Recovery code modal
    document.getElementById('btn-noted-code').addEventListener('click', () => {
      Modal.close('modal-recovery-code');
      App.onLoginSuccess();
    });
  }

  return { render, init, showTab };
})();
