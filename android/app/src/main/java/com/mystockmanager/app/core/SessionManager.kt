package com.mystockmanager.app.core

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("msm_session", Context.MODE_PRIVATE)

    fun setSession(userId: Long, email: String, syncChannelGuid: String) {
        prefs.edit().apply {
            putLong("user_id", userId)
            putString("email", email)
            putString("sync_guid", syncChannelGuid)
            apply()
        }
    }

    fun getUserId(): Long = prefs.getLong("user_id", -1L)
    fun getEmail(): String? = prefs.getString("email", null)
    fun getSyncGuid(): String? = prefs.getString("sync_guid", null)

    fun setSyncGuid(guid: String) {
        prefs.edit().putString("sync_guid", guid).apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = getUserId() != -1L
}
