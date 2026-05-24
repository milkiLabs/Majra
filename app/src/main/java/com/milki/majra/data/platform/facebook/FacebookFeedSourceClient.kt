package com.milki.majra.data.platform.facebook

import android.util.Log
import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.PostMediaItem
import com.milki.majra.data.model.SocialPost
import com.milki.majra.data.model.SocialProfile
import com.milki.majra.data.repository.FeedSourceClient
import com.milki.majra.data.repository.SourceSyncPage
import org.json.JSONObject

/**
 * Facebook feed source client using WebView scraping.
 */
class FacebookFeedSourceClient(
    private val scraper: FacebookWebViewScraper,
) : FeedSourceClient {
    override val platform: Platform = Platform.FACEBOOK

    override suspend fun syncProfile(sourceId: String): SourceSyncPage {
        val username = sourceId.trimUsername()
        
        Log.d(TAG, "Starting mobile WebView sync for Facebook profile: $username")
        
        try {
            // Scrape profile using mobile WebView
            val jsonString = scraper.scrapeProfile(username)
            
            Log.d(TAG, "Received scraped data, length: ${jsonString.length}")
            
            // Parse the JSON response
            val json = JSONObject(jsonString)
            
            if (json.has("error")) {
                val error = json.getString("error")
                Log.e(TAG, "Scraping error: $error")
                throw FacebookScrapingException("Failed to scrape profile: $error")
            }
            
            val displayName = json.optString("displayName", username)
            val profilePicUrl = json.optString("profilePicUrl").takeIf { it.isNotBlank() }
            val userId = json.optString("userId", username)
            
            val account = SocialProfile(
                platform = Platform.FACEBOOK,
                username = username,
                accountId = userId,
                displayName = displayName,
                profilePicUrl = profilePicUrl,
            )
            
            val postsArray = json.optJSONArray("posts")
            val posts = mutableListOf<SocialPost>()
            
            if (postsArray != null) {
                for (i in 0 until postsArray.length()) {
                    val postJson = postsArray.getJSONObject(i)
                    val post = parsePost(postJson, userId, username)
                    if (post != null) {
                        posts.add(post)
                    }
                }
            }
            
            Log.d(TAG, "Parsed ${posts.size} posts from WebView")
            
            return SourceSyncPage(
                account = account,
                userId = userId,
                posts = posts,
                nextPageToken = null,
                hasMorePosts = false,
            )
        } catch (e: Exception) {
            Log.e(TAG, "WebView sync failed: ${e.message}", e)
            throw e
        }
    }

    override suspend fun loadOlderPosts(profile: SocialProfile): SourceSyncPage {
        // WebView scraper doesn't support pagination
        Log.d(TAG, "WebView scraper doesn't support pagination")
        return SourceSyncPage(
            account = profile,
            userId = profile.accountId,
            posts = emptyList(),
            nextPageToken = null,
            hasMorePosts = false,
        )
    }
    
    private fun parsePost(json: JSONObject, userId: String, username: String): SocialPost? {
        try {
            val postId = json.optString("id")
            if (postId.isBlank()) return null
            
            val text = json.optString("text", "")
            val timestamp = json.optLong("timestamp", System.currentTimeMillis() / 1000)
            val permalink = json.optString("permalink", "https://www.facebook.com/$username/posts/$postId")
            
            val mediaItems = mutableListOf<PostMediaItem>()
            
            // Extract images
            val imagesArray = json.optJSONArray("images")
            if (imagesArray != null) {
                for (i in 0 until imagesArray.length()) {
                    val imageUrl = imagesArray.optString(i)
                    if (imageUrl.isNotBlank()) {
                        mediaItems.add(
                            PostMediaItem(
                                imageUrl = imageUrl,
                                videoUrl = null,
                                mediaType = PostMediaItem.MEDIA_TYPE_IMAGE,
                            )
                        )
                    }
                }
            }
            
            // Extract video
            val videoUrl = json.optString("video").takeIf { it.isNotBlank() }
            if (videoUrl != null) {
                mediaItems.add(
                    PostMediaItem(
                        imageUrl = "",
                        videoUrl = videoUrl,
                        mediaType = PostMediaItem.MEDIA_TYPE_VIDEO,
                    )
                )
            }
            
            val postMediaType = when {
                mediaItems.any { it.mediaType == PostMediaItem.MEDIA_TYPE_VIDEO } -> SocialPost.MEDIA_TYPE_VIDEO
                mediaItems.size > 1 -> SocialPost.MEDIA_TYPE_CAROUSEL
                mediaItems.size == 1 -> SocialPost.MEDIA_TYPE_IMAGE
                else -> SocialPost.MEDIA_TYPE_IMAGE
            }
            
            return SocialPost(
                platform = Platform.FACEBOOK,
                id = postId,
                platformPostId = postId,
                accountId = userId,
                username = username,
                mediaType = postMediaType,
                caption = text,
                timestampSeconds = timestamp,
                permalink = permalink,
                mediaItems = mediaItems,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing post: ${e.message}", e)
            return null
        }
    }

    private fun String.trimUsername(): String = trim().removePrefix("@").trim('/').lowercase()
    
    companion object {
        private const val TAG = "FacebookFeedSource"
    }
}

class FacebookScrapingException(message: String, cause: Throwable? = null) : Exception(message, cause)
