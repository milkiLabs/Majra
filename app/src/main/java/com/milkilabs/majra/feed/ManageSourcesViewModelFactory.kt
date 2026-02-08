package com.milkilabs.majra.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.milkilabs.majra.core.repository.FeedRepository
import com.milkilabs.majra.medium.MediumSyncer
import com.milkilabs.majra.podcast.PodcastSyncer
import com.milkilabs.majra.rss.RssSyncer
import com.milkilabs.majra.youtube.YoutubeSyncer

/** Factory for the Manage Sources view model. */
class ManageSourcesViewModelFactory(
    private val repository: FeedRepository,
    private val rssSyncer: RssSyncer,
    private val podcastSyncer: PodcastSyncer,
    private val youtubeSyncer: YoutubeSyncer,
    private val mediumSyncer: MediumSyncer,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManageSourcesViewModel(
            repository,
            rssSyncer,
            podcastSyncer,
            youtubeSyncer,
            mediumSyncer,
        ) as T
    }
}
