import { WebSocketServer } from 'ws';
import fs from 'fs';
import path from 'path';

/**
 * Serveur de Synchronisation MyStockManager v3
 * Gère la persistance et la réconciliation d'état (Suppression post-reconnexion)
 */

const rooms = new Map();
const DATA_DIR = './data';

if (!fs.existsSync(DATA_DIR)) {
    fs.mkdirSync(DATA_DIR);
}

const PORT = 3000;
const wss = new WebSocketServer({ port: PORT });

// --- Authentification des connexions (device_token délivré par api.noshi.be) ---
// Le token est vérifié auprès du service de comptes avant d'accepter une room ;
// résultat mis en cache brièvement pour ne pas le solliciter à chaque reconnexion.
const ACCOUNT_SERVICE_URL = process.env.ACCOUNT_SERVICE_URL || 'http://127.0.0.1:3001';
const INTERNAL_SHARED_SECRET = process.env.INTERNAL_SHARED_SECRET;
const TOKEN_CACHE_TTL_MS = 60 * 1000;
const tokenCache = new Map(); // token -> { validUntil, compteId, guid }

async function validateToken(token) {
    if (!token) return null;

    const cached = tokenCache.get(token);
    if (cached && cached.validUntil > Date.now()) return cached;

    try {
        const res = await fetch(`${ACCOUNT_SERVICE_URL}/internal/token/check`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-Internal-Secret': INTERNAL_SHARED_SECRET ?? '' },
            body: JSON.stringify({ token }),
        });
        if (!res.ok) return null;
        const data = await res.json();
        if (!data.valid) return null;

        const entry = { validUntil: Date.now() + TOKEN_CACHE_TTL_MS, compteId: data.compte_id, guid: data.guid };
        tokenCache.set(token, entry);
        return entry;
    } catch (e) {
        // Échec réseau vers le service de comptes -> on refuse (fail-closed), pas
        // de repli permissif : mieux vaut un canal indisponible qu'un canal ouvert.
        console.error('[auth] vérification du token échouée (account-service injoignable ?)', e.message);
        return null;
    }
}

function loadRoomData(roomId) {
    const filePath = path.join(DATA_DIR, `${roomId}.json`);
    if (fs.existsSync(filePath)) {
        try {
            return JSON.parse(fs.readFileSync(filePath, 'utf8'));
        } catch (e) {
            return {};
        }
    }
    return {};
}

function saveRoomData(roomId, data) {
    const filePath = path.join(DATA_DIR, `${roomId}.json`);
    fs.writeFileSync(filePath, JSON.stringify(data));
}

wss.on('connection', async (ws, req) => {
    const parsedUrl = new URL(req.url, 'http://internal');
    const room = parsedUrl.pathname.replace('/', '') || 'default';
    const token = parsedUrl.searchParams.get('token');

    const auth = await validateToken(token);
    if (!auth) {
        ws.close(4401, 'unauthorized');
        return;
    }
    if (auth.guid !== room) {
        ws.close(4401, 'guid mismatch');
        return;
    }

    if (!rooms.has(room)) {
        rooms.set(room, new Set());
    }
    rooms.get(room).add(ws);

    console.log(`[Connexion] Client dans la room : ${room}`);

    // --- ENVOI DE L'ÉTAT ACTUEL AU NOUVEAU CLIENT ---
    const currentState = loadRoomData(room);
    const objects = Object.values(currentState);

    if (objects.length > 0) {
        console.log(`[${room}] Envoi de ${objects.length} objets existants.`);
        objects.forEach(obj => {
            ws.send(JSON.stringify(obj));
        });

        // ENVOI DU CATALOGUE D'IDS POUR NETTOYAGE
        // Format data: "type:id,type:id,..."
        const catalog = objects.map(obj => {
            try {
                const entity = JSON.parse(obj.data);
                return `${obj.type}:${entity.id}`;
            } catch(e) { return null; }
        }).filter(i => i !== null).join(',');

        ws.send(JSON.stringify({
            type: "system",
            action: "sync_catalog",
            data: catalog,
            origin: "server"
        }));
    }

    ws.on('message', (msg, isBinary) => {
        if (isBinary) return;

        let message;
        try {
            message = JSON.parse(msg.toString());
        } catch (e) { return; }

        if (message.type === 'system') {
            if (message.action === 'ping') return;
            return;
        }

        // --- PERSISTANCE ---
        const currentData = loadRoomData(room);
        const entityId = JSON.parse(message.data).id;
        const objectKey = `${message.type}_${entityId}`;

        if (message.action === 'put') {
            currentData[objectKey] = message;
        } else if (message.action === 'delete') {
            delete currentData[objectKey];
        }
        saveRoomData(room, currentData);

        // --- RELAIS ---
        rooms.get(room).forEach(client => {
            if (client !== ws && client.readyState === 1) {
                client.send(JSON.stringify(message));
            }
        });
    });

    ws.on('close', () => {
        rooms.get(room)?.delete(ws);
        if (rooms.get(room)?.size === 0) rooms.delete(room);
    });
});

console.log(`✅ Serveur de synchronisation v3 prêt sur le port ${PORT}`);
