package com.milkilabs.majra.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.milkilabs.majra.core.model.SourceTypes
import com.milkilabs.majra.feed.SourceListItem

@Composable
fun SourcesScreen(
    sources: List<SourceListItem>,
    onAddSource: (url: String, type: String) -> Unit,
    onSourceSelected: (SourceListItem) -> Unit,
    isAdding: Boolean,
    addErrorMessage: String?,
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var pendingAdd by rememberSaveable { mutableStateOf(false) }
    var urlInput by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(SourceTypes.RSS) }

    LaunchedEffect(isAdding, addErrorMessage) {
        // Close the dialog only after a successful add.
        if (pendingAdd && !isAdding && addErrorMessage == null) {
            pendingAdd = false
            urlInput = ""
            selectedType = SourceTypes.RSS
            showAddDialog = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Text(
            text = "Sources",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Keep RSS, video, and social feeds together.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add source")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (sources.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListItem(
                    headlineContent = { Text("No sources yet") },
                    supportingContent = { Text("Add your first source to build a calm feed.") },
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(sources, key = { it.id }) { item ->
                    val displayName = item.name.ifBlank { item.url }
                    Card(
                        modifier = Modifier.clickable { onSourceSelected(item) },
                    ) {
                        ListItem(
                            headlineContent = { Text(displayName) },
                            supportingContent = { Text(item.type) },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add source") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("URL") },
                        placeholder = { Text("https://example.com/feed.xml") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAdding,
                    )
                    if (selectedType == SourceTypes.YOUTUBE) {
                        Text(
                            text = "Paste a channel handle, channel URL/ID, or playlist URL.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (selectedType == SourceTypes.MEDIUM) {
                        Text(
                            text = "Paste a Medium handle, publication, or RSS feed URL.",
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
                        selectedType = selectedType,
                        onSelected = {
                            if (!isAdding) {
                                selectedType = it
                            }
                        },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddSource(urlInput, selectedType)
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
}

@Composable
private fun SourceTypePicker(
    selectedType: String,
    onSelected: (String) -> Unit,
) {
    val options = listOf(
        SourceTypeOption(SourceTypes.RSS, "RSS", true),
        SourceTypeOption(SourceTypes.YOUTUBE, "YouTube", true),
        SourceTypeOption(SourceTypes.MEDIUM, "Medium", true),
        SourceTypeOption(SourceTypes.BLUESKY, "Bluesky", false),
    )

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
                    selected = option.type == selectedType,
                    onClick = { onSelected(option.type) },
                    enabled = option.enabled,
                    label = { Text(option.label) },
                )
            }
        }
    }
}

private data class SourceTypeOption(
    val type: String,
    val label: String,
    val enabled: Boolean,
)
