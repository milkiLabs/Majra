package com.example.majra.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.majra.settings.AccentPalette
import com.example.majra.settings.ShapeDensity
import com.example.majra.settings.ThemeMode
import com.example.majra.settings.ThemePreferences
import com.example.majra.settings.TypographyScale

data class FeedItem(
    val id: String,
    val title: String,
    val source: String,
    val summary: String,
)

data class SourceItem(
    val id: String,
    val name: String,
    val type: String,
)

@Composable
fun FeedScreen(
    onContentSelected: (FeedItem) -> Unit,
) {
    val items = remember {
        listOf(
            FeedItem(
                id = "rss-101",
                title = "Designing calmer feeds",
                source = "The Humane Web",
                summary = "Ideas for building quiet, focused reading experiences.",
            ),
            FeedItem(
                id = "yt-204",
                title = "Long-form video worth saving",
                source = "Signal Lab",
                summary = "Curated analysis videos with chapters and notes.",
            ),
            FeedItem(
                id = "bsky-88",
                title = "Bluesky news roundup",
                source = "SkyWatch",
                summary = "Daily digest from your saved lists.",
            ),
        )
    }

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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onContentSelected(item) },
                ) {
                    ListItem(
                        headlineContent = { Text(item.title) },
                        supportingContent = { Text(item.summary) },
                        overlineContent = { Text(item.source) },
                    )
                }
            }
        }
    }
}

@Composable
fun SourcesScreen(
    onSourceSelected: (SourceItem) -> Unit,
) {
    val sources = remember {
        listOf(
            SourceItem(
                id = "source-1",
                name = "The Humane Web",
                type = "RSS",
            ),
            SourceItem(
                id = "source-2",
                name = "Signal Lab",
                type = "YouTube",
            ),
            SourceItem(
                id = "source-3",
                name = "SkyWatch",
                type = "Bluesky",
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Sources",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Keep RSS, video, and social feeds together.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            ElevatedButton(onClick = {}) {
                Text("Add")
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
}

@Composable
fun SavedScreen(
    onContentSelected: (FeedItem) -> Unit,
) {
    val items = remember {
        listOf(
            FeedItem(
                id = "saved-11",
                title = "How to build a reading routine",
                source = "The Humane Web",
                summary = "Saved for later: a gentle routine guide.",
            ),
            FeedItem(
                id = "saved-18",
                title = "YouTube chaptering workflow",
                source = "Signal Lab",
                summary = "Your notes, synced with video chapters.",
            ),
        )
    }

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
                        overlineContent = { Text(item.source) },
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
    title: String,
    source: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "From $source",
            style = MaterialTheme.typography.bodyMedium,
        )
        Card {
            ListItem(
                headlineContent = { Text("Reader") },
                supportingContent = { Text("Article or video detail view placeholder.") },
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ElevatedButton(onClick = {}) {
                Text("Save")
            }
            OutlinedButton(onClick = {}) {
                Text("Share")
            }
        }
    }
}

@Composable
fun SourceDetailScreen(
    name: String,
    type: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Type: $type",
            style = MaterialTheme.typography.bodyMedium,
        )
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
            OutlinedButton(onClick = {}) {
                Text("Edit")
            }
            Button(onClick = {}) {
                Text("Remove")
            }
        }
    }
}
