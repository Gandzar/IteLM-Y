package com.example.debug

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

enum class DebugLogLevel(val label: String) {
    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERROR"),
}

data class DebugLogEntry(
    val id: Int,
    val timestampMs: Long,
    val level: DebugLogLevel,
    val tag: String,
    val message: String,
)

data class ActivityRecord(
    val name: String,
    val simpleName: String,
    val state: String,
    val intentAction: String,
    val routeExtra: String,
    val updatedAt: Long,
)

data class MemorySnapshot(
    val jvmUsedMb: Float = 0f,
    val jvmTotalMb: Float = 0f,
    val jvmMaxMb: Float = 1f,
    val nativeAllocatedMb: Float = 0f,
    val nativeTotalMb: Float = 0f,
    val totalPssMb: Float = 0f,
    val systemAvailMb: Float = 0f,
    val systemTotalMb: Float = 1f,
    val isLowMemory: Boolean = false,
    val historyMb: List<Float> = emptyList(),
    val sampledAt: Long = 0L,
)

data class DiagnosticsSummary(
    val appStartedAt: Long = System.currentTimeMillis(),
    val activeActivitiesCount: Int = 0,
    val activities: List<ActivityRecord> = emptyList(),
    val runningServices: List<String> = emptyList(),
    val webViewVersion: String = "-",
)

object AppDiagnostics {
    private const val MAX_LOGS = 300
    private const val MAX_MEMORY_POINTS = 60

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val nextId = AtomicInteger(1)
    private val activityMap = linkedMapOf<Int, ActivityRecord>()
    private val serviceSet = linkedSetOf<String>()
    private var samplerJob: Job? = null
    private var appContext: Context? = null
    private var initialized = false

    private val _logs = MutableStateFlow<List<DebugLogEntry>>(emptyList())
    val logs: StateFlow<List<DebugLogEntry>> = _logs.asStateFlow()

    private val _memory = MutableStateFlow(MemorySnapshot())
    val memory: StateFlow<MemorySnapshot> = _memory.asStateFlow()

    private val _summary = MutableStateFlow(DiagnosticsSummary())
    val summary: StateFlow<DiagnosticsSummary> = _summary.asStateFlow()

    fun init(application: Application) {
        if (initialized) return
        initialized = true
        appContext = application.applicationContext

        val webViewVer = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WebView.getCurrentWebViewPackage()?.versionName ?: "System"
            } else {
                "Legacy"
            }
        }.getOrDefault("Unavailable")

        _summary.value = _summary.value.copy(
            appStartedAt = System.currentTimeMillis(),
            webViewVersion = webViewVer,
        )

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                recordActivity(activity, "CREATED")
            }

            override fun onActivityStarted(activity: Activity) {
                recordActivity(activity, "STARTED")
            }

            override fun onActivityResumed(activity: Activity) {
                recordActivity(activity, "RESUMED")
            }

            override fun onActivityPaused(activity: Activity) {
                recordActivity(activity, "PAUSED")
            }

            override fun onActivityStopped(activity: Activity) {
                recordActivity(activity, "STOPPED")
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                recordActivity(activity, "DESTROYED")
            }
        })

        log(DebugLogLevel.INFO, "App", "Diagnostics initialized (WebView $webViewVer)")
        startMemorySampler()
    }

    private fun recordActivity(activity: Activity, state: String) {
        val key = System.identityHashCode(activity)
        val intentAction = activity.intent?.action.orEmpty().ifBlank { "NONE" }
        val routeExtra = activity.intent?.getStringExtra("ymusic_nav_route").orEmpty()
        val record = ActivityRecord(
            name = activity.javaClass.name,
            simpleName = activity.javaClass.simpleName,
            state = state,
            intentAction = intentAction,
            routeExtra = routeExtra,
            updatedAt = System.currentTimeMillis(),
        )
        synchronized(activityMap) {
            activityMap[key] = record
            if (activityMap.size > 24) {
                val oldestDestroyed = activityMap.entries.firstOrNull { it.value.state == "DESTROYED" }?.key
                if (oldestDestroyed != null) activityMap.remove(oldestDestroyed)
            }
            val list = activityMap.values.sortedByDescending { it.updatedAt }
            val activeCount = list.count { it.state != "DESTROYED" }
            _summary.value = _summary.value.copy(
                activeActivitiesCount = activeCount,
                activities = list,
            )
        }
        if (state == "CREATED" || state == "DESTROYED") {
            val routeNote = if (routeExtra.isNotBlank()) " [route=$routeExtra]" else ""
            log(DebugLogLevel.INFO, "Lifecycle", "${activity.javaClass.simpleName} -> $state ($intentAction)$routeNote")
        }
    }

    fun setServiceRunning(serviceName: String, running: Boolean) {
        synchronized(serviceSet) {
            if (running) serviceSet.add(serviceName) else serviceSet.remove(serviceName)
            _summary.value = _summary.value.copy(runningServices = serviceSet.toList())
        }
        log(DebugLogLevel.INFO, "Service", "$serviceName ${if (running) "STARTED" else "STOPPED"}")
    }

    fun log(level: DebugLogLevel, tag: String, message: String) {
        val entry = DebugLogEntry(
            id = nextId.getAndIncrement(),
            timestampMs = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = sanitizeSensitive(message),
        )
        synchronized(this) {
            val updated = (listOf(entry) + _logs.value).take(MAX_LOGS)
            _logs.value = updated
        }
    }

    fun info(tag: String, message: String) = log(DebugLogLevel.INFO, tag, message)
    fun warn(tag: String, message: String) = log(DebugLogLevel.WARN, tag, message)
    fun error(tag: String, message: String) = log(DebugLogLevel.ERROR, tag, message)

    fun clearLogs() {
        _logs.value = emptyList()
        info("Debug", "Logs cleared")
    }

    fun triggerGc() {
        System.gc()
        System.runFinalization()
        sampleMemoryOnce()
        info("Memory", "Manual GC requested")
    }

    fun sampleMemoryNow() {
        sampleMemoryOnce()
    }

    private fun startMemorySampler() {
        samplerJob?.cancel()
        samplerJob = scope.launch {
            while (isActive) {
                sampleMemoryOnce()
                delay(1200)
            }
        }
    }

    private fun sampleMemoryOnce() {
        val runtime = Runtime.getRuntime()
        val jvmTotal = runtime.totalMemory() / (1024f * 1024f)
        val jvmFree = runtime.freeMemory() / (1024f * 1024f)
        val jvmUsed = (jvmTotal - jvmFree).coerceAtLeast(0f)
        val jvmMax = (runtime.maxMemory() / (1024f * 1024f)).coerceAtLeast(1f)

        val nativeAlloc = Debug.getNativeHeapAllocatedSize() / (1024f * 1024f)
        val nativeTotal = Debug.getNativeHeapSize() / (1024f * 1024f)
        val pssMb = (Debug.getPss() / 1024f).coerceAtLeast(0f)

        var sysAvail = 0f
        var sysTotal = 1f
        var lowMem = false
        appContext?.let { ctx ->
            runCatching {
                val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val mi = ActivityManager.MemoryInfo()
                am.getMemoryInfo(mi)
                sysAvail = mi.availMem / (1024f * 1024f)
                sysTotal = (mi.totalMem / (1024f * 1024f)).coerceAtLeast(1f)
                lowMem = mi.lowMemory
            }
        }

        val currentHistory = _memory.value.historyMb
        val nextHistory = (currentHistory + (if (pssMb > 0f) pssMb else jvmUsed)).takeLast(MAX_MEMORY_POINTS)

        _memory.value = MemorySnapshot(
            jvmUsedMb = jvmUsed,
            jvmTotalMb = jvmTotal,
            jvmMaxMb = jvmMax,
            nativeAllocatedMb = nativeAlloc,
            nativeTotalMb = nativeTotal,
            totalPssMb = pssMb,
            systemAvailMb = sysAvail,
            systemTotalMb = sysTotal,
            isLowMemory = lowMem,
            historyMb = nextHistory,
            sampledAt = System.currentTimeMillis(),
        )
    }

    fun buildTextReport(): String {
        val m = _memory.value
        val s = _summary.value
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
        return buildString {
            appendLine("=== YMusic Lite Diagnostics Report ===")
            appendLine("Android API: ${Build.VERSION.SDK_INT} (${Build.MODEL})")
            appendLine("WebView: ${s.webViewVersion}")
            appendLine(
                String.format(
                    Locale.US,
                    "JVM Heap: %.1f / %.1f MB (Max %.1f MB)",
                    m.jvmUsedMb,
                    m.jvmTotalMb,
                    m.jvmMaxMb,
                ),
            )
            appendLine(
                String.format(
                    Locale.US,
                    "Native Heap: %.1f / %.1f MB | Total PSS: %.1f MB",
                    m.nativeAllocatedMb,
                    m.nativeTotalMb,
                    m.totalPssMb,
                ),
            )
            appendLine(
                String.format(
                    Locale.US,
                    "System RAM Avail: %.0f / %.0f MB (low=%s)",
                    m.systemAvailMb,
                    m.systemTotalMb,
                    m.isLowMemory,
                ),
            )
            appendLine("Running Services: ${s.runningServices.joinToString().ifBlank { "none" }}")
            appendLine("Activities (${s.activities.size}):")
            s.activities.take(10).forEach { act ->
                appendLine("  - ${act.simpleName}: ${act.state} (${act.intentAction}) ${fmt.format(Date(act.updatedAt))}")
            }
            appendLine("Recent Logs:")
            _logs.value.take(60).forEach { entry ->
                appendLine("  [${fmt.format(Date(entry.timestampMs))}] ${entry.level.label}/${entry.tag}: ${entry.message}")
            }
        }
    }

    private fun sanitizeSensitive(raw: String): String = raw
        .replace(Regex("(SAPISIDHASH\\s+)[^\\s,;]+", RegexOption.IGNORE_CASE), "$1[REDACTED]")
        .replace(Regex("(SAPISID=)[^;\\s]+", RegexOption.IGNORE_CASE), "$1[REDACTED]")
        .replace(Regex("(Cookie:\\s*)[^\\n]+", RegexOption.IGNORE_CASE), "$1[REDACTED]")
}
