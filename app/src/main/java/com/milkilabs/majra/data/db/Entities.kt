package com.milkilabs.majra.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.milkilabs.majra.core.model.SourceTypeId

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: SourceTypeId,
    val url: String,
)

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
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
    val readState: String,
)
