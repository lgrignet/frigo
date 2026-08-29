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

    fun setSession(userId: Long, email: String, syncChannelGuid: String, firstName: String? = null, lastName: String? = null) {
        val editor = prefs.edit()
        editor.putLong("user_id", userId)
        editor.putString("email", email)
        editor.putString("sync_guid", syncChannelGuid)
        if (firstName != null) editor.putString("first_name", firstName)
        if (lastName != null) editor.putString("last_name", lastName)
        editor.apply()
    }

    fun getUserId(): Long = prefs.getLong("user_id", -1L)
    fun getEmail(): String? = prefs.getString("email", null)
    fun getSyncGuid(): String? = prefs.getString("sync_guid", null)
    fun getFirstName(): String = prefs.getString("first_name", "") ?: ""
    fun getLastName(): String = prefs.getString("last_name", "") ?: ""

    fun getInitials(): String {
        val f = getFirstName().trim().take(1).uppercase()
        val l = getLastName().trim().take(1).uppercase()
        return if (f.isEmpty() && l.isEmpty()) "??" else "$f$l"
    }

    fun setSyncGuid(guid: String) {
        prefs.edit().putString("sync_guid", guid).apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = getUserId() != -1L
}
