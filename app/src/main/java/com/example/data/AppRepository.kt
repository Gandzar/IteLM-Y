package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ymusic_lite_prefs")

class AppRepository(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val KEY_SETTINGS = stringPreferencesKey("settings_json")
    private val KEY_LIBRARY = stringPreferencesKey("library_json")
    private val KEY_HISTORY = stringPreferencesKey("history_json")
    private val KEY_DOWNLOADS = stringPreferencesKey("downloads_json")
    private val KEY_SCRAPR_META = stringPreferencesKey("scrapr_meta_json")
    private val KEY_ENGINE_META = stringPreferencesKey("engine_meta_json")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        prefs[KEY_SETTINGS]?.let { raw ->
            runCatching { json.decodeFromString<AppSettings>(raw) }.getOrNull()
        } ?: AppSettings()
    }

    val libraryFlow: Flow<List<Track>> = context.dataStore.data.map { prefs ->
        prefs[KEY_LIBRARY]?.let { raw ->
            runCatching { json.decodeFromString<List<Track>>(raw) }.getOrNull()
        } ?: emptyList()
    }

    val historyFlow: Flow<List<ScrapeResult>> = context.dataStore.data.map { prefs ->
        prefs[KEY_HISTORY]?.let { raw ->
            runCatching { json.decodeFromString<List<ScrapeResult>>(raw) }.getOrNull()
        } ?: emptyList()
    }

    val downloadsFlow: Flow<List<DownloadJob>> = context.dataStore.data.map { prefs ->
        prefs[KEY_DOWNLOADS]?.let { raw ->
            runCatching { json.decodeFromString<List<DownloadJob>>(raw) }.getOrNull()
        } ?: emptyList()
    }

    val scraprMetaFlow: Flow<ScraprMeta?> = context.dataStore.data.map { prefs ->
        prefs[KEY_SCRAPR_META]?.let { raw ->
            runCatching { json.decodeFromString<ScraprMeta>(raw) }.getOrNull()
        }
    }

    val engineMetaFlow: Flow<EngineMeta?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ENGINE_META]?.let { raw ->
            runCatching { json.decodeFromString<EngineMeta>(raw) }.getOrNull()
        }
    }

    suspend fun getSettings(): AppSettings = settingsFlow.first()

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SETTINGS]?.let {
                runCatching { json.decodeFromString<AppSettings>(it) }.getOrNull()
            } ?: AppSettings()
            prefs[KEY_SETTINGS] = json.encodeToString(transform(current))
        }
    }

    suspend fun saveLibrary(tracks: List<Track>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LIBRARY] = json.encodeToString(tracks)
        }
    }

    suspend fun saveHistory(items: List<ScrapeResult>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HISTORY] = json.encodeToString(items.take(50))
        }
    }

    suspend fun saveDownloads(jobs: List<DownloadJob>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DOWNLOADS] = json.encodeToString(jobs.take(50))
        }
    }

    suspend fun checkAppUpdate(): ReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(UPDATE_CHECK_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "YMusic-Lite")
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@withContext null
                val data = JSONObject(res.body?.string().orEmpty())
                val tag = data.optString("tag_name").replace(Regex("^v", RegexOption.IGNORE_CASE), "")
                val assets = data.optJSONArray("assets")
                var apkUrl: String? = null
                var apkName: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        val name = a.optString("name")
                        val ct = a.optString("content_type")
                        if (name.lowercase().endsWith(".apk") || ct == "application/vnd.android.package-archive") {
                            apkUrl = a.optString("browser_download_url")
                            apkName = name
                            break
                        }
                    }
                }
                ReleaseInfo(
                    tag = tag,
                    name = data.optString("name").ifBlank { tag },
                    body = data.optString("body"),
                    publishedAt = data.optString("published_at"),
                    htmlUrl = data.optString("html_url").ifBlank { REPO_URL },
                    apkUrl = apkUrl,
                    apkName = apkName,
                    isNewer = isNewerVersion(tag, BuildConfig.VERSION_NAME),
                )
            }
        }.getOrNull()
    }

    suspend fun checkScraprUpdate(currentVersion: String): ScraprUpdateInfo = withContext(Dispatchers.IO) {
        // 1) Try npm registry
        runCatching {
            val req = Request.Builder()
                .url(NPM_LATEST)
                .header("Accept", "application/json")
                .header("User-Agent", "YMusic-Lite")
                .build()
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val data = JSONObject(res.body?.string().orEmpty())
                    val latest = data.optString("version")
                    if (latest.isNotBlank()) {
                        return@withContext ScraprUpdateInfo(
                            current = currentVersion,
                            latest = latest,
                            isNewer = isNewerScraprVersion(latest, currentVersion),
                            description = data.optString("description").ifBlank { null },
                            tarball = data.optJSONObject("dist")?.optString("tarball")?.ifBlank { null },
                            homepage = data.optString("homepage").ifBlank { SCRAPR_REPO_URL },
                            publishedVia = "npm",
                        )
                    }
                }
            }
        }

        // 2) Fallback: raw GitHub package.json
        runCatching {
            val req = Request.Builder()
                .url(GITHUB_PACKAGE_JSON)
                .header("User-Agent", "YMusic-Lite")
                .build()
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val data = JSONObject(res.body?.string().orEmpty())
                    val latest = data.optString("version")
                    if (latest.isNotBlank()) {
                        return@withContext ScraprUpdateInfo(
                            current = currentVersion,
                            latest = latest,
                            isNewer = isNewerScraprVersion(latest, currentVersion),
                            description = data.optString("description").ifBlank { null },
                            homepage = SCRAPR_REPO_URL,
                            publishedVia = "github",
                        )
                    }
                }
            }
        }

        throw IllegalStateException("Tidak bisa cek update scrapr (offline / rate limit)")
    }

    /**
     * Fetches scrapr version metadata from npm/GitHub and saves version/manifest info locally.
     * Complies with Google Play System Integrity rules (no dynamic code loading/execution).
     */
    suspend fun updateScraprMetadata(): ScraprMeta = withContext(Dispatchers.IO) {
        val current = scraprMetaFlow.first()?.version ?: BUILTIN_VERSION
        val info = checkScraprUpdate(current)
        val meta = ScraprMeta(
            version = info.latest,
            updatedAt = System.currentTimeMillis(),
            source = info.publishedVia,
            description = info.description,
            tarball = info.tarball,
            homepage = info.homepage,
        )
        context.dataStore.edit { prefs ->
            prefs[KEY_SCRAPR_META] = json.encodeToString(meta)
        }
        updateSettings {
            it.copy(
                engineVersion = meta.version,
                lastEngineCheck = System.currentTimeMillis(),
                useCustomEngine = true,
                customEngineUrl = info.tarball ?: SCRAPR_REPO_URL,
            )
        }
        meta
    }

    suspend fun revertScraprToBuiltin() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SCRAPR_META)
        }
        updateSettings {
            it.copy(
                useCustomEngine = false,
                engineVersion = BUILTIN_VERSION,
                customEngineUrl = "",
            )
        }
    }

    /**
     * Fetches metadata/manifest text from a user-provided HTTPS URL without executing remote code.
     */
    suspend fun fetchCustomEngineConfig(url: String): EngineMeta = withContext(Dispatchers.IO) {
        val cleaned = url.trim()
        require(cleaned.startsWith("https://")) { "URL tidak valid (harus https://)" }

        val req = Request.Builder()
            .url(cleaned)
            .header("User-Agent", "YMusic-Lite")
            .build()
        val text = client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw IllegalStateException("Gagal unduh: HTTP ${res.code}")
            res.body?.string().orEmpty()
        }
        require(text.length >= 20) { "Respons terlalu kecil, periksa URL konfigurasi" }

        val verRegex1 = Regex("ENGINE_VERSION\\s*=\\s*['\"`]([^'\"`]+)['\"`]")
        val verRegex2 = Regex("version\\s*[:=]\\s*['\"`]([^'\"`]+)['\"`]", RegexOption.IGNORE_CASE)
        val version = verRegex1.find(text)?.groupValues?.getOrNull(1)
            ?: verRegex2.find(text)?.groupValues?.getOrNull(1)
            ?: "custom"

        val meta = EngineMeta(
            version = version,
            sourceUrl = cleaned,
            downloadedAt = System.currentTimeMillis(),
            size = text.length,
        )
        context.dataStore.edit { prefs ->
            prefs[KEY_ENGINE_META] = json.encodeToString(meta)
        }
        updateSettings {
            it.copy(
                useCustomEngine = true,
                customEngineUrl = cleaned,
                engineVersion = version,
                lastEngineCheck = System.currentTimeMillis(),
            )
        }
        meta
    }

    suspend fun revertCustomEngine() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ENGINE_META)
        }
        updateSettings {
            it.copy(
                useCustomEngine = false,
                engineVersion = BUILTIN_VERSION,
            )
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val l = parseVersion(latest)
        val c = parseVersion(current)
        for (i in 0 until maxOf(l.size, c.size)) {
            val a = l.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a > b) return true
            if (a < b) return false
        }
        return false
    }

    private fun isNewerScraprVersion(latest: String, current: String): Boolean {
        if (current == BUILTIN_VERSION || current.contains("lite")) return true
        return isNewerVersion(latest, current)
    }

    private fun parseVersion(v: String): List<Int> =
        v.replace(Regex("^v", RegexOption.IGNORE_CASE), "")
            .split(Regex("[.-]"))
            .map { it.toIntOrNull() ?: 0 }

    companion object {
        const val BUILTIN_VERSION = "0.2.0-lite"
        const val GITHUB_REPO = "ymusic-lite/ymusic-lite"
        const val UPDATE_CHECK_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
        const val REPO_URL = "https://github.com/$GITHUB_REPO"

        const val NPM_LATEST = "https://registry.npmjs.org/@coflyn/scrapr/latest"
        const val GITHUB_PACKAGE_JSON = "https://raw.githubusercontent.com/coflyn/scrapr/main/package.json"
        const val SCRAPR_REPO_URL = "https://github.com/coflyn/scrapr"
        const val SCRAPR_NPM_URL = "https://www.npmjs.com/package/@coflyn/scrapr"
    }
}
