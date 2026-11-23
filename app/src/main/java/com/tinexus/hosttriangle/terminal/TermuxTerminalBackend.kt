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

package com.tinexus.hosttriangle.terminal

import android.content.Context
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.io.File

/**
 * TermuxTerminalBackend
 *
 * Implements TerminalBackend using Termux's terminal-emulator library.
 * Spawns PRoot Debian ARM64 with an interactive bash shell in a real PTY.
 */
class TermuxTerminalBackend(
    private val context: Context,
    private val sessionClient: TerminalSessionClient
) : TerminalBackend {

    var session: TerminalSession? = null
        private set

    private var outputListener: ((ByteArray) -> Unit)? = null

    fun startSession(initialCols: Int = 80, initialRows: Int = 24): TerminalSession {
        val proot = File(context.applicationInfo.nativeLibraryDir, "libproot.so")
        val loader = File(context.applicationInfo.nativeLibraryDir, "libproot-loader.so")
        val rootfsDir = File(context.filesDir, "engine/rootfs")
        val tmpDir = File(context.filesDir, "engine/tmp").apply { mkdirs() }
        val userdataDir = File(context.filesDir, "userdata").apply { mkdirs() }

        if (!proot.exists()) {
            throw IllegalStateException("PRoot binary not found at ${proot.absolutePath}")
        }
        if (!rootfsDir.exists()) {
            throw IllegalStateException("Debian RootFS not found at ${rootfsDir.absolutePath}")
        }

        File(rootfsDir, "srv/app").mkdirs()
        val shellBin = if (File(rootfsDir, "usr/bin/bash").exists()) "/usr/bin/bash" else "/bin/bash"

        val args = arrayOf(
            "proot",
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
            "-b", "${userdataDir.absolutePath}:/srv/app",
            "-r", rootfsDir.absolutePath,
            "-w", "/srv/app",
            "/usr/bin/env",
            "-i",
            "HOME=/root",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "TERM=xterm-256color",
            "LANG=C.UTF-8",
            shellBin,
            "-l"
        )

        val env = arrayOf(
            "PROOT_LOADER=${loader.absolutePath}",
            "PROOT_TMP_DIR=${tmpDir.absolutePath}",
            "LD_LIBRARY_PATH=${context.applicationInfo.nativeLibraryDir}",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/system/bin",
            "TERM=xterm-256color",
            "HOME=/root",
            "PROOT_NO_SECCOMP=1"
        )

        val newSession = TerminalSession(
            proot.absolutePath,
            userdataDir.absolutePath,
            args,
            env,
            2000,
            sessionClient
        )
        this.session = newSession
        return newSession
    }

    override fun write(bytes: ByteArray) {
        session?.write(bytes, 0, bytes.size)
    }

    override fun setOnOutputListener(listener: (ByteArray) -> Unit) {
        this.outputListener = listener
    }

    override fun resize(cols: Int, rows: Int) {
        session?.updateSize(cols, rows)
    }

    override fun close() {
        session?.finishIfRunning()
        session = null
    }
}
