package com.milki.majra.ui.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.milki.majra.data.model.Platform
import com.milki.majra.data.platform.instagram.InstagramUserAgent

/**
 * Platform-agnostic WebView login screen. The [config] parameter controls
 * which platform's login page is loaded and how session success is detected.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
    config: PlatformLoginConfig,
    onSessionCaptured: (cookie: String, userAgent: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var sessionCaptured by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Sign in to ${config.displayName}",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = config.description,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onCancel) {
                Text("Back")
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = {
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.mediaPlaybackRequiresUserGesture = true
                    settings.setSupportMultipleWindows(false)

                    // Apply platform-specific user-agent if specified in config, falling back to Instagram default if needed
                    val customUa = config.userAgent ?: if (config.platform == Platform.INSTAGRAM) {
                        InstagramUserAgent.choose(settings.userAgentString)
                    } else {
                        null
                    }
                    if (customUa != null) {
                        settings.userAgentString = customUa
                    }

                    isFocusable = true
                    isFocusableInTouchMode = true
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            if (sessionCaptured) return

                            isLoading = false
                            val currentUrl = url.orEmpty()
                            android.util.Log.d("LoginScreen", "onPageFinished: $currentUrl")
                            val cookie = CookieManager.getInstance().getCookie(config.cookieDomain).orEmpty()
                            if (config.successUrlCheck(currentUrl) && config.sessionCookieCheck(cookie)) {
                                sessionCaptured = true
                                CookieManager.getInstance().flush()
                                val userAgent = config.userAgent ?: view.settings.userAgentString
                                onSessionCaptured(cookie, userAgent)
                            }
                        }

                        override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            android.util.Log.e("LoginScreen", "onReceivedError: ${error?.description}")
                        }
                    }
                    loadUrl(config.loginUrl)
                }
            },
        )
    }

    LaunchedEffect(Unit) {
        CookieManager.getInstance().flush()
    }

    DisposableEffect(Unit) {
        onDispose {
            // AndroidView handles removing the WebView from the view hierarchy.
            // We do NOT call destroy() here to avoid crashes from pending callbacks.
        }
    }
}
