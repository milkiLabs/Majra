package com.milkilabs.majra.sync

import java.util.concurrent.TimeUnit
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.milkilabs.majra.settings.SyncPreferences

object SyncWorkScheduler {
    private const val UNIQUE_WORK_NAME = "background_sync"

    fun updateSchedule(
        context: Context,
        preferences: SyncPreferences,
    ) {
        if (preferences.isBackgroundSyncEnabled) {
            schedule(context, preferences.syncIntervalHours)
        } else {
            cancel(context)
        }
    }

    private fun schedule(
        context: Context,
        intervalHours: Int,
    ) {
        val safeInterval = intervalHours.coerceAtLeast(6)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            safeInterval.toLong(),
            TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
