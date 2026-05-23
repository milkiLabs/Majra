package com.milki.majra.navigation

import com.milki.majra.data.model.Platform

/** Main feed screen. */
data object HomeRoute

/** Instagram login WebView. */
data object LoginRoute

/** Per-source posts screen. Stacked on top of the home feed via the drawer. */
data class ProfileRoute(val platform: Platform, val accountId: String, val username: String)

/** Full-screen image viewer. */
data class ImageViewerRoute(val imageUrl: String, val caption: String? = null)
