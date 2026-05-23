package com.milki.majra.data.repository

import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.SocialPost
import com.milki.majra.data.model.SocialProfile

/**
 * Platform-agnostic interface that each social media platform implements
 * to provide profile syncing and pagination.
 *
 * To add a new platform, implement this interface and register the client
 * in [AppContainer].
 */
interface FeedSourceClient {
    val platform: Platform
    suspend fun syncProfile(sourceId: String): SourceSyncPage
    suspend fun loadOlderPosts(profile: SocialProfile): SourceSyncPage
}

data class SourceSyncPage(
    val account: SocialProfile,
    val userId: String?,
    val posts: List<SocialPost>,
    val nextPageToken: String?,
    val hasMorePosts: Boolean,
)
