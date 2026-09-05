package com.mystockmanager.app.data.local.dao

import androidx.room.*
import com.mystockmanager.app.data.local.entities.ExpiryHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpiryHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ExpiryHistoryEntity)

    @Query("SELECT * FROM expiry_history WHERE userId = :userId AND clearedAt >= :sinceIso ORDER BY clearedAt DESC")
    fun getRecentSince(userId: String, sinceIso: String): Flow<List<ExpiryHistoryEntity>>

    @Query("DELETE FROM expiry_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM expiry_history WHERE userId = :userId AND clearedAt < :cutoffIso")
    suspend fun deleteOlderThan(userId: String, cutoffIso: String)
}
