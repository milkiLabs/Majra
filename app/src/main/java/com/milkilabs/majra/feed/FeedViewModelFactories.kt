package com.milkilabs.majra.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.milkilabs.majra.core.repository.FeedRepository
import com.milkilabs.majra.medium.MediumSyncer
import com.milkilabs.majra.rss.RssSyncer
import com.milkilabs.majra.youtube.YoutubeSyncer

/** Factory for the main feed view model. */
class FeedViewModelFactory(
    private val repository: FeedRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FeedViewModel(repository) as T
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

/** Factory for sources view model. */
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

/** Factory for article detail view model. */
class ArticleDetailViewModelFactory(
    private val repository: FeedRepository,
    private val articleId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ArticleDetailViewModel(repository, articleId) as T
    }
}

/** Factory for source detail view model. */
class SourceDetailViewModelFactory(
    private val repository: FeedRepository,
    private val sourceId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SourceDetailViewModel(repository, sourceId) as T
    }
}
