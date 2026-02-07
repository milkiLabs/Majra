package com.example.majra.rss

import java.text.DateFormat
import java.util.Date
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.text.HtmlCompat
import androidx.compose.ui.text.AnnotatedString
import com.example.majra.core.model.Article
import com.example.majra.core.viewer.ArticleViewer

class RssArticleViewer : ArticleViewer {
    @Composable
    override fun Render(article: Article) {
        var showWebView by remember { mutableStateOf(false) }
        val clipboardManager = LocalClipboardManager.current
        val uriHandler = LocalUriHandler.current
        val publishedAt = article.publishedAtMillis?.let { millis ->
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(millis))
        }
        val contentHtml = article.content?.takeIf { it.isNotBlank() }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card {
                ListItem(
                    headlineContent = { Text("Open original") },
                    trailingContent = {
                        TextButton(
                            onClick = { showWebView = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text("Open")
                        }
                    },
                )
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "RSS Article",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (article.author != null || publishedAt != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            article.author?.let { author ->
                                Text(
                                    text = author,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            publishedAt?.let { date ->
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Text(
                        text = article.summary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    contentHtml?.let { html ->
                        HtmlBody(
                            html = html,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        if (showWebView) {
            Dialog(
                onDismissRequest = { showWebView = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = MaterialTheme.shapes.small,
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SelectionContainer(
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = article.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(article.url))
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy URL",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(
                            onClick = { uriHandler.openUri(article.url) },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.OpenInBrowser,
                                contentDescription = "Open in browser",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Card(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                WebView(context).apply {
                                    webViewClient = WebViewClient()
                                    settings.javaScriptEnabled = true
                                    loadUrl(article.url)
                                }
                            },
                            update = { view ->
                                if (view.url != article.url) {
                                    view.loadUrl(article.url)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HtmlBody(
    html: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textColor = MaterialTheme.colorScheme.onSurface
    val fontSize = MaterialTheme.typography.bodyMedium.fontSize
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                setTextColor(textColor.toArgb())
                setLineSpacing(0f, 1.2f)
                textSize = with(density) { fontSize.toPx() } / density.density
            }
        },
        update = { view ->
            view.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        },
    )
}
