package com.mystockmanager.app.data.local.dao

import androidx.room.*
import com.mystockmanager.app.data.local.entities.UnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitDao {
    @Query("SELECT * FROM units")
    fun getAllUnits(): Flow<List<UnitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: UnitEntity)

    @Query("DELETE FROM units WHERE label = :label AND id != :fixedId")
    suspend fun cleanupOldUnits(label: String, fixedId: String)

    @Delete
    suspend fun deleteUnit(unit: UnitEntity)

    @Query("DELETE FROM units")
    suspend fun deleteAll()
}
