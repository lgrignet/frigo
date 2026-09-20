package com.mystockmanager.app.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class SyncMessage(
    val type: String, // "item", "storage", "shop", "shopping"
    val action: String, // "put", "delete"
    val data: String, // JSON string de l'entité
    val origin: String // GUID de l'appareil pour éviter les boucles
)
