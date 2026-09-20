package com.mystockmanager.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val createdAt: String,
    val updatedAt: String
)
