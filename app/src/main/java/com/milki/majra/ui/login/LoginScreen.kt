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
import com.milki.majra.data.network.InstagramUserAgent

private const val LOGIN_URL = "https://www.instagram.com/accounts/login/"
private const val INSTAGRAM_HOME_URL = "https://www.instagram.com/"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
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
                text = "Sign in once",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Majra stores only your browser cookie and WebView user-agent locally so background sync can fetch profile pages without keeping a browser open.",
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
                    settings.userAgentString = InstagramUserAgent.choose(settings.userAgentString)
                    settings.setSupportMultipleWindows(false)
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
                            val cookie = CookieManager.getInstance().getCookie(INSTAGRAM_HOME_URL).orEmpty()
                            if (currentUrl.isInstagramHome() && cookie.contains("sessionid=")) {
                                sessionCaptured = true
                                CookieManager.getInstance().flush()
                                onSessionCaptured(cookie, InstagramUserAgent.choose(view.settings.userAgentString))
                            }
                        }

                        override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            android.util.Log.e("LoginScreen", "onReceivedError: ${error?.description}")
                        }
                    }
                    loadUrl(LOGIN_URL)
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

private fun String.isInstagramHome(): Boolean {
    val clean = substringBefore('?').trimEnd('/') + "/"
    return clean == INSTAGRAM_HOME_URL
}
