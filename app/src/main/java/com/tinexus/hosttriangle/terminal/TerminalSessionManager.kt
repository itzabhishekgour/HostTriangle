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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

/**
 * TerminalSessionManager
 *
 * Singleton manager providing persistent Linux PTY sessions across Compose tab transitions.
 * Ensures the interactive bash session inside Debian PRoot remains alive when the user navigates
 * between Dashboard, Deployment, and Terminal tabs, seamlessly reattaching TerminalView.
 */
object TerminalSessionManager : TerminalSessionClient, TerminalViewClient {

    private const val TAG = "TerminalSessionMgr"

    @Volatile
    private var isNativeLoaded: Boolean? = null
    var nativeLoadError: String? = null
        private set

    /**
     * Safe-load wrapper for libtermux.so.
     * Prevents app crashes if the native library is missing or fails on unsupported architectures.
     * Guaranteed to load ONLY when the terminal tab is opened, never at app start.
     */
    fun ensureNativeLibraryLoaded(): Boolean {
        isNativeLoaded?.let { return it }
        synchronized(this) {
            isNativeLoaded?.let { return it }
            return try {
                System.loadLibrary("termux")
                isNativeLoaded = true
                nativeLoadError = null
                Log.i(TAG, "libtermux.so loaded successfully via safe loader")
                true
            } catch (e: Throwable) {
                isNativeLoaded = false
                nativeLoadError = e.message ?: "UnsatisfiedLinkError"
                Log.e(TAG, "Safe load failed for libtermux.so: ${e.message}", e)
                false
            }
        }
    }

    fun isSupported(): Boolean = ensureNativeLibraryLoaded()

    var currentBackend: TermuxTerminalBackend? = null
        private set

    val session: TerminalSession?
        get() = currentBackend?.session

    var attachedView: TerminalView? = null
        private set

    var isCtrlActive: Boolean = false

    var onSessionStateChanged: (() -> Unit)? = null

    val isRunning: Boolean
        get() = session?.isRunning == true

    fun getOrCreateSession(context: Context): TerminalSession? {
        if (!ensureNativeLibraryLoaded()) {
            Log.e(TAG, "Cannot create session: native library not loaded")
            return null
        }
        val existing = session
        if (existing != null && existing.isRunning) {
            existing.updateTerminalSessionClient(this)
            return existing
        }

        currentBackend?.close()
        val backend = TermuxTerminalBackend(context.applicationContext, this)
        currentBackend = backend
        val newSession = backend.startSession(80, 24)
        onSessionStateChanged?.invoke()
        return newSession
    }

    fun restartSession(context: Context): TerminalSession? {
        if (!ensureNativeLibraryLoaded()) {
            Log.e(TAG, "Cannot restart session: native library not loaded")
            return null
        }
        currentBackend?.close()
        val backend = TermuxTerminalBackend(context.applicationContext, this)
        currentBackend = backend
        val newSession = backend.startSession(80, 24)
        attachedView?.attachSession(newSession)
        attachedView?.onScreenUpdated()
        onSessionStateChanged?.invoke()
        return newSession
    }

    fun attachView(view: TerminalView, context: Context): Boolean {
        if (!ensureNativeLibraryLoaded()) {
            Log.e(TAG, "Cannot attach view: native library not loaded")
            return false
        }
        this.attachedView = view
        view.setTerminalViewClient(this)
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        val sess = getOrCreateSession(context) ?: return false
        view.attachSession(sess)
        view.setTextSize(34)
        view.post {
            view.requestFocus()
            view.onScreenUpdated()
        }
        return true
    }

    fun detachView(view: TerminalView) {
        if (this.attachedView == view) {
            this.attachedView = null
        }
    }

    fun write(bytes: ByteArray) {
        session?.write(bytes, 0, bytes.size)
    }

    fun writeText(text: String) {
        write(text.toByteArray(Charsets.UTF_8))
    }

    fun destroy() {
        currentBackend?.close()
        currentBackend = null
        attachedView = null
        onSessionStateChanged?.invoke()
    }

    // ==========================================
    // TerminalSessionClient Implementation
    // ==========================================
    override fun onTextChanged(changedSession: TerminalSession?) {
        attachedView?.onScreenUpdated()
    }

    override fun onTitleChanged(changedSession: TerminalSession?) {}

    override fun onSessionFinished(finishedSession: TerminalSession?) {
        if (finishedSession != session) return
        onSessionStateChanged?.invoke()
    }

    override fun onCopyTextToClipboard(session: TerminalSession?, text: String?) {
        val view = attachedView ?: return
        if (!text.isNullOrEmpty()) {
            val cm = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("Terminal", text))
            Toast.makeText(view.context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPasteTextFromClipboard(session: TerminalSession?) {
        val view = attachedView ?: return
        val cm = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = cm?.primaryClip?.getItemAt(0)?.text?.toString()
        if (!clip.isNullOrEmpty()) {
            session?.write(clip)
        }
    }

    override fun onBell(session: TerminalSession?) {}
    override fun onColorsChanged(session: TerminalSession?) {}
    override fun onTerminalCursorStateChange(state: Boolean) {}
    override fun getTerminalCursorStyle(): Int? = null

    // ==========================================
    // TerminalViewClient Implementation
    // ==========================================
    override fun onScale(scale: Float): Float = 1.0f

    override fun onSingleTapUp(e: MotionEvent?) {
        attachedView?.let { view ->
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
            val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun shouldBackButtonBeMappedToEscape(): Boolean = false
    override fun shouldEnforceCharBasedInput(): Boolean = true
    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
    override fun isTerminalViewSelected(): Boolean = true
    override fun copyModeChanged(copyMode: Boolean) {}

    override fun onKeyDown(keyCode: Int, e: KeyEvent?, session: TerminalSession?): Boolean = false
    override fun onKeyUp(keyCode: Int, e: KeyEvent?): Boolean = false
    override fun onLongPress(event: MotionEvent?): Boolean = false

    override fun readControlKey(): Boolean {
        val active = isCtrlActive
        if (active) {
            isCtrlActive = false
        }
        return active
    }

    override fun readAltKey(): Boolean = false
    override fun readShiftKey(): Boolean = false
    override fun readFnKey(): Boolean = false

    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession?): Boolean = false
    override fun onEmulatorSet() {}

    // Logging methods
    override fun logError(tag: String?, message: String?) { Log.e(tag ?: "Termux", message ?: "") }
    override fun logWarn(tag: String?, message: String?) { Log.w(tag ?: "Termux", message ?: "") }
    override fun logInfo(tag: String?, message: String?) { Log.i(tag ?: "Termux", message ?: "") }
    override fun logDebug(tag: String?, message: String?) { Log.d(tag ?: "Termux", message ?: "") }
    override fun logVerbose(tag: String?, message: String?) { Log.v(tag ?: "Termux", message ?: "") }
    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) { Log.e(tag ?: "Termux", message, e) }
    override fun logStackTrace(tag: String?, e: Exception?) { Log.e(tag ?: "Termux", "Exception", e) }
}
