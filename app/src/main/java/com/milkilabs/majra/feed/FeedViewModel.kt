package com.milkilabs.majra.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milkilabs.majra.core.model.ReadState
import com.milkilabs.majra.core.model.Source
import com.milkilabs.majra.core.repository.FeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Orchestrates the main feed list, filtering, and source picker data.
 */
class FeedViewModel(
    private val repository: FeedRepository,
) : ViewModel() {
    private val sourcesState: StateFlow<List<Source>> = repository.sources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val filtersState = MutableStateFlow(FeedFilters())

    /** Current filter selections for the feed screen. */
    val filters: StateFlow<FeedFilters> = filtersState

    /** Source list for filter UI and pickers. */
    val sourceItems: StateFlow<List<SourceListItem>> = sourcesState
        .map { sources ->
            sources.map { source ->
                SourceListItem(
                    id = source.id,
                    name = source.name,
                    type = source.type,
                    url = source.url,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Feed items filtered by read status, source type, and source id. */
    val items: StateFlow<List<FeedListItem>> = combine(
        repository.feed,
        sourcesState,
        filtersState,
    ) { articles, sources, filters ->
        val sourceNames = sources.associateBy({ it.id }, { it.name })
        articles
            .asSequence()
            .filter { article ->
                when (filters.readFilter) {
                    FeedReadFilter.Unread -> article.readState == ReadState.Unread
                    FeedReadFilter.Read -> article.readState == ReadState.Read
                    FeedReadFilter.All -> true
                }
            }
            .filter { article ->
                filters.sourceType?.let { type -> article.sourceType == type } ?: true
            }
            .filter { article ->
                filters.sourceId?.let { id -> article.sourceId == id } ?: true
            }
            .map { article ->
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
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Cycle read filter: Unread -> Read -> All. */
    fun cycleReadFilter() {
        val current = filtersState.value
        val next = when (current.readFilter) {
            FeedReadFilter.Unread -> FeedReadFilter.Read
            FeedReadFilter.Read -> FeedReadFilter.All
            FeedReadFilter.All -> FeedReadFilter.Unread
        }
        filtersState.value = current.copy(readFilter = next)
    }

    /** Apply a source type filter; clears incompatible source selections. */
    fun setSourceTypeFilter(type: String?) {
        val current = filtersState.value
        val nextSourceId = if (type == null) {
            null
        } else {
            current.sourceId?.takeIf { sourceId ->
                sourcesState.value.firstOrNull { it.id == sourceId }?.type == type
            }
        }
        filtersState.value = current.copy(sourceType = type, sourceId = nextSourceId)
    }

    /** Apply a source filter and align the type filter to the selected source. */
    fun setSourceFilter(sourceId: String?) {
        val current = filtersState.value
        if (sourceId == null) {
            filtersState.value = current.copy(sourceId = null)
            return
        }
        val source = sourcesState.value.firstOrNull { it.id == sourceId }
        filtersState.value = current.copy(
            sourceId = sourceId,
            sourceType = source?.type ?: current.sourceType,
        )
    }

    /** Reset filters to the default unread-only view. */
    fun clearFilters() {
        filtersState.value = FeedFilters()
    }

    /** Reset only the read filter back to unread. */
    fun resetReadFilter() {
        filtersState.value = filtersState.value.copy(readFilter = FeedReadFilter.Unread)
    }

    /** Update the read state for a feed item. */
    fun setReadState(articleId: String, state: ReadState) {
        viewModelScope.launch {
            repository.markRead(articleId, state)
        }
    }
}
