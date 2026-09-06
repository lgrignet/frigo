package com.mystockmanager.app.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.data.local.entities.ShoppingEntity
import com.mystockmanager.app.data.remote.RecipeDto
import com.mystockmanager.app.data.repository.PrefsRepository
import com.mystockmanager.app.data.repository.RecipeRepository
import com.mystockmanager.app.data.repository.ShoppingRepository
import com.mystockmanager.app.data.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class RecipeIngredientState(
    val name: String,
    val quantity: Double?,
    val unit: String?,
    val inStock: Boolean,
    val checked: Boolean
)

/**
 * "En stock" est calculé en direct contre StockRepository.getItems (pas un
 * instantané figé au chargement) — si le stock change pendant que l'écran est
 * ouvert (ex. produit ajouté via ItemForm), les cases se mettent à jour toutes
 * seules pour les ingrédients que l'utilisateur n'a pas déjà cochés/décochés
 * manuellement (voir [_manualChecked]).
 */
@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val shoppingRepository: ShoppingRepository,
    private val stockRepository: StockRepository,
    private val prefsRepository: PrefsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val userId = sessionManager.getUserId().toString()

    private val _recipe = MutableStateFlow<RecipeDto?>(null)
    val recipe: StateFlow<RecipeDto?> = _recipe

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    /** Ingrédients dont l'état coché a été choisi explicitement par l'utilisateur (name -> checked). */
    private val _manualChecked = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    private fun normalize(s: String) = s.trim().lowercase()

    private val stockNames: Flow<Set<String>> = stockRepository.getItems(userId)
        .map { items -> items.map { normalize(it.name) }.toSet() }

    val ingredientStates: StateFlow<List<RecipeIngredientState>> = combine(
        _recipe, stockNames, _manualChecked
    ) { recipe, stock, manual ->
        recipe?.ingredients.orEmpty().map { ingredient ->
            val key = normalize(ingredient.name)
            val inStock = stock.any { s -> s.contains(key) || key.contains(s) }
            val checked = manual[ingredient.name] ?: !inStock
            RecipeIngredientState(ingredient.name, ingredient.quantity, ingredient.unit, inStock, checked)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var loaded = false

    fun load(recipeId: String) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            _loading.value = true
            try {
                val lang = prefsRepository.getPrefs(userId).first()?.lang ?: "fr"
                _recipe.value = recipeRepository.getRecipe(recipeId, lang)
            } catch (e: Exception) {
                _recipe.value = null
            } finally {
                _loading.value = false
            }
        }
    }

    fun toggleIngredient(state: RecipeIngredientState) {
        _manualChecked.value = _manualChecked.value + (state.name to !state.checked)
    }

    /** Marque la recette choisie et ajoute les ingrédients cochés à la liste de courses. */
    fun confirm() {
        val r = _recipe.value ?: return
        val checkedIngredients = ingredientStates.value.filter { it.checked }

        viewModelScope.launch {
            recipeRepository.chooseRecipe(r.id)
            val now = Instant.now().toString()
            checkedIngredients.forEach { state ->
                shoppingRepository.addShoppingItem(
                    ShoppingEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        name = state.name,
                        quantity = (state.quantity ?: 1.0).toString(),
                        unit = state.unit ?: "",
                        source = "recipe",
                        requestorInitials = sessionManager.getInitials(),
                        addedAt = now
                    )
                )
            }
            _saved.value = true
        }
    }
}
