package com.mystockmanager.app.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.data.local.entities.ShopEntity
import com.mystockmanager.app.data.local.entities.ShoppingEntity
import com.mystockmanager.app.data.local.entities.StorageEntity
import com.mystockmanager.app.data.local.entities.UnitEntity
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

    val shops: StateFlow<List<ShopEntity>> = stockRepository.getShops(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storages: StateFlow<List<StorageEntity>> = stockRepository.getStorages(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val units: StateFlow<List<UnitEntity>> = stockRepository.getUnits(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filterShopId = MutableStateFlow<String?>(null)
    val filterShopId = _filterShopId.asStateFlow()

    val shoppingItems: StateFlow<List<ShoppingEntity>> = combine(
        shoppingRepository.getShoppingList(userId),
        _filterShopId
    ) { items, filterId ->
        val filtered = if (filterId == null) items
        else items.filter { it.shopId == filterId }
        
        // Sort: not checked first, then by date added
        filtered.sortedWith(compareBy<ShoppingEntity> { it.checked }.thenByDescending { it.addedAt })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilterShopId(shopId: String?) {
        _filterShopId.value = shopId
    }

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

    fun addItem(name: String, quantity: String, unit: String, shopId: String? = null, targetStorageId: String? = null) {
        viewModelScope.launch {
            val item = ShoppingEntity(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                quantity = quantity,
                unit = unit,
                shopId = shopId,
                targetStorageId = targetStorageId,
                addedAt = java.time.Instant.now().toString()
            )
            shoppingRepository.addShoppingItem(item)
        }
    }

    fun updateItem(item: ShoppingEntity) {
        viewModelScope.launch {
            shoppingRepository.addShoppingItem(item)
        }
    }

    fun deleteItem(item: ShoppingEntity) {
        viewModelScope.launch {
            shoppingRepository.deleteShoppingItem(item)
        }
    }

    fun moveToStock(item: ShoppingEntity, storageId: String) {
        viewModelScope.launch {
            shoppingRepository.moveToStock(item, storageId)
        }
    }
}
