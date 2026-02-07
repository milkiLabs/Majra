package com.milkilabs.majra.core.model

data class Source(
    val id: String,
    val name: String,
    val type: String,
    val url: String,
)

data class Feed(
    val id: String,
    val sourceId: String,
    val title: String,
)

enum class ReadState {
    Unread,
    Read,
}

data class Article(
    val id: String,
    val sourceId: String,
    val sourceType: String,
    val title: String,
    val summary: String,
    val content: String?,
    val url: String,
    val author: String?,
    val publishedAtMillis: Long?,
    val isSaved: Boolean,
    val readState: ReadState,
)

object SourceTypes {
    const val RSS = "rss"
    const val YOUTUBE = "youtube"
    const val MEDIUM = "medium"
    const val BLUESKY = "bluesky"
}
