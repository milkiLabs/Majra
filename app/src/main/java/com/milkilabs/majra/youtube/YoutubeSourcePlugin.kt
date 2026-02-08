package com.milkilabs.majra.youtube

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import com.milkilabs.majra.core.model.SourceTypeId
import com.milkilabs.majra.core.source.SourceInputMode
import com.milkilabs.majra.core.source.SourcePlugin
import com.milkilabs.majra.core.source.SourceResolveResult
import com.milkilabs.majra.core.viewer.ArticleViewer

class YoutubeSourcePlugin(
    private val syncer: YoutubeSyncer,
    override val viewer: ArticleViewer,
) : SourcePlugin {
    override val id: SourceTypeId = SourceTypeId.Youtube
    override val displayName: String = "YouTube"
    override val icon = Icons.Filled.PlayArrow
    override val inputMode: SourceInputMode = SourceInputMode.UrlOrHandle
    override val inputHint: String = "https://youtube.com/@handle"
    override val isEnabled: Boolean = true

    override suspend fun resolve(input: String): SourceResolveResult {
        return when (val result = syncer.resolveSource(input)) {
            is YoutubeResolveResult.Error -> SourceResolveResult.Error(result.message)
            is YoutubeResolveResult.Success -> SourceResolveResult.Success(
                url = result.source.feedUrl,
                name = result.source.displayName,
            )
        }
    }

    override suspend fun syncAll() {
        syncer.syncAll()
    }

    override suspend fun syncSource(sourceId: String) {
        syncer.syncSource(sourceId)
    }
}
