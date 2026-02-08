package com.milkilabs.majra.podcast

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

class PodcastSyncer(
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

    suspend fun syncAll() = withContext(Dispatchers.IO) {
        val sources = sourceDao.getSourcesByType(SourceTypes.PODCAST)
        syncSources(sources)
    }

    suspend fun syncSource(sourceId: String) = withContext(Dispatchers.IO) {
        val source = sourceDao.getSourceById(sourceId) ?: return@withContext
        if (source.type != SourceTypes.PODCAST) return@withContext
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
                    audioUrl = audioUrlFor(item),
                    audioMimeType = audioMimeTypeFor(item),
                    audioDurationSeconds = durationSecondsFor(item),
                    episodeNumber = episodeNumberFor(item),
                    imageUrl = imageUrlFor(item),
                    publishedAtMillis = publishedAtMillisFor(item),
                    isSaved = false,
                    readState = ReadState.Unread.name,
                )
            }

            if (candidates.isEmpty()) return@forEach

            val existing = articleDao.getByIds(candidates.map { it.id })
                .associateBy { it.id }
            val merged = candidates.map { candidate ->
                val current = existing[candidate.id]
                val resolvedPublishedAt = when {
                    current?.publishedAtMillis != null -> current.publishedAtMillis
                    candidate.publishedAtMillis != null -> candidate.publishedAtMillis
                    else -> System.currentTimeMillis()
                }
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

    private fun audioUrlFor(item: RssItem): String? {
        return firstNonBlank(
            nestedStringProperty(item, "enclosure", "url"),
            stringProperty(item, "enclosureUrl"),
            stringProperty(item, "audioUrl"),
            stringProperty(item, "audio"),
        )
    }

    private fun audioMimeTypeFor(item: RssItem): String? {
        return firstNonBlank(
            nestedStringProperty(item, "enclosure", "type"),
            stringProperty(item, "enclosureType"),
        )
    }

    private fun durationSecondsFor(item: RssItem): Int? {
        return parseDurationSeconds(
            firstNonBlank(
                stringProperty(item, "itunesDuration"),
                stringProperty(item, "duration"),
            ),
        )
    }

    private fun episodeNumberFor(item: RssItem): Int? {
        val raw = firstNonBlank(
            stringProperty(item, "itunesEpisode"),
            stringProperty(item, "episode"),
        )
        return raw?.toIntOrNull()
    }

    private fun imageUrlFor(item: RssItem): String? {
        return firstNonBlank(
            stringProperty(item, "itunesImage"),
            stringProperty(item, "imageUrl"),
            nestedStringProperty(item, "image", "url"),
        )
    }

    private fun parseDurationSeconds(raw: String?): Int? {
        val text = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (text.all { it.isDigit() }) {
            return text.toIntOrNull()
        }
        val parts = text.split(":")
        if (parts.size !in 2..3) return null
        if (parts.any { it.isBlank() || it.any { ch -> !ch.isDigit() } }) return null
        val numbers = parts.mapNotNull { it.toIntOrNull() }
        if (numbers.size != parts.size) return null
        return if (numbers.size == 2) {
            numbers[0] * 60 + numbers[1]
        } else {
            numbers[0] * 3600 + numbers[1] * 60 + numbers[2]
        }
    }


    // Use reflection to read optional RSS parser fields across versions.
    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim()
    }

    private fun stringProperty(target: Any, name: String): String? {
        val method = target.javaClass.methods.firstOrNull { method ->
            method.name == getterName(name) && method.parameterCount == 0
        }
        val value = runCatching { method?.invoke(target) }.getOrNull() as? String
        return value?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun nestedStringProperty(target: Any, name: String, nested: String): String? {
        val parent = objectProperty(target, name) ?: return null
        return stringProperty(parent, nested)
    }

    private fun objectProperty(target: Any, name: String): Any? {
        val method = target.javaClass.methods.firstOrNull { method ->
            method.name == getterName(name) && method.parameterCount == 0
        }
        return runCatching { method?.invoke(target) }.getOrNull()
    }

    private fun getterName(name: String): String {
        if (name.isEmpty()) return name
        val first = name[0].uppercaseChar()
        return "get$first${name.substring(1)}"
    }
}
