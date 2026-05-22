package com.milki.majra.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [AccountEntity::class, PostEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(PostMediaConverters::class)
abstract class MajraDatabase : RoomDatabase() {
    abstract fun instagramDao(): InstagramDao
}
