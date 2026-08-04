package com.mystockmanager.app.data.local.dao

import androidx.room.*
import com.mystockmanager.app.data.local.entities.ShopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    @Query("SELECT * FROM shops WHERE userId = :userId")
    fun getAllShops(userId: String): Flow<List<ShopEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopEntity)

    @Delete
    suspend fun deleteShop(shop: ShopEntity)

    @Query("SELECT * FROM shops WHERE id = :id")
    suspend fun getShopById(id: String): ShopEntity?
}
