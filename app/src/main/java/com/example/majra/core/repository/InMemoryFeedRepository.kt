package com.example.majra.core.repository

import com.example.majra.core.model.Article
import com.example.majra.core.model.ReadState
import com.example.majra.core.model.Source
import com.example.majra.core.model.SourceTypes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class InMemoryFeedRepository : FeedRepository {
    private val sourcesState = MutableStateFlow(
        listOf(
            Source(
                id = "source-rss-1",
                name = "The Humane Web",
                type = SourceTypes.RSS,
                url = "https://example.com/feed.xml",
            ),
        )
    )
    private val articlesState = MutableStateFlow(
        listOf(
            Article(
                id = "rss-101",
                sourceId = "source-rss-1",
                sourceType = SourceTypes.RSS,
                title = "Designing calmer feeds",
                summary = "Ideas for building quiet, focused reading experiences.",
                content = "A gentle approach to reader-first design.",
                url = "https://example.com/reader-first",
                author = "Humane Web",
                publishedAtMillis = null,
                isSaved = false,
                readState = ReadState.Unread,
            ),
        )
    )

    override val sources: Flow<List<Source>> = sourcesState.asStateFlow()
    override val feed: Flow<List<Article>> = articlesState.asStateFlow()
    override val saved: Flow<List<Article>> = articlesState.map { articles ->
        articles.filter { it.isSaved }
    }

    override suspend fun addSource(source: Source) {
        sourcesState.value = sourcesState.value + source
    }

    override suspend fun updateSource(sourceId: String, name: String, url: String) {
        val updated = sourcesState.value.map { source ->
            if (source.id == sourceId) {
                source.copy(name = name, url = url)
            } else {
                source
            }
        }
        sourcesState.value = updated
    }

    override suspend fun removeSource(sourceId: String) {
        sourcesState.value = sourcesState.value.filterNot { it.id == sourceId }
        articlesState.value = articlesState.value.filterNot { it.sourceId == sourceId }
    }

    override suspend fun getArticle(articleId: String): Article? {
        return articlesState.value.firstOrNull { it.id == articleId }
    }

    override suspend fun markRead(articleId: String, state: ReadState) {
        updateArticle(articleId) { article ->
            article.copy(readState = state)
        }
    }

    override suspend fun toggleSaved(articleId: String) {
        updateArticle(articleId) { article ->
            article.copy(isSaved = !article.isSaved)
        }
    }

    private fun updateArticle(articleId: String, update: (Article) -> Article) {
        val updated = articlesState.value.map { article ->
            if (article.id == articleId) update(article) else article
        }
        articlesState.value = updated
    }
}
