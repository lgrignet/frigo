package com.mystockmanager.app.data.local.dao

import androidx.room.*
import com.mystockmanager.app.data.local.entities.StorageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageDao {
    @Query("SELECT * FROM storages WHERE userId = :userId")
    fun getAllStorages(userId: String): Flow<List<StorageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStorage(storage: StorageEntity)

    @Delete
    suspend fun deleteStorage(storage: StorageEntity)

    @Query("SELECT * FROM storages WHERE id = :id")
    suspend fun getStorageById(id: String): StorageEntity?
}
