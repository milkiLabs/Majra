package com.milki.majra.data.model

data class SocialProfile(
    val platform: Platform = Platform.INSTAGRAM,
    val username: String,
    val accountId: String = username,
    val displayName: String? = null,
    val profilePicUrl: String? = null,
    val userId: String? = null,
    val nextPageToken: String? = null,
    val hasMorePosts: Boolean = false,
    val lastSyncedAtMillis: Long = 0L,
) {
    val nextMaxId: String?
        get() = nextPageToken
}

