package com.milkilabs.majra.settings

/** User-facing background sync settings. */
data class SyncPreferences(
    val isBackgroundSyncEnabled: Boolean = false,
    val syncIntervalHours: Int = 12,
    val notifyOnNewItems: Boolean = true,
    val lastSyncAttemptMillis: Long? = null,
    val lastSyncSuccessMillis: Long? = null,
    val lastArticleCount: Int = 0,
    val lastNotifiedMillis: Long? = null,
)
