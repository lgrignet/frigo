package com.mystockmanager.app.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.data.local.entities.ItemEntity
import com.mystockmanager.app.data.local.entities.ShopEntity
import com.mystockmanager.app.data.local.entities.StorageEntity
import com.mystockmanager.app.data.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ItemFormViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val userId = sessionManager.getUserId().toString()

    val storages: StateFlow<List<StorageEntity>> = stockRepository.getStorages(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shops: StateFlow<List<ShopEntity>> = stockRepository.getShops(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()

    private val _itemToEdit = MutableStateFlow<ItemEntity?>(null)
    val itemToEdit = _itemToEdit.asStateFlow()

    fun loadItem(itemId: String) {
        viewModelScope.launch {
            val item = stockRepository.getItems(userId).first().find { it.id == itemId }
            _itemToEdit.value = item
        }
    }

    fun saveItem(
        id: String? = null,
        name: String,
        quantity: Double,
        unit: String,
        expiryDate: String?,
        storageId: String,
        shopId: String?,
        photo: String?,
        restockThreshold: Int,
        restockBuyQuantity: Double,
        notes: String
    ) {
        viewModelScope.launch {
            val now = Instant.now().toString()
            val item = ItemEntity(
                id = id ?: UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                quantity = quantity,
                unit = unit,
                expiryDate = expiryDate,
                storageId = storageId,
                shopId = shopId,
                photo = photo ?: _itemToEdit.value?.photo, 
                restockThreshold = restockThreshold,
                restockBuyQuantity = restockBuyQuantity,
                notes = notes,
                createdAt = _itemToEdit.value?.createdAt ?: now,
                updatedAt = now
            )
            stockRepository.addItem(item)
            _saveSuccess.emit(Unit)
        }
    }
}
