package com.milki.majra.data.platform.x

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.milki.majra.BuildConfig
import com.milki.majra.data.local.SessionStore
import com.milki.majra.data.model.Platform
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

class XWebViewScraper(
    private val context: Context,
    private val sessionStore: SessionStore,
) {
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun scrapeProfile(username: String, scrollCount: Int): String = withContext(Dispatchers.Main) {
        val session = sessionStore.current(Platform.X)
        if (!session.isAuthenticated) {
            error("X session is missing. Please sign in first.")
        }

        val cleanUsername = username.trimUsername()
        val url = "https://x.com/$cleanUsername"
        if (BuildConfig.DEBUG) Log.d(TAG, "Loading X profile: $url")
        syncCookies(session.cookie)

        val webView = WebView(context)
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(1366, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY),
        )
        webView.layout(0, 0, 1366, 900)

        @Suppress("DEPRECATION")
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            userAgentString = DESKTOP_USER_AGENT
            loadWithOverviewMode = true
            useWideViewPort = true
            mediaPlaybackRequiresUserGesture = true
            blockNetworkImage = false
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.message()?.let { message ->
                    if (BuildConfig.DEBUG && message.contains("[MajraX]")) Log.d(TAG, message)
                }
                return true
            }
        }

        val pageLoaded = CompletableDeferred<Unit>()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                if (BuildConfig.DEBUG) Log.d(TAG, "X page finished: $finishedUrl")
                view?.evaluateJavascript(INTERCEPT_SCRIPT, null)
                if (!pageLoaded.isCompleted) pageLoaded.complete(Unit)
            }
        }

        webView.loadUrl(url)

        try {
            withTimeout(75_000) {
                pageLoaded.await()
                delay(4_000)
                for (i in 1..scrollCount) {
                    webView.evaluateJavascript("window.scrollTo(0, ${i * 900});", null)
                    delay(1_100)
                }
                delay(3_000)

                val deferred = CompletableDeferred<String>()
                webView.evaluateJavascript(extractScript(cleanUsername)) { value ->
                    val result = value ?: """{"posts":[]}"""
                    saveDebugPayload(cleanUsername, result)
                    deferred.complete(result)
                }
                deferred.await()
            }
        } catch (error: Exception) {
            Log.e(TAG, "X scraping failed: ${error.message}", error)
            """{"error":"${error.message?.jsonEscape() ?: "Unknown X scraping failure"}","posts":[]}"""
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
        val domains = listOf("https://x.com", "https://twitter.com", "https://mobile.x.com")
        parts.forEach { part ->
            domains.forEach { domain -> cookieManager.setCookie(domain, part) }
        }
        cookieManager.flush()
        if (BuildConfig.DEBUG) Log.d(TAG, "Synced ${parts.size} X cookie parts")
    }

    private fun saveDebugPayload(username: String, payload: String) {
        if (!BuildConfig.DEBUG) return
        runCatching {
            val cleanPayload = payload.trim('"')
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .replace("\\\\", "\\")
            val dir = File(context.getExternalFilesDir(null), "debug")
            dir.mkdirs()
            File(dir, "x_${username}_captured.json").writeText(cleanPayload)
            Log.d(TAG, "Saved X debug payload to ${dir.absolutePath}")
        }.onFailure { Log.w(TAG, "Could not save X debug payload: ${it.message}") }
    }

    private fun extractScript(username: String): String = EXTRACT_SCRIPT.replace("__MAJRA_USERNAME__", username.jsonEscape())

    private fun String.trimUsername(): String = trim().removePrefix("@").trim('/').lowercase()

    private fun String.jsonEscape(): String = replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    private companion object {
        const val TAG = "XWebViewScraper"
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

        val INTERCEPT_SCRIPT = """
(function() {
    if (window.__majraXInstalled) return;
    window.__majraXInstalled = true;
    window.__majraXCaptured = [];
    console.log('[MajraX] installing network interceptor');

    function store(url, text) {
        if (!url || (!url.includes('/graphql/') && !url.includes('/i/api/'))) return;
        try {
            window.__majraXCaptured.push({ url: String(url), text: String(text || ''), timestamp: Date.now() });
            console.log('[MajraX] captured ' + window.__majraXCaptured.length + ' ' + String(url).substring(0, 120));
        } catch (error) {
            console.log('[MajraX] capture failed ' + error.message);
        }
    }

    const originalFetch = window.fetch;
    window.fetch = function() {
        const args = arguments;
        const url = typeof args[0] === 'string' ? args[0] : (args[0] && args[0].url);
        return originalFetch.apply(this, args).then(function(response) {
            if (url) response.clone().text().then(function(text) { store(url, text); }).catch(function(){});
            return response;
        });
    };

    const originalOpen = XMLHttpRequest.prototype.open;
    const originalSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.open = function(method, url) {
        this.__majraXUrl = url;
        return originalOpen.apply(this, arguments);
    };
    XMLHttpRequest.prototype.send = function() {
        this.addEventListener('load', function() { store(this.__majraXUrl, this.responseText); });
        return originalSend.apply(this, arguments);
    };
})();
        """.trimIndent()

        val EXTRACT_SCRIPT = """
(function() {
    try {
        const targetUsername = "__MAJRA_USERNAME__";
        const posts = [];
        const seen = {};
        let displayName = '';
        let profilePicUrl = '';

        function addPost(post) {
            if (!post || !post.id || seen[post.id]) return;
            post.images = unique(post.images || []).filter(isUsefulMedia);
            post.videos = unique(post.videos || []).filter(isUsefulMedia);
            if (!post.text && post.images.length === 0 && post.videos.length === 0) return;
            seen[post.id] = true;
            posts.push(post);
        }

        function unique(values) {
            const out = [];
            const map = {};
            values.forEach(function(value) {
                if (value && !map[value]) {
                    map[value] = true;
                    out.push(value);
                }
            });
            return out;
        }

        function isUsefulMedia(url) {
            return url && (url.indexOf('twimg.com/media') >= 0 || url.indexOf('video.twimg.com') >= 0 || url.indexOf('pbs.twimg.com') >= 0);
        }

        function textOf(value) {
            if (!value) return '';
            if (typeof value === 'string') return value;
            if (value.full_text) return value.full_text;
            if (value.text) return value.text;
            if (value.legacy) return textOf(value.legacy);
            return '';
        }

        function findPermalink(article, id) {
            const anchors = Array.from(article.querySelectorAll('a[href*="/status/"]'));
            const anchor = anchors.find(function(a) { return a.href.indexOf('/' + targetUsername + '/status/') >= 0; }) || anchors[0];
            return anchor ? anchor.href : 'https://x.com/' + targetUsername + '/status/' + id;
        }

        Array.from(document.querySelectorAll('[data-testid="UserName"]')).forEach(function(node) {
            if (!displayName && node.innerText && node.innerText.toLowerCase().indexOf('@' + targetUsername.toLowerCase()) >= 0) {
                displayName = node.innerText.split('\n')[0].trim();
            }
        });
        const avatar = document.querySelector('img[src*="profile_images"]');
        if (avatar) profilePicUrl = avatar.src;

        Array.from(document.querySelectorAll('article[data-testid="tweet"]')).forEach(function(article) {
            const statusLink = Array.from(article.querySelectorAll('a[href*="/status/"]')).find(function(a) {
                return /\/status\/[0-9]+/.test(a.getAttribute('href') || '');
            });
            if (!statusLink) return;
            const match = statusLink.getAttribute('href').match(/\/status\/([0-9]+)/);
            if (!match) return;
            const id = match[1];
            const textNode = article.querySelector('[data-testid="tweetText"]');
            const timeNode = article.querySelector('time');
            const images = Array.from(article.querySelectorAll('img[src*="twimg.com/media"], img[src*="pbs.twimg.com/media"]')).map(function(img) { return img.src; });
            const videos = Array.from(article.querySelectorAll('video')).map(function(video) { return video.currentSrc || video.src; });
            addPost({
                id: id,
                text: textNode ? textNode.innerText : '',
                timestamp: timeNode && timeNode.dateTime ? Math.floor(Date.parse(timeNode.dateTime) / 1000) : Math.floor(Date.now() / 1000),
                permalink: findPermalink(article, id),
                images: images,
                videos: videos
            });
        });

        function visit(value) {
            if (!value || typeof value !== 'object') return;
            if (Array.isArray(value)) {
                value.forEach(visit);
                return;
            }

            const legacy = value.legacy || value.tweet_results?.result?.legacy || value.tweet?.legacy;
            const restId = value.rest_id || value.tweet_results?.result?.rest_id || value.tweet?.rest_id;
            if (legacy && restId && (legacy.user_id_str || legacy.id_str || legacy.full_text)) {
                const entities = legacy.extended_entities || legacy.entities || {};
                const media = entities.media || [];
                const images = [];
                const videos = [];
                media.forEach(function(item) {
                    if (item.media_url_https) images.push(item.media_url_https);
                    const variants = item.video_info && item.video_info.variants ? item.video_info.variants : [];
                    let best = '';
                    let bestRate = -1;
                    variants.forEach(function(variant) {
                        if (variant.content_type === 'video/mp4' && variant.url && (variant.bitrate || 0) > bestRate) {
                            best = variant.url;
                            bestRate = variant.bitrate || 0;
                        }
                    });
                    if (best) videos.push(best);
                });
                addPost({
                    id: legacy.id_str || restId,
                    text: textOf(legacy),
                    timestamp: legacy.created_at ? Math.floor(Date.parse(legacy.created_at) / 1000) : Math.floor(Date.now() / 1000),
                    permalink: 'https://x.com/' + targetUsername + '/status/' + (legacy.id_str || restId),
                    images: images,
                    videos: videos
                });
            }

            Object.keys(value).forEach(function(key) { visit(value[key]); });
        }

        (window.__majraXCaptured || []).forEach(function(item) {
            try {
                const json = JSON.parse(item.text);
                visit(json);
            } catch (error) {
                item.text.split('\n').forEach(function(line) {
                    try { visit(JSON.parse(line)); } catch (ignored) {}
                });
            }
        });

        return JSON.stringify({
            displayName: displayName,
            profilePicUrl: profilePicUrl,
            posts: posts,
            debug: {
                capturedCount: (window.__majraXCaptured || []).length,
                domPosts: document.querySelectorAll('article[data-testid="tweet"]').length,
                extractedPosts: posts.length
            }
        });
    } catch (error) {
        return JSON.stringify({ error: error.message, posts: [], debug: error.stack });
    }
})();
        """.trimIndent()
    }
}
