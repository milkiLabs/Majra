package com.milki.majra.data.platform.instagram

import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.SocialProfile
import com.milki.majra.data.repository.FeedSourceClient
import com.milki.majra.data.repository.SourceSyncPage

class InstagramFeedSourceClient(
    private val httpClient: InstagramHttpClient,
    private val parser: InstagramHtmlParser,
) : FeedSourceClient {
    override val platform: Platform = Platform.INSTAGRAM

    override suspend fun syncProfile(sourceId: String): SourceSyncPage {
        val profilePayload = runCatching {
            httpClient.fetchProfileJson(sourceId)
        }.getOrElse {
            httpClient.fetchProfileHtml(sourceId)
        }
        val profile = parser.parseProfile(sourceId, profilePayload)
        val feedPayload = runCatching {
            profile.userId?.let { httpClient.fetchUserFeedJson(it, profile.account.username) }
        }.getOrNull()
        val parsed = parser.parseProfile(
            username = profile.account.username,
            html = profilePayload,
            feedJson = feedPayload,
        )
        return SourceSyncPage(
            account = parsed.account,
            userId = parsed.userId,
            posts = parsed.posts,
            nextPageToken = parsed.nextMaxId,
            hasMorePosts = parsed.hasMorePosts,
        )
    }

    override suspend fun loadOlderPosts(profile: SocialProfile): SourceSyncPage {
        val userId = profile.userId ?: error("@${profile.username} needs one fresh sync before loading older posts.")
        val nextPageToken = profile.nextPageToken ?: error("No older posts are available for @${profile.username}.")
        val feedPayload = httpClient.fetchUserFeedJson(
            userId = userId,
            username = profile.username,
            maxId = nextPageToken,
        )
        val page = parser.parseFeedPage(profile.username, feedPayload, profile.accountId)
        return SourceSyncPage(
            account = profile,
            userId = userId,
            posts = page.posts,
            nextPageToken = page.nextMaxId,
            hasMorePosts = page.hasMorePosts,
        )
    }
}
