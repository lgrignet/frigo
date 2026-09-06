package com.mystockmanager.app.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.data.local.entities.DomicileEntity
import com.mystockmanager.app.data.local.entities.ExpiryHistoryEntity
import com.mystockmanager.app.data.local.entities.ItemEntity
import com.mystockmanager.app.data.local.entities.ShoppingEntity
import com.mystockmanager.app.data.local.entities.StorageEntity
import com.mystockmanager.app.data.repository.ExpiryHistoryRepository
import com.mystockmanager.app.data.repository.PrefsRepository
import com.mystockmanager.app.data.repository.ShoppingRepository
import com.mystockmanager.app.data.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AllItemsViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val shoppingRepository: ShoppingRepository,
    private val prefsRepository: PrefsRepository,
    private val expiryHistoryRepository: ExpiryHistoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val userId = sessionManager.getUserId().toString()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _expiryCleared = MutableSharedFlow<ExpiryHistoryEntity>()
    /** Émis juste après qu'une date de péremption a été effacée (quantité tombée à 0) — pour un snackbar « Annuler » immédiat. */
    val expiryCleared = _expiryCleared.asSharedFlow()

    /** Dates de péremption effacées dans les 10 dernières minutes, restaurables. */
    val recentExpiryClears: StateFlow<List<ExpiryHistoryEntity>> = expiryHistoryRepository.getRecent(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dateFormat: StateFlow<String> = prefsRepository.getPrefs(userId)
        .map { it?.dateFormat ?: "european" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "european")

    init {
        viewModelScope.launch { expiryHistoryRepository.purgeExpired(userId) }
    }

    fun restoreExpiry(entry: ExpiryHistoryEntity) {
        viewModelScope.launch { expiryHistoryRepository.restore(entry) }
    }

    /** Mode sélection multiple (appui long) — utilisé pour lancer une recherche de recettes sur plusieurs produits. */
    private val _selectedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedItemIds: StateFlow<Set<String>> = _selectedItemIds

    fun toggleItemSelection(itemId: String) {
        _selectedItemIds.value = if (_selectedItemIds.value.contains(itemId)) {
            _selectedItemIds.value - itemId
        } else {
            _selectedItemIds.value + itemId
        }
    }

    fun clearSelection() {
        _selectedItemIds.value = emptySet()
    }

    private val _selectedDomicileId = MutableStateFlow<String?>(null)
    val selectedDomicileId = _selectedDomicileId.asStateFlow()

    private val _selectedStorageId = MutableStateFlow<String?>(null)
    val selectedStorageId = _selectedStorageId.asStateFlow()

    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    val warningDays: StateFlow<Int> = prefsRepository.getPrefs(userId)
        .map { it?.expiryWarningDays ?: 7 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 7)

    val activeDomicileId: StateFlow<String?> = prefsRepository.getPrefs(userId)
        .map { it?.activeDomicileId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val domiciles: StateFlow<List<DomicileEntity>> = stockRepository.getDomiciles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storages: StateFlow<List<StorageEntity>> = stockRepository.getStorages(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<ItemEntity>> = combine(
        stockRepository.getItems(userId),
        _searchQuery,
        _selectedDomicileId,
        _selectedStorageId,
        storages
    ) { allItems, query, domId, storId, allStorages ->
        var filtered = allItems
        
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }
        
        if (domId != null) {
            val storagesInDom = allStorages.filter { it.domicileId == domId }.map { it.id }
            filtered = filtered.filter { storagesInDom.contains(it.storageId) }
        }
        
        if (storId != null) {
            filtered = filtered.filter { it.storageId == storId }
        }
        
        filtered.sortedBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterDomicile(id: String?) {
        _selectedDomicileId.value = id
        _selectedStorageId.value = null // Reset storage when domicile changes
    }

    fun setFilterStorage(id: String?) {
        _selectedStorageId.value = id
    }

    fun deleteItem(item: ItemEntity) {
        viewModelScope.launch {
            stockRepository.deleteItem(item)
        }
    }

    fun adjustQuantity(item: ItemEntity, delta: Double) {
        viewModelScope.launch {
            val newQuantity = (item.quantity + delta).coerceAtLeast(0.0)
            if (newQuantity != item.quantity) {
                val updatedItem = item.copy(quantity = newQuantity)
                val cleared = stockRepository.addItem(updatedItem)
                if (cleared != null) {
                    _expiryCleared.emit(cleared)
                }
            }
        }
    }

    fun addToShoppingList(item: ItemEntity) {
        viewModelScope.launch {
            val buyQty = if (item.restockBuyQuantity > 0) item.restockBuyQuantity else 1.0
            val shoppingItem = ShoppingEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = item.name,
                quantity = buyQty.toString(),
                unit = item.unit,
                source = "manual",
                itemId = item.id,
                shopId = item.shopId,
                requestorInitials = item.requestorInitials,
                checked = false,
                addedAt = Instant.now().toString()
            )
            shoppingRepository.addShoppingItem(shoppingItem)
            _message.emit(item.name)
        }
    }
}
