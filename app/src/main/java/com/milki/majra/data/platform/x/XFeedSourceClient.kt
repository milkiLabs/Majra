package com.milki.majra.data.platform.x

import android.util.Log
import com.milki.majra.BuildConfig
import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.SocialProfile
import com.milki.majra.data.repository.FeedSourceClient
import com.milki.majra.data.repository.SourceSyncPage

class XFeedSourceClient(
    private val scraper: XWebViewScraper,
    private val parser: XGraphQLParser,
) : FeedSourceClient {
    override val platform: Platform = Platform.X

    override suspend fun syncProfile(sourceId: String): SourceSyncPage {
        val username = sourceId.trimUsername()
        if (BuildConfig.DEBUG) Log.d(TAG, "Syncing X profile @$username")

        val payload = scraper.scrapeProfile(username, scrollCount = INITIAL_SCROLLS)
        val parsed = parser.parse(username, payload)
        val hasMorePosts = parsed.posts.isNotEmpty()

        return SourceSyncPage(
            account = parsed.account,
            userId = parsed.account.accountId,
            posts = parsed.posts,
            nextPageToken = if (hasMorePosts) "scroll:$INITIAL_SCROLLS" else null,
            hasMorePosts = hasMorePosts,
        )
    }

    override suspend fun loadOlderPosts(profile: SocialProfile): SourceSyncPage {
        val currentScrollCount = profile.nextPageToken
            ?.removePrefix("scroll:")
            ?.toIntOrNull()
            ?: INITIAL_SCROLLS
        val nextScrollCount = minOf(currentScrollCount + OLDER_SCROLL_STEP, MAX_SCROLLS)
        if (BuildConfig.DEBUG) Log.d(TAG, "Loading older X posts for @${profile.username}; scrolls=$nextScrollCount")

        val payload = scraper.scrapeProfile(profile.username, scrollCount = nextScrollCount)
        val parsed = parser.parse(profile.username, payload)
        val hasMorePosts = parsed.posts.isNotEmpty() && nextScrollCount < MAX_SCROLLS

        return SourceSyncPage(
            account = parsed.account,
            userId = parsed.account.accountId,
            posts = parsed.posts,
            nextPageToken = if (hasMorePosts) "scroll:$nextScrollCount" else null,
            hasMorePosts = hasMorePosts,
        )
    }

    private fun String.trimUsername(): String = trim().removePrefix("@").trim('/').lowercase()

    private companion object {
        const val TAG = "XFeedSource"
        const val INITIAL_SCROLLS = 5
        const val OLDER_SCROLL_STEP = 4
        const val MAX_SCROLLS = 24
    }
}
