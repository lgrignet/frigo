package com.mystockmanager.app.data.local.dao

import androidx.room.*
import com.mystockmanager.app.data.local.entities.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE userId = :userId")
    fun getAllItems(userId: String): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)

    @Delete
    suspend fun deleteItem(item: ItemEntity)

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: String): ItemEntity?

    @Query("SELECT * FROM items WHERE userId = :userId AND restockThreshold > 0 AND quantity <= restockThreshold")
    fun getLowStockItems(userId: String): Flow<List<ItemEntity>>
}
