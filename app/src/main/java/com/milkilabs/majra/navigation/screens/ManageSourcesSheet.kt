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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.milkilabs.majra.core.model.SourceTypeId
import com.milkilabs.majra.core.source.SourceInputMode
import com.milkilabs.majra.core.source.SourceTypeUi
import com.milkilabs.majra.feed.SourceListItem
import com.milkilabs.majra.feed.SyncStatus
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSourcesSheet(
    status: SyncStatus,
    sources: List<SourceListItem>,
    isAdding: Boolean,
    addErrorMessage: String?,
    sourceTypeOptions: List<SourceTypeUi>,
    onDismiss: () -> Unit,
    onSyncAll: () -> Unit,
    onSyncSource: (String) -> Unit,
    onAddSource: (String, SourceTypeId) -> Unit,
    onUpdateSource: (String, String, String) -> Unit,
    onRemoveSource: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<SourceTypeId?>(null) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var pendingAdd by rememberSaveable { mutableStateOf(false) }
    var urlInput by rememberSaveable { mutableStateOf("") }
    val sourceTypeSaver = remember {
        Saver<SourceTypeId, String>(
            save = { it.value },
            restore = { SourceTypeId.fromValue(it) },
        )
    }
    var selectedAddType by rememberSaveable(stateSaver = sourceTypeSaver) {
        mutableStateOf<SourceTypeId>(SourceTypeId.Rss)
    }
    var editingSource by remember { mutableStateOf<SourceListItem?>(null) }
    var removingSource by remember { mutableStateOf<SourceListItem?>(null) }
    var editNameInput by rememberSaveable { mutableStateOf("") }
    var editUrlInput by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(isAdding, addErrorMessage) {
        if (pendingAdd && !isAdding && addErrorMessage == null) {
            pendingAdd = false
            urlInput = ""
            selectedAddType = SourceTypeId.Rss
            showAddDialog = false
        }
    }
    LaunchedEffect(editingSource?.id) {
        editNameInput = editingSource?.name.orEmpty()
        editUrlInput = editingSource?.url.orEmpty()
    }
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
                text = "Manage sources",
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
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add source")
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
                sourceTypeOptions.forEach { option ->
                    FilterChip(
                        selected = selectedType == option.id,
                        onClick = { selectedType = option.id },
                        enabled = option.isEnabled,
                        label = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(option.label)
                            }
                        },
                    )
                }
            }
            if (sources.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("No sources yet") },
                        supportingContent = { Text("Add a source to start syncing.") },
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredSources, key = { it.id }) { source ->
                        val isActive = status.currentSourceId == source.id && status.isSyncing
                        var rowMenuExpanded by remember { mutableStateOf(false) }
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
                                    Text(syncRowLabel(source, isActive, sourceTypeOptions))
                                },
                                trailingContent = {
                                    if (isActive) {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .size(width = 48.dp, height = 4.dp),
                                        )
                                    } else {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            TextButton(
                                                onClick = { onSyncSource(source.id) },
                                                enabled = !status.isSyncing,
                                            ) {
                                                Text("Sync")
                                            }
                                            IconButton(onClick = { rowMenuExpanded = true }) {
                                                Icon(
                                                    imageVector = Icons.Filled.MoreVert,
                                                    contentDescription = "Source actions",
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = rowMenuExpanded,
                                                onDismissRequest = { rowMenuExpanded = false },
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Edit") },
                                                    onClick = {
                                                        rowMenuExpanded = false
                                                        editingSource = source
                                                    },
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Remove") },
                                                    onClick = {
                                                        rowMenuExpanded = false
                                                        removingSource = source
                                                    },
                                                )
                                            }
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

    if (showAddDialog) {
        val selectedOption = sourceTypeOptions.firstOrNull { it.id == selectedAddType }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add source") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("URL") },
                        placeholder = {
                            Text(selectedOption?.inputHint ?: "https://example.com/feed.xml")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAdding,
                    )
                    selectedOption?.let { option ->
                        val helperText = when (option.inputMode) {
                            SourceInputMode.UrlOnly -> "Paste a ${option.label} feed URL."
                            SourceInputMode.UrlOrHandle -> "Paste a ${option.label} URL or handle."
                        }
                        Text(
                            text = helperText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    addErrorMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    SourceTypePicker(
                        selectedType = selectedAddType,
                        options = sourceTypeOptions,
                        onSelected = {
                            if (!isAdding) {
                                selectedAddType = it
                            }
                        },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddSource(urlInput, selectedAddType)
                        pendingAdd = true
                    },
                    enabled = urlInput.isNotBlank() && !isAdding,
                ) {
                    if (isAdding) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .size(18.dp),
                        )
                    } else {
                        Text("Add")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddDialog = false },
                    enabled = !isAdding,
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    editingSource?.let { source ->
        AlertDialog(
            onDismissRequest = { editingSource = null },
            title = { Text("Edit source") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text("Name") },
                        placeholder = { Text("Optional") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editUrlInput,
                        onValueChange = { editUrlInput = it },
                        label = { Text("URL") },
                        placeholder = { Text("https://example.com/feed.xml") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateSource(source.id, editNameInput, editUrlInput)
                        editingSource = null
                    },
                    enabled = editUrlInput.isNotBlank(),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingSource = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    removingSource?.let { source ->
        AlertDialog(
            onDismissRequest = { removingSource = null },
            title = { Text("Remove source") },
            text = { Text("This removes the source and its articles.") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveSource(source.id)
                        removingSource = null
                    },
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { removingSource = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SourceTypePicker(
    selectedType: SourceTypeId,
    options: List<SourceTypeUi>,
    onSelected: (SourceTypeId) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Source type",
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option.id == selectedType,
                    onClick = { onSelected(option.id) },
                    enabled = option.isEnabled,
                    label = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(option.label)
                        }
                    },
                )
            }
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

private fun syncRowLabel(
    source: SourceListItem,
    isActive: Boolean,
    options: List<SourceTypeUi>,
): String {
    return if (isActive) {
        "Syncing ${sourceTypeLabel(source.type, options)}"
    } else {
        sourceTypeLabel(source.type, options)
    }
}

private fun sourceTypeLabel(type: SourceTypeId, options: List<SourceTypeUi>): String {
    return options.firstOrNull { it.id == type }?.label ?: type.value.uppercase(Locale.US)
}
