package com.milki.majra.data.platform.facebook

import android.util.Log
import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.PostMediaItem
import com.milki.majra.data.model.SocialPost
import com.milki.majra.data.model.SocialProfile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parser for Facebook GraphQL API responses
 */
class FacebookGraphQLParser {

    data class ParsedProfile(
        val account: SocialProfile,
        val posts: List<SocialPost>,
        val nextCursor: String?,
        val hasMorePosts: Boolean,
    )

    fun parseTimelineResponse(username: String, jsonResponse: String): ParsedProfile {
        Log.d(TAG, "Parsing GraphQL response for: $username, length: ${jsonResponse.length}")
        
        try {
            val json = JSONObject(jsonResponse)
            
            // Check for errors
            if (json.has("errors")) {
                val errors = json.getJSONArray("errors")
                if (errors.length() > 0) {
                    val errorMsg = errors.getJSONObject(0).optString("message", "Unknown GraphQL error")
                    Log.e(TAG, "GraphQL error: $errorMsg")
                    throw FacebookGraphQLException("GraphQL error: $errorMsg")
                }
            }
            
            // Navigate to the data
            val data = json.optJSONObject("data")
            if (data == null) {
                Log.e(TAG, "No data field in GraphQL response")
                return ParsedProfile(
                    account = SocialProfile(
                        platform = Platform.FACEBOOK,
                        username = username,
                        accountId = username,
                        displayName = username,
                        profilePicUrl = null,
                    ),
                    posts = emptyList(),
                    nextCursor = null,
                    hasMorePosts = false,
                )
            }
            
            // Try to find user/profile data
            val user = data.optJSONObject("user") ?: data.optJSONObject("node")
            
            if (user == null) {
                Log.w(TAG, "No user/node found in data. Available keys: ${data.keys().asSequence().toList()}")
            }
            
            // Extract profile info
            val displayName = user?.optString("name") ?: username
            val userId = user?.optString("id") ?: username
            val profilePicUrl = user?.optJSONObject("profile_picture")
                ?.optJSONObject("uri")
                ?.optString("uri")
            
            Log.d(TAG, "Extracted profile - name: $displayName, id: $userId, pic: ${profilePicUrl != null}")
            
            val account = SocialProfile(
                platform = Platform.FACEBOOK,
                username = username,
                accountId = userId,
                displayName = displayName,
                profilePicUrl = profilePicUrl,
            )
            
            // Extract posts from timeline
            val posts = mutableListOf<SocialPost>()
            var nextCursor: String? = null
            var hasMore = false
            
            // Try different paths to find timeline/feed data
            val timeline = user?.optJSONObject("timeline_list_feed_units")
                ?: user?.optJSONObject("timeline_feed_units")
                ?: user?.optJSONObject("profile_timeline_feed")
            
            if (timeline != null) {
                val edges = timeline.optJSONArray("edges")
                if (edges != null) {
                    for (i in 0 until edges.length()) {
                        val edge = edges.getJSONObject(i)
                        val node = edge.optJSONObject("node")
                        if (node != null) {
                            val post = parsePost(node, userId, username)
                            if (post != null) {
                                posts.add(post)
                            }
                        }
                    }
                }
                
                // Extract pagination info
                val pageInfo = timeline.optJSONObject("page_info")
                if (pageInfo != null) {
                    hasMore = pageInfo.optBoolean("has_next_page", false)
                    nextCursor = pageInfo.optString("end_cursor").takeIf { it.isNotBlank() }
                }
            }
            
            Log.d(TAG, "Extracted ${posts.size} posts, hasMore: $hasMore, cursor: $nextCursor")
            
            return ParsedProfile(
                account = account,
                posts = posts,
                nextCursor = nextCursor,
                hasMorePosts = hasMore,
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GraphQL response: ${e.message}", e)
            throw FacebookGraphQLException("Failed to parse GraphQL response: ${e.message}", e)
        }
    }
    
    private fun parsePost(node: JSONObject, userId: String, username: String): SocialPost? {
        try {
            // Extract post ID
            val postId = node.optString("id") ?: node.optString("post_id")
            if (postId.isBlank()) {
                Log.d(TAG, "Post has no ID, skipping")
                return null
            }
            
            // Extract text/caption
            val message = node.optJSONObject("message")?.optString("text")
                ?: node.optString("text")
                ?: ""
            
            // Extract timestamp
            val timestamp = node.optLong("created_time", 0)
            val timestampSeconds = if (timestamp > 0) timestamp else System.currentTimeMillis() / 1000
            
            // Extract permalink
            val permalink = node.optString("url")
                ?: "https://www.facebook.com/$username/posts/$postId"
            
            // Extract media
            val mediaItems = mutableListOf<PostMediaItem>()
            val attachments = node.optJSONArray("attachments")
                ?: node.optJSONObject("attachments")?.optJSONArray("data")
            
            if (attachments != null) {
                for (i in 0 until attachments.length()) {
                    val attachment = attachments.getJSONObject(i)
                    val media = attachment.optJSONObject("media")
                    
                    if (media != null) {
                        val mediaType = media.optString("__typename")
                        val imageUrl = media.optJSONObject("image")?.optString("uri") ?: ""
                        val videoUrl = media.optJSONObject("playable_url")?.optString("uri")
                        
                        when {
                            videoUrl != null -> {
                                mediaItems.add(
                                    PostMediaItem(
                                        imageUrl = imageUrl,
                                        videoUrl = videoUrl,
                                        mediaType = PostMediaItem.MEDIA_TYPE_VIDEO,
                                    )
                                )
                            }
                            imageUrl.isNotBlank() -> {
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
                }
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
                caption = message,
                timestampSeconds = timestampSeconds,
                permalink = permalink,
                mediaItems = mediaItems,
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing post: ${e.message}", e)
            return null
        }
    }
    
    companion object {
        private const val TAG = "FacebookGraphQLParser"
    }
}

class FacebookGraphQLException(message: String, cause: Throwable? = null) : Exception(message, cause)
