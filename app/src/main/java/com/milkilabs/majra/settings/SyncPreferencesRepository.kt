package com.milkilabs.majra.settings

import java.io.IOException
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Preferences DataStore for background sync settings. */
val Context.syncPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sync_preferences",
)

class SyncPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val syncPreferences: Flow<SyncPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            SyncPreferences(
                isBackgroundSyncEnabled = preferences[Keys.backgroundEnabled] ?: false,
                syncIntervalHours = preferences[Keys.syncIntervalHours] ?: 12,
                notifyOnNewItems = preferences[Keys.notifyOnNewItems] ?: true,
                lastSyncAttemptMillis = preferences[Keys.lastSyncAttemptMillis],
                lastSyncSuccessMillis = preferences[Keys.lastSyncSuccessMillis],
                lastArticleCount = preferences[Keys.lastArticleCount] ?: 0,
                lastNotifiedMillis = preferences[Keys.lastNotifiedMillis],
            )
        }

    suspend fun setBackgroundSyncEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.backgroundEnabled] = enabled
        }
    }

    suspend fun setSyncIntervalHours(hours: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.syncIntervalHours] = hours
        }
    }

    suspend fun setNotifyOnNewItems(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.notifyOnNewItems] = enabled
        }
    }

    suspend fun updateSyncAttempt(timestampMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.lastSyncAttemptMillis] = timestampMillis
        }
    }

    suspend fun updateSyncSuccess(timestampMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.lastSyncSuccessMillis] = timestampMillis
        }
    }

    suspend fun updateArticleCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.lastArticleCount] = count
        }
    }

    suspend fun updateLastNotified(timestampMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.lastNotifiedMillis] = timestampMillis
        }
    }

    private object Keys {
        val backgroundEnabled = booleanPreferencesKey("background_sync_enabled")
        val syncIntervalHours = intPreferencesKey("sync_interval_hours")
        val notifyOnNewItems = booleanPreferencesKey("notify_on_new_items")
        val lastSyncAttemptMillis = longPreferencesKey("last_sync_attempt_millis")
        val lastSyncSuccessMillis = longPreferencesKey("last_sync_success_millis")
        val lastArticleCount = intPreferencesKey("last_article_count")
        val lastNotifiedMillis = longPreferencesKey("last_notified_millis")
    }
}
