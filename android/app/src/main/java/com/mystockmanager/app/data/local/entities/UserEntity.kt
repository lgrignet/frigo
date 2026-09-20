package com.mystockmanager.app.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val firstName: String = "",
    val lastName: String = "",
    val passwordHash: String,
    val salt: String,
    val recoveryHash: String,
    val recoverySalt: String,
    val syncChannelGuid: String,
    val createdAt: String,
    /** Identifiant du compte côté service de comptes (api.noshi.be), null tant que non synchronisé. */
    val compteId: String? = null,
    /** Jeton d'appareil renvoyé par api.noshi.be (register/login/migrate). */
    val deviceToken: String? = null,
    val emailVerifie: Boolean = false
)
