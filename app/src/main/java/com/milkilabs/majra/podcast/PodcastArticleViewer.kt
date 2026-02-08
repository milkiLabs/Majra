package com.milkilabs.majra.podcast

import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
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

class PodcastArticleViewer : ArticleViewer {
    @Composable
    override fun Render(article: Article) {
        var showAudio by remember { mutableStateOf(false) }
        var showWebView by remember { mutableStateOf(false) }
        val publishedAt = article.publishedAtMillis?.let { millis ->
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(millis))
        }
        val duration = article.audioDurationSeconds?.let { formatDuration(it) }
        val contentHtml = article.content?.takeIf { it.isNotBlank() }
        val audioUrl = article.audioUrl

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card {
                ListItem(
                    headlineContent = { Text("Listen") },
                    supportingContent = {
                        Text(audioSummaryLabel(audioUrl, duration))
                    },
                    trailingContent = {
                        TextButton(
                            onClick = { showAudio = true },
                            enabled = audioUrl != null,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text("Play")
                        }
                    },
                )
            }
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
                    if (article.author != null || publishedAt != null || duration != null ||
                        article.episodeNumber != null
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            article.author?.let { author ->
                                Text(
                                    text = author,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            article.episodeNumber?.let { episode ->
                                Text(
                                    text = "Episode $episode",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            duration?.let { value ->
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        if (showAudio && audioUrl != null) {
            InAppBrowserDialog(
                url = audioUrl,
                onDismiss = { showAudio = false },
            )
        }
        if (showWebView) {
            InAppBrowserDialog(
                url = article.url,
                onDismiss = { showWebView = false },
            )
        }
    }

    private fun audioSummaryLabel(audioUrl: String?, duration: String?): String {
        return when {
            audioUrl == null -> "No audio found for this episode."
            duration != null -> "Duration $duration"
            else -> "Audio available"
        }
    }

    private fun formatDuration(seconds: Int): String {
        val secondsLong = seconds.toLong()
        val hours = TimeUnit.SECONDS.toHours(secondsLong)
        val minutes = TimeUnit.SECONDS.toMinutes(secondsLong) % 60
        val secs = (secondsLong % 60).toInt()
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, secs)
        }
    }
}
