package com.mystockmanager.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val label: String // e.g. "kg", "g", "pièce(s)"
)
