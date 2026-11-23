package com.termux.terminal

/**
 * Package-scoped bridge providing access to package-private com.termux.terminal.JNI
 * methods for HostTriangle functional verification tests and session runner.
 */
object JniBridge {

    fun createSubprocess(
        cmd: String,
        cwd: String,
        args: Array<String>?,
        envVars: Array<String>?,
        processIdArray: IntArray,
        rows: Int,
        cols: Int
    ): Int {
        return JNI.createSubprocess(cmd, cwd, args, envVars, processIdArray, rows, cols)
    }

    fun setPtyWindowSize(fd: Int, rows: Int, cols: Int) {
        JNI.setPtyWindowSize(fd, rows, cols)
    }

    fun waitFor(pid: Int): Int {
        return JNI.waitFor(pid)
    }

    fun close(fd: Int) {
        JNI.close(fd)
    }
}
