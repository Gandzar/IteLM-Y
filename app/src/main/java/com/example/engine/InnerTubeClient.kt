package com.example.engine

import com.example.data.AccountProfile
import com.example.data.ResolvedStreams
import com.example.data.StreamCandidate
import com.example.data.Track
import com.example.data.TrackSource
import com.example.data.YtPlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object InnerTubeClient {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private const val YT_MUSIC_API = "https://music.youtube.com/youtubei/v1"
    private const val YT_API = "https://www.youtube.com/youtubei/v1"

    private const val WEB_REMIX_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    private const val ANDROID_VR_UA =
        "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
    private const val IOS_UA =
        "com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 18_1_0 like Mac OS X;)"
    private const val ANDROID_UA =
        "com.google.android.youtube/19.45.36 (Linux; U; Android 14) gzip"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchAccountProfile(authHeaders: Map<String, String>): AccountProfile? = withContext(Dispatchers.IO) {
        if (authHeaders.isEmpty()) return@withContext null
        runCatching {
            val payload = JSONObject().put("context", webRemixContext())
            val resJson = postJson("$YT_MUSIC_API/account/account_menu?prettyPrint=false", payload, WEB_REMIX_UA, authHeaders)
                ?: return@withContext null

            val header = findObjectsByKey(resJson, "activeAccountHeaderRenderer").firstOrNull()
                ?: return@withContext null

            val name = extractText(header.optJSONObject("accountName")).ifBlank { "Akun Google" }
            val handle = extractText(header.optJSONObject("channelHandle"))
                .ifBlank { extractText(header.optJSONObject("email")) }
                .ifBlank { "YouTube Music" }
            val avatar = extractLastThumbnail(header.optJSONObject("accountPhoto"))

            AccountProfile(
                isLoggedIn = true,
                name = name,
                handle = handle,
                avatarUrl = avatar,
                lastSyncedAt = System.currentTimeMillis(),
            )
        }.getOrNull()
    }

    suspend fun fetchHomeFeed(
        queryHint: String? = null,
        authHeaders: Map<String, String> = emptyMap(),
    ): List<Track> = withContext(Dispatchers.IO) {
        if (!queryHint.isNullOrBlank()) {
            return@withContext searchMusic(queryHint, authHeaders)
        }

        val musicHome = browseTracks("FEmusic_home", authHeaders)
        if (musicHome.isNotEmpty()) return@withContext musicHome

        val ytWebHome = browseYoutubeWeb("FEwhat_to_watch", authHeaders = authHeaders)
        if (ytWebHome.isNotEmpty()) return@withContext ytWebHome

        val explore = browseTracks("FEmusic_explore", authHeaders)
        if (explore.isNotEmpty()) return@withContext explore

        searchMusic("mix musik populer", authHeaders)
    }

    suspend fun fetchTrendingMusic(authHeaders: Map<String, String> = emptyMap()): List<Track> = withContext(Dispatchers.IO) {
        val explore = browseTracks("FEmusic_explore", authHeaders)
        if (explore.isNotEmpty()) return@withContext explore

        val ytTrending = browseYoutubeWeb(
            browseId = "FEtrending",
            params = "4gINGgt5dG1hX2NoYXJ0cw%3D%3D",
            authHeaders = authHeaders,
        )
        if (ytTrending.isNotEmpty()) return@withContext ytTrending

        searchMusic("musik trending indonesia terbaru", authHeaders)
    }

    suspend fun fetchRecommendations(authHeaders: Map<String, String> = emptyMap()): List<Track> = withContext(Dispatchers.IO) {
        val home = browseTracks("FEmusic_home", authHeaders)
        if (home.size >= 5) return@withContext home.reversed()

        val ytWeb = browseYoutubeWeb("FEwhat_to_watch", authHeaders = authHeaders)
        if (ytWeb.isNotEmpty()) return@withContext ytWeb

        searchMusic("slowed reverb visualizer dj remix populer", authHeaders)
    }

    suspend fun fetchTopSongs(authHeaders: Map<String, String> = emptyMap()): List<Track> = withContext(Dispatchers.IO) {
        val charts = browseTracks("FEmusic_charts", authHeaders)
        if (charts.isNotEmpty()) return@withContext charts

        val explore = browseTracks("FEmusic_explore", authHeaders)
        if (explore.isNotEmpty()) return@withContext explore

        searchMusic("top songs indonesia hits viral", authHeaders)
    }

    suspend fun searchMusic(
        query: String,
        authHeaders: Map<String, String> = emptyMap(),
        songsOnly: Boolean = false,
    ): List<Track> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()

        runCatching {
            val payload = JSONObject()
                .put("context", webRemixContext())
                .put("query", q)
            if (songsOnly) {
                payload.put("params", "EgWKAQIIAWoMEAMQBBAJEAoQBRAV")
            }
            val resJson = postJson("$YT_MUSIC_API/search?prettyPrint=false", payload, WEB_REMIX_UA, authHeaders)
                ?: return@withContext emptyList()
            parseTracksFromResponse(resJson)
        }.getOrDefault(emptyList())
    }

    suspend fun fetchLikedMusic(authHeaders: Map<String, String>): List<Track> = withContext(Dispatchers.IO) {
        if (authHeaders.isEmpty()) return@withContext emptyList()
        val primary = browseTracks("FEmusic_liked_videos", authHeaders)
        if (primary.isNotEmpty()) return@withContext primary
        browseTracks("VLLM", authHeaders)
    }

    suspend fun fetchLibrarySongs(authHeaders: Map<String, String>): List<Track> = withContext(Dispatchers.IO) {
        if (authHeaders.isEmpty()) return@withContext emptyList()
        browseTracks("FEmusic_library_landing", authHeaders)
    }

    suspend fun fetchUserHistory(authHeaders: Map<String, String>): List<Track> = withContext(Dispatchers.IO) {
        if (authHeaders.isEmpty()) return@withContext emptyList()
        browseTracks("FEmusic_history", authHeaders)
    }

    suspend fun fetchUserPlaylists(authHeaders: Map<String, String>): List<YtPlaylist> = withContext(Dispatchers.IO) {
        if (authHeaders.isEmpty()) return@withContext emptyList()
        runCatching {
            val payload = JSONObject()
                .put("context", webRemixContext())
                .put("browseId", "FEmusic_liked_playlists")
            val resJson = postJson("$YT_MUSIC_API/browse?prettyPrint=false", payload, WEB_REMIX_UA, authHeaders)
                ?: return@withContext emptyList()

            val seenIds = hashSetOf<String>()
            val out = mutableListOf<YtPlaylist>()

            findObjectsByKey(resJson, "musicTwoRowItemRenderer").forEach { item ->
                val browseId = item.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("browseEndpoint")
                    ?.optString("browseId")
                    .orEmpty()
                val title = extractText(item.optJSONObject("title"))
                val subtitle = extractText(item.optJSONObject("subtitle"))
                val thumb = extractLastThumbnail(
                    item.optJSONObject("thumbnailRenderer")
                        ?.optJSONObject("musicThumbnailRenderer")
                        ?.optJSONObject("thumbnail"),
                )
                val countMatch = Regex("\\b\\d+\\s*(video|lagu|songs?|tracks?)\\b", RegexOption.IGNORE_CASE)
                    .find(subtitle)?.value.orEmpty()
                if (browseId.isNotBlank() && title.isNotBlank() && seenIds.add(browseId)) {
                    out.add(
                        YtPlaylist(
                            id = browseId.removePrefix("VL"),
                            browseId = browseId,
                            title = title,
                            subtitle = subtitle.ifBlank { "Playlist" },
                            trackCount = countMatch,
                            thumbnail = thumb,
                        ),
                    )
                }
            }

            findObjectsByKey(resJson, "musicResponsiveListItemRenderer").forEach { item ->
                val browseId = item.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("browseEndpoint")
                    ?.optString("browseId")
                    .orEmpty()
                if (browseId.isNotBlank() && seenIds.add(browseId)) {
                    val cols = item.optJSONArray("flexColumns")
                    val title = cols?.optJSONObject(0)
                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")
                        ?.let(::extractText)
                        .orEmpty()
                    val subtitle = cols?.optJSONObject(1)
                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")
                        ?.let(::extractText)
                        .orEmpty()
                    val thumb = extractLastThumbnail(
                        item.optJSONObject("thumbnail")
                            ?.optJSONObject("musicThumbnailRenderer")
                            ?.optJSONObject("thumbnail"),
                    )
                    val countMatch = Regex("\\b\\d+\\s*(video|lagu|songs?|tracks?)\\b", RegexOption.IGNORE_CASE)
                        .find(subtitle)?.value.orEmpty()
                    if (title.isNotBlank()) {
                        out.add(
                            YtPlaylist(
                                id = browseId.removePrefix("VL"),
                                browseId = browseId,
                                title = title,
                                subtitle = subtitle.ifBlank { "Playlist" },
                                trackCount = countMatch,
                                thumbnail = thumb,
                            ),
                        )
                    }
                }
            }
            out
        }.getOrDefault(emptyList())
    }

    suspend fun fetchPlaylistTracks(
        browseId: String,
        authHeaders: Map<String, String>,
    ): List<Track> = withContext(Dispatchers.IO) {
        val normalized = if (browseId.startsWith("VL") || browseId.startsWith("FE")) browseId else "VL$browseId"
        browseTracks(normalized, authHeaders)
    }

    suspend fun resolveStreams(
        videoId: String,
        authHeaders: Map<String, String> = emptyMap(),
    ): ResolvedStreams? = withContext(Dispatchers.IO) {
        val cleanId = videoId.trim()
        if (cleanId.isEmpty()) return@withContext null

        val configs = listOf(
            PlayerClientConfig("ANDROID_VR", ANDROID_VR_UA, androidVrContext(), useAuth = false),
            PlayerClientConfig("IOS", IOS_UA, iosContext(), useAuth = false),
            PlayerClientConfig("ANDROID", ANDROID_UA, androidContext(), useAuth = false),
            PlayerClientConfig("WEB_REMIX", WEB_REMIX_UA, webRemixContext(), useAuth = true),
        )

        var bestMetadataOnly: ResolvedStreams? = null

        for (cfg in configs) {
            val headers = if (cfg.useAuth) authHeaders else emptyMap()
            val payload = JSONObject()
                .put("context", cfg.context)
                .put("videoId", cleanId)
                .put("contentCheckOk", true)
                .put("racyCheckOk", true)

            val endpoint = if (cfg.name == "WEB_REMIX") {
                "$YT_MUSIC_API/player?prettyPrint=false"
            } else {
                "$YT_API/player?prettyPrint=false"
            }

            val resJson = postJson(endpoint, payload, cfg.userAgent, headers) ?: continue
            val parsed = parsePlayerResponse(cleanId, resJson, cfg.userAgent) ?: continue

            if (parsed.bestAudio != null || parsed.bestVideo != null) {
                return@withContext parsed
            }
            if (bestMetadataOnly == null && parsed.title.isNotBlank()) {
                bestMetadataOnly = parsed
            }
        }

        if (authHeaders.isNotEmpty()) {
            for (cfg in configs.take(2)) {
                val payload = JSONObject()
                    .put("context", cfg.context)
                    .put("videoId", cleanId)
                    .put("contentCheckOk", true)
                    .put("racyCheckOk", true)
                val resJson = postJson("$YT_API/player?prettyPrint=false", payload, cfg.userAgent, authHeaders)
                    ?: continue
                val parsed = parsePlayerResponse(cleanId, resJson, cfg.userAgent) ?: continue
                if (parsed.bestAudio != null || parsed.bestVideo != null) {
                    return@withContext parsed
                }
            }
        }

        bestMetadataOnly
    }

    private fun browseTracks(browseId: String, authHeaders: Map<String, String>): List<Track> = runCatching {
        val payload = JSONObject()
            .put("context", webRemixContext())
            .put("browseId", browseId)
        val resJson = postJson("$YT_MUSIC_API/browse?prettyPrint=false", payload, WEB_REMIX_UA, authHeaders)
            ?: return emptyList()
        parseTracksFromResponse(resJson)
    }.getOrDefault(emptyList())

    private fun browseYoutubeWeb(
        browseId: String,
        params: String? = null,
        authHeaders: Map<String, String> = emptyMap(),
    ): List<Track> = runCatching {
        val payload = JSONObject()
            .put("context", webContext())
            .put("browseId", browseId)
        if (!params.isNullOrBlank()) payload.put("params", params)
        val ytHeaders = authHeaders.mapValues { (k, v) ->
            if (k == "Origin" || k == "X-Origin") "https://www.youtube.com" else v
        }
        val resJson = postJson("$YT_API/browse?prettyPrint=false", payload, WEB_REMIX_UA, ytHeaders)
            ?: return emptyList()
        parseTracksFromResponse(resJson)
    }.getOrDefault(emptyList())

    private fun parsePlayerResponse(
        videoId: String,
        resJson: JSONObject,
        userAgent: String,
    ): ResolvedStreams? {
        val details = resJson.optJSONObject("videoDetails")
        val title = details?.optString("title").orEmpty()
        val author = details?.optString("author").orEmpty()
        val durationSec = details?.optString("lengthSeconds")?.toIntOrNull() ?: 0
        val thumb = extractLastThumbnail(details?.optJSONObject("thumbnail"))
            .ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" }

        val streamingData = resJson.optJSONObject("streamingData")
        val audioCandidates = mutableListOf<StreamCandidate>()
        val videoCandidates = mutableListOf<StreamCandidate>()

        if (streamingData != null) {
            val adaptive = streamingData.optJSONArray("adaptiveFormats")
            if (adaptive != null) {
                for (i in 0 until adaptive.length()) {
                    val fmt = adaptive.optJSONObject(i) ?: continue
                    val url = fmt.optString("url")
                    if (!url.startsWith("https://")) continue
                    val mimeType = fmt.optString("mimeType")
                    val bitrate = fmt.optInt("bitrate", 0)
                    when {
                        mimeType.startsWith("audio/") -> {
                            val isMp4 = mimeType.contains("mp4", ignoreCase = true)
                            val ext = if (isMp4) "m4a" else "opus"
                            val kbps = if (bitrate > 0) "${bitrate / 1000} kbps" else "Audio"
                            audioCandidates.add(
                                StreamCandidate(
                                    url = url,
                                    mimeType = mimeType,
                                    ext = ext,
                                    bitrate = bitrate,
                                    qualityLabel = "${ext.uppercase()} $kbps",
                                    isAudio = true,
                                    userAgent = userAgent,
                                ),
                            )
                        }
                        mimeType.startsWith("video/mp4") -> {
                            val quality = fmt.optString("qualityLabel").ifBlank { "MP4" }
                            videoCandidates.add(
                                StreamCandidate(
                                    url = url,
                                    mimeType = mimeType,
                                    ext = "mp4",
                                    bitrate = bitrate,
                                    qualityLabel = "$quality (video-only)",
                                    isAudio = false,
                                    userAgent = userAgent,
                                ),
                            )
                        }
                    }
                }
            }

            val progressive = streamingData.optJSONArray("formats")
            if (progressive != null) {
                for (i in 0 until progressive.length()) {
                    val fmt = progressive.optJSONObject(i) ?: continue
                    val url = fmt.optString("url")
                    if (!url.startsWith("https://")) continue
                    val mimeType = fmt.optString("mimeType")
                    val bitrate = fmt.optInt("bitrate", 0)
                    val quality = fmt.optString("qualityLabel").ifBlank { "MP4" }
                    videoCandidates.add(
                        StreamCandidate(
                            url = url,
                            mimeType = mimeType,
                            ext = "mp4",
                            bitrate = bitrate,
                            qualityLabel = quality,
                            isAudio = false,
                            userAgent = userAgent,
                        ),
                    )
                }
            }
        }

        val sortedAudio = audioCandidates.sortedWith(
            compareByDescending<StreamCandidate> { it.ext == "m4a" }.thenByDescending { it.bitrate },
        )
        val sortedVideo = videoCandidates.sortedWith(
            compareByDescending<StreamCandidate> { !it.qualityLabel.contains("video-only") }
                .thenByDescending { it.bitrate },
        )

        return ResolvedStreams(
            videoId = videoId,
            title = title,
            author = author,
            durationSec = durationSec,
            thumbnail = thumb,
            audioStreams = sortedAudio,
            videoStreams = sortedVideo,
            bestAudio = sortedAudio.firstOrNull(),
            bestVideo = sortedVideo.firstOrNull(),
        )
    }

    private fun parseTracksFromResponse(root: JSONObject): List<Track> {
        val seenIds = hashSetOf<String>()
        val tracks = mutableListOf<Track>()

        // 1) YT Music responsive list items
        findObjectsByKey(root, "musicResponsiveListItemRenderer").forEach { item ->
            val videoId = extractVideoId(item)
            if (videoId.isNullOrBlank() || !seenIds.add(videoId)) return@forEach

            val flexCols = item.optJSONArray("flexColumns") ?: JSONArray()
            val title = flexCols.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.let(::extractText)
                .orEmpty()
            if (title.isBlank()) return@forEach

            val subtitleRaw = flexCols.optJSONObject(1)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.let(::extractText)
                .orEmpty()

            val fixedDuration = item.optJSONArray("fixedColumns")
                ?.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                ?.optJSONObject("text")
                ?.let(::extractText)
                .orEmpty()

            val durationSec = parseDurationSeconds(fixedDuration)
                .takeIf { it > 0 }
                ?: parseDurationSeconds(subtitleRaw)

            val artist = cleanArtistSubtitle(subtitleRaw).ifBlank { "YouTube Music" }
            val thumb = extractLastThumbnail(
                item.optJSONObject("thumbnail")
                    ?.optJSONObject("musicThumbnailRenderer")
                    ?.optJSONObject("thumbnail"),
            ).ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" }

            tracks.add(
                Track(
                    id = videoId,
                    title = title,
                    artist = artist,
                    duration = durationSec,
                    thumbnail = thumb,
                    source = TrackSource.YOUTUBE,
                    videoId = videoId,
                    subtitle = subtitleRaw.ifBlank { artist },
                ),
            )
        }

        // 2) YT Music two-row items (Home mixes, quick picks, charts)
        findObjectsByKey(root, "musicTwoRowItemRenderer").forEach { item ->
            val navEndpoint = item.optJSONObject("navigationEndpoint")
            val watchEndpoint = navEndpoint?.optJSONObject("watchEndpoint")
                ?: item.optJSONObject("thumbnailOverlay")
                    ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("musicPlayButtonRenderer")
                    ?.optJSONObject("playNavigationEndpoint")
                    ?.optJSONObject("watchEndpoint")

            val watchPlaylistEndpoint = navEndpoint?.optJSONObject("watchPlaylistEndpoint")
                ?: item.optJSONObject("thumbnailOverlay")
                    ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("musicPlayButtonRenderer")
                    ?.optJSONObject("playNavigationEndpoint")
                    ?.optJSONObject("watchPlaylistEndpoint")

            val browseId = navEndpoint?.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
            val videoId = watchEndpoint?.optString("videoId").orEmpty()
            val playlistId = watchPlaylistEndpoint?.optString("playlistId").orEmpty()
                .ifBlank { browseId.removePrefix("VL") }

            val key = videoId.ifBlank { playlistId }
            if (key.isBlank() || !seenIds.add(key)) return@forEach

            val title = extractText(item.optJSONObject("title"))
            val subtitleRaw = extractText(item.optJSONObject("subtitle"))
            val artist = cleanArtistSubtitle(subtitleRaw).ifBlank { "YouTube Music" }
            val thumb = extractLastThumbnail(
                item.optJSONObject("thumbnailRenderer")
                    ?.optJSONObject("musicThumbnailRenderer")
                    ?.optJSONObject("thumbnail"),
            ).ifBlank {
                if (videoId.isNotBlank()) "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" else ""
            }

            val isMixItem = videoId.isBlank() || subtitleRaw.contains("Mix", ignoreCase = true) ||
                title.startsWith("Mix", ignoreCase = true)

            if (title.isNotBlank()) {
                tracks.add(
                    Track(
                        id = key,
                        title = title,
                        artist = artist,
                        duration = parseDurationSeconds(subtitleRaw),
                        thumbnail = thumb,
                        source = TrackSource.YOUTUBE,
                        videoId = videoId.ifBlank { null },
                        subtitle = subtitleRaw.ifBlank { artist },
                        badgeText = if (isMixItem) "Mix" else "",
                        isMix = isMixItem,
                        playlistBrowseId = browseId.ifBlank {
                            if (playlistId.isNotBlank()) "VL$playlistId" else null
                        },
                    ),
                )
            }
        }

        // 3) Standard YouTube videoRenderer / compactVideoRenderer / gridVideoRenderer
        for (rendererKey in listOf("videoRenderer", "compactVideoRenderer", "gridVideoRenderer")) {
            findObjectsByKey(root, rendererKey).forEach { item ->
                val videoId = item.optString("videoId")
                if (videoId.isBlank() || !seenIds.add(videoId)) return@forEach

                val title = extractText(item.optJSONObject("title"))
                if (title.isBlank()) return@forEach

                val owner = extractText(item.optJSONObject("ownerText"))
                    .ifBlank { extractText(item.optJSONObject("shortBylineText")) }
                    .ifBlank { extractText(item.optJSONObject("longBylineText")) }
                    .ifBlank { "YouTube" }
                val views = extractText(item.optJSONObject("shortViewCountText"))
                val published = extractText(item.optJSONObject("publishedTimeText"))
                val lengthText = extractText(item.optJSONObject("lengthText"))
                val durationSec = parseDurationSeconds(lengthText)
                val thumb = extractLastThumbnail(item.optJSONObject("thumbnail"))
                    .ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" }

                val subtitleParts = listOf(owner, views, published).filter { it.isNotBlank() }

                tracks.add(
                    Track(
                        id = videoId,
                        title = title,
                        artist = owner,
                        duration = durationSec,
                        thumbnail = thumb,
                        source = TrackSource.YOUTUBE,
                        videoId = videoId,
                        subtitle = subtitleParts.joinToString(" · "),
                        badgeText = lengthText,
                    ),
                )
            }
        }

        return tracks
    }

    private fun extractVideoId(item: JSONObject): String? {
        item.optJSONObject("playlistItemData")?.optString("videoId")?.takeIf { it.isNotBlank() }?.let { return it }

        item.optJSONObject("overlay")
            ?.optJSONObject("musicItemThumbnailOverlayRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("musicPlayButtonRenderer")
            ?.optJSONObject("playNavigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val flexCols = item.optJSONArray("flexColumns") ?: return null
        for (i in 0 until flexCols.length()) {
            val runs = flexCols.optJSONObject(i)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs") ?: continue
            for (r in 0 until runs.length()) {
                val vid = runs.optJSONObject(r)
                    ?.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("watchEndpoint")
                    ?.optString("videoId")
                if (!vid.isNullOrBlank()) return vid
            }
        }
        return null
    }

    private fun cleanArtistSubtitle(raw: String): String {
        if (raw.isBlank()) return ""
        val parts = raw.split("•").map { it.trim() }.filter { part ->
            part.isNotBlank() &&
                !Regex("^\\d{1,2}(:\\d{2}){1,2}$").matches(part) &&
                part !in setOf("Song", "Video", "Lagu", "Album", "Single", "EP")
        }
        return parts.firstOrNull().orEmpty()
    }

    private fun parseDurationSeconds(text: String): Int {
        val match = Regex("\\b(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{2})\\b").find(text) ?: return 0
        val hours = match.groupValues[1].toIntOrNull() ?: 0
        val mins = match.groupValues[2].toIntOrNull() ?: 0
        val secs = match.groupValues[3].toIntOrNull() ?: 0
        return hours * 3600 + mins * 60 + secs
    }

    private fun extractText(obj: JSONObject?): String {
        if (obj == null) return ""
        obj.optString("simpleText").takeIf { it.isNotBlank() }?.let { return it }
        val runs = obj.optJSONArray("runs") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text").orEmpty())
        }
        return sb.toString().trim()
    }

    private fun extractLastThumbnail(thumbObj: JSONObject?): String {
        val arr = thumbObj?.optJSONArray("thumbnails") ?: return ""
        for (i in arr.length() - 1 downTo 0) {
            val url = arr.optJSONObject(i)?.optString("url").orEmpty()
            if (url.startsWith("https://")) return url
        }
        return ""
    }

    private fun findObjectsByKey(node: Any?, targetKey: String): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        fun walk(current: Any?) {
            when (current) {
                is JSONObject -> {
                    val keys = current.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val v = current.opt(k)
                        if (k == targetKey && v is JSONObject) {
                            result.add(v)
                        }
                        walk(v)
                    }
                }
                is JSONArray -> {
                    for (i in 0 until current.length()) {
                        walk(current.opt(i))
                    }
                }
            }
        }
        walk(node)
        return result
    }

    private fun postJson(
        url: String,
        body: JSONObject,
        userAgent: String,
        extraHeaders: Map<String, String>,
    ): JSONObject? {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))

        extraHeaders.forEach { (k, v) ->
            if (k.isNotBlank() && v.isNotBlank()) {
                builder.header(k, v)
            }
        }

        client.newCall(builder.build()).execute().use { res ->
            if (!res.isSuccessful) return null
            val raw = res.body?.string().orEmpty()
            if (raw.isBlank()) return null
            return JSONObject(raw)
        }
    }

    private fun webRemixContext(): JSONObject = JSONObject().put(
        "client",
        JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", "1.20241127.01.00")
            .put("hl", "id")
            .put("gl", "ID"),
    )

    private fun webContext(): JSONObject = JSONObject().put(
        "client",
        JSONObject()
            .put("clientName", "WEB")
            .put("clientVersion", "2.20241127.01.00")
            .put("hl", "id")
            .put("gl", "ID"),
    )

    private fun androidVrContext(): JSONObject = JSONObject().put(
        "client",
        JSONObject()
            .put("clientName", "ANDROID_VR")
            .put("clientVersion", "1.60.19")
            .put("deviceMake", "Oculus")
            .put("deviceModel", "Quest 3")
            .put("osName", "Android")
            .put("osVersion", "12L")
            .put("androidSdkVersion", 32)
            .put("hl", "en")
            .put("gl", "US"),
    )

    private fun iosContext(): JSONObject = JSONObject().put(
        "client",
        JSONObject()
            .put("clientName", "IOS")
            .put("clientVersion", "19.45.4")
            .put("deviceMake", "Apple")
            .put("deviceModel", "iPhone16,2")
            .put("osName", "iPhone")
            .put("osVersion", "18.1.0.22B83")
            .put("hl", "en")
            .put("gl", "US"),
    )

    private fun androidContext(): JSONObject = JSONObject().put(
        "client",
        JSONObject()
            .put("clientName", "ANDROID")
            .put("clientVersion", "19.45.36")
            .put("osName", "Android")
            .put("osVersion", "14")
            .put("androidSdkVersion", 34)
            .put("hl", "en")
            .put("gl", "US"),
    )

    private data class PlayerClientConfig(
        val name: String,
        val userAgent: String,
        val context: JSONObject,
        val useAuth: Boolean,
    )
}
