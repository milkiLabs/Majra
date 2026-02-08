package com.milkilabs.majra.core.model

import kotlinx.serialization.Serializable

data class Source(
    val id: String,
    val name: String,
    val type: SourceTypeId,
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
    val sourceType: SourceTypeId,
    val title: String,
    val summary: String,
    val content: String?,
    val url: String,
    val author: String?,
    val audioUrl: String?,
    val audioMimeType: String?,
    val audioDurationSeconds: Int?,
    val episodeNumber: Int?,
    val imageUrl: String?,
    val publishedAtMillis: Long?,
    val isSaved: Boolean,
    val readState: ReadState,
)

@Serializable
sealed interface SourceTypeId {
    val value: String

    @Serializable
    data object Rss : SourceTypeId {
        override val value: String = "rss"
    }

    @Serializable
    data object Podcast : SourceTypeId {
        override val value: String = "podcast"
    }

    @Serializable
    data object Youtube : SourceTypeId {
        override val value: String = "youtube"
    }

    @Serializable
    data object Medium : SourceTypeId {
        override val value: String = "medium"
    }

    @Serializable
    data object Bluesky : SourceTypeId {
        override val value: String = "bluesky"
    }

    @Serializable
    data class Custom(override val value: String) : SourceTypeId

    companion object {
        fun fromValue(value: String): SourceTypeId = when (value) {
            Rss.value -> Rss
            Podcast.value -> Podcast
            Youtube.value -> Youtube
            Medium.value -> Medium
            Bluesky.value -> Bluesky
            else -> Custom(value)
        }
    }
}
