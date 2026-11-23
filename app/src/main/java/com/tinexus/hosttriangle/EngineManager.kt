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

/**
 * EngineManager
 *
 * Singleton registry providing access to the active LinuxEngine implementation.
 * Exclusively provides ProotEngine for 100% Debian ARM64 userland compatibility.
 */
object EngineManager {

    @Volatile
    private var activeEngineInstance: LinuxEngine? = null

    fun getEngine(context: Context): LinuxEngine {
        return activeEngineInstance ?: synchronized(this) {
            activeEngineInstance ?: ProotEngine(context.applicationContext).also {
                activeEngineInstance = it
            }
        }
    }
}
