package com.milkilabs.majra.rss

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import com.milkilabs.majra.core.model.SourceTypeId
import com.milkilabs.majra.core.source.SourceInputMode
import com.milkilabs.majra.core.source.SourcePlugin
import com.milkilabs.majra.core.source.SourceResolveResult
import com.milkilabs.majra.core.viewer.ArticleViewer

class RssSourcePlugin(
    private val syncer: RssSyncer,
    override val viewer: ArticleViewer,
) : SourcePlugin {
    override val id: SourceTypeId = SourceTypeId.Rss
    override val displayName: String = "RSS"
    override val icon = Icons.Filled.Public
    override val inputMode: SourceInputMode = SourceInputMode.UrlOnly
    override val inputHint: String = "https://example.com/feed.xml"
    override val isEnabled: Boolean = true

    override suspend fun resolve(input: String): SourceResolveResult {
        val title = syncer.resolveTitle(input)
        return SourceResolveResult.Success(url = input, name = title)
    }

    override suspend fun syncAll() {
        syncer.syncAll()
    }

    override suspend fun syncSource(sourceId: String) {
        syncer.syncSource(sourceId)
    }
}
