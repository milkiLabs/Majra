package com.milki.majra.data.platform.instagram

import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.SocialPost
import com.milki.majra.data.model.SocialProfile
import com.milki.majra.data.model.PostMediaItem
import org.json.JSONArray
import org.json.JSONObject

class InstagramHtmlParser {
    fun parseProfile(username: String, html: String, feedJson: String? = null): ParsedInstagramProfile {
        val normalizedUsername = username.normalizeUsername()
        val profileRoots = extractJsonRoots(html)
        val feedRoots = feedJson?.let(::extractJsonRoots).orEmpty()
        val roots = profileRoots + feedRoots
        val user = roots.asSequence()
            .flatMap { root -> root.findObjects().asSequence() }
            .firstOrNull { candidate -> candidate.looksLikeUser(normalizedUsername) }
            ?: roots.asSequence()
                .flatMap { root -> root.findObjects().asSequence() }
                .firstOrNull { candidate -> candidate.looksLikeProfileHeader(normalizedUsername) }
            ?: throw InstagramParseException("Could not find profile data for @$normalizedUsername in Instagram response.")

        val account = SocialProfile(
            platform = Platform.INSTAGRAM,
            username = user.optString("username", normalizedUsername).normalizeUsername(),
            accountId = user.optNullableString("id") ?: user.optString("username", normalizedUsername).normalizeUsername(),
            displayName = user.optNullableString("full_name") ?: user.optNullableString("name"),
            profilePicUrl = user.optNullableString("profile_pic_url_hd")
                ?: user.optNullableString("profile_pic_url"),
        )
        val userId = user.optNullableString("id")

        val postNodes = user.extractPostNodes()
            .ifEmpty {
                feedRoots.flatMap { root -> root.extractPostNodes() }
            }
            .ifEmpty {
                roots.flatMap { root -> root.extractPostNodes() }
            }

        val posts = postNodes
            .mapNotNull { node -> node.toPost(account.username, account.accountId) }
            .distinctBy { it.id }
            .sortedByDescending { it.timestampSeconds }

        val feedPage = parseFeedPage(account.username, feedJson ?: html)

        return ParsedInstagramProfile(
            account = account,
            userId = userId,
            posts = posts,
            nextMaxId = feedPage.nextMaxId,
            hasMorePosts = feedPage.hasMorePosts,
        )
    }

    fun parseFeedPage(username: String, feedJson: String, accountId: String = username.normalizeUsername()): ParsedInstagramFeedPage {
        val roots = extractJsonRoots(feedJson)
        val posts = roots
            .flatMap { root -> root.extractPostNodes() }
            .mapNotNull { node -> node.toPost(username.normalizeUsername(), accountId) }
            .distinctBy { it.id }
            .sortedByDescending { it.timestampSeconds }

        val nextMaxId = roots.asSequence()
            .mapNotNull { root -> root.optNullableString("next_max_id") ?: root.optNullableString("nextMaxId") }
            .firstOrNull()
        val hasMorePosts = roots.any { root ->
            root.optBoolean("more_available", false) ||
                root.optBoolean("moreAvailable", false) ||
                !root.optNullableString("next_max_id").isNullOrBlank()
        }

        return ParsedInstagramFeedPage(
            posts = posts,
            nextMaxId = nextMaxId,
            hasMorePosts = hasMorePosts && !nextMaxId.isNullOrBlank(),
        )
    }

    fun extractUserId(html: String, username: String): String? {
        val normalizedUsername = username.normalizeUsername()
        val roots = extractJsonRoots(html)
        return (roots.asSequence()
            .flatMap { root -> root.findObjects().asSequence() }
            .firstOrNull { candidate -> candidate.looksLikeUser(normalizedUsername) }
            ?: roots.asSequence()
                .flatMap { root -> root.findObjects().asSequence() }
                .firstOrNull { candidate -> candidate.looksLikeProfileHeader(normalizedUsername) })
            ?.optNullableString("id")
    }

    private fun extractJsonRoots(html: String): List<JSONObject> {
        val roots = mutableListOf<JSONObject>()
        html.trim()
            .takeIf { it.startsWith("{") }
            ?.toJsonObjectOrNull()
            ?.let(roots::add)

        APPLICATION_JSON_SCRIPT.findAll(html).forEach { match ->
            match.groupValues.getOrNull(1)
                ?.htmlUnescape()
                ?.toJsonObjectOrNull()
                ?.let(roots::add)
        }

        SHARED_DATA_SCRIPT.findAll(html).forEach { match ->
            match.groupValues.getOrNull(1)
                ?.toJsonObjectOrNull()
                ?.let(roots::add)
        }

        return roots
    }

    private fun JSONObject.looksLikeUser(username: String): Boolean {
        val candidateUsername = optString("username").normalizeUsername()
        val hasTimelineMedia = has("edge_owner_to_timeline_media") ||
            has("edge_felix_video_timeline") ||
            has("edge_owner_to_timeline_video_media") ||
            has("media")
        return candidateUsername == username && hasTimelineMedia
    }

    private fun JSONObject.looksLikeProfileHeader(username: String): Boolean {
        val candidateUsername = optString("username").normalizeUsername()
        val hasProfileIdentity = has("id") &&
            (has("full_name") || has("profile_pic_url") || has("profile_pic_url_hd"))
        return candidateUsername == username && hasProfileIdentity
    }

    private fun JSONObject.extractPostNodes(): List<JSONObject> {
        val posts = mutableListOf<JSONObject>()

        listOf(
            "edge_owner_to_timeline_media",
            "edge_felix_video_timeline",
            "edge_owner_to_timeline_video_media",
            "media",
            "feed",
        ).forEach { key ->
            optJSONObject(key)?.extractNodesFromConnection()?.let(posts::addAll)
        }
        optJSONArray("items")?.forEachObject(posts::add)

        findObjects()
            .filter {
                (it.has("shortcode") || it.has("code")) &&
                    (it.has("display_url") ||
                        it.has("thumbnail_src") ||
                        it.has("image_versions2") ||
                        it.has("video_versions") ||
                        it.has("video_url"))
            }
            .forEach(posts::add)

        return posts
    }

    private fun JSONObject.extractNodesFromConnection(): List<JSONObject> {
        val nodes = mutableListOf<JSONObject>()
        optJSONArray("edges")?.forEachObject { edge ->
            edge.optJSONObject("node")?.let(nodes::add)
        }
        optJSONArray("items")?.forEachObject(nodes::add)
        return nodes
    }

    private fun JSONObject.toPost(username: String, accountId: String): SocialPost? {
        val shortcode = optNullableString("shortcode") ?: optNullableString("code") ?: return null
        val id = optNullableString("id") ?: shortcode

        val mediaItemsList = mutableListOf<PostMediaItem>()

        // Try extracting from carousel_media array
        val carouselMedia = optJSONArray("carousel_media")
        if (carouselMedia != null && carouselMedia.length() > 0) {
            for (i in 0 until carouselMedia.length()) {
                carouselMedia.optJSONObject(i)?.toMediaItem()?.let { mediaItemsList.add(it) }
            }
        }

        // Try extracting from edge_sidecar_to_children edges array
        val sidecarChildren = optJSONObject("edge_sidecar_to_children")?.optJSONArray("edges")
        if (sidecarChildren != null && sidecarChildren.length() > 0) {
            for (i in 0 until sidecarChildren.length()) {
                sidecarChildren.optJSONObject(i)?.optJSONObject("node")?.toMediaItem()?.let { mediaItemsList.add(it) }
            }
        }

        if (mediaItemsList.isEmpty()) {
            val img = imageUrl() ?: return null
            val vid = videoUrl()
            val isVid = vid != null || optBoolean("is_video", false) || optInt("media_type", 0) == 2
            mediaItemsList.add(
                PostMediaItem(
                    imageUrl = img,
                    videoUrl = if (isVid) vid else null,
                    mediaType = if (isVid) PostMediaItem.MEDIA_TYPE_VIDEO else PostMediaItem.MEDIA_TYPE_IMAGE
                )
            )
        }

        val mediaType = if (mediaItemsList.size > 1) {
            SocialPost.MEDIA_TYPE_CAROUSEL
        } else if (mediaItemsList.firstOrNull()?.isVideo == true) {
            SocialPost.MEDIA_TYPE_VIDEO
        } else {
            SocialPost.MEDIA_TYPE_IMAGE
        }

        val caption = optJSONObject("edge_media_to_caption")
            ?.optJSONArray("edges")
            ?.optJSONObject(0)
            ?.optJSONObject("node")
            ?.optNullableString("text")
            ?: optJSONObject("caption")?.optNullableString("text")
            ?: optNullableString("accessibility_caption")
            ?: ""

        val timestamp = optLongOrNull("taken_at_timestamp")
            ?: optLongOrNull("taken_at")
            ?: optLongOrNull("device_timestamp")
            ?: 0L

        return SocialPost(
            platform = Platform.INSTAGRAM,
            id = id,
            platformPostId = shortcode,
            accountId = accountId,
            username = username,
            mediaType = mediaType,
            caption = caption,
            timestampSeconds = timestamp,
            permalink = "https://www.instagram.com/p/$shortcode/",
            mediaItems = mediaItemsList,
        )
    }

    private fun JSONObject.toMediaItem(): PostMediaItem? {
        val imageUrl = imageUrl() ?: return null
        val videoUrl = videoUrl()
        val isVideo = videoUrl != null || optBoolean("is_video", false) || optInt("media_type", 0) == 2
        return PostMediaItem(
            imageUrl = imageUrl,
            videoUrl = if (isVideo) videoUrl else null,
            mediaType = if (isVideo) PostMediaItem.MEDIA_TYPE_VIDEO else PostMediaItem.MEDIA_TYPE_IMAGE
        )
    }

    private fun JSONObject.imageUrl(): String? = optNullableString("display_url")
        ?: optNullableString("thumbnail_src")
        ?: optJSONObject("image_versions2")
            ?.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optNullableString("url")

    private fun JSONObject.videoUrl(): String? = optNullableString("video_url")
        ?: optJSONArray("video_versions")
            ?.optJSONObject(0)
            ?.optNullableString("url")

    private fun JSONObject.findObjects(): List<JSONObject> {
        val found = mutableListOf<JSONObject>()
        fun visit(value: Any?) {
            when (value) {
                is JSONObject -> {
                    found += value
                    value.keys().forEachRemaining { key -> visit(value.opt(key)) }
                }
                is JSONArray -> value.forEach { visit(it) }
            }
        }
        visit(this)
        return found
    }

    private fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
        for (index in 0 until length()) {
            optJSONObject(index)?.let(block)
        }
    }

    private fun JSONArray.forEach(block: (Any?) -> Unit) {
        for (index in 0 until length()) block(opt(index))
    }

    private fun String.toJsonObjectOrNull(): JSONObject? = runCatching { JSONObject(this) }.getOrNull()

    private fun String.htmlUnescape(): String = replace("&quot;", "\"")
        .replace("&#34;", "\"")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

    private fun String.normalizeUsername(): String = trim().removePrefix("@").trim('/').lowercase()

    private fun JSONObject.optNullableString(key: String): String? = optString(key).takeIf { it.isNotBlank() && it != "null" }

    private fun JSONObject.optLongOrNull(key: String): Long? = if (has(key)) runCatching { getLong(key) }.getOrNull() else null

    companion object {
        private val APPLICATION_JSON_SCRIPT = Regex(
            pattern = "<script[^>]+type=[\"']application/json[\"'][^>]*>(.*?)</script>",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val SHARED_DATA_SCRIPT = Regex(
            pattern = "window\\._sharedData\\s*=\\s*(\\{.*?\\})\\s*;</script>",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    }
}

data class ParsedInstagramProfile(
    val account: SocialProfile,
    val userId: String?,
    val posts: List<SocialPost>,
    val nextMaxId: String?,
    val hasMorePosts: Boolean,
)

data class ParsedInstagramFeedPage(
    val posts: List<SocialPost>,
    val nextMaxId: String?,
    val hasMorePosts: Boolean,
)

class InstagramParseException(message: String) : IllegalStateException(message)
