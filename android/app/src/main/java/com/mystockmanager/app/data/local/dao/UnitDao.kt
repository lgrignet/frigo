package com.mystockmanager.app.data.local.dao

import androidx.room.*
import com.mystockmanager.app.data.local.entities.UnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitDao {
    @Query("SELECT * FROM units WHERE userId = :userId")
    fun getAllUnits(userId: String): Flow<List<UnitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: UnitEntity)

    @Query("DELETE FROM units WHERE userId = :userId AND label = :label AND id != :fixedId")
    suspend fun cleanupOldUnits(userId: String, label: String, fixedId: String)

    @Delete
    suspend fun deleteUnit(unit: UnitEntity)

    @Query("DELETE FROM units WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
