package com.mystockmanager.app.data.local.dao

import androidx.room.*
import com.mystockmanager.app.data.local.entities.DomicileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DomicileDao {
    @Query("SELECT * FROM domiciles")
    fun getAllDomiciles(): Flow<List<DomicileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomicile(domicile: DomicileEntity)

    @Delete
    suspend fun deleteDomicile(domicile: DomicileEntity)

    @Query("SELECT * FROM domiciles WHERE id = :id LIMIT 1")
    suspend fun getDomicileById(id: String): DomicileEntity?
}
