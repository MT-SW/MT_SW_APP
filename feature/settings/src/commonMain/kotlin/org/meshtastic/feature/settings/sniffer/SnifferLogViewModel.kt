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
package org.meshtastic.feature.settings.sniffer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.model.MeshLog
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed

/** One packet the locally connected node overheard but which was not addressed to it. */
data class SniffedPacket(
    val fromId: String,
    val toId: String,
    val channel: Int,
    val hopStart: Int,
    val hopLimit: Int,
    val rssi: Int?,
    val snr: Float,
    val portNum: Int?,
    val isEncrypted: Boolean,
    val payloadHex: String,
    val receivedAtMillis: Long,
)

/**
 * Sniffer Log screen ViewModel, reached from Settings → Advanced. Sniffer mode has no distinguishing marker on the
 * wire — firmware's `sendPacketToPhoneRaw()` forwards a sniffed packet through the exact same `FromRadio.packet`
 * channel as ordinary traffic. So this decodes live from the same [MeshLog] stream as everything else and applies a
 * heuristic: any packet whose `to` is neither the locally connected node nor the broadcast address was not meant for
 * us, so it must have been sniffed. Local module-reply copies (`from == myNodeNum`) are rarer and not distinguished
 * from genuinely sniffed traffic in this first pass. Always reflects the locally connected node — there is no
 * per-node variant of this screen.
 */
@KoinViewModel
class SnifferLogViewModel(private val meshLogRepository: MeshLogRepository, private val nodeRepository: NodeRepository) :
    ViewModel() {

    val sniffedPackets: StateFlow<List<SniffedPacket>> =
        meshLogRepository
            .getAllLogs()
            .map { logs -> logs.mapNotNull(::toSniffedPacket).sortedByDescending { it.receivedAtMillis } }
            .stateInWhileSubscribed(initialValue = emptyList())

    private fun toSniffedPacket(log: MeshLog): SniffedPacket? {
        val packet = log.meshPacket ?: return null
        val myNodeNum = nodeRepository.myNodeInfo.value?.myNodeNum ?: return null
        if (packet.to == myNodeNum || packet.to == NodeAddress.NODENUM_BROADCAST) return null

        val decodedPayload = packet.decoded?.payload
        return SniffedPacket(
            fromId = NodeAddress.numToDefaultId(packet.from),
            toId = NodeAddress.numToDefaultId(packet.to),
            channel = packet.channel,
            hopStart = packet.hop_start,
            hopLimit = packet.hop_limit,
            rssi = packet.rx_rssi,
            snr = packet.rx_snr,
            portNum = packet.decoded?.portnum?.value,
            isEncrypted = decodedPayload == null,
            payloadHex = (decodedPayload ?: packet.encrypted)?.hex().orEmpty(),
            receivedAtMillis = log.received_date,
        )
    }
}