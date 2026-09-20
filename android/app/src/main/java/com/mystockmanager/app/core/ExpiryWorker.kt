package com.mystockmanager.app.core

import android.content.Context
import android.content.res.Configuration
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mystockmanager.app.R
import com.mystockmanager.app.data.repository.PrefsRepository
import com.mystockmanager.app.data.repository.StockRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

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
                if (dateStr.isBlank()) return@let false
                try {
                    val expiryDate = parseDate(dateStr)
                    val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate)
                    daysUntil <= warningDays
                } catch (e: Exception) {
                    false
                }
            } ?: false
        }

        if (expiringItems.isNotEmpty()) {
            // La langue de l'appli est pilotée par prefs.lang (Room) ; on force cette locale
            // sur le contexte du Worker, qui sinon suivrait la langue du système.
            val ctx = localizedContext(prefs.lang)
            val title = ctx.getString(R.string.notif_expiry_title)
            val message = if (expiringItems.size == 1) {
                ctx.getString(R.string.notif_expiry_single, expiringItems.first().name)
            } else {
                ctx.getString(R.string.notif_expiry_multiple, expiringItems.size)
            }
            notificationHelper.showExpiryNotification(title, message)
        }

        return Result.success()
    }

    private fun localizedContext(lang: String): Context {
        val config = Configuration(applicationContext.resources.configuration)
        config.setLocale(Locale(lang))
        return applicationContext.createConfigurationContext(config)
    }

    private fun parseDate(dateStr: String): LocalDate {
        return if (dateStr.contains("-")) {
            LocalDate.parse(dateStr)
        } else {
            LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }
    }
}
