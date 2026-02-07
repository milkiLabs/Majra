package com.milkilabs.majra.rss

import java.text.DateFormat
import java.util.Date
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.milkilabs.majra.core.model.Article
import com.milkilabs.majra.core.viewer.ArticleViewer
import com.milkilabs.majra.ui.article.HtmlBody
import com.milkilabs.majra.ui.article.InAppBrowserDialog

class RssArticleViewer : ArticleViewer {
    @Composable
    override fun Render(article: Article) {
        var showWebView by remember { mutableStateOf(false) }
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
                    // Summary uses a softer tone to separate it from the full content.
                    Text(
                        text = article.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // Content is rendered as body HTML below the summary.
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
            InAppBrowserDialog(
                url = article.url,
                onDismiss = { showWebView = false },
            )
        }
    }
}
