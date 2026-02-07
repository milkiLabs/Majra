package com.milkilabs.majra.youtube

import java.net.URI
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/** Outcome of resolving a YouTube input into a feed URL. */
sealed class YoutubeResolveResult {
    data class Success(val source: YoutubeResolvedSource) : YoutubeResolveResult()
    data class Error(val message: String) : YoutubeResolveResult()
}

/** Normalized YouTube source information used for storage and sync. */
data class YoutubeResolvedSource(
    val feedUrl: String,
    val displayName: String?,
)

/**
 * Resolve YouTube inputs (handle URL, channel URL/ID, playlist URL) into feed URLs.
 *
 * Uses the YouTube oEmbed endpoint to resolve handles to channel IDs.
 */
class YoutubeUrlResolver(
    private val httpClient: OkHttpClient,
) {
    suspend fun resolve(input: String): YoutubeResolveResult = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return@withContext YoutubeResolveResult.Error("URL cannot be blank.")
        }

        val normalizedInput = normalizeInput(trimmed)

        if (isFeedUrl(normalizedInput)) {
            return@withContext YoutubeResolveResult.Success(
                YoutubeResolvedSource(feedUrl = normalizedInput, displayName = null),
            )
        }

        val channelId = channelIdFromInput(normalizedInput)
        if (channelId != null) {
            return@withContext YoutubeResolveResult.Success(
                YoutubeResolvedSource(feedUrl = channelFeedUrl(channelId), displayName = null),
            )
        }

        val playlistId = playlistIdFromInput(normalizedInput)
        if (playlistId != null) {
            return@withContext YoutubeResolveResult.Success(
                YoutubeResolvedSource(feedUrl = playlistFeedUrl(playlistId), displayName = null),
            )
        }

        val handle = handleFromInput(normalizedInput)
        if (handle != null) {
            return@withContext resolveHandle(handle)
        }

        YoutubeResolveResult.Error("Unsupported YouTube URL.")
    }

    private fun resolveHandle(handle: String): YoutubeResolveResult {
        val handleUrls = listOf(
            "https://www.youtube.com/@$handle",
            "https://youtube.com/@$handle",
            "https://www.youtube.com/@$handle/videos",
            "https://www.youtube.com/@$handle/about",
            "https://youtube.com/@$handle/videos",
            "https://youtube.com/@$handle/about",
        )
        handleUrls.forEach { handleUrl ->
            val oembedUrl =
                "https://www.youtube.com/oembed?url=${URLEncoder.encode(handleUrl, "UTF-8")}&format=json"
            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Majra/1.0")
                .header("Accept", "application/json")
                .header("Accept-Language", "en-US,en;q=0.9")
                .get()
                .build()
            val response = runCatching { httpClient.newCall(request).execute() }.getOrNull()
            response?.use { result ->
                if (result.isSuccessful) {
                    val body = result.body?.string().orEmpty()
                    val payload = runCatching { JSONObject(body) }.getOrNull()
                    val authorUrl = payload?.optString("author_url").orEmpty()
                    val authorName = payload?.optString("author_name")?.takeIf { it.isNotBlank() }
                    val resolvedChannelId = channelIdFromInput(authorUrl)
                    if (resolvedChannelId != null) {
                        return YoutubeResolveResult.Success(
                            YoutubeResolvedSource(
                                feedUrl = channelFeedUrl(resolvedChannelId),
                                displayName = authorName,
                            ),
                        )
                    }
                }
            }
        }

        // Fallback: fetch the handle page and try to extract the channelId from the HTML.
        handleUrls.forEach { handleUrl ->
            val request = Request.Builder()
                .url(handleUrl)
                .header("User-Agent", "Majra/1.0")
                .header("Accept", "text/html")
                .header("Accept-Language", "en-US,en;q=0.9")
                .get()
                .build()
            val response = runCatching { httpClient.newCall(request).execute() }.getOrNull()
            response?.use { result ->
                if (!result.isSuccessful) return@use
                val body = result.body?.string().orEmpty()
                val channelId = extractChannelId(body)
                if (channelId != null) {
                    return YoutubeResolveResult.Success(
                        YoutubeResolvedSource(
                            feedUrl = channelFeedUrl(channelId),
                            displayName = null,
                        ),
                    )
                }
            }
        }

        return YoutubeResolveResult.Error(
            "Could not resolve the YouTube handle. Try a channel ID or playlist URL.",
        )
    }

    private fun channelIdFromInput(input: String): String? {
        if (CHANNEL_ID_REGEX.matches(input)) return input
        val uri = runCatching { URI(input) }.getOrNull() ?: return null
        if (!isYoutubeHost(uri.host)) return null
        val path = uri.path?.trimEnd('/').orEmpty()
        if (!path.startsWith("/channel/")) return null
        val candidate = path.removePrefix("/channel/")
        return candidate.takeIf { CHANNEL_ID_REGEX.matches(it) }
    }

    private fun playlistIdFromInput(input: String): String? {
        val uri = runCatching { URI(input) }.getOrNull() ?: return null
        if (!isYoutubeHost(uri.host)) return null
        val query = uri.query.orEmpty()
        val listParam = query.split("&")
            .firstOrNull { it.startsWith("list=") }
            ?.removePrefix("list=")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        return listParam
    }

    private fun handleFromInput(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.startsWith("@")) {
            return trimmed.removePrefix("@").trim().takeIf { it.isNotBlank() }
        }
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        if (!isYoutubeHost(uri.host)) return null
        val path = uri.path?.trimEnd('/').orEmpty()
        return if (path.startsWith("/@")) {
            path.removePrefix("/@").trim().takeIf { it.isNotBlank() }
        } else {
            null
        }
    }

    private fun isFeedUrl(input: String): Boolean {
        val uri = runCatching { URI(input) }.getOrNull() ?: return false
        if (!isYoutubeHost(uri.host)) return false
        val path = uri.path.orEmpty()
        return path.contains("/feeds/videos.xml")
    }

    private fun normalizeInput(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        return when {
            trimmed.startsWith("youtube.com/") -> "https://$trimmed"
            trimmed.startsWith("www.youtube.com/") -> "https://$trimmed"
            trimmed.startsWith("m.youtube.com/") -> "https://$trimmed"
            else -> trimmed
        }
    }

    private fun channelFeedUrl(channelId: String): String {
        return "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
    }

    private fun playlistFeedUrl(playlistId: String): String {
        return "https://www.youtube.com/feeds/videos.xml?playlist_id=$playlistId"
    }

    private fun isYoutubeHost(host: String?): Boolean {
        val normalized = host?.lowercase(Locale.US)?.removePrefix("www.") ?: return false
        return normalized == "youtube.com" || normalized == "m.youtube.com"
    }

    private fun extractChannelId(body: String): String? {
        val patterns = listOf(
            CHANNEL_ID_JSON_REGEX,
            CHANNEL_ID_ESCAPED_JSON_REGEX,
            BROWSE_ID_JSON_REGEX,
            BROWSE_ID_ESCAPED_JSON_REGEX,
            EXTERNAL_ID_JSON_REGEX,
            EXTERNAL_ID_ESCAPED_JSON_REGEX,
        )
        patterns.forEach { pattern ->
            val match = pattern.find(body)?.groupValues?.getOrNull(1)
            if (match != null && CHANNEL_ID_REGEX.matches(match)) {
                return match
            }
        }
        return null
    }

    private companion object {
        val CHANNEL_ID_REGEX = Regex("^UC[a-zA-Z0-9_-]{22}$")
        val CHANNEL_ID_JSON_REGEX = Regex("\"channelId\":\"(UC[a-zA-Z0-9_-]{22})\"")
        val CHANNEL_ID_ESCAPED_JSON_REGEX = Regex("\\\\\"channelId\\\\\":\\\\\"(UC[a-zA-Z0-9_-]{22})\\\\\"")
        val BROWSE_ID_JSON_REGEX = Regex("\"browseId\":\"(UC[a-zA-Z0-9_-]{22})\"")
        val BROWSE_ID_ESCAPED_JSON_REGEX = Regex("\\\\\"browseId\\\\\":\\\\\"(UC[a-zA-Z0-9_-]{22})\\\\\"")
        val EXTERNAL_ID_JSON_REGEX = Regex("\"externalId\":\"(UC[a-zA-Z0-9_-]{22})\"")
        val EXTERNAL_ID_ESCAPED_JSON_REGEX = Regex("\\\\\"externalId\\\\\":\\\\\"(UC[a-zA-Z0-9_-]{22})\\\\\"")
    }
}
