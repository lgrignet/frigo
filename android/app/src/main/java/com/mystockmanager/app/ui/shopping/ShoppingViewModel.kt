package com.mystockmanager.app.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.data.local.entities.ShoppingEntity
import com.mystockmanager.app.data.repository.ShoppingRepository
import com.mystockmanager.app.data.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val stockRepository: StockRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val userId = sessionManager.getUserId().toString()

    init {
        // Déclencher le réassort auto dès qu'un produit change en base
        stockRepository.getItems(userId)
            .onEach { refreshAutoRestock() }
            .launchIn(viewModelScope)
    }

    val shoppingItems: StateFlow<List<ShoppingEntity>> = shoppingRepository.getShoppingList(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshAutoRestock() {
        viewModelScope.launch {
            shoppingRepository.syncAutoRestock(userId)
        }
    }

    fun toggleChecked(item: ShoppingEntity) {
        viewModelScope.launch {
            shoppingRepository.toggleItem(item.id, !item.checked)
        }
    }

    fun addItem(name: String, quantity: String, unit: String) {
        viewModelScope.launch {
            val item = ShoppingEntity(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                quantity = quantity,
                unit = unit,
                addedAt = java.time.Instant.now().toString()
            )
            shoppingRepository.addShoppingItem(item)
        }
    }

    fun updateItem(item: ShoppingEntity) {
        viewModelScope.launch {
            shoppingRepository.addShoppingItem(item) // REPLACE logic
        }
    }

    fun deleteItem(item: ShoppingEntity) {
        viewModelScope.launch {
            shoppingRepository.deleteShoppingItem(item)
        }
    }
}
