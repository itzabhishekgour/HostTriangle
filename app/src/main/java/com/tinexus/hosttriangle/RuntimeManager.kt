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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * RuntimeManager
 *
 * Provisioning manager for Debian-based headless runtimes inside LinuxEngine.
 * Orchestrates on-demand installation of minimal Debian headless packages:
 * - Eclipse Temurin headless JREs (temurin-21-jre, temurin-17-jre, temurin-11-jre, temurin-8-jre)
 * - Node.js LTS
 * - Python 3 Runtime
 */
object RuntimeManager {

    sealed class Status {
        object NotInstalled : Status()
        data class Downloading(val progress: Float, val downloadedMb: Float, val totalMb: Float) : Status()
        object Extracting : Status()
        object Ready : Status()
        data class Error(val message: String) : Status()
    }

    data class RuntimeSpec(
        val runtime: HostTriangleServerService.TargetRuntime,
        val title: String,
        val subtitle: String,
        val versionTag: String,
        val sizeMb: Float,
        val aptPackageName: String,
        val checkCommand: String,
        val sampleTemplateName: String
    )

    data class JavaOption(
        val version: Int,
        val label: String,
        val packageName: String,
        val sizeMb: Float
    )

    val availableJavaOptions = listOf(
        JavaOption(21, "Java 21 LTS (Default)", "temurin-21-jre", 45.0f),
        JavaOption(17, "Java 17 LTS", "temurin-17-jre", 42.0f),
        JavaOption(11, "Java 11 LTS", "temurin-11-jre", 40.0f),
        JavaOption(8, "Java 8 LTS", "temurin-8-jre", 32.0f)
    )

    val supportedRuntimes = listOf(
        RuntimeSpec(
            runtime = HostTriangleServerService.TargetRuntime.SPRING_BOOT,
            title = "Java 21 JRE (Temurin LTS)",
            subtitle = "Headless JRE (~45MB) for Spring Boot & JVM",
            versionTag = "Temurin 21 JRE",
            sizeMb = 45.0f,
            aptPackageName = "temurin-21-jre",
            checkCommand = "java -version",
            sampleTemplateName = "spring-backend.jar"
        ),
        RuntimeSpec(
            runtime = HostTriangleServerService.TargetRuntime.NODE_JS,
            title = "Node.js 20 LTS",
            subtitle = "Express.js, Fastify, REST APIs & WebSockets",
            versionTag = "Node.js v20 (Debian)",
            sizeMb = 35.0f,
            aptPackageName = "nodejs",
            checkCommand = "node -v",
            sampleTemplateName = "index.js"
        ),
        RuntimeSpec(
            runtime = HostTriangleServerService.TargetRuntime.PYTHON,
            title = "Python 3.11 Runtime",
            subtitle = "FastAPI, Flask, ASGI & DePIN Compute Relays",
            versionTag = "Python 3.11 (Debian)",
            sizeMb = 45.0f,
            aptPackageName = "python3 python3-pip",
            checkCommand = "python3 --version",
            sampleTemplateName = "main.py"
        )
    )

    private val _statusMap = MutableStateFlow<Map<HostTriangleServerService.TargetRuntime, Status>>(emptyMap())
    val statusMap: StateFlow<Map<HostTriangleServerService.TargetRuntime, Status>> = _statusMap.asStateFlow()

    private val _installedJavaVersion = MutableStateFlow<Int>(0)
    val installedJavaVersion: StateFlow<Int> = _installedJavaVersion.asStateFlow()

    private val managerScope = CoroutineScope(Dispatchers.IO)
    private val activeJobs = mutableMapOf<HostTriangleServerService.TargetRuntime, Job>()

    fun refreshStatuses(context: Context) {
        managerScope.launch {
            val engine = EngineManager.getEngine(context)
            val isEngineInstalled = engine.isInstalled()

            val newMap = mutableMapOf<HostTriangleServerService.TargetRuntime, Status>()

            if (!isEngineInstalled) {
                supportedRuntimes.forEach { spec ->
                    newMap[spec.runtime] = Status.NotInstalled
                }
                _statusMap.value = newMap
                _installedJavaVersion.value = 0
                return@launch
            }

            supportedRuntimes.forEach { spec ->
                val installed = isRuntimeInstalled(engine, spec)
                newMap[spec.runtime] = if (installed) Status.Ready else Status.NotInstalled
            }

            _statusMap.value = newMap
            _installedJavaVersion.value = detectInstalledJavaVersion(engine)
        }
    }

    private fun isRuntimeInstalled(engine: LinuxEngine, spec: RuntimeSpec): Boolean {
        return try {
            val proc = engine.exec(spec.checkCommand)
            val exit = proc.waitFor()
            exit == 0
        } catch (_: Exception) {
            false
        }
    }

    private fun detectInstalledJavaVersion(engine: LinuxEngine): Int {
        return try {
            val proc = engine.exec("java -version")
            val output = StringBuilder()
            proc.inputStream.bufferedReader().use { r -> output.append(r.readText()) }
            proc.errorStream.bufferedReader().use { r -> output.append("\n").append(r.readText()) }
            proc.waitFor()

            val text = output.toString()
            // Match e.g. 'version "21.0.2"' or 'version "17.0.10"' or 'version "1.8.0_402"'
            val regex = """version\s+"(?:1\.)?(\d+)""".toRegex()
            val match = regex.find(text)
            match?.groupValues?.get(1)?.toIntOrNull() ?: 0
        } catch (_: Exception) {
            0
        }
    }

    fun isInstalled(context: Context, runtime: HostTriangleServerService.TargetRuntime): Boolean {
        val status = _statusMap.value[runtime]
        return status is Status.Ready
    }

    fun installRuntime(context: Context, spec: RuntimeSpec) {
        installRuntime(context, spec.runtime)
    }

    fun installRuntime(context: Context, runtime: HostTriangleServerService.TargetRuntime) {
        val spec = supportedRuntimes.find { it.runtime == runtime } ?: return
        installPackage(context, runtime, spec.aptPackageName, spec.title)
    }

    fun installJavaVersion(context: Context, version: Int, onComplete: ((Boolean) -> Unit)? = null) {
        val opt = availableJavaOptions.find { it.version == version } ?: availableJavaOptions.first()
        installPackage(context, HostTriangleServerService.TargetRuntime.SPRING_BOOT, opt.packageName, opt.label) { success ->
            onComplete?.invoke(success)
        }
    }

    private fun installPackage(
        context: Context,
        runtime: HostTriangleServerService.TargetRuntime,
        packageName: String,
        displayTitle: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (activeJobs.containsKey(runtime)) return

        val job = managerScope.launch {
            var success = false
            try {
                val engine = EngineManager.getEngine(context)

                // 1. Ensure LinuxEngine (Debian PRoot RootFS) is installed
                if (!engine.isInstalled()) {
                    updateStatus(runtime, Status.Downloading(0.05f, 5f, 100f))
                    HostTriangleServerService.log(
                        HostTriangleServerService.LogLevel.INFO,
                        "RuntimeManager",
                        "Linux engine not installed. Provisioning Debian rootfs..."
                    )

                    engine.install { pct ->
                        val progressFloat = pct / 100f
                        if (pct < 90) {
                            updateStatus(
                                runtime,
                                Status.Downloading(progressFloat, pct.toFloat(), 100f)
                            )
                        } else {
                            updateStatus(runtime, Status.Extracting)
                        }
                    }
                }

                // 2. Run real Debian APT package installation
                updateStatus(runtime, Status.Downloading(0.92f, 92f, 100f))
                HostTriangleServerService.log(
                    HostTriangleServerService.LogLevel.INFO,
                    "RuntimeManager",
                    "Executing apt-get install -y $packageName..."
                )

                val aptCommand = "DEBIAN_FRONTEND=noninteractive apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends $packageName"
                val proc = engine.exec(aptCommand)

                proc.inputStream.bufferedReader().forEachLine { line ->
                    HostTriangleServerService.log(
                        HostTriangleServerService.LogLevel.DEBUG,
                        "APT",
                        line
                    )
                }

                val exitCode = proc.waitFor()
                if (exitCode == 0) {
                    refreshStatuses(context)
                    updateStatus(runtime, Status.Ready)
                    HostTriangleServerService.log(
                        HostTriangleServerService.LogLevel.INFO,
                        "RuntimeManager",
                        "$displayTitle successfully provisioned via Debian APT."
                    )
                    success = true
                } else {
                    val errMsg = "APT exited with code $exitCode"
                    updateStatus(runtime, Status.Error(errMsg))
                    HostTriangleServerService.log(
                        HostTriangleServerService.LogLevel.ERROR,
                        "RuntimeManager",
                        errMsg
                    )
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Installation failed"
                updateStatus(runtime, Status.Error(msg))
                HostTriangleServerService.log(
                    HostTriangleServerService.LogLevel.ERROR,
                    "RuntimeManager",
                    "Runtime install error: $msg"
                )
            } finally {
                activeJobs.remove(runtime)
                onComplete?.invoke(success)
            }
        }

        activeJobs[runtime] = job
    }

    fun purgeUnusedJavaRuntimes(context: Context, keepVersion: Int, onResult: (Boolean, String) -> Unit) {
        managerScope.launch {
            try {
                val engine = EngineManager.getEngine(context)
                val packagesToPurge = availableJavaOptions
                    .filter { it.version != keepVersion }
                    .map { it.packageName }
                    .joinToString(" ")

                if (packagesToPurge.isBlank()) {
                    onResult(true, "No other runtimes to purge.")
                    return@launch
                }

                HostTriangleServerService.log(
                    HostTriangleServerService.LogLevel.INFO,
                    "RuntimeManager",
                    "Purging unused runtimes: $packagesToPurge..."
                )
                val proc = engine.exec("DEBIAN_FRONTEND=noninteractive apt-get purge -y $packagesToPurge && DEBIAN_FRONTEND=noninteractive apt-get autoremove -y")
                val exitCode = proc.waitFor()
                refreshStatuses(context)
                if (exitCode == 0) {
                    onResult(true, "Purged unused runtimes and reclaimed disk storage.")
                } else {
                    onResult(false, "Purge exited with code $exitCode")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Purge failed")
            }
        }
    }

    // Node.js Version Detection
    data class NodeDetectionResult(
        val version: String,
        val isExplicit: Boolean,
        val warning: String?
    )

    fun detectNodeRequirement(workspaceDir: File): NodeDetectionResult {
        val pkgJson = File(workspaceDir, "package.json")
        if (pkgJson.exists()) {
            try {
                val content = pkgJson.readText()
                val regex = """"engines"\s*:\s*\{[^}]*"node"\s*:\s*"([^"]+)"""".toRegex()
                val match = regex.find(content)
                if (match != null) {
                    return NodeDetectionResult(
                        version = match.groupValues[1],
                        isExplicit = true,
                        warning = null
                    )
                }
            } catch (_: Exception) {}
        }
        return NodeDetectionResult(
            version = "20 LTS",
            isExplicit = false,
            warning = "No 'engines.node' specified in package.json. Defaulting to Node.js 20 LTS."
        )
    }

    // Python Version Detection
    data class PythonDetectionResult(
        val version: String,
        val isExplicit: Boolean,
        val sourceFile: String?,
        val warning: String?
    )

    fun detectPythonRequirement(workspaceDir: File): PythonDetectionResult {
        val runtimeTxt = File(workspaceDir, "runtime.txt")
        if (runtimeTxt.exists()) {
            try {
                val line = runtimeTxt.readLines().firstOrNull { it.isNotBlank() }?.trim()
                if (!line.isNullOrBlank()) {
                    return PythonDetectionResult(
                        version = line.removePrefix("python-"),
                        isExplicit = true,
                        sourceFile = "runtime.txt",
                        warning = null
                    )
                }
            } catch (_: Exception) {}
        }
        val pyproject = File(workspaceDir, "pyproject.toml")
        if (pyproject.exists()) {
            try {
                val text = pyproject.readText()
                val regex = """requires-python\s*=\s*"([^"]+)"""".toRegex()
                val match = regex.find(text)
                if (match != null) {
                    return PythonDetectionResult(
                        version = match.groupValues[1],
                        isExplicit = true,
                        sourceFile = "pyproject.toml",
                        warning = null
                    )
                }
            } catch (_: Exception) {}
        }
        return PythonDetectionResult(
            version = "3.11",
            isExplicit = false,
            sourceFile = null,
            warning = "No 'runtime.txt' or 'pyproject.toml' found. Defaulting to Python 3.11."
        )
    }

    private fun updateStatus(runtime: HostTriangleServerService.TargetRuntime, status: Status) {
        val current = _statusMap.value.toMutableMap()
        current[runtime] = status
        _statusMap.value = current
    }
}
