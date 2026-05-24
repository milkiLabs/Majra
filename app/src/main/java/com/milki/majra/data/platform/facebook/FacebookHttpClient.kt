package com.milki.majra.data.platform.facebook

import android.util.Log
import com.milki.majra.data.local.SessionStore
import com.milki.majra.data.model.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class FacebookHttpClient(
    private val sessionStore: SessionStore,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    
    // Cache tokens to avoid repeated page loads
    private var cachedDtsg: String? = null
    private var cachedLsd: String? = null
    private var cachedJazoest: String? = null

    /**
     * Fetch tokens from Facebook homepage
     * These tokens are required for GraphQL requests
     */
    private suspend fun fetchTokens(): TokenSet = withContext(Dispatchers.IO) {
        // Return cached tokens if available
        if (cachedDtsg != null && cachedLsd != null) {
            return@withContext TokenSet(cachedDtsg!!, cachedLsd, cachedJazoest)
        }
        
        val session = sessionStore.current(Platform.FACEBOOK)
        if (!session.isAuthenticated) {
            throw FacebookNetworkException("Facebook session is missing. Please sign in first.")
        }

        Log.d(TAG, "Fetching tokens from www.facebook.com")

        val request = Request.Builder()
            .url("https://www.facebook.com/")
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
            Log.d(TAG, "Token fetch response code: ${response.code}, body length: ${body.length}")
            
            if (!response.isSuccessful) {
                throw FacebookNetworkException(
                    "Facebook returned HTTP ${response.code}. ${body.take(180)}".trim()
                )
            }
            
            // Extract fb_dtsg token
            val dtsgRegex = Regex(""""DTSGInitialData"[^}]*"token":"([^"]+)"""")
            val dtsgMatch = dtsgRegex.find(body)
            val dtsg = dtsgMatch?.groupValues?.get(1)
            
            // Alternative dtsg extraction
            val dtsgAlt = if (dtsg == null) {
                Regex(""""dtsg":\{"token":"([^"]+)"""").find(body)?.groupValues?.get(1)
            } else dtsg
            
            // Extract lsd token (optional but helpful)
            val lsdRegex = Regex(""""LSD"[^}]*"token":"([^"]+)"""")
            val lsd = lsdRegex.find(body)?.groupValues?.get(1)
            
            // Extract jazoest (anti-CSRF token)
            val jazoestRegex = Regex(""""jazoest":"([^"]+)"""")
            val jazoest = jazoestRegex.find(body)?.groupValues?.get(1)
            
            Log.d(TAG, "Extracted tokens - dtsg: ${dtsgAlt != null}, lsd: ${lsd != null}, jazoest: ${jazoest != null}")
            
            if (dtsgAlt == null) {
                // Save HTML for debugging
                try {
                    val debugFile = File("/sdcard/Download/facebook_tokens_page.html")
                    debugFile.writeText(body)
                    Log.d(TAG, "Saved token page to: ${debugFile.absolutePath}")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not save debug file: ${e.message}")
                }
                throw FacebookNetworkException("Could not extract fb_dtsg token from Facebook page")
            }
            
            // Cache tokens
            cachedDtsg = dtsgAlt
            cachedLsd = lsd
            cachedJazoest = jazoest
            
            TokenSet(dtsgAlt, lsd, jazoest)
        }
    }

    /**
     * Resolve username to numeric user ID by fetching their profile page
     */
    private suspend fun resolveUserId(username: String): String = withContext(Dispatchers.IO) {
        val session = sessionStore.current(Platform.FACEBOOK)
        val cleanUsername = username.trimUsername()
        
        Log.d(TAG, "Resolving user ID for: $cleanUsername")
        
        val request = Request.Builder()
            .url("https://www.facebook.com/$cleanUsername")
            .header("Cookie", session.cookie)
            .header("User-Agent", DESKTOP_USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .get()
            .build()
        
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            
            if (!response.isSuccessful) {
                throw FacebookNetworkException("Could not load profile page for $cleanUsername")
            }
            
            // Try multiple patterns to extract user ID
            val patterns = listOf(
                Regex(""""userID":"(\d+)""""),
                Regex(""""USER_ID":"(\d+)""""),
                Regex(""""user_id":"(\d+)""""),
                Regex(""""profileID":"(\d+)""""),
                Regex(""""profile_id":"(\d+)""""),
                Regex(""""entity_id":"(\d+)""""),
            )
            
            for (pattern in patterns) {
                val match = pattern.find(body)
                if (match != null) {
                    val userId = match.groupValues[1]
                    Log.d(TAG, "Resolved $cleanUsername -> user ID: $userId")
                    return@withContext userId
                }
            }
            
            // If we can't find numeric ID, use username as fallback
            Log.w(TAG, "Could not extract numeric user ID, using username: $cleanUsername")
            return@withContext cleanUsername
        }
    }

    /**
     * Fetch timeline posts using GraphQL API
     */
    suspend fun fetchTimelineGraphQL(
        username: String,
        cursor: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val session = sessionStore.current(Platform.FACEBOOK)
        if (!session.isAuthenticated) {
            throw FacebookNetworkException("Facebook session is missing. Please sign in first.")
        }

        val cleanUsername = username.trimUsername()
        val tokens = fetchTokens()
        
        // Resolve username to numeric user ID
        val userId = resolveUserId(cleanUsername)
        
        Log.d(TAG, "Fetching GraphQL timeline for: $cleanUsername (ID: $userId), cursor: $cursor")

        // Build variables JSON - use the exact format from browser
        val variables = JSONObject().apply {
            put("userID", userId)  // Must be numeric user ID
            put("count", 3)  // Start with small count for testing
            put("scale", 1)
            if (cursor != null) {
                put("cursor", cursor)
            }
        }
        
        Log.d(TAG, "GraphQL variables: ${variables.toString()}")
        
        val formBody = FormBody.Builder()
            .add("fb_dtsg", tokens.dtsg)
            .add("doc_id", PROFILE_TIMELINE_DOC_ID)
            .add("variables", variables.toString())
            .apply {
                tokens.lsd?.let { add("lsd", it) }
                tokens.jazoest?.let { add("jazoest", it) }
                
                // Additional parameters that Facebook expects
                add("__a", "1")
                add("__req", "1")
                add("dpr", "1")
                add("__ccg", "GOOD")
                add("__comet_req", "15")
                add("fb_api_caller_class", "RelayModern")
                add("fb_api_req_friendly_name", "ProfileCometTimelineFeedQuery")
                add("server_timestamps", "true")
            }
            .build()

        val request = Request.Builder()
            .url("https://www.facebook.com/api/graphql/")
            .header("Cookie", session.cookie)
            .header("User-Agent", DESKTOP_USER_AGENT)
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Origin", "https://www.facebook.com")
            .header("Referer", "https://www.facebook.com/$cleanUsername")
            .header("X-FB-Friendly-Name", "ProfileCometTimelineFeedQuery")
            .header("X-FB-LSD", tokens.lsd ?: "")
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Site", "same-origin")
            .post(formBody)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            Log.d(TAG, "GraphQL response code: ${response.code}, body length: ${body.length}")
            
            // Save response for debugging
            try {
                val debugFile = File("/sdcard/Download/facebook_graphql_response.json")
                debugFile.writeText(body)
                Log.d(TAG, "Saved GraphQL response to: ${debugFile.absolutePath}")
            } catch (e: Exception) {
                Log.w(TAG, "Could not save debug file: ${e.message}")
            }
            
            if (!response.isSuccessful) {
                // Token might be expired, clear cache
                cachedDtsg = null
                cachedLsd = null
                cachedJazoest = null
                throw FacebookNetworkException(
                    "Facebook GraphQL returned HTTP ${response.code}. ${body.take(300)}".trim()
                )
            }
            body
        }
    }

    /**
     * Alternative: Fetch using mobile basic site (mbasic.facebook.com)
     * This is simpler HTML that's easier to parse
     */
    suspend fun fetchMobileBasicProfile(username: String, cursor: String? = null): String = 
        withContext(Dispatchers.IO) {
            val session = sessionStore.current(Platform.FACEBOOK)
            if (!session.isAuthenticated) {
                throw FacebookNetworkException("Facebook session is missing. Please sign in first.")
            }

            val cleanUsername = username.trimUsername()
            val baseUrl = "https://mbasic.facebook.com/$cleanUsername"
            val url = if (cursor != null) {
                "$baseUrl?v=timeline&cursor=$cursor"
            } else {
                "$baseUrl?v=timeline"
            }

            Log.d(TAG, "Fetching mbasic profile: $url")

            val request = Request.Builder()
                .url(url)
                .header("Cookie", session.cookie)
                .header("User-Agent", MOBILE_USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", "https://mbasic.facebook.com/")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                Log.d(TAG, "mbasic response code: ${response.code}, body length: ${body.length}")
                
                // Save response for debugging
                try {
                    val debugFile = File("/sdcard/Download/facebook_mbasic_response.html")
                    debugFile.writeText(body)
                    Log.d(TAG, "Saved mbasic response to: ${debugFile.absolutePath}")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not save debug file: ${e.message}")
                }
                
                if (!response.isSuccessful) {
                    throw FacebookNetworkException(
                        "Facebook returned HTTP ${response.code}. ${body.take(180)}".trim()
                    )
                }
                body
            }
        }

    private fun String.trimUsername(): String = trim().removePrefix("@").trim('/').lowercase()
    
    data class TokenSet(
        val dtsg: String,
        val lsd: String?,
        val jazoest: String?,
    )

    companion object {
        private const val TAG = "FacebookHttpClient"
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        
        // GraphQL doc_id for profile timeline query
        // This may need to be updated periodically - find by inspecting network traffic
        private const val PROFILE_TIMELINE_DOC_ID = "35707794818864821"
    }
}

class FacebookNetworkException(message: String, cause: Throwable? = null) : IOException(message, cause)
