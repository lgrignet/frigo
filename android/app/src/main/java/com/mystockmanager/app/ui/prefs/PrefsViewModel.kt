package com.mystockmanager.app.ui.prefs

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.core.SyncManager
import com.mystockmanager.app.data.local.entities.PreferenceEntity
import com.mystockmanager.app.data.repository.PrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrefsViewModel @Inject constructor(
    private val prefsRepository: PrefsRepository,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager
) : ViewModel() {

    private val userId = sessionManager.getUserId().toString()

    val prefs: StateFlow<PreferenceEntity> = prefsRepository.getPrefs(userId)
        .map { it ?: PreferenceEntity(userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferenceEntity(userId))

    val syncGuid = MutableStateFlow(sessionManager.getSyncGuid() ?: "")

    fun updateSyncGuid(newGuid: String) {
        viewModelScope.launch {
            sessionManager.setSyncGuid(newGuid)
            syncGuid.value = newGuid
            syncManager.stopSync()
            syncManager.startSync()
        }
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            prefsRepository.savePrefs(prefs.value.copy(theme = theme))
        }
    }

    fun updateLang(lang: String) {
        viewModelScope.launch {
            prefsRepository.savePrefs(prefs.value.copy(lang = lang))
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
        }
    }

    fun updateExpiryDays(days: Int) {
        viewModelScope.launch {
            prefsRepository.savePrefs(prefs.value.copy(expiryWarningDays = days))
        }
    }

    fun updateDateFormat(format: String) {
        viewModelScope.launch {
            prefsRepository.savePrefs(prefs.value.copy(dateFormat = format))
        }
    }

    fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.savePrefs(prefs.value.copy(notificationsEnabled = enabled))
        }
    }
}
