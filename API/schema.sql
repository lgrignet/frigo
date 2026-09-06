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

-- === Recettes générées par IA (Gemini ou autre provider, voir src/ai/) ===

CREATE TABLE IF NOT EXISTS recettes (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    cuisine_type         text NOT NULL,               -- liste figée côté app (strings.xml), pas de CHECK ici
    langue_origine       char(2) NOT NULL CHECK (langue_origine IN ('fr','en','es','de','nl')),
    ingredient_principal text,                        -- nullable : absent pour une recherche multi-sélection sans priorité unique
    ingredients_cles     text[] NOT NULL,              -- ingrédients normalisés (minuscule, sans accent), pour le matching de cache
    servings             smallint,
    image_emoji          text,                          -- indépendant de la langue, pas de photo générée en v1
    created_at           timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_recettes_cuisine_type ON recettes (cuisine_type);
CREATE INDEX IF NOT EXISTS idx_recettes_ingredient_principal ON recettes (ingredient_principal);
CREATE INDEX IF NOT EXISTS idx_recettes_ingredients_cles ON recettes USING GIN (ingredients_cles);

CREATE TABLE IF NOT EXISTS recettes_traductions (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    recette_id  uuid NOT NULL REFERENCES recettes (id) ON DELETE CASCADE,
    langue      char(2) NOT NULL CHECK (langue IN ('fr','en','es','de','nl')),
    titre       text NOT NULL,
    ingredients jsonb NOT NULL,   -- [{"nom": "...", "quantite": 200, "unite": "g"}, ...]
    etapes      jsonb NOT NULL,   -- ["étape 1", "étape 2", ...] — ordre = index
    created_at  timestamptz NOT NULL DEFAULT now(),
    UNIQUE (recette_id, langue)
);

CREATE INDEX IF NOT EXISTS idx_recettes_traductions_recette_id ON recettes_traductions (recette_id);

-- Cooldown court : évite de reproposer les mêmes recettes à répétition immédiate
CREATE TABLE IF NOT EXISTS recettes_proposees_foyer (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    guid        text NOT NULL,           -- foyer (comptes.guid) ; pas de FK, guid n'est pas une clé naturelle unique
    recette_id  uuid NOT NULL REFERENCES recettes (id) ON DELETE CASCADE,
    proposed_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (guid, recette_id)
);

CREATE INDEX IF NOT EXISTS idx_recettes_proposees_foyer_guid ON recettes_proposees_foyer (guid);

-- Cooldown long : recette réellement choisie/cuisinée (historique conservé, pas UNIQUE : peut être re-choisie plus tard)
CREATE TABLE IF NOT EXISTS recettes_choisies_foyer (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    guid        text NOT NULL,
    recette_id  uuid NOT NULL REFERENCES recettes (id) ON DELETE CASCADE,
    chosen_at   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_recettes_choisies_foyer_guid ON recettes_choisies_foyer (guid);
CREATE INDEX IF NOT EXISTS idx_recettes_choisies_foyer_recette_id ON recettes_choisies_foyer (recette_id);

-- Observabilité + garde-fou de coût réel (quota par foyer)
CREATE TABLE IF NOT EXISTS ia_appels_log (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    provider            text NOT NULL,
    guid                text,
    cuisine_type        text,
    langue              char(2),
    recettes_generees   smallint NOT NULL DEFAULT 0,
    succes              boolean NOT NULL,
    erreur              text,
    created_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ia_appels_log_guid_created_at ON ia_appels_log (guid, created_at);
