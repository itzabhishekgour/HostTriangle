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

/**
 * TerminalBackend
 *
 * Abstraction layer decoupling terminal emulation & process management
 * from the presentation layer, allowing the underlying emulator (Termux, etc.)
 * to be swapped without rewriting the UI or session management.
 */
interface TerminalBackend {
    fun write(bytes: ByteArray)
    fun setOnOutputListener(listener: (ByteArray) -> Unit)
    fun resize(cols: Int, rows: Int)
    fun close()
}
