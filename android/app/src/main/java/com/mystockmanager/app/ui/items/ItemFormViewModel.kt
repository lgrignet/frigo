package com.mystockmanager.app.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystockmanager.app.core.ProductInfoService
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.data.local.entities.*
import com.mystockmanager.app.data.repository.PrefsRepository
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
    private val prefsRepository: PrefsRepository,
    private val sessionManager: SessionManager,
    private val productInfoService: ProductInfoService
) : ViewModel() {

    private val userId = sessionManager.getUserId().toString()

    val dateFormat: StateFlow<String> = prefsRepository.getPrefs(userId)
        .map { it?.dateFormat ?: "european" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "european")

    val lang: StateFlow<String> = prefsRepository.getPrefs(userId)
        .map { it?.lang ?: "fr" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "fr")

    val activeDomicileId: StateFlow<String?> = prefsRepository.getPrefs(userId)
        .map { it?.activeDomicileId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val domiciles: StateFlow<List<DomicileEntity>> = stockRepository.getDomiciles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storages: StateFlow<List<StorageEntity>> = stockRepository.getStorages(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shops: StateFlow<List<ShopEntity>> = stockRepository.getShops(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val units: StateFlow<List<UnitEntity>> = stockRepository.getUnits(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()

    private val _itemToEdit = MutableStateFlow<ItemEntity?>(null)
    val itemToEdit = _itemToEdit.asStateFlow()

    private val _productFoundName = MutableSharedFlow<String>()
    val productFoundName = _productFoundName.asSharedFlow()

    private val _isLoadingProduct = MutableStateFlow(false)
    val isLoadingProduct = _isLoadingProduct.asStateFlow()

    fun loadItem(itemId: String) {
        viewModelScope.launch {
            val item = stockRepository.getItems(userId).first().find { it.id == itemId }
            _itemToEdit.value = item
        }
    }

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            _isLoadingProduct.value = true
            val name = productInfoService.getProductName(barcode)
            if (name != null) {
                _productFoundName.emit(name)
            }
            _isLoadingProduct.value = false
        }
    }

    fun saveItem(
        id: String? = null,
        name: String,
        quantity: Double,
        unit: String,
        barcode: String?,
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
                barcode = barcode,
                requestorInitials = if (id == null) sessionManager.getInitials() else _itemToEdit.value?.requestorInitials,
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
