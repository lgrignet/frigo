package com.mystockmanager.app.core

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mystockmanager.app.data.repository.PrefsRepository
import com.mystockmanager.app.data.repository.StockRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@HiltWorker
class ExpiryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val stockRepository: StockRepository,
    private val prefsRepository: PrefsRepository,
    private val sessionManager: SessionManager,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = sessionManager.getUserId().toString()
        if (userId == "-1") return Result.success()

        val prefs = prefsRepository.getPrefs(userId).first()
        if (prefs == null || !prefs.notificationsEnabled) return Result.success()

        val items = stockRepository.getItems(userId).first()
        val warningDays = prefs.expiryWarningDays
        
        val expiringItems = items.filter { item ->
            item.expiryDate?.let { dateStr ->
                try {
                    val expiryDate = LocalDate.parse(dateStr)
                    val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate)
                    daysUntil in 0..warningDays
                } catch (e: Exception) {
                    false
                }
            } ?: false
        }

        if (expiringItems.isNotEmpty()) {
            val title = "Attention : Produits bientôt périmés"
            val message = if (expiringItems.size == 1) {
                "Le produit '${expiringItems.first().name}' périme bientôt."
            } else {
                "${expiringItems.size} produits vont bientôt périmer."
            }
            notificationHelper.showExpiryNotification(title, message)
        }

        return Result.success()
    }
}
