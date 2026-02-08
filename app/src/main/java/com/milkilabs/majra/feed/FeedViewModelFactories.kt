package com.milkilabs.majra.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.milkilabs.majra.core.repository.FeedRepository
import com.milkilabs.majra.core.source.SourcePluginRegistry

/** Factory for the main feed view model. */
class FeedViewModelFactory(
    private val repository: FeedRepository,
    private val sourceRegistry: SourcePluginRegistry,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FeedViewModel(repository, sourceRegistry) as T
    }
}

/** Factory for saved items view model. */
class SavedViewModelFactory(
    private val repository: FeedRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SavedViewModel(repository) as T
    }
}

/** Factory for article detail view model. */
class ArticleDetailViewModelFactory(
    private val repository: FeedRepository,
    private val articleId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ArticleDetailViewModel(repository, articleId) as T
    }
}

