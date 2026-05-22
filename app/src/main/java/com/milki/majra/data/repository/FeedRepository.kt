package com.milki.majra.data.repository

import com.milki.majra.data.db.AccountEntity
import com.milki.majra.data.db.InstagramDao
import com.milki.majra.data.db.PostEntity
import com.milki.majra.data.local.SourceSession
import com.milki.majra.data.local.SessionStore
import com.milki.majra.data.model.FeedItem
import com.milki.majra.data.model.Platform
import com.milki.majra.data.model.SocialPost
import com.milki.majra.data.model.SocialProfile
import com.milki.majra.data.network.InstagramHttpClient
import com.milki.majra.data.scraper.InstagramHtmlParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FeedRepository(
    private val dao: InstagramDao,
    private val sessionStore: SessionStore,
    private val clients: List<FeedSourceClient>,
    private val clock: () -> Long,
) {
    val feed: Flow<List<FeedItem>> = dao.observeFeed().map { rows -> rows.map { it.toModel() } }

    val accounts: Flow<List<SocialProfile>> = dao.observeAccounts().map { rows -> rows.map { it.toModel() } }

    fun session(platform: Platform): Flow<SourceSession> = sessionStore.session(platform)

    fun postsForAccount(platform: Platform, accountId: String): Flow<List<FeedItem>> =
        dao.observePostsForAccount(platform, accountId.normalizeSourceId(platform)).map { rows -> rows.map { it.toModel() } }

    suspend fun saveSession(platform: Platform, cookie: String, userAgent: String) {
        sessionStore.save(platform = platform, cookie = cookie, userAgent = userAgent)
    }

    suspend fun clearSession(platform: Platform) {
        sessionStore.clear(platform)
    }

    suspend fun syncSource(platform: Platform, rawSourceId: String): SyncResult {
        val username = rawSourceId.normalizeSourceId(platform)
        if (username.isBlank()) return SyncResult.Failure("Enter a username first.")

        return runCatching {
            val client = clientFor(platform)
            val parsed = client.syncProfile(username)
            dao.upsertAccount(
                AccountEntity.fromModel(
                    account = parsed.account,
                    syncedAtMillis = clock(),
                    userId = parsed.userId,
                    nextPageToken = parsed.nextPageToken,
                    hasMorePosts = parsed.hasMorePosts,
                ),
            )
            dao.upsertPosts(parsed.posts.map(PostEntity::fromModel))
            SyncResult.Success(username = parsed.account.username, postCount = parsed.posts.size)
        }.getOrElse { error ->
            SyncResult.Failure(error.message ?: "Could not sync @$username.")
        }
    }

    suspend fun loadOlderPosts(platform: Platform, rawAccountId: String): SyncResult {
        val username = rawAccountId.normalizeSourceId(platform)
        if (username.isBlank()) return SyncResult.Failure("Choose an account first.")

        return runCatching {
            val client = clientFor(platform)
            val account = dao.getAccount(platform, username)
                ?: return SyncResult.Failure("@$username has not been synced yet.")
            val userId = account.userId
                ?: return SyncResult.Failure("@$username needs one fresh sync before loading older posts.")
            val nextPageToken = account.nextPageToken
                ?: return SyncResult.Failure("No older posts are available for @$username.")
            if (!account.hasMorePosts) {
                return SyncResult.Failure("No older posts are available for @$username.")
            }

            val page = client.loadOlderPosts(
                profile = account.toModel().copy(
                    userId = userId,
                    nextPageToken = nextPageToken,
                ),
            )
            dao.upsertPosts(page.posts.map(PostEntity::fromModel))
            dao.upsertAccount(
                account.copy(
                    nextPageToken = page.nextPageToken,
                    hasMorePosts = page.hasMorePosts,
                    lastSyncedAtMillis = clock(),
                ),
            )
            SyncResult.Success(username = account.username, postCount = page.posts.size)
        }.getOrElse { error ->
            SyncResult.Failure(error.message ?: "Could not load older posts for @$username.")
        }
    }

    suspend fun removeAccount(platform: Platform, accountId: String) {
        dao.deleteAccount(platform, accountId.normalizeSourceId(platform))
    }

    private fun clientFor(platform: Platform): FeedSourceClient =
        clients.firstOrNull { it.platform == platform }
            ?: throw IllegalArgumentException("${platform.displayName} is not supported yet.")

    private fun String.normalizeSourceId(platform: Platform): String {
        val cleaned = trim().removePrefix("@").trim('/')
        return when (platform) {
            Platform.INSTAGRAM,
            Platform.FACEBOOK,
            Platform.X -> cleaned.lowercase()
            Platform.RSS -> cleaned
        }
    }
}

interface FeedSourceClient {
    val platform: Platform
    suspend fun syncProfile(sourceId: String): SourceSyncPage
    suspend fun loadOlderPosts(profile: SocialProfile): SourceSyncPage
}

data class SourceSyncPage(
    val account: SocialProfile,
    val userId: String?,
    val posts: List<SocialPost>,
    val nextPageToken: String?,
    val hasMorePosts: Boolean,
)

class InstagramFeedSourceClient(
    private val httpClient: InstagramHttpClient,
    private val parser: InstagramHtmlParser,
) : FeedSourceClient {
    override val platform: Platform = Platform.INSTAGRAM

    override suspend fun syncProfile(sourceId: String): SourceSyncPage {
        val profilePayload = runCatching {
            httpClient.fetchProfileJson(sourceId)
        }.getOrElse {
            httpClient.fetchProfileHtml(sourceId)
        }
        val profile = parser.parseProfile(sourceId, profilePayload)
        val feedPayload = runCatching {
            profile.userId?.let { httpClient.fetchUserFeedJson(it, profile.account.username) }
        }.getOrNull()
        val parsed = parser.parseProfile(
            username = profile.account.username,
            html = profilePayload,
            feedJson = feedPayload,
        )
        return SourceSyncPage(
            account = parsed.account,
            userId = parsed.userId,
            posts = parsed.posts,
            nextPageToken = parsed.nextMaxId,
            hasMorePosts = parsed.hasMorePosts,
        )
    }

    override suspend fun loadOlderPosts(profile: SocialProfile): SourceSyncPage {
        val userId = profile.userId ?: error("@${profile.username} needs one fresh sync before loading older posts.")
        val nextPageToken = profile.nextPageToken ?: error("No older posts are available for @${profile.username}.")
        val feedPayload = httpClient.fetchUserFeedJson(
            userId = userId,
            username = profile.username,
            maxId = nextPageToken,
        )
        val page = parser.parseFeedPage(profile.username, feedPayload, profile.accountId)
        return SourceSyncPage(
            account = profile,
            userId = userId,
            posts = page.posts,
            nextPageToken = page.nextMaxId,
            hasMorePosts = page.hasMorePosts,
        )
    }
}

sealed interface SyncResult {
    data class Success(val username: String, val postCount: Int) : SyncResult
    data class Failure(val message: String) : SyncResult
}
