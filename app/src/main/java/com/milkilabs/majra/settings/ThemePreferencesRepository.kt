package com.milkilabs.majra.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Preferences DataStore for theme-related settings. */
val Context.themePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences",
)

/**
 * Persists theme preferences and exposes them as a Flow for UI consumption.
 *
 * Uses DataStore Preferences to keep the storage simple and reliable for small key/value state.
 */
class ThemePreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    /** Stream of theme preferences with safe fallbacks on IO errors. */
    val themePreferences: Flow<ThemePreferences> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                ThemePreferences(
                    mode = preferences.enumValueOrDefault(
                        key = Keys.themeMode,
                        default = ThemeMode.System,
                    ),
                    accentPalette = preferences.enumValueOrDefault(
                        key = Keys.accentPalette,
                        default = AccentPalette.Evergreen,
                    ),
                    typographyScale = preferences.enumValueOrDefault(
                        key = Keys.typographyScale,
                        default = TypographyScale.Default,
                    ),
                    shapeDensity = preferences.enumValueOrDefault(
                        key = Keys.shapeDensity,
                        default = ShapeDensity.Rounded,
                    ),
                )
            }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[Keys.themeMode] = mode.name
        }
    }

    suspend fun setAccentPalette(palette: AccentPalette) {
        dataStore.edit { preferences ->
            preferences[Keys.accentPalette] = palette.name
        }
    }

    suspend fun setTypographyScale(scale: TypographyScale) {
        dataStore.edit { preferences ->
            preferences[Keys.typographyScale] = scale.name
        }
    }

    suspend fun setShapeDensity(density: ShapeDensity) {
        dataStore.edit { preferences ->
            preferences[Keys.shapeDensity] = density.name
        }
    }

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val accentPalette = stringPreferencesKey("accent_palette")
        val typographyScale = stringPreferencesKey("typography_scale")
        val shapeDensity = stringPreferencesKey("shape_density")
    }
}

/** Map a stored string to an enum, falling back to a default when missing or invalid. */
private inline fun <reified T : Enum<T>> Preferences.enumValueOrDefault(
    key: Preferences.Key<String>,
    default: T,
): T {
    val storedValue = this[key] ?: return default
    return enumValues<T>().firstOrNull { it.name == storedValue } ?: default
}

private fun Preferences.enumValueOrDefault(
    key: Preferences.Key<String>,
    default: AccentPalette,
): AccentPalette {
    val storedValue = this[key] ?: return default
    val normalizedValue = when (storedValue) {
        "SufiGreen" -> AccentPalette.Evergreen.name
        else -> storedValue
    }
    return enumValues<AccentPalette>().firstOrNull { it.name == normalizedValue } ?: default
}
