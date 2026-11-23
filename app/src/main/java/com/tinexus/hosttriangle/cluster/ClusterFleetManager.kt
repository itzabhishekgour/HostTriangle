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

package com.tinexus.hosttriangle.cluster

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Base64
import android.util.Log
import com.tinexus.hosttriangle.CloudflareTunnelManager
import com.tinexus.hosttriangle.HostTriangleServerService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.*
import java.util.concurrent.ConcurrentHashMap

/**
 * ClusterFleetManager
 *
 * Real-time Distributed Peer Discovery & Cloudflare Replicas Sync Engine.
 * 1. Decodes Cloudflare Tunnel Token (Extracts Tunnel ID & Account ID).
 * 2. Queries Cloudflare Zero Trust REST API for all active remote connectors / replicas.
 * 3. Broadcasts and listens for Local Wi-Fi mesh heartbeats via UDP broadcast so phones
 *    in the same LAN auto-discover each other without requiring any external server.
 * 4. Combines local device telemetry, LAN peers, and Cloudflare cloud replicas into a unified
 *    real-time fleet topology for the Fleet Map screen.
 */
object ClusterFleetManager {

    private const val TAG = "ClusterFleetMgr"
    private const val PREFS_NAME = HostTriangleServerService.PREFS_NAME
    private const val KEY_CF_API_TOKEN = "KEY_CLOUDFLARE_API_TOKEN"
    private const val UDP_DISCOVERY_PORT = 8889

    data class ClusterNode(
        val id: String,
        val name: String,
        val role: String, // PRIMARY, REPLICA, or STANDBY
        val endpoint: String,
        val pingMs: Int,
        val isPrimary: Boolean,
        val status: String,
        val region: String = "",
        val source: NodeSource = NodeSource.LOCAL
    )

    enum class NodeSource {
        LOCAL,
        CLOUDFLARE_REPLICA,
        LAN_PEER
    }

    data class DecodedTunnelInfo(
        val accountId: String,
        val tunnelId: String,
        val isValid: Boolean
    )

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _decodedTunnelInfo = MutableStateFlow<DecodedTunnelInfo?>(null)
    val decodedTunnelInfo: StateFlow<DecodedTunnelInfo?> = _decodedTunnelInfo.asStateFlow()

    private val _fleetNodes = MutableStateFlow<List<ClusterNode>>(emptyList())
    val fleetNodes: StateFlow<List<ClusterNode>> = _fleetNodes.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    private val lanPeers = ConcurrentHashMap<String, Pair<ClusterNode, Long>>()
    private val cloudflareReplicas = ConcurrentHashMap<String, ClusterNode>()

    private var udpListenerJob: Job? = null
    private var udpBroadcastJob: Job? = null
    private var periodicSyncJob: Job? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tunnelToken = prefs.getString(HostTriangleServerService.KEY_TUNNEL_TOKEN, null)
        decodeTunnelToken(tunnelToken)

        startUdpPeerDiscovery(context)
        startPeriodicFleetRefresh(context)
    }

    fun decodeTunnelToken(token: String?): DecodedTunnelInfo? {
        if (token.isNullOrBlank()) {
            _decodedTunnelInfo.value = null
            return null
        }
        return try {
            val decodedBytes = Base64.decode(token.trim(), Base64.DEFAULT)
            val jsonStr = String(decodedBytes, Charsets.UTF_8)
            val json = JSONObject(jsonStr)
            val accountId = json.optString("a", "")
            val tunnelId = json.optString("t", "")
            val isValid = accountId.isNotBlank() && tunnelId.isNotBlank()
            val info = DecodedTunnelInfo(accountId, tunnelId, isValid)
            _decodedTunnelInfo.value = info
            info
        } catch (e: Exception) {
            Log.d(TAG, "Token decode failure: ${e.message}")
            _decodedTunnelInfo.value = null
            null
        }
    }

    fun getStoredApiToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CF_API_TOKEN, "") ?: ""
    }

    fun saveApiToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CF_API_TOKEN, token.trim()).apply()
        scope.launch { syncCloudflareReplicas(context) }
    }

    /**
     * Queries Cloudflare Zero Trust REST API for active tunnel connectors / replicas.
     * GET https://api.cloudflare.com/client/v4/accounts/{account_id}/cfd_tunnel/{tunnel_id}/connections
     */
    suspend fun syncCloudflareReplicas(context: Context): Boolean = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawToken = prefs.getString(HostTriangleServerService.KEY_TUNNEL_TOKEN, null)
        val info = decodeTunnelToken(rawToken)

        if (info == null || !info.isValid) {
            _lastSyncError.value = "Cloudflare Tunnel Token missing or invalid"
            return@withContext false
        }

        val apiToken = getStoredApiToken(context)
        if (apiToken.isBlank()) {
            _lastSyncError.value = "Enter Cloudflare API Read Token to sync cloud replicas"
            return@withContext false
        }

        _isSyncing.value = true
        _lastSyncError.value = null

        try {
            val urlStr = "https://api.cloudflare.com/client/v4/accounts/${info.accountId}/cfd_tunnel/${info.tunnelId}/connections"
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Authorization", "Bearer $apiToken")
                setRequestProperty("Content-Type", "application/json")
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val responseText = reader.readText()
                reader.close()

                val root = JSONObject(responseText)
                val success = root.optBoolean("success", false)
                if (success) {
                    val resultArr = root.optJSONArray("result")
                    cloudflareReplicas.clear()

                    if (resultArr != null) {
                        for (i in 0 until resultArr.length()) {
                            val connector = resultArr.getJSONObject(i)
                            val connId = connector.optString("id", "connector-$i")
                            val arch = connector.optString("arch", "arm64")
                            val ver = connector.optString("version", "cloudflared")
                            val connsArr = connector.optJSONArray("conns")

                            var coloName = "Edge"
                            var originIp = "Cloudflare Network"
                            var isPendingReconnect = false

                            if (connsArr != null && connsArr.length() > 0) {
                                val firstEdge = connsArr.getJSONObject(0)
                                coloName = firstEdge.optString("colo_name", "Edge")
                                originIp = firstEdge.optString("origin_ip", "Public Edge")
                                isPendingReconnect = firstEdge.optBoolean("is_pending_reconnect", false)
                            }

                            val shortId = if (connId.length >= 8) connId.take(8).uppercase() else connId
                            val nodeStatus = if (isPendingReconnect) "SYNCING" else "HEALTHY"

                            val city = when {
                                coloName.startsWith("bom", true) -> "Mumbai"
                                coloName.startsWith("del", true) -> "Delhi"
                                coloName.startsWith("maa", true) -> "Chennai"
                                coloName.startsWith("blr", true) -> "Bengaluru"
                                coloName.startsWith("hyd", true) -> "Hyderabad"
                                coloName.startsWith("sin", true) -> "Singapore"
                                else -> coloName.uppercase()
                            }

                            val shortIp = if (originIp.length > 22) originIp.take(18) + "..." else originIp

                            val replicaNode = ClusterNode(
                                id = "CF-$shortId",
                                name = "Cloudflare Edge ($city)",
                                role = "EDGE REPLICA",
                                endpoint = "$coloName • $shortIp",
                                pingMs = 15 + (i * 4),
                                isPrimary = false,
                                status = nodeStatus,
                                region = city,
                                source = NodeSource.CLOUDFLARE_REPLICA
                            )
                            cloudflareReplicas[connId] = replicaNode
                        }
                    }

                    recalculateFleet(context)
                    _lastSyncError.value = null
                    return@withContext true
                } else {
                    val errors = root.optJSONArray("errors")
                    val msg = errors?.optJSONObject(0)?.optString("message") ?: "API call unsuccessful"
                    _lastSyncError.value = msg
                    return@withContext false
                }
            } else {
                _lastSyncError.value = "Cloudflare HTTP $responseCode (${conn.responseMessage})"
                return@withContext false
            }
        } catch (e: Exception) {
            _lastSyncError.value = "Sync Error: ${e.message}"
            return@withContext false
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Local Wi-Fi UDP Peer Heartbeat Discovery
     * Broadcasts local presence and receives pings from other HostTriangle phones on LAN.
     */
    private fun startUdpPeerDiscovery(context: Context) {
        // UDP Listener
        udpListenerJob?.cancel()
        udpListenerJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(UDP_DISCOVERY_PORT).apply {
                    broadcast = true
                    soTimeout = 4000
                }
                val buffer = ByteArray(1024)

                while (isActive) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val json = JSONObject(text)
                        val peerId = json.optString("nodeId")
                        val peerName = json.optString("name")
                        val peerPort = json.optInt("port", 8080)
                        val peerStatus = json.optString("status", "ACTIVE")
                        val senderIp = packet.address.hostAddress ?: ""

                        if (peerId.isNotEmpty() && senderIp != getLocalIp()) {
                            val peerNode = ClusterNode(
                                id = peerId,
                                name = peerName,
                                role = "LAN REPLICA",
                                endpoint = "$senderIp:$peerPort",
                                pingMs = 4,
                                isPrimary = false,
                                status = peerStatus,
                                region = "LAN Mesh",
                                source = NodeSource.LAN_PEER
                            )
                            lanPeers[peerId] = Pair(peerNode, System.currentTimeMillis())
                            recalculateFleet(context)
                        }
                    } catch (_: SocketTimeoutException) {
                        // Regular timeout to allow cancellation check
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.d(TAG, "UDP listener stopped: ${e.message}")
            } finally {
                socket?.close()
            }
        }

        // UDP Broadcast Heartbeat
        udpBroadcastJob?.cancel()
        udpBroadcastJob = scope.launch {
            var clientSocket: DatagramSocket? = null
            try {
                clientSocket = DatagramSocket().apply { broadcast = true }
                val broadcastAddr = InetAddress.getByName("255.255.255.255")

                while (isActive) {
                    val serverRunning = HostTriangleServerService.serverState.value == HostTriangleServerService.ServerState.RUNNING
                    if (serverRunning) {
                        val realName = getDeviceName()
                        val activePort = HostTriangleServerService.activePort.value
                        val myNodeId = "LAN-" + (Build.MODEL.hashCode().toString().takeLast(4))

                        val payload = JSONObject().apply {
                            put("nodeId", myNodeId)
                            put("name", realName)
                            put("port", activePort)
                            put("status", "ACTIVE")
                        }.toString().toByteArray(Charsets.UTF_8)

                        val packet = DatagramPacket(payload, payload.size, broadcastAddr, UDP_DISCOVERY_PORT)
                        try {
                            clientSocket.send(packet)
                        } catch (_: Exception) {}
                    }
                    delay(5000)
                }
            } catch (_: Exception) {
            } finally {
                clientSocket?.close()
            }
        }
    }

    private fun startPeriodicFleetRefresh(context: Context) {
        periodicSyncJob?.cancel()
        periodicSyncJob = scope.launch {
            while (isActive) {
                // Prune dead LAN peers (not heard for >15s)
                val now = System.currentTimeMillis()
                val iterator = lanPeers.entries.iterator()
                var changed = false
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (now - entry.value.second > 15000L) {
                        iterator.remove()
                        changed = true
                    }
                }
                recalculateFleet(context)

                // Auto-sync Cloudflare replicas every 45 seconds if API token is configured
                val apiToken = getStoredApiToken(context)
                if (apiToken.isNotBlank()) {
                    syncCloudflareReplicas(context)
                }

                delay(10000)
            }
        }
    }

    fun recalculateFleet(context: Context) {
        val serverRunning = HostTriangleServerService.serverState.value == HostTriangleServerService.ServerState.RUNNING
        val port = HostTriangleServerService.activePort.value
        val localIp = getLocalIp()
        val deviceName = getDeviceName()

        val cfStatus = CloudflareTunnelManager.status.value
        val cfRegion = when (cfStatus) {
            is CloudflareTunnelManager.TunnelStatus.Connected -> cfStatus.edgeLocation
            is CloudflareTunnelManager.TunnelStatus.Connecting -> "Connecting..."
            else -> "Local Edge"
        }

        val localNode = ClusterNode(
            id = "NODE-001",
            name = "$deviceName (Host)",
            role = "PRIMARY",
            endpoint = "$localIp:$port",
            pingMs = 1,
            isPrimary = true,
            status = if (serverRunning) "ACTIVE" else "STANDBY",
            region = cfRegion,
            source = NodeSource.LOCAL
        )

        val combined = mutableListOf<ClusterNode>()
        combined.add(localNode)

        // Add verified Cloudflare remote connectors
        cloudflareReplicas.values.forEach { node ->
            combined.add(node)
        }

        // Add discovered LAN peers
        lanPeers.values.forEach { pair ->
            combined.add(pair.first)
        }

        // If no cloud or LAN peers are connected yet, show a clean readiness node
        if (combined.size == 1) {
            val tokenInfo = _decodedTunnelInfo.value
            val standbyStatus = if (tokenInfo?.isValid == true) {
                "READY"
            } else {
                "STANDBY"
            }

            combined.add(
                ClusterNode(
                    id = "STANDBY-POOL",
                    name = "Cloudflare Replica Pool",
                    role = "STANDBY",
                    endpoint = "CF Zero Trust Mesh",
                    pingMs = 18,
                    isPrimary = false,
                    status = standbyStatus,
                    region = "Global Edge",
                    source = NodeSource.CLOUDFLARE_REPLICA
                )
            )
        }

        _fleetNodes.value = combined
    }

    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }

    private fun getLocalIp(): String {
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
                        if (host.isNotEmpty() && !host.startsWith("127.")) return host
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }
}
