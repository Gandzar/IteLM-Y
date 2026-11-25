package com.example.engine

import com.example.data.AppSettings
import com.example.data.MediaItem
import com.example.data.PlatformId
import com.example.data.ResolvedStreams
import com.example.data.ScrapeResult
import com.example.data.ScraperServer
import com.example.data.ScraperServerKind
import com.example.data.ServerStatus
import com.example.data.StreamCandidate
import com.example.debug.AppDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

object ServerManager {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val builtInServers: List<ScraperServer> = listOf(
        ScraperServer(
            id = "auto",
            name = "Auto Smart (Failover)",
            kind = ScraperServerKind.AUTO,
            baseUrl = "https://music.youtube.com",
            description = "Coba InnerTube tercepat & multi-platform scraper otomatis",
            supportsMultiPlatform = true,
            isBuiltIn = true,
        ),
        ScraperServer(
            id = "yt_vr",
            name = "YouTube VR Client",
            kind = ScraperServerKind.INNERTUBE_VR,
            baseUrl = "https://www.youtube.com",
            description = "Kualitas HD MP4 & M4A/Opus stabil tanpa perlu login",
            supportsMultiPlatform = false,
            isBuiltIn = true,
        ),
        ScraperServer(
            id = "yt_ios",
            name = "YouTube iOS Client",
            kind = ScraperServerKind.INNERTUBE_IOS,
            baseUrl = "https://www.youtube.com",
            description = "Stream mobile AAC & MP4 alternatif saat VR terbatas",
            supportsMultiPlatform = false,
            isBuiltIn = true,
        ),
        ScraperServer(
            id = "yt_android",
            name = "YouTube Android Client",
            kind = ScraperServerKind.INNERTUBE_ANDROID,
            baseUrl = "https://www.youtube.com",
            description = "Resolver Android standar dengan progresif MP4",
            supportsMultiPlatform = false,
            isBuiltIn = true,
        ),
        ScraperServer(
            id = "yt_web_remix",
            name = "YT Music Web (Akun Login)",
            kind = ScraperServerKind.INNERTUBE_WEB_REMIX,
            baseUrl = "https://music.youtube.com",
            description = "Memakai cookie Google & SAPISIDHASH untuk konten akun",
            supportsMultiPlatform = false,
            isBuiltIn = true,
        ),
        ScraperServer(
            id = "piped_private_coffee",
            name = "Piped Mirror #1",
            kind = ScraperServerKind.PIPED_MIRROR,
            baseUrl = "https://api.piped.private.coffee",
            description = "Server mirror YouTube eksternal alternatif (tanpa login)",
            supportsMultiPlatform = false,
            isBuiltIn = true,
        ),
        ScraperServer(
            id = "piped_kavin",
            name = "Piped Mirror #2",
            kind = ScraperServerKind.PIPED_MIRROR,
            baseUrl = "https://pipedapi.kavin.rocks",
            description = "Server mirror cadangan untuk audio/video YouTube",
            supportsMultiPlatform = false,
            isBuiltIn = true,
        ),
        ScraperServer(
            id = "builtin_scraper",
            name = "Builtin Multi-Scraper",
            kind = ScraperServerKind.BUILTIN_SCRAPER,
            baseUrl = "https://www.youtube.com/oembed",
            description = "Ekstraktor tautan langsung, oEmbed & media lintas platform",
            supportsMultiPlatform = true,
            isBuiltIn = true,
        ),
    )

    fun allServers(settings: AppSettings): List<ScraperServer> =
        builtInServers + settings.customServers

    fun resolveActiveServer(settings: AppSettings, overrideServerId: String? = null): ScraperServer {
        val targetId = overrideServerId ?: settings.activeServerId
        return allServers(settings).firstOrNull { it.id == targetId } ?: builtInServers.first()
    }

    fun formatSizeMb(sizeBytes: Long, bitrate: Int, durationSec: Int): String {
        val exact = sizeBytes > 0L
        val effectiveBytes = when {
            exact -> sizeBytes
            bitrate > 0 && durationSec > 0 -> (bitrate.toLong() * durationSec.toLong()) / 8L
            else -> 0L
        }
        if (effectiveBytes <= 0L) return ""
        val mb = effectiveBytes.toDouble() / (1024.0 * 1024.0)
        val formatted = if (mb >= 0.1) {
            String.format(Locale.US, "%.1f MB", mb)
        } else {
            val kb = effectiveBytes.toDouble() / 1024.0
            String.format(Locale.US, "%.0f KB", kb)
        }
        return if (exact) formatted else "~$formatted"
    }

    suspend fun pingServer(server: ScraperServer): ScraperServer = withContext(Dispatchers.IO) {
        val probeUrl = when (server.kind) {
            ScraperServerKind.AUTO, ScraperServerKind.INNERTUBE_WEB_REMIX -> "https://music.youtube.com/generate_204"
            ScraperServerKind.INNERTUBE_VR, ScraperServerKind.INNERTUBE_IOS, ScraperServerKind.INNERTUBE_ANDROID ->
                "https://www.youtube.com/generate_204"
            ScraperServerKind.PIPED_MIRROR -> "${server.baseUrl.trimEnd('/')}/streams/dQw4w9WgXcQ"
            ScraperServerKind.BUILTIN_SCRAPER -> "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=dQw4w9WgXcQ&format=json"
            ScraperServerKind.CUSTOM_API -> server.baseUrl
        }
        if (!probeUrl.startsWith("https://")) {
            return@withContext server.copy(lastStatus = ServerStatus.OFFLINE, lastPingMs = null)
        }
        val t0 = System.currentTimeMillis()
        val ok = runCatching {
            val req = Request.Builder()
                .url(probeUrl)
                .header("User-Agent", "YMusic-Lite/0.3.0")
                .build()
            client.newCall(req).execute().use { res ->
                res.isSuccessful || res.code in 200..399
            }
        }.getOrDefault(false)
        val elapsed = (System.currentTimeMillis() - t0).coerceAtLeast(1L)
        AppDiagnostics.info("Server", "Ping ${server.name}: ${if (ok) "${elapsed}ms" else "OFFLINE"}")
        server.copy(
            lastPingMs = if (ok) elapsed else null,
            lastStatus = if (ok) ServerStatus.ONLINE else ServerStatus.OFFLINE,
        )
    }

    suspend fun resolveWithServer(
        videoId: String,
        server: ScraperServer,
        authHeaders: Map<String, String>,
    ): ResolvedStreams? = withContext(Dispatchers.IO) {
        when (server.kind) {
            ScraperServerKind.AUTO -> InnerTubeClient.resolveStreams(videoId, authHeaders, null)
                ?.copy(sourceServerName = server.name)

            ScraperServerKind.INNERTUBE_VR -> InnerTubeClient.resolveStreams(videoId, authHeaders, "ANDROID_VR")
                ?.copy(sourceServerName = server.name)

            ScraperServerKind.INNERTUBE_IOS -> InnerTubeClient.resolveStreams(videoId, authHeaders, "IOS")
                ?.copy(sourceServerName = server.name)

            ScraperServerKind.INNERTUBE_ANDROID -> InnerTubeClient.resolveStreams(videoId, authHeaders, "ANDROID")
                ?.copy(sourceServerName = server.name)

            ScraperServerKind.INNERTUBE_WEB_REMIX -> InnerTubeClient.resolveStreams(videoId, authHeaders, "WEB_REMIX")
                ?.copy(sourceServerName = server.name)

            ScraperServerKind.PIPED_MIRROR -> fetchFromPipedMirror(server, videoId)
                ?: InnerTubeClient.resolveStreams(videoId, authHeaders, null)

            ScraperServerKind.BUILTIN_SCRAPER -> InnerTubeClient.resolveStreams(videoId, authHeaders, null)
                ?.copy(sourceServerName = server.name)

            ScraperServerKind.CUSTOM_API -> scrapeYoutubeViaCustomServer(server, "https://www.youtube.com/watch?v=$videoId", videoId)
                ?: InnerTubeClient.resolveStreams(videoId, authHeaders, null)
        }
    }

    suspend fun fetchFromPipedMirror(server: ScraperServer, videoId: String): ResolvedStreams? = withContext(Dispatchers.IO) {
        val base = server.baseUrl.trimEnd('/')
        if (!base.startsWith("https://")) return@withContext null
        runCatching {
            val req = Request.Builder()
                .url("$base/streams/$videoId")
                .header("Accept", "application/json")
                .header("User-Agent", "YMusic-Lite/0.3.0")
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@withContext null
                val json = JSONObject(res.body?.string().orEmpty())
                val title = json.optString("title")
                val author = json.optString("uploader")
                val duration = json.optInt("duration", 0)
                val thumb = json.optString("thumbnailUrl")
                    .ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" }

                val audios = mutableListOf<StreamCandidate>()
                val videos = mutableListOf<StreamCandidate>()

                val audioArr = json.optJSONArray("audioStreams") ?: JSONArray()
                for (i in 0 until audioArr.length()) {
                    val o = audioArr.optJSONObject(i) ?: continue
                    val u = o.optString("url")
                    if (!u.startsWith("https://")) continue
                    val bitrate = o.optInt("bitrate", 0)
                    val mime = o.optString("mimeType", "audio/mp4")
                    val ext = if (mime.contains("webm") || mime.contains("opus")) "opus" else "m4a"
                    val sizeBytes = o.optLong("contentLength", 0L)
                    val quality = o.optString("quality").ifBlank { "${bitrate / 1000} kbps" }
                    audios.add(
                        StreamCandidate(
                            url = u,
                            mimeType = mime,
                            ext = ext,
                            bitrate = bitrate,
                            qualityLabel = "${ext.uppercase()} $quality",
                            isAudio = true,
                            userAgent = "YMusic-Lite/0.3.0",
                            sizeBytes = sizeBytes,
                            formattedSize = formatSizeMb(sizeBytes, bitrate, duration),
                            hasAudio = true,
                            clientSourceLabel = server.name,
                        ),
                    )
                }

                val videoArr = json.optJSONArray("videoStreams") ?: JSONArray()
                for (i in 0 until videoArr.length()) {
                    val o = videoArr.optJSONObject(i) ?: continue
                    val u = o.optString("url")
                    if (!u.startsWith("https://")) continue
                    val bitrate = o.optInt("bitrate", 0)
                    val mime = o.optString("mimeType", "video/mp4")
                    val ext = if (mime.contains("webm")) "webm" else "mp4"
                    val videoOnly = o.optBoolean("videoOnly", false)
                    val fps = o.optInt("fps", 0)
                    val sizeBytes = o.optLong("contentLength", 0L)
                    val q = o.optString("quality").ifBlank { "MP4" }
                    val label = if (videoOnly) "$q (video-only)" else q
                    videos.add(
                        StreamCandidate(
                            url = u,
                            mimeType = mime,
                            ext = ext,
                            bitrate = bitrate,
                            qualityLabel = label,
                            isAudio = false,
                            userAgent = "YMusic-Lite/0.3.0",
                            sizeBytes = sizeBytes,
                            formattedSize = formatSizeMb(sizeBytes, bitrate, duration),
                            fps = fps,
                            hasAudio = !videoOnly,
                            clientSourceLabel = server.name,
                        ),
                    )
                }

                val sortedA = audios.sortedByDescending { it.bitrate }
                val sortedV = videos.sortedWith(
                    compareByDescending<StreamCandidate> { it.hasAudio }.thenByDescending { it.bitrate },
                )
                AppDiagnostics.info("Server", "Piped ${server.name}: ${sortedA.size} audio, ${sortedV.size} video")
                ResolvedStreams(
                    videoId = videoId,
                    title = title,
                    author = author,
                    durationSec = duration,
                    thumbnail = thumb,
                    audioStreams = sortedA,
                    videoStreams = sortedV,
                    bestAudio = sortedA.firstOrNull(),
                    bestVideo = sortedV.firstOrNull(),
                    sourceServerName = server.name,
                )
            }
        }.getOrNull()
    }

    suspend fun scrapeViaCustomJsonServer(server: ScraperServer, targetUrl: String): ScrapeResult? = withContext(Dispatchers.IO) {
        val base = server.baseUrl.trimEnd('/')
        if (!base.startsWith("https://")) return@withContext null
        runCatching {
            val payload = JSONObject().put("url", targetUrl)
            val req = Request.Builder()
                .url(base)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "YMusic-Lite/0.3.0")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@withContext null
                val body = res.body?.string().orEmpty()
                if (body.isBlank()) return@withContext null
                val json = JSONObject(body)
                val items = mutableListOf<MediaItem>()

                // Cobalt / scrapr-style JSON parsing
                json.optString("url").takeIf { it.startsWith("https://") }?.let { direct ->
                    val name = json.optString("filename")
                    val ext = name.substringAfterLast('.', "mp4")
                    val isAudio = ext in setOf("mp3", "m4a", "opus", "ogg", "wav")
                    items.add(
                        MediaItem(
                            type = if (isAudio) "audio" else "video",
                            quality = server.name,
                            url = direct,
                            ext = ext,
                        ),
                    )
                }
                val arr = json.optJSONArray("items") ?: json.optJSONArray("picker") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val u = o.optString("url")
                    if (!u.startsWith("https://")) continue
                    items.add(
                        MediaItem(
                            type = o.optString("type", "video"),
                            quality = o.optString("quality", server.name),
                            url = u,
                            ext = o.optString("ext").ifBlank { null },
                            size = o.optString("size").ifBlank { null },
                        ),
                    )
                }
                if (items.isEmpty()) return@withContext null
                ScrapeResult(
                    status = true,
                    platform = PlatformDetector.detectPlatform(targetUrl),
                    platformLabel = server.name,
                    title = json.optString("title").ifBlank { "Media dari ${server.name}" },
                    author = json.optString("author").ifBlank { null },
                    thumbnail = json.optString("thumbnail").ifBlank { null },
                    items = items,
                    rawUrl = targetUrl,
                )
            }
        }.getOrNull()
    }

    private suspend fun scrapeYoutubeViaCustomServer(
        server: ScraperServer,
        url: String,
        videoId: String,
    ): ResolvedStreams? {
        val scraped = scrapeViaCustomJsonServer(server, url) ?: return null
        val audios = scraped.items.filter { it.type == "audio" }.map {
            StreamCandidate(
                url = it.url,
                mimeType = "audio/mp4",
                ext = it.ext ?: "mp3",
                qualityLabel = it.quality ?: "Audio",
                isAudio = true,
                formattedSize = it.size.orEmpty(),
                clientSourceLabel = server.name,
            )
        }
        val videos = scraped.items.filter { it.type != "audio" }.map {
            StreamCandidate(
                url = it.url,
                mimeType = "video/mp4",
                ext = it.ext ?: "mp4",
                qualityLabel = it.quality ?: "Video",
                isAudio = false,
                formattedSize = it.size.orEmpty(),
                clientSourceLabel = server.name,
            )
        }
        return ResolvedStreams(
            videoId = videoId,
            title = scraped.title,
            author = scraped.author.orEmpty(),
            thumbnail = scraped.thumbnail.orEmpty(),
            audioStreams = audios,
            videoStreams = videos,
            bestAudio = audios.firstOrNull(),
            bestVideo = videos.firstOrNull(),
            sourceServerName = server.name,
        )
    }
}
