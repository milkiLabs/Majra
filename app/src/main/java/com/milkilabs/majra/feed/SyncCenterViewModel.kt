package com.milkilabs.majra.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milkilabs.majra.core.model.Source
import com.milkilabs.majra.core.model.SourceTypes
import com.milkilabs.majra.core.repository.FeedRepository
import com.milkilabs.majra.medium.MediumSyncer
import com.milkilabs.majra.rss.RssSyncer
import com.milkilabs.majra.youtube.YoutubeSyncer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Centralized sync coordinator for the global Sync Center sheet.
 */
class SyncCenterViewModel(
    private val repository: FeedRepository,
    private val rssSyncer: RssSyncer,
    private val youtubeSyncer: YoutubeSyncer,
    private val mediumSyncer: MediumSyncer,
) : ViewModel() {
    private val statusState = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = statusState
    private val sourcesState: StateFlow<List<Source>> = repository.sources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sources: StateFlow<List<SourceListItem>> = sourcesState
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

    /** Sync every source sequentially to provide progress updates. */
    fun syncAll() {
        if (statusState.value.isSyncing) return
        viewModelScope.launch {
            runSync(sourcesState.value)
        }
    }

    /** Sync a single source on demand. */
    fun syncSource(sourceId: String) {
        if (statusState.value.isSyncing) return
        viewModelScope.launch {
            val source = sourcesState.value.firstOrNull { it.id == sourceId }
                ?: return@launch
            runSync(listOf(source))
        }
    }

    private suspend fun runSync(targets: List<Source>) {
        if (targets.isEmpty()) return
        statusState.value = SyncStatus(
            isSyncing = true,
            total = targets.size,
            completed = 0,
            currentSourceId = null,
            errorMessage = null,
            lastSyncedMillis = statusState.value.lastSyncedMillis,
        )
        var completed = 0
        var lastError: String? = null
        targets.forEach { source ->
            statusState.value = statusState.value.copy(currentSourceId = source.id)
            val result = runCatching { syncSourceInternal(source) }
            if (result.isFailure) {
                lastError = "Sync failed for ${source.name.ifBlank { source.url }}"
            }
            completed += 1
            statusState.value = statusState.value.copy(completed = completed)
        }
        statusState.value = statusState.value.copy(
            isSyncing = false,
            currentSourceId = null,
            lastSyncedMillis = System.currentTimeMillis(),
            errorMessage = lastError,
        )
    }

    private suspend fun syncSourceInternal(source: Source) {
        when (source.type) {
            SourceTypes.RSS -> rssSyncer.syncSource(source.id)
            SourceTypes.YOUTUBE -> youtubeSyncer.syncSource(source.id)
            SourceTypes.MEDIUM -> mediumSyncer.syncSource(source.id)
            else -> Unit
        }
    }
}

data class SyncStatus(
    val isSyncing: Boolean = false,
    val completed: Int = 0,
    val total: Int = 0,
    val currentSourceId: String? = null,
    val lastSyncedMillis: Long? = null,
    val errorMessage: String? = null,
)
