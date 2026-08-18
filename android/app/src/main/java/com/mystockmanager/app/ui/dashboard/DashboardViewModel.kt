package com.mystockmanager.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.data.local.entities.ItemEntity
import com.mystockmanager.app.data.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

import com.mystockmanager.app.data.repository.PrefsRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val prefsRepository: PrefsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val userId = sessionManager.getUserId().toString()

    val expiringItems: StateFlow<List<ItemEntity>> = combine(
        stockRepository.getItems(userId),
        prefsRepository.getPrefs(userId)
    ) { items, prefs ->
        val warningDays = prefs?.expiryWarningDays ?: 7
        val today = LocalDate.now()
        
        items.filter { item ->
            val dateStr = item.expiryDate
            if (dateStr.isNullOrBlank()) return@filter false
            
            try {
                val expiryDate = parseDate(dateStr)
                val daysUntil = ChronoUnit.DAYS.between(today, expiryDate)
                // On affiche si c'est déjà périmé (days < 0) ou si ça périme bientôt
                daysUntil <= warningDays
            } catch (e: Exception) {
                false
            }
        }.sortedBy { 
            try { parseDate(it.expiryDate!!) } catch(e: Exception) { LocalDate.MAX }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun parseDate(dateStr: String): LocalDate {
        return if (dateStr.contains("-")) {
            LocalDate.parse(dateStr) // ISO YYYY-MM-DD
        } else {
            // Tentative format européen DD/MM/YYYY
            LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }
    }
}
