/*
 * Copyright (c) 2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.meshtastic.core.service.background

import co.touchlab.kermit.Logger
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.database.DatabaseManager
import org.meshtastic.core.database.entity.NodeEntity
import org.meshtastic.core.database.entity.Packet
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.textMentionsNode
import org.meshtastic.core.model.util.MeshDataMapper
import org.meshtastic.core.model.util.NodeIdLookup
import org.meshtastic.core.model.util.rxTimeOrNull
import org.meshtastic.core.model.util.snrOrNull
import org.meshtastic.core.repository.MeshNotificationManager
import org.meshtastic.core.repository.deviceNotificationTitle
import org.meshtastic.core.repository.isSenderMuted
import org.meshtastic.proto.MeshPacket
import org.meshtastic.proto.PortNum
import org.meshtastic.proto.Position
import org.meshtastic.proto.Telemetry
import org.meshtastic.proto.User

/**
 * What a device that is kept connected in the background does with the packets it hears (MT_SW), next to the log entry:
 * a text message or alert goes to the database of that device and raises a notification that carries the device name;
 * node names, positions and telemetry update the node list of that device. When the app shows the device later,
 * everything is already there. [deviceName] and [onMessage] are asked fresh each time, so a renamed device and the
 * unread counter stay current. Errors only go to the log.
 */
class BackgroundPacketProcessor(
    private val address: String,
    private val databaseManager: DatabaseManager,
    private val notifier: MeshNotificationManager,
    private val deviceName: () -> String,
    private val onMessage: () -> Unit,
    private val senderMutes: () -> Map<String, Long> = { emptyMap() },
    private val isFiltered: (String, Boolean) -> Boolean = { _, _ -> false },
) {
    private val mapper =
        MeshDataMapper(
            object : NodeIdLookup {
                override fun toNodeID(nodeNum: Int): String = if (nodeNum == NodeAddress.NODENUM_BROADCAST) {
                    NodeAddress.ID_BROADCAST
                } else {
                    NodeAddress.numToDefaultId(nodeNum)
                }
            },
        )

    /** Handles [packet] heard by the device whose own node number is [myNodeNum]. */
    @Suppress("TooGenericExceptionCaught")
    suspend fun process(myNodeNum: Int, packet: MeshPacket) {
        val decoded = packet.decoded ?: return
        try {
            when (decoded.portnum) {
                PortNum.TEXT_MESSAGE_APP,
                PortNum.ALERT_APP,
                -> text(myNodeNum, packet)

                PortNum.NODEINFO_APP -> user(packet)

                PortNum.POSITION_APP -> position(packet)

                PortNum.TELEMETRY_APP -> telemetry(packet)

                else -> Unit
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "Background device: a packet could not be handled: ${e.message}" }
        }
    }

    @Suppress("ReturnCount")
    private suspend fun text(myNodeNum: Int, packet: MeshPacket) {
        val decoded = packet.decoded ?: return
        // Our own messages and reactions (a reply with an emoji) are not new messages for this list.
        if (packet.from == myNodeNum || (decoded.reply_id != 0 && decoded.emoji != 0)) return
        val data = mapper.toDataPacket(packet) ?: return
        data.status = MessageStatus.RECEIVED
        val isAlert = decoded.portnum == PortNum.ALERT_APP
        val body = (if (isAlert) data.alert else data.text) ?: return
        val isBroadcast = data.to == NodeAddress.ID_BROADCAST
        val contactKey = "${data.channel}${if (isBroadcast) data.to else data.from}"
        var sender: String? = null
        var muted = false
        var stored = false
        var filtered = false
        databaseManager.withDeviceDb(address, includeActive = true) { db ->
            val node = db.nodeInfoDao().getNodeEntitiesByNums(listOf(packet.from)).firstOrNull()
            if (node?.isIgnored == true) return@withDeviceDb
            if (db.packetDao().findPacketsWithId(data.id).any { it.data.from == data.from }) return@withDeviceDb
            sender = node?.displayName?.ifBlank { null } ?: node?.longName ?: node?.user?.long_name?.ifBlank { null }
            val settings = db.packetDao().getContactSettings(contactKey)
            // Like on the device in use: a muted node or sender stays muted, a muted conversation is broken through
            // by a message that mentions this device.
            val mentionsMe = textMentionsNode(body, NodeAddress.numToDefaultId(myNodeNum))
            val senderMuted = senderMutes().isSenderMuted(contactKey, packet.from, nowMillis)
            val conversationMuted = settings?.isMuted == true
            muted = node?.isMuted == true || senderMuted || (conversationMuted && !mentionsMe)
            filtered = !isAlert && isFiltered(body, settings?.filteringDisabled == true)
            db.packetDao()
                .insertAndGetId(
                    Packet(
                        uuid = 0L,
                        myNodeNum = myNodeNum,
                        port_num = data.dataType,
                        contact_key = contactKey,
                        received_time = nowMillis,
                        read = false,
                        data = data,
                        packetId = data.id,
                        snr = data.snr,
                        rssi = data.rssi,
                        hopsAway = data.hopsAway,
                        messageText = body,
                        filtered = filtered,
                    ),
                )
            stored = true
        }
        if (!stored || filtered) return
        onMessage()
        if (muted) return
        if (isAlert) {
            // A critical alert breaks through like on the device in use: its own notification, with the sender's name.
            notifier.showAlertNotification(
                contactKey,
                deviceNotificationTitle(deviceName(), sender ?: data.from.orEmpty()),
                body,
            )
            return
        }
        notifier.showBackgroundMessageNotification(
            deviceAddress = address,
            deviceName = deviceName(),
            contactKey = contactKey,
            sender = sender ?: data.from.orEmpty(),
            text = body,
            isBroadcast = isBroadcast,
            channelName = null,
        )
    }

    private suspend fun user(packet: MeshPacket) {
        val payload = packet.decoded?.payload ?: return
        val user = User.ADAPTER.decode(payload)
        updateNode(packet, create = true) { node ->
            node.user = user
            node.longName = user.long_name
            node.shortName = user.short_name
        }
    }

    private suspend fun position(packet: MeshPacket) {
        val payload = packet.decoded?.payload ?: return
        val position = Position.ADAPTER.decode(payload)
        val lat = position.latitude_i ?: 0
        val lon = position.longitude_i ?: 0
        if (lat == 0 && lon == 0) return
        updateNode(packet, create = false) { node ->
            node.position = position
            node.latitude = lat * COORDINATE_SCALE
            node.longitude = lon * COORDINATE_SCALE
        }
    }

    private suspend fun telemetry(packet: MeshPacket) {
        val payload = packet.decoded?.payload ?: return
        val telemetry = Telemetry.ADAPTER.decode(payload)
        updateNode(packet, create = false) { node ->
            when {
                telemetry.device_metrics != null -> node.deviceTelemetry = telemetry
                telemetry.environment_metrics != null -> node.environmentTelemetry = telemetry
                telemetry.power_metrics != null -> node.powerTelemetry = telemetry
                telemetry.air_quality_metrics != null -> node.airQualityTelemetry = telemetry
            }
        }
    }

    /** Applies [change] to the node that sent [packet] in the database of this device and refreshes its reception. */
    private suspend fun updateNode(packet: MeshPacket, create: Boolean, change: (NodeEntity) -> Unit) {
        databaseManager.withDeviceDb(address, includeActive = true) { db ->
            val dao = db.nodeInfoDao()
            val existing = dao.getNodeEntitiesByNums(listOf(packet.from)).firstOrNull()
            if (existing == null && !create) return@withDeviceDb
            val node = existing ?: NodeEntity(num = packet.from)
            change(node)
            packet.rxTimeOrNull()?.let { node.lastHeard = it }
            packet.snrOrNull()?.let { node.snr = it }
            packet.rx_rssi?.let { node.rssi = it }
            node.channel = packet.channel
            dao.upsert(node)
        }
    }

    private companion object {
        const val COORDINATE_SCALE = 1e-7
    }
}
