package com.milkilabs.majra.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milkilabs.majra.core.repository.FeedRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds a single source for editing and removal.
 */
class SourceDetailViewModel(
    private val repository: FeedRepository,
    private val sourceId: String,
) : ViewModel() {
    val state: StateFlow<SourceDetailState> = repository.sources
        .map { sources ->
            SourceDetailState(source = sources.firstOrNull { it.id == sourceId })
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SourceDetailState(source = null),
        )

    /** Update display name or URL for the current source. */
    fun updateSource(name: String, url: String) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) return
        val trimmedName = name.trim()
        viewModelScope.launch {
            repository.updateSource(sourceId, trimmedName, trimmedUrl)
        }
    }

    /** Remove the source and its articles. */
    fun removeSource() {
        viewModelScope.launch {
            repository.removeSource(sourceId)
        }
    }
}
