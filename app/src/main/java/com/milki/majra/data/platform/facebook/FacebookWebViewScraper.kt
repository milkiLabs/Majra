package com.milki.majra.data.platform.facebook

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.milki.majra.data.local.SessionStore
import com.milki.majra.data.model.Platform
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class FacebookWebViewScraper(
    private val context: Context,
    private val sessionStore: SessionStore,
) {
    suspend fun scrapeProfile(username: String, scrollCount: Int = 1): String = withContext(Dispatchers.Main) {
        val session = sessionStore.current(Platform.FACEBOOK)
        val url = "https://m.facebook.com/${username.trimUsername()}"
        
        // Sync CookieManager cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        session.cookie.split(";").forEach { cookiePart ->
            if (cookiePart.isNotBlank()) {
                cookieManager.setCookie("https://.facebook.com/", cookiePart.trim())
            }
        }
        cookieManager.flush()
        
        val deferredResult = CompletableDeferred<String>()
        val webView = WebView(context)
        
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = MOBILE_USER_AGENT
        }
        
        class JsInterface {
            @android.webkit.JavascriptInterface
            fun processResult(jsonStr: String) {
                deferredResult.complete(jsonStr)
            }

            @android.webkit.JavascriptInterface
            fun onError(error: String) {
                deferredResult.completeExceptionally(Exception(error))
            }
        }
        
        webView.addJavascriptInterface(JsInterface(), "HTMLBridge")
        
        val pageLoadedDeferred = CompletableDeferred<Unit>()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                pageLoadedDeferred.complete(Unit)
            }
        }
        
        webView.loadUrl(url)
        
        val scrollAndExtractJob = launch {
            try {
                // Wait for the page to finish loading initially
                pageLoadedDeferred.await()
                
                // Perform scrolling to load dynamic content
                for (i in 0 until scrollCount) {
                    webView.evaluateJavascript("window.scrollTo(0, document.body.scrollHeight);", null)
                    delay(1500)
                }
                
                // Inject the DOM extraction script
                webView.evaluateJavascript(EXTRACTION_SCRIPT, null)
            } catch (e: Exception) {
                deferredResult.completeExceptionally(e)
            }
        }
        
        try {
            withTimeout(30000) { // 30 seconds max timeout
                deferredResult.await()
            }
        } finally {
            scrollAndExtractJob.cancel()
            webView.stopLoading()
            webView.destroy()
        }
    }

    private fun String.trimUsername(): String = trim().removePrefix("@").trim('/').lowercase()

    companion object {
        private const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

        private val EXTRACTION_SCRIPT = """
            (function() {
                try {
                    let displayName = document.title ? document.title.replace(/\s*\|\s*Facebook/i, '').replace(/\s*-\s*Facebook/i, '').trim() : '';
                    if (!displayName || displayName.toLowerCase() === 'facebook' || displayName.toLowerCase() === 'error') {
                        const h1 = document.querySelector('h1, h2, strong');
                        if (h1) displayName = h1.textContent.trim();
                    }
                    
                    let profilePicUrl = '';
                    const img = document.querySelector('img[alt*="profile picture"]') || document.querySelector('img[alt*="profile pic"]');
                    if (img) profilePicUrl = img.src;
                    if (!profilePicUrl) {
                        const imgs = document.querySelectorAll('img');
                        for (const image of imgs) {
                            const src = image.src;
                            if (src && (src.includes('fbcdn') || src.includes('scontent')) && (src.includes('/t39.30808-1/') || src.includes('/100x100/'))) {
                                profilePicUrl = src;
                                break;
                            }
                        }
                    }
                    
                    let userId = null;
                    const ownerIdEl = document.querySelector('[data-owner-id]');
                    if (ownerIdEl) userId = ownerIdEl.getAttribute('data-owner-id');
                    if (!userId) {
                        const links = document.querySelectorAll('a');
                        for (const link of links) {
                            const href = link.href || '';
                            const match = href.match(/[?&]id=(\d+)/) || href.match(/\/messages\/thread\/(\d+)/) || href.match(/\/composer\/\?id=(\d+)/);
                            if (match) {
                                userId = match[1];
                                break;
                            }
                        }
                    }
                    if (!userId) {
                        const bodyHtml = document.body.innerHTML;
                        const match = bodyHtml.match(/\"id\"\:\"(\d+)\"/i) || bodyHtml.match(/\"owner_id\"\:\"(\d+)\"/i) || bodyHtml.match(/\"profile_owner\"\:\{\"id\"\:\"(\d+)\"/i);
                        if (match) userId = match[1];
                    }

                    const posts = [];
                    const articleEls = document.querySelectorAll('article, [role="article"]');
                    articleEls.forEach(el => {
                        let postId = null;
                        let permalink = null;
                        const links = el.querySelectorAll('a');
                        for (const link of links) {
                            const href = link.href || '';
                            if (!href) continue;
                            
                            const postsMatch = href.match(/\/posts\/(\d+)/) || href.match(/\/story\.php\?story_fbid=(\d+)/);
                            if (postsMatch) { 
                                postId = postsMatch[1]; 
                                permalink = href.startsWith('http') ? href : 'https://www.facebook.com' + href; 
                                break; 
                            }
                            
                            const fbidMatch = href.match(/fbid=(\d+)/) || href.match(/story_fbid=(\d+)/);
                            if (fbidMatch) { 
                                postId = fbidMatch[1]; 
                                permalink = href.startsWith('http') ? href : 'https://www.facebook.com' + href; 
                                break; 
                            }
                        }
                        
                        if (!postId) return;

                        let text = '';
                        const textEl = el.querySelector('[data-testid="post_message"]') || 
                                       el.querySelector('[data-sigil*="expose"]') || 
                                       el.querySelector('.story_body_container') ||
                                       el.querySelector('.msg') ||
                                       el.querySelector('p');
                        if (textEl) {
                            text = textEl.textContent.trim();
                        } else {
                            text = el.innerText.trim();
                        }

                        const images = [];
                        const imgEls = el.querySelectorAll('img');
                        imgEls.forEach(img => {
                            const src = img.src;
                            if (!src) return;
                            if (src.includes('/emoji/') || src.includes('/rsrc.php/') || src.includes('static.xx.fbcdn') || src.includes('avatar')) return;
                            if (src.includes('fbcdn') || src.includes('scontent') || src.endsWith('.jpg') || src.endsWith('.png')) {
                                if (!images.includes(src)) images.push(src);
                            }
                        });

                        let videoUrl = null;
                        const videoEl = el.querySelector('video');
                        if (videoEl && videoEl.src) {
                            videoUrl = videoEl.src;
                        }

                        let timestamp = 0;
                        const abbrEl = el.querySelector('abbr');
                        if (abbrEl) {
                            const utime = abbrEl.getAttribute('data-utime') || abbrEl.getAttribute('data-shorten');
                            if (utime) timestamp = parseInt(utime);
                        }
                        if (!timestamp) {
                            const timeEl = el.querySelector('time');
                            if (timeEl && timeEl.getAttribute('datetime')) {
                                timestamp = Math.floor(Date.parse(timeEl.getAttribute('datetime')) / 1000);
                            }
                        }
                        if (!timestamp) {
                            timestamp = Math.floor(Date.now() / 1000);
                        }

                        posts.push({
                            id: postId,
                            text: text,
                            timestamp: timestamp,
                            images: images,
                            video: videoUrl,
                            permalink: permalink
                        });
                    });

                    const result = {
                        displayName: displayName,
                        profilePicUrl: profilePicUrl,
                        userId: userId,
                        posts: posts
                    };

                    window.HTMLBridge.processResult(JSON.stringify(result));
                } catch(e) {
                    window.HTMLBridge.onError(e.message);
                }
            })();
        """.trimIndent()
    }
}
