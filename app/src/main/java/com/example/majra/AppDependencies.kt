package com.example.majra

import com.example.majra.core.model.SourceTypes
import com.example.majra.core.repository.FeedRepository
import com.example.majra.core.repository.InMemoryFeedRepository
import com.example.majra.core.viewer.ArticleViewer
import com.example.majra.core.viewer.DefaultViewerRegistry
import com.example.majra.core.viewer.ViewerRegistry
import com.example.majra.rss.RssArticleViewer

class AppDependencies(
    val feedRepository: FeedRepository,
    val viewerRegistry: ViewerRegistry,
) {
    companion object {
        fun createDefault(): AppDependencies {
            val rssViewer = RssArticleViewer()
            val fallbackViewer = ArticleViewer { article ->
                rssViewer.Render(article)
            }
            return AppDependencies(
                feedRepository = InMemoryFeedRepository(),
                viewerRegistry = DefaultViewerRegistry(
                    viewers = mapOf(
                        SourceTypes.RSS to rssViewer,
                    ),
                    fallback = fallbackViewer,
                ),
            )
        }
    }
}
