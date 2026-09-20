package com.mystockmanager.app.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.data.remote.RecipeDto
import com.mystockmanager.app.data.repository.PrefsRepository
import com.mystockmanager.app.data.repository.RecipeRepository
import com.mystockmanager.app.data.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RecipeResultsUiState {
    object Loading : RecipeResultsUiState()
    object Error : RecipeResultsUiState()
    data class Success(
        val recipes: List<RecipeDto>,
        val degraded: Boolean,
        val degradedReason: String? = null
    ) : RecipeResultsUiState()
}

@HiltViewModel
class RecipeResultsViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val stockRepository: StockRepository,
    private val prefsRepository: PrefsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val userId = sessionManager.getUserId().toString()

    private val _uiState = MutableStateFlow<RecipeResultsUiState>(RecipeResultsUiState.Loading)
    val uiState: StateFlow<RecipeResultsUiState> = _uiState

    private var searched = false
    private var lastItemIds: List<String> = emptyList()
    private var lastCuisineTypes: List<String> = emptyList()

    /** [itemIds] : produits sélectionnés/en priorité — le reste du stock complète le pool d'ingrédients disponibles. */
    fun search(itemIds: List<String>, cuisineTypes: List<String>) {
        if (searched) return
        searched = true
        lastItemIds = itemIds
        lastCuisineTypes = cuisineTypes

        viewModelScope.launch {
            _uiState.value = RecipeResultsUiState.Loading
            try {
                val allItems = stockRepository.getItems(userId).first()
                val poolNames = allItems.map { it.name }.filter { it.isNotBlank() }
                val priorityNames = allItems.filter { itemIds.contains(it.id) }.map { it.name }
                    .ifEmpty { poolNames.take(1) }
                val lang = prefsRepository.getPrefs(userId).first()?.lang ?: "fr"

                val response = recipeRepository.searchRecipes(
                    ingredients = poolNames,
                    priorityIngredients = priorityNames,
                    cuisineTypes = cuisineTypes,
                    language = lang
                )
                _uiState.value = RecipeResultsUiState.Success(response.recipes, response.degraded, response.degradedReason)
            } catch (e: Exception) {
                _uiState.value = RecipeResultsUiState.Error
            }
        }
    }

    /** À appeler après qu'une pub récompensée a été regardée jusqu'au bout : réclame le bonus puis relance la recherche. */
    fun claimAdBonusAndRetry() {
        viewModelScope.launch {
            try {
                recipeRepository.claimAdBonus()
                searched = false
                search(lastItemIds, lastCuisineTypes)
            } catch (e: Exception) {
                // Best-effort : en cas d'échec, l'utilisateur reste sur l'état dégradé actuel et peut retenter la pub.
            }
        }
    }
}
