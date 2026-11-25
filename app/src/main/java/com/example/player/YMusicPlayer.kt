package com.example.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.data.RepeatMode
import com.example.data.ResolvedStreams
import com.example.data.Track
import com.example.data.TrackSource
import com.example.debug.AppDiagnostics
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
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val sleepTimerRemainingSec: Long? = null,
    val sleepAfterCurrentTrack: Boolean = false,
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
    private var sleepTimerJob: Job? = null
    private var cachedResolved: ResolvedStreams? = null
    private var pendingSeekSec: Float = 0f
    private var activeStreamUrl: String = ""
    private val failedStreamUrls = hashSetOf<String>()
    private var retriedFreshResolve: Boolean = false

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
                                handleTrackCompleted(dur)
                            } else {
                                _state.update { it.copy(durationSec = dur) }
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        val s = _state.value
                        val track = s.track
                        val videoId = track?.videoId
                        AppDiagnostics.warn("Player", "Playback error (${error.errorCodeName}) for ${track?.title}: ${error.message}")
                        if (activeStreamUrl.isNotBlank()) {
                            failedStreamUrls.add(activeStreamUrl)
                        }
                        if (s.mode == PlayerMode.AUDIO && track != null && !videoId.isNullOrBlank()) {
                            val nextCandidate = cachedResolved
                                ?.takeIf { it.videoId == videoId }
                                ?.let { res ->
                                    (res.audioStreams + res.videoStreams.filter { it.hasAudio })
                                        .firstOrNull { it.url !in failedStreamUrls }
                                }
                            if (nextCandidate != null) {
                                AppDiagnostics.info("Player", "Retrying alternative stream: ${nextCandidate.qualityLabel}")
                                playAudioUri(
                                    track = track.copy(url = nextCandidate.url),
                                    streamUrl = nextCandidate.url,
                                    userAgent = nextCandidate.userAgent.ifBlank { "YMusic-Lite/0.3.0" },
                                    codecLabel = "${nextCandidate.qualityLabel} · 8D DSP",
                                    canSwitchToVideo = true,
                                    seekToSec = s.currentTimeSec,
                                    queue = s.queue,
                                    index = s.currentIndex,
                                )
                                return
                            }
                            if (!retriedFreshResolve) {
                                retriedFreshResolve = true
                                cachedResolved = null
                                AppDiagnostics.info("Player", "Refreshing streams for $videoId after playback error")
                                resolveAndPlayAudio(track, videoId, startSec = s.currentTimeSec, queue = s.queue, index = s.currentIndex)
                                return
                            }
                            fallbackToYoutubeIframe(track, s.currentTimeSec)
                        } else {
                            _state.update { it.copy(playing = false, resolvingStream = false) }
                        }
                    }
                })
            }
    }

    private fun handleTrackCompleted(dur: Float) {
        val s = _state.value
        if (s.sleepAfterCurrentTrack) {
            AppDiagnostics.info("Player", "Sleep timer triggered at end of track")
            _state.update {
                it.copy(
                    playing = false,
                    currentTimeSec = 0f,
                    durationSec = dur,
                    sleepAfterCurrentTrack = false,
                    sleepTimerRemainingSec = null,
                )
            }
            exoPlayer?.seekTo(0L)
            exoPlayer?.pause()
            BackgroundPlaybackService.sync(appContext)
            return
        }
        when (s.repeatMode) {
            RepeatMode.ONE -> {
                exoPlayer?.seekTo(0L)
                exoPlayer?.play()
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                val advanced = playNextInternal(fromAutoComplete = true)
                if (!advanced) {
                    _state.update { it.copy(playing = false, currentTimeSec = 0f, durationSec = dur) }
                }
            }
        }
    }

    fun play(track: Track, queueContext: List<Track>? = null) {
        val currentState = _state.value
        val nextQueue: List<Track>
        val nextIndex: Int
        if (queueContext != null && queueContext.isNotEmpty()) {
            val playable = queueContext.filter { !it.isMix || !it.videoId.isNullOrBlank() || !it.url.isNullOrBlank() }
                .ifEmpty { listOf(track) }
            nextQueue = if (playable.any { it.id == track.id }) playable else listOf(track) + playable
            nextIndex = nextQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        } else if (currentState.queue.any { it.id == track.id }) {
            nextQueue = currentState.queue
            nextIndex = nextQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        } else {
            nextQueue = listOf(track)
            nextIndex = 0
        }

        playInternal(
            track = track,
            queue = nextQueue,
            index = nextIndex,
        )
    }

    private fun playInternal(track: Track, queue: List<Track>, index: Int) {
        resolveJob?.cancel()
        progressJob?.cancel()
        exoPlayer?.pause()
        exoPlayer?.clearMediaItems()
        pendingSeekSec = 0f
        activeStreamUrl = ""
        failedStreamUrls.clear()
        retriedFreshResolve = false

        AppDiagnostics.info("Player", "Play requested: ${track.title} (queue ${index + 1}/${queue.size})")
        SpatialHostBridge.currentTitle.value = track.title
        SpatialHostBridge.currentLyrics.value = track.artist
        if (MixerRepository.floatingLyricsEnabled.value) {
            FloatingBubbleService.syncLyrics(appContext)
        }

        val rawUrl = track.url?.trim().orEmpty()
        val isLocalUri = rawUrl.startsWith("/") ||
            rawUrl.startsWith("content://", ignoreCase = true) ||
            rawUrl.startsWith("file://", ignoreCase = true)
        val hasVideoId = !track.videoId.isNullOrBlank()
        val directUrl = rawUrl.takeIf {
            it.isNotBlank() && MediaScraper.isDirectMediaUrl(it) && (isLocalUri || track.source != TrackSource.YOUTUBE || !hasVideoId)
        }

        if (directUrl != null) {
            playAudioUri(
                track = track,
                streamUrl = directUrl,
                userAgent = "YMusic-Lite/0.3.0",
                codecLabel = if (isLocalUri) "Lokal · 8D DSP" else "Direct · 8D DSP",
                canSwitchToVideo = hasVideoId,
                seekToSec = 0f,
                queue = queue,
                index = index,
            )
            return
        }

        if (track.source == TrackSource.YOUTUBE && hasVideoId) {
            val vid = track.videoId!!
            _state.update { prev ->
                prev.copy(
                    track = track,
                    playing = false,
                    currentTimeSec = 0f,
                    durationSec = track.duration.toFloat(),
                    mode = PlayerMode.AUDIO,
                    youtubeExpanded = false,
                    resolvingStream = true,
                    canSwitchToVideo = true,
                    audioCodecLabel = "Ekstrak audio…",
                    queue = queue,
                    currentIndex = index,
                )
            }
            resolveAndPlayAudio(track, vid, startSec = 0f, queue = queue, index = index)
            return
        }

        if (rawUrl.isNotBlank() && MediaScraper.isDirectMediaUrl(rawUrl)) {
            playAudioUri(
                track = track,
                streamUrl = rawUrl,
                userAgent = "YMusic-Lite/0.3.0",
                codecLabel = "Audio · 8D DSP",
                canSwitchToVideo = hasVideoId,
                seekToSec = 0f,
                queue = queue,
                index = index,
            )
            return
        }

        AppDiagnostics.warn("Player", "Skipping non-media URL for playback: ${rawUrl.take(80)}")
        _state.update { it.copy(track = track, mode = PlayerMode.NONE, playing = false, resolvingStream = false, queue = queue, currentIndex = index) }
    }

    private fun resolveAndPlayAudio(
        track: Track,
        videoId: String,
        startSec: Float,
        queue: List<Track>,
        index: Int,
    ) {
        resolveJob?.cancel()
        resolveJob = scope.launch {
            val cached = cachedResolved?.takeIf { it.videoId == videoId }
            val resolved = cached ?: runCatching {
                InnerTubeClient.resolveStreams(videoId, authHeadersProvider())
            }.getOrNull()

            if (resolved != null) {
                cachedResolved = resolved
            }

            if (_state.value.track?.id != track.id) return@launch

            val bestAudio = resolved?.let { res ->
                (res.audioStreams + res.videoStreams.filter { it.hasAudio })
                    .firstOrNull { it.url !in failedStreamUrls }
            }
            if (bestAudio != null) {
                val enrichedTrack = track.copy(
                    duration = if (track.duration > 0) track.duration else resolved.durationSec,
                    thumbnail = track.thumbnail.ifBlank { resolved.thumbnail },
                    url = bestAudio.url,
                )
                val updatedQueue = queue.mapIndexed { idx, t -> if (idx == index) enrichedTrack else t }
                playAudioUri(
                    track = enrichedTrack,
                    streamUrl = bestAudio.url,
                    userAgent = bestAudio.userAgent.ifBlank { "YMusic-Lite/0.3.0" },
                    codecLabel = "${bestAudio.qualityLabel} · 8D DSP",
                    canSwitchToVideo = true,
                    seekToSec = startSec,
                    queue = updatedQueue,
                    index = index,
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
        queue: List<Track>,
        index: Int,
    ) {
        pendingSeekSec = seekToSec.coerceAtLeast(0f)
        val prev = _state.value
        _state.value = prev.copy(
            track = track,
            playing = true,
            currentTimeSec = seekToSec,
            durationSec = track.duration.toFloat(),
            mode = PlayerMode.AUDIO,
            youtubeExpanded = false,
            resolvingStream = false,
            canSwitchToVideo = canSwitchToVideo,
            audioCodecLabel = codecLabel,
            queue = queue,
            currentIndex = index,
        )

        activeStreamUrl = streamUrl
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)
        val dataSourceFactory = DefaultDataSource.Factory(appContext, httpFactory)

        val uri = if (streamUrl.startsWith("/")) {
            android.net.Uri.fromFile(java.io.File(streamUrl))
        } else {
            android.net.Uri.parse(streamUrl)
        }
        val mediaItemBuilder = MediaItem.Builder().setUri(uri)
        val lowerUrl = streamUrl.lowercase()
        when {
            lowerUrl.contains(".m3u8") || lowerUrl.contains("hls_playlist") ->
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            lowerUrl.contains(".mpd") ->
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
        }

        val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
            .createMediaSource(mediaItemBuilder.build())

        exoPlayer?.apply {
            playbackParameters = PlaybackParameters(prev.playbackSpeed, prev.playbackPitch)
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
        AppDiagnostics.info("Player", "Streaming audio ($codecLabel): ${track.title}")
        BackgroundPlaybackService.sync(appContext)
        startAudioProgressPolling()
    }

    private fun fallbackToYoutubeIframe(track: Track, startSec: Float) {
        progressJob?.cancel()
        exoPlayer?.pause()
        exoPlayer?.clearMediaItems()
        _state.update { prev ->
            prev.copy(
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
            resolveAndPlayAudio(track, videoId, startSec = pos, queue = s.queue, index = s.currentIndex)
        }
    }

    fun playNext(): Boolean = playNextInternal(fromAutoComplete = false)

    private fun playNextInternal(fromAutoComplete: Boolean): Boolean {
        val s = _state.value
        if (s.queue.isEmpty()) return false
        val nextIdx = when {
            s.shuffleEnabled && s.queue.size > 1 -> {
                val pool = s.queue.indices.filter { it != s.currentIndex }
                pool.random()
            }
            s.currentIndex + 1 < s.queue.size -> s.currentIndex + 1
            s.repeatMode == RepeatMode.ALL || !fromAutoComplete -> 0
            else -> -1
        }
        if (nextIdx !in s.queue.indices) return false
        playInternal(s.queue[nextIdx], s.queue, nextIdx)
        return true
    }

    fun playPrevious(): Boolean {
        val s = _state.value
        if (s.queue.isEmpty()) return false
        if (s.currentTimeSec > 4f) {
            seek(0f)
            return true
        }
        val prevIdx = when {
            s.currentIndex - 1 >= 0 -> s.currentIndex - 1
            s.repeatMode == RepeatMode.ALL || s.queue.size > 1 -> s.queue.lastIndex
            else -> 0
        }
        if (prevIdx !in s.queue.indices) return false
        playInternal(s.queue[prevIdx], s.queue, prevIdx)
        return true
    }

    fun playAtQueueIndex(index: Int) {
        val s = _state.value
        if (index !in s.queue.indices) return
        playInternal(s.queue[index], s.queue, index)
    }

    fun addToQueueNext(track: Track) {
        val s = _state.value
        if (s.track == null || s.queue.isEmpty()) {
            play(track)
            return
        }
        val filtered = s.queue.filterNot { it.id == track.id }
        val currentId = s.track.id
        val curIdx = filtered.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        val insertPos = (curIdx + 1).coerceAtMost(filtered.size)
        val updated = filtered.toMutableList().apply { add(insertPos, track) }
        _state.update { it.copy(queue = updated, currentIndex = curIdx) }
    }

    fun removeFromQueue(index: Int) {
        val s = _state.value
        if (index !in s.queue.indices || s.queue.size <= 1) return
        val updated = s.queue.toMutableList().apply { removeAt(index) }
        val nextCur = when {
            index < s.currentIndex -> (s.currentIndex - 1).coerceAtLeast(0)
            index == s.currentIndex -> s.currentIndex.coerceAtMost(updated.lastIndex)
            else -> s.currentIndex
        }
        _state.update { it.copy(queue = updated, currentIndex = nextCur) }
        if (index == s.currentIndex && nextCur in updated.indices) {
            playInternal(updated[nextCur], updated, nextCur)
        }
    }

    fun toggleShuffle() {
        _state.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    }

    fun cycleRepeatMode() {
        _state.update { st ->
            val next = when (st.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            st.copy(repeatMode = next)
        }
    }

    fun setSpeedAndPitch(speed: Float, pitch: Float) {
        val sp = speed.coerceIn(0.5f, 2.0f)
        val pt = pitch.coerceIn(0.6f, 1.5f)
        _state.update { it.copy(playbackSpeed = sp, playbackPitch = pt) }
        exoPlayer?.playbackParameters = PlaybackParameters(sp, pt)
    }

    fun setSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            cancelSleepTimer()
            return
        }
        val totalSec = minutes * 60L
        _state.update { it.copy(sleepTimerRemainingSec = totalSec, sleepAfterCurrentTrack = false) }
        AppDiagnostics.info("Player", "Sleep timer set for $minutes min")
        sleepTimerJob = scope.launch {
            var remaining = totalSec
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _state.update { it.copy(sleepTimerRemainingSec = remaining) }
            }
            if (isActive) {
                AppDiagnostics.info("Player", "Sleep timer expired -> pausing playback")
                pause()
                _state.update { it.copy(sleepTimerRemainingSec = null, sleepAfterCurrentTrack = false) }
                BackgroundPlaybackService.sync(appContext)
            }
        }
    }

    fun setSleepAfterTrack() {
        sleepTimerJob?.cancel()
        _state.update { it.copy(sleepTimerRemainingSec = null, sleepAfterCurrentTrack = true) }
        AppDiagnostics.info("Player", "Sleep timer set for end of current track")
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _state.update { it.copy(sleepTimerRemainingSec = null, sleepAfterCurrentTrack = false) }
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
        sleepTimerJob?.cancel()
        exoPlayer?.pause()
        exoPlayer?.clearMediaItems()
        _state.value = PlayerState()
        SpatialHostBridge.currentTitle.value = null
        SpatialHostBridge.currentLyrics.value = null
    }

    fun release() {
        resolveJob?.cancel()
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        SpatialHostBridge.onTogglePlayback = null
        SpatialHostBridge.onPausePlayback = null
    }
}
