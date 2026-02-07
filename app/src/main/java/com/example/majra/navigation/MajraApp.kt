package com.example.majra.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.example.majra.AppDependencies
import com.example.majra.feed.ArticleDetailViewModel
import com.example.majra.feed.ArticleDetailViewModelFactory
import com.example.majra.feed.FeedViewModel
import com.example.majra.feed.FeedViewModelFactory
import com.example.majra.feed.SavedViewModel
import com.example.majra.feed.SavedViewModelFactory
import com.example.majra.feed.SourcesViewModel
import com.example.majra.feed.SourcesViewModelFactory
import com.example.majra.settings.AccentPalette
import com.example.majra.settings.ShapeDensity
import com.example.majra.settings.ThemeMode
import com.example.majra.settings.ThemePreferences
import com.example.majra.settings.TypographyScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MajraApp(
    appDependencies: AppDependencies,
    themePreferences: ThemePreferences,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentPaletteChange: (AccentPalette) -> Unit,
    onTypographyScaleChange: (TypographyScale) -> Unit,
    onShapeDensityChange: (ShapeDensity) -> Unit,
) {
    val topLevelDestinations = remember {
        listOf(
            TopLevelNavItem(
                key = Feed,
                label = "Feed",
                title = "Feed",
                icon = Icons.Filled.Home,
            ),
            TopLevelNavItem(
                key = Sources,
                label = "Sources",
                title = "Sources",
                icon = Icons.AutoMirrored.Filled.List,
            ),
            TopLevelNavItem(
                key = Saved,
                label = "Saved",
                title = "Saved",
                icon = Icons.Filled.Bookmark,
            ),
            TopLevelNavItem(
                key = Settings,
                label = "Settings",
                title = "Settings",
                icon = Icons.Filled.Settings,
            ),
        )
    }
    val topLevelByKey = remember(topLevelDestinations) {
        topLevelDestinations.associateBy { it.key }
    }

    // Each tab has its own back stack; switching tabs preserves history.
    val navigationState = rememberNavigationState(
        startRoute = Feed,
        topLevelRoutes = topLevelDestinations.map { it.key }.toSet(),
    )
    val navigator = remember { Navigator(navigationState) }

    // Entry provider maps keys on the back stack to composable content.
    val entryProvider = entryProvider<NavKey> {
        entry<Feed> {
            val viewModel = viewModel<FeedViewModel>(
                factory = FeedViewModelFactory(appDependencies.feedRepository),
            )
            val items = viewModel.items.collectAsState()
            FeedScreen(
                items = items.value,
                onContentSelected = { item ->
                    navigator.navigate(
                        ContentDetail(
                            contentId = item.id,
                            title = item.title,
                            sourceType = item.sourceType,
                        )
                    )
                },
            )
        }
        entry<Sources> {
            val viewModel = viewModel<SourcesViewModel>(
                factory = SourcesViewModelFactory(appDependencies.feedRepository),
            )
            val sources = viewModel.items.collectAsState()
            SourcesScreen(
                sources = sources.value,
                onSourceSelected = { item ->
                    navigator.navigate(
                        SourceDetail(
                            sourceId = item.id,
                            name = item.name,
                            type = item.type,
                        )
                    )
                },
            )
        }
        entry<Saved> {
            val viewModel = viewModel<SavedViewModel>(
                factory = SavedViewModelFactory(appDependencies.feedRepository),
            )
            val items = viewModel.items.collectAsState()
            SavedScreen(
                items = items.value,
                onContentSelected = { item ->
                    navigator.navigate(
                        ContentDetail(
                            contentId = item.id,
                            title = item.title,
                            sourceType = item.sourceType,
                        )
                    )
                },
            )
        }
        entry<Settings> {
            SettingsScreen(
                themePreferences = themePreferences,
                onThemeModeChange = onThemeModeChange,
                onAccentPaletteChange = onAccentPaletteChange,
                onTypographyScaleChange = onTypographyScaleChange,
                onShapeDensityChange = onShapeDensityChange,
            )
        }
        entry<ContentDetail> { key ->
            val viewModel = viewModel<ArticleDetailViewModel>(
                factory = ArticleDetailViewModelFactory(
                    repository = appDependencies.feedRepository,
                    articleId = key.contentId,
                ),
            )
            val state = viewModel.state.collectAsState()
            ContentDetailScreen(
                state = state.value,
                viewerRegistry = appDependencies.viewerRegistry,
                onToggleSaved = viewModel::toggleSaved,
                onMarkRead = viewModel::markRead,
            )
        }
        entry<SourceDetail> { key ->
            SourceDetailScreen(
                name = key.name,
                type = key.type,
            )
        }
    }

    Scaffold(
        topBar = {
            val currentStack = navigationState.currentBackStack()
            val currentKey = currentStack.last()
            val showBack = currentStack.size > 1
            TopAppBar(
                title = { Text(titleFor(currentKey, topLevelByKey)) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = { navigator.goBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = destination.key == navigationState.topLevelRoute,
                        onClick = { navigator.navigate(destination.key) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        }
    ) { padding ->
        MajraNavDisplay(
            padding = padding,
            entries = navigationState.toDecoratedEntries(entryProvider),
            onBack = { navigator.goBack() },
        )
    }
}

@Composable
private fun MajraNavDisplay(
    padding: PaddingValues,
    entries: List<NavEntry<NavKey>>,
    onBack: () -> Unit,
) {
    // NavDisplay renders the current back stack entries with transitions.
    NavDisplay(
        modifier = Modifier.padding(padding),
        entries = entries,
        onBack = onBack,
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { it / 6 },
                animationSpec = tween(durationMillis = 220)
            ) + fadeIn(animationSpec = tween(durationMillis = 220)) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { -it / 6 },
                    animationSpec = tween(durationMillis = 220)
                ) + fadeOut(animationSpec = tween(durationMillis = 140))
        },
        popTransitionSpec = {
            slideInHorizontally(
                initialOffsetX = { -it / 6 },
                animationSpec = tween(durationMillis = 200)
            ) + fadeIn(animationSpec = tween(durationMillis = 200)) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { it / 6 },
                    animationSpec = tween(durationMillis = 200)
                ) + fadeOut(animationSpec = tween(durationMillis = 120))
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(
                initialOffsetX = { -it / 6 },
                animationSpec = tween(durationMillis = 200)
            ) + fadeIn(animationSpec = tween(durationMillis = 200)) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { it / 6 },
                    animationSpec = tween(durationMillis = 200)
                ) + fadeOut(animationSpec = tween(durationMillis = 120))
        },
    )
}

private data class TopLevelNavItem(
    val key: MajraNavKey,
    val label: String,
    val title: String,
    val icon: ImageVector,
)

private fun titleFor(
    key: NavKey,
    topLevelByKey: Map<MajraNavKey, TopLevelNavItem>,
): String {
    return when (key) {
        is ContentDetail -> key.title
        is SourceDetail -> key.name
        is MajraNavKey -> topLevelByKey[key]?.title.orEmpty()
        else -> ""
    }
}
