package com.milkilabs.majra.core.source

import java.util.Locale
import com.milkilabs.majra.core.model.SourceTypeId
import androidx.compose.ui.graphics.vector.ImageVector
import com.milkilabs.majra.core.viewer.ArticleViewer

enum class SourceInputMode {
    UrlOnly,
    UrlOrHandle,
}

data class SourceTypeUi(
    val id: SourceTypeId,
    val label: String,
    val isEnabled: Boolean,
    val icon: ImageVector,
    val inputMode: SourceInputMode,
    val inputHint: String,
)

sealed interface SourceResolveResult {
    data class Success(
        val url: String,
        val name: String?,
    ) : SourceResolveResult

    data class Error(val message: String) : SourceResolveResult
}

interface SourcePlugin {
    val id: SourceTypeId
    val displayName: String
    val icon: ImageVector
    val inputMode: SourceInputMode
    val inputHint: String
    val isEnabled: Boolean
    val viewer: ArticleViewer?

    suspend fun resolve(input: String): SourceResolveResult
    suspend fun syncAll()
    suspend fun syncSource(sourceId: String)
}

class SourcePluginRegistry(
    plugins: List<SourcePlugin>,
) {
    val all: List<SourcePlugin> = plugins
    val enabled: List<SourcePlugin> = plugins.filter { it.isEnabled }

    fun pluginFor(id: SourceTypeId): SourcePlugin? = all.firstOrNull { it.id == id }

    fun labelFor(id: SourceTypeId): String =
        pluginFor(id)?.displayName ?: id.value.uppercase(Locale.US)

    fun typeOptions(): List<SourceTypeUi> = all.map { plugin ->
        SourceTypeUi(
            id = plugin.id,
            label = plugin.displayName,
            isEnabled = plugin.isEnabled,
            icon = plugin.icon,
            inputMode = plugin.inputMode,
            inputHint = plugin.inputHint,
        )
    }
}
