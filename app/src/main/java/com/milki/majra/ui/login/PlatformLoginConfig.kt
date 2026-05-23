package com.milki.majra.ui.login

import com.milki.majra.data.model.Platform

/**
 * Describes a platform's WebView login flow. Each supported platform provides
 * its own config so that [LoginScreen] can remain platform-agnostic.
 *
 * To add a new platform login, create a new factory method here and register
 * a route for it in the navigation graph.
 */
data class PlatformLoginConfig(
    val platform: Platform,
    val loginUrl: String,
    val displayName: String,
    val description: String,
    /** Returns true when the WebView URL indicates a successful login. */
    val successUrlCheck: (String) -> Boolean,
    /** The domain used to read cookies from CookieManager. */
    val cookieDomain: String,
    /** Returns true when the cookie string contains a valid session marker. */
    val sessionCookieCheck: (String) -> Boolean,
    /** Optional custom user-agent. Null = use the WebView default. */
    val userAgent: String? = null,
) {
    companion object {
        fun instagram(): PlatformLoginConfig = PlatformLoginConfig(
            platform = Platform.INSTAGRAM,
            loginUrl = "https://www.instagram.com/accounts/login/",
            displayName = "Instagram",
            description = "Majra stores only your browser cookie and WebView user-agent locally so background sync can fetch profile pages without keeping a browser open.",
            successUrlCheck = { url ->
                val clean = url.substringBefore('?').trimEnd('/') + "/"
                clean == "https://www.instagram.com/"
            },
            cookieDomain = "https://www.instagram.com/",
            sessionCookieCheck = { cookie -> cookie.contains("sessionid=") },
            // InstagramUserAgent will be applied in LoginScreen for Instagram
        )

        fun facebook(): PlatformLoginConfig = PlatformLoginConfig(
            platform = Platform.FACEBOOK,
            loginUrl = "https://www.facebook.com/login/",
            displayName = "Facebook",
            description = "Majra stores only your browser cookie locally so it can fetch profile pages from Facebook without keeping a browser open.",
            successUrlCheck = { url ->
                val clean = url.substringBefore('?').trimEnd('/')
                clean == "https://m.facebook.com" ||
                    clean.startsWith("https://m.facebook.com/home") ||
                    clean == "https://m.facebook.com/" ||
                    clean == "https://www.facebook.com" ||
                    clean == "https://www.facebook.com/" ||
                    clean.startsWith("https://www.facebook.com/home")
            },
            cookieDomain = "https://.facebook.com/",
            sessionCookieCheck = { cookie -> cookie.contains("c_user=") },
            userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        )

        fun forPlatform(platform: Platform): PlatformLoginConfig = when (platform) {
            Platform.INSTAGRAM -> instagram()
            Platform.FACEBOOK -> facebook()
            else -> throw IllegalArgumentException("${platform.displayName} login is not supported yet.")
        }
    }
}
