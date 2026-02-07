package com.example.majra

import android.content.Context
import com.prof18.rssparser.RssParserBuilder
import com.example.majra.core.model.SourceTypes
import com.example.majra.core.repository.FeedRepository
import com.example.majra.core.viewer.ArticleViewer
import com.example.majra.core.viewer.DefaultViewerRegistry
import com.example.majra.core.viewer.ViewerRegistry
import com.example.majra.data.db.AppDatabase
import com.example.majra.data.repository.RoomFeedRepository
import com.example.majra.bluesky.BlueskyArticleViewer
import com.example.majra.medium.MediumArticleViewer
import com.example.majra.rss.RssArticleViewer
import com.example.majra.rss.RssSyncer
import com.example.majra.youtube.YoutubeArticleViewer

class AppDependencies(
    val feedRepository: FeedRepository,
    val viewerRegistry: ViewerRegistry,
    val rssSyncer: RssSyncer,
) {
    companion object {
        fun createDefault(context: Context): AppDependencies {
            val database = AppDatabase.create(context)
            val rssParser = RssParserBuilder().build()
            val rssViewer = RssArticleViewer()
            val youtubeViewer = YoutubeArticleViewer()
            val mediumViewer = MediumArticleViewer()
            val blueskyViewer = BlueskyArticleViewer()
            val fallbackViewer = ArticleViewer { article ->
                rssViewer.Render(article)
            }
            return AppDependencies(
                feedRepository = RoomFeedRepository(
                    sourceDao = database.sourceDao(),
                    articleDao = database.articleDao(),
                ),
                rssSyncer = RssSyncer(
                    sourceDao = database.sourceDao(),
                    articleDao = database.articleDao(),
                    parser = rssParser,
                ),
                viewerRegistry = DefaultViewerRegistry(
                    viewers = mapOf(
                        SourceTypes.RSS to rssViewer,
                        SourceTypes.YOUTUBE to youtubeViewer,
                        SourceTypes.MEDIUM to mediumViewer,
                        SourceTypes.BLUESKY to blueskyViewer,
                    ),
                    fallback = fallbackViewer,
                ),
            )
        }
    }
}
