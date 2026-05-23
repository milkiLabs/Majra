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

class FacebookWebViewScraper(
    private val context: Context,
    private val sessionStore: SessionStore,
) {
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun scrapeProfile(username: String, scrollCount: Int = 3): String = withContext(Dispatchers.Main) {
        val session = sessionStore.current(Platform.FACEBOOK)
        val cleanUsername = username.trimUsername()
        val url = "https://www.facebook.com/$cleanUsername?sk=timeline"

        syncCookies(session.cookie)

        val webView = WebView(context)
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY),
        )
        webView.layout(0, 0, 1080, 1920)

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

        val pageLoaded = CompletableDeferred<Unit>()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d(TAG, "onPageFinished: $url")
                if (!pageLoaded.isCompleted) {
                    pageLoaded.complete(Unit)
                }
            }
        }

        webView.loadUrl(url)

        try {
            withTimeout(90000) {
                pageLoaded.await()

                for (i in 0 until 5) {
                    val ready = CompletableDeferred<String>()
                    webView.evaluateJavascript("document.readyState") { v -> ready.complete(v ?: "") }
                    if (ready.await().trim('"') == "complete") break
                    delay(500)
                }

                delay(5000)

                val allScrolls = scrollCount * 4
                for (i in 0 until allScrolls) {
                    webView.evaluateJavascript(SCROLL_SCRIPT, null)
                    delay(2000)
                }

                delay(5000)

                val deferred = CompletableDeferred<String>()
                webView.evaluateJavascript(EXTRACTION_SCRIPT) { value ->
                    deferred.complete(value ?: """{"posts":[]}""")
                }
                deferred.await()
            }
        } catch (e: Exception) {
            """{"error":"${e.message?.replace("\"", "\\\"")?.replace("\n", "\\n") ?: "Unknown"}","posts":[]}"""
        } finally {
            webView.stopLoading()
            webView.destroy()
        }
    }

    private fun syncCookies(cookie: String) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        val parts = cookie.split(";").map { it.trim() }.filter { it.isNotBlank() }
        val domains = listOf("https://facebook.com", "https://www.facebook.com")
        for (part in parts) {
            for (domain in domains) {
                cookieManager.setCookie(domain, part)
            }
        }
        cookieManager.flush()
    }

    private fun String.trimUsername(): String = trim().removePrefix("@").trim('/').lowercase()

    companion object {
        private const val TAG = "FBWebViewScraper"
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

        private const val SCROLL_SCRIPT = "window.scrollBy(0, window.innerHeight * 0.8);"

        private val EXTRACTION_SCRIPT = """
(function() {
    try {
        var displayName = '';
        var title = (document.title || '').replace(/\s*[-|–]\s*Facebook/i, '').trim();
        if (title && title.toLowerCase() !== 'facebook' && title.toLowerCase() !== 'error') displayName = title;
        if (!displayName) {
            var h1 = document.querySelector('h1');
            if (h1) displayName = h1.textContent.trim();
        }

        var profilePicUrl = '';
        var picEl = document.querySelector('img[alt*="profile" i]');
        if (!picEl) picEl = document.querySelector('image[href*="scontent"]');
        if (picEl) profilePicUrl = picEl.src || picEl.getAttribute('href') || '';
        if (!profilePicUrl) {
            var imgs = document.querySelectorAll('img');
            for (var i = 0; i < imgs.length; i++) {
                var s = imgs[i].src || '';
                if (s.indexOf('scontent') > -1 && s.indexOf('/v/') > -1) { profilePicUrl = s; break; }
            }
        }

        var containers = document.querySelectorAll('[role="article"]');
        var toProcess = containers.length > 0 ? containers : [];

        var posts = [];
        for (var i = 0; i < toProcess.length; i++) {
            var post = extractPost(toProcess[i]);
            if (post) posts.push(post);
        }

        var seen = {};
        var unique = [];
        for (var i = 0; i < posts.length; i++) {
            if (!seen[posts[i].id]) { seen[posts[i].id] = true; unique.push(posts[i]); }
        }

        return { displayName: displayName, profilePicUrl: profilePicUrl, userId: '', posts: unique };

    } catch(e) {
        return {error: e.message, posts: []};
    }

    function extractPost(el) {
        var postId = '';
        var permalink = '';

        var links = el.tagName === 'A' ? [el] : el.querySelectorAll('a');
        for (var i = 0; i < links.length; i++) {
            var h = links[i].href || '';
            if (!h) continue;

            var m = h.match(/\/story\.php\?.*?story_fbid=(\d+)/i);
            if (m) { postId = 'sfb_' + m[1]; permalink = h.indexOf('http') === 0 ? h : 'https://www.facebook.com' + h; break; }

            m = h.match(/\/posts\/(\d+)/);
            if (m) { postId = m[1]; permalink = h.indexOf('http') === 0 ? h : 'https://www.facebook.com' + h; break; }

            m = h.match(/\/posts\/(pfbid[a-zA-Z0-9]+)/);
            if (m) { postId = m[1]; permalink = h.indexOf('http') === 0 ? h : 'https://www.facebook.com' + h; break; }

            m = h.match(/fbid=(\d+)/);
            if (m) { postId = 'fbid_' + m[1]; permalink = h.indexOf('http') === 0 ? h : 'https://www.facebook.com' + h; break; }
        }

        if (!postId) return null;

        if (permalink) {
            var qIdx = permalink.indexOf('?');
            if (qIdx > -1) permalink = permalink.substring(0, qIdx);
        }

        var text = '';
        var textEl = el.querySelector('[data-ad-comet-preview="message"], [data-testid="post_message"], .userContent, div[dir="auto"]');
        if (textEl) text = textEl.textContent.trim();
        if (!text) {
            var p = el.querySelector('p');
            if (p) text = p.textContent.trim();
        }
        if (!text) {
            text = (el.innerText || '').replace(/\s+/g, ' ').substring(0, 2000).trim();
        }

        var images = [];
        var imgEls = el.querySelectorAll('img');
        for (var i = 0; i < imgEls.length; i++) {
            var s = imgEls[i].src || '';
            if (!s || s.indexOf('emoji') > -1 || s.indexOf('rsrc') > -1 || s.indexOf('static.xx.fbcdn') > -1 || s.indexOf('_nc_ads') > -1 || s.indexOf('pixel') > -1) continue;
            if (s.indexOf('scontent') > -1 || s.indexOf('fbcdn') > -1 || s.match(/\.(jpg|jpeg|png|gif|webp)(\?|$)/i)) {
                if (images.indexOf(s) === -1) images.push(s);
            }
        }

        var videoUrl = '';
        var videoEl = el.querySelector('video');
        if (videoEl && videoEl.src) videoUrl = videoEl.src;
        if (!videoUrl) {
            var v = el.querySelector('a[href*="/videos/"]');
            if (v) videoUrl = v.href;
        }

        var timestamp = 0;
        var abbr = el.querySelector('abbr[data-utime]');
        if (abbr) { var u = abbr.getAttribute('data-utime'); if (u) timestamp = parseInt(u) * 1000; }
        if (!timestamp) {
            var t = el.querySelector('time');
            if (t && t.getAttribute('datetime')) timestamp = Date.parse(t.getAttribute('datetime'));
        }
        if (!timestamp) {
            var span = el.querySelector('[data-utime]');
            if (span) { var u = span.getAttribute('data-utime'); if (u) timestamp = parseInt(u) * 1000; }
        }
        if (!timestamp) timestamp = Date.now();

        return { id: postId, text: text, timestamp: Math.floor(timestamp / 1000), images: images, video: videoUrl, permalink: permalink };
    }
})();
        """.trimIndent()
    }
}
