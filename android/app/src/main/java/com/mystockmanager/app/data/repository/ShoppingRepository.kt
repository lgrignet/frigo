package com.mystockmanager.app.data.repository

import com.mystockmanager.app.core.SyncManager
import com.mystockmanager.app.data.local.dao.ItemDao
import com.mystockmanager.app.data.local.dao.ShoppingDao
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
        // On récupère l'objet mis à jour pour le synchroniser
        // Note: Dans une version plus avancée, on pourrait n'envoyer qu'un "patch"
        // Mais pour la robustesse, renvoyer l'objet complet est plus sûr.
    }

    suspend fun updateShoppingItem(item: ShoppingEntity) {
        shoppingDao.insertShoppingItem(item)
        syncManager.syncShopping(item, "put")
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
