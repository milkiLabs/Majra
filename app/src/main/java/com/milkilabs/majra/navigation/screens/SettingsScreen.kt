package com.milkilabs.majra.navigation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.milkilabs.majra.settings.AccentPalette
import com.milkilabs.majra.settings.ShapeDensity
import com.milkilabs.majra.settings.ThemeMode
import com.milkilabs.majra.settings.ThemePreferences
import com.milkilabs.majra.settings.TypographyScale

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
