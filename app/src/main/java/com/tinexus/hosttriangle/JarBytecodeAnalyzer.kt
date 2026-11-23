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

import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * JarBytecodeAnalyzer
 *
 * Inspects uploaded pre-compiled JARs and detects the MAX major version of actual
 * application classes (e.g., inside BOOT-INF/classes/ or root packages), strictly
 * ignoring Spring Boot loader / launcher classes which are compiled targeting
 * older Java baselines.
 */
object JarBytecodeAnalyzer {

    data class AnalysisResult(
        val isJar: Boolean,
        val isSpringBoot: Boolean,
        val maxMajorVersion: Int,
        val requiredJavaVersion: Int,
        val scannedClassesCount: Int
    )

    fun analyze(jarFile: File): AnalysisResult {
        if (!jarFile.exists() || !jarFile.name.endsWith(".jar", ignoreCase = true)) {
            return AnalysisResult(isJar = false, isSpringBoot = false, maxMajorVersion = 0, requiredJavaVersion = 0, scannedClassesCount = 0)
        }

        var zipFile: ZipFile? = null
        try {
            zipFile = ZipFile(jarFile)
            val entries = zipFile.entries()

            val springBootAppClasses = mutableListOf<ZipEntry>()
            val rootAppClasses = mutableListOf<ZipEntry>()
            var isSpringBoot = false

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name

                if (name.startsWith("BOOT-INF/classes/") && name.endsWith(".class")) {
                    isSpringBoot = true
                    springBootAppClasses.add(entry)
                } else if (name.endsWith(".class")) {
                    // Ignore Spring Boot loaders, shaded nested jars, and multi-release version dirs
                    if (!name.startsWith("org/springframework/boot/loader/") &&
                        !name.startsWith("BOOT-INF/lib/") &&
                        !name.startsWith("META-INF/versions/")
                    ) {
                        rootAppClasses.add(entry)
                    }
                }
            }

            // Strictly target application classes: if Spring Boot, evaluate BOOT-INF/classes/
            val targetClasses = if (isSpringBoot && springBootAppClasses.isNotEmpty()) {
                springBootAppClasses
            } else {
                rootAppClasses
            }

            var maxMajor = 0
            var scannedCount = 0
            val buffer = ByteArray(8)

            for (entry in targetClasses) {
                zipFile.getInputStream(entry).use { stream ->
                    val read = readFully(stream, buffer)
                    if (read == 8) {
                        // Check CAFEBABE magic
                        if (buffer[0] == 0xCA.toByte() && buffer[1] == 0xFE.toByte() &&
                            buffer[2] == 0xBA.toByte() && buffer[3] == 0xBE.toByte()
                        ) {
                            val major = ((buffer[6].toInt() and 0xFF) shl 8) or (buffer[7].toInt() and 0xFF)
                            if (major > maxMajor) {
                                maxMajor = major
                            }
                            scannedCount++
                        }
                    }
                }
            }

            val javaVer = if (maxMajor >= 45) {
                maxMajor - 44 // 52 = Java 8, 55 = Java 11, 61 = Java 17, 65 = Java 21, 69 = Java 25
            } else {
                0
            }

            return AnalysisResult(
                isJar = true,
                isSpringBoot = isSpringBoot,
                maxMajorVersion = maxMajor,
                requiredJavaVersion = javaVer,
                scannedClassesCount = scannedCount
            )
        } catch (e: Exception) {
            return AnalysisResult(isJar = true, isSpringBoot = false, maxMajorVersion = 0, requiredJavaVersion = 0, scannedClassesCount = 0)
        } finally {
            try { zipFile?.close() } catch (_: Exception) {}
        }
    }

    private fun readFully(stream: InputStream, buffer: ByteArray): Int {
        var totalRead = 0
        while (totalRead < buffer.size) {
            val read = stream.read(buffer, totalRead, buffer.size - totalRead)
            if (read == -1) break
            totalRead += read
        }
        return totalRead
    }
}
