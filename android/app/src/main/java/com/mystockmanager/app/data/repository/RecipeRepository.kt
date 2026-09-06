package com.mystockmanager.app.data.repository

import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.data.remote.MyRecipesResponse
import com.mystockmanager.app.data.remote.RecipeApi
import com.mystockmanager.app.data.remote.RecipeApiException
import com.mystockmanager.app.data.remote.RecipeDto
import com.mystockmanager.app.data.remote.RecipeSearchResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(
    private val recipeApi: RecipeApi,
    private val sessionManager: SessionManager
) {
    suspend fun searchRecipes(
        ingredients: List<String>,
        priorityIngredients: List<String>,
        cuisineTypes: List<String>,
        language: String,
        count: Int = 5
    ): RecipeSearchResponse {
        val token = sessionManager.getDeviceToken() ?: throw RecipeApiException("Non authentifié.", 401)
        return recipeApi.search(token, ingredients, priorityIngredients, cuisineTypes, language, count)
    }

    /** Marque une recette comme choisie (best-effort — ne bloque pas l'utilisateur si le réseau flanche). */
    suspend fun chooseRecipe(recipeId: String) {
        val token = sessionManager.getDeviceToken() ?: return
        try {
            recipeApi.choose(token, recipeId)
        } catch (e: Exception) {
            // Non bloquant : au pire la recette sera reproposée un peu plus tôt que prévu.
        }
    }

    suspend fun getRecipe(recipeId: String, language: String): RecipeDto {
        val token = sessionManager.getDeviceToken() ?: throw RecipeApiException("Non authentifié.", 401)
        return recipeApi.getRecipe(token, recipeId, language)
    }

    suspend fun getMyRecipes(language: String, limit: Int = 30): MyRecipesResponse {
        val token = sessionManager.getDeviceToken() ?: throw RecipeApiException("Non authentifié.", 401)
        return recipeApi.getMine(token, language, limit)
    }
}
