package com.milkilabs.majra.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prof18.rssparser.RssParserBuilder
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import com.milkilabs.majra.bluesky.BlueskySourcePlugin
import com.milkilabs.majra.core.source.SourcePlugin
import com.milkilabs.majra.core.source.SourcePluginRegistry
import com.milkilabs.majra.core.viewer.ArticleViewer
import com.milkilabs.majra.data.db.AppDatabase
import com.milkilabs.majra.medium.MediumSourcePlugin
import com.milkilabs.majra.medium.MediumSyncer
import com.milkilabs.majra.medium.MediumUrlResolver
import com.milkilabs.majra.podcast.PodcastSourcePlugin
import com.milkilabs.majra.podcast.PodcastSyncer
import com.milkilabs.majra.rss.RssSourcePlugin
import com.milkilabs.majra.rss.RssSyncer
import com.milkilabs.majra.settings.SyncPreferencesRepository
import com.milkilabs.majra.settings.syncPreferencesDataStore
import com.milkilabs.majra.youtube.YoutubeSourcePlugin
import com.milkilabs.majra.youtube.YoutubeSyncer
import com.milkilabs.majra.youtube.YoutubeUrlResolver

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val syncPreferencesRepository = SyncPreferencesRepository(
            applicationContext.syncPreferencesDataStore,
        )
        val preferences = syncPreferencesRepository.syncPreferences.first()
        val now = System.currentTimeMillis()
        syncPreferencesRepository.updateSyncAttempt(now)

        val database = AppDatabase.create(applicationContext)
        val sourceDao = database.sourceDao()
        val articleDao = database.articleDao()
        val sourceCount = sourceDao.countSources()
        if (sourceCount == 0) {
            return Result.success()
        }

        val registry = buildSourceRegistry(database)
        val syncSucceeded = syncAllSources(registry.enabled)
        if (!syncSucceeded) {
            return Result.retry()
        }

        val newCount = articleDao.countArticles()
        val delta = (newCount - preferences.lastArticleCount).coerceAtLeast(0)
        val shouldNotify = shouldNotify(
            delta = delta,
            notifyOnNewItems = preferences.notifyOnNewItems,
            lastNotifiedMillis = preferences.lastNotifiedMillis,
            nowMillis = now,
        )
        if (shouldNotify) {
            SyncNotificationHelper.notifyNewItems(applicationContext, delta)
            syncPreferencesRepository.updateLastNotified(now)
        }
        syncPreferencesRepository.updateArticleCount(newCount)
        syncPreferencesRepository.updateSyncSuccess(now)
        return Result.success()
    }

    private fun buildSourceRegistry(database: AppDatabase): SourcePluginRegistry {
        val httpClient = OkHttpClient()
        val rssParser = RssParserBuilder().build()
        val mediumUrlResolver = MediumUrlResolver()
        val rssSyncer = RssSyncer(
            sourceDao = database.sourceDao(),
            articleDao = database.articleDao(),
            parser = rssParser,
        )
        val podcastSyncer = PodcastSyncer(
            sourceDao = database.sourceDao(),
            articleDao = database.articleDao(),
            parser = rssParser,
        )
        val youtubeSyncer = YoutubeSyncer(
            sourceDao = database.sourceDao(),
            articleDao = database.articleDao(),
            parser = rssParser,
            urlResolver = YoutubeUrlResolver(httpClient),
        )
        val mediumSyncer = MediumSyncer(
            sourceDao = database.sourceDao(),
            articleDao = database.articleDao(),
            parser = rssParser,
            urlResolver = mediumUrlResolver,
        )
        val noopViewer = ArticleViewer { }
        val plugins = listOf<SourcePlugin>(
            RssSourcePlugin(rssSyncer, viewer = noopViewer),
            PodcastSourcePlugin(podcastSyncer, viewer = noopViewer),
            YoutubeSourcePlugin(youtubeSyncer, viewer = noopViewer),
            MediumSourcePlugin(mediumSyncer, viewer = noopViewer),
            BlueskySourcePlugin(viewer = noopViewer),
        )
        return SourcePluginRegistry(plugins)
    }

    private suspend fun syncAllSources(plugins: List<SourcePlugin>): Boolean {
        var success = true
        plugins.forEach { plugin ->
            val result = runCatching { plugin.syncAll() }
            if (result.isFailure) {
                success = false
            }
        }
        return success
    }

    private fun shouldNotify(
        delta: Int,
        notifyOnNewItems: Boolean,
        lastNotifiedMillis: Long?,
        nowMillis: Long,
    ): Boolean {
        if (!notifyOnNewItems) return false
        if (delta < MIN_NEW_ITEMS) return false
        val lastNotify = lastNotifiedMillis ?: return true
        return nowMillis - lastNotify >= MIN_NOTIFY_INTERVAL_MILLIS
    }

    private companion object {
        const val MIN_NEW_ITEMS = 3
        const val MIN_NOTIFY_INTERVAL_MILLIS = 6 * 60 * 60 * 1000L
    }
}
