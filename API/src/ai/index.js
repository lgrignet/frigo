import * as gemini from './geminiProvider.js';

/**
 * Point d'entrée unique vers le fournisseur IA actif — voir AI_PROVIDER dans
 * .env. Ajouter un fournisseur = un nouveau fichier exposant les deux mêmes
 * fonctions (generateRecipes, translateRecipe) + une entrée dans PROVIDERS,
 * aucun autre code appelant à changer.
 */
const PROVIDERS = {
    gemini,
};

function getProvider() {
    const name = process.env.AI_PROVIDER || 'gemini';
    const provider = PROVIDERS[name];
    if (!provider) {
        throw new Error(`Fournisseur IA inconnu : "${name}" (AI_PROVIDER). Valeurs possibles : ${Object.keys(PROVIDERS).join(', ')}.`);
    }
    return provider;
}

export function generateRecipes(params) {
    return getProvider().generateRecipes(params);
}

export function translateRecipe(params) {
    return getProvider().translateRecipe(params);
}
