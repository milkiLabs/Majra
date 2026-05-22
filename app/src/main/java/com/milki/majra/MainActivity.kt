package com.milki.majra
import com.milki.majra.data.model.Platform

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.milki.majra.media.VideoPlaybackController
import com.milki.majra.ui.feed.FeedScreen
import com.milki.majra.ui.feed.FeedViewModel
import com.milki.majra.ui.feed.paginationLabel
import com.milki.majra.ui.login.LoginScreen
import com.milki.majra.ui.profile.ProfilePostsScreen
import com.milki.majra.ui.theme.MajraTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView

class MainActivity : ComponentActivity() {
    private val container by lazy { AppContainer(applicationContext) }
    private val videoPlaybackController by lazy { VideoPlaybackController(applicationContext) }

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
                    )
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        if (videoPlaybackController.hasActivePlayback()) {
            enterPipForVideo()
        }
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
}

@Composable
fun MajraApp(
    viewModel: FeedViewModel,
    container: AppContainer,
    videoPlaybackController: VideoPlaybackController,
    onEnterPictureInPicture: () -> Unit,
) {
    val playbackState by videoPlaybackController.state.collectAsState()
    val isInPip = playbackState.isInPictureInPicture

    // In PiP mode: render ONLY the video player fullscreen, no app chrome
    if (isInPip && playbackState.activeMediaKey != null) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    player = videoPlaybackController.player
                }
            },
            update = { view ->
                if (view.player !== videoPlaybackController.player) {
                    view.player = videoPlaybackController.player
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    val state by viewModel.uiState.collectAsState()
    val backStack = remember { mutableStateListOf<Any>(HomeRoute) }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
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
                                        Text(
                                            text = "@${account.username}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
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
                            onLoginClick = { backStack.add(LoginRoute) },
                            onSyncClick = { platform, accountId, username -> viewModel.sync(platform, accountId, username) },
                            onLoadOlderClick = { platform, accountId -> viewModel.loadOlder(platform, accountId) },
                            onMessageShown = viewModel::dismissMessage,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onEnterPictureInPicture = onEnterPictureInPicture,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    is LoginRoute -> NavEntry(key) {
                        LoginScreen(
                            onSessionCaptured = { cookie, userAgent ->
                                scope.launch {
                                    container.repository.saveSession(com.milki.majra.data.model.Platform.INSTAGRAM, cookie, userAgent)
                                    backStack.removeLastOrNull()
                                }
                            },
                            onCancel = { backStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize(),
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
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    else -> error("Unknown route: $key")
                }
            },
        )
    }
}
