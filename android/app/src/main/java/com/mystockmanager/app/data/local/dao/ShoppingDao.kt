package com.mystockmanager.app.data.local.dao

import androidx.room.*
import com.mystockmanager.app.data.local.entities.ShoppingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_list")
    fun getShoppingList(): Flow<List<ShoppingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingEntity)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingEntity)

    @Query("UPDATE shopping_list SET checked = :checked WHERE id = :id")
    suspend fun toggleItem(id: String, checked: Boolean)

    @Query("SELECT * FROM shopping_list WHERE id = :id LIMIT 1")
    suspend fun getShoppingItemById(id: String): ShoppingEntity?

    @Query("SELECT * FROM shopping_list WHERE source = 'auto'")
    suspend fun getAutoRestockItems(): List<ShoppingEntity>

    @Query("SELECT * FROM shopping_list WHERE itemId = :itemId AND source = 'auto' LIMIT 1")
    suspend fun getAutoEntryForItem(itemId: String): ShoppingEntity?

    @Query("DELETE FROM shopping_list")
    suspend fun deleteAll()
}
