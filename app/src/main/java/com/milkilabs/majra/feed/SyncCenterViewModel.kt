package com.milkilabs.majra.feed

import java.net.URI
import java.util.Locale
import java.util.UUID
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milkilabs.majra.core.model.Source
import com.milkilabs.majra.core.model.SourceTypes
import com.milkilabs.majra.core.repository.FeedRepository
import com.milkilabs.majra.medium.MediumResolveResult
import com.milkilabs.majra.medium.MediumSyncer
import com.milkilabs.majra.rss.RssSyncer
import com.milkilabs.majra.youtube.YoutubeResolveResult
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
    private val isAddingState = MutableStateFlow(false)
    val isAdding: StateFlow<Boolean> = isAddingState
    private val addErrorState = MutableStateFlow<String?>(null)
    val addError: StateFlow<String?> = addErrorState
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

    /** Validate input, resolve to canonical feed URL, then add and sync. */
    fun addSource(
        url: String,
        type: String,
    ) {
        val trimmedUrl = url.trim()
        if (isAddingState.value) return
        addErrorState.value = null
        if (trimmedUrl.isBlank()) {
            addErrorState.value = "URL cannot be blank."
            return
        }
        if (type == SourceTypes.RSS && !isValidUrl(trimmedUrl)) {
            addErrorState.value = "Enter a valid URL."
            return
        }
        if (type == SourceTypes.YOUTUBE && !isValidUrl(trimmedUrl) && !trimmedUrl.startsWith("@")) {
            addErrorState.value = "Enter a valid YouTube URL or handle."
            return
        }
        if (type == SourceTypes.MEDIUM && !isValidUrl(trimmedUrl) && !trimmedUrl.startsWith("@")) {
            addErrorState.value = "Enter a valid Medium URL or handle."
            return
        }
        viewModelScope.launch {
            isAddingState.value = true
            try {
                val resolved = when (type) {
                    SourceTypes.RSS -> resolveRssSource(trimmedUrl)
                    SourceTypes.YOUTUBE -> resolveYoutubeSource(trimmedUrl)
                    SourceTypes.MEDIUM -> resolveMediumSource(trimmedUrl)
                    else -> null
                }
                if (resolved == null) {
                    addErrorState.value = "Unsupported source type."
                    return@launch
                }
                if (resolved.errorMessage != null) {
                    addErrorState.value = resolved.errorMessage
                    return@launch
                }
                val finalUrl = resolved.url
                if (sourcesState.value.any { it.url.equals(finalUrl, ignoreCase = true) }) {
                    addErrorState.value = "Source already exists."
                    return@launch
                }
                val finalName = resolved.name?.ifBlank { null }
                    ?: fallbackName(finalUrl)
                val source = Source(
                    id = UUID.randomUUID().toString(),
                    name = finalName,
                    type = type,
                    url = finalUrl,
                )
                repository.addSource(source)
                if (!statusState.value.isSyncing) {
                    runSync(listOf(source))
                }
            } catch (exception: Exception) {
                addErrorState.value = "Could not add source. Please try again."
            } finally {
                isAddingState.value = false
            }
        }
    }

    /** Update display name or URL for a source. */
    fun updateSource(
        sourceId: String,
        name: String,
        url: String,
    ) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) return
        val trimmedName = name.trim()
        viewModelScope.launch {
            repository.updateSource(sourceId, trimmedName, trimmedUrl)
        }
    }

    /** Remove the source and its articles. */
    fun removeSource(sourceId: String) {
        viewModelScope.launch {
            repository.removeSource(sourceId)
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

    private suspend fun resolveRssSource(url: String): ResolvedSource {
        val resolvedName = rssSyncer.resolveTitle(url)
        return ResolvedSource(url = url, name = resolvedName, errorMessage = null)
    }

    private suspend fun resolveYoutubeSource(url: String): ResolvedSource {
        return when (val result = youtubeSyncer.resolveSource(url)) {
            is YoutubeResolveResult.Error -> ResolvedSource(
                url = url,
                name = null,
                errorMessage = result.message,
            )
            is YoutubeResolveResult.Success -> ResolvedSource(
                url = result.source.feedUrl,
                name = result.source.displayName,
                errorMessage = null,
            )
        }
    }

    private suspend fun resolveMediumSource(url: String): ResolvedSource {
        return when (val result = mediumSyncer.resolveSource(url)) {
            is MediumResolveResult.Error -> ResolvedSource(
                url = url,
                name = null,
                errorMessage = result.message,
            )
            is MediumResolveResult.Success -> ResolvedSource(
                url = result.source.feedUrl,
                name = result.source.displayName,
                errorMessage = null,
            )
        }
    }

    private data class ResolvedSource(
        val url: String,
        val name: String?,
        val errorMessage: String?,
    )

    // Keep URL checks lightweight while catching obvious mistakes.
    private fun isValidUrl(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase(Locale.US)
        return !scheme.isNullOrBlank() && !uri.host.isNullOrBlank() &&
            (scheme == "http" || scheme == "https")
    }

    private fun fallbackName(url: String): String {
        val host = runCatching { URI(url).host }.getOrNull()
        val cleaned = host?.removePrefix("www.")?.takeIf { it.isNotBlank() }
        return cleaned ?: url
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
