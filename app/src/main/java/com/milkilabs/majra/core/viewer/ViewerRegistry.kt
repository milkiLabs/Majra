package com.milkilabs.majra.core.viewer

import androidx.compose.runtime.Composable
import com.milkilabs.majra.core.model.Article
import com.milkilabs.majra.core.model.SourceTypeId

fun interface ArticleViewer {
    @Composable
    fun Render(article: Article)
}

interface ViewerRegistry {
    fun viewerFor(sourceType: SourceTypeId): ArticleViewer
}

class DefaultViewerRegistry(
    private val viewers: Map<SourceTypeId, ArticleViewer>,
    private val fallback: ArticleViewer,
) : ViewerRegistry {
    override fun viewerFor(sourceType: SourceTypeId): ArticleViewer =
        viewers[sourceType] ?: fallback
}
