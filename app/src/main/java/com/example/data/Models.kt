package com.example.data

import kotlinx.serialization.Serializable

@Serializable
enum class TrackSource {
    YOUTUBE,
    LOCAL,
    URL,
}

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Int = 0,
    val thumbnail: String = "",
    val source: TrackSource = TrackSource.YOUTUBE,
    val url: String? = null,
    val videoId: String? = null,
)

@Serializable
enum class PlatformId {
    YOUTUBE,
    TIKTOK,
    INSTAGRAM,
    TWITTER,
    FACEBOOK,
    SPOTIFY,
    SOUNDCLOUD,
    PINTEREST,
    REDDIT,
    BILIBILI,
    THREADS,
    UNKNOWN,
}

@Serializable
data class MediaItem(
    val type: String, // "video" | "audio" | "image" | "file"
    val quality: String? = null,
    val url: String,
    val ext: String? = null,
    val size: String? = null,
)

@Serializable
data class ScrapeResult(
    val status: Boolean,
    val platform: PlatformId,
    val platformLabel: String,
    val title: String,
    val author: String? = null,
    val thumbnail: String? = null,
    val duration: Int? = null,
    val items: List<MediaItem> = emptyList(),
    val error: String? = null,
    val rawUrl: String,
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
enum class DownloadFormat(val ext: String, val label: String) {
    MP3("mp3", "MP3"),
    MP4("mp4", "MP4"),
}

@Serializable
enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    CONVERTING,
    DONE,
    ERROR,
}

@Serializable
data class DownloadJob(
    val id: String,
    val track: Track,
    val format: DownloadFormat,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Int = 0,
    val error: String? = null,
    val filePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class AppSettings(
    val engineVersion: String = "0.2.0-lite",
    val lastEngineCheck: Long = 0L,
    val quality: String = "high",
    val downloadFolder: String = "YMusicLite",
    val autoUpdateEngine: Boolean = false,
    val customEngineUrl: String = "",
    val useCustomEngine: Boolean = false,
    val autoCheckAppUpdate: Boolean = true,
    val youtubeApiKey: String = "",
)

@Serializable
data class ScraprMeta(
    val version: String,
    val updatedAt: Long,
    val source: String,
    val description: String? = null,
    val tarball: String? = null,
    val homepage: String? = null,
    val size: Long? = null,
)

data class ScraprUpdateInfo(
    val current: String,
    val latest: String,
    val isNewer: Boolean,
    val description: String? = null,
    val tarball: String? = null,
    val homepage: String? = null,
    val publishedVia: String,
)

data class ReleaseInfo(
    val tag: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val htmlUrl: String,
    val apkUrl: String? = null,
    val apkName: String? = null,
    val isNewer: Boolean,
)

@Serializable
data class EngineMeta(
    val version: String,
    val sourceUrl: String,
    val downloadedAt: Long,
    val size: Int,
)

enum class AppTab(val route: String) {
    DOWNLOAD("download"),
    SEARCH("search"),
    SPATIAL("spatial"),
    LIBRARY("library"),
    HISTORY("history"),
    SETTINGS("settings"),
}
