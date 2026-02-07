package com.example.majra.navigation

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
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.majra.core.model.SourceTypes
import com.example.majra.feed.SourceListItem

@Composable
fun SourcesScreen(
    sources: List<SourceListItem>,
    onAddSource: (url: String, type: String) -> Unit,
    onSyncRss: () -> Unit,
    onSourceSelected: (SourceListItem) -> Unit,
    isAdding: Boolean,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingAdd by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(SourceTypes.RSS) }

    LaunchedEffect(isAdding) {
        if (pendingAdd && !isAdding) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onSyncRss,
                modifier = Modifier.weight(1f),
            ) {
                Text("Sync RSS")
            }
            ElevatedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.weight(1f),
            ) {
                Text("Add source")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(sources, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.clickable { onSourceSelected(item) },
                ) {
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = { Text(item.type) },
                    )
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
        SourceTypeOption(SourceTypes.YOUTUBE, "YouTube", false),
        SourceTypeOption(SourceTypes.MEDIUM, "Medium", false),
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
