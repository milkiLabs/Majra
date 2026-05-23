package com.milki.majra.media

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class VideoQuality(
    val label: String,
    val maxWidth: Int?,
    val maxHeight: Int?,
) {
    Auto("Auto", null, null),
    DataSaver("Data saver", 426, 240),
    Sd("SD", 854, 480),
    Hd("HD", 1280, 720),
    FullHd("Full HD", 1920, 1080),
}

data class VideoPlaybackState(
    val player: Player? = null,
    val activeMediaKey: String? = null,
    val isPlaying: Boolean = false,
    val isEnded: Boolean = false,
    val playbackSpeed: Float = 1f,
    val quality: VideoQuality = VideoQuality.Auto,
    val isInPictureInPicture: Boolean = false,
    val aspectRatio: Float = 1f,
    val isFullscreen: Boolean = false,
)

class VideoPlaybackController(context: Context) {
    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(VideoPlaybackState())
    val state: StateFlow<VideoPlaybackState> = _state

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying, isEnded = if (isPlaying) false else it.isEnded) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                _state.update { it.copy(isEnded = true, isPlaying = false) }
            }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _state.update { it.copy(playbackSpeed = playbackParameters.speed) }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            val ratio = if (videoSize.height > 0) {
                videoSize.width.toFloat() / videoSize.height.toFloat()
            } else {
                1f
            }
            _state.update { it.copy(aspectRatio = ratio.coerceIn(0.42f, 2.39f)) }
        }
    }

    init {
        val sessionToken = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val c = future.get()
                controller = c
                val ratio = if (c.videoSize.height > 0) {
                    c.videoSize.width.toFloat() / c.videoSize.height.toFloat()
                } else {
                    1f
                }
                _state.update {
                    it.copy(
                        player = c,
                        isPlaying = c.isPlaying,
                        playbackSpeed = c.playbackParameters.speed,
                        aspectRatio = ratio.coerceIn(0.42f, 2.39f)
                    )
                }
                c.addListener(playerListener)
                applyQuality(state.value.quality)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    fun play(mediaKey: String, url: String, title: String? = null, artist: String? = null) {
        val p = controller ?: return
        val current = state.value
        if (current.activeMediaKey != mediaKey) {
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(title ?: "Majra Video")
                .setArtist(artist ?: "Majra")
                .build()
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaId(mediaKey)
                .setMediaMetadata(mediaMetadata)
                .build()
            p.setMediaItem(mediaItem)
            p.prepare()
            _state.update { it.copy(activeMediaKey = mediaKey) }
        }
        p.setPlaybackSpeed(current.playbackSpeed)
        p.play()
    }

    fun toggle(mediaKey: String, url: String, title: String? = null, artist: String? = null) {
        val p = controller ?: return
        val current = state.value
        if (current.activeMediaKey == mediaKey && p.isPlaying) {
            p.pause()
        } else {
            if (current.isEnded) {
                p.seekTo(0)
                _state.update { it.copy(isEnded = false) }
            }
            play(mediaKey, url, title, artist)
        }
    }

    fun pauseCurrent(mediaKey: String) {
        val p = controller ?: return
        if (state.value.activeMediaKey == mediaKey && p.isPlaying) {
            p.pause()
        }
    }

    fun pause() {
        controller?.pause()
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3f)
        controller?.setPlaybackSpeed(clamped)
        _state.update { it.copy(playbackSpeed = clamped) }
    }

    fun setQuality(quality: VideoQuality) {
        applyQuality(quality)
        _state.update { it.copy(quality = quality) }
    }

    fun setIsInPictureInPicture(isInPictureInPicture: Boolean) {
        _state.update { it.copy(isInPictureInPicture = isInPictureInPicture) }
    }

    fun toggleFullscreen() {
        _state.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun setFullscreen(isFullscreen: Boolean) {
        _state.update { it.copy(isFullscreen = isFullscreen) }
    }

    fun hasActivePlayback(): Boolean = controller?.isPlaying ?: false

    fun release() {
        controller?.removeListener(playerListener)
        controller = null
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        controllerFuture = null
    }

    private fun applyQuality(quality: VideoQuality) {
        val p = controller ?: return
        val builder = p.trackSelectionParameters.buildUpon()
        if (quality.maxWidth == null || quality.maxHeight == null) {
            builder.clearVideoSizeConstraints()
        } else {
            builder.setMaxVideoSize(quality.maxWidth, quality.maxHeight)
        }
        p.trackSelectionParameters = builder.build()
    }
}
