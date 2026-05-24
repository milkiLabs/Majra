package com.milki.majra.data.platform.x

import android.util.Log
import com.milki.majra.BuildConfig
import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.PostMediaItem
import com.milki.majra.data.model.SocialPost
import com.milki.majra.data.model.SocialProfile
import org.json.JSONArray
import org.json.JSONObject

class XGraphQLParser {
    data class ParsedProfile(
        val account: SocialProfile,
        val posts: List<SocialPost>,
    )

    fun parse(username: String, rawPayload: String): ParsedProfile {
        val normalizedUsername = username.trimUsername()
        val json = JSONObject(rawPayload.toJsonObjectText())
        if (json.has("error")) {
            throw XParseException("X extraction failed: ${json.optString("error")}")
        }

        val displayName = json.optNullableString("displayName")
            ?.takeIf { it.lowercase() != "x" }
            ?: normalizedUsername
        val profilePicUrl = json.optNullableString("profilePicUrl")

        val account = SocialProfile(
            platform = Platform.X,
            username = normalizedUsername,
            accountId = normalizedUsername,
            displayName = displayName,
            profilePicUrl = profilePicUrl,
        )

        val posts = mutableListOf<SocialPost>()
        json.optJSONArray("posts")?.forEachObject { postJson ->
            runCatching { postJson.toPost(normalizedUsername) }
                .getOrNull()
                ?.let(posts::add)
        }

        val sortedPosts = posts
            .distinctBy { it.id }
            .sortedByDescending { it.timestampSeconds }

        if (BuildConfig.DEBUG) {
            val debug = json.optJSONObject("debug")
            debugLog("Parsed ${sortedPosts.size} X posts; captured=${debug?.optInt("capturedCount")}; dom=${debug?.optInt("domPosts")}")
        }

        return ParsedProfile(account = account, posts = sortedPosts)
    }

    private fun debugLog(message: String) {
        runCatching { Log.d(TAG, message) }
    }

    private fun JSONObject.toPost(username: String): SocialPost? {
        val postId = optNullableString("id") ?: return null
        val text = optNullableString("text").orEmpty()
        val permalink = optNullableString("permalink")
            ?.takeIf { it.startsWith("http") }
            ?: "https://x.com/$username/status/$postId"
        val timestamp = optLongOrNull("timestamp") ?: System.currentTimeMillis() / 1000

        val imageUrls = optJSONArray("images").toStringList()
        val videoUrls = optJSONArray("videos").toStringList()
        val mediaItems = mutableListOf<PostMediaItem>()

        videoUrls.forEachIndexed { index, videoUrl ->
            val thumbnail = imageUrls.getOrNull(index) ?: imageUrls.firstOrNull() ?: videoUrl
            mediaItems += PostMediaItem(
                imageUrl = thumbnail,
                videoUrl = videoUrl,
                mediaType = PostMediaItem.MEDIA_TYPE_VIDEO,
            )
        }

        imageUrls
            .drop(videoUrls.size.coerceAtMost(imageUrls.size))
            .forEach { imageUrl ->
                mediaItems += PostMediaItem(
                    imageUrl = imageUrl,
                    mediaType = PostMediaItem.MEDIA_TYPE_IMAGE,
                )
            }

        if (text.isBlank() && mediaItems.isEmpty()) return null

        val mediaType = when {
            mediaItems.size > 1 -> SocialPost.MEDIA_TYPE_CAROUSEL
            mediaItems.firstOrNull()?.isVideo == true -> SocialPost.MEDIA_TYPE_VIDEO
            else -> SocialPost.MEDIA_TYPE_IMAGE
        }

        return SocialPost(
            platform = Platform.X,
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

    private fun String.toJsonObjectText(): String {
        val trimmed = trim()
        if (trimmed.startsWith("{")) return trimmed
        return trimmed.trim('"')
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\/", "/")
            .replace("\\\\", "\\")
    }

    private fun String.trimUsername(): String = trim().removePrefix("@").trim('/').lowercase()

    private fun JSONObject.optNullableString(key: String): String? =
        optString(key).takeIf { it.isNotBlank() && it != "null" && it != "undefined" }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key)) runCatching { getLong(key) }.getOrNull() else null

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val values = mutableListOf<String>()
        for (index in 0 until length()) {
            optString(index).takeIf { it.isNotBlank() && it != "null" }?.let(values::add)
        }
        return values.distinct()
    }

    private fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
        for (index in 0 until length()) {
            optJSONObject(index)?.let(block)
        }
    }

    private companion object {
        const val TAG = "XGraphQLParser"
    }
}

class XParseException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)
