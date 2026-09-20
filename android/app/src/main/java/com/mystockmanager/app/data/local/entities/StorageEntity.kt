package com.mystockmanager.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "storages")
data class StorageEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val domicileId: String? = null, // Linked to a Domicile
    val name: String,
    val icon: String = "📦",
    val type: String = "dry", // cold, frozen, dry
    val isDefault: Boolean = false,
    val createdAt: String
)
