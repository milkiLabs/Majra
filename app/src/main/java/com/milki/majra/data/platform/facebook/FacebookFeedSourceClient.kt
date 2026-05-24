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
            // Scrape profile using WebView with GraphQL interception
            val jsonResult = scraper.scrapeProfile(username)
            
            Log.d(TAG, "Received GraphQL data, length: ${jsonResult.length}")
            
            // Parse the captured GraphQL data
            val parsed = parser.parseGraphQLData(username, jsonResult)
            
            Log.d(TAG, "Parsed result: ${parsed.posts.size} posts")
            
            return SourceSyncPage(
                account = parsed.account,
                userId = parsed.account.accountId,
                posts = parsed.posts,
                nextPageToken = null,
                hasMorePosts = false,
            )
        } catch (e: Exception) {
            Log.e(TAG, "WebView GraphQL sync failed: ${e.message}", e)
            throw e
        }
    }

    override suspend fun loadOlderPosts(profile: SocialProfile): SourceSyncPage {
        // For now, no pagination support
        Log.d(TAG, "Pagination not yet supported for Facebook")
        return SourceSyncPage(
            account = profile,
            userId = profile.accountId,
            posts = emptyList(),
            nextPageToken = null,
            hasMorePosts = false,
        )
    }

    private fun String.trimUsername(): String = trim().removePrefix("@").trim('/').lowercase()
    
    companion object {
        private const val TAG = "FacebookFeedSource"
    }
}
