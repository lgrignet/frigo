package com.mystockmanager.app.data.repository

import com.mystockmanager.app.data.local.dao.ExpiryHistoryDao
import com.mystockmanager.app.data.local.dao.ItemDao
import com.mystockmanager.app.data.local.entities.ExpiryHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Historique local (non synchronisé) des dates de péremption effacées
 * automatiquement quand un article tombe à quantité 0 — voir
 * StockRepository.addItem. Fenêtre de rétention de [RETENTION_MINUTES].
 */
@Singleton
class ExpiryHistoryRepository @Inject constructor(
    private val historyDao: ExpiryHistoryDao,
    private val itemDao: ItemDao,
    private val stockRepository: StockRepository
) {
    fun getRecent(userId: String): Flow<List<ExpiryHistoryEntity>> =
        historyDao.getRecentSince(userId, cutoffIso())

    /** Restaure la date de péremption sur l'article concerné (s'il existe encore) et retire l'entrée de l'historique. */
    suspend fun restore(entry: ExpiryHistoryEntity) {
        val item = itemDao.getItemById(entry.itemId) ?: return
        stockRepository.addItem(item.copy(expiryDate = entry.previousExpiryDate))
        historyDao.deleteById(entry.id)
    }

    /** À appeler périodiquement (ex. à l'ouverture de l'écran) pour ne pas laisser traîner d'entrées expirées. */
    suspend fun purgeExpired(userId: String) {
        historyDao.deleteOlderThan(userId, cutoffIso())
    }

    private fun cutoffIso(): String = Instant.now().minus(RETENTION_MINUTES, ChronoUnit.MINUTES).toString()

    companion object {
        const val RETENTION_MINUTES = 10L
    }
}
