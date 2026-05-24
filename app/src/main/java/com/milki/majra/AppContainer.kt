package com.milki.majra

import android.content.Context
import androidx.room.Room
import com.milki.majra.data.db.MajraDatabase
import com.milki.majra.data.local.SessionStore
import com.milki.majra.data.platform.facebook.FacebookFeedSourceClient
import com.milki.majra.data.platform.facebook.FacebookGraphQLParser
import com.milki.majra.data.platform.facebook.FacebookHttpClient
import com.milki.majra.data.platform.instagram.InstagramFeedSourceClient
import com.milki.majra.data.platform.instagram.InstagramHttpClient
import com.milki.majra.data.platform.instagram.InstagramHtmlParser
import com.milki.majra.data.repository.FeedRepository

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

    // ── Instagram ────────────────────────────────────────────────────────

    private val instagramParser = InstagramHtmlParser()
    private val instagramHttpClient = InstagramHttpClient(sessionStore)

    // ── Facebook ─────────────────────────────────────────────────────────

    private val facebookGraphQLParser = FacebookGraphQLParser()
    private val facebookHttpClient = FacebookHttpClient(sessionStore)

    // ── Repository (platform-agnostic) ───────────────────────────────────

    val repository: FeedRepository = FeedRepository(
        dao = database.feedDao(),
        sessionStore = sessionStore,
        clients = listOf(
            InstagramFeedSourceClient(
                httpClient = instagramHttpClient,
                parser = instagramParser,
            ),
            FacebookFeedSourceClient(
                httpClient = facebookHttpClient,
                graphQLParser = facebookGraphQLParser,
            ),
        ),
        clock = { System.currentTimeMillis() },
    )
}
