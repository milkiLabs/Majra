package com.milkilabs.majra.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.milkilabs.majra.core.model.ReadState
import com.milkilabs.majra.feed.FeedListItem

@Composable
fun FeedScreen(
    items: List<FeedListItem>,
    onContentSelected: (FeedListItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Text(
            text = "Feed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Everything new across your sources, one calm stream.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (items.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListItem(
                    headlineContent = { Text("No items yet") },
                    supportingContent = { Text("Add a source to start reading.") },
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    val isUnread = item.readState == ReadState.Unread
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onContentSelected(item) },
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = item.title,
                                    fontWeight = if (isUnread) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = item.summary,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                            },
                            overlineContent = { Text(item.sourceName.ifBlank { "Unknown source" }) },
                            trailingContent = {
                                if (isUnread) {
                                    Text(
                                        text = "Unread",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
