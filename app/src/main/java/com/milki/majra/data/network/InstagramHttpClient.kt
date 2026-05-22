package com.milki.majra.data.network

import com.milki.majra.data.local.SessionStore
import com.milki.majra.data.model.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class InstagramHttpClient(
    private val sessionStore: SessionStore,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchProfileHtml(username: String): String = withContext(Dispatchers.IO) {
        val session = sessionStore.current(Platform.INSTAGRAM)
        if (!session.isAuthenticated) {
            throw InstagramNetworkException("Instagram session is missing. Please sign in first.")
        }

        val request = Request.Builder()
            .url("https://www.instagram.com/${username.trimUsername()}/")
            .header("Cookie", session.cookie)
            .header("User-Agent", InstagramUserAgent.choose(session.userAgent))
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", "https://www.instagram.com/")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw InstagramNetworkException(
                    "Instagram returned HTTP ${response.code}. ${body.take(180)}".trim(),
                )
            }
            body
        }
    }

    suspend fun fetchProfileJson(username: String): String = withContext(Dispatchers.IO) {
        val session = sessionStore.current(Platform.INSTAGRAM)
        if (!session.isAuthenticated) {
            throw InstagramNetworkException("Instagram session is missing. Please sign in first.")
        }

        val trimmedUsername = username.trimUsername()
        val url = "https://www.instagram.com/api/v1/users/web_profile_info/"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("username", trimmedUsername)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Cookie", session.cookie)
            .header("User-Agent", InstagramUserAgent.choose(session.userAgent))
            .header("Accept", "application/json")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", "https://www.instagram.com/$trimmedUsername/")
            .header("X-IG-App-ID", INSTAGRAM_WEB_APP_ID)
            .header("X-Requested-With", "XMLHttpRequest")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw InstagramNetworkException(
                    "Instagram returned HTTP ${response.code}. ${body.take(180)}".trim(),
                )
            }
            body
        }
    }

    suspend fun fetchUserFeedJson(
        userId: String,
        username: String,
        maxId: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val session = sessionStore.current(Platform.INSTAGRAM)
        if (!session.isAuthenticated) {
            throw InstagramNetworkException("Instagram session is missing. Please sign in first.")
        }

        val trimmedUsername = username.trimUsername()
        val url = "https://www.instagram.com/api/v1/feed/user/$userId/"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("count", "12")
            .apply {
                if (!maxId.isNullOrBlank()) {
                    addQueryParameter("max_id", maxId)
                }
            }
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Cookie", session.cookie)
            .header("User-Agent", InstagramUserAgent.choose(session.userAgent))
            .header("Accept", "application/json")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", "https://www.instagram.com/$trimmedUsername/")
            .header("X-IG-App-ID", INSTAGRAM_WEB_APP_ID)
            .header("X-Requested-With", "XMLHttpRequest")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw InstagramNetworkException(
                    "Instagram returned HTTP ${response.code}. ${body.take(180)}".trim(),
                )
            }
            body
        }
    }

    private fun String.trimUsername(): String = trim().removePrefix("@").trim('/').lowercase()

    private companion object {
        const val INSTAGRAM_WEB_APP_ID = "936619743392459"
    }
}

class InstagramNetworkException(message: String, cause: Throwable? = null) : IOException(message, cause)
