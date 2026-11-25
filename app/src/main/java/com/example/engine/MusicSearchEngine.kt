package com.example.engine

import com.example.data.Track
import com.example.data.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class EngineSearchResult(
    val tracks: List<Track>,
    val nextPageToken: String? = null,
    val isDemo: Boolean = false,
)

object MusicSearchEngine {
    const val ENGINE_VERSION = "0.2.0-lite"
    private const val YT_API_BASE = "https://www.googleapis.com/youtube/v3"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun searchTracks(
        query: String,
        apiKey: String = "",
        maxResults: Int = 20,
    ): EngineSearchResult = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext EngineSearchResult(emptyList())

        val key = apiKey.trim()
        if (key.isEmpty()) {
            return@withContext EngineSearchResult(demoTracks(q), isDemo = true)
        }

        try {
            val encodedQ = URLEncoder.encode(q, "UTF-8")
            val url = "$YT_API_BASE/search?part=snippet&type=video&maxResults=$maxResults&q=$encodedQ&key=$key&videoCategoryId=10"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    return@withContext EngineSearchResult(demoTracks(q), isDemo = true)
                }
                val body = res.body?.string().orEmpty()
                val json = JSONObject(body)
                val arr = json.optJSONArray("items")
                val tracks = buildList {
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val videoId = item.getJSONObject("id").optString("videoId")
                            val snippet = item.getJSONObject("snippet")
                            val thumbs = snippet.optJSONObject("thumbnails")
                            val thumb = thumbs?.optJSONObject("medium")?.optString("url")
                                ?: thumbs?.optJSONObject("default")?.optString("url")
                                ?: ""
                            if (videoId.isNotBlank()) {
                                add(
                                    Track(
                                        id = videoId,
                                        videoId = videoId,
                                        title = snippet.optString("title"),
                                        artist = snippet.optString("channelTitle"),
                                        duration = 0,
                                        thumbnail = thumb,
                                        source = TrackSource.YOUTUBE,
                                    ),
                                )
                            }
                        }
                    }
                }
                EngineSearchResult(
                    tracks = tracks.ifEmpty { demoTracks(q) },
                    nextPageToken = json.optString("nextPageToken").ifBlank { null },
                    isDemo = tracks.isEmpty(),
                )
            }
        } catch (_: Exception) {
            EngineSearchResult(demoTracks(q), isDemo = true)
        }
    }

    private fun demoTracks(query: String): List<Track> = listOf(
        Track(
            id = "demo-1",
            videoId = "dQw4w9WgXcQ",
            title = "$query – Demo Track 1",
            artist = "YMusic Lite Demo",
            duration = 212,
            thumbnail = "https://i.ytimg.com/vi/dQw4w9WgXcQ/mqdefault.jpg",
            source = TrackSource.YOUTUBE,
        ),
        Track(
            id = "demo-2",
            videoId = "9bZkp7q19f0",
            title = "$query – Demo Track 2",
            artist = "YMusic Lite Demo",
            duration = 253,
            thumbnail = "https://i.ytimg.com/vi/9bZkp7q19f0/mqdefault.jpg",
            source = TrackSource.YOUTUBE,
        ),
    )
}
