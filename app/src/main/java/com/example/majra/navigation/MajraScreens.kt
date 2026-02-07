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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.majra.core.viewer.ViewerRegistry
import com.example.majra.feed.ArticleDetailState
import com.example.majra.feed.FeedListItem
import com.example.majra.feed.SourceListItem
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
fun SourcesScreen(
    sources: List<SourceListItem>,
    onSourceSelected: (SourceListItem) -> Unit,
) {
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
    onMarkRead: () -> Unit,
) {
    val article = state.article
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = article?.title.orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (state.sourceName.isNotEmpty()) {
                "From ${state.sourceName}"
            } else {
                ""
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        if (article != null) {
            val viewer = viewerRegistry.viewerFor(article.sourceType)
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    viewer.Render(article)
                }
            }
        } else {
            Card {
                ListItem(
                    headlineContent = { Text("Loading") },
                    supportingContent = { Text("Fetching article details...") },
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ElevatedButton(
                onClick = onToggleSaved,
                enabled = article != null,
            ) {
                Text(if (article?.isSaved == true) "Saved" else "Save")
            }
            OutlinedButton(
                onClick = onMarkRead,
                enabled = article != null,
            ) {
                Text("Mark read")
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
