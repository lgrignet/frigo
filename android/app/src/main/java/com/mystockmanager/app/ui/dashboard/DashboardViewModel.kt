package com.mystockmanager.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.data.local.entities.ItemEntity
import com.mystockmanager.app.data.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val expiringItems: StateFlow<List<ItemEntity>> = stockRepository
        .getItems(sessionManager.getUserId().toString())
        .map { items -> 
            // Filtrer les produits périmant dans les 7 jours (logique simplifiée pour l'instant)
            items.filter { it.expiryDate != null }.sortedBy { it.expiryDate }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
