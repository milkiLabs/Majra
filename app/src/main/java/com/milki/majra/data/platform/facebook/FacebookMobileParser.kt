package com.milki.majra.data.platform.facebook

import android.util.Log
import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.PostMediaItem
import com.milki.majra.data.model.SocialPost
import com.milki.majra.data.model.SocialProfile
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Parser for Facebook's mobile basic site (mbasic.facebook.com)
 * This site has simpler HTML structure that's easier and more reliable to parse
 */
class FacebookMobileParser {

    data class ParsedProfile(
        val account: SocialProfile,
        val posts: List<SocialPost>,
        val nextCursor: String?,
        val hasMorePosts: Boolean,
    )

    fun parseProfile(username: String, html: String): ParsedProfile {
        Log.d(TAG, "Parsing profile for: $username, HTML length: ${html.length}")
        
        val doc = Jsoup.parse(html)
        
        // Log some diagnostic info
        Log.d(TAG, "Document title: ${doc.title()}")
        Log.d(TAG, "Body text length: ${doc.body().text().length}")
        
        // Extract profile information
        val displayName = extractDisplayName(doc, username)
        val profilePicUrl = extractProfilePicUrl(doc)
        val userId = extractUserId(doc, username)

        Log.d(TAG, "Extracted profile - name: $displayName, userId: $userId, pic: ${profilePicUrl != null}")

        val account = SocialProfile(
            platform = Platform.FACEBOOK,
            username = username,
            accountId = userId,
            displayName = displayName,
            profilePicUrl = profilePicUrl,
        )

        // Extract posts
        val posts = extractPosts(doc, userId, username)
        
        Log.d(TAG, "Extracted ${posts.size} posts")

        // Extract pagination cursor
        val nextCursor = extractNextCursor(doc)
        
        Log.d(TAG, "Next cursor: $nextCursor")

        return ParsedProfile(
            account = account,
            posts = posts,
            nextCursor = nextCursor,
            hasMorePosts = nextCursor != null && posts.isNotEmpty(),
        )
    }

    private fun extractDisplayName(doc: Document, fallback: String): String {
        // Try title first
        val title = doc.title()
            .replace(Regex("\\s*[-|–]\\s*Facebook.*", RegexOption.IGNORE_CASE), "")
            .trim()
        if (title.isNotBlank() && !title.equals("Facebook", ignoreCase = true)) {
            return title
        }

        // Try header
        doc.select("div#objects_container").firstOrNull()?.let { container ->
            container.select("h3, h2, h1").firstOrNull()?.text()?.trim()?.let {
                if (it.isNotBlank()) return it
            }
        }

        // Try any strong tag near the top
        doc.select("strong").firstOrNull()?.text()?.trim()?.let {
            if (it.isNotBlank() && it.length < 100) return it
        }

        return fallback
    }

    private fun extractProfilePicUrl(doc: Document): String? {
        // Look for profile picture
        doc.select("img[alt*='profile' i], img[alt*='picture' i]").firstOrNull()?.let {
            val src = it.absUrl("src")
            if (src.contains("scontent") || src.contains("fbcdn")) {
                return src
            }
        }

        // Look for any large image near the top
        doc.select("img").take(5).forEach { img ->
            val src = img.absUrl("src")
            if (src.contains("scontent") && !src.contains("emoji")) {
                return src
            }
        }

        return null
    }

    private fun extractUserId(doc: Document, username: String): String {
        // Try to extract user ID from various places
        
        // From profile link
        doc.select("a[href*='profile.php?id=']").firstOrNull()?.let { link ->
            val href = link.attr("href")
            Regex("profile\\.php\\?id=(\\d+)").find(href)?.groupValues?.get(1)?.let {
                return it
            }
        }

        // From data attributes
        doc.select("[data-sigil='profile-card']").firstOrNull()?.let { elem ->
            elem.attr("data-id").takeIf { it.isNotBlank() }?.let { return it }
        }

        // Fallback to username
        return username
    }

    private fun extractPosts(doc: Document, userId: String, username: String): List<SocialPost> {
        val posts = mutableListOf<SocialPost>()

        // Try multiple selectors for posts
        val selectors = listOf(
            "div[data-ft]",
            "article",
            "div.story_body_container",
            "div[role='article']",
            "div#m_story_permalink_view",
            "div.timeline"
        )
        
        var postElements: List<Element> = emptyList()
        for (selector in selectors) {
            postElements = doc.select(selector).toList()
            if (postElements.isNotEmpty()) {
                Log.d(TAG, "Found ${postElements.size} elements with selector: $selector")
                break
            }
        }
        
        if (postElements.isEmpty()) {
            Log.w(TAG, "No post elements found with any selector")
            // Log some of the HTML structure for debugging
            Log.d(TAG, "Available div classes: ${doc.select("div[class]").take(10).map { it.className() }}")
            Log.d(TAG, "Available article tags: ${doc.select("article").size}")
            return emptyList()
        }

        for ((index, postElem) in postElements.withIndex()) {
            try {
                val post = extractPost(postElem, userId, username)
                if (post != null) {
                    posts.add(post)
                    Log.d(TAG, "Successfully extracted post $index: ${post.platformPostId}")
                } else {
                    Log.d(TAG, "Post $index returned null (likely not a valid post)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting post $index: ${e.message}", e)
                continue
            }
        }

        Log.d(TAG, "Total posts extracted: ${posts.size}")
        return posts.distinctBy { it.platformPostId }
    }

    private fun extractPost(elem: Element, userId: String, username: String): SocialPost? {
        // Extract post ID and permalink
        val (postId, permalink) = extractPostIdAndLink(elem) ?: return null

        // Extract text content
        val text = extractPostText(elem)

        // Extract timestamp
        val timestamp = extractTimestamp(elem)

        // Extract media
        val images = extractImages(elem)
        val videoUrl = extractVideoUrl(elem)

        // Build media items
        val mediaItems = mutableListOf<PostMediaItem>()
        if (videoUrl != null) {
            mediaItems.add(
                PostMediaItem(
                    imageUrl = images.firstOrNull() ?: "",
                    videoUrl = videoUrl,
                    mediaType = PostMediaItem.MEDIA_TYPE_VIDEO,
                )
            )
        } else {
            images.forEach { url ->
                mediaItems.add(
                    PostMediaItem(
                        imageUrl = url,
                        videoUrl = null,
                        mediaType = PostMediaItem.MEDIA_TYPE_IMAGE,
                    )
                )
            }
        }

        val mediaType = when {
            videoUrl != null -> SocialPost.MEDIA_TYPE_VIDEO
            mediaItems.size > 1 -> SocialPost.MEDIA_TYPE_CAROUSEL
            else -> SocialPost.MEDIA_TYPE_IMAGE
        }

        return SocialPost(
            platform = Platform.FACEBOOK,
            id = postId,
            platformPostId = postId,
            accountId = userId,
            username = username,
            mediaType = mediaType,
            caption = text,
            timestampSeconds = timestamp,
            permalink = permalink,
            mediaItems = mediaItems,
        )
    }

    private fun extractPostIdAndLink(elem: Element): Pair<String, String>? {
        // Try different link patterns
        val linkPatterns = listOf(
            "a[href*='/story.php?']",
            "a[href*='/posts/']",
            "a[href*='/permalink/']",
            "a[href*='fbid=']",
        )

        for (pattern in linkPatterns) {
            elem.select(pattern).firstOrNull()?.let { link ->
                val href = link.absUrl("href")
                
                // Extract post ID from URL
                val postId = when {
                    href.contains("story_fbid=") -> {
                        Regex("story_fbid=(\\d+)").find(href)?.groupValues?.get(1)?.let { "sfb_$it" }
                    }
                    href.contains("/posts/") -> {
                        Regex("/posts/([^/?]+)").find(href)?.groupValues?.get(1)
                    }
                    href.contains("/permalink/") -> {
                        Regex("/permalink/([^/?]+)").find(href)?.groupValues?.get(1)
                    }
                    href.contains("fbid=") -> {
                        Regex("fbid=(\\d+)").find(href)?.groupValues?.get(1)?.let { "fbid_$it" }
                    }
                    else -> null
                }

                if (postId != null) {
                    val cleanPermalink = href.split("?").first()
                    return Pair(postId, cleanPermalink)
                }
            }
        }

        return null
    }

    private fun extractPostText(elem: Element): String {
        // Try different text containers
        val textSelectors = listOf(
            "div[data-ft] > div > div",
            "div.story_body_container",
            "p",
            "div > span",
        )

        for (selector in textSelectors) {
            elem.select(selector).firstOrNull()?.let { textElem ->
                val text = textElem.text().trim()
                if (text.isNotBlank() && text.length > 10) {
                    return text
                }
            }
        }

        return ""
    }

    private fun extractTimestamp(elem: Element): Long {
        // Try abbr with data-utime
        elem.select("abbr[data-utime]").firstOrNull()?.let { abbr ->
            abbr.attr("data-utime").toLongOrNull()?.let {
                return it
            }
        }

        // Try to parse relative time text
        elem.select("abbr").firstOrNull()?.text()?.let { timeText ->
            parseRelativeTime(timeText)?.let { return it }
        }

        // Fallback to current time
        return System.currentTimeMillis() / 1000
    }

    private fun parseRelativeTime(text: String): Long? {
        val now = System.currentTimeMillis() / 1000
        val lowerText = text.lowercase()

        return when {
            lowerText.contains("just now") || lowerText.contains("now") -> now
            lowerText.contains("min") -> {
                val mins = Regex("(\\d+)").find(lowerText)?.groupValues?.get(1)?.toLongOrNull() ?: 1
                now - (mins * 60)
            }
            lowerText.contains("hr") || lowerText.contains("hour") -> {
                val hours = Regex("(\\d+)").find(lowerText)?.groupValues?.get(1)?.toLongOrNull() ?: 1
                now - (hours * 3600)
            }
            lowerText.contains("day") -> {
                val days = Regex("(\\d+)").find(lowerText)?.groupValues?.get(1)?.toLongOrNull() ?: 1
                now - (days * 86400)
            }
            lowerText.contains("week") -> {
                val weeks = Regex("(\\d+)").find(lowerText)?.groupValues?.get(1)?.toLongOrNull() ?: 1
                now - (weeks * 604800)
            }
            else -> null
        }
    }

    private fun extractImages(elem: Element): List<String> {
        val images = mutableListOf<String>()

        elem.select("img").forEach { img ->
            val src = img.absUrl("src")
            if (src.isNotBlank() && 
                (src.contains("scontent") || src.contains("fbcdn")) &&
                !src.contains("emoji") &&
                !src.contains("static") &&
                !src.contains("rsrc")) {
                images.add(src)
            }
        }

        return images.distinct()
    }

    private fun extractVideoUrl(elem: Element): String? {
        // Look for video elements
        elem.select("video").firstOrNull()?.let { video ->
            video.absUrl("src").takeIf { it.isNotBlank() }?.let { return it }
        }

        // Look for video links
        elem.select("a[href*='/video'], a[href*='/videos/']").firstOrNull()?.let { link ->
            return link.absUrl("href")
        }

        return null
    }

    private fun extractNextCursor(doc: Document): String? {
        // Look for "See more posts" or pagination link
        val paginationSelectors = listOf(
            "a[href*='cursor=']",
            "a[href*='timeend=']",
            "a[href*='aftercursor=']",
            "a:contains(See more)",
            "a:contains(Show more)",
            "div#see_more_pager a",
            "div#m_more_item a"
        )
        
        for (selector in paginationSelectors) {
            doc.select(selector).lastOrNull()?.let { link ->
                val href = link.attr("href")
                Log.d(TAG, "Found pagination link with selector '$selector': $href")
                
                Regex("cursor=([^&]+)").find(href)?.groupValues?.get(1)?.let {
                    Log.d(TAG, "Extracted cursor: $it")
                    return it
                }
                
                Regex("timeend=([^&]+)").find(href)?.groupValues?.get(1)?.let {
                    Log.d(TAG, "Extracted timeend cursor: $it")
                    return it
                }
                
                Regex("aftercursor=([^&]+)").find(href)?.groupValues?.get(1)?.let {
                    Log.d(TAG, "Extracted aftercursor: $it")
                    return it
                }
            }
        }

        Log.d(TAG, "No pagination cursor found")
        return null
    }
    
    companion object {
        private const val TAG = "FacebookMobileParser"
    }
}
