import express from 'express';
import rateLimit from 'express-rate-limit';
import { pool, query, withTransaction } from './db.js';
import { generateRecipes as aiGenerateRecipes, translateRecipe as aiTranslateRecipe } from './ai/index.js';

export const router = express.Router();

const SUPPORTED_LANGUAGES = ['fr', 'en', 'es', 'de', 'nl'];
const COOLDOWN_PROPOSED_HOURS = Number(process.env.RECIPE_COOLDOWN_PROPOSED_HOURS) || 24;
const COOLDOWN_CHOSEN_DAYS = Number(process.env.RECIPE_COOLDOWN_CHOSEN_DAYS) || 30;
const QUOTA_PER_FOYER_PER_DAY = Number(process.env.RECIPE_QUOTA_PER_FOYER_PER_DAY) || 20;
// Une pub doit débloquer une recherche complète (5 recettes, le count par défaut
// utilisé par l'app), pas juste 1 recette isolée — le quota est compté en recettes
// générées, pas en recherches, voir quotaRemaining() plus bas.
const AD_BONUS = Number(process.env.RECIPE_AD_BONUS) || 5;

// --- Rate limiting générique par IP (anti-abus), cohérent avec /account/* ---
const recipesLimiter = rateLimit({
    windowMs: Number(process.env.RECIPE_RATE_LIMIT_WINDOW_MS) || 15 * 60 * 1000,
    max: Number(process.env.RECIPE_RATE_LIMIT_MAX) || 30,
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Trop de requêtes, réessayez plus tard.' },
});
router.use(recipesLimiter);

// --- Helpers ---
function isNonEmptyString(v) {
    return typeof v === 'string' && v.trim().length > 0;
}

function isStringArray(v) {
    return Array.isArray(v) && v.length > 0 && v.every((x) => typeof x === 'string' && x.trim().length > 0);
}

/** Minuscules, sans accents, singulier grossier — pour le matching de cache uniquement, jamais affiché. */
function normalizeIngredient(name) {
    return name
        .normalize('NFD').replace(/\p{Diacritic}/gu, '') // enleve les accents (decomposition NFD + suppression des marques diacritiques Unicode)
        .toLowerCase()
        .trim()
        .replace(/s$/, '');
}

function normalizedSortedKeys(names) {
    return [...new Set(names.map(normalizeIngredient))].sort();
}

// wrapper async -> évite la répétition du try/catch (même pattern que server.js)
function h(fn) {
    return (req, res) => fn(req, res).catch((err) => {
        console.error(`[recipes ${req.method} ${req.path}]`, err);
        res.status(500).json({ error: 'Erreur interne.' });
    });
}

/** Résout compte_id + guid à partir du device_token, et les attache à req. */
async function requireDevice(req, res, next) {
    const auth = req.get('Authorization') || '';
    const token = auth.startsWith('Bearer ') ? auth.slice(7).trim() : null;
    if (!isNonEmptyString(token)) {
        return res.status(401).json({ error: 'Non authentifié.' });
    }

    const { rows } = await pool.query(
        `SELECT dt.compte_id, c.guid FROM device_tokens dt
         JOIN comptes c ON c.id = dt.compte_id
         WHERE dt.token = $1 AND dt.revoked_at IS NULL`,
        [token],
    );
    const row = rows[0];
    if (!row) return res.status(401).json({ error: 'Non authentifié.' });
    if (!row.guid) return res.status(400).json({ error: "Aucun foyer n'est encore rattaché à ce compte." });

    req.compteId = row.compte_id;
    req.guid = row.guid;
    next();
}

function rowToRecipe(row) {
    return {
        id: row.id,
        title: row.titre,
        cuisineType: row.cuisine_type,
        servings: row.servings,
        ingredients: row.ingredients,
        steps: row.etapes,
        imageEmoji: row.image_emoji,
    };
}

/** Recettes déjà en cache pour ce foyer/cuisines/langue, hors cooldowns (court + long). */
async function findCachedRecipes({ cuisineTypes, priorityKeys, guid, language, limit }) {
    const { rows } = await pool.query(
        `SELECT r.id, r.cuisine_type, r.servings, r.image_emoji, rt.titre, rt.ingredients, rt.etapes
         FROM recettes r
         JOIN recettes_traductions rt ON rt.recette_id = r.id AND rt.langue = $1
         WHERE r.cuisine_type = ANY($2::text[])
           AND r.ingredients_cles && $3::text[]
           AND r.id NOT IN (
               SELECT recette_id FROM recettes_proposees_foyer
               WHERE guid = $4 AND proposed_at > now() - ($5 || ' hours')::interval
               UNION
               SELECT recette_id FROM recettes_choisies_foyer
               WHERE guid = $4 AND chosen_at > now() - ($6 || ' days')::interval
           )
         ORDER BY random()
         LIMIT $7`,
        [language, cuisineTypes, priorityKeys, guid, COOLDOWN_PROPOSED_HOURS, COOLDOWN_CHOSEN_DAYS, limit],
    );
    return rows.map(rowToRecipe);
}

async function quotaRemaining(guid) {
    const { rows } = await query(
        `SELECT
             (SELECT COALESCE(SUM(recettes_generees), 0) FROM ia_appels_log
              WHERE guid = $1 AND succes = true AND created_at > now() - interval '1 day') AS utilise,
             (SELECT COALESCE(SUM(montant), 0) FROM bonus_quota_foyer
              WHERE guid = $1 AND created_at > now() - interval '1 day') AS bonus`,
        [guid],
    );
    const { utilise, bonus } = rows[0] || {};
    return QUOTA_PER_FOYER_PER_DAY + Number(bonus || 0) - Number(utilise || 0);
}

async function logAiCall({ guid, cuisineTypes, language, generatedCount, success, error }) {
    await query(
        `INSERT INTO ia_appels_log (provider, guid, cuisine_type, langue, recettes_generees, succes, erreur)
         VALUES ($1, $2, $3, $4, $5, $6, $7)`,
        [process.env.AI_PROVIDER || 'gemini', guid, cuisineTypes.join(','), language, generatedCount, success, error || null],
    );
}

/**
 * Insère une recette générée par l'IA en base, en réutilisant une recette
 * existante (dédup exacte par cuisine + ingrédient principal + ensemble
 * d'ingrédients normalisés) si elle existe déjà — on ajoute alors juste la
 * traduction dans cette langue si elle manquait. [cuisineTypes] est l'ensemble
 * demandé ; la recette est classée sous celui que l'IA a effectivement choisi
 * (recipe.cuisineType), avec repli sur le premier demandé si absent/invalide.
 */
async function saveGeneratedRecipe({ recipe, cuisineTypes, language, priorityKeys }) {
    const effectiveCuisineType = cuisineTypes.includes(recipe.cuisineType) ? recipe.cuisineType : cuisineTypes[0];
    const ingredientNames = (recipe.ingredients || []).map((i) => i.name);
    const ingredientsCles = normalizedSortedKeys(ingredientNames);
    const ingredientPrincipal = priorityKeys.find((k) => ingredientsCles.includes(k)) || null;

    return withTransaction(async (client) => {
        const { rows: existingRows } = await client.query(
            `SELECT id FROM recettes
             WHERE cuisine_type = $1
               AND ingredient_principal IS NOT DISTINCT FROM $2
               AND ingredients_cles = $3::text[]
             LIMIT 1`,
            [effectiveCuisineType, ingredientPrincipal, ingredientsCles],
        );

        let recetteId = existingRows[0]?.id;

        if (!recetteId) {
            const { rows } = await client.query(
                `INSERT INTO recettes (cuisine_type, langue_origine, ingredient_principal, ingredients_cles, servings, image_emoji)
                 VALUES ($1, $2, $3, $4, $5, $6)
                 RETURNING id`,
                [effectiveCuisineType, language, ingredientPrincipal, ingredientsCles, recipe.servings || null, recipe.imageEmoji || null],
            );
            recetteId = rows[0].id;
        }

        await client.query(
            `INSERT INTO recettes_traductions (recette_id, langue, titre, ingredients, etapes)
             VALUES ($1, $2, $3, $4, $5)
             ON CONFLICT (recette_id, langue) DO NOTHING`,
            [recetteId, language, recipe.title, JSON.stringify(recipe.ingredients || []), JSON.stringify(recipe.steps || [])],
        );

        return {
            id: recetteId,
            title: recipe.title,
            cuisineType: effectiveCuisineType,
            servings: recipe.servings || null,
            ingredients: recipe.ingredients || [],
            steps: recipe.steps || [],
            imageEmoji: recipe.imageEmoji || null,
        };
    });
}

// --- POST /recipes/search ---
router.post('/search', requireDevice, h(async (req, res) => {
    const { ingredients, priorityIngredients, cuisineTypes, language, count } = req.body || {};

    if (!isStringArray(ingredients)) {
        return res.status(400).json({ error: 'ingredients requis (tableau de chaînes non vide).' });
    }
    if (!isStringArray(priorityIngredients)) {
        return res.status(400).json({ error: 'priorityIngredients requis (tableau de chaînes non vide).' });
    }
    if (!isStringArray(cuisineTypes)) {
        return res.status(400).json({ error: 'cuisineTypes requis (tableau de chaînes non vide).' });
    }
    if (!SUPPORTED_LANGUAGES.includes(language)) {
        return res.status(400).json({ error: `language invalide (${SUPPORTED_LANGUAGES.join(', ')}).` });
    }
    const wantedCount = Number.isInteger(count) && count > 0 && count <= 10 ? count : 5;
    const cuisineTypesCapped = cuisineTypes.slice(0, 5);

    const priorityKeys = normalizedSortedKeys(priorityIngredients);

    let recipes = await findCachedRecipes({
        cuisineTypes: cuisineTypesCapped,
        priorityKeys,
        guid: req.guid,
        language,
        limit: wantedCount,
    });

    let degraded = false;
    let degradedReason = null;

    if (recipes.length < wantedCount) {
        const remaining = wantedCount - recipes.length;
        const remainingQuota = await quotaRemaining(req.guid);

        if (remainingQuota <= 0) {
            degraded = true;
            degradedReason = 'quota_exceeded';
        } else {
            const requestedCount = Math.min(remaining, remainingQuota);
            try {
                const generated = await aiGenerateRecipes({
                    ingredients: ingredients.map(String),
                    priorityIngredients,
                    cuisineTypes: cuisineTypesCapped,
                    language,
                    count: requestedCount,
                    excludeTitles: recipes.map((r) => r.title),
                });

                const saved = [];
                for (const recipe of generated) {
                    saved.push(await saveGeneratedRecipe({ recipe, cuisineTypes: cuisineTypesCapped, language, priorityKeys }));
                }
                recipes = recipes.concat(saved);

                await logAiCall({ guid: req.guid, cuisineTypes: cuisineTypesCapped, language, generatedCount: saved.length, success: true });

                // Le quota a limité le nombre de recettes demandées à l'IA (moins que
                // ce qu'il aurait fallu pour compléter wantedCount) : la recherche a
                // "réussi" mais est incomplète pour cette raison, il faut le signaler
                // au client au même titre qu'un quota totalement épuisé.
                if (requestedCount < remaining) {
                    degraded = true;
                    degradedReason = 'quota_exceeded';
                }
            } catch (err) {
                console.error('[recipes/search] échec appel IA', err);
                degraded = true;
                degradedReason = 'ai_unavailable';
                await logAiCall({ guid: req.guid, cuisineTypes: cuisineTypesCapped, language, generatedCount: 0, success: false, error: String(err.message || err) });
            }
        }
    }

    recipes = recipes.slice(0, wantedCount);

    if (recipes.length > 0) {
        await withTransaction(async (client) => {
            for (const r of recipes) {
                await client.query(
                    `INSERT INTO recettes_proposees_foyer (guid, recette_id) VALUES ($1, $2)
                     ON CONFLICT (guid, recette_id) DO UPDATE SET proposed_at = now()`,
                    [req.guid, r.id],
                );
            }
        });
    }

    res.json({ recipes, degraded, degradedReason });
}));

// --- POST /recipes/ad-bonus ---
// Appelé après qu'une pub récompensée a été regardée jusqu'au bout (onUserEarnedReward
// côté client). Le montant du bonus est décidé ici, jamais fourni par le client.
router.post('/ad-bonus', requireDevice, h(async (req, res) => {
    await query(
        'INSERT INTO bonus_quota_foyer (guid, montant) VALUES ($1, $2)',
        [req.guid, AD_BONUS],
    );
    const remaining = await quotaRemaining(req.guid);
    res.json({ bonusGranted: AD_BONUS, quotaRemaining: remaining });
}));

// --- POST /recipes/:id/choose ---
router.post('/:id/choose', requireDevice, h(async (req, res) => {
    const { rows } = await query('SELECT id FROM recettes WHERE id = $1', [req.params.id]);
    if (!rows[0]) return res.status(404).json({ error: 'Recette introuvable.' });

    await query(
        'INSERT INTO recettes_choisies_foyer (guid, recette_id) VALUES ($1, $2)',
        [req.guid, req.params.id],
    );

    res.json({ ok: true });
}));

// --- GET /recipes/mine?language=fr ---
// Recettes déjà choisies par ce foyer, les plus récentes d'abord — enregistrées
// via /recipes/:id/choose. DOIT rester déclarée avant /:id (sinon Express
// interprète "mine" comme une valeur de :id).
router.get('/mine', requireDevice, h(async (req, res) => {
    const language = req.query.language;
    if (!SUPPORTED_LANGUAGES.includes(language)) {
        return res.status(400).json({ error: `language invalide (${SUPPORTED_LANGUAGES.join(', ')}).` });
    }
    const limit = Math.min(Number(req.query.limit) || 30, 100);

    const { rows } = await pool.query(
        `SELECT r.id, r.cuisine_type, r.servings, r.image_emoji, rt.titre, rt.ingredients, rt.etapes, latest.chosen_at
         FROM (
             SELECT recette_id, MAX(chosen_at) AS chosen_at
             FROM recettes_choisies_foyer
             WHERE guid = $1
             GROUP BY recette_id
         ) latest
         JOIN recettes r ON r.id = latest.recette_id
         JOIN recettes_traductions rt ON rt.recette_id = r.id AND rt.langue = $2
         ORDER BY latest.chosen_at DESC
         LIMIT $3`,
        [req.guid, language, limit],
    );

    res.json({
        recipes: rows.map((row) => ({ ...rowToRecipe(row), chosenAt: row.chosen_at })),
    });
}));

// --- GET /recipes/:id?language=fr ---
router.get('/:id', requireDevice, h(async (req, res) => {
    const language = req.query.language;
    if (!SUPPORTED_LANGUAGES.includes(language)) {
        return res.status(400).json({ error: `language invalide (${SUPPORTED_LANGUAGES.join(', ')}).` });
    }

    const { rows: recetteRows } = await query('SELECT * FROM recettes WHERE id = $1', [req.params.id]);
    const recette = recetteRows[0];
    if (!recette) return res.status(404).json({ error: 'Recette introuvable.' });

    const { rows: traductionRows } = await query(
        'SELECT * FROM recettes_traductions WHERE recette_id = $1 AND langue = $2',
        [req.params.id, language],
    );
    let traduction = traductionRows[0];

    if (!traduction) {
        const { rows: anyTraductionRows } = await query(
            'SELECT * FROM recettes_traductions WHERE recette_id = $1 LIMIT 1',
            [req.params.id],
        );
        const source = anyTraductionRows[0];
        if (!source) return res.status(404).json({ error: 'Recette introuvable.' });

        const translated = await aiTranslateRecipe({
            titre: source.titre,
            ingredients: source.ingredients,
            etapes: source.etapes,
            targetLanguage: language,
        });
        if (!translated) return res.status(502).json({ error: 'Traduction indisponible pour le moment.' });

        const { rows: insertedRows } = await query(
            `INSERT INTO recettes_traductions (recette_id, langue, titre, ingredients, etapes)
             VALUES ($1, $2, $3, $4, $5)
             ON CONFLICT (recette_id, langue) DO NOTHING
             RETURNING *`,
            [req.params.id, language, translated.title, JSON.stringify(translated.ingredients || source.ingredients), JSON.stringify(translated.steps || source.etapes)],
        );
        traduction = insertedRows[0] || (await query(
            'SELECT * FROM recettes_traductions WHERE recette_id = $1 AND langue = $2',
            [req.params.id, language],
        )).rows[0];
    }

    res.json(rowToRecipe({
        id: recette.id,
        cuisine_type: recette.cuisine_type,
        servings: recette.servings,
        image_emoji: recette.image_emoji,
        titre: traduction.titre,
        ingredients: traduction.ingredients,
        etapes: traduction.etapes,
    }));
}));
