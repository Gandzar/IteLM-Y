package com.example.download

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.AppRepository
import com.example.data.DownloadFormat
import com.example.data.DownloadJob
import com.example.data.DownloadStatus
import com.example.data.Track
import com.example.debug.AppDiagnostics
import com.example.engine.InnerTubeClient
import com.example.engine.MediaScraper
import com.example.notify.AppNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class DownloadManager(
    private val context: Context,
    private val repository: AppRepository,
    private val authHeadersProvider: () -> Map<String, String> = { emptyMap() },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val jobs: StateFlow<List<DownloadJob>> = _jobs.asStateFlow()

    init {
        scope.launch {
            _jobs.value = repository.downloadsFlow.first()
        }
    }

    fun enqueue(
        track: Track,
        format: DownloadFormat,
        overrideExt: String? = null,
        overrideUserAgent: String? = null,
    ): DownloadJob {
        val job = DownloadJob(
            id = "dl-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(5)}",
            track = track,
            format = format,
            status = DownloadStatus.QUEUED,
            progress = 0,
        )
        AppDiagnostics.info("Download", "Queued ${format.label}: ${track.title}")
        updateJobs { listOf(job) + it }
        DownloadForegroundService.sync(context)
        scope.launch { processJob(job.id, track, format, overrideExt, overrideUserAgent) }
        return job
    }

    private suspend fun processJob(
        jobId: String,
        track: Track,
        format: DownloadFormat,
        overrideExt: String?,
        overrideUserAgent: String?,
    ) {
        updateSingle(jobId) { it.copy(status = DownloadStatus.DOWNLOADING, progress = 5) }

        var targetUrl = track.url?.takeIf { it.isNotBlank() && MediaScraper.isDirectMediaUrl(it) }
        var userAgent = overrideUserAgent?.takeIf { it.isNotBlank() } ?: "YMusic-Lite/0.3.0"
        var actualExt = overrideExt?.takeIf { it.isNotBlank() } ?: format.ext

        // Resolve via InnerTube when track is a YouTube/YT Music item without direct stream URL
        if (targetUrl == null && !track.videoId.isNullOrBlank()) {
            val resolved = runCatching {
                InnerTubeClient.resolveStreams(track.videoId, authHeadersProvider())
            }.getOrNull()
            val candidate = if (format == DownloadFormat.MP3) {
                resolved?.bestAudio
            } else {
                resolved?.videoStreams?.firstOrNull { !it.qualityLabel.contains("video-only") }
                    ?: resolved?.bestVideo
                    ?: resolved?.bestAudio
            }
            if (candidate != null) {
                targetUrl = candidate.url
                if (candidate.userAgent.isNotBlank()) userAgent = candidate.userAgent
                actualExt = candidate.ext
            }
        }

        if (targetUrl == null) {
            val errMsg = "Stream URL belum tersedia untuk video ini. Coba login akun Google atau tempel URL media langsung."
            AppDiagnostics.error("Download", "No stream URL for ${track.title}")
            var failedJob: DownloadJob? = null
            updateSingle(jobId) {
                val next = it.copy(status = DownloadStatus.ERROR, progress = 0, error = errMsg)
                failedJob = next
                next
            }
            failedJob?.let { AppNotifications.notifyDownloadFailed(context, it) }
            return
        }

        try {
            val req = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", userAgent)
                .build()

            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    throw IllegalStateException("HTTP ${res.code}")
                }
                val body = res.body ?: throw IllegalStateException("Respons kosong")
                val contentLength = body.contentLength()
                val fileName = sanitizeFileName("${track.artist} - ${track.title}.$actualExt")
                val folder = repository.getSettings().downloadFolder.ifBlank { "YMusicLite" }

                val mimeType = when (actualExt.lowercase()) {
                    "m4a", "aac" -> "audio/mp4"
                    "opus", "ogg" -> "audio/ogg"
                    "mp3" -> "audio/mpeg"
                    "wav" -> "audio/wav"
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    "webm" -> "video/webm"
                    else -> "video/mp4"
                }

                val saved = saveStream(
                    fileName = fileName,
                    folder = folder,
                    mimeType = mimeType,
                ) { outStream ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesReadTotal = 0L
                        var lastReported = 5
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            outStream.write(buffer, 0, read)
                            bytesReadTotal += read
                            if (contentLength > 0) {
                                val pct = ((bytesReadTotal * 90) / contentLength).toInt().coerceIn(5, 95)
                                if (pct >= lastReported + 5) {
                                    lastReported = pct
                                    updateSingle(jobId) { it.copy(progress = pct) }
                                }
                            }
                        }
                    }
                }

                updateSingle(jobId) { it.copy(status = DownloadStatus.CONVERTING, progress = 96) }
                delay(150)
                var doneJob: DownloadJob? = null
                updateSingle(jobId) {
                    val next = it.copy(
                        status = DownloadStatus.DONE,
                        progress = 100,
                        filePath = saved.displayPath,
                        contentUri = saved.uriString,
                        mimeType = mimeType,
                    )
                    doneJob = next
                    next
                }
                AppDiagnostics.info("Download", "Finished: ${saved.displayPath}")
                doneJob?.let { AppNotifications.notifyDownloadFinished(context, it) }
            }
        } catch (e: Exception) {
            AppDiagnostics.error("Download", "Failed ${track.title}: ${e.message}")
            var failedJob: DownloadJob? = null
            updateSingle(jobId) {
                val next = it.copy(
                    status = DownloadStatus.ERROR,
                    error = e.message ?: "Download gagal",
                )
                failedJob = next
                next
            }
            failedJob?.let { AppNotifications.notifyDownloadFailed(context, it) }
        }
    }

    private data class SavedFileInfo(val displayPath: String, val uriString: String)

    private fun saveStream(
        fileName: String,
        folder: String,
        mimeType: String,
        writer: (java.io.OutputStream) -> Unit,
    ): SavedFileInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/$folder")
            }
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
                ?: throw IllegalStateException("Gagal membuat file di Documents/$folder")
            resolver.openOutputStream(uri)?.use(writer)
                ?: throw IllegalStateException("Tidak dapat menulis ke file")
            return SavedFileInfo(
                displayPath = "Documents/$folder/$fileName",
                uriString = uri.toString(),
            )
        } else {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val dir = File(baseDir, folder).apply { if (!exists()) mkdirs() }
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use(writer)
            return SavedFileInfo(
                displayPath = outFile.absolutePath,
                uriString = android.net.Uri.fromFile(outFile).toString(),
            )
        }
    }

    fun remove(id: String) {
        updateJobs { list -> list.filterNot { it.id == id } }
    }

    fun clearFinished() {
        updateJobs { list ->
            list.filter {
                it.status == DownloadStatus.DOWNLOADING ||
                    it.status == DownloadStatus.QUEUED ||
                    it.status == DownloadStatus.CONVERTING
            }
        }
    }

    private fun updateSingle(id: String, transform: (DownloadJob) -> DownloadJob) {
        updateJobs { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    private fun updateJobs(transform: (List<DownloadJob>) -> List<DownloadJob>) {
        val updated = transform(_jobs.value)
        _jobs.value = updated
        scope.launch { repository.saveDownloads(updated) }
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[<>:\"/\\\\|?*]"), "_").take(120)
}
