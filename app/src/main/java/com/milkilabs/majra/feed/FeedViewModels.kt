package com.milkilabs.majra.feed

import java.net.URI
import java.util.Locale
import java.util.UUID
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.milkilabs.majra.core.model.Article
import com.milkilabs.majra.core.model.ReadState
import com.milkilabs.majra.core.model.Source
import com.milkilabs.majra.core.model.SourceTypes
import com.milkilabs.majra.core.repository.FeedRepository
import com.milkilabs.majra.medium.MediumResolveResult
import com.milkilabs.majra.medium.MediumSyncer
import com.milkilabs.majra.rss.RssSyncer
import com.milkilabs.majra.youtube.YoutubeResolveResult
import com.milkilabs.majra.youtube.YoutubeSyncer

data class FeedListItem(
    val id: String,
    val sourceId: String,
    val title: String,
    val sourceName: String,
    val sourceType: String,
    val summary: String,
    val readState: ReadState,
)

data class SourceListItem(
    val id: String,
    val name: String,
    val type: String,
    val url: String,
)

data class ArticleDetailState(
    val article: Article?,
    val sourceName: String,
)

data class SourceDetailState(
    val source: Source?,
)

class FeedViewModel(
    private val repository: FeedRepository,
) : ViewModel() {
    val items: StateFlow<List<FeedListItem>> = combine(
        repository.feed,
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

class SourcesViewModel(
    private val repository: FeedRepository,
    private val rssSyncer: RssSyncer,
    private val youtubeSyncer: YoutubeSyncer,
    private val mediumSyncer: MediumSyncer,
) : ViewModel() {
    private val isAddingState = MutableStateFlow(false)
    val isAdding: StateFlow<Boolean> = isAddingState
    private val isSyncingState = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = isSyncingState
    private val addErrorState = MutableStateFlow<String?>(null)
    val addError: StateFlow<String?> = addErrorState
    private val syncErrorState = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = syncErrorState
    val items: StateFlow<List<SourceListItem>> = repository.sources
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
                if (items.value.any { it.url.equals(finalUrl, ignoreCase = true) }) {
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
                syncSourcesInternal()
            } catch (exception: Exception) {
                addErrorState.value = "Could not add source. Please try again."
            } finally {
                isAddingState.value = false
            }
        }
    }

    fun syncSources() {
        viewModelScope.launch {
            syncSourcesInternal()
        }
    }

    private suspend fun syncSourcesInternal() {
        if (isSyncingState.value) return
        isSyncingState.value = true
        syncErrorState.value = null
        try {
            rssSyncer.syncAll()
            youtubeSyncer.syncAll()
            mediumSyncer.syncAll()
        } catch (exception: Exception) {
            syncErrorState.value = "Sync failed. Check your connection and try again."
        } finally {
            isSyncingState.value = false
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

    fun markRead() {
        viewModelScope.launch {
            repository.markRead(articleId, ReadState.Read)
        }
    }

    fun toggleSaved() {
        viewModelScope.launch {
            repository.toggleSaved(articleId)
        }
    }
}

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

    fun updateSource(name: String, url: String) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) return
        val trimmedName = name.trim()
        viewModelScope.launch {
            repository.updateSource(sourceId, trimmedName, trimmedUrl)
        }
    }

    fun removeSource() {
        viewModelScope.launch {
            repository.removeSource(sourceId)
        }
    }
}

class FeedViewModelFactory(
    private val repository: FeedRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FeedViewModel(repository) as T
    }
}

class SavedViewModelFactory(
    private val repository: FeedRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SavedViewModel(repository) as T
    }
}

class SourcesViewModelFactory(
    private val repository: FeedRepository,
    private val rssSyncer: RssSyncer,
    private val youtubeSyncer: YoutubeSyncer,
    private val mediumSyncer: MediumSyncer,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SourcesViewModel(repository, rssSyncer, youtubeSyncer, mediumSyncer) as T
    }
}

class ArticleDetailViewModelFactory(
    private val repository: FeedRepository,
    private val articleId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ArticleDetailViewModel(repository, articleId) as T
    }
}

class SourceDetailViewModelFactory(
    private val repository: FeedRepository,
    private val sourceId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SourceDetailViewModel(repository, sourceId) as T
    }
}
