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
import org.meshtastic.feature.settings.debugging.sanitizeForExport
import org.meshtastic.feature.settings.util.decodePayloadFromMeshLog

/** One packet the locally connected node overheard but which was not addressed to it. */
data class SniffedPacket(
    val fromId: String,
    val toId: String,
    val fromShortName: String?,
    val toShortName: String?,
    val channel: Int,
    val hopStart: Int,
    val hopLimit: Int,
    val rssi: Int?,
    val snr: Float,
    val portNum: Int?,
    val isEncrypted: Boolean,
    val payloadHex: String,
    val decodedPayload: String?,
    val receivedAtMillis: Long,
) {
    /** Multi-line, redacted representation for the per-packet copy action — reuses the Debug Panel's redaction. */
    val copyText: String
        get() = sanitizeForExport(
            buildString {
                appendLine("$fromId → $toId")
                appendLine("ch $channel • hops ${hopStart - hopLimit}/$hopStart")
                appendLine(listOfNotNull(rssi?.let { "RSSI $it" }, "SNR $snr").joinToString(" • "))
                appendLine(if (isEncrypted) "encrypted" else portNum?.let { "port $it" } ?: "unknown port")
                append(decodedPayload ?: payloadHex)
            },
        )
}

/**
 * Sniffer Log screen ViewModel, reached from Settings → Advanced. Sniffer mode has no distinguishing marker on the
 * wire — firmware's `sendPacketToPhoneRaw()` forwards a sniffed packet through the exact same `FromRadio.packet`
 * channel as ordinary traffic. So this decodes live from the same [MeshLog] stream as everything else and applies a
 * heuristic: any packet not addressed to the locally connected node was not meant for us, so it must have been
 * sniffed. By request, this screen shows the complete unfiltered packet log instead — including our own outgoing
 * requests and their responses — so a full request/response pair (e.g. traceroute) can be inspected together here.
 * Always reflects the locally connected node — there is no per-node variant of this screen.
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
        nodeRepository.myNodeInfo.value?.myNodeNum ?: return null

        val nodeMap = nodeRepository.nodeDBbyNum.value
        val decodedPayload = packet.decoded?.payload
        return SniffedPacket(
            fromId = NodeAddress.numToDefaultId(packet.from),
            toId = NodeAddress.numToDefaultId(packet.to),
            fromShortName = nodeMap[packet.from]?.user?.short_name?.takeIf { it.isNotBlank() },
            toShortName = nodeMap[packet.to]?.user?.short_name?.takeIf { it.isNotBlank() },
            channel = packet.channel,
            hopStart = packet.hop_start,
            hopLimit = packet.hop_limit,
            rssi = packet.rx_rssi,
            snr = packet.rx_snr,
            portNum = packet.decoded?.portnum?.value,
            isEncrypted = decodedPayload == null,
            payloadHex = (decodedPayload ?: packet.encrypted)?.hex().orEmpty(),
            decodedPayload = if (decodedPayload != null) decodePayloadFromMeshLog(log, nodeRepository) else null,
            receivedAtMillis = log.received_date,
        )
    }
}