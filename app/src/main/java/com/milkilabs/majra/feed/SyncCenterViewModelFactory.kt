package com.milkilabs.majra.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.milkilabs.majra.core.repository.FeedRepository
import com.milkilabs.majra.medium.MediumSyncer
import com.milkilabs.majra.rss.RssSyncer
import com.milkilabs.majra.youtube.YoutubeSyncer

/** Factory for the global sync center view model. */
class SyncCenterViewModelFactory(
    private val repository: FeedRepository,
    private val rssSyncer: RssSyncer,
    private val youtubeSyncer: YoutubeSyncer,
    private val mediumSyncer: MediumSyncer,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SyncCenterViewModel(repository, rssSyncer, youtubeSyncer, mediumSyncer) as T
    }
}
