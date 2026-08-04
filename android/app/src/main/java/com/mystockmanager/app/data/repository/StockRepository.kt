package com.mystockmanager.app.data.repository

import com.mystockmanager.app.core.SyncManager
import com.mystockmanager.app.data.local.dao.ItemDao
import com.mystockmanager.app.data.local.dao.ShopDao
import com.mystockmanager.app.data.local.dao.StorageDao
import com.mystockmanager.app.data.local.entities.ItemEntity
import com.mystockmanager.app.data.local.entities.ShopEntity
import com.mystockmanager.app.data.local.entities.StorageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepository @Inject constructor(
    private val itemDao: ItemDao,
    private val storageDao: StorageDao,
    private val shopDao: ShopDao,
    private val syncManager: SyncManager
) {
    fun getItems(userId: String): Flow<List<ItemEntity>> = itemDao.getAllItems(userId)
    
    fun getStorages(userId: String): Flow<List<StorageEntity>> = storageDao.getAllStorages(userId)

    fun getShops(userId: String): Flow<List<ShopEntity>> = shopDao.getAllShops(userId)

    suspend fun addItem(item: ItemEntity) {
        itemDao.insertItem(item)
        syncManager.syncItem(item, "put")
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
}
