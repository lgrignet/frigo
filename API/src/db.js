import pg from 'pg';

/**
 * Pool PostgreSQL. La chaîne de connexion vient de DATABASE_URL (.env),
 * pointe sur 127.0.0.1:5432, base mystockmanager_accounts, rôle applicatif dédié.
 */
export const pool = new pg.Pool({
    connectionString: process.env.DATABASE_URL,
    max: 10,
    idleTimeoutMillis: 30_000,
});

pool.on('error', (err) => {
    console.error('[db] erreur inattendue sur un client idle du pool', err);
});

export function query(text, params) {
    return pool.query(text, params);
}

/** Exécute `fn` dans une transaction, avec COMMIT/ROLLBACK automatique. */
export async function withTransaction(fn) {
    const client = await pool.connect();
    try {
        await client.query('BEGIN');
        const result = await fn(client);
        await client.query('COMMIT');
        return result;
    } catch (err) {
        await client.query('ROLLBACK');
        throw err;
    } finally {
        client.release();
    }
}
