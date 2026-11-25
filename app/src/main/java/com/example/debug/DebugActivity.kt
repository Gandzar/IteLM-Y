package com.example.debug

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.MainActivity
import com.example.ui.screens.DebugScreen
import com.example.ui.theme.YMusicLiteTheme
import com.example.ui.theme.YtBg
import com.example.ui.theme.YtCard
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtOrange
import com.example.ui.theme.YtTextPrimary

class DebugActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        super.onCreate(savedInstanceState)

        setContent {
            YMusicLiteTheme {
                Scaffold(
                    containerColor = YtBg,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        Column {
                            TopAppBar(
                                navigationIcon = {
                                    IconButton(
                                        onClick = { finish() },
                                        modifier = Modifier.testTag("debug_activity_back_btn"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Tutup Debug",
                                            tint = YtTextPrimary,
                                        )
                                    }
                                },
                                title = {
                                    Text(
                                        text = "YMusic Debug & RAM",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = YtTextPrimary,
                                    )
                                },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            startActivity(
                                                Intent(this@DebugActivity, MainActivity::class.java).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                },
                                            )
                                        },
                                        modifier = Modifier.testTag("debug_open_main_btn"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Home,
                                            contentDescription = "Buka Aplikasi Utama",
                                            tint = YtOrange,
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = YtCard,
                                    titleContentColor = YtTextPrimary,
                                ),
                            )
                            HorizontalDivider(color = YtDivider)
                        }
                    },
                ) { innerPadding ->
                    DebugScreen(
                        isStandaloneActivity = true,
                        onToast = { msg -> Toast.makeText(this@DebugActivity, msg, Toast.LENGTH_SHORT).show() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    )
                }
            }
        }
    }
}
