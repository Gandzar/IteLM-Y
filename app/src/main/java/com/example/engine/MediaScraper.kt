package com.example.engine

import com.example.data.MediaItem
import com.example.data.PlatformId
import com.example.data.ScrapeResult
import com.example.data.Track
import com.example.data.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object MediaScraper {
    const val SCRAPER_VERSION = "0.2.0-lite"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val directMediaRegex = Regex("\\.(mp3|m4a|wav|ogg|aac|flac|mp4|webm|mkv)(\\?.*)?$", RegexOption.IGNORE_CASE)

    fun isDirectMediaUrl(url: String): Boolean = directMediaRegex.containsMatchIn(url.trim())

    suspend fun scrapeUrl(url: String): ScrapeResult = withContext(Dispatchers.IO) {
        val cleaned = url.trim()
        if (cleaned.isEmpty()) {
            return@withContext emptyResult(cleaned, PlatformId.UNKNOWN, "URL kosong")
        }

        if (isDirectMediaUrl(cleaned)) {
            val ext = directMediaRegex.find(cleaned)?.groupValues?.getOrNull(1)?.lowercase() ?: "mp3"
            val isAudio = ext in setOf("mp3", "m4a", "wav", "ogg", "aac", "flac")
            val fileName = cleaned.substringAfterLast('/').substringBefore('?').ifBlank { "Direct Media" }
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
                    ),
                ),
                rawUrl = cleaned,
            )
        }

        val platform = PlatformDetector.detectPlatform(cleaned)
        val label = PlatformDetector.platformLabel(platform)

        try {
            when (platform) {
                PlatformId.YOUTUBE -> scrapeYoutube(cleaned)
                PlatformId.TIKTOK -> scrapeTiktok(cleaned)
                else -> scrapeGeneric(cleaned, platform, label)
            }
        } catch (e: Exception) {
            emptyResult(cleaned, platform, e.message ?: "Scrape gagal")
        }
    }

    private fun emptyResult(url: String, platform: PlatformId, error: String): ScrapeResult = ScrapeResult(
        status = false,
        platform = platform,
        platformLabel = PlatformDetector.platformLabel(platform),
        title = "",
        items = emptyList(),
        error = error,
        rawUrl = url,
    )

    private fun scrapeYoutube(url: String): ScrapeResult {
        val videoId = PlatformDetector.extractYoutubeId(url)
            ?: return emptyResult(url, PlatformId.YOUTUBE, "Video ID tidak ditemukan")

        var title = "YouTube Video"
        var author = ""
        var thumbnail = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        runCatching {
            val encoded = URLEncoder.encode("https://www.youtube.com/watch?v=$videoId", "UTF-8")
            val req = Request.Builder()
                .url("https://www.youtube.com/oembed?url=$encoded&format=json")
                .header("User-Agent", "YMusic-Lite/0.2.0")
                .build()
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        title = json.optString("title", title).ifBlank { title }
                        author = json.optString("author_name", "")
                        thumbnail = json.optString("thumbnail_url", thumbnail).ifBlank { thumbnail }
                    }
                }
            }
        }

        val items = listOf(
            MediaItem(
                type = "video",
                quality = "YouTube (buka di player)",
                url = "https://www.youtube.com/watch?v=$videoId",
                ext = "mp4",
            ),
        )

        return ScrapeResult(
            status = true,
            platform = PlatformId.YOUTUBE,
            platformLabel = "YouTube",
            title = title,
            author = author.ifBlank { null },
            thumbnail = thumbnail,
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
