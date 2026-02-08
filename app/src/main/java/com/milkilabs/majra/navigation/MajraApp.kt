package com.milkilabs.majra.navigation

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.milkilabs.majra.AppDependencies
import com.milkilabs.majra.feed.ArticleDetailViewModel
import com.milkilabs.majra.feed.ArticleDetailViewModelFactory
import com.milkilabs.majra.feed.FeedViewModel
import com.milkilabs.majra.feed.FeedViewModelFactory
import com.milkilabs.majra.feed.SavedViewModel
import com.milkilabs.majra.feed.SavedViewModelFactory
import com.milkilabs.majra.feed.ManageSourcesViewModel
import com.milkilabs.majra.feed.ManageSourcesViewModelFactory
import com.milkilabs.majra.settings.AccentPalette
import com.milkilabs.majra.settings.ShapeDensity
import com.milkilabs.majra.settings.ThemeMode
import com.milkilabs.majra.settings.ThemePreferences
import com.milkilabs.majra.settings.TypographyScale

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
    var isManageSourcesOpen by remember { mutableStateOf(false) }
    val syncViewModel = viewModel<ManageSourcesViewModel>(
        factory = ManageSourcesViewModelFactory(
            repository = appDependencies.feedRepository,
            rssSyncer = appDependencies.rssSyncer,
            youtubeSyncer = appDependencies.youtubeSyncer,
            mediumSyncer = appDependencies.mediumSyncer,
        ),
    )
    val syncStatus = syncViewModel.status.collectAsState()
    val syncSources = syncViewModel.sources.collectAsState()
    val isAddingSource = syncViewModel.isAdding.collectAsState()
    val addSourceError = syncViewModel.addError.collectAsState()

    // Entry provider maps keys on the back stack to composable content.
    val entryProvider = entryProvider<NavKey> {
        entry<Feed> {
            val viewModel = viewModel<FeedViewModel>(
                factory = FeedViewModelFactory(appDependencies.feedRepository),
            )
            val items = viewModel.items.collectAsState()
            val filters = viewModel.filters.collectAsState()
            val sources = viewModel.sourceItems.collectAsState()
            FeedScreen(
                items = items.value,
                filters = filters.value,
                sources = sources.value,
                syncStatus = syncStatus.value,
                onRefresh = syncViewModel::syncAll,
                onContentSelected = { item ->
                    navigator.navigate(
                        ContentDetail(
                            contentId = item.id,
                            sourceId = item.sourceId,
                            sourceType = item.sourceType,
                            sourceName = item.sourceName,
                        )
                    )
                },
                onCycleReadFilter = viewModel::cycleReadFilter,
                onResetReadFilter = viewModel::resetReadFilter,
                onSourceTypeSelected = viewModel::setSourceTypeFilter,
                onSourceSelected = viewModel::setSourceFilter,
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
                            sourceId = item.sourceId,
                            sourceType = item.sourceType,
                            sourceName = item.sourceName,
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
            LaunchedEffect(key.contentId) {
                viewModel.markRead()
            }
            ContentDetailScreen(
                state = state.value,
                viewerRegistry = appDependencies.viewerRegistry,
                onToggleSaved = viewModel::toggleSaved,
            )
        }
    }

    Scaffold(
        topBar = {
            val currentStack = navigationState.currentBackStack()
            val currentKey = currentStack.last()
            val sources = appDependencies.feedRepository.sources
                .collectAsState(initial = emptyList())
            val showBack = currentStack.size > 1
            var showMenu by remember { mutableStateOf(false) }
            val title = when (currentKey) {
                is ContentDetail -> {
                    val source = sources.value.firstOrNull { it.id == currentKey.sourceId }
                    val sourceName = source?.name
                        ?.ifBlank { source.url }
                        ?: currentKey.sourceName.ifBlank { "Unknown source" }
                    val sourceLabel = currentKey.sourceType.uppercase()
                    "$sourceLabel · $sourceName"
                }
                else -> titleFor(currentKey, topLevelByKey)
            }
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
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
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More",
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Manage sources") },
                            onClick = {
                                showMenu = false
                                isManageSourcesOpen = true
                            },
                        )
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

    if (isManageSourcesOpen) {
        ManageSourcesSheet(
            status = syncStatus.value,
            sources = syncSources.value,
            isAdding = isAddingSource.value,
            addErrorMessage = addSourceError.value,
            onDismiss = { isManageSourcesOpen = false },
            onSyncAll = syncViewModel::syncAll,
            onSyncSource = syncViewModel::syncSource,
            onAddSource = syncViewModel::addSource,
            onUpdateSource = syncViewModel::updateSource,
            onRemoveSource = syncViewModel::removeSource,
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
        is MajraNavKey -> topLevelByKey[key]?.title.orEmpty()
        else -> ""
    }
}
