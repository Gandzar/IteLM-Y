package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.auth.YouTubeAuthManager
import com.example.ui.theme.YtBg
import com.example.ui.theme.YtCard
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtOrange
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTextSecondary

private const val LOGIN_START_URL =
    "https://accounts.google.com/ServiceLogin?service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fmusic.youtube.com%2F"

private const val MOBILE_CHROME_UA =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

private fun isTrustedAuthHost(host: String?): Boolean {
    if (host.isNullOrBlank()) return false
    val h = host.lowercase()
    return h == "google.com" || h.endsWith(".google.com") ||
        h == "youtube.com" || h.endsWith(".youtube.com") ||
        h == "gstatic.com" || h.endsWith(".gstatic.com") ||
        h == "googleusercontent.com" || h.endsWith(".googleusercontent.com") ||
        h == "ggpht.com" || h.endsWith(".ggpht.com")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleLoginDialog(
    onDismiss: () -> Unit,
    onCookiesCaptured: (String) -> Unit,
    onError: (String) -> Unit,
) {
    var currentUrl by remember { mutableStateOf("accounts.google.com") }
    var isLoading by remember { mutableStateOf(true) }
    var completed by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(YtBg)
                .statusBarsPadding(),
            color = YtBg,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(YtCard)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("login_close_btn"),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup login", tint = YtTextPrimary)
                    }
                    Icon(Icons.Default.Lock, contentDescription = null, tint = YtOrange)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Login Google / YouTube",
                            style = MaterialTheme.typography.titleSmall,
                            color = YtTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            text = currentUrl,
                            style = MaterialTheme.typography.labelSmall,
                            color = YtTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Button(
                        onClick = {
                            val cookies = YouTubeAuthManager.collectCookiesFromCookieManager()
                            if (YouTubeAuthManager.hasAuthCookieInString(cookies)) {
                                if (!completed) {
                                    completed = true
                                    onCookiesCaptured(cookies)
                                }
                            } else {
                                onError("Silakan selesaikan login ke akun Google terlebih dahulu")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YtOrange,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier.testTag("login_sync_btn"),
                    ) {
                        Text("Selesai", fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(color = YtDivider)
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = YtOrange,
                        trackColor = YtDivider,
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                val cm = CookieManager.getInstance()
                                cm.setAcceptCookie(true)
                                cm.setAcceptThirdPartyCookies(this, true)

                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.userAgentString = MOBILE_CHROME_UA
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true

                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                    ): Boolean {
                                        val uri = request?.url ?: return true
                                        if (uri.scheme != "https" || !isTrustedAuthHost(uri.host)) {
                                            return true
                                        }
                                        return false
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isLoading = true
                                        url?.let {
                                            currentUrl = runCatching { Uri.parse(it).host }.getOrNull() ?: it
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        val host = url?.let { runCatching { Uri.parse(it).host }.getOrNull() }?.lowercase()
                                        if (!completed && (host == "music.youtube.com" || host == "www.youtube.com" || host == "m.youtube.com")) {
                                            val cookies = YouTubeAuthManager.collectCookiesFromCookieManager()
                                            if (YouTubeAuthManager.hasAuthCookieInString(cookies)) {
                                                completed = true
                                                onCookiesCaptured(cookies)
                                            }
                                        }
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?,
                                    ): Boolean {
                                        isLoading = false
                                        view?.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                        onError("Renderer WebView dimuat ulang")
                                        return true
                                    }
                                }
                                loadUrl(LOGIN_START_URL)
                                webViewRef = this
                            }
                        },
                        onRelease = { wv ->
                            wv.stopLoading()
                            wv.destroy()
                            if (webViewRef == wv) webViewRef = null
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
