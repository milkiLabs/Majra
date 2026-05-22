package com.milki.majra

import android.content.Context
import androidx.room.Room
import com.milki.majra.data.db.MajraDatabase
import com.milki.majra.data.local.SessionStore
import com.milki.majra.data.network.InstagramHttpClient
import com.milki.majra.data.repository.FeedRepository
import com.milki.majra.data.repository.InstagramFeedSourceClient
import com.milki.majra.data.scraper.InstagramHtmlParser

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    val sessionStore: SessionStore = SessionStore(applicationContext)

    private val database: MajraDatabase = Room.databaseBuilder(
        applicationContext,
        MajraDatabase::class.java,
        "majra.db",
    )
        .fallbackToDestructiveMigration(true)
        .build()

    private val htmlParser = InstagramHtmlParser()

    private val instagramHttpClient = InstagramHttpClient(sessionStore)

    val repository: FeedRepository = FeedRepository(
        dao = database.instagramDao(),
        sessionStore = sessionStore,
        clients = listOf(
            InstagramFeedSourceClient(
                httpClient = instagramHttpClient,
                parser = htmlParser,
            ),
        ),
        clock = { System.currentTimeMillis() },
    )
}
