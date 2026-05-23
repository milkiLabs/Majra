package com.milki.majra
import com.milki.majra.data.model.Platform

import android.Manifest
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import coil.compose.AsyncImage
import com.milki.majra.navigation.HomeRoute
import com.milki.majra.navigation.LoginRoute
import com.milki.majra.navigation.ProfileRoute
import com.milki.majra.navigation.ImageViewerRoute
import com.milki.majra.media.VideoPlaybackController
import com.milki.majra.ui.feed.FeedScreen
import com.milki.majra.ui.feed.FeedViewModel
import com.milki.majra.ui.feed.paginationLabel
import com.milki.majra.ui.feed.PlatformBadge
import com.milki.majra.ui.login.LoginScreen
import com.milki.majra.ui.login.PlatformLoginConfig
import com.milki.majra.ui.profile.ProfilePostsScreen
import com.milki.majra.ui.theme.MajraTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.PlayerView
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.milki.majra.ui.feed.VideoControls
import kotlinx.coroutines.delay
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

class MainActivity : ComponentActivity() {
    private val container by lazy { AppContainer(applicationContext) }
    private val videoPlaybackController by lazy { VideoPlaybackController(applicationContext) }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val feedViewModel: FeedViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FeedViewModel(container.repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        enableEdgeToEdge()
        setContent {
            MajraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MajraApp(
                        viewModel = feedViewModel,
                        container = container,
                        videoPlaybackController = videoPlaybackController,
                        onEnterPictureInPicture = ::enterPipForVideo,
                        onEnterFullscreen = ::enterFullscreen,
                        onExitFullscreen = ::exitFullscreen,
                    )
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        videoPlaybackController.setIsInPictureInPicture(isInPictureInPictureMode)
    }

    override fun onDestroy() {
        videoPlaybackController.release()
        super.onDestroy()
    }

    private fun enterPipForVideo() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !videoPlaybackController.hasActivePlayback()) {
            return
        }
        val ratio = videoPlaybackController.state.value.aspectRatio
        val width = (ratio * 1_000).toInt().coerceIn(420, 2_390)
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(width, 1_000))
            .build()
        enterPictureInPictureMode(params)
        videoPlaybackController.setIsInPictureInPicture(true)
    }

    private fun enterFullscreen() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        videoPlaybackController.setFullscreen(true)
    }

    private fun exitFullscreen() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.show(WindowInsetsCompat.Type.systemBars())
        videoPlaybackController.setFullscreen(false)
    }
}

@Composable
fun MajraApp(
    viewModel: FeedViewModel,
    container: AppContainer,
    videoPlaybackController: VideoPlaybackController,
    onEnterPictureInPicture: () -> Unit,
    onEnterFullscreen: () -> Unit,
    onExitFullscreen: () -> Unit,
) {
    val playbackState by videoPlaybackController.state.collectAsState()
    val isInPip = playbackState.isInPictureInPicture

    // In PiP mode: render ONLY the video player fullscreen, no app chrome
    if (isInPip && playbackState.activeMediaKey != null) {
        val player = playbackState.player
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    this.player = player
                }
            },
            update = { view ->
                if (view.player !== player) {
                    view.player = player
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    // Fullscreen mode: render only the video player with controls
    if (playbackState.isFullscreen && playbackState.activeMediaKey != null) {
        FullscreenVideoPlayer(
            videoPlaybackController = videoPlaybackController,
            onEnterPictureInPicture = onEnterPictureInPicture,
            onExitFullscreen = onExitFullscreen,
        )
        return
    }

    val state by viewModel.uiState.collectAsState()
    val backStack = remember { mutableStateListOf<Any>(HomeRoute) }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val isLoginRoute = backStack.lastOrNull() is LoginRoute

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isLoginRoute,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Profiles",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))

                    if (state.accounts.isEmpty()) {
                        Text(
                            text = "No profiles added yet.\nSync a username from the feed to get started.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        state.accounts.forEach { account ->
                            val isSelected = backStack.lastOrNull()
                                .let { it is ProfileRoute && it.platform == account.platform && it.accountId == account.accountId }

                            NavigationDrawerItem(
                                label = {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "@${account.username}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            PlatformBadge(platform = account.platform)
                                        }
                                        account.displayName?.let { name ->
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        Text(
                                            text = account.paginationLabel(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                icon = {
                                    AsyncImage(
                                        model = account.profilePicUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    // Pop back to home, then push the profile route
                                    while (backStack.size > 1) backStack.removeLast()
                                    backStack.add(ProfileRoute(account.platform, account.accountId, account.username))
                                },
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }
        },
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = { key ->
                when (key) {
                    is HomeRoute -> NavEntry(key) {
                        FeedScreen(
                            state = state,
                            videoPlaybackController = videoPlaybackController,
                            onLoginClick = { platform -> backStack.add(LoginRoute(platform)) },
                            onSyncClick = { platform, accountId, username -> viewModel.sync(platform, accountId, username) },
                            onLoadOlderClick = { platform, accountId -> viewModel.loadOlder(platform, accountId) },
                            onMessageShown = viewModel::dismissMessage,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onEnterPictureInPicture = onEnterPictureInPicture,
                            onEnterFullscreen = onEnterFullscreen,
                            onOpenImage = { url, caption -> backStack.add(ImageViewerRoute(url, caption)) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    is LoginRoute -> NavEntry(key) {
                        val config = PlatformLoginConfig.forPlatform(key.platform)
                        LoginScreen(
                            config = config,
                            onSessionCaptured = { cookie, userAgent ->
                                scope.launch {
                                    container.repository.saveSession(key.platform, cookie, userAgent)
                                    backStack.removeLastOrNull()
                                }
                            },
                            onCancel = { backStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    is ImageViewerRoute -> NavEntry(key) {
                        FullscreenImageViewer(
                            imageUrl = key.imageUrl,
                            caption = key.caption,
                            onClose = { backStack.removeLastOrNull() },
                        )
                    }

                    is ProfileRoute -> NavEntry(key) {
                        val profilePosts by viewModel.postsForAccount(key.platform, key.accountId)
                            .collectAsState(initial = emptyList())
                        val account = state.accounts.find { it.platform == key.platform && it.accountId == key.accountId }
                        val sourceKey = "${key.platform.storageKey}:${key.accountId}"

                        ProfilePostsScreen(
                            username = key.username,
                            account = account,
                            posts = profilePosts,
                            videoPlaybackController = videoPlaybackController,
                            isSyncing = state.syncingSourceKey == sourceKey,
                            isLoadingOlder = state.loadingOlderSourceKey == sourceKey,
                            message = state.message,
                            onBack = { backStack.removeLastOrNull() },
                            onSyncClick = { viewModel.sync(key.platform, key.accountId, key.username) },
                            onLoadOlderClick = { viewModel.loadOlder(key.platform, key.accountId) },
                            onMessageShown = viewModel::dismissMessage,
                            onEnterPictureInPicture = onEnterPictureInPicture,
                            onEnterFullscreen = onEnterFullscreen,
                            onOpenImage = { url, caption -> backStack.add(ImageViewerRoute(url, caption)) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    else -> error("Unknown route: $key")
                }
            },
        )
    }
}

@Composable
private fun FullscreenVideoPlayer(
    videoPlaybackController: VideoPlaybackController,
    onEnterPictureInPicture: () -> Unit,
    onExitFullscreen: () -> Unit,
) {
    val playbackState by videoPlaybackController.state.collectAsState()
    val mediaKey = playbackState.activeMediaKey ?: return

    var controlsVisible by remember { mutableStateOf(true) }
    var hideTimestamp by remember { mutableLongStateOf(0L) }

    LaunchedEffect(hideTimestamp) {
        if (controlsVisible) {
            delay(3_000)
            controlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                controlsVisible = !controlsVisible
                if (controlsVisible) hideTimestamp = System.nanoTime()
            },
    ) {
        BackHandler(onBack = onExitFullscreen)
        val player = playbackState.player
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    this.player = player
                }
            },
            update = { view ->
                if (view.player !== player) {
                    view.player = player
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        VideoControls(
            mediaKey = mediaKey,
            videoUrl = mediaKey,
            videoPlaybackController = videoPlaybackController,
            onEnterPictureInPicture = onEnterPictureInPicture,
            onToggleFullscreen = onExitFullscreen,
            visible = controlsVisible,
            onInteraction = { hideTimestamp = System.nanoTime() },
            username = "",
            caption = null,
        )

        if (controlsVisible) {
            IconButton(
                onClick = onExitFullscreen,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Exit fullscreen",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun FullscreenImageViewer(
    imageUrl: String,
    caption: String?,
    onClose: () -> Unit,
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    BackHandler(onBack = onClose)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = caption,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                ),
            contentScale = ContentScale.Fit,
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Close",
                tint = Color.White,
            )
        }
    }
}
