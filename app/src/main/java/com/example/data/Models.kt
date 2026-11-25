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
    val badgeText: String = "",
    val subtitle: String = "",
    val isMix: Boolean = false,
    val playlistBrowseId: String? = null,
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
    val contentUri: String? = null,
    val mimeType: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class ScraperServerKind(val label: String) {
    AUTO("Auto Failover"),
    INNERTUBE_VR("InnerTube VR"),
    INNERTUBE_IOS("InnerTube iOS"),
    INNERTUBE_ANDROID("InnerTube Android"),
    INNERTUBE_WEB_REMIX("YT Music Web (Login)"),
    PIPED_MIRROR("Piped Mirror"),
    BUILTIN_SCRAPER("Builtin Scraper"),
    CUSTOM_API("Custom API Server"),
}

@Serializable
enum class ServerStatus {
    UNKNOWN,
    ONLINE,
    OFFLINE,
}

@Serializable
data class ScraperServer(
    val id: String,
    val name: String,
    val kind: ScraperServerKind,
    val baseUrl: String = "",
    val description: String = "",
    val supportsMultiPlatform: Boolean = true,
    val isBuiltIn: Boolean = true,
    val lastPingMs: Long? = null,
    val lastStatus: ServerStatus = ServerStatus.UNKNOWN,
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
    val activeServerId: String = "auto",
    val customServers: List<ScraperServer> = emptyList(),
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

enum class AppTab(val route: String, val title: String, val isPrimaryTab: Boolean) {
    HOME("home", "Beranda", true),
    TRENDING("trending", "Musik trending", true),
    RECOMMENDED("recommended", "Rekomendasi", true),
    TOP_SONGS("top_songs", "Lagu teratas", true),
    LIBRARY("library", "Perpustakaan", true),
    SEARCH("search", "Cari", false),
    DOWNLOAD("download", "All Downloader", false),
    SERVERS("servers", "Menu Server", false),
    SPATIAL("spatial", "Spatial 8D", false),
    HISTORY("history", "Riwayat", false),
    SETTINGS("settings", "Pengaturan", false),
    DEBUG("debug", "Debug & RAM", false),
}

enum class HomeChip(val label: String, val queryHint: String?) {
    ALL("Semua", null),
    MUSIC("Musik", "musik mix hits indonesia"),
    MIX("Mix", "mix playlist dj music"),
    LIVE("Live", "live music performance session"),
    CARTOON("Kartun", "anime ost music video"),
    NATURE("Alam", "relaxing nature ambient music"),
}

enum class LibrarySubView {
    OVERVIEW,
    LOCAL,
    LIKED_MUSIC,
    PLAYLISTS,
}

@Serializable
data class AccountProfile(
    val isLoggedIn: Boolean = false,
    val name: String = "",
    val handle: String = "",
    val avatarUrl: String = "",
    val lastSyncedAt: Long = 0L,
)

@Serializable
data class YtPlaylist(
    val id: String,
    val browseId: String,
    val title: String,
    val subtitle: String = "",
    val trackCount: String = "",
    val thumbnail: String = "",
)

data class StreamCandidate(
    val url: String,
    val mimeType: String,
    val ext: String,
    val bitrate: Int = 0,
    val qualityLabel: String = "",
    val isAudio: Boolean = true,
    val userAgent: String = "",
    val sizeBytes: Long = 0L,
    val formattedSize: String = "",
    val fps: Int = 0,
    val hasAudio: Boolean = true,
    val clientSourceLabel: String = "",
)

data class ResolvedStreams(
    val videoId: String,
    val title: String = "",
    val author: String = "",
    val durationSec: Int = 0,
    val thumbnail: String = "",
    val audioStreams: List<StreamCandidate> = emptyList(),
    val videoStreams: List<StreamCandidate> = emptyList(),
    val bestAudio: StreamCandidate? = null,
    val bestVideo: StreamCandidate? = null,
    val sourceServerName: String = "",
)

enum class RepeatMode(val label: String) {
    OFF("Mati"),
    ALL("Ulangi Semua"),
    ONE("Ulangi 1 Lagu"),
}

data class YtComment(
    val id: String,
    val author: String,
    val avatarUrl: String = "",
    val text: String,
    val publishedTime: String = "",
    val likeCount: String = "",
)

data class CommentsPageResult(
    val comments: List<YtComment> = emptyList(),
    val continuationToken: String? = null,
    val createCommentParams: String? = null,
    val isLikedByMe: Boolean? = null,
    val likeCountText: String = "",
)

data class LyricsState(
    val videoId: String = "",
    val text: String = "",
    val sourceLabel: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class QualityPickerRequest(
    val id: String = "qp-${System.currentTimeMillis()}",
    val track: Track? = null,
    val rawUrl: String = "",
    val initialFormat: DownloadFormat? = null,
)

enum class LibrarySection(val label: String) {
    LOCAL("Lokal"),
    LIKED_MUSIC("Liked Music"),
    PLAYLISTS("Playlist"),
}

enum class HistorySection(val label: String) {
    SCRAPE("Scrape"),
    YOUTUBE_MUSIC("YT Music"),
}

enum class PreviewKind(val label: String) {
    VIDEO("Video"),
    AUDIO("Audio"),
    IMAGE("Gambar"),
    FILE("Berkas"),
}

data class PreviewTarget(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val urlOrUri: String,
    val mimeType: String = "",
    val ext: String = "",
    val kind: PreviewKind,
    val thumbnail: String = "",
    val videoId: String? = null,
    val downloadJobId: String? = null,
    val userAgent: String = "YMusic-Lite/0.3.0",
) {
    companion object {
        fun detectKind(mimeType: String?, ext: String?, typeHint: String? = null): PreviewKind {
            val m = mimeType.orEmpty().lowercase()
            val e = ext.orEmpty().lowercase().trimStart('.')
            val t = typeHint.orEmpty().lowercase()
            return when {
                m.startsWith("video/") || e in setOf("mp4", "webm", "mkv", "mov", "3gp", "m4v") || t == "video" -> PreviewKind.VIDEO
                m.startsWith("audio/") || e in setOf("mp3", "m4a", "opus", "ogg", "wav", "aac", "flac") || t == "audio" -> PreviewKind.AUDIO
                m.startsWith("image/") || e in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp") || t == "image" -> PreviewKind.IMAGE
                else -> PreviewKind.FILE
            }
        }
    }
}

