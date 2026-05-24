package com.milki.majra.data.platform.facebook

import android.util.Log
import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.SocialProfile
import com.milki.majra.data.repository.FeedSourceClient
import com.milki.majra.data.repository.SourceSyncPage

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
        
        Log.d(TAG, "Starting WebView GraphQL sync for Facebook profile: $username")
        
        try {
            // Scrape profile using WebView with GraphQL interception (initial load with 5 scrolls)
            val jsonResult = scraper.scrapeProfile(username, scrollCount = 5)
            
            Log.d(TAG, "Received GraphQL data, length: ${jsonResult.length}")
            
            // Parse the captured GraphQL data
            val parsed = parser.parseGraphQLData(username, jsonResult)
            
            Log.d(TAG, "Parsed result: ${parsed.posts.size} posts")
            
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
        
        Log.d(TAG, "Loading older posts for Facebook profile: $username")
        
        try {
            // Parse the pagination token to get scroll count
            val currentScrollCount = profile.nextPageToken?.removePrefix("scroll:")?.toIntOrNull() ?: 5
            val newScrollCount = currentScrollCount + 5 // Scroll 5 more times
            
            Log.d(TAG, "Scrolling $newScrollCount times to load older posts")
            
            // Scrape with more scrolls to get older posts
            val jsonResult = scraper.scrapeProfile(username, scrollCount = newScrollCount)
            
            Log.d(TAG, "Received GraphQL data, length: ${jsonResult.length}")
            
            // Parse the captured GraphQL data
            val parsed = parser.parseGraphQLData(username, jsonResult)
            
            Log.d(TAG, "Parsed result: ${parsed.posts.size} posts total")
            
            // Always allow loading more posts (user can keep scrolling)
            // We'll stop when no new posts are found
            val hasMorePosts = parsed.posts.isNotEmpty()
            
            return SourceSyncPage(
                account = parsed.account,
                userId = parsed.account.accountId,
                posts = parsed.posts, // Return all posts, the repository will deduplicate
                nextPageToken = if (hasMorePosts) "scroll:$newScrollCount" else null,
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
