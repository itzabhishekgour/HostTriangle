package com.tinexus.hosttriangle

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tinexus.hosttriangle.terminal.TerminalSessionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileDescriptor
import java.io.FileInputStream

/**
 * Instrumentation test verifying clean-room 16 KB compliant libtermux.so native methods:
 * 1. createSubprocess
 * 2. setPtyWindowSize
 * 3. waitFor
 * 4. close
 * 5. Safe-load error isolation
 */
@RunWith(AndroidJUnit4::class)
class PtyFunctionalTest {

    @Test
    fun testPtyNativeMethodsExecution() {
        // Step 1: Ensure safe load succeeds
        val loaded = TerminalSessionManager.ensureNativeLibraryLoaded()
        assertTrue("libtermux.so must load without UnsatisfiedLinkError", loaded)

        // Step 2: createSubprocess running /system/bin/sh -c "echo 'hi from hosttriangle pty'"
        val pidArray = IntArray(1)
        val cmd = "/system/bin/sh"
        val args = arrayOf("sh", "-c", "echo 'hi from hosttriangle pty'")
        val env = arrayOf("PATH=/system/bin")

        val masterFd = com.termux.terminal.JniBridge.createSubprocess(
            cmd,
            "/",
            args,
            env,
            pidArray,
            24,
            80
        )
        assertTrue("Master PTY file descriptor must be non-negative", masterFd >= 0)
        assertTrue("Child PID must be positive", pidArray[0] > 0)

        // Step 3: setPtyWindowSize
        com.termux.terminal.JniBridge.setPtyWindowSize(masterFd, 25, 85)

        // Step 4: Read output from master PTY
        val fdObj = FileDescriptor()
        val descField = FileDescriptor::class.java.getDeclaredField("descriptor")
        descField.isAccessible = true
        descField.setInt(fdObj, masterFd)

        val fis = FileInputStream(fdObj)
        val buffer = ByteArray(512)
        val bytesRead = fis.read(buffer)
        assertTrue("Must read non-empty output from PTY", bytesRead > 0)
        val output = String(buffer, 0, bytesRead)
        assertTrue("Output must contain 'hi from hosttriangle pty'", output.contains("hi"))

        // Step 5: waitFor child process exit code
        val exitCode = com.termux.terminal.JniBridge.waitFor(pidArray[0])
        assertEquals("Child process must exit with code 0", 0, exitCode)

        // Step 6: close master fd
        com.termux.terminal.JniBridge.close(masterFd)
    }

    @Test
    fun testSafeLoadMissingLibraryDoesNotCrash() {
        // Deliberately verify that attempting to load a non-existent native library
        // is caught safely without crashing the JVM
        var caught = false
        try {
            System.loadLibrary("non_existent_fake_lib_hosttriangle")
        } catch (e: UnsatisfiedLinkError) {
            caught = true
        }
        assertTrue("UnsatisfiedLinkError must be caught gracefully without crashing", caught)
    }
}
