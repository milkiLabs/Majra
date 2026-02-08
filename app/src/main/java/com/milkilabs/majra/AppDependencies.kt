package com.milkilabs.majra

import android.content.Context
import com.prof18.rssparser.RssParserBuilder
import okhttp3.OkHttpClient
import com.milkilabs.majra.core.model.SourceTypes
import com.milkilabs.majra.core.repository.FeedRepository
import com.milkilabs.majra.core.viewer.ArticleViewer
import com.milkilabs.majra.core.viewer.DefaultViewerRegistry
import com.milkilabs.majra.core.viewer.ViewerRegistry
import com.milkilabs.majra.data.db.AppDatabase
import com.milkilabs.majra.data.repository.RoomFeedRepository
import com.milkilabs.majra.bluesky.BlueskyArticleViewer
import com.milkilabs.majra.medium.MediumArticleViewer
import com.milkilabs.majra.medium.MediumSyncer
import com.milkilabs.majra.medium.MediumUrlResolver
import com.milkilabs.majra.podcast.PodcastArticleViewer
import com.milkilabs.majra.podcast.PodcastSyncer
import com.milkilabs.majra.rss.RssArticleViewer
import com.milkilabs.majra.rss.RssSyncer
import com.milkilabs.majra.youtube.YoutubeArticleViewer
import com.milkilabs.majra.youtube.YoutubeSyncer
import com.milkilabs.majra.youtube.YoutubeUrlResolver

// Simple container that wires app-wide services without a full DI framework.
class AppDependencies(
    val feedRepository: FeedRepository,
    val viewerRegistry: ViewerRegistry,
    val rssSyncer: RssSyncer,
    val podcastSyncer: PodcastSyncer,
    val youtubeSyncer: YoutubeSyncer,
    val mediumSyncer: MediumSyncer,
) {
    companion object {
        // Centralized construction keeps setup consistent and makes testing easier to swap.
        fun createDefault(context: Context): AppDependencies {
            val database = AppDatabase.create(context)
            val httpClient = OkHttpClient()
            val rssParser = RssParserBuilder().build()
            val mediumUrlResolver = MediumUrlResolver()
            val rssViewer = RssArticleViewer()
            val podcastViewer = PodcastArticleViewer()
            val youtubeViewer = YoutubeArticleViewer()
            val mediumViewer = MediumArticleViewer()
            val blueskyViewer = BlueskyArticleViewer()
            val fallbackViewer = ArticleViewer { article ->
                rssViewer.Render(article)
            }
            return AppDependencies(
                feedRepository = RoomFeedRepository(
                    database = database,
                    sourceDao = database.sourceDao(),
                    articleDao = database.articleDao(),
                ),
                rssSyncer = RssSyncer(
                    sourceDao = database.sourceDao(),
                    articleDao = database.articleDao(),
                    parser = rssParser,
                ),
                podcastSyncer = PodcastSyncer(
                    sourceDao = database.sourceDao(),
                    articleDao = database.articleDao(),
                    parser = rssParser,
                ),
                youtubeSyncer = YoutubeSyncer(
                    sourceDao = database.sourceDao(),
                    articleDao = database.articleDao(),
                    parser = rssParser,
                    urlResolver = YoutubeUrlResolver(httpClient),
                ),
                mediumSyncer = MediumSyncer(
                    sourceDao = database.sourceDao(),
                    articleDao = database.articleDao(),
                    parser = rssParser,
                    urlResolver = mediumUrlResolver,
                ),
                viewerRegistry = DefaultViewerRegistry(
                    viewers = mapOf(
                        SourceTypes.RSS to rssViewer,
                        SourceTypes.PODCAST to podcastViewer,
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
