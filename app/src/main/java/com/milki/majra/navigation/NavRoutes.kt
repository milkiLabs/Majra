package com.milki.majra.navigation

import com.milki.majra.data.model.Platform

/** Main feed screen. */
data object HomeRoute

/** Platform login WebView. */
data class LoginRoute(val platform: Platform)

/** Per-source posts screen. Stacked on top of the home feed via the drawer. */
data class ProfileRoute(val platform: Platform, val accountId: String, val username: String)

/** Full-screen image viewer. */
data class ImageViewerRoute(val imageUrl: String, val caption: String? = null)
