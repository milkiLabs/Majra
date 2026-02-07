package com.milkilabs.majra.core.viewer

import androidx.compose.runtime.Composable
import com.milkilabs.majra.core.model.Article

fun interface ArticleViewer {
    @Composable
    fun Render(article: Article)
}

interface ViewerRegistry {
    fun viewerFor(sourceType: String): ArticleViewer
}

class DefaultViewerRegistry(
    private val viewers: Map<String, ArticleViewer>,
    private val fallback: ArticleViewer,
) : ViewerRegistry {
    override fun viewerFor(sourceType: String): ArticleViewer =
        viewers[sourceType] ?: fallback
}
