package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.debug.AppDiagnostics
import com.example.player.YMusicPlayer
import com.example.ui.theme.YtCard
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtOrange
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTextSecondary
import kotlin.math.roundToInt

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbedOverlay(
    videoId: String,
    initialSeekSec: Float,
    expanded: Boolean,
    player: YMusicPlayer,
    onSwitchToAudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val safeVideoId = remember(videoId) { sanitizeVideoId(videoId) }
    var useSoftwareLayer by remember { mutableStateOf(isVirtualRenderDevice()) }
    var crashCount by remember(safeVideoId) { mutableIntStateOf(0) }
    var webViewEpoch by remember(safeVideoId) { mutableIntStateOf(0) }
    var rendererUnavailable by remember(safeVideoId) { mutableStateOf(false) }

    DisposableEffect(safeVideoId, player) {
        player.onYoutubeToggle = {
            webViewRef?.evaluateJavascript(
                "if (window.ytPlayer && ytPlayer.getPlayerState) { if (ytPlayer.getPlayerState() === 1) ytPlayer.pauseVideo(); else ytPlayer.playVideo(); }",
                null,
            )
        }
        player.onYoutubePause = {
            webViewRef?.evaluateJavascript(
                "if (window.ytPlayer && ytPlayer.pauseVideo) ytPlayer.pauseVideo();",
                null,
            )
        }
        player.onYoutubeSeek = { sec ->
            webViewRef?.evaluateJavascript(
                "if (window.ytPlayer && ytPlayer.seekTo) ytPlayer.seekTo(${sec.coerceAtLeast(0f)}, true);",
                null,
            )
        }
        onDispose {
            player.onYoutubeToggle = null
            player.onYoutubePause = null
            player.onYoutubeSeek = null
            webViewRef = null
        }
    }

    if (!expanded || safeVideoId == null) return

    Surface(
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = YtCard,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, YtDivider),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "YouTube Video",
                    style = MaterialTheme.typography.labelMedium,
                    color = YtTextSecondary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onSwitchToAudio,
                        modifier = Modifier.testTag("yt_overlay_switch_audio_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = YtOrange,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Text("Audio 8D", color = YtOrange, style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(
                        onClick = {
                            runCatching {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.youtube.com/watch?v=$safeVideoId"),
                                )
                                context.startActivity(intent)
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Buka di YouTube",
                            tint = YtTextSecondary,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                if (rendererUnavailable) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF12151B))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Renderer WebView terhenti di perangkat ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = YtTextPrimary,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onSwitchToAudio,
                                colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(Icons.Default.Headphones, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Audio 8D")
                            }
                            OutlinedButton(
                                onClick = {
                                    useSoftwareLayer = true
                                    crashCount = 0
                                    rendererUnavailable = false
                                    webViewEpoch++
                                },
                                border = BorderStroke(1.dp, YtDivider),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = YtTextPrimary)
                                Spacer(Modifier.width(4.dp))
                                Text("Muat Ulang", color = YtTextPrimary)
                            }
                        }
                    }
                } else {
                    key(safeVideoId, webViewEpoch) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    if (useSoftwareLayer) {
                                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
                                    }
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    settings.setSupportMultipleWindows(false)
                                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        settings.safeBrowsingEnabled = false
                                    }
                                    webChromeClient = WebChromeClient()
                                    webViewClient = object : WebViewClient() {
                                        override fun onRenderProcessGone(
                                            view: WebView?,
                                            detail: RenderProcessGoneDetail?,
                                        ): Boolean {
                                            val didCrash = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                detail?.didCrash() == true
                                            } else {
                                                true
                                            }
                                            AppDiagnostics.warn(
                                                "WebView",
                                                "YouTubeEmbed renderer gone (didCrash=$didCrash, crashCount=$crashCount)",
                                            )
                                            if (webViewRef == view) {
                                                webViewRef = null
                                            }
                                            safelyDestroyWebView(view)
                                            useSoftwareLayer = true
                                            if (crashCount == 0) {
                                                crashCount = 1
                                                webViewEpoch++
                                            } else {
                                                rendererUnavailable = true
                                            }
                                            return true
                                        }
                                    }
                                    addJavascriptInterface(
                                        object {
                                            @JavascriptInterface
                                            fun onState(playing: Boolean, current: Float, duration: Float) {
                                                post {
                                                    player.updateYoutubePlaybackState(playing, current, duration)
                                                }
                                            }
                                        },
                                        "AndroidBridge",
                                    )
                                    tag = safeVideoId
                                    loadDataWithBaseURL(
                                        "https://www.youtube.com",
                                        buildYoutubeHtml(safeVideoId, initialSeekSec.roundToInt().coerceAtLeast(0)),
                                        "text/html",
                                        "utf-8",
                                        null,
                                    )
                                    webViewRef = this
                                }
                            },
                            update = { wv ->
                                if (wv.tag != safeVideoId) {
                                    wv.tag = safeVideoId
                                    wv.loadDataWithBaseURL(
                                        "https://www.youtube.com",
                                        buildYoutubeHtml(safeVideoId, initialSeekSec.roundToInt().coerceAtLeast(0)),
                                        "text/html",
                                        "utf-8",
                                        null,
                                    )
                                }
                            },
                            onRelease = { wv ->
                                if (webViewRef == wv) webViewRef = null
                                safelyDestroyWebView(wv)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(176.dp),
                        )
                    }
                }
            }
        }
    }
}

internal fun isVirtualRenderDevice(): Boolean {
    val fp = Build.FINGERPRINT.lowercase()
    val hw = Build.HARDWARE.lowercase()
    val model = Build.MODEL.lowercase()
    val product = Build.PRODUCT.lowercase()
    val brand = Build.BRAND.lowercase()
    return fp.startsWith("generic") ||
        fp.startsWith("unknown") ||
        fp.contains("emulator") ||
        hw.contains("goldfish") ||
        hw.contains("ranchu") ||
        model.contains("emulator") ||
        model.contains("android sdk") ||
        product.contains("sdk") ||
        product.contains("emulator") ||
        brand.startsWith("generic")
}

internal fun safelyDestroyWebView(wv: WebView?) {
    if (wv == null) return
    runCatching {
        wv.stopLoading()
        wv.loadUrl("about:blank")
        wv.clearHistory()
        (wv.parent as? ViewGroup)?.removeView(wv)
        wv.removeAllViews()
        wv.destroy()
    }
}

private fun sanitizeVideoId(raw: String): String? {
    val trimmed = raw.trim()
    return trimmed.takeIf { Regex("^[A-Za-z0-9_-]{6,20}$").matches(it) }
}

private fun buildYoutubeHtml(videoId: String, startSeconds: Int): String = """
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
      <style>html,body,#player{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden;}</style>
    </head>
    <body>
      <div id="player"></div>
      <script src="https://www.youtube.com/iframe_api"></script>
      <script>
        var ytPlayer = null;
        var ytTimer = null;
        function clearYtTimer() {
          if (ytTimer !== null) {
            clearInterval(ytTimer);
            ytTimer = null;
          }
        }
        window.addEventListener('beforeunload', clearYtTimer);
        window.addEventListener('unload', clearYtTimer);
        function onYouTubeIframeAPIReady() {
          ytPlayer = new YT.Player('player', {
            videoId: '$videoId',
            playerVars: { autoplay: 1, start: $startSeconds, controls: 1, modestbranding: 1, rel: 0, playsinline: 1 },
            events: {
              onReady: function(e) {
                clearYtTimer();
                ytTimer = setInterval(function() {
                  try {
                    if (ytPlayer && ytPlayer.getCurrentTime) {
                      var playing = ytPlayer.getPlayerState() === 1;
                      AndroidBridge.onState(playing, ytPlayer.getCurrentTime() || 0, ytPlayer.getDuration() || 0);
                    }
                  } catch (_) {}
                }, 500);
              },
              onStateChange: function(e) {
                try {
                  if (ytPlayer && ytPlayer.getCurrentTime) {
                    AndroidBridge.onState(e.data === 1, ytPlayer.getCurrentTime() || 0, ytPlayer.getDuration() || 0);
                  }
                } catch (_) {}
              },
              onError: function(_) {
                clearYtTimer();
              }
            }
          });
        }
      </script>
    </body>
    </html>
""".trimIndent()
