package com.milkilabs.majra.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources ORDER BY name")
    fun observeSources(): Flow<List<SourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sources: List<SourceEntity>)

    @Query("SELECT COUNT(*) FROM sources")
    suspend fun countSources(): Int

    @Query("SELECT * FROM sources WHERE type = :type ORDER BY name")
    suspend fun getSourcesByType(type: String): List<SourceEntity>

    @Query("UPDATE sources SET name = :name, url = :url WHERE id = :sourceId")
    suspend fun updateSource(sourceId: String, name: String, url: String)

    @Query("DELETE FROM sources WHERE id = :sourceId")
    suspend fun deleteById(sourceId: String)
}

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY publishedAtMillis DESC, id DESC")
    fun observeArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isSaved = 1 ORDER BY publishedAtMillis DESC, id DESC")
    fun observeSavedArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :articleId LIMIT 1")
    suspend fun getArticle(articleId: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(articles: List<ArticleEntity>)

    @Update
    suspend fun updateArticle(article: ArticleEntity)

    @Query("UPDATE articles SET readState = :readState WHERE id = :articleId")
    suspend fun updateReadState(articleId: String, readState: String)

    @Query("UPDATE articles SET isSaved = :isSaved WHERE id = :articleId")
    suspend fun updateSaved(articleId: String, isSaved: Boolean)

    @Query("DELETE FROM articles WHERE sourceId = :sourceId")
    suspend fun deleteBySourceId(sourceId: String)
}
