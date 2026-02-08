package com.milkilabs.majra.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milkilabs.majra.core.model.ReadState
import com.milkilabs.majra.core.repository.FeedRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Provides a single article with resolved source name for the detail view.
 */
class ArticleDetailViewModel(
    private val repository: FeedRepository,
    private val articleId: String,
) : ViewModel() {
    val state: StateFlow<ArticleDetailState> = combine(
        repository.feed,
        repository.sources,
    ) { articles, sources ->
        val article = articles.firstOrNull { it.id == articleId }
        val sourceName = article?.let { current ->
            sources.firstOrNull { it.id == current.sourceId }?.name
        }.orEmpty()
        ArticleDetailState(article = article, sourceName = sourceName)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ArticleDetailState(article = null, sourceName = ""),
    )

    /** Mark the current article as read. */
    fun markRead() {
        viewModelScope.launch {
            repository.markRead(articleId, ReadState.Read)
        }
    }

    /** Toggle saved state for the current article. */
    fun toggleSaved() {
        viewModelScope.launch {
            repository.toggleSaved(articleId)
        }
    }
}
