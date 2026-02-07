package com.milkilabs.majra.medium

import java.net.URI
import java.util.Locale

/** Outcome of resolving a Medium input into a feed URL. */
sealed class MediumResolveResult {
    data class Success(val source: MediumResolvedSource) : MediumResolveResult()
    data class Error(val message: String) : MediumResolveResult()
}

/** Normalized Medium source information used for storage and sync. */
data class MediumResolvedSource(
    val feedUrl: String,
    val displayName: String?,
)

/** Resolve Medium handles and URLs into RSS feed URLs. */
class MediumUrlResolver {
    fun resolve(input: String): MediumResolveResult {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return MediumResolveResult.Error("URL cannot be blank.")
        }

        val normalizedInput = normalizeInput(trimmed)

        if (isFeedUrl(normalizedInput)) {
            return MediumResolveResult.Success(
                MediumResolvedSource(feedUrl = normalizedInput, displayName = null),
            )
        }

        val handle = handleFromInput(normalizedInput)
        if (handle != null) {
            return MediumResolveResult.Success(
                MediumResolvedSource(feedUrl = handleFeedUrl(handle), displayName = "@$handle"),
            )
        }

        val publication = publicationFromInput(normalizedInput)
        if (publication != null) {
            return MediumResolveResult.Success(
                MediumResolvedSource(feedUrl = publicationFeedUrl(publication), displayName = publication),
            )
        }

        val customFeedUrl = customDomainFeedFromInput(normalizedInput)
        if (customFeedUrl != null) {
            return MediumResolveResult.Success(
                MediumResolvedSource(feedUrl = customFeedUrl, displayName = null),
            )
        }

        return MediumResolveResult.Error("Unsupported Medium URL.")
    }

    private fun handleFromInput(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.startsWith("@")) {
            return trimmed.removePrefix("@").trim().takeIf { it.isNotBlank() }
        }
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        if (!isMediumHost(uri.host)) return null
        val path = uri.path?.trimEnd('/').orEmpty()
        return if (path.startsWith("/@")) {
            path.removePrefix("/@").trim().takeIf { it.isNotBlank() }
        } else {
            null
        }
    }

    private fun publicationFromInput(input: String): String? {
        val uri = runCatching { URI(input) }.getOrNull() ?: return null
        if (!isMediumHost(uri.host)) return null
        val path = uri.path?.trim('/').orEmpty()
        if (path.isBlank()) return null
        if (path.startsWith("@")) return null
        if (path.startsWith("feed/")) return null
        val root = path.substringBefore('/')
        return root.takeIf { it.isNotBlank() }
    }

    private fun isFeedUrl(input: String): Boolean {
        val uri = runCatching { URI(input) }.getOrNull() ?: return false
        return uri.path.orEmpty().startsWith("/feed/")
    }

    private fun customDomainFeedFromInput(input: String): String? {
        val uri = runCatching { URI(input) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        if (isMediumHost(host)) return null
        val scheme = uri.scheme?.lowercase(Locale.US) ?: return null
        if (scheme != "http" && scheme != "https") return null

        val path = uri.path?.trimEnd('/').orEmpty()
        val feedPath = when {
            path.isBlank() -> "/feed"
            path.startsWith("/feed/") -> path
            path.startsWith("/feed") -> path
            else -> "/feed$path"
        }
        val query = uri.query?.takeIf { it.isNotBlank() }?.let { "?$it" }.orEmpty()
        return "$scheme://$host$feedPath$query"
    }

    private fun normalizeInput(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        return when {
            trimmed.startsWith("medium.com/") -> "https://$trimmed"
            trimmed.startsWith("www.medium.com/") -> "https://$trimmed"
            else -> trimmed
        }
    }

    private fun handleFeedUrl(handle: String): String {
        return "https://medium.com/feed/@$handle"
    }

    private fun publicationFeedUrl(publication: String): String {
        return "https://medium.com/feed/$publication"
    }

    private fun isMediumHost(host: String?): Boolean {
        val normalized = host?.lowercase(Locale.US)?.removePrefix("www.") ?: return false
        return normalized == "medium.com"
    }
}
