package com.milkilabs.majra.rss

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import androidx.core.text.HtmlCompat
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.milkilabs.majra.core.model.ReadState
import com.milkilabs.majra.core.model.SourceTypes
import com.milkilabs.majra.data.db.ArticleDao
import com.milkilabs.majra.data.db.ArticleEntity
import com.milkilabs.majra.data.db.SourceDao

class RssSyncer(
    private val sourceDao: SourceDao,
    private val articleDao: ArticleDao,
    private val parser: RssParser,
) {
    suspend fun resolveTitle(url: String): String? = withContext(Dispatchers.IO) {
        runCatching { parser.getRssChannel(url) }
            .getOrNull()
            ?.title
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    // Fetch RSS items for all RSS sources and upsert them into Room.
    suspend fun syncAll() = withContext(Dispatchers.IO) {
        val sources = sourceDao.getSourcesByType(SourceTypes.RSS)
        syncSources(sources)
    }

    // Fetch RSS items for a single source.
    suspend fun syncSource(sourceId: String) = withContext(Dispatchers.IO) {
        val source = sourceDao.getSourceById(sourceId) ?: return@withContext
        if (source.type != SourceTypes.RSS) return@withContext
        syncSources(listOf(source))
    }

    private suspend fun syncSources(sources: List<com.milkilabs.majra.data.db.SourceEntity>) {
        if (sources.isEmpty()) return

        val pendingArticles = mutableListOf<ArticleEntity>()
        sources.forEach { source ->
            val channel = runCatching { parser.getRssChannel(source.url) }.getOrNull()
            val items = channel?.items.orEmpty()
            if (items.isEmpty()) return@forEach

            val candidates = items.mapNotNull { item ->
                val stableKey = item.guid ?: item.link ?: item.title ?: item.pubDate
                if (stableKey.isNullOrBlank()) return@mapNotNull null
                ArticleEntity(
                    id = stableId(source.id, stableKey),
                    sourceId = source.id,
                    sourceType = source.type,
                    title = item.title?.takeIf { it.isNotBlank() } ?: "Untitled",
                    summary = summaryFor(item),
                    content = item.content?.takeIf { it.isNotBlank() },
                    url = item.link ?: source.url,
                    author = item.author?.takeIf { it.isNotBlank() },
                    publishedAtMillis = publishedAtMillisFor(item),
                    isSaved = false,
                    readState = ReadState.Unread.name,
                )
            }

            if (candidates.isEmpty()) return@forEach

            val existing = articleDao.getByIds(candidates.map { it.id })
                .associateBy { it.id }
            // Preserve user state while refreshing fetched fields.
            val merged = candidates.map { candidate ->
                val current = existing[candidate.id]
                val resolvedPublishedAt = when {
                    current?.publishedAtMillis != null -> current.publishedAtMillis
                    candidate.publishedAtMillis != null -> candidate.publishedAtMillis
                    else -> System.currentTimeMillis()
                }
                // Preserve user state and keep timestamps stable across refreshes.
                candidate.copy(
                    publishedAtMillis = resolvedPublishedAt,
                    isSaved = current?.isSaved ?: candidate.isSaved,
                    readState = current?.readState ?: candidate.readState,
                )
            }
            pendingArticles.addAll(merged)
        }

        if (pendingArticles.isNotEmpty()) {
            articleDao.upsertAll(pendingArticles)
        }
    }

    private fun stableId(sourceId: String, stableKey: String): String {
        return UUID.nameUUIDFromBytes("$sourceId|$stableKey".toByteArray()).toString()
    }

    // Try common RSS date formats; fall back to null when unknown.
    private fun publishedAtMillisFor(item: RssItem): Long? {
        val dateText = item.pubDate?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val patterns = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm Z",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        )
        patterns.forEach { pattern ->
            val formatter = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            runCatching { formatter.parse(dateText)?.time }
                .getOrNull()
                ?.let { return it }
        }
        return null
    }

    private fun summaryFor(item: RssItem): String {
        val description = item.description?.trim().orEmpty()
        val raw = if (description.isNotEmpty()) {
            description
        } else {
            item.content?.trim().orEmpty()
        }
        return cleanSummary(raw)
    }

    private fun cleanSummary(raw: String): String {
        if (raw.isBlank()) return ""
        val plainText = HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_COMPACT)
            .toString()
            .replace("\\s+".toRegex(), " ")
            .trim()
        val limit = 200
        return if (plainText.length <= limit) {
            plainText
        } else {
            plainText.take(limit).trimEnd() + "..."
        }
    }
}
