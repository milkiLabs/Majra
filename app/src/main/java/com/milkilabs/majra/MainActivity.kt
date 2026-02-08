package com.milkilabs.majra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.milkilabs.majra.navigation.MajraApp
import com.milkilabs.majra.settings.SyncPreferences
import com.milkilabs.majra.settings.SyncPreferencesRepository
import com.milkilabs.majra.settings.syncPreferencesDataStore
import com.milkilabs.majra.settings.ThemePreferences
import com.milkilabs.majra.settings.ThemePreferencesRepository
import com.milkilabs.majra.settings.themePreferencesDataStore
import com.milkilabs.majra.sync.SyncWorkScheduler
import com.milkilabs.majra.ui.theme.MajraTheme

class MainActivity : ComponentActivity() {
    private val themePreferencesRepository by lazy {
        ThemePreferencesRepository(applicationContext.themePreferencesDataStore)
    }
    private val syncPreferencesRepository by lazy {
        SyncPreferencesRepository(applicationContext.syncPreferencesDataStore)
    }
    // Keep the splash screen visible until app dependencies are ready.
    private var isAppReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !isAppReady }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePreferences by themePreferencesRepository.themePreferences.collectAsState(
                initial = ThemePreferences(),
            )
            val syncPreferences by syncPreferencesRepository.syncPreferences.collectAsState(
                initial = SyncPreferences(),
            )
            val scope = rememberCoroutineScope()
            val appDependencies by produceState<AppDependencies?>(initialValue = null) {
                value = withContext(Dispatchers.IO) {
                    AppDependencies.createDefault(applicationContext)
                }
            }

            MajraTheme(
                themePreferences = themePreferences,
            ) {
                LaunchedEffect(appDependencies) {
                    if (appDependencies != null) {
                        isAppReady = true
                    }
                }
                if (appDependencies == null) {
                    LoadingScreen()
                } else {
                    MajraApp(
                        appDependencies = appDependencies!!,
                        themePreferences = themePreferences,
                        syncPreferences = syncPreferences,
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
                        onBackgroundSyncToggle = { enabled ->
                            scope.launch {
                                syncPreferencesRepository.setBackgroundSyncEnabled(enabled)
                            }
                        },
                        onSyncIntervalChange = { hours ->
                            scope.launch {
                                syncPreferencesRepository.setSyncIntervalHours(hours)
                            }
                        },
                        onNotifyToggle = { enabled ->
                            scope.launch {
                                syncPreferencesRepository.setNotifyOnNewItems(enabled)
                            }
                        },
                    )
                }
            }

            LaunchedEffect(syncPreferences) {
                SyncWorkScheduler.updateSchedule(
                    context = applicationContext,
                    preferences = syncPreferences,
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Loading...",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
