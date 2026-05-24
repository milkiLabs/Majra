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
 * Facebook scraper using mobile web (m.facebook.com) for simpler, more reliable extraction.
 * 
 * Architecture:
 * 1. Load m.facebook.com/{username} with cookies
 * 2. Wait for page to fully load (no scrolling needed - mobile shows posts immediately)
 * 3. Extract posts from simple mobile DOM structure
 * 4. Parse story containers which have consistent structure
 */
class FacebookWebViewScraper(
    private val context: Context,
    private val sessionStore: SessionStore,
) {
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun scrapeProfile(username: String): String = withContext(Dispatchers.Main) {
        val session = sessionStore.current(Platform.FACEBOOK)
        val cleanUsername = username.trimUsername()
        
        // Use mobile Facebook - much simpler DOM, posts load immediately
        val url = "https://m.facebook.com/$cleanUsername"
        
        Log.d(TAG, "Loading mobile Facebook profile: $url")

        syncCookies(session.cookie)

        val webView = WebView(context)
        
        // Mobile viewport size
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(412, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(915, View.MeasureSpec.EXACTLY),
        )
        webView.layout(0, 0, 412, 915)

        @Suppress("DEPRECATION")
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            userAgentString = MOBILE_USER_AGENT
            loadWithOverviewMode = true
            useWideViewPort = false
            builtInZoomControls = false
            displayZoomControls = false
            blockNetworkImage = false
        }
        
        // Enable console logging
        webView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d(TAG, "JS Console: ${it.message()}")
                }
                return true
            }
        }

        val pageLoaded = CompletableDeferred<Unit>()
        var loadCount = 0
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                loadCount++
                Log.d(TAG, "onPageFinished ($loadCount): $url")
                // Mobile Facebook sometimes triggers multiple loads
                if (loadCount >= 1 && !pageLoaded.isCompleted) {
                    pageLoaded.complete(Unit)
                }
            }
        }

        webView.loadUrl(url)

        try {
            withTimeout(30000) {
                pageLoaded.await()
                
                // Wait for content to render - mobile Facebook uses heavy JS
                Log.d(TAG, "Waiting for JavaScript to render content...")
                delay(5000)
                
                // Check if page is ready
                for (i in 0 until 10) {
                    val ready = CompletableDeferred<String>()
                    webView.evaluateJavascript("document.readyState") { v -> 
                        ready.complete(v ?: "") 
                    }
                    if (ready.await().trim('"') == "complete") break
                    delay(500)
                }
                
                // Wait for React/WebLite to render posts
                Log.d(TAG, "Waiting for posts to render...")
                delay(8000)
                
                Log.d(TAG, "Extracting posts from mobile DOM")
                
                // First, save the HTML for debugging
                val htmlDeferred = CompletableDeferred<String>()
                webView.evaluateJavascript("document.documentElement.outerHTML") { html ->
                    htmlDeferred.complete(html ?: "")
                }
                val htmlContent = htmlDeferred.await()
                
                // Save to file for inspection
                try {
                    val debugFile = java.io.File("/sdcard/Download/facebook_mobile_page.html")
                    // Remove quotes and unescape
                    val cleanHtml = htmlContent.trim('"').replace("\\n", "\n").replace("\\\"", "\"")
                    debugFile.writeText(cleanHtml)
                    Log.d(TAG, "Saved mobile HTML to: ${debugFile.absolutePath}, size: ${cleanHtml.length}")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not save debug HTML: ${e.message}")
                }
                
                val deferred = CompletableDeferred<String>()
                webView.evaluateJavascript(MOBILE_EXTRACTION_SCRIPT) { value ->
                    val result = value ?: """{"posts":[]}"""
                    Log.d(TAG, "Extraction result length: ${result.length}")
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
        
        Log.d(TAG, "Synced ${parts.size} cookie parts to Facebook domains")
    }

    private fun String.trimUsername(): String = trim().removePrefix("@").trim('/').lowercase()

    companion object {
        private const val TAG = "FBWebViewScraper"
        
        // Mobile user agent for simpler page structure
        private const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        /**
         * Extraction script for mobile Facebook (m.facebook.com)
         * 
         * Mobile Facebook structure:
         * - Modern m.facebook uses React/WebLite with dynamic rendering
         * - Posts may be in various container types
         * - Need to search broadly for post indicators
         */
        private val MOBILE_EXTRACTION_SCRIPT = """
(function() {
    try {
        // Debug: log what we find
        console.log('Starting extraction...');
        console.log('Document title: ' + document.title);
        console.log('Body text length: ' + document.body.textContent.length);
        
        // Extract profile info
        var displayName = '';
        var title = (document.title || '').replace(/\s*[-|–]\s*Facebook/i, '').trim();
        if (title && title.toLowerCase() !== 'facebook' && title.toLowerCase() !== 'error') {
            displayName = title;
        }
        
        console.log('Display name: ' + displayName);
        
        // Profile picture
        var profilePicUrl = '';
        var imgs = document.querySelectorAll('img');
        console.log('Found ' + imgs.length + ' images');
        
        for (var i = 0; i < imgs.length; i++) {
            var src = imgs[i].src || '';
            if (src.indexOf('scontent') > -1) {
                profilePicUrl = src;
                break;
            }
        }
        
        // Find all links that might be posts
        var allLinks = document.querySelectorAll('a');
        console.log('Found ' + allLinks.length + ' links');
        
        var postLinks = [];
        for (var i = 0; i < allLinks.length; i++) {
            var href = allLinks[i].href || '';
            if (href.indexOf('/story.php') > -1 || 
                href.indexOf('/posts/') > -1 || 
                href.indexOf('/permalink/') > -1 ||
                href.match(/[?&]fbid=/)) {
                postLinks.push({
                    href: href,
                    text: allLinks[i].textContent.substring(0, 100)
                });
            }
        }
        
        console.log('Found ' + postLinks.length + ' potential post links');
        
        // Try to find post containers by looking for common patterns
        var posts = [];
        var seenIds = {};
        
        for (var i = 0; i < postLinks.length; i++) {
            var link = postLinks[i];
            var href = link.href;
            
            // Extract post ID
            var postId = '';
            var match = href.match(/story_fbid=(\d+)/);
            if (match) {
                postId = 'story_' + match[1];
            } else {
                match = href.match(/\/posts\/([a-zA-Z0-9_-]+)/);
                if (match) postId = match[1];
                else {
                    match = href.match(/[?&]fbid=(\d+)/);
                    if (match) postId = 'fbid_' + match[1];
                    else {
                        match = href.match(/\/permalink\/(\d+)/);
                        if (match) postId = 'perm_' + match[1];
                    }
                }
            }
            
            if (!postId || seenIds[postId]) continue;
            seenIds[postId] = true;
            
            // Clean permalink
            var permalink = href;
            if (permalink.indexOf('?') > -1) {
                permalink = permalink.split('?')[0];
            }
            if (!permalink.startsWith('http')) {
                permalink = 'https://m.facebook.com' + permalink;
            }
            
            posts.push({
                id: postId,
                text: link.text.trim(),
                timestamp: Math.floor(Date.now() / 1000),
                images: [],
                video: '',
                permalink: permalink
            });
        }
        
        console.log('Extracted ' + posts.length + ' posts');
        
        return {
            displayName: displayName,
            profilePicUrl: profilePicUrl,
            userId: '',
            posts: posts
        };
        
    } catch(e) {
        console.error('Extraction error: ' + e.message);
        return {error: e.message, posts: [], debug: e.stack};
    }
})();
        """.trimIndent()
    }
}
