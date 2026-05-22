package com.milki.majra.media

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
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
    val activeMediaKey: String? = null,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1f,
    val quality: VideoQuality = VideoQuality.Auto,
    val isInPictureInPicture: Boolean = false,
    val aspectRatio: Float = 1f,
)

class VideoPlaybackController(context: Context) {
    private val appContext = context.applicationContext

    val player: ExoPlayer = ExoPlayer.Builder(appContext)
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val mediaSession = MediaSession.Builder(appContext, player).build()

    private val _state = MutableStateFlow(VideoPlaybackState())
    val state: StateFlow<VideoPlaybackState> = _state

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
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
        })
        applyQuality(VideoQuality.Auto)
    }

    fun play(mediaKey: String, url: String) {
        val current = state.value
        if (current.activeMediaKey != mediaKey) {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            _state.update { it.copy(activeMediaKey = mediaKey) }
        }
        player.setPlaybackSpeed(current.playbackSpeed)
        player.play()
    }

    fun toggle(mediaKey: String, url: String) {
        if (state.value.activeMediaKey == mediaKey && player.isPlaying) {
            player.pause()
        } else {
            play(mediaKey, url)
        }
    }

    fun pauseCurrent(mediaKey: String) {
        if (state.value.activeMediaKey == mediaKey && player.isPlaying) {
            player.pause()
        }
    }

    fun pause() {
        player.pause()
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3f)
        player.setPlaybackSpeed(clamped)
        _state.update { it.copy(playbackSpeed = clamped) }
    }

    fun setQuality(quality: VideoQuality) {
        applyQuality(quality)
        _state.update { it.copy(quality = quality) }
    }

    fun setIsInPictureInPicture(isInPictureInPicture: Boolean) {
        _state.update { it.copy(isInPictureInPicture = isInPictureInPicture) }
    }

    fun hasActivePlayback(): Boolean = player.isPlaying

    fun release() {
        mediaSession.release()
        player.release()
    }

    private fun applyQuality(quality: VideoQuality) {
        val builder = player.trackSelectionParameters.buildUpon()
        if (quality.maxWidth == null || quality.maxHeight == null) {
            builder.clearVideoSizeConstraints()
        } else {
            builder.setMaxVideoSize(quality.maxWidth, quality.maxHeight)
        }
        player.trackSelectionParameters = builder.build()
    }
}
