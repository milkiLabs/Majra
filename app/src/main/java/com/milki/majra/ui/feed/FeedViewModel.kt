package com.milki.majra.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milki.majra.data.model.FeedItem
import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.SocialProfile
import com.milki.majra.data.repository.FeedRepository
import com.milki.majra.data.repository.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedViewModel(
    private val repository: FeedRepository,
) : ViewModel() {
    private val syncingSourceKey = MutableStateFlow<String?>(null)
    private val loadingOlderSourceKey = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)

    private val feedData = combine(
        repository.session(Platform.INSTAGRAM), // TODO: Eventually track auth state per platform if needed by UI globally
        repository.feed,
        repository.accounts,
    ) { session, feed, accounts ->
        FeedData(session.isAuthenticated, feed, accounts)
    }

    val uiState: StateFlow<FeedUiState> = combine(
        feedData,
        syncingSourceKey,
        loadingOlderSourceKey,
        message,
    ) { data, currentSync, currentOlderLoad, currentMessage ->
        FeedUiState(
            isAuthenticated = data.isAuthenticated,
            feed = data.feed,
            accounts = data.accounts,
            syncingSourceKey = currentSync,
            loadingOlderSourceKey = currentOlderLoad,
            message = currentMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedUiState(),
    )

    fun sync(platform: Platform, accountId: String, username: String) {
        viewModelScope.launch {
            val sourceKey = "${platform.storageKey}:$accountId"
            syncingSourceKey.value = sourceKey
            message.value = null
            when (val result = repository.syncSource(platform, accountId)) {
                is SyncResult.Success -> message.value = "Synced ${result.postCount} posts from @${result.username}."
                is SyncResult.Failure -> message.value = result.message
            }
            syncingSourceKey.value = null
        }
    }

    fun loadOlder(platform: Platform, accountId: String) {
        viewModelScope.launch {
            val sourceKey = "${platform.storageKey}:$accountId"
            loadingOlderSourceKey.value = sourceKey
            message.value = null
            when (val result = repository.loadOlderPosts(platform, accountId)) {
                is SyncResult.Success -> message.value = if (result.postCount == 0) {
                    "No older posts found for @${result.username}."
                } else {
                    "Loaded ${result.postCount} older posts from @${result.username}."
                }
                is SyncResult.Failure -> message.value = result.message
            }
            loadingOlderSourceKey.value = null
        }
    }

    fun postsForAccount(platform: Platform, accountId: String): Flow<List<FeedItem>> =
        repository.postsForAccount(platform, accountId)

    fun dismissMessage() {
        message.value = null
    }
}

data class FeedUiState(
    val isAuthenticated: Boolean = false,
    val feed: List<FeedItem> = emptyList(),
    val accounts: List<SocialProfile> = emptyList(),
    val syncingSourceKey: String? = null,
    val loadingOlderSourceKey: String? = null,
    val message: String? = null,
) {
    val isBusy: Boolean
        get() = syncingSourceKey != null || loadingOlderSourceKey != null
}

private data class FeedData(
    val isAuthenticated: Boolean,
    val feed: List<FeedItem>,
    val accounts: List<SocialProfile>,
)
