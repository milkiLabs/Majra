package com.milki.majra.data.db

import androidx.room.Entity
import androidx.room.Index
import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.SocialProfile

@Entity(
    tableName = "accounts",
    primaryKeys = ["platform", "accountId"],
    indices = [Index(value = ["sourceKey"], unique = true)],
)
data class AccountEntity(
    val platform: Platform,
    val accountId: String,
    val sourceKey: String,
    val username: String,
    val displayName: String?,
    val profilePicUrl: String?,
    val userId: String?,
    val nextPageToken: String?,
    val hasMorePosts: Boolean,
    val lastSyncedAtMillis: Long,
) {
    fun toModel(): SocialProfile = SocialProfile(
        platform = platform,
        accountId = accountId,
        username = username,
        displayName = displayName,
        profilePicUrl = profilePicUrl,
        userId = userId,
        nextPageToken = nextPageToken,
        hasMorePosts = hasMorePosts,
        lastSyncedAtMillis = lastSyncedAtMillis,
    )

    companion object {
        fun fromModel(
            account: SocialProfile,
            syncedAtMillis: Long,
            userId: String? = account.userId,
            nextPageToken: String? = account.nextPageToken,
            hasMorePosts: Boolean = account.hasMorePosts,
        ): AccountEntity = AccountEntity(
            platform = account.platform,
            accountId = account.accountId,
            sourceKey = account.sourceKey(),
            username = account.username,
            displayName = account.displayName,
            profilePicUrl = account.profilePicUrl,
            userId = userId,
            nextPageToken = nextPageToken,
            hasMorePosts = hasMorePosts,
            lastSyncedAtMillis = syncedAtMillis,
        )
    }
}

private fun SocialProfile.sourceKey(): String = "${platform.storageKey}:$accountId"
