package com.example.majra.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.majra.core.viewer.ViewerRegistry
import com.example.majra.feed.ArticleDetailState

@Composable
fun ContentDetailScreen(
    state: ArticleDetailState,
    viewerRegistry: ViewerRegistry,
    onToggleSaved: () -> Unit,
) {
    val article = state.article
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = article?.title.orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            IconButton(
                onClick = onToggleSaved,
                enabled = article != null,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(
                    imageVector = if (article?.isSaved == true) {
                        Icons.Filled.Bookmark
                    } else {
                        Icons.Outlined.BookmarkBorder
                    },
                    contentDescription = if (article?.isSaved == true) {
                        "Saved"
                    } else {
                        "Save"
                    },
                )
            }
        }
        if (article != null) {
            val viewer = viewerRegistry.viewerFor(article.sourceType)
            viewer.Render(article)
        } else {
            Card {
                ListItem(
                    headlineContent = { Text("Loading") },
                    supportingContent = { Text("Fetching article details...") },
                )
            }
        }
    }
}
