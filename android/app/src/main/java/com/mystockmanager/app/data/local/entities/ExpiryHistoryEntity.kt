package com.mystockmanager.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Trace, pendant une dizaine de minutes, la date de péremption effacée
 * automatiquement quand la quantité d'un article tombe à 0 — permet de la
 * restaurer en cas d'erreur (StockRepository.addItem, ExpiryHistoryRepository).
 * Purement locale à l'appareil, jamais synchronisée entre appareils du foyer.
 */
@Entity(tableName = "expiry_history")
data class ExpiryHistoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val itemId: String,
    val itemName: String,
    val previousExpiryDate: String,
    val clearedAt: String
)
