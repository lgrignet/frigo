package com.mystockmanager.app.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.data.remote.RecipeDto
import com.mystockmanager.app.data.repository.PrefsRepository
import com.mystockmanager.app.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MyRecipesUiState {
    object Loading : MyRecipesUiState()
    object Error : MyRecipesUiState()
    data class Success(val recipes: List<RecipeDto>) : MyRecipesUiState()
}

/** Liste des recettes déjà choisies par ce foyer — accessible depuis l'icône globale, quel que soit l'onglet. */
@HiltViewModel
class MyRecipesViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val prefsRepository: PrefsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val userId = sessionManager.getUserId().toString()

    private val _uiState = MutableStateFlow<MyRecipesUiState>(MyRecipesUiState.Loading)
    val uiState: StateFlow<MyRecipesUiState> = _uiState

    init {
        viewModelScope.launch {
            try {
                val lang = prefsRepository.getPrefs(userId).first()?.lang ?: "fr"
                val response = recipeRepository.getMyRecipes(lang)
                _uiState.value = MyRecipesUiState.Success(response.recipes)
            } catch (e: Exception) {
                _uiState.value = MyRecipesUiState.Error
            }
        }
    }
}
