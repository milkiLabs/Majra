package com.milkilabs.majra.medium

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
import com.milkilabs.majra.core.model.SourceTypeId
import com.milkilabs.majra.data.db.ArticleDao
import com.milkilabs.majra.data.db.ArticleEntity
import com.milkilabs.majra.data.db.SourceDao

/**
 * Sync Medium sources by reading RSS feeds for each stored source.
 */
class MediumSyncer(
    private val sourceDao: SourceDao,
    private val articleDao: ArticleDao,
    private val parser: RssParser,
    private val urlResolver: MediumUrlResolver,
) {
    /** Resolve a user input into a normalized Medium feed URL plus display name. */
    suspend fun resolveSource(input: String): MediumResolveResult = withContext(Dispatchers.IO) {
        when (val resolved = urlResolver.resolve(input)) {
            is MediumResolveResult.Error -> resolved
            is MediumResolveResult.Success -> {
                val title = runCatching { parser.getRssChannel(resolved.source.feedUrl) }
                    .getOrNull()
                    ?.title
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                MediumResolveResult.Success(
                    resolved.source.copy(displayName = title ?: resolved.source.displayName),
                )
            }
        }
    }

    /** Fetch Medium feed items for all stored Medium sources and upsert them into Room. */
    suspend fun syncAll() = withContext(Dispatchers.IO) {
        val sources = sourceDao.getSourcesByType(SourceTypeId.Medium)
        syncSources(sources)
    }

    /** Fetch Medium feed items for a single stored source. */
    suspend fun syncSource(sourceId: String) = withContext(Dispatchers.IO) {
        val source = sourceDao.getSourceById(sourceId) ?: return@withContext
        if (source.type != SourceTypeId.Medium) return@withContext
        syncSources(listOf(source))
    }

    private suspend fun syncSources(sources: List<com.milkilabs.majra.data.db.SourceEntity>) {
        if (sources.isEmpty()) return

        val pendingArticles = mutableListOf<ArticleEntity>()
        sources.forEach { source ->
            val channel = parser.getRssChannel(source.url)
            val items = channel.items.orEmpty()
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
                    audioUrl = null,
                    audioMimeType = null,
                    audioDurationSeconds = null,
                    episodeNumber = null,
                    imageUrl = null,
                    publishedAtMillis = publishedAtMillisFor(item),
                    isSaved = false,
                    readState = ReadState.Unread.name,
                )
            }

            if (candidates.isEmpty()) return@forEach

            val existing = fetchExisting(candidates.map { it.id })
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

    private suspend fun fetchExisting(ids: List<String>): Map<String, ArticleEntity> {
        if (ids.isEmpty()) return emptyMap()
        val results = mutableMapOf<String, ArticleEntity>()
        ids.chunked(500).forEach { chunk ->
            articleDao.getByIds(chunk).forEach { entity ->
                results[entity.id] = entity
            }
        }
        return results
    }

    private fun stableId(sourceId: String, stableKey: String): String {
        return UUID.nameUUIDFromBytes("$sourceId|$stableKey".toByteArray()).toString()
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

    // Try common RSS/Atom date formats; fall back to null when unknown.
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
}
