package com.mystockmanager.app.ui.storages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.data.local.entities.ShopEntity
import com.mystockmanager.app.data.local.entities.StorageEntity
import com.mystockmanager.app.data.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoragesViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val userId = sessionManager.getUserId().toString()

    val storages: StateFlow<List<StorageEntity>> = stockRepository.getStorages(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shops: StateFlow<List<ShopEntity>> = stockRepository.getShops(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createStorage(name: String, icon: String, type: String) {
        viewModelScope.launch {
            val storage = StorageEntity(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                icon = icon,
                type = type,
                createdAt = java.time.Instant.now().toString()
            )
            stockRepository.addStorage(storage)
        }
    }

    fun updateStorage(storage: StorageEntity) {
        viewModelScope.launch {
            stockRepository.addStorage(storage)
        }
    }

    fun deleteStorage(storage: StorageEntity) {
        viewModelScope.launch {
            stockRepository.deleteStorage(storage)
        }
    }

    fun createShop(name: String) {
        viewModelScope.launch {
            val shop = ShopEntity(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                createdAt = java.time.Instant.now().toString(),
                updatedAt = java.time.Instant.now().toString()
            )
            stockRepository.addShop(shop)
        }
    }

    fun updateShop(shop: ShopEntity) {
        viewModelScope.launch {
            stockRepository.addShop(shop.copy(updatedAt = java.time.Instant.now().toString()))
        }
    }

    fun deleteShop(shop: ShopEntity) {
        viewModelScope.launch {
            stockRepository.deleteShop(shop)
        }
    }

    fun addDefaultDataIfEmpty() {
        viewModelScope.launch {
            // Logic to create defaults if nothing exists (handled by AuthRepository during register usually)
        }
    }
}
