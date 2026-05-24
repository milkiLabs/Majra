package com.milki.majra.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.milki.majra.data.model.Platform
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Transaction
    @Query("SELECT * FROM posts ORDER BY timestampSeconds ASC")
    fun observeFeed(): Flow<List<PostWithAccount>>

    @Query("SELECT * FROM accounts ORDER BY platform, username COLLATE NOCASE")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE platform = :platform AND accountId = :accountId LIMIT 1")
    suspend fun getAccount(platform: Platform, accountId: String): AccountEntity?

    @Upsert
    suspend fun upsertAccount(account: AccountEntity)

    @Upsert
    suspend fun upsertPosts(posts: List<PostEntity>)

    @Query("DELETE FROM posts WHERE platform = :platform AND accountId = :accountId")
    suspend fun deletePostsForAccount(platform: Platform, accountId: String)

    @Query("DELETE FROM accounts WHERE platform = :platform AND accountId = :accountId")
    suspend fun deleteAccount(platform: Platform, accountId: String)

    @Transaction
    @Query("SELECT * FROM posts WHERE platform = :platform AND accountId = :accountId ORDER BY timestampSeconds DESC")
    fun observePostsForAccount(platform: Platform, accountId: String): Flow<List<PostWithAccount>>
}
