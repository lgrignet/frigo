// ============================================================
//  crypto.js — Chiffrement Web Crypto API
//  PBKDF2 (hash mdp) + AES-256-GCM (chiffrement données)
// ============================================================

const Crypto = (() => {

  // ── Utilitaires binaires ───────────────────────────────────
  const toBase64 = (buf) => btoa(String.fromCharCode(...new Uint8Array(buf)));
  const fromBase64 = (b64) => Uint8Array.from(atob(b64), c => c.charCodeAt(0));
  const toHex = (buf) => [...new Uint8Array(buf)].map(b => b.toString(16).padStart(2,'0')).join('');
  const fromHex = (hex) => new Uint8Array(hex.match(/../g).map(h => parseInt(h, 16)));
  const encode = (str) => new TextEncoder().encode(str);
  const decode = (buf) => new TextDecoder().decode(buf);

  // ── Génération salt / IV ───────────────────────────────────
  function generateSalt(bytes = 32) {
    return toHex(crypto.getRandomValues(new Uint8Array(bytes)));
  }

  function generateIV() {
    return crypto.getRandomValues(new Uint8Array(12)); // 96-bit for GCM
  }

  // ── Dérivation de clé PBKDF2 ──────────────────────────────
  async function deriveKey(password, saltHex) {
    const saltBytes = fromHex(saltHex);
    const baseKey = await crypto.subtle.importKey(
      'raw', encode(password), 'PBKDF2', false, ['deriveKey']
    );
    return crypto.subtle.deriveKey(
      {
        name: 'PBKDF2',
        salt: saltBytes,
        iterations: 150000,
        hash: 'SHA-256'
      },
      baseKey,
      { name: 'AES-GCM', length: 256 },
      false,
      ['encrypt', 'decrypt']
    );
  }

  // ── Hash mot de passe (pour vérification login) ────────────
  async function hashPassword(password, saltHex) {
    const saltBytes = fromHex(saltHex);
    const baseKey = await crypto.subtle.importKey(
      'raw', encode(password), 'PBKDF2', false, ['deriveBits']
    );
    const hashBuf = await crypto.subtle.deriveBits(
      { name: 'PBKDF2', salt: saltBytes, iterations: 150000, hash: 'SHA-256' },
      baseKey, 256
    );
    return toBase64(hashBuf);
  }

  // ── Génération code de récupération ───────────────────────
  function generateRecoveryCode() {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let code = '';
    const rand = crypto.getRandomValues(new Uint8Array(20));
    for (let i = 0; i < 20; i++) {
      if (i > 0 && i % 5 === 0) code += '-';
      code += chars[rand[i] % chars.length];
    }
    return code; // Format: XXXXX-XXXXX-XXXXX-XXXXX
  }

  // ── Hash code de récupération ──────────────────────────────
  async function hashRecoveryCode(code, saltHex) {
    // Use same PBKDF2 but fewer iterations (code is stored separately)
    return hashPassword(code.replace(/-/g, ''), saltHex);
  }

  // ── Chiffrement AES-256-GCM ────────────────────────────────
  async function encrypt(plaintext, key) {
    const iv = generateIV();
    const encoded = encode(typeof plaintext === 'string' ? plaintext : JSON.stringify(plaintext));
    const cipherBuf = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, encoded);
    // Return: base64(iv) + "." + base64(ciphertext)
    return toBase64(iv) + '.' + toBase64(cipherBuf);
  }

  async function decrypt(ciphertext, key) {
    const [ivB64, datB64] = ciphertext.split('.');
    const iv = fromBase64(ivB64);
    const data = fromBase64(datB64);
    const plainBuf = await crypto.subtle.decrypt({ name: 'AES-GCM', iv }, key, data);
    return decode(plainBuf);
  }

  // ── Session key management ─────────────────────────────────
  // We store the raw key bytes in sessionStorage so we can reimport it
  // The key itself is derived from the password — never store password in memory longer than needed

  let _sessionKey = null; // in-memory only during session

  async function setSessionKey(password, saltHex) {
    _sessionKey = await deriveKey(password, saltHex);
  }

  function getSessionKey() {
    return _sessionKey;
  }

  function clearSessionKey() {
    _sessionKey = null;
  }

  // ── Encrypt/decrypt with session key ──────────────────────
  async function encryptWithSession(data) {
    if (!_sessionKey) throw new Error('No session key');
    return encrypt(data, _sessionKey);
  }

  async function decryptWithSession(ciphertext) {
    if (!_sessionKey) throw new Error('No session key');
    return decrypt(ciphertext, _sessionKey);
  }

  return {
    generateSalt,
    generateIV,
    hashPassword,
    generateRecoveryCode,
    hashRecoveryCode,
    encrypt,
    decrypt,
    setSessionKey,
    getSessionKey,
    clearSessionKey,
    encryptWithSession,
    decryptWithSession,
  };
})();
