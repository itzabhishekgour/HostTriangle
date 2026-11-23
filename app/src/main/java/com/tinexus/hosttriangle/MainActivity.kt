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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.tinexus.hosttriangle.cluster.ClusterFleetManager
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.view.TerminalView
import com.tinexus.hosttriangle.terminal.TerminalSessionManager
import androidx.core.content.ContextCompat
import com.tinexus.hosttriangle.ui.theme.HostTriangleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale

// -------------------------------------------------------------------------------------------------
// UI DESIGN SYSTEM COLOR TOKENS
// -------------------------------------------------------------------------------------------------
data class PocketPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceSecondary: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentEmerald: Color,
    val accentEmeraldLight: Color,
    val accentIndigo: Color,
    val accentIndigoLight: Color,
    val accentCyan: Color,
    val warning: Color,
    val warningLight: Color,
    val alert: Color,
    val alertLight: Color,
    val terminalBg: Color,
    val terminalText: Color,
    val terminalBorder: Color
)

// Light Mode Palette
val LightPalette = PocketPalette(
    isDark = false,
    background = Color(0xFFF8FAFC), // Slate 50
    surface = Color(0xFFFFFFFF),    // Pure white
    surfaceSecondary = Color(0xFFF1F5F9), // Slate 100
    border = Color(0xFFE2E8F0),     // Hairline slate 200
    textPrimary = Color(0xFF0F172A), // Deep Slate 900
    textSecondary = Color(0xFF475569), // Slate 600
    textMuted = Color(0xFF94A3B8),   // Slate 400
    accentEmerald = Color(0xFF059669), // Emerald 600
    accentEmeraldLight = Color(0xFFECFDF5), // Emerald 50
    accentIndigo = Color(0xFF4F46E5), // Indigo 600
    accentIndigoLight = Color(0xFFEEF2FF), // Indigo 50
    accentCyan = Color(0xFF0284C7), // Sky 600
    warning = Color(0xFFD97706),   // Amber 600
    warningLight = Color(0xFFFEF3C7),
    alert = Color(0xFFDC2626),     // Rose 600
    alertLight = Color(0xFFFEF2F2),
    terminalBg = Color(0xFF0F172A), // Midnight Slate
    terminalText = Color(0xFFF1F5F9),
    terminalBorder = Color(0xFF1E293B)
)

// Dark Mode Palette
val DarkPalette = PocketPalette(
    isDark = true,
    background = Color(0xFF0B0F19), // Midnight
    surface = Color(0xFF111827),    // Slate 900
    surfaceSecondary = Color(0xFF1E293B), // Slate 800
    border = Color(0xFF1F2937),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    accentEmerald = Color(0xFF10B981),
    accentEmeraldLight = Color(0x2610B981),
    accentIndigo = Color(0xFF6366F1),
    accentIndigoLight = Color(0x266366F1),
    accentCyan = Color(0xFF38BDF8),
    warning = Color(0xFFFBBF24),
    warningLight = Color(0x26FBBF24),
    alert = Color(0xFFF87171),
    alertLight = Color(0x26F87171),
    terminalBg = Color(0xFF050811),
    terminalText = Color(0xFFF1F5F9),
    terminalBorder = Color(0xFF1E293B)
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

// -------------------------------------------------------------------------------------------------
// ACTIVITY ENTRY POINT
// -------------------------------------------------------------------------------------------------
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure dataDir is 0751 so `adb shell run-as com.tinexus.hosttriangle` works for debugging
        try {
            android.system.Os.chmod(applicationInfo.dataDir, 0b111_101_001) // 0751
        } catch (_: Exception) {}
        enableEdgeToEdge()

        setContent {
            // Default to light theme
            var isDarkMode by remember { mutableStateOf(false) }
            val palette = if (isDarkMode) DarkPalette else LightPalette

            CompositionLocalProvider(LocalPalette provides palette) {
                HostTriangleTheme(darkTheme = isDarkMode) {
                    HostTriangleMainContainer(
                        isDarkMode = isDarkMode,
                        onToggleTheme = { isDarkMode = !isDarkMode }
                    )
                }
            }
        }
    }
}

sealed class NavScreen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : NavScreen("dashboard", "Dashboard", Icons.Default.Home)
    object Deployment : NavScreen("deployment", "Deployment", Icons.Default.Build)
    object LiveLogs : NavScreen("live_logs", "Terminal", Icons.AutoMirrored.Filled.List)
    object Clusters : NavScreen("clusters", "Fleet Map", Icons.Default.Share)
}

// -------------------------------------------------------------------------------------------------
// MAIN CONTAINER SCAFFOLD & TOP APP BAR
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostTriangleMainContainer(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalPalette.current
    var selectedScreen by remember { mutableStateOf<NavScreen>(NavScreen.Dashboard) }

    val serverState by HostTriangleServerService.serverState.collectAsState()
    val isRunning = serverState == HostTriangleServerService.ServerState.RUNNING

    val isEngineInstalledInitial = remember { EngineManager.getEngine(context).isInstalled() }
    var showSetupWizard by remember { mutableStateOf(!isEngineInstalledInitial) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notification permission required for background server.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        // Auto-prompt battery optimization exemption once on startup if not already granted
        if (!BackgroundOptimizationHelper.isBatteryOptimizationIgnored(context)) {
            val prefs = context.getSharedPreferences(HostTriangleServerService.PREFS_NAME, Context.MODE_PRIVATE)
            val hasPrompted = prefs.getBoolean("has_prompted_battery_exemption", false)
            if (!hasPrompted) {
                prefs.edit().putBoolean("has_prompted_battery_exemption", true).apply()
                delay(1200L)
                BackgroundOptimizationHelper.requestIgnoreBatteryOptimization(context)
            }
        }
    }

    if (showSetupWizard) {
        LinuxSubsystemSetupScreen(
            onCompleted = {
                showSetupWizard = false
                RuntimeManager.refreshStatuses(context)
            }
        )
    } else {
        Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0B0F19),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(2.dp)) {
                                    Image(
                                        painter = painterResource(id = R.drawable.hosttriangle_icon),
                                        contentDescription = "HostTriangle Emblem",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "HostTriangle",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Edge Compute Node",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textMuted
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Active Status Pill
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isRunning) colors.accentEmeraldLight else colors.surfaceSecondary,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isRunning) colors.accentEmerald else colors.textMuted)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isRunning) "Online" else "Idle",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isRunning) colors.accentEmerald else colors.textSecondary
                                    )
                                }
                            }

                            // Theme Toggle Button
                            IconButton(
                                onClick = onToggleTheme,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Open Source Licenses / About Button
                            IconButton(
                                onClick = { showAboutDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Open Source Notices",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface
                )
            )
        },
        bottomBar = {
            val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
            if (!isKeyboardOpen) {
                NavigationBar(
                    containerColor = colors.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(width = (0.5).dp, color = colors.border)
                ) {
                    val screens = listOf(NavScreen.Dashboard, NavScreen.Deployment, NavScreen.LiveLogs, NavScreen.Clusters)
                    screens.forEach { screen ->
                        val isSelected = selectedScreen == screen
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = {
                                Text(
                                    screen.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            selected = isSelected,
                            onClick = { selectedScreen = screen },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.accentEmerald,
                                selectedTextColor = colors.accentEmerald,
                                unselectedIconColor = colors.textMuted,
                                unselectedTextColor = colors.textMuted,
                                indicatorColor = colors.accentEmeraldLight
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val bottomPad = if (isKeyboardOpen) 0.dp else innerPadding.calculateBottomPadding()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = bottomPad
                )
        ) {
            when (selectedScreen) {
                is NavScreen.Dashboard -> DashboardScreen()
                is NavScreen.Deployment -> DeploymentScreen(onOpenSetupWizard = { showSetupWizard = true })
                is NavScreen.LiveLogs -> LiveLogsScreen(onOpenSetupWizard = { showSetupWizard = true })
                is NavScreen.Clusters -> ClustersFleetScreen()
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = colors.accentEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HostTriangle v0.1.0-beta.1",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "100% Free & Open Source Software licensed under GNU General Public License v3.0 (GPL-3.0-only).",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Third-Party Open Source Components & Notices:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.surfaceSecondary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("• PRoot & PRoot-Loader (GPL-2.0-or-later): github.com/proot-me/proot", fontSize = 10.sp, color = colors.textMuted)
                            Text("• talloc (LGPL-3.0-or-later): talloc.samba.org", fontSize = 10.sp, color = colors.textMuted)
                            Text("• cloudflared (Apache 2.0): github.com/cloudflare/cloudflared", fontSize = 10.sp, color = colors.textMuted)
                            Text("• Termux Terminal View/Emulator (Apache 2.0 / GPL-3.0-only)", fontSize = 10.sp, color = colors.textMuted)
                            Text("• Clean-Room 16KB PTY Engine (GPL-3.0-only): HostTriangle", fontSize = 10.sp, color = colors.textMuted)
                        }
                    }
                    Text(
                        text = "Full licenses and complete copyright notices are available in the repository root (LICENSE and THIRD_PARTY_NOTICES.md).",
                        fontSize = 10.sp,
                        color = colors.textMuted
                    )
                    Text(
                        text = "Intended for students, hobbyists, indie developers, and small personal or college projects. Not intended for mission-critical systems. Provided AS-IS without warranty (GPL-3.0). The user is solely responsible for hosted services, executed commands, and compliance with Cloudflare terms, network provider rules, and applicable law.",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = colors.accentEmerald, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.surface
        )
    }
    }
}

// -------------------------------------------------------------------------------------------------
// SCREEN 1: DASHBOARD
// -------------------------------------------------------------------------------------------------
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val colors = LocalPalette.current

    val serverState by HostTriangleServerService.serverState.collectAsState()
    val telemetry by HostTriangleServerService.telemetry.collectAsState()
    val publicUrl by HostTriangleServerService.publicUrl.collectAsState()
    val activePort by HostTriangleServerService.activePort.collectAsState()
    val cfStatus by CloudflareTunnelManager.status.collectAsState()
    val decodedTunnel = ClusterFleetManager.decodedTunnelInfo.collectAsState().value

    val isRunning = serverState == HostTriangleServerService.ServerState.RUNNING
    val isStarting = serverState == HostTriangleServerService.ServerState.STARTING
    val isThrottled = serverState == HostTriangleServerService.ServerState.THROTTLED

    val prefs = remember { context.getSharedPreferences(HostTriangleServerService.PREFS_NAME, Context.MODE_PRIVATE) }
    var tunnelToken by remember { mutableStateOf(prefs.getString(HostTriangleServerService.KEY_TUNNEL_TOKEN, "") ?: "") }

    val workspaceDir = remember { File(context.filesDir, "workspace") }
    var stagedFiles by remember { mutableStateOf(workspaceDir.listFiles()?.filter { it.isFile } ?: emptyList()) }

    var realLocalIp by remember { mutableStateOf("127.0.0.1") }
    LaunchedEffect(Unit) {
        realLocalIp = getRealLocalIpAddress()
        CloudflareTunnelManager.refreshStatus(context)
        stagedFiles = workspaceDir.listFiles()?.filter { it.isFile } ?: emptyList()
        tunnelToken = prefs.getString(HostTriangleServerService.KEY_TUNNEL_TOKEN, "") ?: ""
        if (tunnelToken.isNotBlank()) {
            ClusterFleetManager.decodeTunnelToken(tunnelToken)
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Real-Time Hardware Telemetry Polling (Runs continuously so RAM, Battery & CPU stay live)
    LaunchedEffect(Unit) {
        while (true) {
            HostTriangleServerService.sampleTelemetry(context)
            delay(2000L)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Android 12+ (API 31+) Phantom Process Killer Warning Banner
        if (Build.VERSION.SDK_INT >= 31) {
            item {
                var dismissed by remember { mutableStateOf(false) }
                if (!dismissed) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.warning.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                        colors = CardDefaults.cardColors(containerColor = colors.warningLight),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = colors.warning,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "ANDROID 12+ PHANTOM PROCESS WARNING",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.warning
                                    )
                                }
                                IconButton(
                                    onClick = { dismissed = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Dismiss",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Android 12+ may silently terminate background Linux/JVM child processes. To guarantee 24/7 background uptime, run this one-time ADB command or toggle 'Disable child process restrictions' in Developer Options:",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val adbCommand = "adb shell settings put global settings_enable_monitor_phantom_procs false"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.terminalBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", adbCommand))
                                        Toast.makeText(context, "ADB command copied!", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = adbCommand,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.terminalText,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = colors.accentCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 24/7 Background Reliability & Doze Exemption Card
        item {
            var isBatteryIgnored by remember {
                mutableStateOf(BackgroundOptimizationHelper.isBatteryOptimizationIgnored(context))
            }
            val oemBrand = remember { BackgroundOptimizationHelper.getOemBrand() }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isBatteryIgnored) colors.accentEmerald.copy(alpha = 0.4f) else colors.warning.copy(alpha = 0.5f),
                        RoundedCornerShape(18.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isBatteryIgnored) colors.accentEmerald else colors.warning)
                            )
                            Text(
                                text = "24/7 BACKGROUND RELIABILITY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBatteryIgnored) colors.accentEmerald else colors.warning,
                                letterSpacing = (0.8).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isBatteryIgnored) colors.accentEmeraldLight else colors.warningLight
                        ) {
                            Text(
                                text = if (isBatteryIgnored) "Unrestricted" else "Optimized",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBatteryIgnored) colors.accentEmerald else colors.warning,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Text(
                        text = if (isBatteryIgnored) {
                            "Battery optimization is exempt. CPU WakeLock, WifiLock, and Cloudflare Tunnel watchdog will prevent sleep when screen is off and unplugged."
                        } else {
                            "Android Doze mode is active. When your screen turns off unplugged, Android will throttle CPU and sleep network connections unless set to Unrestricted."
                        },
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 17.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!isBatteryIgnored) {
                            Button(
                                onClick = {
                                    BackgroundOptimizationHelper.requestIgnoreBatteryOptimization(context)
                                    isBatteryIgnored = BackgroundOptimizationHelper.isBatteryOptimizationIgnored(context)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accentEmerald),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Allow Background",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.surfaceSecondary,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    BackgroundOptimizationHelper.openOemAutostartSettings(context)
                                    Toast.makeText(context, "Enable Autostart & lock HostTriangle in background", Toast.LENGTH_LONG).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$oemBrand Settings",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Hero Server Execution Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "NODE EXECUTION STATUS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                letterSpacing = (1.1).sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val (statusText, statusBg, statusColor) = when (serverState) {
                                HostTriangleServerService.ServerState.RUNNING -> Triple("RUNNING", colors.accentEmeraldLight, colors.accentEmerald)
                                HostTriangleServerService.ServerState.STARTING -> Triple("STARTING...", colors.accentIndigoLight, colors.accentIndigo)
                                HostTriangleServerService.ServerState.THROTTLED -> Triple("THROTTLED", colors.warningLight, colors.warning)
                                HostTriangleServerService.ServerState.STOPPED -> Triple("STOPPED", colors.surfaceSecondary, colors.textSecondary)
                                HostTriangleServerService.ServerState.ERROR -> Triple("ERROR", colors.alertLight, colors.alert)
                                HostTriangleServerService.ServerState.MISSING_RUNTIME -> Triple("NEEDS SETUP", colors.warningLight, colors.warning)
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = statusBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                            .alpha(if (isRunning || isStarting) pulseAlpha else 1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = statusText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val stagedJar = stagedFiles.firstOrNull { it.name.endsWith(".jar", true) }
                            val stagedJs = stagedFiles.firstOrNull { it.name.endsWith(".js", true) }
                            val stagedPy = stagedFiles.firstOrNull { it.name.endsWith(".py", true) }
                            val stagedSummary = when {
                                stagedJar != null -> "Spring Boot: ${stagedJar.name}"
                                stagedJs != null -> "Node.js: ${stagedJs.name}"
                                stagedPy != null -> "Python: ${stagedPy.name}"
                                stagedFiles.isNotEmpty() -> "Staged: ${stagedFiles.first().name}"
                                else -> "No artifact staged"
                            }

                            Text(
                                text = stagedSummary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Local Ingress Port: $activePort",
                                fontSize = 10.sp,
                                color = colors.textMuted
                            )
                        }

                        // Start / Stop Service Action Button
                        Button(
                            onClick = {
                                if (isRunning || isStarting) {
                                    val stopIntent = Intent(context, HostTriangleServerService::class.java).apply {
                                        action = HostTriangleServerService.ACTION_STOP_SERVER
                                    }
                                    context.startService(stopIntent)
                                } else {
                                    val workspaceDir = File(context.filesDir, "workspace")
                                    val stagedJar = workspaceDir.listFiles()?.firstOrNull { it.name.endsWith(".jar", ignoreCase = true) }
                                    if (stagedJar != null) {
                                        val analysis = JarBytecodeAnalyzer.analyze(stagedJar)
                                        val installedVer = RuntimeManager.installedJavaVersion.value
                                        if (installedVer == 0) {
                                            Toast.makeText(context, "Cannot launch: Java JRE is not installed. Please install Java in Deployment tab.", Toast.LENGTH_LONG).show()
                                            return@Button
                                        } else if (analysis.requiredJavaVersion > installedVer) {
                                            Toast.makeText(context, "Cannot launch: JAR requires Java ${analysis.requiredJavaVersion}, but installed JRE is Java $installedVer.", Toast.LENGTH_LONG).show()
                                            return@Button
                                        }
                                    }

                                    val startIntent = Intent(context, HostTriangleServerService::class.java).apply {
                                        action = HostTriangleServerService.ACTION_START_SERVER
                                        putExtra(HostTriangleServerService.EXTRA_PORT, activePort)
                                    }
                                    ContextCompat.startForegroundService(context, startIntent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) colors.alert else colors.accentEmerald,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Clear else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRunning) "STOP" else "LAUNCH",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (isThrottled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.alertLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "THERMAL BREACH: Device paused automatically (> 43°C) to protect battery health.",
                                color = colors.alert,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Ingress & Network Routes Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    val isTunnelConnected = isRunning && cfStatus is CloudflareTunnelManager.TunnelStatus.Connected
                    val isTunnelConnecting = isRunning && cfStatus is CloudflareTunnelManager.TunnelStatus.Connecting
                    val isTunnelError = isRunning && cfStatus is CloudflareTunnelManager.TunnelStatus.Error
                    val ingressBadgeText = when {
                        isTunnelConnected -> "GLOBAL TUNNEL ACTIVE"
                        isTunnelConnecting -> "CONNECTING"
                        isRunning -> "LAN ACTIVE"
                        else -> "OFFLINE"
                    }
                    val ingressBadgeBg = when {
                        isTunnelConnected -> colors.accentEmeraldLight
                        isTunnelConnecting -> colors.accentCyan.copy(alpha = 0.12f)
                        isRunning -> colors.accentCyan.copy(alpha = 0.12f)
                        else -> colors.surfaceSecondary
                    }
                    val ingressBadgeColor = when {
                        isTunnelConnected -> colors.accentEmerald
                        isTunnelConnecting -> colors.accentCyan
                        isRunning -> colors.accentCyan
                        else -> colors.textMuted
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NETWORK INGRESS ROUTES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted,
                            letterSpacing = (1.1).sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ingressBadgeBg
                        ) {
                            Text(
                                text = ingressBadgeText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ingressBadgeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceSecondary,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Worldwide Cloudflare Zero Trust Tunnel Route
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cloud,
                                            contentDescription = null,
                                            tint = Color(0xFFF38020),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "CLOUDFLARE ZERO TRUST INGRESS",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.accentIndigo,
                                            letterSpacing = (0.8).sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    val cfHeadline = when {
                                        isTunnelConnected -> "Anycast Route Active [Global]"
                                        isTunnelConnecting -> "Connecting to Edge..."
                                        isTunnelError -> "Tunnel Connection Failed"
                                        tunnelToken.isNotBlank() && decodedTunnel?.isValid == true -> "Tunnel Configured [Ready]"
                                        else -> "Not Configured"
                                    }
                                    val cfHeadlineColor = when {
                                        isTunnelConnected -> colors.accentEmerald
                                        isTunnelConnecting -> colors.accentCyan
                                        isTunnelError -> colors.alert
                                        tunnelToken.isNotBlank() && decodedTunnel?.isValid == true -> colors.accentIndigo
                                        else -> colors.textMuted
                                    }
                                    Text(
                                        text = cfHeadline,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = cfHeadlineColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val cfSub = when {
                                        isTunnelConnected ->
                                            "Traffic from your custom Cloudflare domain routes directly to this phone on port $activePort."
                                        tunnelToken.isNotBlank() && decodedTunnel?.isValid == true ->
                                            "Tunnel ID: ${decodedTunnel?.tunnelId?.take(16)}... (Connects automatically on Launch)"
                                        else ->
                                            "No tunnel token set. Paste your Cloudflare Zero Trust token in Deployment tab for public access."
                                    }
                                    Text(
                                        text = cfSub,
                                        fontSize = 9.sp,
                                        color = colors.textSecondary,
                                        lineHeight = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                val (cfPillText, cfPillBg, cfPillColor) = when {
                                    isTunnelConnected ->
                                        Triple("CONNECTED", colors.accentEmeraldLight, colors.accentEmerald)
                                    isTunnelConnecting ->
                                        Triple("CONNECTING", colors.accentCyan.copy(alpha = 0.12f), colors.accentCyan)
                                    isTunnelError ->
                                        Triple("FAILED", colors.alertLight, colors.alert)
                                    tunnelToken.isNotBlank() && decodedTunnel?.isValid == true ->
                                        Triple("STANDBY", colors.accentIndigoLight, colors.accentIndigo)
                                    else ->
                                        Triple("OFFLINE", colors.surfaceSecondary, colors.textMuted)
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = cfPillBg,
                                    border = BorderStroke(1.dp, cfPillColor.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = cfPillText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = cfPillColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Divider line
                            Surface(
                                modifier = Modifier.fillMaxWidth().height((0.5).dp),
                                color = colors.border
                            ) {}

                            // 2. Real Local Wi-Fi / LAN Endpoint
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Wifi,
                                            contentDescription = null,
                                            tint = colors.accentCyan,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "LOCAL WI-FI / LAN ROUTE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textMuted,
                                            letterSpacing = (0.8).sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "http://$realLocalIp:$activePort",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isRunning) colors.textPrimary else colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isRunning) "Direct high-speed LAN access for devices on the same Wi-Fi." else "Available locally once launched.",
                                        fontSize = 9.sp,
                                        color = colors.textSecondary,
                                        lineHeight = 13.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val localUrl = "http://$realLocalIp:$activePort"
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("HostTriangle Local URL", localUrl)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Local Wi-Fi URL copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Local URL",
                                            tint = colors.accentEmerald,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    if (isRunning) {
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://$realLocalIp:$activePort"))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Cannot open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OpenInBrowser,
                                                contentDescription = "Open Local URL",
                                                tint = colors.accentCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hardware Telemetry Gauges (Cleaned up layout, no wrapping bugs)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SYSTEM VITALS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted,
                            letterSpacing = (1.1).sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.accentIndigoLight
                        ) {
                            Text(
                                text = "Uptime: ${formatUptime(telemetry.uptimeSeconds)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.accentIndigo,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. CPU Core Thermal
                    val cpuTemp = telemetry.cpuTemperature
                    val thermalColor = when {
                        cpuTemp >= 43.0f -> colors.alert
                        cpuTemp >= 39.5f -> colors.warning
                        else -> colors.accentEmerald
                    }
                    MetricRow(
                        title = "CPU Core Temperature",
                        value = String.format(Locale.US, "%.1f °C", cpuTemp),
                        secondary = "Ceiling: 43.0 °C",
                        progress = (cpuTemp / 50.0f).coerceIn(0f, 1f),
                        accentColor = thermalColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. RAM Memory Allocation
                    val ramPct = telemetry.ramUsagePercent
                    val usedGb = String.format(Locale.US, "%.1f", telemetry.ramUsedMb / 1024f)
                    val totalGb = String.format(Locale.US, "%.1f", telemetry.ramTotalMb / 1024f)
                    MetricRow(
                        title = "RAM Memory Allocation",
                        value = "${ramPct.toInt()}%",
                        secondary = "$usedGb GB of $totalGb GB",
                        progress = (ramPct / 100f).coerceIn(0f, 1f),
                        accentColor = colors.accentIndigo
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Battery Capacity State
                    val batPct = telemetry.batteryCapacity
                    val batColor = when {
                        batPct < 15f -> colors.alert
                        batPct < 30f -> colors.warning
                        else -> colors.accentEmerald
                    }
                    val chargingStatus = if (telemetry.isCharging) "Charging" else "On Battery"
                    MetricRow(
                        title = "Battery Level",
                        value = "${batPct.toInt()}%",
                        secondary = chargingStatus,
                        progress = (batPct / 100f).coerceIn(0f, 1f),
                        accentColor = batColor
                    )
                }
            }
        }
    }
}

private fun formatUptime(seconds: Long): String {
    if (seconds <= 0) return "0s"
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}

@Composable
fun MetricRow(
    title: String,
    value: String,
    secondary: String,
    progress: Float,
    accentColor: Color
) {
    val colors = LocalPalette.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Text(
                    text = secondary,
                    fontSize = 10.sp,
                    color = colors.textMuted
                )
            }
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = accentColor,
            trackColor = colors.surfaceSecondary
        )
    }
}

// -------------------------------------------------------------------------------------------------
// SCREEN 2: DEPLOYMENT SCREEN
// -------------------------------------------------------------------------------------------------
@Composable
fun DeploymentScreen(onOpenSetupWizard: (() -> Unit)? = null) {
    val context = LocalContext.current
    val colors = LocalPalette.current

    val activePort by HostTriangleServerService.activePort.collectAsState()
    val cfStatus by CloudflareTunnelManager.status.collectAsState()

    var portInput by remember { mutableStateOf(activePort.toString()) }
    val prefs = remember { context.getSharedPreferences(HostTriangleServerService.PREFS_NAME, Context.MODE_PRIVATE) }
    var autoStartOnBoot by remember {
        mutableStateOf(prefs.getBoolean(HostTriangleServerService.KEY_SERVER_AUTO_START, true))
    }

    var selectedProtocol by remember {
        mutableStateOf(prefs.getString(HostTriangleServerService.KEY_TUNNEL_PROTOCOL, "Cloudflare Tunnel") ?: "Cloudflare Tunnel")
    }
    var customSubdomain by remember {
        mutableStateOf(prefs.getString(HostTriangleServerService.KEY_CUSTOM_SUBDOMAIN, "") ?: "")
    }
    var tunnelToken by remember {
        val stored = prefs.getString(HostTriangleServerService.KEY_TUNNEL_TOKEN, "") ?: ""
        // Purge legacy hardcoded demo token if present in device preferences
        val cleaned = if (stored.contains("eyJhIjoiNzU0MDUyMDY1NTQ3YTEwZWE5MjZjNjA4")) {
            prefs.edit().putString(HostTriangleServerService.KEY_TUNNEL_TOKEN, "").apply()
            ""
        } else {
            stored
        }
        mutableStateOf(cleaned)
    }
    var showAdvancedTunnel by remember { mutableStateOf(false) }
    var showAdvancedRuntime by remember { mutableStateOf(false) }
    var selectedJavaVersion by remember { mutableIntStateOf(21) }
    var isPurgingRuntimes by remember { mutableStateOf(false) }

    val workspaceDir = remember { File(context.filesDir, "workspace").apply { if (!exists()) mkdirs() } }
    var stagedFiles by remember { mutableStateOf(workspaceDir.listFiles()?.filter { it.isFile } ?: emptyList()) }

    var startCommandInput by remember {
        val saved = prefs.getString(HostTriangleServerService.KEY_CUSTOM_START_COMMAND, null)
        val defCmd = HostTriangleServerService.computeDefaultStartCommand(workspaceDir, activePort)
        mutableStateOf(saved ?: defCmd)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var fileName = "project-upload"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }

                val destFile = File(workspaceDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                stagedFiles = workspaceDir.listFiles()?.filter { it.isFile } ?: emptyList()
                if (prefs.getString(HostTriangleServerService.KEY_CUSTOM_START_COMMAND, null).isNullOrBlank()) {
                    val newDef = HostTriangleServerService.computeDefaultStartCommand(workspaceDir, portInput.toIntOrNull() ?: activePort)
                    startCommandInput = newDef
                }
                HostTriangleServerService.log(
                    HostTriangleServerService.LogLevel.INFO,
                    "Workspace",
                    "Uploaded $fileName (${destFile.length() / 1024} KB) to sandbox workspace."
                )
                Toast.makeText(context, "Loaded $fileName into workspace!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                HostTriangleServerService.log(
                    HostTriangleServerService.LogLevel.ERROR,
                    "Workspace",
                    "Upload error: ${e.message}"
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        RuntimeManager.refreshStatuses(context)
        CloudflareTunnelManager.refreshStatus(context)
        stagedFiles = workspaceDir.listFiles()?.filter { it.isFile } ?: emptyList()
        if (tunnelToken.isNotBlank()) {
            ClusterFleetManager.decodeTunnelToken(tunnelToken)
        }
    }

    val isEngineReady = remember { EngineManager.getEngine(context).isInstalled() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Unified Subsystem & Auto-Boot Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isEngineReady) colors.accentEmeraldLight else colors.warningLight,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isEngineReady) Icons.Default.CheckCircle else Icons.Default.Build,
                                        contentDescription = null,
                                        tint = if (isEngineReady) colors.accentEmerald else colors.warning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Debian Linux Subsystem",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isEngineReady) "Debian 12 arm64 • Active" else "Not Installed",
                                    fontSize = 11.sp,
                                    color = if (isEngineReady) colors.accentEmerald else colors.warning,
                                    maxLines = 1
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Auto-Boot",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (autoStartOnBoot) "Active" else "Off",
                                    fontSize = 9.sp,
                                    color = if (autoStartOnBoot) colors.accentEmerald else colors.textMuted,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = autoStartOnBoot,
                                onCheckedChange = { checked ->
                                    autoStartOnBoot = checked
                                    prefs.edit().putBoolean(HostTriangleServerService.KEY_SERVER_AUTO_START, checked).apply()
                                    Toast.makeText(context, "Auto-recovery on boot: $checked", Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.accentEmerald,
                                    uncheckedThumbColor = colors.textMuted,
                                    uncheckedTrackColor = colors.surfaceSecondary
                                )
                            )
                        }
                    }

                    if (onOpenSetupWizard != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.surfaceSecondary,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.clickable { onOpenSetupWizard() }
                            ) {
                                Text(
                                    text = if (isEngineReady) "Re-setup Subsystem" else "Install Subsystem",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.accentCyan,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Application Source & Workspace Artifacts Card
        item {
            val stagedJar = stagedFiles.firstOrNull { it.name.endsWith(".jar", ignoreCase = true) }
            val jarAnalysis = remember(stagedJar, stagedFiles.size) {
                if (stagedJar != null) JarBytecodeAnalyzer.analyze(stagedJar) else null
            }
            val installedJavaVer by RuntimeManager.installedJavaVersion.collectAsState()

            val nodeReq = remember(stagedFiles.size) {
                if (stagedFiles.any { it.name.endsWith(".js", true) || it.name == "package.json" }) {
                    RuntimeManager.detectNodeRequirement(workspaceDir)
                } else null
            }

            val pythonReq = remember(stagedFiles.size) {
                if (stagedFiles.any { it.name.endsWith(".py", true) || it.name == "runtime.txt" || it.name == "pyproject.toml" }) {
                    RuntimeManager.detectPythonRequirement(workspaceDir)
                } else null
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "APPLICATION SOURCE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                letterSpacing = (1.1).sp
                            )
                            Text(
                                text = "Project Artifacts & Workspace",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                stagedFiles = workspaceDir.listFiles()?.filter { it.isFile } ?: emptyList()
                                RuntimeManager.refreshStatuses(context)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Files",
                                tint = colors.accentEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Upload Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceSecondary,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { filePickerLauncher.launch("*/*") }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.accentEmeraldLight,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = "Upload",
                                        tint = colors.accentEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Upload Backend Artifact",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Supports Spring Boot (.jar), Node.js (.js), Python (.py), or .env",
                                    fontSize = 10.sp,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.accentEmerald
                            ) {
                                Text(
                                    text = "Select",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STAGED FILES (${stagedFiles.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted,
                            letterSpacing = (0.8).sp
                        )
                        Text(
                            text = "./workspace",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (stagedFiles.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = colors.surfaceSecondary.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, colors.border.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "Workspace is empty. Tap upload above to stage your backend files.",
                                fontSize = 11.sp,
                                color = colors.textMuted,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            stagedFiles.forEach { file ->
                                val (badgeText, badgeColor, badgeBg) = when {
                                    file.name.endsWith(".jar", true) -> Triple("SPRING BOOT", colors.accentEmerald, colors.accentEmeraldLight)
                                    file.name.endsWith(".js", true) -> Triple("NODE.JS", colors.accentCyan, colors.surfaceSecondary)
                                    file.name.endsWith(".py", true) -> Triple("PYTHON", colors.warning, colors.warningLight)
                                    file.name.contains("env", true) || file.name.endsWith(".properties", true) -> Triple("CONFIG", colors.accentIndigo, colors.accentIndigoLight)
                                    else -> Triple("FILE", colors.textMuted, colors.surfaceSecondary)
                                }

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.surfaceSecondary,
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                text = file.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = badgeBg
                                                ) {
                                                    Text(
                                                        text = badgeText,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = badgeColor,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${file.length() / 1024} KB",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = colors.textMuted
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                file.delete()
                                                stagedFiles = workspaceDir.listFiles()?.filter { it.isFile } ?: emptyList()
                                                if (prefs.getString(HostTriangleServerService.KEY_CUSTOM_START_COMMAND, null).isNullOrBlank()) {
                                                    val newDef = HostTriangleServerService.computeDefaultStartCommand(workspaceDir, portInput.toIntOrNull() ?: activePort)
                                                    startCommandInput = newDef
                                                }
                                                Toast.makeText(context, "Deleted ${file.name}", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = colors.alert,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // JAR Bytecode Auto-Detection & Runtime Compatibility Card
                    if (jarAnalysis != null && jarAnalysis.isJar) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val isCompatible = installedJavaVer >= jarAnalysis.requiredJavaVersion && installedJavaVer > 0
                        val isMissingRuntime = installedJavaVer == 0
                        val isTooOld = installedJavaVer in 1 until jarAnalysis.requiredJavaVersion

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isCompatible -> colors.accentEmeraldLight
                                isTooOld -> colors.alertLight
                                else -> colors.warningLight
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isCompatible -> colors.accentEmerald
                                    isTooOld -> colors.alert
                                    else -> colors.warning
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isCompatible) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = when {
                                                isCompatible -> colors.accentEmerald
                                                isTooOld -> colors.alert
                                                else -> colors.warning
                                            },
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (jarAnalysis.isSpringBoot) "Spring Boot Executable JAR" else "Standard Java JAR",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = colors.surface
                                    ) {
                                        Text(
                                            text = "Java ${jarAnalysis.requiredJavaVersion}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Target: Java ${jarAnalysis.requiredJavaVersion} (Classfile ${jarAnalysis.maxMajorVersion}.0) • ${jarAnalysis.scannedClassesCount} app classes analyzed",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textSecondary
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                when {
                                    isCompatible -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = colors.accentEmerald,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Bytecode Compatible: Verified with installed Java $installedJavaVer JRE runtime.",
                                                fontSize = 10.sp,
                                                color = colors.accentEmerald,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        if (jarAnalysis.requiredJavaVersion < installedJavaVer) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Note: Modern JVMs maintain opcode compatibility, but if your JAR relies on removed APIs (e.g. JAXB/CORBA in Java 11+), choose a dedicated JRE in Advanced Settings.",
                                                fontSize = 9.sp,
                                                color = colors.textMuted
                                            )
                                        }
                                    }
                                    isTooOld -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = colors.alert,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Incompatible Runtime: JAR requires Java ${jarAnalysis.requiredJavaVersion}, but installed JRE is Java $installedJavaVer. Launching will fail with UnsupportedClassVersionError.",
                                                fontSize = 10.sp,
                                                color = colors.alert,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                RuntimeManager.installJavaVersion(context, jarAnalysis.requiredJavaVersion) { success ->
                                                    Toast.makeText(context, if (success) "Installed Java ${jarAnalysis.requiredJavaVersion} JRE!" else "Install failed", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.alert),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Install Java ${jarAnalysis.requiredJavaVersion} JRE (~45MB)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                    else -> {
                                        Text(
                                            text = "Java JRE is not yet installed in Debian subsystem.",
                                            fontSize = 10.sp,
                                            color = colors.warning,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                val verToInstall = jarAnalysis.requiredJavaVersion.coerceAtLeast(21)
                                                RuntimeManager.installJavaVersion(context, verToInstall) { success ->
                                                    Toast.makeText(context, if (success) "Installed Java $verToInstall JRE!" else "Install failed", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.accentEmerald),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Install Java ${jarAnalysis.requiredJavaVersion.coerceAtLeast(21)} JRE (~45MB)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Node.js Detection Card
                    if (nodeReq != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surfaceSecondary,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Node.js Target: ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = nodeReq.version,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.accentCyan
                                    )
                                }
                                if (nodeReq.warning != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = colors.warning,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = nodeReq.warning,
                                            fontSize = 9.sp,
                                            color = colors.textMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Python Detection Card
                    if (pythonReq != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surfaceSecondary,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Python Target: ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = pythonReq.version,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.warning
                                    )
                                }
                                if (pythonReq.warning != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = colors.warning,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = pythonReq.warning,
                                            fontSize = 9.sp,
                                            color = colors.textMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Advanced Runtime Settings (Accordion)
        item {
            val installedJavaVer by RuntimeManager.installedJavaVersion.collectAsState()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvancedRuntime = !showAdvancedRuntime },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ADVANCED RUNTIME SETTINGS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                letterSpacing = (1.1).sp
                            )
                            Text(
                                text = "Runtime Versions & Cleanup",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.surfaceSecondary,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Text(
                                text = if (showAdvancedRuntime) "▲ Close" else "▼ Manage",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (showAdvancedRuntime) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "TARGET JAVA JRE (HEADLESS)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted,
                            letterSpacing = (0.5).sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Selector row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RuntimeManager.availableJavaOptions.forEach { opt ->
                                val isSelected = selectedJavaVersion == opt.version
                                val isInstalled = installedJavaVer == opt.version

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) colors.accentEmeraldLight else colors.surfaceSecondary,
                                    border = BorderStroke(1.dp, if (isSelected) colors.accentEmerald else colors.border),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedJavaVersion = opt.version }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Java ${opt.version}",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.accentEmerald else colors.textPrimary,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isInstalled) "Active" else "${opt.sizeMb.toInt()} MB",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isInstalled) colors.accentEmerald else colors.textMuted,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action row for selected Java version
                        val selectedOpt = RuntimeManager.availableJavaOptions.find { it.version == selectedJavaVersion }
                        if (selectedOpt != null && installedJavaVer != selectedJavaVersion) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "On-demand headless JRE (~${selectedOpt.sizeMb.toInt()}MB)",
                                    fontSize = 10.sp,
                                    color = colors.textMuted,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        RuntimeManager.installJavaVersion(context, selectedJavaVersion) { success ->
                                            Toast.makeText(context, if (success) "Installed Java $selectedJavaVersion JRE!" else "Install failed", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentEmerald),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Switch to Java $selectedJavaVersion", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Storage Purge Unused Runtimes section
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.border.copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Storage Optimizer",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Purge unused JREs to reclaim phone storage",
                                    fontSize = 9.sp,
                                    color = colors.textMuted
                                )
                            }

                            Button(
                                onClick = {
                                    isPurgingRuntimes = true
                                    RuntimeManager.purgeUnusedJavaRuntimes(
                                        context,
                                        keepVersion = if (installedJavaVer > 0) installedJavaVer else 21
                                    ) { success, msg ->
                                        isPurgingRuntimes = false
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isPurgingRuntimes,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceSecondary),
                                border = BorderStroke(1.dp, colors.border),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = if (isPurgingRuntimes) "Cleaning..." else "Clean Unused Runtimes",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.alert
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Application Start Command & Port Configuration Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PROCESS RUNTIME",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                letterSpacing = (1.1).sp
                            )
                            Text(
                                text = "Start Command & Port",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.accentEmeraldLight
                        ) {
                            Text(
                                text = "PaaS Engine",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentEmerald,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "START COMMAND",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted,
                        letterSpacing = (0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = startCommandInput,
                        onValueChange = { newVal ->
                            startCommandInput = newVal
                            prefs.edit().putString(HostTriangleServerService.KEY_CUSTOM_START_COMMAND, newVal).apply()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = colors.textPrimary
                        ),
                        placeholder = {
                            Text(
                                text = "e.g. java -jar backend.jar --server.port=8080",
                                fontSize = 12.sp,
                                color = colors.textMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accentEmerald,
                            unfocusedBorderColor = colors.border,
                            focusedContainerColor = colors.surfaceSecondary,
                            unfocusedContainerColor = colors.surfaceSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = false,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Command Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.surfaceSecondary,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.clickable {
                                val defCmd = HostTriangleServerService.computeDefaultStartCommand(
                                    workspaceDir,
                                    portInput.toIntOrNull() ?: HostTriangleServerService.DEFAULT_PORT
                                )
                                startCommandInput = defCmd
                                prefs.edit().putString(HostTriangleServerService.KEY_CUSTOM_START_COMMAND, defCmd).apply()
                            }
                        ) {
                            Text(
                                text = "↺ Auto-Detect",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.accentCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.surfaceSecondary,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.clickable {
                                if (!startCommandInput.contains("-Xmx")) {
                                    val updated = if (startCommandInput.contains("-jar")) {
                                        startCommandInput.replace("-jar", "-Xmx512m -jar")
                                    } else {
                                        "$startCommandInput -Xmx512m"
                                    }
                                    startCommandInput = updated
                                    prefs.edit().putString(HostTriangleServerService.KEY_CUSTOM_START_COMMAND, updated).apply()
                                }
                            }
                        ) {
                            Text(
                                text = "+ 512MB RAM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.accentEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.surfaceSecondary,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.clickable {
                                if (!startCommandInput.contains("spring.profiles.active")) {
                                    val updated = "$startCommandInput --spring.profiles.active=prod"
                                    startCommandInput = updated
                                    prefs.edit().putString(HostTriangleServerService.KEY_CUSTOM_START_COMMAND, updated).apply()
                                }
                            }
                        ) {
                            Text(
                                text = "+ Prod Profile",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.accentIndigo,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Ingress Port Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "INTERNAL INGRESS PORT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                letterSpacing = (0.5).sp
                            )
                            Text(
                                text = "Target port your app listens on",
                                fontSize = 10.sp,
                                color = colors.textMuted
                            )
                        }

                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 5) {
                                    portInput = input
                                    val parsed = input.toIntOrNull()
                                    if (parsed != null && parsed in 1024..65535) {
                                        prefs.edit().putInt(HostTriangleServerService.KEY_CONFIG_PORT, parsed).apply()
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(96.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = colors.textPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accentEmerald,
                                unfocusedBorderColor = colors.border,
                                focusedContainerColor = colors.surfaceSecondary,
                                unfocusedContainerColor = colors.surfaceSecondary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Port Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("8080", "3000", "5000", "8000").forEach { p ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (portInput == p) colors.accentEmeraldLight else colors.surfaceSecondary,
                                border = BorderStroke(1.dp, if (portInput == p) colors.accentEmerald else colors.border),
                                modifier = Modifier.clickable {
                                    portInput = p
                                    prefs.edit().putInt(HostTriangleServerService.KEY_CONFIG_PORT, p.toInt()).apply()
                                }
                            ) {
                                Text(
                                    text = ":$p",
                                    fontSize = 10.sp,
                                    fontWeight = if (portInput == p) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (portInput == p) colors.accentEmerald else colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Global Zero Trust Ingress & Cloudflare BYOC Card
        item {
            val decodedTunnel = ClusterFleetManager.decodedTunnelInfo.collectAsState().value
            var cfApiTokenInput by remember { mutableStateOf(ClusterFleetManager.getStoredApiToken(context)) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "GLOBAL ZERO TRUST INGRESS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                letterSpacing = (1.1).sp
                            )
                            Text(
                                text = "Cloudflare Tunnel Gateway",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFF7ED)
                        ) {
                            Text(
                                text = "BYOC Pro",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF38020),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tunnel Identity & Status Banner
                    val isTokenConfigured = tunnelToken.isNotBlank() && decodedTunnel?.isValid == true
                    val statusPillBg = if (isTokenConfigured) colors.accentEmeraldLight else colors.surfaceSecondary
                    val statusPillBorder = if (isTokenConfigured) colors.accentEmerald else colors.border
                    val statusPillColor = if (isTokenConfigured) colors.accentEmerald else colors.textMuted
                    val statusText = if (isTokenConfigured) "READY" else if (tunnelToken.isNotBlank()) "INVALID TOKEN" else "NOT CONFIGURED"

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceSecondary,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = null,
                                        tint = Color(0xFFF38020),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Cloudflare Anycast Gateway",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = statusPillBg,
                                    border = BorderStroke(1.dp, statusPillBorder)
                                ) {
                                    Text(
                                        text = statusText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusPillColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            if (decodedTunnel != null && decodedTunnel.isValid) {
                                Text(
                                    text = "Tunnel: ${decodedTunnel.tunnelId.take(16)}... • Acc: ${decodedTunnel.accountId.take(10)}...",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    text = "Enter your Cloudflare Zero Trust Tunnel token below to route ingress traffic to this device.",
                                    fontSize = 10.sp,
                                    color = colors.textMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Credentials Input 1: Tunnel Secret Token
                    Text(
                        text = "CLOUDFLARE TUNNEL TOKEN (SECRET)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted,
                        letterSpacing = (0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = tunnelToken,
                        onValueChange = { input ->
                            tunnelToken = input.trim()
                            prefs.edit().putString(HostTriangleServerService.KEY_TUNNEL_TOKEN, tunnelToken).apply()
                            ClusterFleetManager.decodeTunnelToken(tunnelToken)
                        },
                        placeholder = {
                            Text(
                                text = "Enter Cloudflare Token",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textMuted
                            )
                        },
                        trailingIcon = if (tunnelToken.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = {
                                        tunnelToken = ""
                                        prefs.edit().putString(HostTriangleServerService.KEY_TUNNEL_TOKEN, "").apply()
                                        ClusterFleetManager.decodeTunnelToken("")
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Token",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = colors.textPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accentEmerald,
                            unfocusedBorderColor = colors.border,
                            focusedContainerColor = colors.surfaceSecondary,
                            unfocusedContainerColor = colors.surfaceSecondary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Credentials Input 2: Cloudflare API Read Token
                    Text(
                        text = "CLOUDFLARE API READ TOKEN (FLEET MAP SYNC)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted,
                        letterSpacing = (0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = cfApiTokenInput,
                        onValueChange = { input ->
                            cfApiTokenInput = input.trim()
                            ClusterFleetManager.saveApiToken(context, cfApiTokenInput)
                        },
                        placeholder = {
                            Text(
                                text = "Enter Cloudflare API Read Token",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textMuted
                            )
                        },
                        trailingIcon = if (cfApiTokenInput.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = {
                                        cfApiTokenInput = ""
                                        ClusterFleetManager.saveApiToken(context, "")
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear API Token",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = colors.textPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF38020),
                            unfocusedBorderColor = colors.border,
                            focusedContainerColor = colors.surfaceSecondary,
                            unfocusedContainerColor = colors.surfaceSecondary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Built-in Cloudflare Daemon Status Strip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceSecondary,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (cfStatus) {
                                                    is CloudflareTunnelManager.TunnelStatus.Connected,
                                                    is CloudflareTunnelManager.TunnelStatus.Ready -> colors.accentEmerald
                                                    is CloudflareTunnelManager.TunnelStatus.Connecting,
                                                    is CloudflareTunnelManager.TunnelStatus.Downloading -> colors.accentCyan
                                                    is CloudflareTunnelManager.TunnelStatus.Error -> colors.alert
                                                    else -> colors.textMuted
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Daemon Status",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = when (cfStatus) {
                                                is CloudflareTunnelManager.TunnelStatus.Connected -> "ACTIVE [MUMBAI EDGE]"
                                                is CloudflareTunnelManager.TunnelStatus.Connecting -> "CONNECTING..."
                                                is CloudflareTunnelManager.TunnelStatus.Ready -> "DAEMON READY (AUTO-LAUNCH)"
                                                is CloudflareTunnelManager.TunnelStatus.Downloading -> "DOWNLOADING (${((cfStatus as CloudflareTunnelManager.TunnelStatus.Downloading).progress * 100).toInt()}%)"
                                                is CloudflareTunnelManager.TunnelStatus.NotInstalled -> "NOT INSTALLED"
                                                is CloudflareTunnelManager.TunnelStatus.Stopped -> "IDLE (STARTS ON LAUNCH)"
                                                is CloudflareTunnelManager.TunnelStatus.Error -> "INSTALL / RETRY"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = when (cfStatus) {
                                                is CloudflareTunnelManager.TunnelStatus.Connected,
                                                is CloudflareTunnelManager.TunnelStatus.Ready -> colors.accentEmerald
                                                is CloudflareTunnelManager.TunnelStatus.Connecting,
                                                is CloudflareTunnelManager.TunnelStatus.Downloading -> colors.accentCyan
                                                is CloudflareTunnelManager.TunnelStatus.Error -> colors.alert
                                                else -> colors.textMuted
                                            }
                                        )
                                    }
                                }

                                if (cfStatus is CloudflareTunnelManager.TunnelStatus.NotInstalled || cfStatus is CloudflareTunnelManager.TunnelStatus.Error) {
                                    Button(
                                        onClick = { CloudflareTunnelManager.installDaemon(context) },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentEmerald),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Install", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }

                            if (cfStatus is CloudflareTunnelManager.TunnelStatus.Downloading) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (cfStatus as CloudflareTunnelManager.TunnelStatus.Downloading).progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = colors.accentCyan,
                                    trackColor = colors.border
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Runtime Card Row
@Composable
fun RuntimeCardRow(
    spec: RuntimeManager.RuntimeSpec,
    status: RuntimeManager.Status,
    onInstallClick: () -> Unit,
    onUninstallClick: () -> Unit
) {
    val colors = LocalPalette.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceSecondary,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = spec.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = colors.surface,
                        border = androidx.compose.foundation.BorderStroke((0.5).dp, colors.border)
                    ) {
                        Text(
                            text = "${spec.sizeMb} MB",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = spec.subtitle,
                    fontSize = 10.sp,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            when (status) {
                is RuntimeManager.Status.NotInstalled -> {
                    Button(
                        onClick = onInstallClick,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentEmerald),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Install", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                is RuntimeManager.Status.Ready -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.accentEmeraldLight
                        ) {
                            Text(
                                text = "READY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onUninstallClick,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Uninstall",
                                tint = colors.alert,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                is RuntimeManager.Status.Downloading -> {
                    Text(
                        text = "${(status.progress * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentCyan
                    )
                }
                is RuntimeManager.Status.Extracting -> {
                    Text(
                        text = "STAGING...",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.warning
                    )
                }
                is RuntimeManager.Status.Error -> {
                    Button(
                        onClick = onInstallClick,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.alert),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Retry", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SCREEN 3: NATIVE PTY TERMINAL & SERVER LOGS (PERSISTENT LINUX SUBSYSTEM SHELL)
// -------------------------------------------------------------------------------------------------
@Composable
fun LiveLogsScreen(onOpenSetupWizard: () -> Unit = {}) {
    val context = LocalContext.current
    val colors = LocalPalette.current
    val coroutineScope = rememberCoroutineScope()
    val prootEngine = remember { ProotEngine(context) }
    val isEngineInstalled = remember { prootEngine.isInstalled() }

    var activeTabMode by remember { mutableStateOf("TERMINAL") } // "TERMINAL" or "LOGS"
    var isCtrlActive by remember { mutableStateOf(false) }
    var sessionKey by remember { mutableIntStateOf(0) }
    var terminalViewRef by remember { mutableStateOf<TerminalView?>(null) }

    // Logs state (for Server Logs view)
    val listState = rememberLazyListState()
    val logEntries = remember { mutableStateListOf<HostTriangleServerService.LogEntry>() }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var autoScroll by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        HostTriangleServerService.logFlow.collect { entry ->
            logEntries.add(entry)
            if (logEntries.size > 500) {
                logEntries.removeAt(0)
            }
            if (autoScroll) {
                coroutineScope.launch {
                    listState.animateScrollToItem(logEntries.size - 1)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            terminalViewRef?.let { TerminalSessionManager.detachView(it) }
        }
    }

    val copyLogsToClipboard: () -> Unit = {
        val textToCopy = logEntries.joinToString("\n") { log ->
            "[${log.formattedTime}] [${log.level.name.take(4)}] [${log.tag}] ${log.message}"
        }
        if (textToCopy.isNotBlank()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("HostTriangle Terminal Logs", textToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copied ${logEntries.size} log lines to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    val isImeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(isImeOpen) {
        delay(120)
        terminalViewRef?.post {
            terminalViewRef?.setTopRow(0)
            terminalViewRef?.updateSize()
            terminalViewRef?.onScreenUpdated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        // Mode Selector & Toolbar Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Segmented Mode Switcher: [Terminal] | [Server Logs]
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val isTerminal = activeTabMode == "TERMINAL"
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isTerminal) colors.accentEmerald else Color.Transparent,
                    modifier = Modifier.clickable { activeTabMode = "TERMINAL" }
                ) {
                    Text(
                        text = "Terminal",
                        fontSize = 11.sp,
                        fontWeight = if (isTerminal) FontWeight.Bold else FontWeight.Medium,
                        color = if (isTerminal) Color.White else colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }

                val isLogs = activeTabMode == "LOGS"
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isLogs) colors.accentIndigo else Color.Transparent,
                    modifier = Modifier.clickable { activeTabMode = "LOGS" }
                ) {
                    Text(
                        text = "Server Logs",
                        fontSize = 11.sp,
                        fontWeight = if (isLogs) FontWeight.Bold else FontWeight.Medium,
                        color = if (isLogs) Color.White else colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }

            // Right Actions
            if (activeTabMode == "TERMINAL") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Status dot
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF064E3B).copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "BASH",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399),
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }

                    // Restart Session Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.surfaceSecondary,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.clickable {
                            TerminalSessionManager.restartSession(context)
                            sessionKey++
                            Toast.makeText(context, "Restarted PTY bash session", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Restart",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Restart",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { copyLogsToClipboard() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Logs",
                            tint = colors.accentEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { autoScroll = !autoScroll },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Auto Scroll",
                            tint = if (autoScroll) colors.accentEmerald else colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { logEntries.clear() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = colors.alert,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content Area
        if (activeTabMode == "TERMINAL") {
            if (!isEngineInstalled) {
                // Debian Subsystem Not Installed prompt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = colors.alert,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Linux Subsystem Not Provisioned",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Debian 12 ARM64 rootfs must be extracted before launching the native PTY bash terminal.",
                            fontSize = 12.sp,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onOpenSetupWizard,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accentEmerald),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Open Setup Wizard", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Persistent Native PTY Terminal View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(1.dp, colors.terminalBorder, RoundedCornerShape(12.dp))
                ) {
                    if (!TerminalSessionManager.ensureNativeLibraryLoaded()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Terminal Unsupported",
                                tint = colors.warning,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Terminal is not supported on this device",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Native terminal engine (libtermux.so) could not be loaded.\n${TerminalSessionManager.nativeLoadError ?: "UnsatisfiedLinkError"}",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        key(sessionKey) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    TerminalView(ctx, null).apply {
                                        terminalViewRef = this
                                        TerminalSessionManager.attachView(this, ctx)
                                    }
                                },
                                update = { view ->
                                    terminalViewRef = view
                                    if (TerminalSessionManager.attachedView != view) {
                                        TerminalSessionManager.attachView(view, view.context)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Quick Command Shortcuts
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val quickCommands = listOf("ls -la", "top", "nano", "pwd", "free -m", "java -version", "apt update", "clear")
                    items(quickCommands) { cmd ->
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.clickable {
                                if (cmd == "clear") {
                                    TerminalSessionManager.writeText("clear\n")
                                } else {
                                    TerminalSessionManager.writeText("$cmd\n")
                                }
                            }
                        ) {
                            Text(
                                text = cmd,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.accentEmerald,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Extra Keys Toolbar (Ctrl, Esc, Tab, Ctrl+C, Ctrl+D, Ctrl+Z, Ctrl+O, Ctrl+X, arrows, q)
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sticky CTRL
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isCtrlActive) Color(0xFFF59E0B) else Color(0xFF334155),
                        modifier = Modifier.clickable {
                            isCtrlActive = !isCtrlActive
                            TerminalSessionManager.isCtrlActive = isCtrlActive
                        }
                    ) {
                        Text(
                            text = "CTRL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isCtrlActive) Color.Black else Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    TerminalKeyChip("ESC") { TerminalSessionManager.write(byteArrayOf(0x1B)) }
                    TerminalKeyChip("TAB") { TerminalSessionManager.write("\t".toByteArray()) }
                    TerminalKeyChip("Ctrl+C", alert = true) { TerminalSessionManager.write(byteArrayOf(0x03)) }
                    TerminalKeyChip("Ctrl+D") { TerminalSessionManager.write(byteArrayOf(0x04)) }
                    TerminalKeyChip("Ctrl+Z") { TerminalSessionManager.write(byteArrayOf(0x1A)) }
                    TerminalKeyChip("Ctrl+O") { TerminalSessionManager.write(byteArrayOf(0x0F)) }
                    TerminalKeyChip("Ctrl+X") { TerminalSessionManager.write(byteArrayOf(0x18)) }
                    TerminalKeyChip("▲") { TerminalSessionManager.write("\u001b[A".toByteArray()) }
                    TerminalKeyChip("▼") { TerminalSessionManager.write("\u001b[B".toByteArray()) }
                    TerminalKeyChip("◄") { TerminalSessionManager.write("\u001b[D".toByteArray()) }
                    TerminalKeyChip("►") { TerminalSessionManager.write("\u001b[C".toByteArray()) }
                    TerminalKeyChip("q") { TerminalSessionManager.write("q".toByteArray()) }
                }
            }
        } else {
            // Server Logs View (Spring Boot, Cloudflare, Services)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = colors.terminalBg,
                border = BorderStroke(1.dp, colors.terminalBorder)
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    if (logEntries.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "LOG BUFFER IDLE // WAITING FOR SERVER OUTPUT",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(logEntries) { log ->
                                val logColor = when (log.level) {
                                    HostTriangleServerService.LogLevel.INFO -> Color(0xFF4ADE80)
                                    HostTriangleServerService.LogLevel.DEBUG -> Color(0xFF38BDF8)
                                    HostTriangleServerService.LogLevel.WARN -> Color(0xFFFBBF24)
                                    HostTriangleServerService.LogLevel.ERROR -> Color(0xFFF87171)
                                    HostTriangleServerService.LogLevel.CRITICAL -> Color(0xFFFF4D4D)
                                }

                                val isSpringFormatted = log.message.startsWith("  .") ||
                                        log.message.startsWith(" /") ||
                                        log.message.startsWith("( (") ||
                                        log.message.startsWith(" \\\\") ||
                                        log.message.startsWith("  '") ||
                                        log.message.startsWith(" ===") ||
                                        log.message.startsWith(" :: Spring Boot ::") ||
                                        log.message.contains(" --- [")

                                val displayText = if (isSpringFormatted || log.tag == "RAW") {
                                    log.message
                                } else {
                                    "[${log.formattedTime}] [${log.level.name.take(4)}] [${log.tag}] ${log.message}"
                                }

                                Text(
                                    text = displayText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSpringFormatted && !log.message.contains(" --- [")) Color(0xFF38BDF8) else logColor,
                                    lineHeight = 15.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Log Line", displayText))
                                            Toast.makeText(context, "Log line copied!", Toast.LENGTH_SHORT).show()
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalKeyChip(
    label: String,
    alert: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (alert) Color(0xFFDC2626) else Color(0xFF334155),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

// -------------------------------------------------------------------------------------------------
// SCREEN 4: CLUSTERS FLEET MAP (LIVE CLOUDFLARE REPLICAS & LAN MESH)
// -------------------------------------------------------------------------------------------------

fun getRealDeviceName(): String {
    val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
    val model = Build.MODEL
    return if (model.startsWith(manufacturer, ignoreCase = true)) {
        model
    } else {
        "$manufacturer $model"
    }
}

fun getRealLocalIpAddress(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val intf = interfaces.nextElement()
            if (intf.isLoopback || !intf.isUp) continue
            val addrs = intf.inetAddresses
            while (addrs.hasMoreElements()) {
                val addr = addrs.nextElement()
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    val host = addr.hostAddress ?: ""
                    if (host.isNotEmpty() && !host.startsWith("127.")) {
                        return host
                    }
                }
            }
        }
    } catch (_: Exception) {}
    return "127.0.0.1"
}

@Composable
fun ClustersFleetScreen() {
    val context = LocalContext.current
    val colors = LocalPalette.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        ClusterFleetManager.init(context)
        ClusterFleetManager.recalculateFleet(context)
    }

    val clusterNodes by ClusterFleetManager.fleetNodes.collectAsState()
    val decodedTunnel by ClusterFleetManager.decodedTunnelInfo.collectAsState()
    val isSyncing by ClusterFleetManager.isSyncing.collectAsState()
    val syncError by ClusterFleetManager.lastSyncError.collectAsState()

    var showApiTokenDialog by remember { mutableStateOf(false) }
    var enteredApiToken by remember { mutableStateOf("") }

    if (showApiTokenDialog) {
        AlertDialog(
            onDismissRequest = { showApiTokenDialog = false },
            title = {
                Text(
                    text = "Cloudflare API Token",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a Cloudflare API Token (with Account > Cloudflare Tunnel:Read permission) to auto-sync all live connected replica phones across the globe.",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = enteredApiToken,
                        onValueChange = { enteredApiToken = it },
                        label = { Text("API Bearer Token") },
                        placeholder = { Text("e.g. 1a2b3c4d5e...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF38020),
                            cursorColor = Color(0xFFF38020)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ClusterFleetManager.saveApiToken(context, enteredApiToken)
                        showApiTokenDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF38020))
                ) {
                    Text("Save & Sync", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiTokenDialog = false }) {
                    Text("Cancel", color = colors.textMuted)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Fleet Header Card (Live Metrics & Cloudflare Zero Trust Status)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DEPIN CLUSTER FLEET TOPOLOGY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted,
                            letterSpacing = (1.1).sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (clusterNodes.isNotEmpty()) colors.accentEmerald else colors.textMuted)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE SYNC",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentEmerald,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val primaryCount = clusterNodes.count { it.isPrimary }
                    val replicaCount = clusterNodes.count { !it.isPrimary }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FleetStatPill(
                            title = "Primary",
                            value = "$primaryCount Active",
                            accentColor = colors.accentEmerald,
                            bgColor = colors.accentEmeraldLight,
                            modifier = Modifier.weight(1f)
                        )
                        FleetStatPill(
                            title = "Replicas",
                            value = "$replicaCount ${if (replicaCount == 1) "Node" else "Nodes"}",
                            accentColor = Color(0xFFF38020),
                            bgColor = Color(0xFFFFF7ED),
                            modifier = Modifier.weight(1f)
                        )
                        FleetStatPill(
                            title = "Mesh State",
                            value = if (clusterNodes.size > 1) "Multi-Node" else "Standby",
                            accentColor = colors.accentCyan,
                            bgColor = colors.surfaceSecondary,
                            modifier = Modifier.weight(1.1f)
                        )
                    }

                    // Cloudflare Tunnel & API Connector Banner
                    val tunnelInfo = decodedTunnel
                    if (tunnelInfo != null && tunnelInfo.isValid) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceSecondary,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFF38020))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "CLOUDFLARE REPLICAS SYNC",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF38020),
                                            letterSpacing = (0.5).sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Cloudflare API Token configuration button
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = colors.surface,
                                            border = BorderStroke(1.dp, colors.border),
                                            modifier = Modifier.clickable {
                                                enteredApiToken = ClusterFleetManager.getStoredApiToken(context)
                                                showApiTokenDialog = true
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Cloud,
                                                    contentDescription = "Cloudflare API Token",
                                                    tint = Color(0xFFF38020),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Token",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textPrimary
                                                )
                                            }
                                        }

                                        // Refresh Replicas button
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = colors.accentEmeraldLight,
                                            modifier = Modifier.clickable {
                                                coroutineScope.launch {
                                                    ClusterFleetManager.syncCloudflareReplicas(context)
                                                }
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Sync",
                                                    tint = colors.accentEmerald,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = if (isSyncing) "Syncing..." else "Sync",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.accentEmerald,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Tunnel: ${tunnelInfo.tunnelId.take(12)}...  •  Acc: ${tunnelInfo.accountId.take(10)}...",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val currentSyncError = syncError
                                if (!currentSyncError.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = colors.alert,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = currentSyncError,
                                            fontSize = 9.sp,
                                            color = colors.alert,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fleet Nodes Cards (Real Local Device, Discovered LAN Peers, and Cloudflare Replicas)
        items(clusterNodes) { node ->
            val sourceBadgeColor = when (node.source) {
                ClusterFleetManager.NodeSource.LOCAL -> colors.accentEmerald
                ClusterFleetManager.NodeSource.CLOUDFLARE_REPLICA -> Color(0xFFF38020)
                ClusterFleetManager.NodeSource.LAN_PEER -> colors.accentCyan
            }
            val sourceBadgeBg = when (node.source) {
                ClusterFleetManager.NodeSource.LOCAL -> colors.accentEmeraldLight
                ClusterFleetManager.NodeSource.CLOUDFLARE_REPLICA -> Color(0xFFFFF7ED)
                ClusterFleetManager.NodeSource.LAN_PEER -> colors.surfaceSecondary
            }

            val statusColor = when {
                node.status.contains("HEALTHY", true) || node.status.contains("ACTIVE", true) || node.status.contains("READY", true) -> colors.accentEmerald
                node.status.contains("SYNC", true) || node.status.contains("CONNECT", true) -> Color(0xFFF38020)
                else -> colors.textMuted
            }
            val statusBg = when {
                node.status.contains("HEALTHY", true) || node.status.contains("ACTIVE", true) || node.status.contains("READY", true) -> colors.accentEmeraldLight
                node.status.contains("SYNC", true) || node.status.contains("CONNECT", true) -> Color(0xFFFFF7ED)
                else -> colors.surfaceSecondary
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Row 1: Role Badge + Name (weighted) + Status Badge (softWrap = false)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = sourceBadgeBg
                            ) {
                                Text(
                                    text = node.role,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = sourceBadgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = node.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Status Pill with Live Colored Dot (Never vertically wrapped)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusBg
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = node.status,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sleek subtle horizontal divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.border.copy(alpha = 0.45f))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: HOST Pill + Endpoint (weight 1f) + Region + Ping
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Text(
                                text = "HOST",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                modifier = Modifier
                                    .background(colors.surfaceSecondary, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = node.endpoint,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (node.region.isNotBlank()) {
                                Text(
                                    text = node.region,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (node.isPrimary) colors.accentEmerald else Color(0xFFF38020),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.surfaceSecondary
                            ) {
                                Text(
                                    text = "${node.pingMs}ms",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (node.pingMs <= 20) colors.accentEmerald else colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FleetStatPill(
    title: String,
    value: String,
    accentColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// LINUX SUBSYSTEM SETUP WIZARD
// -------------------------------------------------------------------------------------------------

enum class SetupPhase {
    READY,
    INSTALLING,
    COMPLETED,
    ERROR
}

@Composable
fun LinuxSubsystemSetupScreen(
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalPalette.current
    val coroutineScope = rememberCoroutineScope()

    var phase by remember { mutableStateOf(SetupPhase.READY) }
    var progressPct by remember { mutableIntStateOf(0) }
    var stageText by remember { mutableStateOf("Ready to install Debian Linux Subsystem") }
    var detailText by remember { mutableStateOf("Package size: ~41 MB (Extracted: ~115 MB)") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDetails by remember { mutableStateOf(false) }

    val logEntries = remember { mutableStateListOf<HostTriangleServerService.LogEntry>() }

    LaunchedEffect(Unit) {
        HostTriangleServerService.logFlow.collect { entry ->
            logEntries.add(entry)
            if (logEntries.size > 200) {
                logEntries.removeAt(0)
            }
        }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(logEntries.size) {
        if (showDetails && logEntries.isNotEmpty()) {
            listState.animateScrollToItem(logEntries.size - 1)
        }
    }

    fun startInstallation() {
        phase = SetupPhase.INSTALLING
        errorMessage = null
        progressPct = 5
        stageText = "Initializing installation pipeline..."
        detailText = "Checking device architecture and storage..."

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val engine = EngineManager.getEngine(context)
                engine.installDetailed { p ->
                    progressPct = p.percent
                    stageText = p.stage
                    detailText = p.detail
                }
                phase = SetupPhase.COMPLETED
                RuntimeManager.refreshStatuses(context)
            } catch (e: Exception) {
                errorMessage = e.message ?: "Installation failed. Please check internet connection."
                phase = SetupPhase.ERROR
                HostTriangleServerService.log(
                    HostTriangleServerService.LogLevel.ERROR,
                    "SetupWizard",
                    "Installation failed: ${e.message}"
                )
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Emblem & Subsystem Brand
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                AnimatedVisibility(
                    visible = !showDetails,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0B0F19),
                        border = BorderStroke(1.5.dp, Color(0xFF0284C7)),
                        modifier = Modifier.size(76.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.hosttriangle_icon),
                                contentDescription = "HostTriangle Emblem",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Text(
                    text = "HostTriangle Subsystem Setup",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Debian 12 (arm64) • Rootless Userland",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center
                )
            }

            when (phase) {
                SetupPhase.READY -> {
                    // Explanatory Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "HostTriangle needs a lightweight Debian environment to run your .jar, Node.js, or Python backends locally on this device.",
                                fontSize = 13.sp,
                                color = colors.textSecondary,
                                lineHeight = 19.sp
                            )

                            // Specs Grid
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SetupSpecRow(label = "Linux Userland", value = "Debian 12 Bookworm (arm64)", colors = colors)
                                SetupSpecRow(label = "Sandbox Engine", value = "PRoot v5.1 (Userland)", colors = colors)
                                SetupSpecRow(label = "Archive Size", value = "~41 MB (Debian Mirror)", colors = colors)
                                SetupSpecRow(label = "Host Security", value = "Rootless Sandbox (No Root)", colors = colors)
                            }
                        }
                    }

                    // Network Tip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.accentEmeraldLight,
                        border = BorderStroke(1.dp, colors.accentEmerald.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = colors.accentEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "One-time setup: Requires an active WiFi or mobile data connection to download and extract the initial Debian package.",
                                fontSize = 12.sp,
                                color = colors.accentEmerald,
                                lineHeight = 17.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { startInstallation() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentEmerald),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = Color.White)
                            Text(
                                text = "Install Linux Subsystem",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                SetupPhase.INSTALLING -> {
                    // Installation Progress Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stageText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$progressPct%",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.accentEmerald
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (progressPct / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = colors.accentEmerald,
                                trackColor = colors.surfaceSecondary
                            )

                            // Live Extracting / Download Ticker
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surfaceSecondary,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = detailText.ifEmpty { "Processing userland components..." },
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Step Checklist
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                InstallerStepItem(
                                    title = "Verify Device Architecture (arm64-v8a)",
                                    isDone = true,
                                    isCurrent = false,
                                    colors = colors
                                )
                                InstallerStepItem(
                                    title = "Download Debian 12 Minimal Tarball",
                                    isDone = progressPct >= 60,
                                    isCurrent = progressPct in 1..59,
                                    colors = colors
                                )
                                InstallerStepItem(
                                    title = "Extract Rootfs & Apply Unix Modes (tar.xz)",
                                    isDone = progressPct >= 90,
                                    isCurrent = progressPct in 60..89,
                                    colors = colors
                                )
                                InstallerStepItem(
                                    title = "Configure DNS & Userland Permissions",
                                    isDone = progressPct >= 95,
                                    isCurrent = progressPct in 90..94,
                                    colors = colors
                                )
                                InstallerStepItem(
                                    title = "Configure Package Repositories (APT)",
                                    isDone = progressPct >= 100,
                                    isCurrent = progressPct in 95..99,
                                    colors = colors
                                )
                            }
                        }
                    }

                    // Console Logs Expander
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDetails = !showDetails }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showDetails) "▲ Hide Console Logs" else "▼ Show Console Logs",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.accentEmerald
                        )
                        val eventCount = logEntries.size
                        Text(
                            text = if (eventCount == 1) "1 event" else "$eventCount events",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }

                    if (showDetails) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .border(1.dp, colors.terminalBorder, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = colors.terminalBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp)
                            ) {
                                items(logEntries) { entry ->
                                    Text(
                                        text = "[${entry.tag}] ${entry.message}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (entry.level == HostTriangleServerService.LogLevel.ERROR) colors.alert else colors.terminalText,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                SetupPhase.COMPLETED -> {
                    // Success View
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.accentEmerald.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = colors.accentEmerald,
                                modifier = Modifier.size(54.dp)
                            )

                            Text(
                                text = "Linux Subsystem Ready!",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Debian 12 userland is completely verified and configured in userland sandbox (/srv/app). You can now run Linux shell commands, install runtimes, and host backends.",
                                fontSize = 13.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Button(
                        onClick = onCompleted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentEmerald),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            Text(
                                text = "Launch HostTriangle",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                SetupPhase.ERROR -> {
                    // Error View
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.alert.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = colors.alert,
                                modifier = Modifier.size(48.dp)
                            )

                            Text(
                                text = "Installation Interrupted",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )

                            Text(
                                text = errorMessage ?: "Unknown error occurred during setup.",
                                fontSize = 12.sp,
                                color = colors.alert,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Button(
                        onClick = { startInstallation() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentEmerald),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                            Text(
                                text = "Retry Installation",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupSpecRow(label: String, value: String, colors: PocketPalette) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = colors.textMuted)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun InstallerStepItem(
    title: String,
    isDone: Boolean,
    isCurrent: Boolean,
    colors: PocketPalette
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> colors.accentEmerald
                        isCurrent -> colors.accentEmeraldLight
                        else -> colors.surfaceSecondary
                    }
                )
                .border(
                    width = 1.dp,
                    color = when {
                        isDone -> colors.accentEmerald
                        isCurrent -> colors.accentEmerald
                        else -> colors.border
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            } else if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.accentEmerald)
                )
            }
        }

        Text(
            text = title,
            fontSize = 12.sp,
            color = if (isDone || isCurrent) colors.textPrimary else colors.textMuted,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
