package com.example.majra.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.majra.core.model.ReadState
import com.example.majra.core.model.SourceTypes
import com.example.majra.core.viewer.ViewerRegistry
import com.example.majra.feed.ArticleDetailState
import com.example.majra.feed.FeedListItem
import com.example.majra.feed.SourceListItem
import com.example.majra.feed.SourceDetailState
import com.example.majra.settings.AccentPalette
import com.example.majra.settings.ShapeDensity
import com.example.majra.settings.ThemeMode
import com.example.majra.settings.ThemePreferences
import com.example.majra.settings.TypographyScale

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
                        supportingContent = { Text(item.summary) },
                        overlineContent = { Text(item.sourceName) },
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

@Composable
fun SavedScreen(
    items: List<FeedListItem>,
    onContentSelected: (FeedListItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Text(
            text = "Saved",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Your long reads and videos in one place.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onContentSelected(item) },
                ) {
                    ListItem(
                        headlineContent = { Text(item.title) },
                        supportingContent = { Text(item.summary) },
                        overlineContent = { Text(item.sourceName) },
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    themePreferences: ThemePreferences,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentPaletteChange: (AccentPalette) -> Unit,
    onTypographyScaleChange: (TypographyScale) -> Unit,
    onShapeDensityChange: (ShapeDensity) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        ThemeSection(
            themePreferences = themePreferences,
            onThemeModeChange = onThemeModeChange,
            onAccentPaletteChange = onAccentPaletteChange,
            onTypographyScaleChange = onTypographyScaleChange,
            onShapeDensityChange = onShapeDensityChange,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card {
            ListItem(
                headlineContent = { Text("Sync and accounts") },
                supportingContent = { Text("Connect Bluesky, YouTube, and RSS sync.") },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card {
            ListItem(
                headlineContent = { Text("Reading preferences") },
                supportingContent = { Text("Fonts, spacing, and offline caching.") },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = {}) {
                Text("Import OPML")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = {}) {
                Text("Manage sources")
            }
        }
    }
}

@Composable
private fun ThemeSection(
    themePreferences: ThemePreferences,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentPaletteChange: (AccentPalette) -> Unit,
    onTypographyScaleChange: (TypographyScale) -> Unit,
    onShapeDensityChange: (ShapeDensity) -> Unit,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SettingOption(
                title = "Mode",
                subtitle = "Light, Dark, or System",
            ) {
                OptionChips(
                    options = ThemeMode.entries.toList(),
                    selected = themePreferences.mode,
                    onSelected = onThemeModeChange,
                    label = { it.label },
                )
            }
            SettingOption(
                title = "Accent palette",
                subtitle = "Warm, grounded tones",
            ) {
                OptionChips(
                    options = AccentPalette.entries.toList(),
                    selected = themePreferences.accentPalette,
                    onSelected = onAccentPaletteChange,
                    label = { it.label },
                )
            }
            SettingOption(
                title = "Typography scale",
                subtitle = "Body size and spacing",
            ) {
                OptionChips(
                    options = TypographyScale.entries.toList(),
                    selected = themePreferences.typographyScale,
                    onSelected = onTypographyScaleChange,
                    label = { it.label },
                )
            }
            SettingOption(
                title = "Shape density",
                subtitle = "Rounded or sharp corners",
            ) {
                OptionChips(
                    options = ShapeDensity.entries.toList(),
                    selected = themePreferences.shapeDensity,
                    onSelected = onShapeDensityChange,
                    label = { it.label },
                )
            }
        }
    }
}

@Composable
private fun SettingOption(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
        )
        content()
    }
}

@Composable
private fun <T> OptionChips(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: (T) -> String,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelected(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

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

@Composable
fun SourceDetailScreen(
    name: String,
    type: String,
    state: SourceDetailState,
    onUpdateSource: (String, String) -> Unit,
    onRemoveSource: () -> Unit,
) {
    val source = state.source
    var showEditDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
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
