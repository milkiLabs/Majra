package com.example.majra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.example.majra.navigation.MajraApp
import com.example.majra.settings.ThemePreferences
import com.example.majra.settings.ThemePreferencesRepository
import com.example.majra.settings.themePreferencesDataStore
import com.example.majra.ui.theme.MajraTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val themePreferencesRepository by lazy {
        ThemePreferencesRepository(applicationContext.themePreferencesDataStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePreferences by themePreferencesRepository.themePreferences.collectAsState(
                initial = ThemePreferences(),
            )
            val scope = rememberCoroutineScope()

            MajraTheme(
                themePreferences = themePreferences,
            ) {
                MajraApp(
                    themePreferences = themePreferences,
                    onThemeModeChange = { mode ->
                        scope.launch { themePreferencesRepository.setThemeMode(mode) }
                    },
                    onAccentPaletteChange = { palette ->
                        scope.launch { themePreferencesRepository.setAccentPalette(palette) }
                    },
                    onTypographyScaleChange = { scale ->
                        scope.launch { themePreferencesRepository.setTypographyScale(scale) }
                    },
                    onShapeDensityChange = { density ->
                        scope.launch { themePreferencesRepository.setShapeDensity(density) }
                    },
                )
            }
        }
    }
}
