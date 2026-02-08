package com.milkilabs.majra.bluesky

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import com.milkilabs.majra.core.model.SourceTypeId
import com.milkilabs.majra.core.source.SourceInputMode
import com.milkilabs.majra.core.source.SourcePlugin
import com.milkilabs.majra.core.source.SourceResolveResult
import com.milkilabs.majra.core.viewer.ArticleViewer

class BlueskySourcePlugin(
    override val viewer: ArticleViewer,
) : SourcePlugin {
    override val id: SourceTypeId = SourceTypeId.Bluesky
    override val displayName: String = "Bluesky"
    override val icon = Icons.Filled.Cloud
    override val inputMode: SourceInputMode = SourceInputMode.UrlOrHandle
    override val inputHint: String = "Coming soon"
    override val isEnabled: Boolean = false

    override suspend fun resolve(input: String): SourceResolveResult {
        return SourceResolveResult.Error("Bluesky sources are not supported yet.")
    }

    override suspend fun syncAll() {
        return
    }

    override suspend fun syncSource(sourceId: String) {
        return
    }
}
