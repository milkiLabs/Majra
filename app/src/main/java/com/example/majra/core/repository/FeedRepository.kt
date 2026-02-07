package com.example.majra.core.repository

import com.example.majra.core.model.Article
import com.example.majra.core.model.ReadState
import com.example.majra.core.model.Source
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    val sources: Flow<List<Source>>
    val feed: Flow<List<Article>>
    val saved: Flow<List<Article>>

    suspend fun getArticle(articleId: String): Article?
    suspend fun markRead(articleId: String, state: ReadState)
    suspend fun toggleSaved(articleId: String)
}
