package com.milki.majra.data.platform.facebook

import android.util.Log
import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.SocialProfile
import com.milki.majra.data.repository.FeedSourceClient
import com.milki.majra.data.repository.SourceSyncPage

/**
 * Facebook feed source client using GraphQL API.
 * Much faster and more reliable than WebView scraping.
 */
class FacebookFeedSourceClient(
    private val httpClient: FacebookHttpClient,
    private val graphQLParser: FacebookGraphQLParser,
) : FeedSourceClient {
    override val platform: Platform = Platform.FACEBOOK

    override suspend fun syncProfile(sourceId: String): SourceSyncPage {
        val username = sourceId.trimUsername()
        
        Log.d(TAG, "Starting GraphQL sync for Facebook profile: $username")
        
        try {
            // Fetch timeline using GraphQL
            val jsonResponse = httpClient.fetchTimelineGraphQL(username)
            
            Log.d(TAG, "Received GraphQL response, length: ${jsonResponse.length}")
            
            // Parse the GraphQL response
            val parsed = graphQLParser.parseTimelineResponse(username, jsonResponse)
            
            Log.d(TAG, "Parsed result: ${parsed.posts.size} posts, hasMore: ${parsed.hasMorePosts}, cursor: ${parsed.nextCursor}")
            
            return SourceSyncPage(
                account = parsed.account,
                userId = parsed.account.accountId,
                posts = parsed.posts,
                nextPageToken = parsed.nextCursor,
                hasMorePosts = parsed.hasMorePosts,
            )
        } catch (e: Exception) {
            Log.e(TAG, "GraphQL sync failed: ${e.message}", e)
            throw e
        }
    }

    override suspend fun loadOlderPosts(profile: SocialProfile): SourceSyncPage {
        val cursor = profile.nextPageToken 
            ?: error("No older posts are available for @${profile.username}.")
        
        Log.d(TAG, "Loading older posts for ${profile.username} with cursor: $cursor")
        
        try {
            // Fetch next page using cursor
            val jsonResponse = httpClient.fetchTimelineGraphQL(profile.username, cursor)
            
            // Parse the GraphQL response
            val parsed = graphQLParser.parseTimelineResponse(profile.username, jsonResponse)
            
            Log.d(TAG, "Loaded ${parsed.posts.size} older posts")
            
            return SourceSyncPage(
                account = profile,
                userId = profile.accountId,
                posts = parsed.posts,
                nextPageToken = parsed.nextCursor,
                hasMorePosts = parsed.hasMorePosts,
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
