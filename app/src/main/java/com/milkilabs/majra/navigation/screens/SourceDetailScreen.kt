package com.milkilabs.majra.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.milkilabs.majra.feed.SourceDetailState

@Composable
fun SourceDetailScreen(
    name: String,
    type: String,
    state: SourceDetailState,
    onUpdateSource: (String, String) -> Unit,
    onRemoveSource: () -> Unit,
) {
    val source = state.source
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showRemoveDialog by rememberSaveable { mutableStateOf(false) }
    var nameInput by rememberSaveable { mutableStateOf("") }
    var urlInput by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(source?.name, source?.url) {
        if (!showEditDialog) {
            nameInput = source?.name.orEmpty()
            urlInput = source?.url.orEmpty()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card {
            ListItem(
                headlineContent = { Text("Source URL") },
                supportingContent = { Text(source?.url.orEmpty()) },
            )
        }
        Card {
            ListItem(
                headlineContent = { Text("Update frequency") },
                supportingContent = { Text("Every 30 minutes") },
            )
        }
        Card {
            ListItem(
                headlineContent = { Text("Notifications") },
                supportingContent = { Text("Highlights only") },
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = {
                    nameInput = source?.name.orEmpty()
                    urlInput = source?.url.orEmpty()
                    showEditDialog = true
                },
                enabled = source != null,
            ) {
                Text("Edit source")
            }
            Button(
                onClick = { showRemoveDialog = true },
                enabled = source != null,
            ) {
                Text("Remove")
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit source") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Name") },
                        placeholder = { Text("Optional") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
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
                        onUpdateSource(nameInput, urlInput)
                        showEditDialog = false
                    },
                    enabled = urlInput.isNotBlank(),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove source") },
            text = { Text("This removes the source and its articles.") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveSource()
                        showRemoveDialog = false
                    },
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
