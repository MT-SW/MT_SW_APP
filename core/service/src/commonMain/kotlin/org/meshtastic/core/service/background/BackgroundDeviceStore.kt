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
import kotlinx.coroutines.channels.ReceiveChannel
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.database.DatabaseManager
import org.meshtastic.core.database.entity.ChannelSetEntity
import org.meshtastic.core.database.entity.MyNodeEntity
import org.meshtastic.core.database.entity.NodeEntity
import org.meshtastic.proto.ChannelSet
import org.meshtastic.proto.Config
import org.meshtastic.proto.DeviceMetadata
import org.meshtastic.proto.FromRadio
import org.meshtastic.core.model.TransportPhase
import org.meshtastic.proto.HardwareModel
import org.meshtastic.proto.MyNodeInfo
import org.meshtastic.proto.NodeInfo
import org.meshtastic.proto.Telemetry

/**
 * Keeps what a background radio tells about itself (MT_SW): its own info and its list of nodes go to that radio's own
 * database, which is created when it does not exist yet. So a device that was never shown in the app has the same data
 * as one that was: its name plate, its node list, and a place for its packets. The radio shown in the app is not
 * touched here (the app writes it itself).
 */
internal class BackgroundDeviceStore(
    private val address: String,
    private val databaseManager: DatabaseManager,
    private val onProgress: (String?) -> Unit = {},
    private val onPhase: (TransportPhase) -> Unit = {},
) {
    private var channelCount = 0
    private var myInfo: MyNodeInfo? = null
    private var metadata: DeviceMetadata? = null
    private var lora: Config.LoRaConfig? = null
    private val nodes = LinkedHashMap<Int, NodeInfo>()

    /** Reads the frames of the radio until the channel is closed; writes after every complete list. */
    suspend fun run(frames: ReceiveChannel<FromRadio>) {
        // The same steps the main connection card lists: the configuration, then the nodes, then done.
        onPhase(TransportPhase.HandshakeConfig)
        for (frame in frames) {
            val info = frame.my_info
            val node = frame.node_info
            when {
                info != null -> {
                    myInfo = info
                    metadata = null
                    nodes.clear()
                    channelCount = 0
                    onProgress("Requesting config…")
                }

                frame.metadata != null -> metadata = frame.metadata

                frame.config?.lora != null -> {
                    lora = frame.config?.lora
                    onProgress("Device config received")
                }

                frame.moduleConfig != null -> onProgress("Module config received")

                frame.channel != null -> onProgress("Channels (${++channelCount})")

                node != null -> {
                    if (nodes.isEmpty()) onPhase(TransportPhase.HandshakeNodes)
                    nodes[node.num] = node
                    onProgress("Nodes (${nodes.size})")
                }

                (frame.config_complete_id ?: 0) != 0 -> {
                    if (nodes.isNotEmpty()) write()
                    onPhase(TransportPhase.Connected)
                    onProgress(null)
                }
            }
        }
    }

    private suspend fun write() {
        val mine = myInfo ?: return
        val entityNodes = nodes.values.map { it.toNew() }
        val loraNow = lora
        val written =
            databaseManager.withDeviceDb(address, createIfMissing = true) { db ->
                // The radio settings (region, preset) are kept too: the statistics sync only runs between devices on
                // the same settings.
                if (loraNow != null) {
                    val old = db.channelSetDao().get()
                    val set =
                        (old?.channelSet ?: ChannelSet.Builder().build())
                            .newBuilder()
                            .also { it.lora_config = loraNow }
                            .build()
                    db.channelSetDao().upsert(old?.copy(channelSet = set) ?: ChannelSetEntity(channelSet = set))
                }
                val dao = db.nodeInfoDao()
                val existing = dao.getMyNodeEntity()
                when {
                    existing == null -> dao.installConfig(mine.toEntity(metadata, 0L), entityNodes)

                    // Another node number than the one kept: the app reconciles that itself when the device is shown.
                    existing.myNodeNum != mine.my_node_num -> Unit

                    else -> {
                        dao.setMyNodeInfo(mine.toEntity(metadata, existing.currentPacketId))
                        val known = dao.getNodeEntitiesByNums(nodes.keys.toList()).associateBy { it.num }
                        dao.putAll(
                            nodes.values.map { info -> known[info.num]?.also { info.mergeInto(it) } ?: info.toNew() },
                        )
                    }
                }
            }
        if (written == null) {
            Logger.d { "Background device: its own data was not written (the device is shown in the app)" }
        }
    }

    private fun MyNodeInfo.toEntity(metadata: DeviceMetadata?, packetId: Long) = MyNodeEntity(
        myNodeNum = my_node_num,
        model =
        when (val hw = metadata?.hw_model) {
            null,
            HardwareModel.UNSET,
            -> null

            else -> hw.name.replace('_', '-').replace('p', '.').lowercase()
        },
        firmwareVersion = metadata?.firmware_version?.takeIf { it.isNotBlank() },
        couldUpdate = false,
        shouldUpdate = false,
        currentPacketId = packetId,
        messageTimeoutMsec = MESSAGE_TIMEOUT_MS,
        minAppVersion = min_app_version,
        maxChannels = MAX_CHANNELS,
        hasWifi = metadata?.hasWifi == true,
        deviceId = device_id.hex().ifEmpty { null },
        pioEnv = pio_env.ifEmpty { null },
    )

    private fun NodeInfo.toNew(): NodeEntity = NodeEntity(num = num).also { mergeInto(it) }

    /** Puts what the radio says into [node]; what only the app knows (notes, names given by hand, ...) stays. */
    private fun NodeInfo.mergeInto(node: NodeEntity) {
        user?.let {
            node.user = it
            node.longName = it.long_name
            node.shortName = it.short_name
            it.public_key.takeIf { key -> key.size > 0 }?.let { key -> node.publicKey = key }
        }
        position?.let {
            node.position = it
            val lat = it.latitude_i ?: 0
            val lon = it.longitude_i ?: 0
            if (lat != 0 || lon != 0) {
                node.latitude = lat * COORDINATE_SCALE
                node.longitude = lon * COORDINATE_SCALE
            }
        }
        node.lastHeard = last_heard.coerceAtMost(nowSeconds.toInt())
        if (snr != 0f) node.snr = snr
        device_metrics?.let { node.deviceTelemetry = Telemetry.Builder().also { t -> t.device_metrics = it }.build() }
        node.channel = channel
        node.viaMqtt = via_mqtt
        node.hopsAway = hops_away ?: -1
        node.isFavorite = is_favorite
        node.isIgnored = is_ignored
        node.isMuted = is_muted
    }

    private companion object {
        const val COORDINATE_SCALE = 1e-7
        const val MESSAGE_TIMEOUT_MS = 300_000
        const val MAX_CHANNELS = 8
    }
}
