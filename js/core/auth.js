// ============================================================
//  auth.js — Authentification locale (login / register / session)
// ============================================================

const Auth = (() => {

  const SESSION_KEY = 'msm_session';

  // ── Session ────────────────────────────────────────────────
  function getSession() {
    try {
      const raw = sessionStorage.getItem(SESSION_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch { return null; }
  }

  function setSession(userId, email, syncChannelGuid) {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify({ userId, email, syncChannelGuid, loginAt: Date.now() }));
  }

  function clearSession() {
    sessionStorage.removeItem(SESSION_KEY);
    Crypto.clearSessionKey();
  }

  function isLoggedIn() {
    return !!getSession() && !!Crypto.getSessionKey();
  }

  function getCurrentUserId() {
    return getSession()?.userId ?? null;
  }

  function getCurrentEmail() {
    return getSession()?.email ?? null;
  }

  // ── Registration ───────────────────────────────────────────
  async function register(email, password) {
    const normalized = email.trim().toLowerCase();
    if (!normalized || !password) throw new Error('MISSING_FIELDS');
    if (password.length < 6) throw new Error('PASSWORD_TOO_SHORT');

    // Check email already taken
    const existing = await findUserByEmail(normalized);
    if (existing) throw new Error('EMAIL_EXISTS');

    // Hash password
    const salt = Crypto.generateSalt();
    const passwordHash = await Crypto.hashPassword(password, salt);

    // Generate recovery code
    const recoveryCode = Crypto.generateRecoveryCode();
    const recoverySalt = Crypto.generateSalt();
    const recoveryHash = await Crypto.hashRecoveryCode(recoveryCode, recoverySalt);

    // Create user
    const user = {
      email: normalized,
      passwordHash,
      salt,
      recoveryHash,
      recoverySalt,
      syncChannelGuid: crypto.randomUUID(), // Le canal de synchro par défaut
      createdAt: new Date().toISOString(),
    };

    const userId = await DB.put('users', user);

    // Create default storages
    await createDefaultStorages(userId);
    await createDefaultShops(userId);

    // Create default preferences
    await DB.put('preferences', {
      userId,
      expiryWarningDays: 7,
      notificationsEnabled: false,
      notificationTime: '08:00',
      defaultUnit: 'pièce(s)',
      defaultStorageId: null,
      theme: 'dark',
      adsEnabled: true,
      lang: 'fr',
    });

    // Setup session key
    await Crypto.setSessionKey(password, salt);
    setSession(userId, normalized, user.syncChannelGuid);

    return { userId, recoveryCode };
  }

  // ── Login ──────────────────────────────────────────────────
  async function login(email, password) {
    const normalized = email.trim().toLowerCase();
    const user = await findUserByEmail(normalized);
    if (!user) throw new Error('INVALID_CREDENTIALS');

    const hash = await Crypto.hashPassword(password, user.salt);
    if (hash !== user.passwordHash) throw new Error('INVALID_CREDENTIALS');

    // Assurer qu'un vieux compte a un GUID
    if (!user.syncChannelGuid) {
      user.syncChannelGuid = crypto.randomUUID();
      await DB.put('users', user);
    }

    await Crypto.setSessionKey(password, user.salt);
    setSession(user.id, normalized, user.syncChannelGuid);
    return user;
  }

  async function updateSyncChannel(userId, newGuid) {
    const user = await DB.getOne('users', userId);
    if (!user) return;
    user.syncChannelGuid = newGuid;
    await DB.put('users', user);
    // On met à jour la session pour que l'app sache qu'elle doit se reconnecter
    const session = getSession();
    if (session) {
      session.syncChannelGuid = newGuid;
      sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
    }
  }

  // ── Password recovery ──────────────────────────────────────
  async function recoverWithCode(email, recoveryCode, newPassword) {
    const normalized = email.trim().toLowerCase();
    const user = await findUserByEmail(normalized);
    if (!user) throw new Error('USER_NOT_FOUND');

    const codeHash = await Crypto.hashRecoveryCode(recoveryCode, user.recoverySalt);
    if (codeHash !== user.recoveryHash) throw new Error('INVALID_RECOVERY_CODE');

    if (newPassword.length < 6) throw new Error('PASSWORD_TOO_SHORT');

    // Update password
    const newSalt = Crypto.generateSalt();
    const newHash = await Crypto.hashPassword(newPassword, newSalt);

    // New recovery code
    const newRecoveryCode = Crypto.generateRecoveryCode();
    const newRecoverySalt = Crypto.generateSalt();
    const newRecoveryHash = await Crypto.hashRecoveryCode(newRecoveryCode, newRecoverySalt);

    const updatedUser = {
      ...user,
      passwordHash: newHash,
      salt: newSalt,
      recoveryHash: newRecoveryHash,
      recoverySalt: newRecoverySalt,
    };

    // Assurer qu'un vieux compte a un GUID lors de la récupération
    if (!updatedUser.syncChannelGuid) {
      updatedUser.syncChannelGuid = crypto.randomUUID();
    }

    await DB.put('users', updatedUser);

    await Crypto.setSessionKey(newPassword, newSalt);
    setSession(user.id, normalized, updatedUser.syncChannelGuid);

    return { newRecoveryCode };
  }

  // ── Logout ─────────────────────────────────────────────────
  function logout() {
    clearSession();
  }

  // ── Helpers ────────────────────────────────────────────────
  async function findUserByEmail(email) {
    const db = await DB.open();
    return new Promise((resolve, reject) => {
      const tx = db.transaction('users', 'readonly');
      const req = tx.objectStore('users').index('email').get(email);
      req.onsuccess = (e) => resolve(e.target.result || null);
      req.onerror   = (e) => reject(e.target.error);
    });
  }

  async function createDefaultStorages(userId) {
    const defaults = [
      { name: 'Frigo',      icon: '🧊', type: 'cold',    isDefault: true },
      { name: 'Congélateur',icon: '❄️',  type: 'frozen',  isDefault: false },
      { name: 'Armoire',    icon: '🚪', type: 'dry',     isDefault: false },
      { name: 'Cave',       icon: '🏚️', type: 'dry',     isDefault: false },
    ];
    for (const s of defaults) {
      await DB.put('storages', {
        id: crypto.randomUUID(),
        userId,
        ...s,
        createdAt: new Date().toISOString(),
      });
    }
  }

  async function createDefaultShops(userId) {
    const defaults = ['Carrefour', 'Colruyt', 'Lidl', 'Delhaize'];
    for (const name of defaults) {
      await DB.put('shops', {
        id: crypto.randomUUID(),
        userId,
        name,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      });
    }
  }

  async function listLocalAccounts() {
    return DB.getAll('users');
  }

  function getCurrentSyncChannel() {
    return getSession()?.syncChannelGuid ?? null;
  }

  return {
    register, login, recoverWithCode, logout,
    getSession, isLoggedIn, getCurrentUserId, getCurrentEmail, getCurrentSyncChannel,
    findUserByEmail, listLocalAccounts, updateSyncChannel,
  };
})();
