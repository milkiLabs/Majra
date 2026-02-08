package com.milkilabs.majra.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milkilabs.majra.core.repository.FeedRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Presents saved items with source names resolved for display.
 */
class SavedViewModel(
    private val repository: FeedRepository,
) : ViewModel() {
    val items: StateFlow<List<FeedListItem>> = combine(
        repository.saved,
        repository.sources,
    ) { articles, sources ->
        val sourceNames = sources.associateBy({ it.id }, { it.name })
        articles.map { article ->
            FeedListItem(
                id = article.id,
                sourceId = article.sourceId,
                title = article.title,
                sourceName = sourceNames[article.sourceId].orEmpty(),
                sourceType = article.sourceType,
                summary = article.summary,
                readState = article.readState,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
