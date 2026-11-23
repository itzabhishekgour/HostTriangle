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

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * CloudflareTunnelManager
 *
 * Built-In Zero-Config Cloudflare Tunnel Lifecycle Engine for Android.
 * Automatically provisions the official Linux ARM64 'cloudflared' binary,
 * assigns POSIX 755 execution permissions, and spawns the tunnel subprocess
 * with the user's secret token without requiring Termux or manual terminal commands.
 */
object CloudflareTunnelManager {

    private const val TAG = "CloudflareTunnel"
    private const val CLOUDFLARED_DOWNLOAD_URL =
        "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64"
    private const val ESTIMATED_SIZE_MB = 28.5f

    sealed class TunnelStatus {
        object NotInstalled : TunnelStatus()
        data class Downloading(val progress: Float, val downloadedMb: Float, val totalMb: Float) : TunnelStatus()
        object Ready : TunnelStatus()
        object Connecting : TunnelStatus()
        data class Connected(val edgeLocation: String, val uptimeSec: Long) : TunnelStatus()
        object Stopped : TunnelStatus()
        data class Error(val message: String) : TunnelStatus()
    }

    private val _status = MutableStateFlow<TunnelStatus>(TunnelStatus.NotInstalled)
    val status: StateFlow<TunnelStatus> = _status.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var downloadJob: Job? = null
    private var tunnelProcessJob: Job? = null
    private var activeProcess: Process? = null
    private var tunnelStartTimestamp: Long = 0L

    @Volatile
    private var isDesiredRunning: Boolean = false

    fun getExecutable(context: Context): File {
        val nativeLib = File(context.applicationInfo.nativeLibraryDir, "libcloudflared.so")
        val internalBin = File(context.filesDir, "runtimes/cloudflared/cloudflared")
        val termuxBin = File("/data/data/com.termux/files/usr/bin/cloudflared")
        return when {
            nativeLib.exists() && nativeLib.canExecute() -> nativeLib
            nativeLib.exists() -> nativeLib
            termuxBin.exists() && termuxBin.canExecute() -> termuxBin
            internalBin.exists() && internalBin.canExecute() -> internalBin
            else -> nativeLib
        }
    }

    fun isInstalled(context: Context): Boolean {
        val nativeLib = File(context.applicationInfo.nativeLibraryDir, "libcloudflared.so")
        if (nativeLib.exists() && nativeLib.length() > 1000L) return true
        val bin = getExecutable(context)
        return bin.exists() && bin.length() > 1000L
    }

    fun refreshStatus(context: Context) {
        if (activeProcess != null) return
        if (isInstalled(context)) {
            _status.value = TunnelStatus.Ready
        } else {
            _status.value = TunnelStatus.NotInstalled
        }
    }

    fun installDaemon(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        if (downloadJob?.isActive == true) return
        val bin = getExecutable(context)

        downloadJob = scope.launch {
            try {
                bin.parentFile?.mkdirs()
                _status.value = TunnelStatus.Downloading(0.05f, 0f, ESTIMATED_SIZE_MB)
                HostTriangleServerService.log(
                    HostTriangleServerService.LogLevel.INFO,
                    TAG,
                    "Downloading built-in cloudflared-linux-arm64 binary (~28 MB)..."
                )

                val success = attemptDownloadBinary(bin)
                if (success) {
                    try {
                        Runtime.getRuntime().exec(arrayOf("chmod", "755", bin.absolutePath)).waitFor()
                    } catch (_: Exception) {}
                    bin.setExecutable(true, false)
                    bin.setReadable(true, false)

                    _status.value = TunnelStatus.Ready
                    HostTriangleServerService.log(
                        HostTriangleServerService.LogLevel.INFO,
                        TAG,
                        "Cloudflared ARM64 daemon installed successfully. Ready for instant 1-tap tunnels."
                    )
                    withContext(Dispatchers.Main) { onComplete?.invoke(true) }
                } else {
                    HostTriangleServerService.log(
                        HostTriangleServerService.LogLevel.ERROR,
                        TAG,
                        "Failed to download cloudflared daemon from official CDN. Please verify internet connectivity."
                    )
                    _status.value = TunnelStatus.Error("Daemon download failed")
                    withContext(Dispatchers.Main) { onComplete?.invoke(false) }
                }
            } catch (e: Exception) {
                HostTriangleServerService.log(
                    HostTriangleServerService.LogLevel.ERROR,
                    TAG,
                    "Daemon download error: ${e.message}"
                )
                _status.value = TunnelStatus.Error(e.message ?: "Download failed")
                withContext(Dispatchers.Main) { onComplete?.invoke(false) }
            }
        }
    }

    private suspend fun attemptDownloadBinary(targetFile: File): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(CLOUDFLARED_DOWNLOAD_URL)
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 15000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "HostTriangle-Android/1.0")
            }
            conn.connect()

            if (conn.responseCode !in 200..299) {
                return@withContext false
            }

            val totalBytes = conn.contentLengthLong
            val totalMb = if (totalBytes > 0) totalBytes / (1024f * 1024f) else ESTIMATED_SIZE_MB
            val tempFile = File(targetFile.parentFile, "cloudflared.tmp")

            var downloadedBytes = 0L
            val buffer = ByteArray(16384)

            BufferedInputStream(conn.inputStream).use { input ->
                FileOutputStream(tempFile).use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val curMb = downloadedBytes / (1024f * 1024f)
                        val progress = (curMb / totalMb).coerceIn(0.05f, 0.99f)
                        _status.value = TunnelStatus.Downloading(progress, curMb, totalMb)
                    }
                }
            }

            if (tempFile.renameTo(targetFile)) {
                return@withContext true
            }
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
            return@withContext true
        } catch (e: Exception) {
            HostTriangleServerService.log(
                HostTriangleServerService.LogLevel.WARN,
                TAG,
                "Direct cloudflared CDN download interrupted: ${e.message}"
            )
            return@withContext false
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Starts the Cloudflare Tunnel daemon asynchronously in the background.
     * Passes the user's secret token and pipes stdout/stderr into the terminal log view.
     * Built-in Watchdog automatically restarts the tunnel with exponential backoff if terminated.
     */
    fun startTunnel(context: Context, token: String, localPort: Int = 8080) {
        if (activeProcess != null && isDesiredRunning) {
            HostTriangleServerService.log(
                HostTriangleServerService.LogLevel.WARN,
                TAG,
                "Cloudflare Tunnel daemon is already active."
            )
            return
        }

        val cleanToken = token.trim()
        if (cleanToken.isEmpty()) {
            HostTriangleServerService.log(
                HostTriangleServerService.LogLevel.ERROR,
                TAG,
                "Cannot start Cloudflare Tunnel: Secret Token is empty."
            )
            return
        }

        val bin = getExecutable(context)
        if (!isInstalled(context)) {
            HostTriangleServerService.log(
                HostTriangleServerService.LogLevel.INFO,
                TAG,
                "Cloudflared binary not found on phone. Auto-provisioning daemon now..."
            )
            installDaemon(context) { success ->
                if (success) {
                    startTunnel(context, cleanToken, localPort)
                }
            }
            return
        }

        isDesiredRunning = true
        tunnelProcessJob?.cancel()
        tunnelProcessJob = scope.launch {
            var retryCount = 0

            while (isActive && isDesiredRunning) {
                try {
                    _status.value = TunnelStatus.Connecting
                    tunnelStartTimestamp = System.currentTimeMillis()

                    try {
                        Runtime.getRuntime().exec(arrayOf("chmod", "755", bin.absolutePath)).waitFor()
                    } catch (_: Exception) {}

                    val isScript = try {
                        val head = bin.readBytes().take(2)
                        head.size == 2 && head[0] == '#'.code.toByte() && head[1] == '!'.code.toByte()
                    } catch (_: Exception) {
                        false
                    }

                    val isNativeLib = bin.absolutePath.contains("/lib") || bin.name.endsWith(".so")
                    val commandArgs = if (isScript) {
                        listOf("/system/bin/sh", bin.absolutePath, "tunnel", "run", "--dns-resolver-addrs", "1.1.1.1:53", "--dns-resolver-addrs", "8.8.8.8:53", "--token", cleanToken)
                    } else if (isNativeLib) {
                        listOf(bin.absolutePath, "tunnel", "--no-autoupdate", "run", "--dns-resolver-addrs", "1.1.1.1:53", "--dns-resolver-addrs", "8.8.8.8:53", "--token", cleanToken)
                    } else {
                        listOf(
                            "/system/bin/sh",
                            "-c",
                            "${bin.absolutePath} tunnel --no-autoupdate run --dns-resolver-addrs 1.1.1.1:53 --dns-resolver-addrs 8.8.8.8:53 --token $cleanToken"
                        )
                    }

                    if (retryCount == 0) {
                        HostTriangleServerService.log(
                            HostTriangleServerService.LogLevel.INFO,
                            TAG,
                            "Spawning built-in Cloudflare Tunnel daemon for token: [${cleanToken.take(12)}...${cleanToken.takeLast(6)}]"
                        )
                    } else {
                        HostTriangleServerService.log(
                            HostTriangleServerService.LogLevel.INFO,
                            TAG,
                            "Watchdog: Re-spawning Cloudflare Tunnel daemon (attempt #$retryCount)..."
                        )
                    }

                    val pb = ProcessBuilder(commandArgs)
                        .directory(context.filesDir)

                    val env = pb.environment()
                    env["HOME"] = context.filesDir.absolutePath
                    env["PATH"] = "${bin.parentFile?.absolutePath ?: ""}:/system/bin:/system/xbin"
                    env["TUNNEL_DNS_RESOLVER_ADDRS"] = "1.1.1.1:53,8.8.8.8:53"

                    pb.redirectErrorStream(true)
                    val process = pb.start()
                    activeProcess = process

                    // Asynchronously read tunnel output and detect active connection
                    val readerJob = launch(Dispatchers.IO) {
                        try {
                            process.inputStream.bufferedReader().useLines { lines ->
                                lines.forEach { line ->
                                    if (isActive) {
                                        HostTriangleServerService.log(
                                            HostTriangleServerService.LogLevel.INFO,
                                            "CLOUDFLARE",
                                            line
                                        )

                                        if (line.contains("Registered tunnel connection", ignoreCase = true) ||
                                            line.contains("Connection established", ignoreCase = true) ||
                                            line.contains("Connected to", ignoreCase = true)
                                        ) {
                                            val uptime = (System.currentTimeMillis() - tunnelStartTimestamp) / 1000
                                            val edgeLoc = if (line.contains("location=")) {
                                                val loc = line.substringAfter("location=").substringBefore(" ").trim().uppercase()
                                                if (loc.startsWith("BOM")) "BOM (Mumbai)" else "$loc Edge"
                                            } else {
                                                "BOM (Mumbai)"
                                            }
                                            _status.value = TunnelStatus.Connected(edgeLoc, uptime)
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    val exitCode = process.waitFor()
                    readerJob.cancel()
                    val runtimeSec = (System.currentTimeMillis() - tunnelStartTimestamp) / 1000

                    // If tunnel ran stably for >= 60 seconds, reset backoff counter
                    if (runtimeSec >= 60) {
                        retryCount = 0
                    }

                    if (!isDesiredRunning) {
                        _status.value = TunnelStatus.Stopped
                        break
                    }

                    retryCount++
                    val backoffSec = (1L shl retryCount.coerceAtMost(5)).coerceIn(2L, 30L)
                    HostTriangleServerService.log(
                        HostTriangleServerService.LogLevel.WARN,
                        TAG,
                        "Cloudflare Tunnel process terminated (Exit: $exitCode, ran ${runtimeSec}s). Watchdog auto-restarting in ${backoffSec}s (attempt #$retryCount)..."
                    )
                    _status.value = TunnelStatus.Connecting
                    delay(backoffSec * 1000L)

                } catch (e: Exception) {
                    if (!isDesiredRunning) break
                    retryCount++
                    val backoffSec = (1L shl retryCount.coerceAtMost(5)).coerceIn(2L, 30L)
                    HostTriangleServerService.log(
                        HostTriangleServerService.LogLevel.ERROR,
                        TAG,
                        "Tunnel error: ${e.message}. Watchdog restarting in ${backoffSec}s..."
                    )
                    _status.value = TunnelStatus.Error(e.message ?: "Tunnel crash")
                    delay(backoffSec * 1000L)
                } finally {
                    activeProcess = null
                }
            }
        }
    }

    fun stopTunnel() {
        isDesiredRunning = false
        tunnelProcessJob?.cancel()
        tunnelProcessJob = null

        activeProcess?.let { proc ->
            try {
                proc.destroyForcibly()
                HostTriangleServerService.log(
                    HostTriangleServerService.LogLevel.INFO,
                    TAG,
                    "Cloudflare Tunnel process terminated cleanly."
                )
            } catch (_: Exception) {}
        }
        activeProcess = null
        _status.value = TunnelStatus.Stopped
    }
}
