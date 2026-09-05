package com.mystockmanager.app.data.repository

import com.mystockmanager.app.core.SyncManager
import com.mystockmanager.app.data.local.dao.*
import com.mystockmanager.app.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepository @Inject constructor(
    private val itemDao: ItemDao,
    private val storageDao: StorageDao,
    private val shopDao: ShopDao,
    private val unitDao: UnitDao,
    private val domicileDao: DomicileDao,
    private val expiryHistoryDao: ExpiryHistoryDao,
    private val syncManager: SyncManager
) {
    fun getItems(userId: String): Flow<List<ItemEntity>> = itemDao.getAllItems(userId)
    
    fun getStorages(userId: String): Flow<List<StorageEntity>> = storageDao.getAllStorages(userId)

    fun getShops(userId: String): Flow<List<ShopEntity>> = shopDao.getAllShops(userId)

    fun getUnits(userId: String): Flow<List<UnitEntity>> = unitDao.getAllUnits(userId)

    fun getDomiciles(): Flow<List<DomicileEntity>> = domicileDao.getAllDomiciles()

    suspend fun getDomicileById(id: String): DomicileEntity? = domicileDao.getDomicileById(id)

    /**
     * Enregistre un article. Si sa quantité vient de tomber à 0 (elle était
     * positive juste avant) et qu'une date de péremption est renseignée, celle-ci
     * est effacée automatiquement — un produit épuisé n'a plus de date à
     * surveiller — et son ancienne valeur est journalisée pendant 10 minutes
     * pour pouvoir la restaurer en cas d'erreur (ExpiryHistoryRepository).
     * Retourne l'entrée d'historique créée, ou null si rien n'a été effacé.
     */
    suspend fun addItem(item: ItemEntity): ExpiryHistoryEntity? {
        val existing = itemDao.getItemById(item.id)
        val justDepleted = existing != null && existing.quantity > 0 && item.quantity <= 0 && !item.expiryDate.isNullOrBlank()

        val historyEntry = if (justDepleted) {
            ExpiryHistoryEntity(
                id = UUID.randomUUID().toString(),
                userId = item.userId,
                itemId = item.id,
                itemName = item.name,
                previousExpiryDate = item.expiryDate!!,
                clearedAt = Instant.now().toString()
            )
        } else null

        val toPersist = if (justDepleted) item.copy(expiryDate = null) else item

        if (historyEntry != null) {
            expiryHistoryDao.insert(historyEntry)
        }
        itemDao.insertItem(toPersist)
        syncManager.syncItem(toPersist, "put")
        return historyEntry
    }
    
    suspend fun deleteItem(item: ItemEntity) {
        itemDao.deleteItem(item)
        syncManager.syncItem(item, "delete")
    }

    suspend fun addStorage(storage: StorageEntity) {
        storageDao.insertStorage(storage)
        syncManager.syncStorage(storage, "put")
    }

    suspend fun deleteStorage(storage: StorageEntity) {
        storageDao.deleteStorage(storage)
        syncManager.syncStorage(storage, "delete")
    }

    suspend fun addShop(shop: ShopEntity) {
        shopDao.insertShop(shop)
        syncManager.syncShop(shop, "put")
    }

    suspend fun deleteShop(shop: ShopEntity) {
        shopDao.deleteShop(shop)
        syncManager.syncShop(shop, "delete")
    }

    suspend fun addUnit(unit: UnitEntity) {
        unitDao.insertUnit(unit)
        syncManager.syncUnit(unit, "put")
    }

    suspend fun deleteUnit(unit: UnitEntity) {
        unitDao.deleteUnit(unit)
        syncManager.syncUnit(unit, "delete")
    }

    suspend fun cleanupUnits(userId: String, label: String, fixedId: String) {
        unitDao.cleanupOldUnits(userId, label, fixedId)
    }

    suspend fun addDomicile(domicile: DomicileEntity) {
        domicileDao.insertDomicile(domicile)
        syncManager.syncDomicile(domicile, "put")
    }

    suspend fun deleteDomicile(domicile: DomicileEntity) {
        domicileDao.deleteDomicile(domicile)
        syncManager.syncDomicile(domicile, "delete")
    }

    /**
     * Purge tout le contenu du foyer (articles, rangements, magasins, unités,
     * domiciles) avant de rejoindre un autre salon de synchronisation — appelée
     * par un changement de foyer (§11 du cahier des charges), jamais en usage
     * normal. Purement locale, ne déclenche aucun message de sync (l'ancien salon
     * n'a pas à savoir que cet appareil part).
     */
    suspend fun wipeHouseholdData(userId: String) {
        itemDao.deleteAllForUser(userId)
        storageDao.deleteAllForUser(userId)
        shopDao.deleteAllForUser(userId)
        unitDao.deleteAllForUser(userId)
        domicileDao.deleteAllForUser(userId)
    }
}
