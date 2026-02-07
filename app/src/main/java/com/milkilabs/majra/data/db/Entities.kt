package com.milkilabs.majra.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val url: String,
)

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val sourceType: String,
    val title: String,
    val summary: String,
    val content: String?,
    val url: String,
    val author: String?,
    val publishedAtMillis: Long?,
    val isSaved: Boolean,
    val readState: String,
)
