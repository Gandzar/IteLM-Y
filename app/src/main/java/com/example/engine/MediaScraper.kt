package com.example.engine

import com.example.data.MediaItem
import com.example.data.PlatformId
import com.example.data.ScrapeResult
import com.example.data.Track
import com.example.data.TrackSource
import com.example.debug.AppDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object MediaScraper {
    const val SCRAPER_VERSION = "0.3.0-innertube"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val directMediaRegex = Regex("\\.(mp3|m4a|wav|ogg|opus|aac|flac|mp4|webm|mkv|m3u8|mpd)(\\?.*)?$", RegexOption.IGNORE_CASE)

    fun isDirectMediaUrl(url: String): Boolean {
        val cleaned = url.trim()
        return cleaned.startsWith("/") ||
            cleaned.startsWith("content://", ignoreCase = true) ||
            cleaned.startsWith("file://", ignoreCase = true) ||
            directMediaRegex.containsMatchIn(cleaned) ||
            cleaned.contains("googlevideo.com/videoplayback", ignoreCase = true)
    }

    suspend fun scrapeUrl(
        url: String,
        authHeaders: Map<String, String> = emptyMap(),
        server: com.example.data.ScraperServer = ServerManager.builtInServers.first(),
    ): ScrapeResult = withContext(Dispatchers.IO) {
        val cleaned = url.trim()
        if (cleaned.isEmpty()) {
            return@withContext emptyResult(cleaned, PlatformId.UNKNOWN, "URL kosong")
        }

        if (isDirectMediaUrl(cleaned)) {
            val ext = directMediaRegex.find(cleaned)?.groupValues?.getOrNull(1)?.lowercase() ?: "m4a"
            val isAudio = ext in setOf("mp3", "m4a", "wav", "ogg", "opus", "aac", "flac")
            val fileName = cleaned.substringAfterLast('/').substringBefore('?').ifBlank { "Direct Media" }
            val probeSize = probeRemoteSizeLabel(cleaned)
            return@withContext ScrapeResult(
                status = true,
                platform = PlatformId.UNKNOWN,
                platformLabel = "Direct Media",
                title = fileName,
                author = "URL Langsung",
                items = listOf(
                    MediaItem(
                        type = if (isAudio) "audio" else "video",
                        quality = "Direct Stream",
                        url = cleaned,
                        ext = ext,
                        size = probeSize,
                    ),
                ),
                rawUrl = cleaned,
            )
        }

        if (server.kind == com.example.data.ScraperServerKind.CUSTOM_API) {
            val customRes = ServerManager.scrapeViaCustomJsonServer(server, cleaned)
            if (customRes != null) return@withContext customRes
        }

        val platform = PlatformDetector.detectPlatform(cleaned)
        val label = PlatformDetector.platformLabel(platform)
        AppDiagnostics.info("Scraper", "Scraping $label via ${server.name}")

        try {
            when (platform) {
                PlatformId.YOUTUBE -> scrapeYoutube(cleaned, authHeaders, server)
                PlatformId.TIKTOK -> scrapeTiktok(cleaned)
                else -> scrapeGeneric(cleaned, platform, label)
            }
        } catch (e: Exception) {
            AppDiagnostics.error("Scraper", "Scrape failed ($label): ${e.message}")
            emptyResult(cleaned, platform, e.message ?: "Scrape gagal")
        }
    }

    private fun probeRemoteSizeLabel(url: String): String? = runCatching {
        val req = Request.Builder().url(url).head().header("User-Agent", "YMusic-Lite/0.3.0").build()
        client.newCall(req).execute().use { res ->
            val len = res.header("Content-Length")?.toLongOrNull() ?: 0L
            ServerManager.formatSizeMb(len, 0, 0).ifBlank { null }
        }
    }.getOrNull()

    private fun emptyResult(url: String, platform: PlatformId, error: String): ScrapeResult = ScrapeResult(
        status = false,
        platform = platform,
        platformLabel = PlatformDetector.platformLabel(platform),
        title = "",
        items = emptyList(),
        error = error,
        rawUrl = url,
    )

    private suspend fun scrapeYoutube(
        url: String,
        authHeaders: Map<String, String>,
        server: com.example.data.ScraperServer,
    ): ScrapeResult {
        val videoId = PlatformDetector.extractYoutubeId(url)
            ?: return emptyResult(url, PlatformId.YOUTUBE, "Video ID tidak ditemukan")

        val resolved = runCatching {
            ServerManager.resolveWithServer(videoId, server, authHeaders)
        }.getOrNull()

        var title = resolved?.title?.takeIf { it.isNotBlank() } ?: "YouTube Video"
        var author = resolved?.author.orEmpty()
        var thumbnail = resolved?.thumbnail?.takeIf { it.isNotBlank() }
            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        val duration = resolved?.durationSec?.takeIf { it > 0 }

        // Supplement metadata with oEmbed if title is blank
        if (title == "YouTube Video" || author.isBlank()) {
            runCatching {
                val encoded = URLEncoder.encode("https://www.youtube.com/watch?v=$videoId", "UTF-8")
                val reqBuilder = Request.Builder()
                    .url("https://www.youtube.com/oembed?url=$encoded&format=json")
                    .header("User-Agent", "YMusic-Lite/0.3.0")
                authHeaders.forEach { (k, v) ->
                    if (k.isNotBlank() && v.isNotBlank()) reqBuilder.header(k, v)
                }
                client.newCall(reqBuilder.build()).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string().orEmpty()
                        if (body.isNotBlank()) {
                            val json = JSONObject(body)
                            title = json.optString("title", title).ifBlank { title }
                            if (author.isBlank()) author = json.optString("author_name", "")
                            thumbnail = json.optString("thumbnail_url", thumbnail).ifBlank { thumbnail }
                        }
                    }
                }
            }
        }

        val items = buildList {
            resolved?.audioStreams?.forEach { s ->
                add(
                    MediaItem(
                        type = "audio",
                        quality = s.qualityLabel,
                        url = s.url,
                        ext = s.ext,
                        size = s.formattedSize.ifBlank { null },
                    ),
                )
            }
            resolved?.videoStreams?.forEach { s ->
                add(
                    MediaItem(
                        type = "video",
                        quality = s.qualityLabel,
                        url = s.url,
                        ext = s.ext,
                        size = s.formattedSize.ifBlank { null },
                    ),
                )
            }
            if (isEmpty()) {
                add(
                    MediaItem(
                        type = "video",
                        quality = "YouTube (buka di player)",
                        url = "https://www.youtube.com/watch?v=$videoId",
                        ext = "mp4",
                    ),
                )
            }
        }

        val baseLabel = if (url.contains("music.youtube.com", ignoreCase = true)) "YouTube Music" else "YouTube"
        val resolvedLabel = if (resolved?.sourceServerName?.isNotBlank() == true) {
            "$baseLabel · ${resolved.sourceServerName}"
        } else {
            baseLabel
        }

        return ScrapeResult(
            status = true,
            platform = PlatformId.YOUTUBE,
            platformLabel = resolvedLabel,
            title = title,
            author = author.ifBlank { null },
            thumbnail = thumbnail,
            duration = duration,
            items = items,
            rawUrl = url,
        )
    }

    private fun scrapeTiktok(url: String): ScrapeResult = ScrapeResult(
        status = true,
        platform = PlatformId.TIKTOK,
        platformLabel = "TikTok",
        title = "TikTok Video",
        items = listOf(
            MediaItem(
                type = "video",
                quality = "original",
                url = url,
                ext = "mp4",
            ),
        ),
        rawUrl = url,
        error = "Direct stream butuh engine scraper penuh. Update metadata engine di Settings atau tempel URL media langsung.",
    )

    private fun scrapeGeneric(url: String, platform: PlatformId, label: String): ScrapeResult = ScrapeResult(
        status = true,
        platform = platform,
        platformLabel = label,
        title = "$label media",
        items = listOf(
            MediaItem(
                type = "file",
                quality = "source link",
                url = url,
            ),
        ),
        rawUrl = url,
        error = "Platform ini butuh engine scraper. Cek versi scraper di Settings atau buka tautan sumber.",
    )

    fun scrapeToTrack(r: ScrapeResult): Track? {
        if (!r.status || r.title.isBlank()) return null
        val videoId = if (r.platform == PlatformId.YOUTUBE) {
            PlatformDetector.extractYoutubeId(r.rawUrl)
        } else {
            null
        }
        val best = r.items.firstOrNull { it.type == "audio" } ?: r.items.firstOrNull()
        return Track(
            id = videoId ?: "url-${r.rawUrl.hashCode()}-${System.currentTimeMillis() % 10000}",
            title = r.title,
            artist = r.author?.ifBlank { null } ?: r.platformLabel,
            duration = r.duration ?: 0,
            thumbnail = r.thumbnail.orEmpty(),
            source = if (r.platform == PlatformId.YOUTUBE) TrackSource.YOUTUBE else TrackSource.URL,
            videoId = videoId,
            url = best?.url,
        )
    }
}
