package com.milkilabs.majra.medium

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import com.milkilabs.majra.core.model.SourceTypeId
import com.milkilabs.majra.core.source.SourceInputMode
import com.milkilabs.majra.core.source.SourcePlugin
import com.milkilabs.majra.core.source.SourceResolveResult
import com.milkilabs.majra.core.viewer.ArticleViewer

class MediumSourcePlugin(
    private val syncer: MediumSyncer,
    override val viewer: ArticleViewer,
) : SourcePlugin {
    override val id: SourceTypeId = SourceTypeId.Medium
    override val displayName: String = "Medium"
    override val icon = Icons.Filled.Description
    override val inputMode: SourceInputMode = SourceInputMode.UrlOrHandle
    override val inputHint: String = "https://medium.com/@handle"
    override val isEnabled: Boolean = true

    override suspend fun resolve(input: String): SourceResolveResult {
        return when (val result = syncer.resolveSource(input)) {
            is MediumResolveResult.Error -> SourceResolveResult.Error(result.message)
            is MediumResolveResult.Success -> SourceResolveResult.Success(
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
