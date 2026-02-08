package com.milkilabs.majra.feed

import java.net.URI
import java.util.Locale
import java.util.UUID
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.milkilabs.majra.core.model.Source
import com.milkilabs.majra.core.model.SourceTypeId
import com.milkilabs.majra.core.repository.FeedRepository
import com.milkilabs.majra.core.source.SourceInputMode
import com.milkilabs.majra.core.source.SourcePluginRegistry
import com.milkilabs.majra.core.source.SourceResolveResult
import com.milkilabs.majra.core.source.SourceTypeUi

/**
 * Centralized sync coordinator for the Manage Sources sheet.
 */
class ManageSourcesViewModel(
    private val repository: FeedRepository,
    private val sourceRegistry: SourcePluginRegistry,
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

    val typeOptions: List<SourceTypeUi> = sourceRegistry.typeOptions()

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
        type: SourceTypeId,
    ) {
        val trimmedUrl = url.trim()
        if (isAddingState.value) return
        addErrorState.value = null
        if (trimmedUrl.isBlank()) {
            addErrorState.value = "URL cannot be blank."
            return
        }
        val plugin = sourceRegistry.pluginFor(type)
        if (plugin == null) {
            addErrorState.value = "Unsupported source type."
            return
        }
        if (!plugin.isEnabled) {
            addErrorState.value = "${plugin.displayName} sources are not supported yet."
            return
        }
        val inputError = validateInput(trimmedUrl, plugin.inputMode, plugin.displayName)
        if (inputError != null) {
            addErrorState.value = inputError
            return
        }
        viewModelScope.launch {
            isAddingState.value = true
            try {
                val resolved = plugin.resolve(trimmedUrl)
                val finalUrl = when (resolved) {
                    is SourceResolveResult.Error -> {
                        addErrorState.value = resolved.message
                        return@launch
                    }
                    is SourceResolveResult.Success -> resolved.url
                }
                if (sourcesState.value.any { it.url.equals(finalUrl, ignoreCase = true) }) {
                    addErrorState.value = "Source already exists."
                    return@launch
                }
                val finalName = (resolved as? SourceResolveResult.Success)
                    ?.name
                    ?.ifBlank { null }
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
        val plugin = sourceRegistry.pluginFor(source.type) ?: return
        plugin.syncSource(source.id)
    }

    // Keep URL checks lightweight while catching obvious mistakes.
    private fun isValidUrl(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase(Locale.US)
        return !scheme.isNullOrBlank() && !uri.host.isNullOrBlank() &&
            (scheme == "http" || scheme == "https")
    }

    private fun validateInput(
        input: String,
        inputMode: SourceInputMode,
        displayName: String,
    ): String? {
        return when (inputMode) {
            SourceInputMode.UrlOnly -> {
                if (!isValidUrl(input)) "Enter a valid URL." else null
            }
            SourceInputMode.UrlOrHandle -> {
                if (!isValidUrl(input) && !input.startsWith("@")) {
                    "Enter a valid $displayName URL or handle."
                } else {
                    null
                }
            }
        }
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
