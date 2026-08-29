package com.mystockmanager.app.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.data.local.entities.DomicileEntity
import com.mystockmanager.app.data.local.entities.ShopEntity
import com.mystockmanager.app.data.local.entities.ShoppingEntity
import com.mystockmanager.app.data.local.entities.StorageEntity
import com.mystockmanager.app.data.local.entities.UnitEntity
import com.mystockmanager.app.data.repository.PrefsRepository
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
    private val prefsRepository: PrefsRepository,
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

    val domiciles: StateFlow<List<DomicileEntity>> = stockRepository.getDomiciles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val units: StateFlow<List<UnitEntity>> = stockRepository.getUnits(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filterShopId = MutableStateFlow<String?>(null)
    val filterShopId = _filterShopId.asStateFlow()

    val isAggregated: StateFlow<Boolean> = prefsRepository.getPrefs(userId)
        .map { it?.isShoppingAggregated ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val shoppingItems: StateFlow<List<ShoppingEntity>> = combine(
        shoppingRepository.getShoppingList(userId),
        _filterShopId,
        isAggregated
    ) { items, filterId, aggregated ->
        var filtered = if (filterId == null) items
        else items.filter { it.shopId == filterId }
        
        if (aggregated) {
            filtered = aggregateItems(filtered)
        }
        
        // Sort: not checked first, then by date added
        filtered.sortedWith(compareBy<ShoppingEntity> { it.checked }.thenByDescending { it.addedAt })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun aggregateItems(items: List<ShoppingEntity>): List<ShoppingEntity> {
        // Group by name and unit
        return items.groupBy { "${it.name.lowercase().trim()}_${it.unit.lowercase().trim()}_${it.checked}" }
            .map { (key, group) ->
                if (group.size <= 1) group.first()
                else {
                    val totalQty = group.sumOf { it.quantity.toDoubleOrNull() ?: 0.0 }
                    group.first().copy(
                        id = "agg_${key}",
                        quantity = if (totalQty % 1.0 == 0.0) totalQty.toInt().toString() else totalQty.toString(),
                        requestorInitials = group.mapNotNull { it.requestorInitials }.distinct().joinToString(",")
                    )
                }
            }
    }

    fun setFilterShopId(shopId: String?) {
        _filterShopId.value = shopId
    }

    fun toggleAggregation() {
        viewModelScope.launch {
            val current = isAggregated.value
            prefsRepository.getPrefs(userId).first()?.let {
                prefsRepository.savePrefs(it.copy(isShoppingAggregated = !current))
            }
        }
    }

    fun refreshAutoRestock() {
        viewModelScope.launch {
            shoppingRepository.syncAutoRestock(userId)
        }
    }

    fun toggleChecked(item: ShoppingEntity) {
        viewModelScope.launch {
            if (item.id.startsWith("agg_")) {
                val group = getItemsForAggregated(item)
                group.forEach { shoppingRepository.toggleItem(it.id, !it.checked) }
            } else {
                shoppingRepository.toggleItem(item.id, !item.checked)
            }
        }
    }

    private suspend fun getItemsForAggregated(aggItem: ShoppingEntity): List<ShoppingEntity> {
        val allItems = shoppingRepository.getShoppingList(userId).first()
        return allItems.filter { 
            it.name.lowercase().trim() == aggItem.name.lowercase().trim() && 
            it.unit.lowercase().trim() == aggItem.unit.lowercase().trim() &&
            it.checked == aggItem.checked
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
                requestorInitials = sessionManager.getInitials(),
                targetStorageId = targetStorageId,
                addedAt = java.time.Instant.now().toString()
            )
            shoppingRepository.addShoppingItem(item)
        }
    }

    fun updateItem(item: ShoppingEntity) {
        if (item.id.startsWith("agg_")) return
        viewModelScope.launch {
            shoppingRepository.addShoppingItem(item)
        }
    }

    fun editFirstOfAggregated(aggItem: ShoppingEntity, onFound: (ShoppingEntity) -> Unit) {
        viewModelScope.launch {
            val first = getItemsForAggregated(aggItem).firstOrNull()
            if (first != null) {
                onFound(first)
            }
        }
    }

    fun deleteItem(item: ShoppingEntity) {
        viewModelScope.launch {
            if (item.id.startsWith("agg_")) {
                val group = getItemsForAggregated(item)
                group.forEach { shoppingRepository.deleteShoppingItem(it) }
            } else {
                shoppingRepository.deleteShoppingItem(item)
            }
        }
    }

    fun moveToStock(item: ShoppingEntity, storageId: String) {
        viewModelScope.launch {
            if (item.id.startsWith("agg_")) {
                val group = getItemsForAggregated(item)
                group.forEach { shoppingRepository.moveToStock(it, storageId) }
            } else {
                shoppingRepository.moveToStock(item, storageId)
            }
        }
    }
}
