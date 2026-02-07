package com.example.majra.rss

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.majra.core.model.Article
import com.example.majra.core.viewer.ArticleViewer

class RssArticleViewer : ArticleViewer {
    @Composable
    override fun Render(article: Article) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "RSS Article",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Card {
                ListItem(
                    headlineContent = { Text("Open original") },
                    supportingContent = { Text(article.url) },
                )
            }
        }
    }
}
