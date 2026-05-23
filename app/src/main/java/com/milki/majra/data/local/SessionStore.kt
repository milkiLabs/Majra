package com.milki.majra.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.milki.majra.data.model.Platform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sourceSessionDataStore by preferencesDataStore(name = "source_sessions")

class SessionStore(private val context: Context) {
    fun session(platform: Platform): Flow<SourceSession> = context.sourceSessionDataStore.data.map { preferences ->
        SourceSession(
            platform = platform,
            cookie = preferences[cookieKey(platform)] ?: "",
            userAgent = preferences[userAgentKey(platform)] ?: "",
            accessToken = preferences[accessTokenKey(platform)],
        )
    }

    suspend fun save(platform: Platform, cookie: String, userAgent: String, accessToken: String? = null) {
        context.sourceSessionDataStore.edit { preferences ->
            preferences[cookieKey(platform)] = cookie
            preferences[userAgentKey(platform)] = userAgent
            if (accessToken.isNullOrBlank()) {
                preferences.remove(accessTokenKey(platform))
            } else {
                preferences[accessTokenKey(platform)] = accessToken
            }
        }
    }

    suspend fun clear(platform: Platform) {
        context.sourceSessionDataStore.edit { preferences ->
            preferences.remove(cookieKey(platform))
            preferences.remove(userAgentKey(platform))
            preferences.remove(accessTokenKey(platform))
        }
    }

    suspend fun current(platform: Platform): SourceSession = session(platform).first()

    companion object {
        private fun cookieKey(platform: Platform) = stringPreferencesKey("${platform.storageKey}_cookie")
        private fun userAgentKey(platform: Platform) = stringPreferencesKey("${platform.storageKey}_user_agent")
        private fun accessTokenKey(platform: Platform) = stringPreferencesKey("${platform.storageKey}_access_token")
    }
}

data class SourceSession(
    val platform: Platform,
    val cookie: String,
    val userAgent: String,
    val accessToken: String? = null,
) {
    val isAuthenticated: Boolean
        get() = accessToken?.isNotBlank() == true || when (platform) {
            Platform.INSTAGRAM -> cookie.contains("sessionid=")
            Platform.FACEBOOK -> cookie.contains("c_user=")
            Platform.X -> cookie.contains("auth_token=")
            Platform.RSS -> true
        }
}
