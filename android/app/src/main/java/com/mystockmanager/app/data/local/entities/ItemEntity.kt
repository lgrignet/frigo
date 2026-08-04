package com.mystockmanager.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val expiryDate: String?, // Format ISO YYYY-MM-DD
    val storageId: String,
    val shopId: String?,
    val photo: String?, // Base64 ou Path local
    val restockThreshold: Int = 0,
    val restockBuyQuantity: Double = 1.0,
    val notes: String = "",
    val createdAt: String,
    val updatedAt: String
)
