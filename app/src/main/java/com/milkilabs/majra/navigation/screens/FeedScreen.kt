package com.milkilabs.majra.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.milkilabs.majra.core.model.ReadState
import com.milkilabs.majra.core.model.SourceTypes
import com.milkilabs.majra.feed.FeedFilters
import com.milkilabs.majra.feed.FeedListItem
import com.milkilabs.majra.feed.FeedReadFilter
import com.milkilabs.majra.feed.SourceListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    items: List<FeedListItem>,
    filters: FeedFilters,
    sources: List<SourceListItem>,
    onContentSelected: (FeedListItem) -> Unit,
    onCycleReadFilter: () -> Unit,
    onResetReadFilter: () -> Unit,
    onSourceTypeSelected: (String?) -> Unit,
    onSourceSelected: (String?) -> Unit,
) {
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var isSourceSheetOpen by remember { mutableStateOf(false) }
    var sourceQuery by remember { mutableStateOf("") }
    val selectedSourceName = sources.firstOrNull { it.id == filters.sourceId }
        ?.name
        ?.ifBlank { "Unknown source" }
        ?: "All sources"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        // Top heading for the feed hub.
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
        Spacer(modifier = Modifier.height(12.dp))
        // Inline filter bar keeps the primary actions visible with minimal clutter.
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filters.readFilter != FeedReadFilter.Unread,
                    onClick = onCycleReadFilter,
                    label = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(readFilterLabel(filters.readFilter))
                            if (filters.readFilter != FeedReadFilter.Unread) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear read filter",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onResetReadFilter() },
                                )
                            }
                        }
                    },
                )
                FilterChip(
                    selected = filters.sourceId != null,
                    onClick = {
                        sourceQuery = ""
                        isSourceSheetOpen = true
                    },
                    label = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(selectedSourceName)
                            if (filters.sourceId != null) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear source",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onSourceSelected(null) },
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                        )
                    },
                )
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text("Category") },
                    enabled = false,
                )
                Box {
                    FilterChip(
                        selected = filters.sourceType != null,
                        onClick = { typeMenuExpanded = true },
                        label = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(sourceTypeLabel(filters.sourceType))
                                if (filters.sourceType != null) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear type",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onSourceTypeSelected(null) },
                                    )
                                }
                            }
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                            )
                        },
                    )
                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("All types") },
                            onClick = {
                                typeMenuExpanded = false
                                onSourceTypeSelected(null)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("RSS") },
                            onClick = {
                                typeMenuExpanded = false
                                onSourceTypeSelected(SourceTypes.RSS)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("YouTube") },
                            onClick = {
                                typeMenuExpanded = false
                                onSourceTypeSelected(SourceTypes.YOUTUBE)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Medium") },
                            onClick = {
                                typeMenuExpanded = false
                                onSourceTypeSelected(SourceTypes.MEDIUM)
                            },
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (items.isEmpty()) {
            // Empty state nudges users to add sources.
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

    if (isSourceSheetOpen) {
        // Bottom sheet for searchable source selection.
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val filteredSources = sources.filter { source ->
            val matchesType = filters.sourceType?.let { type -> source.type == type } ?: true
            if (!matchesType) return@filter false
            val query = sourceQuery.trim()
            if (query.isBlank()) return@filter true
            source.name.contains(query, ignoreCase = true) ||
                source.url.contains(query, ignoreCase = true)
        }
        ModalBottomSheet(
            onDismissRequest = { isSourceSheetOpen = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Choose source",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                TextField(
                    value = sourceQuery,
                    onValueChange = { sourceQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search sources") },
                )
                if (sources.isEmpty()) {
                    Text(
                        text = "No sources yet.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSourceSelected(null)
                                        isSourceSheetOpen = false
                                    },
                            ) {
                                ListItem(
                                    headlineContent = { Text("All sources") },
                                    supportingContent = { Text("Show every source") },
                                )
                            }
                        }
                        items(filteredSources, key = { it.id }) { source ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSourceSelected(source.id)
                                        isSourceSheetOpen = false
                                    },
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Text(source.name.ifBlank { "Unknown source" })
                                    },
                                    supportingContent = {
                                        Text(sourceTypeLabel(source.type))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun readFilterLabel(filter: FeedReadFilter): String {
    return when (filter) {
        FeedReadFilter.Unread -> "Unread"
        FeedReadFilter.Read -> "Read"
        FeedReadFilter.All -> "All"
    }
}

private fun sourceTypeLabel(type: String?): String {
    return when (type) {
        null -> "All types"
        SourceTypes.RSS -> "RSS"
        SourceTypes.YOUTUBE -> "YouTube"
        SourceTypes.MEDIUM -> "Medium"
        SourceTypes.BLUESKY -> "Bluesky"
        else -> type.uppercase()
    }
}
