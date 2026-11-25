package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import com.example.player.YMusicPlayer
import com.example.ui.theme.YtCard
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtOrange
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

    DisposableEffect(safeVideoId, player) {
        player.onYoutubeToggle = {
            webViewRef?.evaluateJavascript(
                "if (window.ytPlayer) { if (ytPlayer.getPlayerState() === 1) ytPlayer.pauseVideo(); else ytPlayer.playVideo(); }",
                null,
            )
        }
        player.onYoutubePause = {
            webViewRef?.evaluateJavascript(
                "if (window.ytPlayer) ytPlayer.pauseVideo();",
                null,
            )
        }
        player.onYoutubeSeek = { sec ->
            webViewRef?.evaluateJavascript(
                "if (window.ytPlayer) ytPlayer.seekTo(${sec.coerceAtLeast(0f)}, true);",
                null,
            )
        }
        onDispose {
            player.onYoutubeToggle = null
            player.onYoutubePause = null
            player.onYoutubeSeek = null
            webViewRef?.destroy()
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
        border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
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
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: RenderProcessGoneDetail?,
                                ): Boolean {
                                    view?.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
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
                        wv.stopLoading()
                        wv.destroy()
                        if (webViewRef == wv) webViewRef = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp),
                )
            }
        }
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
        function onYouTubeIframeAPIReady() {
          ytPlayer = new YT.Player('player', {
            videoId: '$videoId',
            playerVars: { autoplay: 1, start: $startSeconds, controls: 1, modestbranding: 1, rel: 0, playsinline: 1 },
            events: {
              onReady: function(e) {
                setInterval(function() {
                  if (ytPlayer && ytPlayer.getCurrentTime) {
                    var playing = ytPlayer.getPlayerState() === 1;
                    AndroidBridge.onState(playing, ytPlayer.getCurrentTime() || 0, ytPlayer.getDuration() || 0);
                  }
                }, 500);
              },
              onStateChange: function(e) {
                if (ytPlayer && ytPlayer.getCurrentTime) {
                  AndroidBridge.onState(e.data === 1, ytPlayer.getCurrentTime() || 0, ytPlayer.getDuration() || 0);
                }
              }
            }
          });
        }
      </script>
    </body>
    </html>
""".trimIndent()
