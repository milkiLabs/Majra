package com.milki.majra.data.model

data class PostMediaItem(
    val imageUrl: String,
    val videoUrl: String? = null,
    val mediaType: String = MEDIA_TYPE_IMAGE,
) {
    val isVideo: Boolean
        get() = mediaType == MEDIA_TYPE_VIDEO && !videoUrl.isNullOrBlank()

    companion object {
        const val MEDIA_TYPE_IMAGE = "image"
        const val MEDIA_TYPE_VIDEO = "video"
    }
}

data class SocialPost(
    val platform: Platform = Platform.INSTAGRAM,
    val id: String,
    val platformPostId: String,
    val accountId: String,
    val username: String,
    val mediaType: String = MEDIA_TYPE_IMAGE,
    val caption: String,
    val timestampSeconds: Long,
    val permalink: String,
    val mediaItems: List<PostMediaItem> = emptyList(),
) {
    val shortcode: String
        get() = platformPostId

    val imageUrl: String
        get() = mediaItems.firstOrNull()?.imageUrl ?: ""

    val videoUrl: String?
        get() = mediaItems.firstOrNull()?.videoUrl

    val isVideo: Boolean
        get() = mediaType == MEDIA_TYPE_VIDEO || (mediaType == MEDIA_TYPE_CAROUSEL && mediaItems.firstOrNull()?.isVideo == true)

    companion object {
        const val MEDIA_TYPE_IMAGE = "image"
        const val MEDIA_TYPE_VIDEO = "video"
        const val MEDIA_TYPE_CAROUSEL = "carousel"
    }
}

