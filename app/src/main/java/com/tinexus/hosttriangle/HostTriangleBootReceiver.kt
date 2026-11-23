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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * HostTriangleBootReceiver
 *
 * High-Availability crash and reboot persistent system receiver.
 * Intercepts BOOT_COMPLETED signals, verifies whether the server engine was
 * actively running or configured for automated recovery prior to the power cycle,
 * and restarts HostTriangleServerService in the foreground.
 */
class HostTriangleBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "HostTriangleBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i(TAG, "System broadcast received: $action")

        val isBootAction = action == Intent.ACTION_BOOT_COMPLETED ||
                action == "android.intent.action.QUICKBOOT_POWERON" ||
                action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && action == Intent.ACTION_LOCKED_BOOT_COMPLETED)

        if (!isBootAction) {
            Log.w(TAG, "Ignored unsupported broadcast action: $action")
            return
        }

        val pendingResult = goAsync()

        try {
            val prefs = context.getSharedPreferences(
                HostTriangleServerService.PREFS_NAME,
                Context.MODE_PRIVATE
            )

            val wasRunningBeforeShutdown = prefs.getBoolean(
                HostTriangleServerService.KEY_SERVER_RUNNING_STATE,
                false
            )
            val isAutoStartEnabled = prefs.getBoolean(
                HostTriangleServerService.KEY_SERVER_AUTO_START,
                true // Default auto-start enabled
            )
            val configuredPort = prefs.getInt(
                HostTriangleServerService.KEY_CONFIG_PORT,
                HostTriangleServerService.DEFAULT_PORT
            )

            Log.i(
                TAG,
                "State Evaluation: wasRunning=$wasRunningBeforeShutdown, isAutoStartEnabled=$isAutoStartEnabled, targetPort=$configuredPort"
            )

            if (wasRunningBeforeShutdown || isAutoStartEnabled) {
                Log.i(TAG, "Starting foreground service after boot.")

                val serviceIntent = Intent(context, HostTriangleServerService::class.java).apply {
                    this.action = HostTriangleServerService.ACTION_START_SERVER
                    putExtra(HostTriangleServerService.EXTRA_PORT, configuredPort)
                }

                ContextCompat.startForegroundService(context, serviceIntent)
                Log.i(TAG, "HostTriangleServerService started successfully.")
            } else {
                Log.i(TAG, "Server was inactive prior to boot. Skipping service startup.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service in boot receiver: ${e.message}", e)
        } finally {
            pendingResult.finish()
        }
    }
}
