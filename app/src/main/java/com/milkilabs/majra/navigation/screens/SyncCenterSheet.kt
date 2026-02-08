package com.milkilabs.majra.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.milkilabs.majra.core.model.SourceTypes
import com.milkilabs.majra.feed.SourceListItem
import com.milkilabs.majra.feed.SyncStatus
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncCenterSheet(
    status: SyncStatus,
    sources: List<SourceListItem>,
    onDismiss: () -> Unit,
    onSyncAll: () -> Unit,
    onSyncSource: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<String?>(null) }
    val filteredSources = sources.filter { source ->
        val matchesType = selectedType?.let { type -> source.type == type } ?: true
        if (!matchesType) return@filter false
        val search = query.trim()
        if (search.isBlank()) return@filter true
        source.name.contains(search, ignoreCase = true) ||
            source.url.contains(search, ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Sync",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = syncStatusLabel(status),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (status.isSyncing && status.total > 0) {
                LinearProgressIndicator(
                    progress = { status.completed.toFloat() / status.total.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            status.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = onSyncAll,
                enabled = !status.isSyncing && sources.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sync all now")
            }
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search sources") },
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { selectedType = null },
                    label = { Text("All") },
                )
                FilterChip(
                    selected = selectedType == SourceTypes.RSS,
                    onClick = { selectedType = SourceTypes.RSS },
                    label = { Text("RSS") },
                )
                FilterChip(
                    selected = selectedType == SourceTypes.YOUTUBE,
                    onClick = { selectedType = SourceTypes.YOUTUBE },
                    label = { Text("YouTube") },
                )
                FilterChip(
                    selected = selectedType == SourceTypes.MEDIUM,
                    onClick = { selectedType = SourceTypes.MEDIUM },
                    label = { Text("Medium") },
                )
            }
            if (sources.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("No sources yet") },
                        supportingContent = { Text("Add a source to enable syncing.") },
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredSources, key = { it.id }) { source ->
                        val isActive = status.currentSourceId == source.id && status.isSyncing
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !status.isSyncing) {
                                    onSyncSource(source.id)
                                },
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(source.name.ifBlank { "Unknown source" })
                                },
                                supportingContent = {
                                    Text(syncRowLabel(source, isActive))
                                },
                                trailingContent = {
                                    if (isActive) {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .size(width = 48.dp, height = 4.dp),
                                        )
                                    } else {
                                        TextButton(
                                            onClick = { onSyncSource(source.id) },
                                            enabled = !status.isSyncing,
                                        ) {
                                            Text("Sync")
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun syncStatusLabel(status: SyncStatus): String {
    return if (status.isSyncing) {
        if (status.total > 0) {
            "Syncing ${status.completed}/${status.total}"
        } else {
            "Syncing"
        }
    } else {
        val lastSynced = status.lastSyncedMillis ?: return "Ready to sync"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - lastSynced)
        if (minutes <= 0) {
            "Last synced just now"
        } else {
            "Last synced ${minutes} min ago"
        }
    }
}

private fun syncRowLabel(source: SourceListItem, isActive: Boolean): String {
    return if (isActive) {
        "Syncing ${sourceTypeLabel(source.type)}"
    } else {
        sourceTypeLabel(source.type)
    }
}

private fun sourceTypeLabel(type: String): String {
    return when (type) {
        SourceTypes.RSS -> "RSS"
        SourceTypes.YOUTUBE -> "YouTube"
        SourceTypes.MEDIUM -> "Medium"
        SourceTypes.BLUESKY -> "Bluesky"
        else -> type.uppercase()
    }
}
