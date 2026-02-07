package com.example.majra.feed

import java.net.URI
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
import com.example.majra.core.model.Article
import com.example.majra.core.model.ReadState
import com.example.majra.core.model.Source
import com.example.majra.core.model.SourceTypes
import com.example.majra.core.repository.FeedRepository
import com.example.majra.rss.RssSyncer

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
) : ViewModel() {
    private val isAddingState = MutableStateFlow(false)
    val isAdding: StateFlow<Boolean> = isAddingState
    val items: StateFlow<List<SourceListItem>> = repository.sources
        .map { sources ->
            sources.map { source ->
                SourceListItem(
                    id = source.id,
                    name = source.name,
                    type = source.type,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addSource(
        url: String,
        type: String,
    ) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) return
        viewModelScope.launch {
            isAddingState.value = true
            try {
            val resolvedName = if (type == SourceTypes.RSS) {
                rssSyncer.resolveTitle(trimmedUrl)
            } else {
                null
            }
            val finalName = resolvedName?.ifBlank { null }
                ?: fallbackName(trimmedUrl)
            val source = Source(
                id = UUID.randomUUID().toString(),
                name = finalName,
                type = type,
                url = trimmedUrl,
            )
            repository.addSource(source)
            if (type == SourceTypes.RSS) {
                rssSyncer.syncAll()
            }
            } finally {
                isAddingState.value = false
            }
        }
    }

    fun syncRss() {
        viewModelScope.launch {
            rssSyncer.syncAll()
        }
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
        val trimmedName = name.trim().ifBlank { trimmedUrl }
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
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SourcesViewModel(repository, rssSyncer) as T
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
