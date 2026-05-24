package com.milki.majra.data.platform.facebook

import android.util.Log
import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.SocialProfile
import com.milki.majra.data.repository.FeedSourceClient
import com.milki.majra.data.repository.SourceSyncPage
import com.milki.majra.BuildConfig

/**
 * Facebook feed source client using WebView with GraphQL interception.
 * 
 * This approach loads Facebook in a WebView and intercepts the GraphQL
 * responses that Facebook makes internally, giving us structured JSON data.
 */
class FacebookFeedSourceClient(
    private val scraper: FacebookWebViewScraper,
    private val parser: FacebookGraphQLParser,
) : FeedSourceClient {
    override val platform: Platform = Platform.FACEBOOK

    override suspend fun syncProfile(sourceId: String): SourceSyncPage {
        val username = sourceId.trimUsername()
        
        if (BuildConfig.DEBUG) Log.d(TAG, "Starting WebView GraphQL sync for Facebook profile: $username")
        
        try {
            // Scrape profile using WebView with GraphQL interception (initial load with 5 scrolls)
            val jsonResult = scraper.scrapeProfile(username, scrollCount = 5)
            
            if (BuildConfig.DEBUG) Log.d(TAG, "Received GraphQL data, length: ${jsonResult.length}")
            
            // Parse the captured GraphQL data
            val parsed = parser.parseGraphQLData(username, jsonResult)
            
            if (BuildConfig.DEBUG) Log.d(TAG, "Parsed result: ${parsed.posts.size} posts")
            
            // Facebook pagination: we can always load more by scrolling more
            // Set hasMorePosts to true if we got any posts
            val hasMorePosts = parsed.posts.isNotEmpty()
            
            return SourceSyncPage(
                account = parsed.account,
                userId = parsed.account.accountId,
                posts = parsed.posts,
                nextPageToken = if (hasMorePosts) "scroll:5" else null,
                hasMorePosts = hasMorePosts,
            )
        } catch (e: Exception) {
            Log.e(TAG, "WebView GraphQL sync failed: ${e.message}", e)
            throw e
        }
    }

    override suspend fun loadOlderPosts(profile: SocialProfile): SourceSyncPage {
        val username = profile.username
        
        if (BuildConfig.DEBUG) Log.d(TAG, "Loading older posts for Facebook profile: $username")
        
        try {
            // Parse the pagination token to get scroll count
            val currentScrollCount = profile.nextPageToken?.removePrefix("scroll:")?.toIntOrNull() ?: 5
            // Only add 3 more scrolls each time to avoid timeout
            val newScrollCount = currentScrollCount + 3
            
            // Cap at 20 scrolls to avoid excessive loading time
            val cappedScrollCount = minOf(newScrollCount, 20)
            
            if (BuildConfig.DEBUG) Log.d(TAG, "Scrolling $cappedScrollCount times to load older posts (was $currentScrollCount)")
            
            // Scrape with more scrolls to get older posts
            val jsonResult = scraper.scrapeProfile(username, scrollCount = cappedScrollCount)
            
            if (BuildConfig.DEBUG) Log.d(TAG, "Received GraphQL data, length: ${jsonResult.length}")
            
            // Parse the captured GraphQL data
            val parsed = parser.parseGraphQLData(username, jsonResult)
            
            if (BuildConfig.DEBUG) Log.d(TAG, "Parsed result: ${parsed.posts.size} posts total")
            
            // Allow loading more posts unless we've hit the cap
            val hasMorePosts = parsed.posts.isNotEmpty() && cappedScrollCount < 20
            
            return SourceSyncPage(
                account = parsed.account,
                userId = parsed.account.accountId,
                posts = parsed.posts, // Return all posts, the repository will deduplicate
                nextPageToken = if (hasMorePosts) "scroll:$cappedScrollCount" else null,
                hasMorePosts = hasMorePosts,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Loading older posts failed: ${e.message}", e)
            throw e
        }
    }

    private fun String.trimUsername(): String = trim().removePrefix("@").trim('/').lowercase()
    
    companion object {
        private const val TAG = "FacebookFeedSource"
    }
}
