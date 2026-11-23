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

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * BackgroundOptimizationHelper
 *
 * Manages device persistence, Doze mode exemptions, and OEM custom task killer bypasses
 * (Xiaomi MIUI/HyperOS, Oppo ColorOS, Vivo FuntouchOS, Samsung OneUI, etc.) so HostTriangle
 * backends run 24/7 with the screen turned off and unplugged.
 */
object BackgroundOptimizationHelper {

    /**
     * Checks if HostTriangle is currently exempt from Android Doze battery optimizations.
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Requests the native Android system exemption dialog for Battery Optimizations.
     * When granted, Android will never throttle CPU or cut networking when the screen is dark.
     */
    fun requestIgnoreBatteryOptimization(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to the battery optimization overview settings screen
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open battery settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Checks if notification permission is granted (critical for foreground status on API 33+).
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Identifies the device manufacturer brand for customized OEM autostart guidance.
     */
    fun getOemBrand(): String {
        val mfg = Build.MANUFACTURER.lowercase()
        return when {
            mfg.contains("xiaomi") || mfg.contains("redmi") || mfg.contains("poco") -> "Xiaomi (MIUI/HyperOS)"
            mfg.contains("samsung") -> "Samsung (OneUI)"
            mfg.contains("oppo") || mfg.contains("realme") || mfg.contains("oneplus") -> "Oppo / Realme (ColorOS)"
            mfg.contains("vivo") || mfg.contains("iqoo") -> "Vivo / iQOO (FuntouchOS)"
            mfg.contains("huawei") || mfg.contains("honor") -> "Huawei / Honor"
            else -> Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Opens manufacturer-specific autostart or background run permissions.
     * OEM task-killers (MIUI, ColorOS, etc.) aggressively kill background services
     * unless explicitly placed on the OEM autostart whitelist.
     */
    fun openOemAutostartSettings(context: Context): Boolean {
        val mfg = Build.MANUFACTURER.lowercase()

        val intentList = mutableListOf<Intent>()

        when {
            // Xiaomi / Redmi / POCO
            mfg.contains("xiaomi") || mfg.contains("redmi") || mfg.contains("poco") -> {
                intentList.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
                intentList.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")))
            }

            // Oppo / Realme
            mfg.contains("oppo") || mfg.contains("realme") -> {
                intentList.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")))
                intentList.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")))
                intentList.add(Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")))
            }

            // Vivo / iQOO
            mfg.contains("vivo") || mfg.contains("iqoo") -> {
                intentList.add(Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")))
                intentList.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")))
                intentList.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.MainGuideActivity")))
            }

            // Samsung
            mfg.contains("samsung") -> {
                intentList.add(Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")))
                intentList.add(Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")))
            }

            // Huawei / Honor
            mfg.contains("huawei") || mfg.contains("honor") -> {
                intentList.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")))
                intentList.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")))
            }

            // OnePlus
            mfg.contains("oneplus") -> {
                intentList.add(Intent().setComponent(ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")))
            }
        }

        // Generic fallback to Android App Details Settings
        intentList.add(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        )

        for (intent in intentList) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
                // Try next intent in chain
            }
        }

        return false
    }
}
