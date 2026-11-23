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

/**
 * InstallProgress
 *
 * Real-time telemetry during Linux engine provisioning (download, tar extraction,
 * chmod mode application, DNS setup, apt configuration).
 */
data class InstallProgress(
    val percent: Int,
    val stage: String,
    val detail: String = "",
    val downloadedMb: Float = 0f,
    val totalMb: Float = 40.9f
)

/**
 * LinuxEngine
 *
 * Core architecture contract for running an isolated Linux userland inside HostTriangle.
 * Decouples process execution, service daemon orchestration, and package management
 * from the underlying virtualization/shim mechanism (PRoot vs Native/Termux).
 */
interface LinuxEngine {
    /**
     * Downloads, extracts, and provisions the Linux environment.
     * @param progress Callback emitting progress percentage (0..100).
     */
    suspend fun install(progress: (Int) -> Unit)

    /**
     * Rich progress callback variant emitting stage titles, current extracting file, and MB counts.
     */
    suspend fun installDetailed(onProgress: (InstallProgress) -> Unit) {
        install { pct ->
            onProgress(InstallProgress(pct, "Setting up Linux userland...", ""))
        }
    }

    /**
     * Checks if the Linux rootfs and engine binaries are fully installed and executable.
     */
    fun isInstalled(): Boolean

    /**
     * Executes a command within the Linux environment.
     * @param cmd Shell command to run.
     * @param env Extra environment variables to inject.
     * @return Running Process handle.
     */
    fun exec(cmd: String, env: Map<String, String> = emptyMap()): Process

    /**
     * Starts a managed background service inside the Linux userland.
     * Pipes output to `userdata/logs/<name>.log` and provides crash-recovery.
     */
    fun startService(name: String, cmd: String)

    /**
     * Stops a managed background service by name.
     */
    fun stopService(name: String)
}
