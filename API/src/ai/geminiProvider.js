const BASE_URL = 'https://generativelanguage.googleapis.com/v1beta/models';
const TIMEOUT_MS = 15_000;

const LANG_NAMES = { fr: 'français', en: 'anglais', es: 'espagnol', de: 'allemand', nl: 'néerlandais' };

const RECIPE_SCHEMA = {
    type: 'OBJECT',
    properties: {
        recipes: {
            type: 'ARRAY',
            items: {
                type: 'OBJECT',
                properties: {
                    title: { type: 'STRING' },
                    cuisineType: { type: 'STRING' },
                    servings: { type: 'INTEGER' },
                    ingredients: {
                        type: 'ARRAY',
                        items: {
                            type: 'OBJECT',
                            properties: {
                                name: { type: 'STRING' },
                                quantity: { type: 'NUMBER' },
                                unit: { type: 'STRING' },
                            },
                            required: ['name', 'quantity', 'unit'],
                        },
                    },
                    steps: { type: 'ARRAY', items: { type: 'STRING' } },
                    imageEmoji: { type: 'STRING' },
                },
                required: ['title', 'cuisineType', 'ingredients', 'steps'],
            },
        },
    },
    required: ['recipes'],
};

/**
 * Appelle Gemini en mode sortie JSON structurée (generationConfig.responseSchema),
 * ce qui évite tout parsing ad-hoc fragile d'un texte libre. Timeout explicite via
 * AbortController : le fetch natif de Node n'en a pas par défaut.
 */
async function callGemini(prompt) {
    const apiKey = process.env.GEMINI_API_KEY;
    const model = process.env.GEMINI_MODEL || 'gemini-2.0-flash';
    if (!apiKey) throw new Error('GEMINI_API_KEY manquante.');

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), TIMEOUT_MS);

    try {
        const res = await fetch(`${BASE_URL}/${model}:generateContent?key=${apiKey}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                contents: [{ parts: [{ text: prompt }] }],
                generationConfig: {
                    responseMimeType: 'application/json',
                    responseSchema: RECIPE_SCHEMA,
                },
            }),
            signal: controller.signal,
        });

        if (!res.ok) {
            throw new Error(`Gemini a répondu ${res.status}`);
        }

        const data = await res.json();
        const text = data?.candidates?.[0]?.content?.parts?.[0]?.text;
        if (!text) throw new Error('Réponse Gemini vide ou inattendue.');

        const parsed = JSON.parse(text);
        return Array.isArray(parsed.recipes) ? parsed.recipes : [];
    } finally {
        clearTimeout(timeout);
    }
}

function buildSearchPrompt({ ingredients, priorityIngredients, cuisineTypes, language, count, excludeTitles }) {
    const langName = LANG_NAMES[language] || 'français';
    const lines = [
        `Tu es un assistant culinaire. Réponds uniquement en ${langName}, y compris les noms d'ingrédients et les étapes.`,
        `Propose exactement ${count} recette(s), chacune appartenant à L'UN de ces types de cuisine (mélange autorisé entre les recettes) : ${cuisineTypes.join(', ')}.`,
        `Pour chaque recette, indique dans le champ "cuisineType" exactement lequel de ces mots tu as utilisé (recopie-le tel quel, sans le traduire) : ${cuisineTypes.join(', ')}.`,
        `Ingrédients à utiliser en priorité (chaque recette doit utiliser au moins un de ceux-ci) : ${priorityIngredients.join(', ')}.`,
        `Autres ingrédients disponibles à utiliser si pertinent, sans obligation : ${ingredients.join(', ')}.`,
        `Pour chaque recette : un titre, le nombre de portions, la liste complète des ingrédients avec quantité numérique et unité, les étapes de préparation dans l'ordre, et un seul emoji représentatif du plat (imageEmoji).`,
    ];
    if (excludeTitles?.length) {
        lines.push(`Ne propose aucune recette dont le titre ou le concept ressemble à celles déjà proposées : ${excludeTitles.join(', ')}.`);
    }
    return lines.join('\n');
}

function buildTranslatePrompt({ titre, ingredients, etapes, targetLanguage }) {
    const langName = LANG_NAMES[targetLanguage] || targetLanguage;
    return [
        `Traduis fidèlement cette recette en ${langName}.`,
        `Ne modifie ni n'ajoute ni ne retire aucun ingrédient, et conserve exactement les mêmes quantités et unités (converties dans leur libellé linguistique uniquement, jamais dans leur valeur).`,
        `Réponds avec un tableau "recipes" contenant un seul élément, au même format que d'habitude.`,
        `Titre original : ${titre}`,
        `Ingrédients (JSON, à traduire champ "name" uniquement) : ${JSON.stringify(ingredients)}`,
        `Étapes (JSON) : ${JSON.stringify(etapes)}`,
    ].join('\n');
}

export async function generateRecipes({ ingredients, priorityIngredients, cuisineTypes, language, count, excludeTitles }) {
    const prompt = buildSearchPrompt({ ingredients, priorityIngredients, cuisineTypes, language, count, excludeTitles });
    return callGemini(prompt);
}

export async function translateRecipe({ titre, ingredients, etapes, targetLanguage }) {
    const prompt = buildTranslatePrompt({ titre, ingredients, etapes, targetLanguage });
    const recipes = await callGemini(prompt);
    return recipes[0] || null;
}
