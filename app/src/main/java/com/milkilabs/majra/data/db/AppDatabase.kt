package com.milkilabs.majra.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.Callback
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.milkilabs.majra.core.model.SourceTypeId

@Database(
    entities = [SourceEntity::class, ArticleEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(SourceTypeIdConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun articleDao(): ArticleDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "majra.db",
            ).addCallback(SeedCallback()).build()
        }
    }
}

private class SeedCallback : Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.execSQL(
            """
            INSERT INTO sources (id, name, type, url)
            VALUES ('source-rss-1', 'The Humane Web', '${SourceTypeId.Rss.value}', 'https://example.com/feed.xml')
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO articles (
                id, sourceId, sourceType, title, summary, content, url, author,
                audioUrl, audioMimeType, audioDurationSeconds, episodeNumber, imageUrl,
                publishedAtMillis, isSaved, readState
            ) VALUES (
                'rss-101', 'source-rss-1', '${SourceTypeId.Rss.value}', 'Designing calmer feeds',
                'Ideas for building quiet, focused reading experiences.',
                'A gentle approach to reader-first design.',
                'https://example.com/reader-first', 'Humane Web',
                NULL, NULL, NULL, NULL, NULL,
                NULL, 0, 'Unread'
            )
            """.trimIndent(),
        )
    }
}
