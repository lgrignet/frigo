package com.mystockmanager.app.ui.prefs

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.core.SyncManager
import com.mystockmanager.app.data.local.entities.DomicileEntity
import com.mystockmanager.app.data.local.entities.PreferenceEntity
import com.mystockmanager.app.data.repository.AuthRepository
import com.mystockmanager.app.data.repository.PrefsRepository
import com.mystockmanager.app.data.repository.ShoppingRepository
import com.mystockmanager.app.data.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrefsViewModel @Inject constructor(
    private val prefsRepository: PrefsRepository,
    private val stockRepository: StockRepository,
    private val shoppingRepository: ShoppingRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager
) : ViewModel() {

    private val userId = sessionManager.getUserId().toString()

    val firstName = MutableStateFlow(sessionManager.getFirstName())
    val lastName = MutableStateFlow(sessionManager.getLastName())
    val emailVerified = MutableStateFlow(sessionManager.getEmailVerified())
    val verifyCodeError = MutableStateFlow(false)

    val prefs: StateFlow<PreferenceEntity> = prefsRepository.getPrefs(userId)
        .map { it ?: PreferenceEntity(userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferenceEntity(userId))

    val domiciles: StateFlow<List<DomicileEntity>> = stockRepository.getDomiciles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncGuid = MutableStateFlow(sessionManager.getSyncGuid() ?: "")

    /**
     * Change de foyer&nbsp;: crée/rejoint le nouveau salon côté serveur, puis
     * PURGE les données locales de l'ancien foyer avant de se reconnecter — plutôt
     * que de compter sur le nettoyage différé via sync_catalog (§5.3/§11 du
     * cahier des charges), qui laisserait temporairement les deux foyers mélangés
     * à l'écran.
     */
    fun updateSyncGuid(newGuid: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = authRepository.changeHousehold(newGuid)
            if (ok) {
                syncManager.stopSync()
                stockRepository.wipeHouseholdData(userId)
                shoppingRepository.wipeHouseholdData(userId)
                prefsRepository.clearHouseholdReferences(userId)
                sessionManager.setSyncGuid(newGuid)
                syncGuid.value = newGuid
                syncManager.startSync()
            }
            onResult(ok)
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

    fun updateActiveDomicile(domicileId: String?) {
        viewModelScope.launch {
            prefsRepository.savePrefs(prefs.value.copy(activeDomicileId = domicileId))
        }
    }

    fun updateShoppingAggregation(aggregated: Boolean) {
        viewModelScope.launch {
            prefsRepository.savePrefs(prefs.value.copy(isShoppingAggregated = aggregated))
        }
    }

    fun verifyEmail(code: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = authRepository.verifyEmail(code)
            verifyCodeError.value = !ok
            if (ok) emailVerified.value = true
            onDone(ok)
        }
    }

    fun resendVerificationCode() {
        viewModelScope.launch { authRepository.resendVerificationCode() }
    }

    fun saveProfile() {
        viewModelScope.launch {
            authRepository.updateProfile(firstName.value, lastName.value)
            // Rafraîchir les valeurs locales depuis le sessionManager mis à jour
            firstName.value = sessionManager.getFirstName()
            lastName.value = sessionManager.getLastName()
        }
    }
}
