package com.milkilabs.majra.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.milkilabs.majra.core.repository.FeedRepository
import com.milkilabs.majra.core.source.SourcePluginRegistry

/** Factory for the Manage Sources view model. */
class ManageSourcesViewModelFactory(
    private val repository: FeedRepository,
    private val sourceRegistry: SourcePluginRegistry,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManageSourcesViewModel(
            repository,
            sourceRegistry,
        ) as T
    }
}
