package com.milkilabs.majra.data.repository

import androidx.room.withTransaction
import com.milkilabs.majra.core.model.Article
import com.milkilabs.majra.core.model.ReadState
import com.milkilabs.majra.core.model.Source
import com.milkilabs.majra.core.repository.FeedRepository
import com.milkilabs.majra.data.db.AppDatabase
import com.milkilabs.majra.data.db.ArticleDao
import com.milkilabs.majra.data.db.ArticleEntity
import com.milkilabs.majra.data.db.SourceDao
import com.milkilabs.majra.data.db.SourceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomFeedRepository(
    private val database: AppDatabase,
    private val sourceDao: SourceDao,
    private val articleDao: ArticleDao,
) : FeedRepository {
    override val sources: Flow<List<Source>> = sourceDao.observeSources().map { entities ->
        entities.map { entity -> entity.toModel() }
    }
    override val feed: Flow<List<Article>> = articleDao.observeArticles().map { entities ->
        entities.map { entity -> entity.toModel() }
    }
    override val saved: Flow<List<Article>> = articleDao.observeSavedArticles().map { entities ->
        entities.map { entity -> entity.toModel() }
    }

    override suspend fun addSource(source: Source) {
        sourceDao.upsertAll(listOf(source.toEntity()))
    }

    override suspend fun updateSource(sourceId: String, name: String, url: String) {
        sourceDao.updateSource(sourceId, name, url)
    }

    override suspend fun removeSource(sourceId: String) {
        // Keep source and articles deletion atomic to avoid partial cleanup.
        database.withTransaction {
            articleDao.deleteBySourceId(sourceId)
            sourceDao.deleteById(sourceId)
        }
    }

    override suspend fun getArticle(articleId: String): Article? {
        return articleDao.getArticle(articleId)?.toModel()
    }

    override suspend fun markRead(articleId: String, state: ReadState) {
        articleDao.updateReadState(articleId, state.name)
    }

    override suspend fun toggleSaved(articleId: String) {
        val current = articleDao.getArticle(articleId) ?: return
        articleDao.updateSaved(articleId, !current.isSaved)
    }
}

private fun SourceEntity.toModel(): Source {
    return Source(
        id = id,
        name = name,
        type = type,
        url = url,
    )
}

private fun Source.toEntity(): SourceEntity {
    return SourceEntity(
        id = id,
        name = name,
        type = type,
        url = url,
    )
}

private fun ArticleEntity.toModel(): Article {
    return Article(
        id = id,
        sourceId = sourceId,
        sourceType = sourceType,
        title = title,
        summary = summary,
        content = content,
        url = url,
        author = author,
        publishedAtMillis = publishedAtMillis,
        isSaved = isSaved,
        readState = ReadState.valueOf(readState),
    )
}
