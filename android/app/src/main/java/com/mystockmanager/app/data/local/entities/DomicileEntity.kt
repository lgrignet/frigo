package com.mystockmanager.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "domiciles")
data class DomicileEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val syncGuid: String,
    val createdAt: String
)
