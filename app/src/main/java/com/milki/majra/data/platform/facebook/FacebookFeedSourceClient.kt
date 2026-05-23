package com.milki.majra.data.platform.facebook

import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.PostMediaItem
import com.milki.majra.data.model.SocialPost
import com.milki.majra.data.model.SocialProfile
import com.milki.majra.data.repository.FeedSourceClient
import com.milki.majra.data.repository.SourceSyncPage
import org.json.JSONObject

class FacebookFeedSourceClient(
    private val scraper: FacebookWebViewScraper,
) : FeedSourceClient {
    override val platform: Platform = Platform.FACEBOOK

    override suspend fun syncProfile(sourceId: String): SourceSyncPage {
        val username = sourceId.trimUsername()
        val jsonStr = scraper.scrapeProfile(username, scrollCount = 3)
        return parseSyncPage(username, jsonStr, null)
    }

    override suspend fun loadOlderPosts(profile: SocialProfile): SourceSyncPage {
        val nextScroll = profile.nextPageToken?.removePrefix("scroll_")?.toIntOrNull() ?: 4
        val jsonStr = scraper.scrapeProfile(profile.username, scrollCount = nextScroll)
        return parseSyncPage(profile.username, jsonStr, nextScroll)
    }

    private fun parseSyncPage(username: String, jsonStr: String, currentScroll: Int?): SourceSyncPage {
        val json = JSONObject(jsonStr)
        val displayName = json.optString("displayName").takeIf { it.isNotBlank() } ?: username
        val profilePicUrl = json.optString("profilePicUrl").takeIf { it.isNotBlank() }
        val userId = json.optString("userId").takeIf { it.isNotBlank() } ?: username

        val account = SocialProfile(
            platform = Platform.FACEBOOK,
            username = username,
            accountId = userId,
            displayName = displayName,
            profilePicUrl = profilePicUrl,
        )

        val posts = mutableListOf<SocialPost>()
        val postsArray = json.getJSONArray("posts")
        for (i in 0 until postsArray.length()) {
            val postObj = postsArray.getJSONObject(i)
            val postId = postObj.getString("id")
            val text = postObj.optString("text")
            val timestamp = postObj.optLong("timestamp")
            val permalink = postObj.optString("permalink")

            val images = mutableListOf<String>()
            val imagesArray = postObj.getJSONArray("images")
            for (j in 0 until imagesArray.length()) {
                images.add(imagesArray.getString(j))
            }
            val videoUrl = postObj.optString("video").takeIf { it.isNotBlank() && it != "null" }

            val mediaItems = mutableListOf<PostMediaItem>()
            if (videoUrl != null) {
                mediaItems.add(
                    PostMediaItem(
                        imageUrl = images.firstOrNull() ?: "",
                        videoUrl = videoUrl,
                        mediaType = PostMediaItem.MEDIA_TYPE_VIDEO,
                    )
                )
            } else {
                images.forEach { url ->
                    mediaItems.add(
                        PostMediaItem(
                            imageUrl = url,
                            videoUrl = null,
                            mediaType = PostMediaItem.MEDIA_TYPE_IMAGE,
                        )
                    )
                }
            }

            val mediaType = when {
                videoUrl != null -> SocialPost.MEDIA_TYPE_VIDEO
                mediaItems.size > 1 -> SocialPost.MEDIA_TYPE_CAROUSEL
                else -> SocialPost.MEDIA_TYPE_IMAGE
            }

            posts.add(
                SocialPost(
                    platform = Platform.FACEBOOK,
                    id = postId,
                    platformPostId = postId,
                    accountId = userId,
                    username = username,
                    mediaType = mediaType,
                    caption = text,
                    timestampSeconds = timestamp,
                    permalink = permalink,
                    mediaItems = mediaItems,
                )
            )
        }

        val sortedPosts = posts.sortedByDescending { it.timestampSeconds }

        val nextScrollVal = (currentScroll ?: 1) + 1
        return SourceSyncPage(
            account = account,
            userId = userId,
            posts = sortedPosts,
            nextPageToken = "scroll_$nextScrollVal",
            hasMorePosts = sortedPosts.isNotEmpty(),
        )
    }

    private fun String.trimUsername(): String = trim().removePrefix("@").trim('/').lowercase()
}
