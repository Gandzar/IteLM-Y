package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.AccountProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

class YouTubeAuthManager(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val encryptedPrefs: SharedPreferences by lazy { createEncryptedPrefs() }

    private val _profile = MutableStateFlow(loadStoredProfile())
    val profile: StateFlow<AccountProfile> = _profile.asStateFlow()

    private fun createEncryptedPrefs(): SharedPreferences {
        return try {
            createEncryptedPrefsInternal()
        } catch (_: Throwable) {
            // Recover gracefully if keystore alias was invalidated across backup/restore
            appContext.deleteSharedPreferences(PREFS_NAME)
            createEncryptedPrefsInternal()
        }
    }

    private fun createEncryptedPrefsInternal(): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            appContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun loadStoredProfile(): AccountProfile {
        val raw = runCatching { encryptedPrefs.getString(KEY_PROFILE, null) }.getOrNull()
        val parsed = raw?.let { runCatching { json.decodeFromString<AccountProfile>(it) }.getOrNull() }
        return if (parsed != null && hasValidSession()) parsed else AccountProfile()
    }

    fun hasValidSession(): Boolean {
        val cookies = getSessionCookies()
        val sapisid = getSapisid()
        return !cookies.isNullOrBlank() && !sapisid.isNullOrBlank()
    }

    fun getSessionCookies(): String? =
        runCatching { encryptedPrefs.getString(KEY_COOKIES, null) }.getOrNull()

    fun getSapisid(): String? =
        runCatching { encryptedPrefs.getString(KEY_SAPISID, null) }.getOrNull()

    fun saveSessionFromWebView(rawCookieString: String): Boolean {
        val parsedMap = parseCookieHeader(rawCookieString)
        val sapisid = parsedMap["SAPISID"]
            ?: parsedMap["__Secure-3PAPISID"]
            ?: parsedMap["__Secure-1PAPISID"]
            ?: return false

        val normalizedCookies = parsedMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
        encryptedPrefs.edit()
            .putString(KEY_COOKIES, normalizedCookies)
            .putString(KEY_SAPISID, sapisid)
            .apply()

        if (!_profile.value.isLoggedIn) {
            val fallbackProfile = AccountProfile(
                isLoggedIn = true,
                name = "Akun YouTube Music",
                handle = "Terhubung",
                lastSyncedAt = System.currentTimeMillis(),
            )
            saveProfile(fallbackProfile)
        }
        return true
    }

    fun saveProfile(accountProfile: AccountProfile) {
        val stamped = accountProfile.copy(
            isLoggedIn = true,
            lastSyncedAt = System.currentTimeMillis(),
        )
        encryptedPrefs.edit()
            .putString(KEY_PROFILE, json.encodeToString(stamped))
            .apply()
        _profile.value = stamped
    }

    fun buildAuthHeaders(origin: String = ORIGIN_YT_MUSIC): Map<String, String> {
        val cookies = getSessionCookies() ?: return emptyMap()
        val sapisid = getSapisid() ?: return emptyMap()
        if (cookies.isBlank() || sapisid.isBlank()) return emptyMap()

        val timestamp = System.currentTimeMillis() / 1000L
        val authHash = computeSapisidHash(timestamp, sapisid, origin)

        return mapOf(
            "Cookie" to cookies,
            "Authorization" to "SAPISIDHASH ${timestamp}_$authHash",
            "X-Origin" to origin,
            "Origin" to origin,
            "X-Goog-AuthUser" to "0",
        )
    }

    fun logout() {
        runCatching { encryptedPrefs.edit().clear().apply() }
        runCatching {
            val cm = CookieManager.getInstance()
            cm.removeAllCookies(null)
            cm.flush()
        }
        runCatching {
            WebStorage.getInstance().deleteAllData()
        }
        _profile.value = AccountProfile()
    }

    companion object {
        private const val PREFS_NAME = "ymusic_secure_auth"
        private const val KEY_COOKIES = "session_cookies"
        private const val KEY_SAPISID = "sapisid_token"
        private const val KEY_PROFILE = "account_profile"
        const val ORIGIN_YT_MUSIC = "https://music.youtube.com"
        const val ORIGIN_YOUTUBE = "https://www.youtube.com"

        fun collectCookiesFromCookieManager(): String = runCatching {
            val cm = CookieManager.getInstance()
            cm.flush()
            val merged = linkedMapOf<String, String>()
            val domains = listOf(
                "https://music.youtube.com",
                "https://www.youtube.com",
                "https://youtube.com",
                "https://accounts.google.com",
            )
            domains.forEach { domain ->
                val raw = cm.getCookie(domain).orEmpty()
                parseCookieHeader(raw).forEach { (k, v) -> merged[k] = v }
            }
            merged.entries.joinToString("; ") { "${it.key}=${it.value}" }
        }.getOrDefault("")

        fun hasAuthCookieInString(raw: String): Boolean {
            val map = parseCookieHeader(raw)
            return map.containsKey("SAPISID") ||
                map.containsKey("__Secure-3PAPISID") ||
                map.containsKey("__Secure-1PAPISID")
        }

        fun parseCookieHeader(raw: String): Map<String, String> {
            val out = linkedMapOf<String, String>()
            raw.split(';').forEach { part ->
                val idx = part.indexOf('=')
                if (idx > 0) {
                    val key = part.substring(0, idx).trim()
                    val value = part.substring(idx + 1).trim()
                    if (key.isNotEmpty() && value.isNotEmpty()) {
                        out[key] = value
                    }
                }
            }
            return out
        }

        private fun computeSapisidHash(timestampSec: Long, sapisid: String, origin: String): String {
            val payload = "$timestampSec $sapisid $origin"
            val digest = MessageDigest.getInstance("SHA-1").digest(payload.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
