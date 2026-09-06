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
    data class Success(val recipes: List<RecipeDto>, val degraded: Boolean) : RecipeResultsUiState()
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

    /** [itemIds] : produits sélectionnés/en priorité — le reste du stock complète le pool d'ingrédients disponibles. */
    fun search(itemIds: List<String>, cuisineType: String) {
        if (searched) return
        searched = true

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
                    cuisineType = cuisineType,
                    language = lang
                )
                _uiState.value = RecipeResultsUiState.Success(response.recipes, response.degraded)
            } catch (e: Exception) {
                _uiState.value = RecipeResultsUiState.Error
            }
        }
    }
}
