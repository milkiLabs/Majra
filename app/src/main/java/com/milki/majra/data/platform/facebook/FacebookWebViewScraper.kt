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
        
        // Use desktop Facebook - more reliable structure
        val url = "https://www.facebook.com/$cleanUsername"
        
        Log.d(TAG, "Loading desktop Facebook profile: $url")

        syncCookies(session.cookie)

        val webView = WebView(context)
        
        // Desktop viewport size
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
                
                // Wait for content to render - desktop Facebook uses React
                Log.d(TAG, "Waiting for JavaScript to render content...")
                delay(6000)
                
                // Check if page is ready
                for (i in 0 until 10) {
                    val ready = CompletableDeferred<String>()
                    webView.evaluateJavascript("document.readyState") { v -> 
                        ready.complete(v ?: "") 
                    }
                    if (ready.await().trim('"') == "complete") break
                    delay(500)
                }
                
                // Wait for React to render posts
                Log.d(TAG, "Waiting for posts to render...")
                delay(5000)
                
                Log.d(TAG, "Extracting posts from desktop DOM")
                
                // First, save the HTML for debugging
                val htmlDeferred = CompletableDeferred<String>()
                webView.evaluateJavascript("document.documentElement.outerHTML") { html ->
                    htmlDeferred.complete(html ?: "")
                }
                val htmlContent = htmlDeferred.await()
                
                // Save to file for inspection
                try {
                    val debugFile = java.io.File("/sdcard/Download/facebook_desktop_page.html")
                    // Remove quotes and unescape
                    val cleanHtml = htmlContent.trim('"').replace("\\n", "\n").replace("\\\"", "\"")
                    debugFile.writeText(cleanHtml)
                    Log.d(TAG, "Saved desktop HTML to: ${debugFile.absolutePath}, size: ${cleanHtml.length}")
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
        
        // Desktop user agent for better page structure
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        /**
         * Extraction script for desktop Facebook (www.facebook.com)
         * 
         * Facebook embeds JSON data in script tags for their React app.
         * We extract this JSON data directly instead of parsing the rendered DOM.
         */
        private val MOBILE_EXTRACTION_SCRIPT = """
(function() {
    try {
        console.log('Starting JSON extraction from desktop Facebook...');
        
        // Extract profile info from page title
        var displayName = '';
        var title = (document.title || '').replace(/\s*[-|–|]\s*Facebook/i, '').trim();
        if (title && title.toLowerCase() !== 'facebook' && title.toLowerCase() !== 'error') {
            displayName = title;
        }
        
        console.log('Display name: ' + displayName);
        
        // Find all script tags that might contain JSON data
        var scripts = document.querySelectorAll('script');
        console.log('Found ' + scripts.length + ' script tags');
        
        var posts = [];
        var profilePicUrl = '';
        var seenIds = {};
        
        // Look for embedded JSON in script tags
        for (var i = 0; i < scripts.length; i++) {
            var scriptContent = scripts[i].textContent || scripts[i].innerHTML || '';
            
            // Skip empty or very small scripts
            if (scriptContent.length < 200) continue;
            
            // Look for story_fbid or post patterns in the script
            if (scriptContent.indexOf('story_fbid') > -1 || 
                scriptContent.indexOf('"posts"') > -1 ||
                scriptContent.indexOf('comet_sections') > -1 ||
                scriptContent.indexOf('ProfileCometTimeline') > -1) {
                
                console.log('Found potential post data in script ' + i + ', length: ' + scriptContent.length);
                
                // Try to find JSON-like structures with post IDs
                var storyMatches = scriptContent.match(/story_fbid[=:](\d+)/g);
                if (storyMatches) {
                    console.log('Found ' + storyMatches.length + ' story_fbid matches');
                    for (var j = 0; j < storyMatches.length; j++) {
                        var idMatch = storyMatches[j].match(/(\d+)/);
                        if (idMatch) {
                            var postId = 'story_' + idMatch[1];
                            if (!seenIds[postId]) {
                                seenIds[postId] = true;
                                
                                // Try to extract text from nearby context in the script
                                var text = '';
                                var textMatch = scriptContent.match(new RegExp('story_fbid[=:]' + idMatch[1] + '[^}]*"text"[^:]*:"([^"]{20,500})"', 'i'));
                                if (textMatch) {
                                    text = textMatch[1];
                                } else {
                                    // Try message field
                                    textMatch = scriptContent.match(new RegExp('story_fbid[=:]' + idMatch[1] + '[^}]*"message"[^:]*:"([^"]{20,500})"', 'i'));
                                    if (textMatch) {
                                        text = textMatch[1];
                                    }
                                }
                                
                                posts.push({
                                    id: postId,
                                    text: text,
                                    timestamp: Math.floor(Date.now() / 1000),
                                    images: [],
                                    video: '',
                                    permalink: 'https://www.facebook.com/story.php?story_fbid=' + idMatch[1]
                                });
                            }
                        }
                    }
                }
            }
        }
        
        console.log('Extracted ' + posts.length + ' posts from JSON');
        
        // Even if we found posts in JSON, try to enrich them with text from DOM
        if (posts.length > 0) {
            console.log('Enriching posts with DOM text content');
            
            for (var i = 0; i < posts.length; i++) {
                var post = posts[i];
                
                // If post has no text, try to find it in the DOM
                if (!post.text || post.text.length < 10) {
                    // Find link with this post ID
                    var postIdNum = post.id.replace('story_', '');
                    var link = document.querySelector('a[href*="story_fbid=' + postIdNum + '"]');
                    
                    if (link) {
                        var article = link.closest('[role="article"]');
                        if (article) {
                            // Find text content in the article
                            var textDivs = article.querySelectorAll('div[dir="auto"]');
                            for (var j = 0; j < textDivs.length; j++) {
                                var divText = textDivs[j].textContent || '';
                                // Skip short text and UI elements
                                if (divText.length > 30 && 
                                    divText.indexOf('Like') === -1 && 
                                    divText.indexOf('Comment') === -1 &&
                                    divText.indexOf('Share') === -1) {
                                    post.text = divText.substring(0, 500).trim();
                                    console.log('Enriched post ' + post.id + ' with text: ' + post.text.substring(0, 50));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // If no posts found in JSON, fall back to DOM link extraction
        if (posts.length === 0) {
            console.log('No posts in JSON, falling back to DOM link extraction');
            
            var allLinks = document.querySelectorAll('a[href*="story.php"], a[href*="/posts/"], a[href*="fbid="]');
            console.log('Found ' + allLinks.length + ' potential post links');
            
            for (var i = 0; i < allLinks.length; i++) {
                var href = allLinks[i].href || '';
                if (!href) continue;
                
                var postId = extractPostIdFromUrl(href);
                if (!postId || seenIds[postId]) continue;
                
                seenIds[postId] = true;
                
                var permalink = href;
                if (permalink.indexOf('?') > -1 && permalink.indexOf('story.php') === -1) {
                    permalink = permalink.split('?')[0];
                }
                if (!permalink.startsWith('http')) {
                    permalink = 'https://www.facebook.com' + permalink;
                }
                
                // Try to find nearby text content - look for article container
                var text = '';
                var article = allLinks[i].closest('[role="article"]');
                if (article) {
                    // Find text content in the article, excluding UI elements
                    var textDivs = article.querySelectorAll('div[dir="auto"]');
                    for (var j = 0; j < textDivs.length; j++) {
                        var divText = textDivs[j].textContent || '';
                        // Skip short text (likely UI elements)
                        if (divText.length > 30 && 
                            divText.indexOf('Like') === -1 && 
                            divText.indexOf('Comment') === -1 &&
                            divText.indexOf('Share') === -1) {
                            text = divText.substring(0, 500).trim();
                            break;
                        }
                    }
                }
                
                posts.push({
                    id: postId,
                    text: text,
                    timestamp: Math.floor(Date.now() / 1000),
                    images: [],
                    video: '',
                    permalink: permalink
                });
            }
        }
        
        // Try to find profile picture
        var imgs = document.querySelectorAll('img');
        for (var i = 0; i < imgs.length; i++) {
            var src = imgs[i].src || '';
            if (src.indexOf('scontent') > -1) {
                profilePicUrl = src;
                break;
            }
        }
        
        console.log('Extracted ' + posts.length + ' posts total');
        
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
    
    function extractPostIdFromUrl(url) {
        var match = url.match(/story_fbid=(\d+)/);
        if (match) return 'story_' + match[1];
        
        match = url.match(/\/posts\/([a-zA-Z0-9_-]+)/);
        if (match) return match[1];
        
        match = url.match(/[?&]fbid=(\d+)/);
        if (match) return 'fbid_' + match[1];
        
        match = url.match(/\/permalink\/(\d+)/);
        if (match) return 'perm_' + match[1];
        
        return null;
    }
})();
        """.trimIndent()
    }
}
