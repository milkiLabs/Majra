package com.milki.majra.data.platform.facebook

import android.util.Log
import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.PostMediaItem
import com.milki.majra.data.model.SocialPost
import com.milki.majra.data.model.SocialProfile
import org.json.JSONObject
import com.milki.majra.BuildConfig

/**
 * Parser for GraphQL data captured from Facebook's WebView.
 * 
 * The WebView scraper intercepts GraphQL responses and returns them as JSON.
 * This parser extracts profile and post information from that JSON.
 */
class FacebookGraphQLParser {

    data class ParsedProfile(
        val account: SocialProfile,
        val posts: List<SocialPost>,
    )

    fun parseGraphQLData(username: String, jsonString: String): ParsedProfile {
        if (BuildConfig.DEBUG) Log.d(TAG, "Parsing GraphQL data for: $username, length: ${jsonString.length}")
        
        try {
            // Remove quotes and unescape if needed
            val cleanJson = jsonString.trim('"')
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
            
            val json = JSONObject(cleanJson)
            
            // Check for errors
            if (json.has("error")) {
                val error = json.getString("error")
                Log.e(TAG, "GraphQL extraction error: $error")
                throw FacebookGraphQLException("GraphQL extraction failed: $error")
            }
            
            // Extract profile info
            val displayName = json.optString("displayName", username)
                .takeIf { it.isNotBlank() && it.lowercase() != "facebook" }
                ?: username
            
            val profilePicUrl = json.optString("profilePicUrl")
                .takeIf { it.isNotBlank() }
            
            val account = SocialProfile(
                platform = Platform.FACEBOOK,
                username = username,
                accountId = username,
                displayName = displayName,
                profilePicUrl = profilePicUrl,
            )
            
            // Extract posts
            val postsArray = json.optJSONArray("posts")
            val posts = mutableListOf<SocialPost>()
            
            if (postsArray != null) {
                for (i in 0 until postsArray.length()) {
                    val postJson = postsArray.optJSONObject(i) ?: continue
                    
                    try {
                        val post = parsePost(postJson, username)
                        if (post != null) {
                            posts.add(post)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse post $i: ${e.message}")
                    }
                }
            }
            
            if (BuildConfig.DEBUG) Log.d(TAG, "Parsed ${posts.size} posts from GraphQL data")
            
            // Log debug info if available
            if (json.has("debug")) {
                val debug = json.getJSONObject("debug")
                if (BuildConfig.DEBUG) Log.d(TAG, "Debug info: capturedCount=${debug.optInt("capturedCount")}, extractedPosts=${debug.optInt("extractedPosts")}")
            }
            
            // Sort posts by timestamp descending (newest first) and deduplicate
            val sortedPosts = posts
                .distinctBy { it.id }
                .sortedByDescending { it.timestampSeconds }
            
            if (BuildConfig.DEBUG) Log.d(TAG, "Returning ${sortedPosts.size} posts sorted by timestamp (newest first)")
            
            return ParsedProfile(
                account = account,
                posts = sortedPosts,
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GraphQL data", e)
            throw FacebookGraphQLException("Failed to parse GraphQL data: ${e.message}", e)
        }
    }

    private fun parsePost(json: JSONObject, username: String): SocialPost? {
        // Extract post ID
        val postId = json.optString("id").takeIf { it.isNotBlank() } ?: return null
        
        // Extract text content
        val text = json.optString("text", "")
        
        // Extract media first to check if post has content
        val videosArray = json.optJSONArray("videos")
        val imagesArray = json.optJSONArray("images")
        val hasMedia = (videosArray != null && videosArray.length() > 0) || 
                       (imagesArray != null && imagesArray.length() > 0)
        
        // Only skip if BOTH text is empty AND no media exists
        // Don't skip too aggressively - let posts through even if extraction might have failed
        if (text.isBlank() && !hasMedia) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Skipping post with no content: $postId")
            return null
        }
        
        // Extract timestamp
        val timestamp = json.optLong("timestamp", System.currentTimeMillis() / 1000)
        
        // Extract permalink
        val permalink = json.optString("permalink")
            .takeIf { it.isNotBlank() && it.startsWith("http") }
            ?: "https://www.facebook.com/$username/posts/$postId"
        
        // Check if this is a shared post
        val isShared = json.optBoolean("isShared", false)
        
        // Extract media
        val mediaItems = mutableListOf<PostMediaItem>()
        
        // Extract videos first (they may have thumbnail images too)
        if (videosArray != null && videosArray.length() > 0) {
            for (i in 0 until videosArray.length()) {
                val videoUrl = videosArray.optString(i)
                if (videoUrl.isNotBlank()) {
                    // Try to find a thumbnail from images array
                    val imagesArray = json.optJSONArray("images")
                    val thumbnailUrl = if (imagesArray != null && imagesArray.length() > i) {
                        imagesArray.optString(i).takeIf { it.isNotBlank() }
                    } else {
                        null
                    }
                    
                    mediaItems.add(
                        PostMediaItem(
                            imageUrl = thumbnailUrl ?: videoUrl,
                            videoUrl = videoUrl,
                            mediaType = PostMediaItem.MEDIA_TYPE_VIDEO
                        )
                    )
                }
            }
        }
        
        // Extract images (skip those already used as video thumbnails)
        if (imagesArray != null) {
            val videosCount = videosArray?.length() ?: 0
            for (i in 0 until imagesArray.length()) {
                // Skip images that were used as video thumbnails
                if (i < videosCount) continue
                
                val imageUrl = imagesArray.optString(i)
                if (imageUrl.isNotBlank()) {
                    mediaItems.add(
                        PostMediaItem(
                            imageUrl = imageUrl,
                            videoUrl = null,
                            mediaType = PostMediaItem.MEDIA_TYPE_IMAGE
                        )
                    )
                }
            }
        }
        
        // Determine media type
        val mediaType = when {
            mediaItems.isEmpty() -> SocialPost.MEDIA_TYPE_IMAGE
            mediaItems.size > 1 -> SocialPost.MEDIA_TYPE_CAROUSEL
            mediaItems.firstOrNull()?.isVideo == true -> SocialPost.MEDIA_TYPE_VIDEO
            else -> SocialPost.MEDIA_TYPE_IMAGE
        }
        
        if (BuildConfig.DEBUG) Log.d(TAG, "Parsed post: $postId, media: ${mediaItems.size}, type: $mediaType, shared: $isShared")
        
        return SocialPost(
            platform = Platform.FACEBOOK,
            id = postId,
            platformPostId = postId,
            accountId = username,
            username = username,
            mediaType = mediaType,
            caption = text,
            timestampSeconds = timestamp,
            permalink = permalink,
            mediaItems = mediaItems,
        )
    }

    companion object {
        private const val TAG = "FacebookGraphQLParser"
    }
}

class FacebookGraphQLException(message: String, cause: Throwable? = null) : Exception(message, cause)
