package com.example.majra.rss

import java.util.UUID
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.majra.core.model.ReadState
import com.example.majra.core.model.SourceTypes
import com.example.majra.data.db.ArticleDao
import com.example.majra.data.db.ArticleEntity
import com.example.majra.data.db.SourceDao

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
        if (sources.isEmpty()) return@withContext

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
                    publishedAtMillis = null,
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
                if (current == null) {
                    candidate
                } else {
                    candidate.copy(
                        isSaved = current.isSaved,
                        readState = current.readState,
                    )
                }
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
        if (description.isNotEmpty()) return description
        return item.content?.trim().orEmpty()
    }
}
