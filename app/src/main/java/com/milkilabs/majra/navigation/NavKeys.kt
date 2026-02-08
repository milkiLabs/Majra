package com.milkilabs.majra.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Nav3 keys are the only thing stored in the back stack. They must be serializable.
sealed interface MajraNavKey : NavKey

// Top-level destinations are used as the root of each tab back stack.
@Serializable
data object Feed : MajraNavKey

@Serializable
data object Saved : MajraNavKey

@Serializable
data object Settings : MajraNavKey

// Detail keys carry enough data to render the screen without extra lookups.
@Serializable
data class ContentDetail(
    val contentId: String,
    val sourceId: String,
    val sourceType: String,
    val sourceName: String,
) : MajraNavKey

