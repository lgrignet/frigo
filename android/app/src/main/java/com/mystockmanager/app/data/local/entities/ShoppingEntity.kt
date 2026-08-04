package com.mystockmanager.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "shopping_list")
data class ShoppingEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val quantity: String,
    val unit: String,
    val source: String = "manual", // auto, manual, recipe
    val itemId: String? = null,
    val shopId: String? = null,
    val checked: Boolean = false,
    val targetStorageId: String? = null,
    val notes: String = "",
    val addedAt: String
)
