package com.milkilabs.majra.feed

import com.milkilabs.majra.core.model.Article
import com.milkilabs.majra.core.model.ReadState
import com.milkilabs.majra.core.model.Source

/**
 * UI-friendly representation of a feed item, enriched with source metadata.
 */
data class FeedListItem(
    val id: String,
    val sourceId: String,
    val title: String,
    val sourceName: String,
    val sourceType: String,
    val summary: String,
    val readState: ReadState,
)

/**
 * Read filter applied to the feed list.
 */
enum class FeedReadFilter {
    Unread,
    Read,
    All,
}

/**
 * Active feed filters combined in the main screen.
 */
data class FeedFilters(
    val readFilter: FeedReadFilter = FeedReadFilter.Unread,
    val sourceType: String? = null,
    val sourceId: String? = null,
)

/**
 * Lightweight source list item for pickers and lists.
 */
data class SourceListItem(
    val id: String,
    val name: String,
    val type: String,
    val url: String,
)

/**
 * Detail UI state for a single article.
 */
data class ArticleDetailState(
    val article: Article?,
    val sourceName: String,
)

/**
 * Detail UI state for a single source.
 */
data class SourceDetailState(
    val source: Source?,
)
