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
import android.os.Build
import android.system.Os
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * ProotEngine
 *
 * Full-fidelity Linux userland implementation using PRoot + Debian Bookworm arm64 rootfs.
 * Bypasses Android package-path mismatches and executes pure glibc/Debian userland
 * without requiring Termux.
 */
class ProotEngine(private val context: Context) : LinuxEngine {

    companion object {
        private const val ROOTFS_URL =
            "https://github.com/termux/proot-distro/releases/download/v4.17.3/debian-bookworm-aarch64-pd-v4.17.3.tar.xz"
        private const val ROOTFS_SHA256 =
            "3a841a794ae5999b33e33b329582ed0379d4f54ca62c6ce5a8eb9cff5ef8900b"

        private var needsNoSeccomp = false
    }

    private val activeServices = ConcurrentHashMap<String, Job>()
    private val activeProcesses = ConcurrentHashMap<String, Process>()
    private val installMutex = Mutex()

    val rootfsDir: File get() = File(context.filesDir, "engine/rootfs")
    val stagingDir: File get() = File(context.filesDir, "engine/rootfs_staging")
    val prootTmpDir: File get() = File(context.filesDir, "engine/tmp").apply { mkdirs() }
    val userdataDir: File get() = File(context.filesDir, "userdata").apply {
        mkdirs()
        File(this, "jars").mkdirs()
        File(this, "configs").mkdirs()
        File(this, "logs").mkdirs()
    }

    override fun isInstalled(): Boolean {
        // Auto-heal if staging was completed but rename was interrupted
        if (!rootfsDir.exists() && stagingDir.exists() && File(stagingDir, "usr/bin/bash").exists()) {
            val moved = stagingDir.renameTo(rootfsDir)
            if (!moved) {
                stagingDir.copyRecursively(rootfsDir, overwrite = true)
                stagingDir.deleteRecursively()
            }
            setupRootfsConfigs(rootfsDir)
        }
        val proot = File(context.applicationInfo.nativeLibraryDir, "libproot.so")
        val bashUsr = File(rootfsDir, "usr/bin/bash")
        val bashBin = File(rootfsDir, "bin/bash")
        val shUsr = File(rootfsDir, "usr/bin/sh")
        val shBin = File(rootfsDir, "bin/sh")
        return proot.exists() && (bashUsr.exists() || bashBin.exists() || shUsr.exists() || shBin.exists())
    }

    override suspend fun install(progress: (Int) -> Unit) {
        installDetailed { progress(it.percent) }
    }

    override suspend fun installDetailed(onProgress: (InstallProgress) -> Unit) = withContext(Dispatchers.IO) {
        installMutex.withLock {
            if (isInstalled()) {
                onProgress(InstallProgress(100, "Installation Complete", "Debian Linux engine is already installed."))
                return@withLock
            }

            HostTriangleServerService.log(
                HostTriangleServerService.LogLevel.INFO,
                "ProotEngine",
                "Initiating Debian Bookworm ARM64 RootFS provisioning..."
            )

            val tarball = File(context.cacheDir, "debian-rootfs.tar.xz")

            try {
                // STEP 1: Download Rootfs tarball (0% -> 60%)
                onProgress(InstallProgress(5, "Connecting to mirror...", "debian-bookworm-aarch64-pd-v4.17.3.tar.xz"))
                downloadWithProgress(ROOTFS_URL, tarball, ROOTFS_SHA256) { frac, dlBytes, totalBytes ->
                    val overall = (5 + frac * 55f).toInt().coerceIn(5, 60)
                    val dlMb = dlBytes / (1024f * 1024f)
                    val totMb = if (totalBytes > 0) totalBytes / (1024f * 1024f) else 40.9f
                    onProgress(
                        InstallProgress(
                            percent = overall,
                            stage = "Downloading Debian 12 Bookworm rootfs...",
                            detail = String.format(java.util.Locale.US, "%.1f MB / %.1f MB (%.0f%%)", dlMb, totMb, frac * 100),
                            downloadedMb = dlMb,
                            totalMb = totMb
                        )
                    )
                }

                // STEP 2: Extract to staging dir (60% -> 90%)
                onProgress(InstallProgress(60, "Preparing sandbox directory...", "engine/rootfs_staging"))
                if (stagingDir.exists()) stagingDir.deleteRecursively()
                stagingDir.mkdirs()

                HostTriangleServerService.log(
                    HostTriangleServerService.LogLevel.INFO,
                    "ProotEngine",
                    "Extracting rootfs archive into staging sandbox..."
                )
                extractTarXz(tarball, stagingDir) { done, total, fileName ->
                    if (total > 0) {
                        val p = (60 + ((done.toFloat() / total) * 30).toInt()).coerceIn(60, 90)
                        onProgress(
                            InstallProgress(
                                percent = p,
                                stage = "Extracting Debian Linux filesystem...",
                                detail = fileName
                            )
                        )
                    }
                }

                // STEP 3: Atomic rename staging -> rootfs (90% -> 92%)
                onProgress(InstallProgress(90, "Finalizing rootfs sandbox layout...", "engine/rootfs"))
                if (rootfsDir.exists()) rootfsDir.deleteRecursively()
                rootfsDir.parentFile?.mkdirs()
                val renamed = stagingDir.renameTo(rootfsDir)
                if (!renamed) {
                    stagingDir.copyRecursively(rootfsDir, overwrite = true)
                    stagingDir.deleteRecursively()
                }

                // STEP 4: Post-extract configuration (92% -> 95%)
                onProgress(InstallProgress(92, "Configuring network DNS and APT security...", "/etc/resolv.conf"))
                setupRootfsConfigs(rootfsDir)

                // STEP 5: First-run setup: certificates & curl inside PRoot (95% -> 100%)
                onProgress(InstallProgress(95, "Configuring Debian packages & Adoptium GPG keys...", "apt-get update"))
                HostTriangleServerService.log(
                    HostTriangleServerService.LogLevel.INFO,
                    "ProotEngine",
                    "Running post-install initialization (apt update & certificates)..."
                )
                val initProc = exec("apt-get update && apt-get install -y --no-install-recommends ca-certificates curl gpg && curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor > /etc/apt/trusted.gpg.d/adoptium.gpg && echo 'deb https://packages.adoptium.net/artifactory/deb bookworm main' > /etc/apt/sources.list.d/adoptium.list && apt-get update")
                initProc.inputStream.bufferedReader().forEachLine { line ->
                    HostTriangleServerService.log(HostTriangleServerService.LogLevel.DEBUG, "DebianInit", line)
                    onProgress(
                        InstallProgress(
                            percent = 97,
                            stage = "Configuring apt packages & Adoptium keys...",
                            detail = line.take(70)
                        )
                    )
                }
                initProc.waitFor()

                onProgress(InstallProgress(100, "Installation Complete!", "Debian 12 ARM64 is ready."))
                HostTriangleServerService.log(
                    HostTriangleServerService.LogLevel.INFO,
                    "ProotEngine",
                    "Debian ARM64 PRoot userland ready and verified."
                )
            } finally {
                try { tarball.delete() } catch (_: Exception) {}
            }
        }
    }

    override fun exec(cmd: String, env: Map<String, String>): Process {
        val proot = File(context.applicationInfo.nativeLibraryDir, "libproot.so")
        val loader = File(context.applicationInfo.nativeLibraryDir, "libproot-loader.so")

        if (!proot.exists()) {
            throw IllegalStateException("PRoot executable not found at ${proot.absolutePath}")
        }

        val userdata = userdataDir.absolutePath
        val rootfs = rootfsDir.absolutePath
        val tmpDir = prootTmpDir.absolutePath

        // Ensure rootfs mountpoint directory exists
        File(rootfsDir, "srv/app").mkdirs()

        val shellBin = if (File(rootfsDir, "usr/bin/bash").exists()) "/usr/bin/bash" else "/bin/bash"

        val args = mutableListOf(
            proot.absolutePath,
            "--kill-on-exit",
            "-0",
            "--link2symlink",
            "--sysvipc",
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/proc/self/fd:/dev/fd",
            "-b", "/proc/self/fd/0:/dev/stdin",
            "-b", "/proc/self/fd/1:/dev/stdout",
            "-b", "/proc/self/fd/2:/dev/stderr",
            "-b", "/sys",
            "-b", "$userdata:/srv/app",
            "-r", rootfs,

            "-w", "/srv/app",
            "/usr/bin/env",
            "-i",
            "HOME=/root",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "TERM=xterm-256color",
            "LANG=C.UTF-8"
        )

        // Append custom environment variables to inside proot
        env.forEach { (k, v) ->
            args.add("$k=$v")
        }

        args.add(shellBin)
        args.add("-l")
        args.add("-c")
        args.add(cmd)

        val pb = ProcessBuilder(args)
            .directory(rootfsDir)
            .redirectErrorStream(true)

        val hostEnv = pb.environment()
        hostEnv["PROOT_LOADER"] = loader.absolutePath
        hostEnv["PROOT_TMP_DIR"] = tmpDir
        hostEnv["LD_LIBRARY_PATH"] = context.applicationInfo.nativeLibraryDir
        if (needsNoSeccomp) {
            hostEnv["PROOT_NO_SECCOMP"] = "1"
        }

        try {
            val proc = pb.start()
            return proc
        } catch (e: Exception) {
            if (!needsNoSeccomp) {
                HostTriangleServerService.log(
                    HostTriangleServerService.LogLevel.WARN,
                    "ProotEngine",
                    "PRoot launch failed. Retrying with PROOT_NO_SECCOMP=1 on device ${Build.MODEL}..."
                )
                needsNoSeccomp = true
                hostEnv["PROOT_NO_SECCOMP"] = "1"
                return pb.start()
            }
            throw e
        }
    }

    override fun startService(name: String, cmd: String) {
        stopService(name)

        val logsDir = File(userdataDir, "logs").apply { mkdirs() }
        val logFile = File(logsDir, "$name.log")

        val job = CoroutineScope(Dispatchers.IO).launch {
            var backoffMs = 1000L
            while (isActive) {
                HostTriangleServerService.log(
                    HostTriangleServerService.LogLevel.INFO,
                    "ProotEngine",
                    "Spawning service [$name]: $cmd"
                )

                try {
                    val process = exec(cmd)
                    activeProcesses[name] = process

                    FileOutputStream(logFile, true).use { logOut ->
                        val reader = process.inputStream.bufferedReader()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val msg = line ?: ""
                            val entry = "$msg\n".toByteArray()
                            logOut.write(entry)
                            logOut.flush()
                            HostTriangleServerService.log(
                                HostTriangleServerService.LogLevel.INFO,
                                name,
                                msg
                            )
                        }
                    }

                    val exitCode = process.waitFor()
                    HostTriangleServerService.log(
                        HostTriangleServerService.LogLevel.WARN,
                        "ProotEngine",
                        "Service [$name] exited with code $exitCode"
                    )
                } catch (e: Exception) {
                    HostTriangleServerService.log(
                        HostTriangleServerService.LogLevel.ERROR,
                        "ProotEngine",
                        "Service [$name] failure: ${e.message}"
                    )
                }

                if (!isActive) break

                HostTriangleServerService.log(
                    HostTriangleServerService.LogLevel.WARN,
                    "ProotEngine",
                    "Restarting service [$name] in ${backoffMs}ms..."
                )
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
            }
        }

        activeServices[name] = job
    }

    override fun stopService(name: String) {
        activeServices.remove(name)?.cancel()
        activeProcesses.remove(name)?.let { proc ->
            try {
                proc.destroyForcibly()
            } catch (_: Exception) {}
        }
    }

    // -----------------------------------------------------------------------------------------
    // INTERNAL UTILITIES
    // -----------------------------------------------------------------------------------------

    private fun downloadWithProgress(
        urlStr: String,
        dest: File,
        expectedSha256: String,
        onProgress: (fraction: Float, downloaded: Long, total: Long) -> Unit
    ) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "HostTriangle-Proot/1.0")
            }
            conn.connect()

            val totalBytes = conn.contentLengthLong
            val digest = MessageDigest.getInstance("SHA-256")

            BufferedInputStream(conn.inputStream).use { input ->
                FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var downloaded = 0L

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        downloaded += read
                        val frac = if (totalBytes > 0) downloaded.toFloat() / totalBytes else 0f
                        onProgress(frac, downloaded, totalBytes)
                    }
                }
            }

            val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
            if (actualSha256.lowercase() != expectedSha256.lowercase()) {
                dest.delete()
                throw SecurityException(
                    "SHA256 checksum mismatch! Expected: $expectedSha256, Actual: $actualSha256"
                )
            }
        } finally {
            conn?.disconnect()
        }
    }

    private fun extractTarXz(
        tarball: File,
        destDir: File,
        onProgress: (done: Long, total: Long, currentFile: String) -> Unit
    ) {
        val estimatedTotalBytes = if (tarball.length() > 0) (tarball.length() * 2.8).toLong() else 115_000_000L
        var processedBytes = 0L
        var stepCounter = 0

        FileInputStream(tarball).use { rawIn ->
            BufferedInputStream(rawIn).use { bufIn ->
                XZInputStream(bufIn).use { xzIn ->
                    TarArchiveInputStream(xzIn).use { tarIn ->
                        val destCanonical = destDir.canonicalPath
                        var entry: TarArchiveEntry? = null

                        while (tarIn.nextEntry.also { entry = it } != null) {
                            val cur = entry ?: break

                            // Strip top-level tarball directory (e.g. debian-bookworm-aarch64/)
                            val rawName = cur.name
                            val cleanName = if (rawName.startsWith("debian-bookworm-aarch64/")) {
                                rawName.removePrefix("debian-bookworm-aarch64/")
                            } else if (rawName == "debian-bookworm-aarch64") {
                                ""
                            } else {
                                rawName.removePrefix("./")
                            }

                            if (cleanName.isEmpty()) {
                                continue
                            }

                            // Tar-slip path traversal validation
                            val targetFile = File(destDir, cleanName)
                            if (!targetFile.canonicalPath.startsWith(destCanonical)) {
                                throw SecurityException("Tar entry traverses outside dest: ${cur.name}")
                            }

                            if (cur.isDirectory) {
                                targetFile.mkdirs()
                                try {
                                    Os.chmod(targetFile.absolutePath, cur.mode)
                                } catch (_: Exception) {}
                            } else if (cur.isSymbolicLink) {
                                targetFile.parentFile?.mkdirs()
                                try { targetFile.delete() } catch (_: Exception) {}
                                val cleanLinkName = if (cur.linkName.startsWith("debian-bookworm-aarch64/")) {
                                    cur.linkName.removePrefix("debian-bookworm-aarch64/")
                                } else {
                                    cur.linkName
                                }
                                try {
                                    Os.symlink(cleanLinkName, targetFile.absolutePath)
                                } catch (_: Exception) {}
                            } else if (cur.isLink) {
                                // HARD LINK SUPPORT (Debian rootfs has extensive hardlinks)
                                targetFile.parentFile?.mkdirs()
                                try { targetFile.delete() } catch (_: Exception) {}
                                val cleanSource = if (cur.linkName.startsWith("debian-bookworm-aarch64/")) {
                                    cur.linkName.removePrefix("debian-bookworm-aarch64/")
                                } else {
                                    cur.linkName
                                }
                                val sourceFile = File(destDir, cleanSource)
                                try {
                                    Os.link(sourceFile.absolutePath, targetFile.absolutePath)
                                } catch (_: Exception) {
                                    // Fallback: If Os.link fails, duplicate the bytes
                                    if (sourceFile.exists()) {
                                        sourceFile.copyTo(targetFile, overwrite = true)
                                    }
                                }
                            } else if (cur.isFile) {
                                targetFile.parentFile?.mkdirs()
                                FileOutputStream(targetFile).use { out ->
                                    tarIn.copyTo(out, bufferSize = 32 * 1024)
                                }
                                // Apply exact Unix file modes
                                try {
                                    Os.chmod(targetFile.absolutePath, cur.mode)
                                } catch (_: Exception) {
                                    if ((cur.mode and 0b001_000_000) != 0 ||
                                        cur.name.contains("bin/") ||
                                        cur.name.contains("sbin/")) {
                                        targetFile.setExecutable(true, false)
                                    }
                                }
                            }

                            processedBytes += cur.size
                            stepCounter++
                            if (stepCounter % 15 == 0 || cur.size > 200_000) {
                                onProgress(processedBytes, estimatedTotalBytes, cleanName)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupRootfsConfigs(rootfs: File) {
        // 1. /etc/resolv.conf
        val etcDir = File(rootfs, "etc").apply { mkdirs() }
        val resolv = File(etcDir, "resolv.conf")
        try { resolv.delete() } catch (_: Exception) {}
        resolv.writeText("nameserver 1.1.1.1\nnameserver 8.8.8.8\n")

        // 2. /etc/hosts
        File(etcDir, "hosts").writeText("127.0.0.1 localhost\n::1 localhost\n")

        // 3. /tmp directory
        File(rootfs, "tmp").apply {
            mkdirs()
            setReadable(true, false)
            setWritable(true, false)
            setExecutable(true, false)
        }

        // 4. APT sandbox configuration (disables setuid _apt user check inside fake root)
        val aptConfDir = File(etcDir, "apt/apt.conf.d").apply { mkdirs() }
        File(aptConfDir, "99hosttriangle").writeText(
            """
            APT::Sandbox::User "root";
            Acquire::Languages "none";
            Acquire::GzipIndexes "true";
            APT::Install-Recommends "false";
            """.trimIndent()
        )
    }
}
