/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Copyright (C) 2026 Abhishek Gour (Tinexus)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.tinexus.hosttriangle

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.HardwarePropertiesManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import com.tinexus.hosttriangle.cluster.ClusterFleetManager

/**
 * HostTriangleServerService
 *
 * Polyglot Multi-Runtime Binary Engine & Persistent Foreground Lifecycle Manager.
 * Dynamically resolves, stages, and executes Spring Boot (.jar), Node.js (index.js),
 * and Python (main.py/app.py) targets via Kotlin ProcessBuilder.
 * Pipes stdout/stderr output asynchronously into thread-safe SharedFlow logs,
 * triggers mock 'Download Runtime' states on missing binaries, and enforces hardware
 * safety (WakeLock/WifiLock retention, 43°C thermal throttle, <15% battery alert).
 */
class HostTriangleServerService : Service() {

    enum class ServerState {
        STOPPED,
        STARTING,
        RUNNING,
        THROTTLED,
        ERROR,
        MISSING_RUNTIME
    }

    enum class LogLevel {
        INFO,
        DEBUG,
        WARN,
        ERROR,
        CRITICAL
    }

    enum class TargetRuntime(
        val identifier: String,
        val displayName: String,
        val relativeBinPath: String,
        val requiredArtifact: String
    ) {
        SPRING_BOOT(
            identifier = "java",
            displayName = "Spring Boot OpenJDK 17",
            relativeBinPath = "runtimes/java/bin/java",
            requiredArtifact = "openjdk-17-aarch64.tar.gz"
        ),
        NODE_JS(
            identifier = "node",
            displayName = "Node.js v20 LTS",
            relativeBinPath = "runtimes/node/bin/node",
            requiredArtifact = "node-v20-linux-arm64.tar.gz"
        ),
        PYTHON(
            identifier = "python",
            displayName = "Python 3.11 Runtime",
            relativeBinPath = "runtimes/python/bin/python",
            requiredArtifact = "python-3.11-linux-arm64.tar.gz"
        ),
        UNKNOWN(
            identifier = "unknown",
            displayName = "Unrecognized Runtime",
            relativeBinPath = "",
            requiredArtifact = ""
        )
    }

    sealed class RuntimeEvent {
        object Idle : RuntimeEvent()
        data class MissingRuntime(
            val runtime: TargetRuntime,
            val expectedBinaryPath: String,
            val requiredArtifact: String
        ) : RuntimeEvent()
        data class DownloadRuntimeRequested(
            val runtime: TargetRuntime,
            val destinationPath: String,
            val progressPercent: Float
        ) : RuntimeEvent()
        data class RuntimeReady(
            val runtime: TargetRuntime,
            val executablePath: String
        ) : RuntimeEvent()
    }

    data class LogEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val formattedTime: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp)),
        val level: LogLevel,
        val tag: String,
        val message: String
    ) {
        fun toConsoleString(): String = "[$formattedTime] [${level.name}] [$tag] $message"
    }

    data class DeviceTelemetry(
        val ramUsagePercent: Float = 0f,
        val ramUsedMb: Long = 0L,
        val ramTotalMb: Long = 0L,
        val cpuTemperature: Float = 36.5f,
        val batteryCapacity: Float = 100f,
        val isCharging: Boolean = false,
        val isThermalBreached: Boolean = false,
        val isLowBattery: Boolean = false,
        val uptimeSeconds: Long = 0L
    )

    data class TargetDescriptor(
        val runtime: TargetRuntime,
        val entryFile: File,
        val workspaceDir: File,
        val binaryExecutable: File,
        val commandArgs: List<String>,
        val environmentVariables: Map<String, String>
    )

    companion object {
        const val TAG = "HostTriangleCore"

        // Intent Actions
        const val ACTION_START_SERVER = "com.tinexus.hosttriangle.action.START_SERVER"
        const val ACTION_STOP_SERVER = "com.tinexus.hosttriangle.action.STOP_SERVER"
        const val ACTION_RESTART_SERVER = "com.tinexus.hosttriangle.action.RESTART_SERVER"
        const val ACTION_TRIGGER_DOWNLOAD_RUNTIME = "com.tinexus.hosttriangle.action.TRIGGER_DOWNLOAD_RUNTIME"

        // Intent Extras
        const val EXTRA_PORT = "com.tinexus.hosttriangle.extra.PORT"
        const val EXTRA_JAR_PATH = "com.tinexus.hosttriangle.extra.JAR_PATH"
        const val EXTRA_WORKSPACE_PATH = "com.tinexus.hosttriangle.extra.WORKSPACE_PATH"
        const val EXTRA_RUNTIME_TYPE = "com.tinexus.hosttriangle.extra.RUNTIME_TYPE"

        // Shared Preferences Persistence Keys
        const val PREFS_NAME = "hosttriangle_engine_prefs"
        const val KEY_SERVER_AUTO_START = "KEY_SERVER_AUTO_START"
        const val KEY_SERVER_RUNNING_STATE = "KEY_SERVER_RUNNING_STATE"
        const val KEY_CONFIG_PORT = "KEY_CONFIG_PORT"
        const val KEY_CUSTOM_SUBDOMAIN = "KEY_CUSTOM_SUBDOMAIN"
        const val KEY_TUNNEL_TOKEN = "KEY_TUNNEL_TOKEN"
        const val KEY_TUNNEL_PROTOCOL = "KEY_TUNNEL_PROTOCOL"
        const val KEY_CUSTOM_START_COMMAND = "KEY_CUSTOM_START_COMMAND"

        // Engine Configuration Defaults
        const val DEFAULT_PORT = 8080
        const val DEFAULT_SUBDOMAIN = ""
        const val DEFAULT_TUNNEL_PROTOCOL = "Cloudflare Tunnel"
        const val DEFAULT_TUNNEL_TOKEN = ""
        const val DEFAULT_TUNNEL_ID = ""
        const val THERMAL_SHUTDOWN_THRESHOLD = 43.0f // Celsius
        const val BATTERY_ALERT_THRESHOLD = 15.0f // Percentage
        const val TELEMETRY_INTERVAL_MS = 5000L

        // Notification Channel Identifiers
        private const val NOTIFICATION_CHANNEL_ID = "hosttriangle_core_engine"
        private const val NOTIFICATION_CHANNEL_NAME = "HostTriangle Core Server Runtime"
        private const val NOTIFICATION_ID = 2048

        // Globally accessible Reactive Flows for UI state synchronization
        private val _serverState = MutableStateFlow(ServerState.STOPPED)
        val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

        private val _activeRuntime = MutableStateFlow(TargetRuntime.UNKNOWN)
        val activeRuntime: StateFlow<TargetRuntime> = _activeRuntime.asStateFlow()

        private val _runtimeEvent = MutableStateFlow<RuntimeEvent>(RuntimeEvent.Idle)
        val runtimeEvent: StateFlow<RuntimeEvent> = _runtimeEvent.asStateFlow()

        private val _telemetry = MutableStateFlow(DeviceTelemetry())
        val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

        // Structured Log Stream
        private val _logFlow = MutableSharedFlow<LogEntry>(
            replay = 100,
            extraBufferCapacity = 500,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val logFlow: SharedFlow<LogEntry> = _logFlow.asSharedFlow()

        // Single Thread-Safe String Stream as required for terminal consumers
        private val _rawLogFlow = MutableSharedFlow<String>(
            replay = 100,
            extraBufferCapacity = 500,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val rawLogFlow: SharedFlow<String> = _rawLogFlow.asSharedFlow()

        private val _activePort = MutableStateFlow(DEFAULT_PORT)
        val activePort: StateFlow<Int> = _activePort.asStateFlow()

        private val _binaryPath = MutableStateFlow("")
        val binaryPath: StateFlow<String> = _binaryPath.asStateFlow()

        private val _publicUrl = MutableStateFlow("http://127.0.0.1:8080")
        val publicUrl: StateFlow<String> = _publicUrl.asStateFlow()

        /**
         * Global thread-safe dispatcher to pipe log messages into both Structured and Raw String flows.
         */
        fun log(level: LogLevel, tag: String, message: String) {
            val entry = LogEntry(level = level, tag = tag, message = message)
            _logFlow.tryEmit(entry)
            _rawLogFlow.tryEmit(entry.toConsoleString())
        }

        private var globalStartTimestamp: Long = 0L

        fun setServerStartTimestamp(ts: Long) {
            globalStartTimestamp = ts
        }

        fun getServerStartTimestamp(): Long = globalStartTimestamp

        fun getActiveJavaExecutable(context: Context): File? {
            val candidates = listOf(
                File("/data/local/tmp/jvm21/bin/java"),
                File(context.filesDir, "usr/lib/jvm/java-21-openjdk/bin/java"),
                File(context.filesDir, "runtimes/java/bin/java"),
                File("/data/local/tmp/jvm/bin/java")
            )
            return candidates.firstOrNull { it.exists() && it.canExecute() }
        }

        fun getJvmLibraryPaths(context: Context): List<String> {
            val candidates = listOf(
                "/data/local/tmp/jvm21/lib",
                "/data/local/tmp/jvm21/lib/server",
                "/data/local/tmp/test_usr/lib",
                "${context.filesDir.absolutePath}/usr/lib/jvm/java-21-openjdk/lib",
                "${context.filesDir.absolutePath}/usr/lib/jvm/java-21-openjdk/lib/server",
                "${context.filesDir.absolutePath}/usr/lib",
                "${context.filesDir.absolutePath}/runtimes/java/lib",
                "${context.filesDir.absolutePath}/runtimes/java/lib/server",
                "/data/local/tmp/jvm/lib",
                "/data/local/tmp/jvm/lib/server"
            )
            return candidates.filter { File(it).exists() }
        }

        fun computeDefaultStartCommand(workspaceDir: File, port: Int = DEFAULT_PORT): String {
            val allFiles = workspaceDir.listFiles() ?: emptyArray()
            val jarFile = allFiles.firstOrNull { it.name.endsWith(".jar", ignoreCase = true) }
            if (jarFile != null) {
                return "java -jar ${jarFile.name} --server.port=$port"
            }
            val jsFile = allFiles.firstOrNull { it.name.equals("index.js", true) || it.name.equals("app.js", true) || it.name.endsWith(".js", true) }
            if (jsFile != null) {
                return "node ${jsFile.name}"
            }
            val pyFile = allFiles.firstOrNull { it.name.equals("main.py", true) || it.name.equals("app.py", true) || it.name.endsWith(".py", true) }
            if (pyFile != null) {
                return "python ${pyFile.name}"
            }
            val shFile = allFiles.firstOrNull { it.name.endsWith(".sh", true) }
            if (shFile != null) {
                return "sh ${shFile.name}"
            }
            return "java -jar app.jar --server.port=$port"
        }

        /**
         * Real-time hardware telemetry sampling engine.
         * Universally extracts RAM, Battery and Thermal metrics across all Android devices,
         * regardless of whether the server is RUNNING or IDLE.
         */
        fun sampleTelemetry(context: Context, serverStartTimestamp: Long = globalStartTimestamp): DeviceTelemetry {
            try {
                // 1. RAM Allocation
                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                var ramUsagePercent = 0f
                var usedMb = 0L
                var totalMb = 0L
                if (actManager != null) {
                    val memInfo = ActivityManager.MemoryInfo()
                    actManager.getMemoryInfo(memInfo)
                    totalMb = memInfo.totalMem / (1024 * 1024)
                    val availMb = memInfo.availMem / (1024 * 1024)
                    usedMb = (totalMb - availMb).coerceAtLeast(0L)
                    if (totalMb > 0) {
                        ramUsagePercent = ((usedMb.toDouble() / totalMb.toDouble()) * 100.0).toFloat()
                    }
                }

                // 2. Battery State
                val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                val batteryPct = if (level >= 0 && scale > 0) {
                    (level.toFloat() / scale.toFloat()) * 100f
                } else {
                    100f
                }

                // 3. CPU Core / Board Thermal Reading
                var cpuTemp = readHardwareCpuTemperature(context)
                if (cpuTemp <= 0f) {
                    // BatteryManager.EXTRA_TEMPERATURE gives battery/board temp in tenths of degree C (e.g. 365 = 36.5 C)
                    val rawTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
                    if (rawTemp > 0) {
                        cpuTemp = rawTemp / 10.0f
                    } else {
                        cpuTemp = 36.8f
                    }
                }

                val uptimeSec = if (serverStartTimestamp > 0) (System.currentTimeMillis() - serverStartTimestamp) / 1000 else 0L
                val isLowBattery = batteryPct < BATTERY_ALERT_THRESHOLD && !isCharging
                val isThermalBreached = cpuTemp >= THERMAL_SHUTDOWN_THRESHOLD

                val updated = DeviceTelemetry(
                    ramUsagePercent = ramUsagePercent,
                    ramUsedMb = usedMb,
                    ramTotalMb = totalMb,
                    cpuTemperature = cpuTemp,
                    batteryCapacity = batteryPct,
                    isCharging = isCharging,
                    isThermalBreached = isThermalBreached,
                    isLowBattery = isLowBattery,
                    uptimeSeconds = uptimeSec
                )
                _telemetry.value = updated
                return updated
            } catch (e: Exception) {
                return _telemetry.value
            }
        }

        private fun readHardwareCpuTemperature(context: Context): Float {
            // Check Linux sysfs thermal zone files
            val paths = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/devices/virtual/thermal/thermal_zone0/temp"
            )
            for (path in paths) {
                try {
                    val file = File(path)
                    if (file.exists() && file.canRead()) {
                        val raw = file.readText().trim().toFloatOrNull()
                        if (raw != null && raw > 0) {
                            val deg = if (raw > 1000f) raw / 1000f else raw
                            if (deg in 20f..95f) return deg
                        }
                    }
                } catch (_: Exception) {}
            }

            // HardwarePropertiesManager fallback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val hwManager = context.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE) as? HardwarePropertiesManager
                    if (hwManager != null) {
                        val temps = hwManager.getDeviceTemperatures(
                            HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU,
                            HardwarePropertiesManager.TEMPERATURE_CURRENT
                        )
                        if (temps.isNotEmpty() && temps[0] > 0f) {
                            return temps[0]
                        }
                    }
                } catch (_: Exception) {}
            }
            return -1f
        }

        /**
         * Executes an arbitrary Linux shell command asynchronously from the workspace sandbox.
         * Pipes stdout/stderr output into the Live Logs terminal stream.
         */
        fun executeShellCommand(context: Context, commandStr: String, onFinished: (() -> Unit)? = null) {
            val rawCmd = commandStr.trim()
            if (rawCmd.isEmpty()) return

            var cmd = rawCmd
            // Strip sudo since user is already root in PRoot (-0)
            if (cmd.startsWith("sudo ")) {
                cmd = cmd.removePrefix("sudo ").trim()
            }
            // Alias 'pkg' to 'apt-get' for users coming from Termux
            if (cmd.startsWith("pkg ")) {
                cmd = cmd.replaceFirst("pkg ", "apt-get ")
            }
            // Auto-append -y to apt / apt-get install commands to prevent hanging on non-interactive prompts
            if ((cmd.startsWith("apt install") || cmd.startsWith("apt-get install")) && !cmd.contains("-y")) {
                cmd = cmd.replaceFirst("install", "install -y")
            }

            CoroutineScope(Dispatchers.IO).launch {
                log(LogLevel.INFO, "SHELL", "$ $cmd")

                // Built-in native HTTP curl engine for simple checks if proot not ready
                if (cmd.startsWith("curl", ignoreCase = true) && !EngineManager.getEngine(context).isInstalled()) {
                    executeBuiltinCurl(cmd)
                    withContext(Dispatchers.Main) {
                        onFinished?.invoke()
                    }
                    return@launch
                }

                try {
                    val engine = EngineManager.getEngine(context)
                    if (!engine.isInstalled()) {
                        log(LogLevel.INFO, "SHELL", "First run: Provisioning Debian ARM64 Linux userland...")
                        engine.install { pct ->
                            if (pct % 20 == 0 || pct == 100) {
                                log(LogLevel.INFO, "SHELL", "Linux Engine setup: $pct%...")
                            }
                        }
                        log(LogLevel.INFO, "SHELL", "Linux Engine ready. Executing: $cmd")
                    }

                    val process = engine.exec(cmd)
                    process.inputStream.bufferedReader().forEachLine { line ->
                        log(LogLevel.INFO, "SHELL", line)
                    }
                    val exitCode = process.waitFor()
                    log(LogLevel.DEBUG, "SHELL", "[Command exit: $exitCode]")
                } catch (e: Exception) {
                    log(LogLevel.ERROR, "SHELL", "Command execution failed: ${e.message}")
                } finally {
                    withContext(Dispatchers.Main) {
                        onFinished?.invoke()
                    }
                }
            }
        }

        private fun executeBuiltinCurl(command: String) {
            try {
                val parts = command.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                val urlCandidate = parts.firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
                    ?: parts.getOrNull(1)?.takeIf { !it.startsWith("-") }
                    ?: "http://127.0.0.1:8080"

                val finalUrl = if (!urlCandidate.startsWith("http://") && !urlCandidate.startsWith("https://")) {
                    "http://$urlCandidate"
                } else {
                    urlCandidate
                }

                log(LogLevel.INFO, "CURL", "* Native HTTP: Connecting to $finalUrl...")
                val url = URL(finalUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 6000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "HostTriangle-Curl/1.0")
                }

                val responseCode = conn.responseCode
                val responseMsg = conn.responseMessage ?: ""
                log(LogLevel.INFO, "CURL", "< HTTP/1.1 $responseCode $responseMsg")

                conn.headerFields.forEach { (key, values) ->
                    if (key != null) {
                        log(LogLevel.DEBUG, "CURL", "< $key: ${values.joinToString(", ")}")
                    }
                }

                val stream = if (responseCode in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
                val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
                if (body.isNotBlank()) {
                    body.lines().take(50).forEach { line ->
                        log(LogLevel.DEBUG, "CURL_OUT", line)
                    }
                } else {
                    log(LogLevel.DEBUG, "CURL_OUT", "[Empty response body]")
                }

                log(LogLevel.INFO, "SHELL", "[Command finished (Exit Code: 0)]")
                conn.disconnect()
            } catch (e: Exception) {
                log(LogLevel.ERROR, "CURL", "Connection failed: ${e.message}")
                log(LogLevel.WARN, "SHELL", "[Command exited with status code: 1]")
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var processExecutionJob: Job? = null
    private var telemetryJob: Job? = null

    private var currentNativeProcess: Process? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var serverStartTimestamp: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        log(LogLevel.INFO, TAG, "HostTriangle Polyglot Multi-Runtime Subsystem initialized.")
        createNotificationChannel()
        RuntimeManager.refreshStatuses(this)
        CloudflareTunnelManager.refreshStatus(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_SERVER
        val requestedPort = intent?.getIntExtra(EXTRA_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
        val requestedWorkspace = intent?.getStringExtra(EXTRA_WORKSPACE_PATH)

        when (action) {
            ACTION_START_SERVER -> {
                if (_serverState.value == ServerState.RUNNING) {
                    log(LogLevel.WARN, TAG, "Start requested, but polyglot engine is already executing.")
                    return START_STICKY
                }
                startServerEngine(requestedPort, requestedWorkspace)
            }
            ACTION_STOP_SERVER -> {
                log(LogLevel.INFO, TAG, "Stop command received via intent action.")
                stopServerEngine(reason = "User Requested Shutdown")
            }
            ACTION_RESTART_SERVER -> {
                log(LogLevel.INFO, TAG, "Restart sequence initiated.")
                stopServerEngine(reason = "Service Restart", preserveSticky = true)
                startServerEngine(requestedPort, requestedWorkspace)
            }
            ACTION_TRIGGER_DOWNLOAD_RUNTIME -> {
                val runtimeTypeStr = intent?.getStringExtra(EXTRA_RUNTIME_TYPE)
                val targetRuntime = TargetRuntime.values().find { it.identifier == runtimeTypeStr } ?: TargetRuntime.SPRING_BOOT
                val spec = RuntimeManager.supportedRuntimes.find { it.runtime == targetRuntime }
                if (spec != null) {
                    RuntimeManager.installRuntime(this, spec)
                }
            }
            else -> {
                log(LogLevel.WARN, TAG, "Unrecognized intent action: $action")
            }
        }

        return START_STICKY
    }

    /**
     * Initializes lock acquisition, notification elevation, dynamic command routing,
     * ProcessBuilder execution, and the 5000ms hardware telemetry loop.
     */
    private fun startServerEngine(port: Int, customWorkspacePath: String?) {
        _serverState.value = ServerState.STARTING
        _activePort.value = port

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val selectedProtocol = prefs.getString(KEY_TUNNEL_PROTOCOL, DEFAULT_TUNNEL_PROTOCOL) ?: DEFAULT_TUNNEL_PROTOCOL
        val configuredSubdomain = prefs.getString(KEY_CUSTOM_SUBDOMAIN, DEFAULT_SUBDOMAIN) ?: DEFAULT_SUBDOMAIN
        val configuredToken = prefs.getString(KEY_TUNNEL_TOKEN, "")?.trim() ?: ""

        if (selectedProtocol.contains("Cloudflare", ignoreCase = true) && configuredToken.isNotBlank()) {
            val decoded = ClusterFleetManager.decodeTunnelToken(configuredToken)
            val tunnelId = decoded?.tunnelId ?: ""
            _publicUrl.value = if (configuredSubdomain.isNotBlank()) "https://$configuredSubdomain" else "Cloudflare Tunnel Active"
            log(LogLevel.INFO, "CloudflareTunnel", "Tunnel ID: ${tunnelId.take(12)}... initialized.")
            log(LogLevel.INFO, "CloudflareTunnel", "Token verified [${configuredToken.take(12)}...].")
            log(LogLevel.INFO, "CloudflareTunnel", "Ingress Route Mapped -> http://127.0.0.1:$port")

            // Auto-launch Built-in Cloudflare Tunnel Subprocess
            CloudflareTunnelManager.startTunnel(this, configuredToken, port)
        } else {
            _publicUrl.value = "http://127.0.0.1:$port"
        }

        serverStartTimestamp = System.currentTimeMillis()
        setServerStartTimestamp(serverStartTimestamp)

        persistServerRunningState(true)
        acquireHardwareLocks()

        val initialNotification = buildNotification("HostTriangle Active: ${_publicUrl.value}")
        startInForeground(initialNotification)

        // Launch Dynamic Command Router & Process Execution Layer
        launchPolyglotExecutionPipeline(port, customWorkspacePath)

        // Launch Telemetry Monitoring Loop (5000ms)
        launchTelemetryLoop()
    }

    /**
     * POLYGLOT DYNAMIC COMMAND ROUTER
     * Inspects segregated sandbox pathways (`/data/user/0/.../files/workspace/`)
     * Identifies Spring Boot (.jar), Node.js (index.js), or Python (main.py/app.py) targets,
     * verifies native binary availability, triggers 'Download Runtime' events if missing,
     * and spawns the native ProcessBuilder pipeline with asynchronous stdout/stderr piping.
     */
    private fun launchPolyglotExecutionPipeline(port: Int, customWorkspacePath: String?) {
        processExecutionJob?.cancel()
        processExecutionJob = serviceScope.launch(Dispatchers.IO) {
            val workspaceDir = resolveWorkspaceDirectory(customWorkspacePath)
            log(LogLevel.INFO, "CommandRouter", "Auditing workspace sandbox: ${workspaceDir.absolutePath}")

            // Dynamic Target Inspection
            val targetDescriptor = detectExecutionTarget(workspaceDir, port)
            _activeRuntime.value = targetDescriptor.runtime
            _binaryPath.value = targetDescriptor.entryFile.absolutePath

            log(LogLevel.INFO, "CommandRouter", "Target Resolved: [${targetDescriptor.runtime.displayName}] -> Entry: ${targetDescriptor.entryFile.name}")

            // Runtime Binary Verification
            val binaryExecutable = targetDescriptor.binaryExecutable
            if (binaryExecutable.exists() && binaryExecutable.canExecute()) {
                _runtimeEvent.value = RuntimeEvent.RuntimeReady(targetDescriptor.runtime, binaryExecutable.absolutePath)
            } else {
                log(
                    LogLevel.INFO,
                    "RuntimeManager",
                    "Native binary path for ${targetDescriptor.runtime.displayName} not staged locally. Command will be resolved via Termux / system PATH."
                )
            }

            // Spawn ProcessBuilder Native Engine
            executeNativeProcess(targetDescriptor)
        }
    }

    /**
     * Inspects sandbox workspace directory and routes execution to discrete targets:
     * 1. SPRING BOOT: Any '*.jar' -> java -jar <file>.jar --server.port=8080
     * 2. NODE.JS: 'index.js' or node project -> node index.js
     * 3. PYTHON: 'main.py' or 'app.py' -> python main.py
     */
    private fun detectExecutionTarget(workspaceDir: File, port: Int): TargetDescriptor {
        if (!workspaceDir.exists()) {
            workspaceDir.mkdirs()
        }

        val allFiles = workspaceDir.listFiles() ?: emptyArray()

        // 1. SPRING BOOT INTERCEPTOR (.jar)
        val jarTarget = allFiles.firstOrNull { it.name.endsWith(".jar", ignoreCase = true) }
        if (jarTarget != null) {
            val javaBin = getActiveJavaExecutable(this) ?: File("/system/bin/sh")
            val javaHome = javaBin.parentFile?.parentFile?.absolutePath ?: ""

            val args = listOf("java", "-jar", jarTarget.name, "--server.port=$port")
            val env = mapOf(
                "JAVA_HOME" to javaHome,
                "SERVER_PORT" to port.toString(),
                "PORT" to port.toString(),
                "TMPDIR" to cacheDir.absolutePath,
                "JAVA_TOOL_OPTIONS" to "-Djava.io.tmpdir=${cacheDir.absolutePath}"
            )
            return TargetDescriptor(TargetRuntime.SPRING_BOOT, jarTarget, workspaceDir, javaBin, args, env)
        }

        // 2. NODE.JS RUNTIME AGENT (index.js / app.js / package.json)
        val nodeTarget = allFiles.firstOrNull { it.name.equals("index.js", true) || it.name.equals("app.js", true) }
            ?: File(workspaceDir, "index.js")
        if (nodeTarget.exists() || allFiles.any { it.name == "package.json" }) {
            val nodeBin = File(filesDir, TargetRuntime.NODE_JS.relativeBinPath)
            val args = listOf(
                nodeBin.absolutePath,
                nodeTarget.name
            )
            val env = mapOf(
                "PORT" to port.toString(),
                "NODE_ENV" to "production"
            )
            return TargetDescriptor(TargetRuntime.NODE_JS, nodeTarget, workspaceDir, nodeBin, args, env)
        }

        // 3. PYTHON STANDALONE EXECUTOR (main.py / app.py)
        val pythonTarget = allFiles.firstOrNull { it.name.equals("main.py", true) || it.name.equals("app.py", true) }
            ?: File(workspaceDir, "main.py")
        if (pythonTarget.exists() || allFiles.any { it.name.endsWith(".py", true) }) {
            val pythonBin = File(filesDir, TargetRuntime.PYTHON.relativeBinPath)
            val args = listOf(
                pythonBin.absolutePath,
                pythonTarget.name
            )
            val env = mapOf(
                "PORT" to port.toString(),
                "PYTHONUNBUFFERED" to "1"
            )
            return TargetDescriptor(TargetRuntime.PYTHON, pythonTarget, workspaceDir, pythonBin, args, env)
        }

        // Default Fallback: Shell Script or Generic Target
        val defaultFile = allFiles.firstOrNull() ?: File(workspaceDir, "app.sh")
        val defaultBin = File("/system/bin/sh")
        val args = listOf("sh", defaultFile.name)
        val env = mapOf("PORT" to port.toString(), "SERVER_PORT" to port.toString())
        return TargetDescriptor(TargetRuntime.UNKNOWN, defaultFile, workspaceDir, defaultBin, args, env)
    }

    /**
     * Resolves workspace directory conforming to `/data/user/0/<package>/files/workspace/`
     */
    private fun resolveWorkspaceDirectory(customPath: String?): File {
        if (!customPath.isNullOrBlank()) {
            val custom = File(customPath)
            if (custom.exists() || custom.mkdirs()) return custom
        }
        val defaultWorkspace = File(filesDir, "workspace").apply { if (!exists()) mkdirs() }

        // Auto-create default .env if not present
        val envFile = File(defaultWorkspace, ".env")
        if (!envFile.exists()) {
            try {
                envFile.writeText("SERVER_PORT=8080\nPORT=8080\n")
                log(LogLevel.INFO, "EnvLoader", "Auto-initialized default .env configuration file in workspace.")
            } catch (_: Exception) {}
        }

        return defaultWorkspace
    }

    /**
     * Spawns process execution and pipes stdout / stderr output asynchronously
     * into the thread-safe SharedFlow logs view via Kotlin Coroutines.
     */
    private suspend fun executeNativeProcess(target: TargetDescriptor) = withContext(Dispatchers.IO) {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val userStartCmd = prefs.getString(KEY_CUSTOM_START_COMMAND, null)?.trim()
            val activePort = prefs.getInt(KEY_CONFIG_PORT, DEFAULT_PORT)

            // Copy workspace files into userdata/ so they are mounted at /srv/app inside Proot
            val userdata = File(filesDir, "userdata").apply { mkdirs() }
            target.workspaceDir.listFiles()?.forEach { f ->
                try {
                    f.copyTo(File(userdata, f.name), overwrite = true)
                } catch (_: Exception) {}
            }

            // Sync any .env file in workspace
            val envFile = File(target.workspaceDir, ".env")
            val injectedEnv = mutableMapOf<String, String>()
            if (envFile.exists()) {
                envFile.readLines().forEach { raw ->
                    val line = raw.trim()
                    if (line.isNotEmpty() && !line.startsWith("#") && line.contains("=")) {
                        val parts = line.split("=", limit = 2)
                        injectedEnv[parts[0].trim()] = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
                    }
                }
            }

            // Determine command to run inside /srv/app
            if (target.entryFile.name.endsWith(".jar", ignoreCase = true)) {
                val analysis = JarBytecodeAnalyzer.analyze(target.entryFile)
                if (analysis.requiredJavaVersion > 0) {
                    log(LogLevel.INFO, "ExecutionEngine", "JAR Bytecode Analysis: Target Java ${analysis.requiredJavaVersion} (Classfile ${analysis.maxMajorVersion}.0, ${analysis.scannedClassesCount} app classes analyzed)")
                }
            }

            val defaultCmd = if (target.entryFile.name.endsWith(".jar", ignoreCase = true)) {
                "java -jar ${target.entryFile.name} --server.port=$activePort --server.address=0.0.0.0"
            } else if (target.entryFile.name.endsWith(".js", ignoreCase = true)) {
                "node ${target.entryFile.name}"
            } else if (target.entryFile.name.endsWith(".py", ignoreCase = true)) {
                "python3 ${target.entryFile.name}"
            } else {
                computeDefaultStartCommand(target.workspaceDir, activePort)
            }

            val finalCmd = if (!userStartCmd.isNullOrBlank()) {
                userStartCmd
            } else {
                defaultCmd
            }

            val insideRunCmd = "cd /srv/app && $finalCmd"

            log(LogLevel.INFO, "ExecutionEngine", "Root Directory: /srv/app (${userdata.absolutePath})")
            log(LogLevel.INFO, "ExecutionEngine", "Executing Start Command: $insideRunCmd")

            _serverState.value = ServerState.RUNNING
            _activePort.value = activePort
            _activeRuntime.value = target.runtime
            serverStartTimestamp = System.currentTimeMillis()
            setServerStartTimestamp(serverStartTimestamp)
            persistServerRunningState(true)

            // Start through LinuxEngine service manager (with backoff & crash recovery)
            val engine = EngineManager.getEngine(this@HostTriangleServerService)
            if (!engine.isInstalled()) {
                log(LogLevel.INFO, "ExecutionEngine", "Linux engine not installed. Provisioning environment...")
                engine.install { pct ->
                    if (pct % 20 == 0 || pct == 100) {
                        log(LogLevel.INFO, "ExecutionEngine", "Setting up Linux userland: $pct%...")
                    }
                }
            }
            engine.startService("backend", insideRunCmd)

            // Auto-start Cloudflare Tunnel
            val cfToken = prefs.getString(KEY_TUNNEL_TOKEN, DEFAULT_TUNNEL_TOKEN)?.trim() ?: DEFAULT_TUNNEL_TOKEN
            if (cfToken.isNotEmpty()) {
                CloudflareTunnelManager.startTunnel(
                    context = this@HostTriangleServerService,
                    token = cfToken,
                    localPort = activePort
                )
            }

            log(LogLevel.INFO, "ExecutionEngine", "Backend service dispatched via LinuxEngine.")
        } catch (e: Exception) {
            log(LogLevel.ERROR, "ProcessManager", "Fatal failure starting process: ${e.message}")
            _serverState.value = ServerState.ERROR
            stopServerEngine("Process spawn failure: ${e.message}")
        }
    }

    private fun isScriptFile(file: File): Boolean {
        if (!file.exists() || !file.canRead() || file.length() < 2) return false
        return try {
            val header = ByteArray(2)
            file.inputStream().use { it.read(header) }
            header[0] == '#'.code.toByte() && header[1] == '!'.code.toByte()
        } catch (_: Exception) {
            false
        }
    }

    private fun isElfBinary(file: File): Boolean {
        return try {
            val header = ByteArray(4)
            file.inputStream().use { it.read(header) }
            header[0] == 0x7F.toByte() && header[1] == 'E'.code.toByte() &&
                    header[2] == 'L'.code.toByte() && header[3] == 'F'.code.toByte()
        } catch (_: Exception) {
            false
        }
    }

    private fun getProcessPid(process: Process): String {
        return try {
            val pidField = process.javaClass.getDeclaredField("pid").apply {
                isAccessible = true
            }
            pidField.get(process)?.toString() ?: "ACTIVE"
        } catch (_: Exception) {
            "ACTIVE"
        }
    }

    /**
     * LOCK MANAGEMENT
     * Acquires native PowerManager.WakeLock and WifiManager.WifiLock to block CPU
     * core scaling or network sleep during screen-off cycles.
     */
    @SuppressLint("WakelockTimeout")
    private fun acquireHardwareLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "HostTriangle::ServerWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire()
                }
                log(LogLevel.DEBUG, TAG, "PowerManager PARTIAL_WAKE_LOCK acquired.")
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                @Suppress("DEPRECATION")
                wifiLock = wifiManager.createWifiLock(
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                    "HostTriangle::ServerWifiLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire()
                }
                log(LogLevel.DEBUG, TAG, "WifiManager WIFI_MODE_FULL_HIGH_PERF acquired.")
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, TAG, "Failure acquiring hardware locks: ${e.message}")
        }
    }

    private fun releaseHardwareLocks() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    log(LogLevel.DEBUG, TAG, "PowerManager WakeLock safely released.")
                }
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, TAG, "Exception releasing WakeLock: ${e.message}")
        } finally {
            wakeLock = null
        }

        try {
            wifiLock?.let {
                if (it.isHeld) {
                    it.release()
                    log(LogLevel.DEBUG, TAG, "WifiManager WifiLock safely released.")
                }
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, TAG, "Exception releasing WifiLock: ${e.message}")
        } finally {
            wifiLock = null
        }
    }

    /**
     * METRICS COROUTINE TELEMETRY LOOP
     * 5000ms loop collecting device vitals:
     * - RAM allocation percentages (ActivityManager.MemoryInfo)
     * - CPU core thermal indices: auto-shutdown/throttle if temp >= 43°C
     * - Battery capacity state: alert trigger if battery < 15%
     */
    private fun launchTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = serviceScope.launch {
            log(LogLevel.DEBUG, TAG, "Telemetry monitoring loop started. Period: ${TELEMETRY_INTERVAL_MS}ms")

            while (isActive) {
                try {
                    val updatedTelemetry = sampleTelemetry(this@HostTriangleServerService, serverStartTimestamp)
                    updateNotification(updatedTelemetry)

                    if (updatedTelemetry.isLowBattery) {
                        log(
                            LogLevel.WARN,
                            "PowerMonitor",
                            "BATTERY ALERT: Capacity at ${String.format(Locale.US, "%.1f", updatedTelemetry.batteryCapacity)}% (< 15%). Connect external power."
                        )
                    }

                    if (updatedTelemetry.isThermalBreached) {
                        log(
                            LogLevel.CRITICAL,
                            "ThermalGovernor",
                            "CRITICAL THERMAL CEILING BREACHED: ${String.format(Locale.US, "%.1f", updatedTelemetry.cpuTemperature)}°C >= 43.0°C! Triggering hardware safety shutdown."
                        )
                        _serverState.value = ServerState.THROTTLED
                        stopServerEngine(reason = "Emergency Thermal Shutdown (CPU > 43°C)")
                        break
                    }
                } catch (e: Exception) {
                    log(LogLevel.ERROR, TAG, "Error in telemetry sampling loop: ${e.message}")
                }

                delay(TELEMETRY_INTERVAL_MS)
            }
        }
    }

    /**
     * ENTERPRISE NOTIFICATION CHANNEL
     * Sticky notification displaying server statistics and a direct "Stop Server" execution action.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent execution status and telemetry for the HostTriangle edge runtime"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop Server Action Pending Intent
        val stopIntent = Intent(this, HostTriangleServerService::class.java).apply {
            action = ACTION_STOP_SERVER
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val port = _activePort.value
        val runtimeName = _activeRuntime.value.displayName
        val title = "HostTriangle [$runtimeName : $port]"

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Server",
                stopPendingIntent
            )
            .build()
    }

    private fun updateNotification(telemetry: DeviceTelemetry) {
        val statsText = "CPU: ${String.format(Locale.US, "%.1f", telemetry.cpuTemperature)}°C | " +
                "RAM: ${telemetry.ramUsagePercent.toInt()}% | " +
                "Battery: ${telemetry.batteryCapacity.toInt()}%"

        val notification = buildNotification(statsText)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopServerEngine(reason: String, preserveSticky: Boolean = false) {
        log(LogLevel.WARN, TAG, "Stopping server engine: $reason")
        processExecutionJob?.cancel()
        processExecutionJob = null

        telemetryJob?.cancel()
        telemetryJob = null
        serverStartTimestamp = 0L
        setServerStartTimestamp(0L)

        // Stop Built-in Cloudflare Tunnel
        EngineManager.getEngine(this).stopService("backend")
        CloudflareTunnelManager.stopTunnel()

        // Terminate native process
        currentNativeProcess?.let { proc ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    proc.destroyForcibly()
                } else {
                    proc.destroy()
                }
                log(LogLevel.INFO, "ProcessManager", "Native process destroyed forcibly.")
            } catch (e: Exception) {
                log(LogLevel.ERROR, "ProcessManager", "Error terminating native process: ${e.message}")
            } finally {
                currentNativeProcess = null
            }
        }

        releaseHardwareLocks()

        if (!preserveSticky) {
            persistServerRunningState(false)
        }

        if (_serverState.value != ServerState.THROTTLED && _serverState.value != ServerState.MISSING_RUNTIME) {
            _serverState.value = ServerState.STOPPED
        }

        log(LogLevel.INFO, TAG, "HostTriangle engine shutdown complete.")

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun persistServerRunningState(isRunning: Boolean) {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SERVER_RUNNING_STATE, isRunning).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        log(LogLevel.INFO, TAG, "HostTriangleServerService onDestroy invoked.")
        com.tinexus.hosttriangle.terminal.TerminalSessionManager.destroy()
        stopServerEngine(reason = "Service Destroyed")
        serviceScope.cancel()
    }
}
