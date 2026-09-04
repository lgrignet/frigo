-- account-service schema — MyStockManager
-- Exécuté en tant que mystockmanager_app (propriétaire des objets).

CREATE TABLE IF NOT EXISTS comptes (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email               text UNIQUE NOT NULL,
    password_hash       text NOT NULL,          -- base64
    password_salt       text NOT NULL,          -- hex
    recovery_code_hash  text,                   -- base64, nullable
    recovery_code_salt  text,                   -- hex, nullable
    nom                 text,
    prenom              text,
    email_verifie       boolean NOT NULL DEFAULT false,
    guid                text,
    created_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_comptes_guid ON comptes (guid);

CREATE TABLE IF NOT EXISTS device_tokens (
    token           text PRIMARY KEY,
    compte_id       uuid NOT NULL REFERENCES comptes (id) ON DELETE CASCADE,
    nom_appareil    text,
    created_at      timestamptz NOT NULL DEFAULT now(),
    last_active_at  timestamptz,
    revoked_at      timestamptz
);

CREATE INDEX IF NOT EXISTS idx_device_tokens_compte_id ON device_tokens (compte_id);

CREATE TABLE IF NOT EXISTS codes_verification (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    compte_id   uuid NOT NULL REFERENCES comptes (id) ON DELETE CASCADE,
    code_hash   text NOT NULL,
    code_salt   text NOT NULL,
    type        text NOT NULL CHECK (type IN ('email_verification', 'password_recovery')),
    expires_at  timestamptz NOT NULL,
    used_at     timestamptz,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_codes_verification_compte_id ON codes_verification (compte_id);
