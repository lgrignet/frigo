package com.mystockmanager.app.ui.storages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.data.local.entities.DomicileEntity
import com.mystockmanager.app.data.local.entities.ShopEntity
import com.mystockmanager.app.data.local.entities.StorageEntity
import com.mystockmanager.app.data.local.entities.UnitEntity
import com.mystockmanager.app.data.repository.PrefsRepository
import com.mystockmanager.app.data.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoragesViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val prefsRepository: PrefsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val userId = sessionManager.getUserId().toString()

    val domiciles: StateFlow<List<DomicileEntity>> = stockRepository.getDomiciles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storages: StateFlow<List<StorageEntity>> = stockRepository.getStorages(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shops: StateFlow<List<ShopEntity>> = stockRepository.getShops(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val units: StateFlow<List<UnitEntity>> = stockRepository.getUnits(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDomicileId: StateFlow<String?> = prefsRepository.getPrefs(userId)
        .map { it?.activeDomicileId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    private fun isDuplicate(name: String, type: String, domicileId: String?, excludeId: String? = null): Boolean {
        return storages.value.any { 
            it.id != excludeId && 
            it.name.lowercase().trim() == name.lowercase().trim() && 
            it.type == type && 
            it.domicileId == domicileId 
        }
    }

    fun createDomicile(name: String) {
        viewModelScope.launch {
            val domicile = DomicileEntity(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                syncGuid = sessionManager.getSyncGuid() ?: "",
                createdAt = java.time.Instant.now().toString()
            )
            stockRepository.addDomicile(domicile)
        }
    }

    fun updateDomicile(domicile: DomicileEntity) {
        viewModelScope.launch {
            stockRepository.addDomicile(domicile)
        }
    }

    fun deleteDomicile(domicile: DomicileEntity) {
        viewModelScope.launch {
            stockRepository.deleteDomicile(domicile)
        }
    }

    fun createStorage(name: String, icon: String, type: String, domicileId: String?) {
        if (isDuplicate(name, type, domicileId)) {
            viewModelScope.launch { _error.emit("Un rangement avec ce nom et ce type existe déjà pour ce domicile.") }
            return
        }
        
        viewModelScope.launch {
            val storage = StorageEntity(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                domicileId = domicileId,
                name = name,
                icon = icon,
                type = type,
                createdAt = java.time.Instant.now().toString()
            )
            stockRepository.addStorage(storage)
        }
    }

    fun updateStorage(storage: StorageEntity) {
        if (isDuplicate(storage.name, storage.type, storage.domicileId, storage.id)) {
            viewModelScope.launch { _error.emit("Un rangement avec ce nom et ce type existe déjà pour ce domicile.") }
            return
        }

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

    fun createUnit(name: String, label: String) {
        viewModelScope.launch {
            val unit = UnitEntity(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                label = label
            )
            stockRepository.addUnit(unit)
        }
    }

    fun updateUnit(unit: UnitEntity) {
        viewModelScope.launch {
            stockRepository.addUnit(unit)
        }
    }

    fun deleteUnit(unit: UnitEntity) {
        viewModelScope.launch {
            stockRepository.deleteUnit(unit)
        }
    }

    fun addDefaultDataIfEmpty() {
        viewModelScope.launch {
            val currentUnits = units.first()
            currentUnits.forEach { unit ->
                if (!unit.id.startsWith("unit_")) {
                    stockRepository.deleteUnit(unit) 
                }
            }

            val defaults = listOf(
                "unit_piece" to ("pièce(s)" to "pièce(s)"),
                "unit_kg" to ("Kilogramme" to "kg"),
                "unit_g" to ("Gramme" to "g"),
                "unit_l" to ("Litre" to "L"),
                "unit_packet" to ("Paquet" to "paquet(s)")
            )

            defaults.forEach { (fixedId, data) ->
                val (name, label) = data
                stockRepository.addUnit(UnitEntity(fixedId, userId, name, label))
            }

            val currentDomiciles = domiciles.first()
            if (currentDomiciles.isEmpty()) {
                val defaultDomId = "dom_main_$userId"
                val mainDomicile = DomicileEntity(
                    id = defaultDomId,
                    userId = userId,
                    name = "Maison",
                    syncGuid = sessionManager.getSyncGuid() ?: "",
                    createdAt = java.time.Instant.now().toString()
                )
                stockRepository.addDomicile(mainDomicile)

                val currentStorages = storages.first()
                currentStorages.forEach { storage ->
                    if (storage.domicileId == null) {
                        stockRepository.addStorage(storage.copy(domicileId = defaultDomId))
                    }
                }

                val prefs = prefsRepository.getPrefs(userId).first()
                if (prefs != null && prefs.activeDomicileId == null) {
                    prefsRepository.savePrefs(prefs.copy(activeDomicileId = defaultDomId))
                }
            }
        }
    }
}
