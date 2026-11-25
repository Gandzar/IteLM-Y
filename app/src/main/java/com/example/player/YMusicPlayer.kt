package com.example.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.data.ResolvedStreams
import com.example.data.Track
import com.example.data.TrackSource
import com.example.engine.InnerTubeClient
import com.example.engine.MediaScraper
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
    val resolvingStream: Boolean = false,
    val canSwitchToVideo: Boolean = false,
    val audioCodecLabel: String = "",
    val resumeSeekSec: Float = 0f,
)

@OptIn(UnstableApi::class)
class YMusicPlayer(
    private val context: Context,
    private val authHeadersProvider: () -> Map<String, String> = { emptyMap() },
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val mixerProcessor = MixerProcessor(appContext)
    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null
    private var resolveJob: Job? = null
    private var cachedResolved: ResolvedStreams? = null
    private var pendingSeekSec: Float = 0f

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

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        exoPlayer = ExoPlayer.Builder(appContext, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
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
                            if (playbackState == Player.STATE_READY && pendingSeekSec > 0f) {
                                val targetMs = (pendingSeekSec * 1000f).toLong().coerceAtLeast(0L)
                                pendingSeekSec = 0f
                                seekTo(targetMs)
                            }
                            if (playbackState == Player.STATE_ENDED) {
                                _state.update { it.copy(playing = false, currentTimeSec = 0f, durationSec = dur) }
                            } else {
                                _state.update { it.copy(durationSec = dur) }
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        val s = _state.value
                        val videoId = s.track?.videoId
                        if (s.mode == PlayerMode.AUDIO && !videoId.isNullOrBlank()) {
                            // Automatic fallback to in-app YouTube IFrame Video Player
                            fallbackToYoutubeIframe(s.track, s.currentTimeSec)
                        } else {
                            _state.update { it.copy(playing = false, resolvingStream = false) }
                        }
                    }
                })
            }
    }

    fun play(track: Track) {
        resolveJob?.cancel()
        progressJob?.cancel()
        exoPlayer?.pause()
        exoPlayer?.clearMediaItems()
        pendingSeekSec = 0f

        SpatialHostBridge.currentTitle.value = track.title
        SpatialHostBridge.currentLyrics.value = track.artist
        if (MixerRepository.floatingLyricsEnabled.value) {
            FloatingBubbleService.syncLyrics(appContext)
        }

        val directUrl = track.url?.takeIf { it.isNotBlank() && MediaScraper.isDirectMediaUrl(it) }
        val hasVideoId = !track.videoId.isNullOrBlank()

        if (directUrl != null) {
            playAudioUri(
                track = track,
                streamUrl = directUrl,
                userAgent = "YMusic-Lite/0.3.0",
                codecLabel = "Direct · 8D DSP",
                canSwitchToVideo = hasVideoId,
                seekToSec = 0f,
            )
            return
        }

        if (track.source == TrackSource.YOUTUBE && hasVideoId) {
            val vid = track.videoId!!
            _state.value = PlayerState(
                track = track,
                playing = false,
                currentTimeSec = 0f,
                durationSec = track.duration.toFloat(),
                mode = PlayerMode.AUDIO,
                youtubeExpanded = false,
                resolvingStream = true,
                canSwitchToVideo = true,
                audioCodecLabel = "Ekstrak audio…",
            )
            resolveAndPlayAudio(track, vid, startSec = 0f)
            return
        }

        val fallbackUrl = track.url
        if (!fallbackUrl.isNullOrBlank()) {
            playAudioUri(
                track = track,
                streamUrl = fallbackUrl,
                userAgent = "YMusic-Lite/0.3.0",
                codecLabel = "Audio · 8D DSP",
                canSwitchToVideo = false,
                seekToSec = 0f,
            )
            return
        }

        _state.value = PlayerState(track = track, mode = PlayerMode.NONE)
    }

    private fun resolveAndPlayAudio(track: Track, videoId: String, startSec: Float) {
        resolveJob?.cancel()
        resolveJob = scope.launch {
            val cached = cachedResolved?.takeIf { it.videoId == videoId }
            val resolved = cached ?: runCatching {
                InnerTubeClient.resolveStreams(videoId, authHeadersProvider())
            }.getOrNull()

            if (resolved != null) {
                cachedResolved = resolved
            }

            // Ensure user hasn't switched track while resolving
            if (_state.value.track?.id != track.id) return@launch

            val bestAudio = resolved?.bestAudio
            if (bestAudio != null) {
                val enrichedTrack = track.copy(
                    duration = if (track.duration > 0) track.duration else (resolved.durationSec),
                    thumbnail = track.thumbnail.ifBlank { resolved.thumbnail },
                    url = bestAudio.url,
                )
                playAudioUri(
                    track = enrichedTrack,
                    streamUrl = bestAudio.url,
                    userAgent = bestAudio.userAgent.ifBlank { "YMusic-Lite/0.3.0" },
                    codecLabel = "${bestAudio.qualityLabel} · 8D DSP",
                    canSwitchToVideo = true,
                    seekToSec = startSec,
                )
            } else {
                fallbackToYoutubeIframe(track, startSec)
            }
        }
    }

    private fun playAudioUri(
        track: Track,
        streamUrl: String,
        userAgent: String,
        codecLabel: String,
        canSwitchToVideo: Boolean,
        seekToSec: Float,
    ) {
        pendingSeekSec = seekToSec.coerceAtLeast(0f)
        _state.value = PlayerState(
            track = track,
            playing = true,
            currentTimeSec = seekToSec,
            durationSec = track.duration.toFloat(),
            mode = PlayerMode.AUDIO,
            youtubeExpanded = false,
            resolvingStream = false,
            canSwitchToVideo = canSwitchToVideo,
            audioCodecLabel = codecLabel,
        )

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(streamUrl))

        exoPlayer?.apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
        startAudioProgressPolling()
    }

    private fun fallbackToYoutubeIframe(track: Track, startSec: Float) {
        progressJob?.cancel()
        exoPlayer?.pause()
        exoPlayer?.clearMediaItems()
        _state.value = PlayerState(
            track = track,
            playing = true,
            currentTimeSec = startSec,
            durationSec = track.duration.toFloat(),
            mode = PlayerMode.YOUTUBE_IFRAME,
            youtubeExpanded = true,
            resolvingStream = false,
            canSwitchToVideo = true,
            audioCodecLabel = "Mode Video",
            resumeSeekSec = startSec,
        )
    }

    fun switchAudioVideoMode() {
        val s = _state.value
        val track = s.track ?: return
        val videoId = track.videoId ?: return

        if (s.mode == PlayerMode.AUDIO) {
            resolveJob?.cancel()
            val pos = s.currentTimeSec
            progressJob?.cancel()
            exoPlayer?.pause()
            _state.value = s.copy(
                mode = PlayerMode.YOUTUBE_IFRAME,
                youtubeExpanded = true,
                playing = true,
                resolvingStream = false,
                audioCodecLabel = "Mode Video",
                resumeSeekSec = pos,
            )
        } else if (s.mode == PlayerMode.YOUTUBE_IFRAME) {
            val pos = s.currentTimeSec
            onYoutubePause?.invoke()
            _state.value = s.copy(
                mode = PlayerMode.AUDIO,
                youtubeExpanded = false,
                playing = false,
                resolvingStream = true,
                audioCodecLabel = "Beralih ke Audio 8D…",
            )
            resolveAndPlayAudio(track, videoId, startSec = pos)
        }
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
        if (s.track == null || s.resolvingStream) return
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
        resolveJob?.cancel()
        progressJob?.cancel()
        exoPlayer?.pause()
        exoPlayer?.clearMediaItems()
        _state.value = PlayerState()
        SpatialHostBridge.currentTitle.value = null
        SpatialHostBridge.currentLyrics.value = null
    }

    fun release() {
        resolveJob?.cancel()
        progressJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        SpatialHostBridge.onTogglePlayback = null
        SpatialHostBridge.onPausePlayback = null
    }
}
