package com.milki.majra.ui.feed

import com.milki.majra.data.model.Platform
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.milki.majra.data.model.FeedItem
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.milki.majra.data.model.PostMediaItem
import com.milki.majra.data.model.SocialPost
import com.milki.majra.data.model.SocialProfile
import com.milki.majra.media.VideoPlaybackController
import com.milki.majra.media.VideoQuality
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    state: FeedUiState,
    videoPlaybackController: VideoPlaybackController,
    onSyncClick: (Platform, String, String) -> Unit,
    onLoadOlderClick: (Platform, String) -> Unit,
    onMessageShown: () -> Unit,
    onOpenDrawer: () -> Unit,
    onEnterPictureInPicture: () -> Unit,
    onEnterFullscreen: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var username by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(20.dp),
        ) {
            item {
                HeaderCard(
                    isAuthenticated = state.isAuthenticated,
                    username = username,
                    isBusy = state.isBusy,
                    onUsernameChange = { username = it },
                    onLoginClick = onLoginClick,
                    onSyncClick = { onSyncClick(Platform.INSTAGRAM, username, username) },
                    onOpenDrawer = onOpenDrawer,
                )
            }

            if (state.accounts.isNotEmpty()) {
                item {
                    SourceShelf(
                        accounts = state.accounts,
                        syncingSourceKey = state.syncingSourceKey,
                        loadingOlderSourceKey = state.loadingOlderSourceKey,
                        onSyncClick = onSyncClick,
                        onLoadOlderClick = onLoadOlderClick,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
            }

            if (state.feed.isEmpty()) {
                item { EmptyFeedCard(state.isAuthenticated) }
            } else {
                items(state.feed, key = { "${it.post.platform.storageKey}:${it.post.id}" }) { item ->
                    PostCard(
                        item = item,
                        videoPlaybackController = videoPlaybackController,
                        onEnterPictureInPicture = onEnterPictureInPicture,
                        onToggleFullscreen = onEnterFullscreen,
                    )
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun HeaderCard(
    isAuthenticated: Boolean,
    username: String,
    isBusy: Boolean,
    onUsernameChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onSyncClick: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open profiles drawer",
                    )
                }
                Text(
                    text = "Majra",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = "A private, intentional Instagram reader. Add only accounts you truly want to read, sync manually, and stay out of the algorithmic feed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!isAuthenticated) {
                Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign in to Instagram")
                }
            }
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                enabled = isAuthenticated && !isBusy,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Username") },
                prefix = { Text("@") },
                supportingText = { Text("Manual sync only. No endless background feed.") },
            )
            Button(
                enabled = isAuthenticated && !isBusy,
                onClick = onSyncClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Fetch latest posts")
                }
            }
        }
    }
}

@Composable
private fun SourceShelf(
    accounts: List<SocialProfile>,
    syncingSourceKey: String?,
    loadingOlderSourceKey: String?,
    onSyncClick: (Platform, String, String) -> Unit,
    onLoadOlderClick: (Platform, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Sources",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${accounts.size} tracked",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(accounts, key = { "${it.platform.storageKey}:${it.accountId}" }) { account ->
                val sourceKey = "${account.platform.storageKey}:${account.accountId}"
                SourceCard(
                    account = account,
                    isSyncing = syncingSourceKey == sourceKey,
                    isLoadingOlder = loadingOlderSourceKey == sourceKey,
                    isAnyBusy = syncingSourceKey != null || loadingOlderSourceKey != null,
                    onSyncClick = { onSyncClick(account.platform, account.accountId, account.username) },
                    onLoadOlderClick = { onLoadOlderClick(account.platform, account.accountId) },
                )
            }
        }
    }
}

@Composable
private fun SourceCard(
    account: SocialProfile,
    isSyncing: Boolean,
    isLoadingOlder: Boolean,
    isAnyBusy: Boolean,
    onSyncClick: () -> Unit,
    onLoadOlderClick: () -> Unit,
) {
    Card(
        modifier = Modifier.width(248.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AsyncImage(
                    model = account.profilePicUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "@${account.username}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = account.paginationLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onSyncClick,
                    enabled = !isAnyBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Latest")
                    }
                }
                Button(
                    onClick = onLoadOlderClick,
                    enabled = account.hasMorePosts && !isAnyBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isLoadingOlder) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Older")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFeedCard(isAuthenticated: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (isAuthenticated) "Your quiet feed is empty" else "Start with one deliberate sign-in",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (isAuthenticated) {
                    "Enter a username and fetch posts. Majra saves them locally for calm reading later."
                } else {
                    "After sign-in, Majra captures your session cookie locally and closes the WebView."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun PostCard(
    item: FeedItem,
    videoPlaybackController: VideoPlaybackController,
    onEnterPictureInPicture: () -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    Card(shape = RoundedCornerShape(28.dp)) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AsyncImage(
                    model = item.account.profilePicUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "@${item.account.username}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    item.account.displayName?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    text = item.post.timestampSeconds.formatTimestamp(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PostMedia(
                post = item.post,
                username = item.account.username,
                videoPlaybackController = videoPlaybackController,
                onEnterPictureInPicture = onEnterPictureInPicture,
                onToggleFullscreen = onToggleFullscreen,
            )
            if (item.post.caption.isNotBlank()) {
                Text(
                    text = item.post.caption,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
internal fun PostMedia(
    post: SocialPost,
    username: String,
    videoPlaybackController: VideoPlaybackController,
    onEnterPictureInPicture: () -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    val items = post.mediaItems

    if (items.size > 1) {
        val pagerState = rememberPagerState(pageCount = { items.size })
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = items[page]
                PagerMediaItem(
                    item = item,
                    caption = post.caption,
                    username = username,
                    videoPlaybackController = videoPlaybackController,
                    onEnterPictureInPicture = onEnterPictureInPicture,
                    onToggleFullscreen = onToggleFullscreen,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(100))
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${items.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            PageIndicator(
                pageCount = items.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
            )
        }
    } else if (items.isNotEmpty()) {
        val item = items.first()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            PagerMediaItem(
                item = item,
                caption = post.caption,
                username = username,
                videoPlaybackController = videoPlaybackController,
                onEnterPictureInPicture = onEnterPictureInPicture,
                onToggleFullscreen = onToggleFullscreen,
            )
        }
    }
}

@Composable
internal fun PagerMediaItem(
    item: PostMediaItem,
    caption: String?,
    username: String,
    videoPlaybackController: VideoPlaybackController,
    onEnterPictureInPicture: () -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    val playbackState by videoPlaybackController.state.collectAsState()
    val mediaKey = item.videoUrl.orEmpty()
    val isActiveVideo = item.isVideo && playbackState.activeMediaKey == mediaKey
    val player = playbackState.player

    if (item.isVideo && isActiveVideo) {
        var controlsVisible by remember { mutableStateOf(true) }
        var hideTimestamp by remember { mutableLongStateOf(0L) }

        // Auto-hide controls after 3 seconds
        LaunchedEffect(hideTimestamp) {
            if (controlsVisible) {
                delay(3_000)
                controlsVisible = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pauseWhenMostlyHidden(mediaKey, videoPlaybackController)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    controlsVisible = !controlsVisible
                    if (controlsVisible) hideTimestamp = System.nanoTime()
                },
        ) {
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
                videoUrl = item.videoUrl.orEmpty(),
                videoPlaybackController = videoPlaybackController,
                onEnterPictureInPicture = onEnterPictureInPicture,
                onToggleFullscreen = onToggleFullscreen,
                visible = controlsVisible,
                onInteraction = { hideTimestamp = System.nanoTime() },
                username = username,
                caption = caption,
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (item.isVideo) {
                        Modifier.clickable {
                            item.videoUrl?.let { url ->
                                videoPlaybackController.play(
                                    mediaKey = mediaKey,
                                    url = url,
                                    title = "@$username",
                                    artist = caption
                                )
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = caption?.takeIf { it.isNotBlank() },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (item.isVideo) {
                Card(
                    onClick = {
                        item.videoUrl?.let { url ->
                            videoPlaybackController.play(
                                mediaKey = mediaKey,
                                url = url,
                                title = "@$username",
                                artist = caption
                            )
                        }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = "Play Video",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
internal fun VideoControls(
    mediaKey: String,
    videoUrl: String,
    videoPlaybackController: VideoPlaybackController,
    onEnterPictureInPicture: () -> Unit,
    onToggleFullscreen: () -> Unit,
    visible: Boolean,
    onInteraction: () -> Unit,
    username: String,
    caption: String?,
) {
    val playbackState by videoPlaybackController.state.collectAsState()
    val player = playbackState.player
    var speedMenuOpen by remember { mutableStateOf(false) }
    var qualityMenuOpen by remember { mutableStateOf(false) }

    // Track position for the seekbar
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }

    // Poll player position while playing
    LaunchedEffect(playbackState.isPlaying, playbackState.activeMediaKey, player) {
        while (true) {
            if (player != null) {
                if (!isSeeking) {
                    positionMs = player.currentPosition.coerceAtLeast(0L)
                }
                durationMs = player.duration.let { if (it < 0) 0L else it }
            }
            delay(250)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient scrim at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        )
                    )
            )

            // Center play/pause button
            IconButton(
                onClick = {
                    videoPlaybackController.toggle(
                        mediaKey = mediaKey,
                        url = videoUrl,
                        title = "@$username",
                        artist = caption
                    )
                    onInteraction()
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
            ) {
                Icon(
                    imageVector = when {
                        playbackState.isEnded -> Icons.Default.Replay
                        playbackState.isPlaying -> Icons.Default.Pause
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = when {
                        playbackState.isEnded -> "Replay"
                        playbackState.isPlaying -> "Pause"
                        else -> "Play"
                    },
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp),
            ) {
                // Seekbar
                val sliderPosition = if (isSeeking) {
                    seekValue
                } else {
                    if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f
                }
                Slider(
                    value = sliderPosition,
                    onValueChange = { value ->
                        isSeeking = true
                        seekValue = value
                        onInteraction()
                    },
                    onValueChangeFinished = {
                        val seekTo = (seekValue * durationMs).toLong()
                        player?.seekTo(seekTo)
                        positionMs = seekTo
                        isSeeking = false
                        onInteraction()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                    ),
                )

                // Time + controls row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Timestamp
                    Text(
                        text = "${formatDuration(positionMs)} / ${formatDuration(durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Speed
                    Box {
                        TextButton(onClick = {
                            speedMenuOpen = true
                            onInteraction()
                        }) {
                            Text(
                                text = "${playbackState.playbackSpeed.formatSpeed()}x",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        DropdownMenu(
                            expanded = speedMenuOpen,
                            onDismissRequest = { speedMenuOpen = false },
                        ) {
                            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed.formatSpeed()}x") },
                                    onClick = {
                                        videoPlaybackController.setPlaybackSpeed(speed)
                                        speedMenuOpen = false
                                        onInteraction()
                                    },
                                )
                            }
                        }
                    }

                    // Quality
                    Box {
                        TextButton(onClick = {
                            qualityMenuOpen = true
                            onInteraction()
                        }) {
                            Text(
                                text = playbackState.quality.label,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        DropdownMenu(
                            expanded = qualityMenuOpen,
                            onDismissRequest = { qualityMenuOpen = false },
                        ) {
                            VideoQuality.entries.forEach { quality ->
                                DropdownMenuItem(
                                    text = { Text(quality.label) },
                                    onClick = {
                                        videoPlaybackController.setQuality(quality)
                                        qualityMenuOpen = false
                                        onInteraction()
                                    },
                                )
                            }
                        }
                    }

                    // PiP
                    TextButton(onClick = {
                        onEnterPictureInPicture()
                        onInteraction()
                    }) {
                        Text(
                            text = "PiP",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Fullscreen
                    TextButton(onClick = {
                        onToggleFullscreen()
                        onInteraction()
                    }) {
                        Text(
                            text = if (playbackState.isFullscreen) "Exit FS" else "Fullscreen",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1_000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun Modifier.pauseWhenMostlyHidden(
    mediaKey: String,
    videoPlaybackController: VideoPlaybackController,
): Modifier = onGloballyPositioned { coordinates ->
    val playbackState = videoPlaybackController.state.value
    if (playbackState.activeMediaKey != mediaKey || playbackState.isInPictureInPicture) {
        return@onGloballyPositioned
    }
    val visibleFraction = coordinates.visibleFractionInRoot()
    if (visibleFraction < 0.4f) {
        videoPlaybackController.pauseCurrent(mediaKey)
    }
}

private fun androidx.compose.ui.layout.LayoutCoordinates.visibleFractionInRoot(): Float {
    val rootSize = findRootCoordinates().size
    val bounds = boundsInRoot()
    val rootBounds = Rect(
        left = 0f,
        top = 0f,
        right = rootSize.width.toFloat(),
        bottom = rootSize.height.toFloat(),
    )
    val left = max(bounds.left, rootBounds.left)
    val top = max(bounds.top, rootBounds.top)
    val right = min(bounds.right, rootBounds.right)
    val bottom = min(bounds.bottom, rootBounds.bottom)
    val visibleArea = max(0f, right - left) * max(0f, bottom - top)
    val totalArea = bounds.width * bounds.height
    return if (totalArea <= 0f) 0f else visibleArea / totalArea
}

private fun Float.formatSpeed(): String {
    val asInt = toInt()
    return if (this == asInt.toFloat()) asInt.toString() else toString()
}

@Composable
internal fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100))
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            }
            val size = if (isSelected) 8.dp else 6.dp
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

internal fun Long.formatTimestamp(): String {
    if (this <= 0L) return ""
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(this * 1_000))
}

internal fun SocialProfile.paginationLabel(): String = when {
    lastSyncedAtMillis <= 0L -> "Ready to sync"
    hasMorePosts -> "Older posts available"
    else -> "Archive complete"
}
