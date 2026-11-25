package com.example

import android.app.Application
import com.example.auth.YouTubeAuthManager
import com.example.data.AppRepository
import com.example.debug.AppDiagnostics
import com.example.download.DownloadManager
import com.example.notify.AppNotifications
import com.example.player.YMusicPlayer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class YMusicApplication : Application() {
    val authManager: YouTubeAuthManager by lazy { YouTubeAuthManager(this) }
    val repository: AppRepository by lazy { AppRepository(this) }
    val player: YMusicPlayer by lazy { YMusicPlayer(this) { authManager.buildAuthHeaders() } }
    val downloadManager: DownloadManager by lazy { DownloadManager(this, repository) { authManager.buildAuthHeaders() } }

    private val _globalToast = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val globalToast: SharedFlow<String> = _globalToast.asSharedFlow()

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppDiagnostics.init(this)
        AppNotifications.ensureChannels(this)
    }

    fun postGlobalToast(msg: String) {
        _globalToast.tryEmit(msg)
    }

    companion object {
        @Volatile
        lateinit var instance: YMusicApplication
            private set
    }
}
