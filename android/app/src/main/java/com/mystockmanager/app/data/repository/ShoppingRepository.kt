package com.mystockmanager.app.data.repository

import com.mystockmanager.app.core.SyncManager
import com.mystockmanager.app.data.local.dao.ItemDao
import com.mystockmanager.app.data.local.dao.ShoppingDao
import com.mystockmanager.app.data.local.entities.ItemEntity
import com.mystockmanager.app.data.local.entities.ShoppingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingRepository @Inject constructor(
    private val shoppingDao: ShoppingDao,
    private val itemDao: ItemDao,
    private val syncManager: SyncManager
) {
    fun getShoppingList(userId: String): Flow<List<ShoppingEntity>> = shoppingDao.getShoppingList(userId)

    suspend fun addShoppingItem(item: ShoppingEntity) {
        shoppingDao.insertShoppingItem(item)
        syncManager.syncShopping(item, "put")
    }

    suspend fun deleteShoppingItem(item: ShoppingEntity) {
        shoppingDao.deleteShoppingItem(item)
        syncManager.syncShopping(item, "delete")
    }

    suspend fun toggleItem(id: String, checked: Boolean) {
        shoppingDao.toggleItem(id, checked)
        // Récupérer l'objet mis à jour pour synchroniser le nouvel état
        val updatedItem = shoppingDao.getShoppingItemById(id)
        if (updatedItem != null) {
            syncManager.syncShopping(updatedItem, "put")
        }
    }

    suspend fun updateShoppingItem(item: ShoppingEntity) {
        shoppingDao.insertShoppingItem(item)
        syncManager.syncShopping(item, "put")
    }

    suspend fun moveToStock(shoppingItem: ShoppingEntity, storageId: String) {
        val userId = shoppingItem.userId
        val qty = shoppingItem.quantity.toDoubleOrNull() ?: 0.0

        if (shoppingItem.itemId != null) {
            // Update existing item
            val existingItem = itemDao.getAllItems(userId).first().find { it.id == shoppingItem.itemId }
            if (existingItem != null) {
                val updatedItem = existingItem.copy(
                    quantity = existingItem.quantity + qty,
                    storageId = storageId,
                    updatedAt = Instant.now().toString()
                )
                itemDao.insertItem(updatedItem)
                syncManager.syncItem(updatedItem, "put")
            }
        } else {
            // Create new item from shopping list
            val newItem = ItemEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = shoppingItem.name,
                quantity = qty,
                unit = shoppingItem.unit,
                expiryDate = null,
                storageId = storageId,
                shopId = shoppingItem.shopId,
                photo = null,
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString()
            )
            itemDao.insertItem(newItem)
            syncManager.syncItem(newItem, "put")
        }

        // Remove from shopping list
        deleteShoppingItem(shoppingItem)
    }

    suspend fun syncAutoRestock(userId: String) {
        val lowStockItems = itemDao.getLowStockItems(userId).first()
        val existingAutoEntries = shoppingDao.getAutoRestockItems(userId)
        
        val lowStockIds = lowStockItems.map { it.id }.toSet()

        // 1. Ajouter ou mettre à jour les produits en stock bas
        for (item in lowStockItems) {
            val existing = existingAutoEntries.find { it.itemId == item.id }
            val buyQty = if (item.restockBuyQuantity > 0) item.restockBuyQuantity else 1.0
            
            val entry = ShoppingEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                userId = userId,
                name = item.name,
                quantity = buyQty.toString(),
                unit = item.unit,
                source = "auto",
                itemId = item.id,
                shopId = item.shopId,
                checked = existing?.checked ?: false,
                addedAt = existing?.addedAt ?: Instant.now().toString()
            )
            shoppingDao.insertShoppingItem(entry)
        }

        // 2. Supprimer les entrées 'auto' dont le produit n'est plus en stock bas
        for (autoEntry in existingAutoEntries) {
            if (autoEntry.itemId != null && !lowStockIds.contains(autoEntry.itemId)) {
                shoppingDao.deleteShoppingItem(autoEntry)
            }
        }
    }
}
