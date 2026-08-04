package com.mystockmanager.app.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.data.local.entities.ItemEntity
import com.mystockmanager.app.data.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllItemsViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val items: StateFlow<List<ItemEntity>> = combine(
        stockRepository.getItems(sessionManager.getUserId().toString()),
        _searchQuery
    ) { allItems, query ->
        if (query.isBlank()) {
            allItems.sortedBy { it.name }
        } else {
            allItems.filter { it.name.contains(query, ignoreCase = true) }
                .sortedBy { it.name }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteItem(item: ItemEntity) {
        viewModelScope.launch {
            stockRepository.deleteItem(item)
        }
    }
}
