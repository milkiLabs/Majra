package com.example.majra.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.majra.core.model.Article
import com.example.majra.core.model.ReadState
import com.example.majra.core.repository.FeedRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FeedListItem(
    val id: String,
    val title: String,
    val sourceName: String,
    val sourceType: String,
    val summary: String,
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
                title = article.title,
                sourceName = sourceNames[article.sourceId].orEmpty(),
                sourceType = article.sourceType,
                summary = article.summary,
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
                title = article.title,
                sourceName = sourceNames[article.sourceId].orEmpty(),
                sourceType = article.sourceType,
                summary = article.summary,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class SourcesViewModel(
    private val repository: FeedRepository,
) : ViewModel() {
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
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SourcesViewModel(repository) as T
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
