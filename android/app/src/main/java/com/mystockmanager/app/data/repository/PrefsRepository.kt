package com.mystockmanager.app.data.repository

import com.mystockmanager.app.core.SyncManager
import com.mystockmanager.app.data.local.dao.PreferenceDao
import com.mystockmanager.app.data.local.entities.PreferenceEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefsRepository @Inject constructor(
    private val preferenceDao: PreferenceDao,
    private val syncManager: SyncManager
) {
    fun getPrefs(userId: String): Flow<PreferenceEntity?> = preferenceDao.getPreferences(userId)

    suspend fun savePrefs(prefs: PreferenceEntity) {
        preferenceDao.insertPreferences(prefs)
        syncManager.syncPrefs(prefs)
    }
}
