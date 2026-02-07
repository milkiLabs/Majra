package com.milkilabs.majra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import com.milkilabs.majra.settings.AccentPalette
import com.milkilabs.majra.settings.ShapeDensity
import com.milkilabs.majra.settings.ThemeMode
import com.milkilabs.majra.settings.ThemePreferences

@Composable
/**
 * App theme entry point; resolves palette, typography scale, and shapes from user preferences.
 */
fun MajraTheme(
    themePreferences: ThemePreferences,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themePreferences.mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val palette = themePreferences.accentPalette.tokens()
    val colorScheme = if (useDarkTheme) palette.dark else palette.light
    val typography = typographyForScale(themePreferences.typographyScale)
    val shapes = shapesForDensity(themePreferences.shapeDensity)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
        content = content
    )
}

private data class AccentPaletteTokens(
    val light: androidx.compose.material3.ColorScheme,
    val dark: androidx.compose.material3.ColorScheme,
)

/** Map a palette choice to light/dark Material color schemes. */
private fun AccentPalette.tokens(): AccentPaletteTokens {
    return when (this) {
        AccentPalette.Evergreen -> AccentPaletteTokens(
            light = lightColorScheme(
                primary = EvergreenLightPrimary,
                onPrimary = EvergreenLightOnPrimary,
                primaryContainer = EvergreenLightPrimaryContainer,
                onPrimaryContainer = EvergreenLightOnPrimaryContainer,
                secondary = EvergreenLightSecondary,
                onSecondary = EvergreenLightOnSecondary,
                secondaryContainer = EvergreenLightSecondaryContainer,
                onSecondaryContainer = EvergreenLightOnSecondaryContainer,
                tertiary = EvergreenLightTertiary,
                onTertiary = EvergreenLightOnTertiary,
                tertiaryContainer = EvergreenLightTertiaryContainer,
                onTertiaryContainer = EvergreenLightOnTertiaryContainer,
                background = EvergreenLightBackground,
                onBackground = EvergreenLightOnBackground,
                surface = EvergreenLightSurface,
                onSurface = EvergreenLightOnSurface,
                surfaceVariant = EvergreenLightSurfaceVariant,
                onSurfaceVariant = EvergreenLightOnSurfaceVariant,
                outline = EvergreenLightOutline,
            ),
            dark = darkColorScheme(
                primary = EvergreenDarkPrimary,
                onPrimary = EvergreenDarkOnPrimary,
                primaryContainer = EvergreenDarkPrimaryContainer,
                onPrimaryContainer = EvergreenDarkOnPrimaryContainer,
                secondary = EvergreenDarkSecondary,
                onSecondary = EvergreenDarkOnSecondary,
                secondaryContainer = EvergreenDarkSecondaryContainer,
                onSecondaryContainer = EvergreenDarkOnSecondaryContainer,
                tertiary = EvergreenDarkTertiary,
                onTertiary = EvergreenDarkOnTertiary,
                tertiaryContainer = EvergreenDarkTertiaryContainer,
                onTertiaryContainer = EvergreenDarkOnTertiaryContainer,
                background = EvergreenDarkBackground,
                onBackground = EvergreenDarkOnBackground,
                surface = EvergreenDarkSurface,
                onSurface = EvergreenDarkOnSurface,
                surfaceVariant = EvergreenDarkSurfaceVariant,
                onSurfaceVariant = EvergreenDarkOnSurfaceVariant,
                outline = EvergreenDarkOutline,
            ),
        )
        AccentPalette.Cedar -> AccentPaletteTokens(
            light = lightColorScheme(
                primary = CedarLightPrimary,
                onPrimary = CedarLightOnPrimary,
                primaryContainer = CedarLightPrimaryContainer,
                onPrimaryContainer = CedarLightOnPrimaryContainer,
                secondary = CedarLightSecondary,
                onSecondary = CedarLightOnSecondary,
                secondaryContainer = CedarLightSecondaryContainer,
                onSecondaryContainer = CedarLightOnSecondaryContainer,
                tertiary = CedarLightTertiary,
                onTertiary = CedarLightOnTertiary,
                tertiaryContainer = CedarLightTertiaryContainer,
                onTertiaryContainer = CedarLightOnTertiaryContainer,
                background = CedarLightBackground,
                onBackground = CedarLightOnBackground,
                surface = CedarLightSurface,
                onSurface = CedarLightOnSurface,
                surfaceVariant = CedarLightSurfaceVariant,
                onSurfaceVariant = CedarLightOnSurfaceVariant,
                outline = CedarLightOutline,
            ),
            dark = darkColorScheme(
                primary = CedarDarkPrimary,
                onPrimary = CedarDarkOnPrimary,
                primaryContainer = CedarDarkPrimaryContainer,
                onPrimaryContainer = CedarDarkOnPrimaryContainer,
                secondary = CedarDarkSecondary,
                onSecondary = CedarDarkOnSecondary,
                secondaryContainer = CedarDarkSecondaryContainer,
                onSecondaryContainer = CedarDarkOnSecondaryContainer,
                tertiary = CedarDarkTertiary,
                onTertiary = CedarDarkOnTertiary,
                tertiaryContainer = CedarDarkTertiaryContainer,
                onTertiaryContainer = CedarDarkOnTertiaryContainer,
                background = CedarDarkBackground,
                onBackground = CedarDarkOnBackground,
                surface = CedarDarkSurface,
                onSurface = CedarDarkOnSurface,
                surfaceVariant = CedarDarkSurfaceVariant,
                onSurfaceVariant = CedarDarkOnSurfaceVariant,
                outline = CedarDarkOutline,
            ),
        )
        AccentPalette.Oasis -> AccentPaletteTokens(
            light = lightColorScheme(
                primary = OasisLightPrimary,
                onPrimary = OasisLightOnPrimary,
                primaryContainer = OasisLightPrimaryContainer,
                onPrimaryContainer = OasisLightOnPrimaryContainer,
                secondary = OasisLightSecondary,
                onSecondary = OasisLightOnSecondary,
                secondaryContainer = OasisLightSecondaryContainer,
                onSecondaryContainer = OasisLightOnSecondaryContainer,
                tertiary = OasisLightTertiary,
                onTertiary = OasisLightOnTertiary,
                tertiaryContainer = OasisLightTertiaryContainer,
                onTertiaryContainer = OasisLightOnTertiaryContainer,
                background = OasisLightBackground,
                onBackground = OasisLightOnBackground,
                surface = OasisLightSurface,
                onSurface = OasisLightOnSurface,
                surfaceVariant = OasisLightSurfaceVariant,
                onSurfaceVariant = OasisLightOnSurfaceVariant,
                outline = OasisLightOutline,
            ),
            dark = darkColorScheme(
                primary = OasisDarkPrimary,
                onPrimary = OasisDarkOnPrimary,
                primaryContainer = OasisDarkPrimaryContainer,
                onPrimaryContainer = OasisDarkOnPrimaryContainer,
                secondary = OasisDarkSecondary,
                onSecondary = OasisDarkOnSecondary,
                secondaryContainer = OasisDarkSecondaryContainer,
                onSecondaryContainer = OasisDarkOnSecondaryContainer,
                tertiary = OasisDarkTertiary,
                onTertiary = OasisDarkOnTertiary,
                tertiaryContainer = OasisDarkTertiaryContainer,
                onTertiaryContainer = OasisDarkOnTertiaryContainer,
                background = OasisDarkBackground,
                onBackground = OasisDarkOnBackground,
                surface = OasisDarkSurface,
                onSurface = OasisDarkOnSurface,
                surfaceVariant = OasisDarkSurfaceVariant,
                onSurfaceVariant = OasisDarkOnSurfaceVariant,
                outline = OasisDarkOutline,
            ),
        )
    }
}

/** Build Material shapes based on the selected corner density. */
private fun shapesForDensity(density: ShapeDensity): Shapes {
    val (small, medium, large) = when (density) {
        ShapeDensity.Rounded -> Triple(12.dp, 18.dp, 26.dp)
        ShapeDensity.Sharp -> Triple(2.dp, 6.dp, 10.dp)
    }
    return Shapes(
        small = androidx.compose.foundation.shape.RoundedCornerShape(small),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(medium),
        large = androidx.compose.foundation.shape.RoundedCornerShape(large),
    )
}