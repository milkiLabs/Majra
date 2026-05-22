package com.milki.majra.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.PostMediaItem
import com.milki.majra.data.model.SocialPost

@Entity(
    tableName = "posts",
    primaryKeys = ["platform", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["platform", "accountId"],
            childColumns = ["platform", "accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["platform", "accountId"]), Index("sourceKey"), Index("timestampSeconds")],
)
data class PostEntity(
    val id: String,
    val platform: Platform,
    val platformPostId: String,
    val accountId: String,
    val sourceKey: String,
    val username: String,
    val mediaType: String,
    val caption: String,
    val timestampSeconds: Long,
    val permalink: String,
    val mediaItems: List<PostMediaItem>,
) {
    fun toModel(): SocialPost = SocialPost(
        platform = platform,
        id = id,
        platformPostId = platformPostId,
        accountId = accountId,
        username = username,
        mediaType = mediaType,
        caption = caption,
        timestampSeconds = timestampSeconds,
        permalink = permalink,
        mediaItems = mediaItems,
    )

    companion object {
        fun fromModel(post: SocialPost): PostEntity = PostEntity(
            id = post.id,
            platform = post.platform,
            platformPostId = post.platformPostId,
            accountId = post.accountId,
            sourceKey = post.sourceKey(),
            username = post.username,
            mediaType = post.mediaType,
            caption = post.caption,
            timestampSeconds = post.timestampSeconds,
            permalink = post.permalink,
            mediaItems = post.mediaItems,
        )
    }
}

private fun SocialPost.sourceKey(): String = "${platform.storageKey}:$accountId"
