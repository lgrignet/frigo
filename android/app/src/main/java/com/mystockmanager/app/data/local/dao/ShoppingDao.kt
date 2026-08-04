package com.mystockmanager.app.data.local.dao

import androidx.room.*
import com.mystockmanager.app.data.local.entities.ShoppingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_list WHERE userId = :userId")
    fun getShoppingList(userId: String): Flow<List<ShoppingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingEntity)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingEntity)

    @Query("UPDATE shopping_list SET checked = :checked WHERE id = :id")
    suspend fun toggleItem(id: String, checked: Boolean)

    @Query("SELECT * FROM shopping_list WHERE userId = :userId AND source = 'auto'")
    suspend fun getAutoRestockItems(userId: String): List<ShoppingEntity>

    @Query("SELECT * FROM shopping_list WHERE userId = :userId AND itemId = :itemId AND source = 'auto' LIMIT 1")
    suspend fun getAutoEntryForItem(userId: String, itemId: String): ShoppingEntity?
}
