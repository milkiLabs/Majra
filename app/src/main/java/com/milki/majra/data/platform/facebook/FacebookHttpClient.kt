package com.milki.majra.data.platform.facebook

import android.util.Log
import com.milki.majra.data.local.SessionStore
import com.milki.majra.data.model.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Facebook HTTP client using internal web APIs.
 * 
 * Approach: Instead of scraping HTML/WebView, we use the same APIs that
 * Facebook's web app uses to fetch data. This is more reliable and faster.
 * 
 * Key endpoints:
 * 1. Profile page HTML - contains embedded JSON data with posts
 * 2. Extract the initial data from <script> tags
 */
class FacebookHttpClient(
    private val sessionStore: SessionStore,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Fetch profile page HTML which contains embedded JSON data.
     * Facebook embeds all the initial post data in the HTML for faster loading.
     */
    suspend fun fetchProfilePage(username: String): String = withContext(Dispatchers.IO) {
        val session = sessionStore.current(Platform.FACEBOOK)
        if (!session.isAuthenticated) {
            throw FacebookNetworkException("Facebook session is missing. Please sign in first.")
        }

        val cleanUsername = username.trimUsername()
        val url = "https://www.facebook.com/$cleanUsername"
        
        Log.d(TAG, "Fetching Facebook profile page: $url")

        val request = Request.Builder()
            .url(url)
            .header("Cookie", session.cookie)
            .header("User-Agent", DESKTOP_USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            Log.d(TAG, "Profile page response: ${response.code}, length: ${body.length}")
            
            // Save HTML for debugging
            try {
                val debugFile = java.io.File("/sdcard/Download/facebook_http_profile.html")
                debugFile.writeText(body)
                Log.d(TAG, "Saved profile HTML to: ${debugFile.absolutePath}")
            } catch (e: Exception) {
                Log.w(TAG, "Could not save debug file: ${e.message}")
            }
            
            if (!response.isSuccessful) {
                throw FacebookNetworkException(
                    "Facebook returned HTTP ${response.code}. ${body.take(300)}".trim()
                )
            }
            body
        }
    }

    private fun String.trimUsername(): String = trim().removePrefix("@").trim('/').lowercase()

    companion object {
        private const val TAG = "FacebookHttpClient"
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}

class FacebookNetworkException(message: String, cause: Throwable? = null) : IOException(message, cause)