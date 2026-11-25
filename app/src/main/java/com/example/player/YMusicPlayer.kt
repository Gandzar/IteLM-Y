package com.example.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.example.data.Track
import com.example.data.TrackSource
import dev.bagja.spatialaudio.audio.FloatingBubbleService
import dev.bagja.spatialaudio.audio.MixerProcessor
import dev.bagja.spatialaudio.audio.SpatialHostBridge
import dev.bagja.spatialaudio.data.MixerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PlayerMode {
    NONE,
    AUDIO,
    YOUTUBE_IFRAME,
}

data class PlayerState(
    val track: Track? = null,
    val playing: Boolean = false,
    val currentTimeSec: Float = 0f,
    val durationSec: Float = 0f,
    val mode: PlayerMode = PlayerMode.NONE,
    val youtubeExpanded: Boolean = true,
)

@OptIn(UnstableApi::class)
class YMusicPlayer(private val context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val mixerProcessor = MixerProcessor(appContext)
    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    // Callback hooks when YouTube IFrame WebView is active
    var onYoutubeToggle: (() -> Unit)? = null
    var onYoutubePause: (() -> Unit)? = null
    var onYoutubeSeek: ((Float) -> Unit)? = null

    init {
        MixerRepository.init(appContext)
        initExoPlayer()
        SpatialHostBridge.onTogglePlayback = { toggle() }
        SpatialHostBridge.onPausePlayback = { pause() }
    }

    private fun initExoPlayer() {
        val renderersFactory = object : DefaultRenderersFactory(appContext) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf(mixerProcessor))
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .build()
        }

        exoPlayer = ExoPlayer.Builder(appContext, renderersFactory)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (_state.value.mode == PlayerMode.AUDIO) {
                            _state.update { it.copy(playing = isPlaying) }
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (_state.value.mode == PlayerMode.AUDIO) {
                            val dur = duration.takeIf { it > 0 }?.div(1000f) ?: _state.value.durationSec
                            if (playbackState == Player.STATE_ENDED) {
                                _state.update { it.copy(playing = false, currentTimeSec = 0f, durationSec = dur) }
                            } else {
                                _state.update { it.copy(durationSec = dur) }
                            }
                        }
                    }
                })
            }
    }

    fun play(track: Track) {
        exoPlayer?.pause()
        exoPlayer?.clearMediaItems()
        progressJob?.cancel()

        SpatialHostBridge.currentTitle.value = track.title
        SpatialHostBridge.currentLyrics.value = track.artist
        if (MixerRepository.floatingLyricsEnabled.value) {
            FloatingBubbleService.syncLyrics(appContext)
        }

        if (track.source == TrackSource.YOUTUBE && !track.videoId.isNullOrBlank()) {
            _state.value = PlayerState(
                track = track,
                playing = true,
                currentTimeSec = 0f,
                durationSec = track.duration.toFloat(),
                mode = PlayerMode.YOUTUBE_IFRAME,
                youtubeExpanded = true,
            )
            return
        }

        val mediaUrl = track.url
        if (!mediaUrl.isNullOrBlank()) {
            _state.value = PlayerState(
                track = track,
                playing = true,
                currentTimeSec = 0f,
                durationSec = track.duration.toFloat(),
                mode = PlayerMode.AUDIO,
            )
            exoPlayer?.apply {
                setMediaItem(MediaItem.fromUri(mediaUrl))
                prepare()
                playWhenReady = true
            }
            startAudioProgressPolling()
            return
        }

        _state.value = PlayerState(track = track, mode = PlayerMode.NONE)
    }

    private fun startAudioProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _state.value.mode == PlayerMode.AUDIO) {
                exoPlayer?.let { p ->
                    val pos = (p.currentPosition / 1000f).coerceAtLeast(0f)
                    val dur = p.duration.takeIf { it > 0 }?.div(1000f) ?: _state.value.durationSec
                    _state.update { it.copy(currentTimeSec = pos, durationSec = dur, playing = p.isPlaying) }
                }
                delay(400)
            }
        }
    }

    fun toggle() {
        val s = _state.value
        if (s.track == null) return
        when (s.mode) {
            PlayerMode.YOUTUBE_IFRAME -> onYoutubeToggle?.invoke()
            PlayerMode.AUDIO -> exoPlayer?.let { p ->
                if (p.isPlaying) p.pause() else p.play()
            }
            PlayerMode.NONE -> Unit
        }
    }

    fun pause() {
        when (_state.value.mode) {
            PlayerMode.YOUTUBE_IFRAME -> onYoutubePause?.invoke()
            PlayerMode.AUDIO -> exoPlayer?.pause()
            PlayerMode.NONE -> Unit
        }
    }

    fun seek(seconds: Float) {
        when (_state.value.mode) {
            PlayerMode.YOUTUBE_IFRAME -> onYoutubeSeek?.invoke(seconds)
            PlayerMode.AUDIO -> exoPlayer?.seekTo((seconds * 1000f).toLong().coerceAtLeast(0L))
            PlayerMode.NONE -> Unit
        }
    }

    fun toggleYoutubeExpanded() {
        _state.update { it.copy(youtubeExpanded = !it.youtubeExpanded) }
    }

    fun updateYoutubePlaybackState(playing: Boolean, currentSec: Float, durationSec: Float) {
        if (_state.value.mode != PlayerMode.YOUTUBE_IFRAME) return
        _state.update {
            it.copy(
                playing = playing,
                currentTimeSec = currentSec.coerceAtLeast(0f),
                durationSec = if (durationSec > 0f) durationSec else it.durationSec,
            )
        }
    }

    fun stop() {
        progressJob?.cancel()
        exoPlayer?.pause()
        exoPlayer?.clearMediaItems()
        _state.value = PlayerState()
        SpatialHostBridge.currentTitle.value = null
        SpatialHostBridge.currentLyrics.value = null
    }

    fun release() {
        progressJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        SpatialHostBridge.onTogglePlayback = null
        SpatialHostBridge.onPausePlayback = null
    }
}
