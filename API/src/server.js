import 'dotenv/config';
import crypto from 'crypto';
import express from 'express';
import rateLimit from 'express-rate-limit';

import { pool, query, withTransaction } from './db.js';
import { hashPassword, verifyPassword, generateSaltHex } from './hash.js';
import { sendVerificationEmail } from './mailer.js';

const PORT = Number(process.env.PORT) || 3001;
const HOST = process.env.HOST || '127.0.0.1';
const VERIFICATION_TTL_MS = 15 * 60 * 1000; // 15 minutes

const app = express();

// Le service tourne derrière nginx (un seul proxy) : on fait confiance au 1er hop
// pour récupérer l'IP client réelle (X-Forwarded-For) côté rate-limit.
app.set('trust proxy', 1);
app.use(express.json({ limit: '32kb' }));

// --- Rate limiting large sur toutes les routes /account/* ---
const accountLimiter = rateLimit({
    windowMs: Number(process.env.RATE_LIMIT_WINDOW_MS) || 15 * 60 * 1000,
    max: Number(process.env.RATE_LIMIT_MAX) || 20,
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Trop de requêtes, réessayez plus tard.' },
});
app.use('/account', accountLimiter);

// --- Helpers ---
function newDeviceToken() {
    return crypto.randomBytes(32).toString('hex');
}

function sixDigitCode() {
    return String(crypto.randomInt(0, 1_000_000)).padStart(6, '0');
}

function normalizeEmail(email) {
    return String(email).trim().toLowerCase();
}

function isNonEmptyString(v) {
    return typeof v === 'string' && v.trim().length > 0;
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

async function findCompteByEmail(email, client = pool) {
    const { rows } = await client.query('SELECT * FROM comptes WHERE email = $1', [email]);
    return rows[0] || null;
}

async function createDeviceToken(client, compteId, deviceName) {
    const token = newDeviceToken();
    await client.query(
        `INSERT INTO device_tokens (token, compte_id, nom_appareil, last_active_at)
         VALUES ($1, $2, $3, now())`,
        [token, compteId, isNonEmptyString(deviceName) ? deviceName.trim() : null],
    );
    return token;
}

// wrapper async -> évite la répétition du try/catch
function h(fn) {
    return (req, res) => fn(req, res).catch((err) => {
        // Violation de contrainte d'unicité (email) -> conflit
        if (err && err.code === '23505') {
            return res.status(409).json({
                error: 'Un compte existe déjà avec cet email. Utilisez /account/login.',
            });
        }
        console.error(`[${req.method} ${req.path}]`, err);
        res.status(500).json({ error: 'Erreur interne.' });
    });
}

// --- Santé ---
app.get('/health', h(async (_req, res) => {
    await query('SELECT 1');
    res.json({ status: 'ok' });
}));

// --- POST /account/migrate ---
// Compte migré depuis un appareil : hash/sel déjà calculés côté app, transférés
// tels quels. On ne recalcule jamais ici.
app.post('/account/migrate', h(async (req, res) => {
    const {
        email, nom, prenom,
        password_hash, password_salt,
        recovery_code_hash, recovery_code_salt,
        guid, device_name,
    } = req.body || {};

    if (!isNonEmptyString(email) || !EMAIL_RE.test(email.trim())) {
        return res.status(400).json({ error: 'Email invalide.' });
    }
    if (!isNonEmptyString(password_hash) || !isNonEmptyString(password_salt)) {
        return res.status(400).json({ error: 'password_hash et password_salt sont requis.' });
    }

    const normEmail = normalizeEmail(email);

    const existing = await findCompteByEmail(normEmail);
    if (existing) {
        return res.status(409).json({
            error: 'Un compte existe déjà avec cet email. Sur ce nouvel appareil, utilisez /account/login pour vous y rattacher.',
        });
    }

    const result = await withTransaction(async (client) => {
        const { rows } = await client.query(
            `INSERT INTO comptes
                (email, password_hash, password_salt, recovery_code_hash, recovery_code_salt,
                 nom, prenom, email_verifie, guid)
             VALUES ($1, $2, $3, $4, $5, $6, $7, true, $8)
             RETURNING id, email_verifie, guid`,
            [
                normEmail,
                password_hash,
                password_salt,
                isNonEmptyString(recovery_code_hash) ? recovery_code_hash : null,
                isNonEmptyString(recovery_code_salt) ? recovery_code_salt : null,
                isNonEmptyString(nom) ? nom.trim() : null,
                isNonEmptyString(prenom) ? prenom.trim() : null,
                isNonEmptyString(guid) ? guid.trim() : null,
            ],
        );
        const compte = rows[0];
        const token = await createDeviceToken(client, compte.id, device_name);
        return {
            token,
            compte_id: compte.id,
            email_verifie: compte.email_verifie,
            guid: compte.guid,
        };
    });

    res.status(201).json(result);
}));

// --- POST /account/register ---
// Inscription neuve : le service génère sel + hash, crée le compte non vérifié,
// envoie un code par email. Le compte est utilisable immédiatement.
app.post('/account/register', h(async (req, res) => {
    const { email, password, nom, prenom, guid, device_name } = req.body || {};

    if (!isNonEmptyString(email) || !EMAIL_RE.test(email.trim())) {
        return res.status(400).json({ error: 'Email invalide.' });
    }
    if (!isNonEmptyString(password) || password.length < 8) {
        return res.status(400).json({ error: 'Mot de passe requis (8 caractères minimum).' });
    }

    const normEmail = normalizeEmail(email);

    if (await findCompteByEmail(normEmail)) {
        return res.status(409).json({
            error: 'Un compte existe déjà avec cet email. Utilisez /account/login.',
        });
    }

    const saltHex = generateSaltHex();
    const passwordHashB64 = hashPassword(password, saltHex);

    const code = sixDigitCode();
    const codeSaltHex = generateSaltHex();
    const codeHashB64 = hashPassword(code, codeSaltHex);
    const expiresAt = new Date(Date.now() + VERIFICATION_TTL_MS);

    const result = await withTransaction(async (client) => {
        const { rows } = await client.query(
            `INSERT INTO comptes
                (email, password_hash, password_salt, nom, prenom, email_verifie, guid)
             VALUES ($1, $2, $3, $4, $5, false, $6)
             RETURNING id, email_verifie, guid`,
            [
                normEmail,
                passwordHashB64,
                saltHex,
                isNonEmptyString(nom) ? nom.trim() : null,
                isNonEmptyString(prenom) ? prenom.trim() : null,
                isNonEmptyString(guid) ? guid.trim() : null,
            ],
        );
        const compte = rows[0];

        await client.query(
            `INSERT INTO codes_verification
                (compte_id, code_hash, code_salt, type, expires_at)
             VALUES ($1, $2, $3, 'email_verification', $4)`,
            [compte.id, codeHashB64, codeSaltHex, expiresAt],
        );

        const token = await createDeviceToken(client, compte.id, device_name);
        return {
            token,
            compte_id: compte.id,
            email_verifie: compte.email_verifie,
            guid: compte.guid,
        };
    });

    // L'envoi de l'email ne doit pas faire échouer la création de compte.
    try {
        await sendVerificationEmail(normEmail, code);
    } catch (mailErr) {
        console.error('[register] échec envoi email de vérification', mailErr);
    }

    res.status(201).json(result);
}));

// --- POST /account/login ---
app.post('/account/login', h(async (req, res) => {
    const { email, password, guid, device_name } = req.body || {};

    const GENERIC_401 = { error: 'Email ou mot de passe incorrect.' };

    if (!isNonEmptyString(email) || !isNonEmptyString(password)) {
        return res.status(401).json(GENERIC_401);
    }

    const normEmail = normalizeEmail(email);
    const compte = await findCompteByEmail(normEmail);
    if (!compte) {
        return res.status(401).json(GENERIC_401);
    }

    if (!verifyPassword(password, compte.password_salt, compte.password_hash)) {
        return res.status(401).json(GENERIC_401);
    }

    // guid est optionnel côté login :
    //  - absent/null/vide          -> aucune vérification, on procède.
    //  - fourni, compte sans guid  -> cas résiduel : on enregistre ce guid sur le compte.
    //  - fourni, identique         -> rien à faire, on procède.
    //  - fourni, différent         -> vrai conflit de foyer : 409 (à traiter via un futur
    //                                 endpoint dédié de changement de foyer, pas ici).
    const providedGuid = isNonEmptyString(guid) ? guid.trim() : null;
    if (compte.guid && providedGuid && compte.guid !== providedGuid) {
        return res.status(409).json({ error: 'Ce compte est déjà rattaché à un autre foyer.' });
    }

    const finalGuid = compte.guid || providedGuid;

    const result = await withTransaction(async (client) => {
        if (!compte.guid && providedGuid) {
            await client.query('UPDATE comptes SET guid = $1 WHERE id = $2', [providedGuid, compte.id]);
        }
        const token = await createDeviceToken(client, compte.id, device_name);
        return {
            token,
            compte_id: compte.id,
            email_verifie: compte.email_verifie,
            guid: finalGuid,
        };
    });

    res.json(result);
}));

// --- POST /account/password/recover ---
// Réinitialisation par code de récupération (20 caractères, généré et affiché une
// seule fois côté app à l'inscription — cf. recovery_code_hash/salt). Pas d'email :
// le code EST le facteur de récupération. Succès == connexion (renvoie un token).
app.post('/account/password/recover', h(async (req, res) => {
    const { email, recovery_code, new_password, guid, device_name } = req.body || {};

    const GENERIC_401 = { error: 'Email ou code de récupération invalide.' };

    if (!isNonEmptyString(email) || !isNonEmptyString(recovery_code)) {
        return res.status(401).json(GENERIC_401);
    }
    if (!isNonEmptyString(new_password) || new_password.length < 8) {
        return res.status(400).json({ error: 'Nouveau mot de passe requis (8 caractères minimum).' });
    }

    const normEmail = normalizeEmail(email);
    const compte = await findCompteByEmail(normEmail);
    if (!compte || !compte.recovery_code_hash || !compte.recovery_code_salt) {
        return res.status(401).json(GENERIC_401);
    }

    const normalizedCode = recovery_code.replace(/-/g, '').trim().toUpperCase();
    if (!verifyPassword(normalizedCode, compte.recovery_code_salt, compte.recovery_code_hash)) {
        return res.status(401).json(GENERIC_401);
    }

    const providedGuid = isNonEmptyString(guid) ? guid.trim() : null;
    if (compte.guid && providedGuid && compte.guid !== providedGuid) {
        return res.status(409).json({ error: 'Ce compte est déjà rattaché à un autre foyer.' });
    }
    const finalGuid = compte.guid || providedGuid;

    const newSaltHex = generateSaltHex();
    const newPasswordHashB64 = hashPassword(new_password, newSaltHex);

    const result = await withTransaction(async (client) => {
        await client.query(
            `UPDATE comptes SET password_hash = $1, password_salt = $2, guid = $3 WHERE id = $4`,
            [newPasswordHashB64, newSaltHex, finalGuid, compte.id],
        );
        const token = await createDeviceToken(client, compte.id, device_name);
        return {
            token,
            compte_id: compte.id,
            email_verifie: compte.email_verifie,
            guid: finalGuid,
        };
    });

    res.json(result);
}));

app.use((req, res) => res.status(404).json({ error: 'Route inconnue.' }));

const server = app.listen(PORT, HOST, () => {
    console.log(`✅ account-service à l'écoute sur http://${HOST}:${PORT}`);
});

function shutdown(signal) {
    console.log(`[${signal}] arrêt en cours...`);
    server.close(() => {
        pool.end().then(() => process.exit(0));
    });
}
process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT', () => shutdown('SIGINT'));
