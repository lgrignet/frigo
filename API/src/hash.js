import crypto from 'crypto';

/**
 * Hachage de mot de passe — paramètres IDENTIQUES à ceux de l'app Android
 * (CryptoManager.kt) : PBKDF2WithHmacSHA256, 150 000 itérations, clé dérivée
 * de 256 bits (32 octets).
 *
 * Convention de stockage (cohérente avec l'app) :
 *   - le sel est stocké en hexadécimal  -> décodé en octets bruts avant dérivation
 *   - le hash est stocké en base64
 */
const ITERATIONS = 150_000;
const KEYLEN = 32; // 256 bits
const DIGEST = 'sha256';

/**
 * Dérive le hash (base64) d'un mot de passe à partir d'un sel hexadécimal.
 * @param {string} password
 * @param {string} saltHex sel au format hexadécimal
 * @returns {string} hash en base64
 */
export function hashPassword(password, saltHex) {
    const salt = Buffer.from(saltHex, 'hex');
    const derived = crypto.pbkdf2Sync(password, salt, ITERATIONS, KEYLEN, DIGEST);
    return derived.toString('base64');
}

/** Génère un sel aléatoire de 32 octets, renvoyé en hexadécimal. */
export function generateSaltHex() {
    return crypto.randomBytes(32).toString('hex');
}

/**
 * Compare en temps constant un mot de passe candidat au hash base64 stocké.
 * @returns {boolean}
 */
export function verifyPassword(password, saltHex, expectedHashB64) {
    let expected;
    try {
        expected = Buffer.from(expectedHashB64, 'base64');
    } catch {
        return false;
    }
    const actual = Buffer.from(hashPassword(password, saltHex), 'base64');
    if (actual.length !== expected.length || actual.length === 0) return false;
    return crypto.timingSafeEqual(actual, expected);
}
