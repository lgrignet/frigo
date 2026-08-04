package com.mystockmanager.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey val userId: String,
    val expiryWarningDays: Int = 7,
    val notificationsEnabled: Boolean = false,
    val notificationTime: String = "08:00",
    val defaultUnit: String = "pièce(s)",
    val defaultStorageId: String? = null,
    val theme: String = "dark",
    val lang: String = "fr",
    val dateFormat: String = "european"
)
