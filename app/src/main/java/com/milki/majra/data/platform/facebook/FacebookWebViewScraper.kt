package com.milki.majra.data.platform.facebook

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.milki.majra.data.local.SessionStore
import com.milki.majra.data.model.Platform
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Facebook scraper that intercepts GraphQL responses from the WebView.
 * 
 * Architecture:
 * 1. Load Facebook profile page in WebView
 * 2. Inject JavaScript to intercept fetch/XHR requests
 * 3. Capture GraphQL responses that Facebook makes internally
 * 4. Extract post data from the captured GraphQL JSON
 * 
 * This is the most reliable approach since we get the exact data
 * that Facebook's own JavaScript uses to render the page.
 */
class FacebookWebViewScraper(
    private val context: Context,
    private val sessionStore: SessionStore,
) {
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun scrapeProfile(username: String): String = withContext(Dispatchers.Main) {
        val session = sessionStore.current(Platform.FACEBOOK)
        val cleanUsername = username.trimUsername()
        
        // Load the timeline/posts page directly
        val url = "https://www.facebook.com/$cleanUsername/posts"
        
        Log.d(TAG, "Loading Facebook timeline with GraphQL interception: $url")

        syncCookies(session.cookie)

        val webView = WebView(context)
        
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(1366, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(768, View.MeasureSpec.EXACTLY),
        )
        webView.layout(0, 0, 1366, 768)

        @Suppress("DEPRECATION")
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            userAgentString = DESKTOP_USER_AGENT
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            blockNetworkImage = false
        }
        
        webView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d(TAG, "JS: ${it.message()}")
                }
                return true
            }
        }

        val pageLoaded = CompletableDeferred<Unit>()
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d(TAG, "Page finished: $url")
                
                // Inject GraphQL interception script immediately
                view?.evaluateJavascript(GRAPHQL_INTERCEPT_SCRIPT) { result ->
                    Log.d(TAG, "Interception script injected: $result")
                }
                
                if (!pageLoaded.isCompleted) {
                    pageLoaded.complete(Unit)
                }
            }
        }

        webView.loadUrl(url)

        try {
            withTimeout(30000) {
                pageLoaded.await()
                
                // Wait for initial GraphQL responses
                Log.d(TAG, "Waiting for initial GraphQL responses...")
                delay(4000)
                
                // Scroll down multiple times to trigger timeline post loading
                Log.d(TAG, "Scrolling to trigger timeline loading...")
                for (i in 1..5) {
                    val scrollY = i * 800
                    webView.evaluateJavascript("window.scrollTo(0, $scrollY);", null)
                    delay(1500)
                }
                
                // Wait for timeline GraphQL requests to complete
                Log.d(TAG, "Waiting for timeline GraphQL responses...")
                delay(5000)
                
                Log.d(TAG, "Extracting captured GraphQL data")
                
                val deferred = CompletableDeferred<String>()
                webView.evaluateJavascript(GET_CAPTURED_GRAPHQL_SCRIPT) { value ->
                    val result = value ?: """{"posts":[]}"""
                    Log.d(TAG, "Captured GraphQL data length: ${result.length}")
                    
                    // Save for debugging
                    try {
                        val debugFile = java.io.File("/sdcard/Download/facebook_graphql_captured.json")
                        val cleanResult = result.trim('"')
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                        debugFile.writeText(cleanResult)
                        Log.d(TAG, "Saved captured GraphQL to: ${debugFile.absolutePath}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not save debug file: ${e.message}")
                    }
                    
                    deferred.complete(result)
                }
                deferred.await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Scraping failed: ${e.message}", e)
            """{"error":"${e.message?.replace("\"", "\\\"")?.replace("\n", "\\n") ?: "Unknown"}","posts":[]}"""
        } finally {
            webView.stopLoading()
            webView.destroy()
        }
    }

    private fun syncCookies(cookie: String) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(WebView(context), true)
        
        val parts = cookie.split(";").map { it.trim() }.filter { it.isNotBlank() }
        val domains = listOf(
            "https://facebook.com",
            "https://www.facebook.com",
            "https://m.facebook.com"
        )
        
        for (part in parts) {
            for (domain in domains) {
                cookieManager.setCookie(domain, part)
            }
        }
        cookieManager.flush()
        
        Log.d(TAG, "Synced ${parts.size} cookie parts")
    }

    private fun String.trimUsername(): String = trim().removePrefix("@").trim('/').lowercase()

    companion object {
        private const val TAG = "FBWebViewScraper"
        
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        /**
         * JavaScript to intercept fetch and XMLHttpRequest calls.
         * This captures GraphQL responses that Facebook makes internally.
         */
        private val GRAPHQL_INTERCEPT_SCRIPT = """
(function() {
    console.log('[Interceptor] Installing GraphQL interceptor...');
    
    // Storage for captured GraphQL responses
    window.__capturedGraphQL = [];
    
    // Intercept fetch API
    const originalFetch = window.fetch;
    window.fetch = function(...args) {
        const url = args[0];
        const urlString = typeof url === 'string' ? url : url.url;
        
        return originalFetch.apply(this, args).then(response => {
            // Check if this is a GraphQL request
            if (urlString && (urlString.includes('graphql') || urlString.includes('/api/'))) {
                console.log('[Interceptor] Captured fetch to: ' + urlString.substring(0, 100));
                
                // Clone and read the response
                response.clone().text().then(text => {
                    try {
                        // Facebook sometimes sends NDJSON (newline-delimited JSON)
                        // Try to parse as single JSON first
                        try {
                            const json = JSON.parse(text);
                            window.__capturedGraphQL.push({
                                url: urlString,
                                data: json,
                                timestamp: Date.now()
                            });
                            console.log('[Interceptor] Stored GraphQL response, total: ' + window.__capturedGraphQL.length);
                        } catch (e) {
                            // If single parse fails, try splitting by newlines (NDJSON)
                            const lines = text.split('\n').filter(line => line.trim());
                            let parsed = 0;
                            for (let line of lines) {
                                try {
                                    const json = JSON.parse(line);
                                    window.__capturedGraphQL.push({
                                        url: urlString,
                                        data: json,
                                        timestamp: Date.now()
                                    });
                                    parsed++;
                                } catch (lineError) {
                                    // Skip invalid lines
                                }
                            }
                            if (parsed > 0) {
                                console.log('[Interceptor] Stored ' + parsed + ' NDJSON responses, total: ' + window.__capturedGraphQL.length);
                            } else {
                                console.log('[Interceptor] Failed to parse response: ' + e.message);
                            }
                        }
                    } catch (e) {
                        console.log('[Interceptor] Failed to parse response: ' + e.message);
                    }
                });
            }
            
            return response;
        });
    };
    
    // Intercept XMLHttpRequest
    const originalOpen = XMLHttpRequest.prototype.open;
    const originalSend = XMLHttpRequest.prototype.send;
    
    XMLHttpRequest.prototype.open = function(method, url, ...rest) {
        this.__interceptedUrl = url;
        return originalOpen.apply(this, [method, url, ...rest]);
    };
    
    XMLHttpRequest.prototype.send = function(...args) {
        this.addEventListener('load', function() {
            const url = this.__interceptedUrl;
            if (url && (url.includes('graphql') || url.includes('/api/'))) {
                console.log('[Interceptor] Captured XHR to: ' + url.substring(0, 100));
                
                try {
                    const text = this.responseText;
                    // Facebook sometimes sends NDJSON (newline-delimited JSON)
                    // Try to parse as single JSON first
                    try {
                        const json = JSON.parse(text);
                        window.__capturedGraphQL.push({
                            url: url,
                            data: json,
                            timestamp: Date.now()
                        });
                        console.log('[Interceptor] Stored XHR response, total: ' + window.__capturedGraphQL.length);
                    } catch (e) {
                        // If single parse fails, try splitting by newlines (NDJSON)
                        const lines = text.split('\n').filter(line => line.trim());
                        let parsed = 0;
                        for (let line of lines) {
                            try {
                                const json = JSON.parse(line);
                                window.__capturedGraphQL.push({
                                    url: url,
                                    data: json,
                                    timestamp: Date.now()
                                });
                                parsed++;
                            } catch (lineError) {
                                // Skip invalid lines
                            }
                        }
                        if (parsed > 0) {
                            console.log('[Interceptor] Stored ' + parsed + ' NDJSON XHR responses, total: ' + window.__capturedGraphQL.length);
                        } else {
                            console.log('[Interceptor] Failed to parse XHR response: ' + e.message);
                        }
                    }
                } catch (e) {
                    console.log('[Interceptor] Failed to parse XHR response: ' + e.message);
                }
            }
        });
        
        return originalSend.apply(this, args);
    };
    
    console.log('[Interceptor] GraphQL interceptor installed successfully');
})();
        """.trimIndent()

        /**
         * JavaScript to extract and parse captured GraphQL responses.
         * This processes the intercepted data and extracts posts.
         */
        private val GET_CAPTURED_GRAPHQL_SCRIPT = """
(function() {
    try {
        console.log('[Extractor] Processing ' + (window.__capturedGraphQL || []).length + ' captured responses');
        
        const captured = window.__capturedGraphQL || [];
        const posts = [];
        const seenIds = {};
        let displayName = '';
        let profilePicUrl = '';
        
        // Save raw captured data for debugging
        const rawDebug = [];
        
        // Process each captured GraphQL response
        for (let i = 0; i < captured.length; i++) {
            const item = captured[i];
            const data = item.data;
            
            if (!data) continue;
            
            console.log('[Extractor] Processing response ' + i + ' from: ' + (item.url || '').substring(0, 50));
            
            // Save first 500 chars of each response for debugging
            rawDebug.push({
                index: i,
                url: (item.url || '').substring(0, 100),
                dataPreview: JSON.stringify(data).substring(0, 500)
            });
            
            // Recursively search for post data in the response
            findPosts(data, posts, seenIds);
            
            // Try to find profile info
            const profile = findProfile(data);
            if (profile) {
                if (profile.name) displayName = profile.name;
                if (profile.pic) profilePicUrl = profile.pic;
            }
        }
        
        console.log('[Extractor] Extracted ' + posts.length + ' posts');
        
        // If no display name found, try from page title
        if (!displayName) {
            displayName = (document.title || '').replace(/\s*[-|–|]\s*Facebook/i, '').trim();
        }
        
        return JSON.stringify({
            displayName: displayName,
            profilePicUrl: profilePicUrl,
            posts: posts,
            debug: {
                capturedCount: captured.length,
                extractedPosts: posts.length,
                rawResponses: rawDebug
            }
        });
        
    } catch (e) {
        console.error('[Extractor] Error: ' + e.message);
        return JSON.stringify({
            error: e.message,
            posts: [],
            debug: e.stack
        });
    }
    
    // Helper function to recursively find posts in GraphQL response
    function findPosts(obj, posts, seenIds) {
        if (!obj || typeof obj !== 'object') return;
        
        // Facebook uses different structures for posts:
        // 1. Story/post nodes with __typename: "Story"
        // 2. Feed edge nodes with comet_sections
        // 3. Timeline items with creation_story
        
        // Check for Story type (most common in timeline)
        if (obj.__typename === 'Story' || obj.node?.__typename === 'Story') {
            const story = obj.node || obj;
            const postId = story.id || story.post_id;
            
            if (postId && !seenIds[postId]) {
                seenIds[postId] = true;
                
                // Extract text from comet_sections or message
                let text = '';
                if (story.comet_sections) {
                    const sections = story.comet_sections;
                    if (sections.message) {
                        text = sections.message.story?.message?.text || '';
                    } else if (sections.content) {
                        text = sections.content.story?.message?.text || '';
                    }
                }
                if (!text && story.message) {
                    text = story.message.text || story.message;
                }
                
                // Extract timestamp
                let timestamp = Math.floor(Date.now() / 1000);
                if (story.created_time) {
                    timestamp = story.created_time;
                } else if (story.publish_time) {
                    timestamp = story.publish_time;
                }
                
                // Extract permalink
                let permalink = story.url || story.permalink_url || '';
                
                // Extract media
                const images = [];
                const videos = [];
                
                if (story.attachments) {
                    const atts = Array.isArray(story.attachments) ? story.attachments : [story.attachments];
                    for (let att of atts) {
                        if (att.media) {
                            if (att.media.image?.uri) images.push(att.media.image.uri);
                            if (att.media.source) videos.push(att.media.source);
                        }
                    }
                }
                
                posts.push({
                    id: postId,
                    text: text,
                    timestamp: timestamp,
                    images: images,
                    video: videos.length > 0 ? videos[0] : '',
                    permalink: permalink
                });
                
                console.log('[Extractor] Found Story post: ' + postId + ', text: ' + text.substring(0, 50));
            }
        }
        
        // Check for generic post structure (fallback)
        else if (obj.id && (obj.message || obj.story || obj.text || obj.creation_story)) {
            const postId = String(obj.id);
            
            if (!seenIds[postId]) {
                seenIds[postId] = true;
                
                // Extract text content
                let text = '';
                if (obj.message && obj.message.text) {
                    text = obj.message.text;
                } else if (typeof obj.message === 'string') {
                    text = obj.message;
                } else if (obj.story && obj.story.text) {
                    text = obj.story.text;
                } else if (typeof obj.story === 'string') {
                    text = obj.story;
                } else if (obj.text) {
                    text = obj.text;
                }
                
                // Extract timestamp
                let timestamp = Math.floor(Date.now() / 1000);
                if (obj.created_time) {
                    timestamp = obj.created_time;
                } else if (obj.publish_time) {
                    timestamp = obj.publish_time;
                } else if (obj.timestamp) {
                    timestamp = obj.timestamp;
                }
                
                // Extract permalink
                let permalink = '';
                if (obj.url) {
                    permalink = obj.url;
                } else if (obj.permalink_url) {
                    permalink = obj.permalink_url;
                }
                
                // Extract media
                const images = [];
                const videos = [];
                
                if (obj.attachments && obj.attachments.data) {
                    for (let att of obj.attachments.data) {
                        if (att.media) {
                            if (att.media.image && att.media.image.uri) {
                                images.push(att.media.image.uri);
                            }
                            if (att.media.source) {
                                videos.push(att.media.source);
                            }
                        }
                    }
                }
                
                posts.push({
                    id: postId,
                    text: text,
                    timestamp: timestamp,
                    images: images,
                    video: videos.length > 0 ? videos[0] : '',
                    permalink: permalink
                });
                
                console.log('[Extractor] Found generic post: ' + postId + ', text: ' + text.substring(0, 50));
            }
        }
        
        // Recursively search in arrays and objects
        if (Array.isArray(obj)) {
            for (let item of obj) {
                findPosts(item, posts, seenIds);
            }
        } else {
            for (let key in obj) {
                if (obj.hasOwnProperty(key)) {
                    findPosts(obj[key], posts, seenIds);
                }
            }
        }
    }
    
    // Helper function to find profile info
    function findProfile(obj) {
        if (!obj || typeof obj !== 'object') return null;
        
        // Check if this looks like a user/profile object
        if (obj.name && (obj.profile_picture || obj.picture)) {
            return {
                name: obj.name,
                pic: obj.profile_picture ? obj.profile_picture.uri : (obj.picture ? obj.picture.uri : '')
            };
        }
        
        // Recursively search
        if (Array.isArray(obj)) {
            for (let item of obj) {
                const result = findProfile(item);
                if (result) return result;
            }
        } else {
            for (let key in obj) {
                if (obj.hasOwnProperty(key)) {
                    const result = findProfile(obj[key]);
                    if (result) return result;
                }
            }
        }
        
        return null;
    }
})();
        """.trimIndent()
    }
}
