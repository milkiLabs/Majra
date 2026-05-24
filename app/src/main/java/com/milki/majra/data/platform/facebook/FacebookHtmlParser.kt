package com.milki.majra.data.platform.facebook

import android.util.Log
import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.PostMediaItem
import com.milki.majra.data.model.SocialPost
import com.milki.majra.data.model.SocialProfile
import org.json.JSONObject

/**
 * Parser for Facebook profile HTML pages.
 * 
 * Facebook embeds initial data as JSON in <script> tags in the HTML.
 * We extract this JSON data instead of parsing the rendered DOM.
 */
class FacebookHtmlParser {

    data class ParsedProfile(
        val account: SocialProfile,
        val posts: List<SocialPost>,
        val nextCursor: String?,
        val hasMorePosts: Boolean,
    )

    fun parseProfilePage(username: String, html: String): ParsedProfile {
        Log.d(TAG, "Parsing Facebook profile HTML for: $username, length: ${html.length}")
        
        try {
            // Extract profile name from title
            val titleMatch = Regex("<title>([^<]+)</title>").find(html)
            val displayName = titleMatch?.groupValues?.get(1)
                ?.replace(" - Facebook", "")
                ?.replace(" | Facebook", "")
                ?.trim() ?: username
            
            Log.d(TAG, "Display name: $displayName")
            
            // Find all story_fbid references in the HTML
            val storyMatches = Regex("story_fbid=(\\d+)").findAll(html).toList()
            Log.d(TAG, "Found ${storyMatches.size} story_fbid references")
            
            // Also check for other post patterns
            val postsMatches = Regex("/posts/(\\w+)").findAll(html).toList()
            Log.d(TAG, "Found ${postsMatches.size} /posts/ references")
            
            // Check if this looks like a splash screen
            if (html.contains("splash") || html.contains("loading") && html.length < 50000) {
                Log.w(TAG, "HTML might be a splash screen or loading page")
            }
            
            val posts = mutableListOf<SocialPost>()
            val seenIds = mutableSetOf<String>()
            
            for (match in storyMatches) {
                val storyId = match.groupValues[1]
                val postId = "story_$storyId"
                
                if (seenIds.contains(postId)) continue
                seenIds.add(postId)
                
                // Try to find text content near this story_fbid
                val matchStart = match.range.first
                val contextStart = maxOf(0, matchStart - 2000)
                val contextEnd = minOf(html.length, matchStart + 2000)
                val context = html.substring(contextStart, contextEnd)
                
                // Look for text content in the context
                var text = ""
                val textPatterns = listOf(
                    Regex(""""text":"([^"]{30,500})""""),
                    Regex(""""message":\{"text":"([^"]{30,500})""""),
                    Regex(""""body":"([^"]{30,500})""""),
                )
                
                for (pattern in textPatterns) {
                    val textMatch = pattern.find(context)
                    if (textMatch != null) {
                        text = textMatch.groupValues[1]
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                        break
                    }
                }
                
                posts.add(
                    SocialPost(
                        platform = Platform.FACEBOOK,
                        id = postId,
                        platformPostId = postId,
                        accountId = username,
                        username = username,
                        mediaType = SocialPost.MEDIA_TYPE_IMAGE,
                        caption = text,
                        timestampSeconds = System.currentTimeMillis() / 1000,
                        permalink = "https://www.facebook.com/story.php?story_fbid=$storyId",
                        mediaItems = emptyList(),
                    )
                )
            }
            
            Log.d(TAG, "Extracted ${posts.size} posts")
            
            val account = SocialProfile(
                platform = Platform.FACEBOOK,
                username = username,
                accountId = username,
                displayName = displayName,
                profilePicUrl = null,
            )
            
            return ParsedProfile(
                account = account,
                posts = posts,
                nextCursor = null,
                hasMorePosts = false,
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing profile HTML: ${e.message}", e)
            throw FacebookParsingException("Failed to parse profile HTML: ${e.message}", e)
        }
    }
    
    companion object {
        private const val TAG = "FacebookHtmlParser"
    }
}

class FacebookParsingException(message: String, cause: Throwable? = null) : Exception(message, cause)
