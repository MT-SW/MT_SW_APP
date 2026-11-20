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
package org.meshtastic.core.database

import co.touchlab.kermit.Logger
import org.meshtastic.core.database.entity.MeshLog
import org.meshtastic.core.database.entity.NodeEntity
import org.meshtastic.proto.Telemetry

/**
 * Keeps the statistics of the nodes that two device databases both know equal. Each radio has its own database, so the
 * same node (heard by two radios) has its own telemetry history in each. Only nodes present in BOTH databases are
 * touched: a node known to one database only is never copied to the other.
 *
 * For such a node each telemetry kind and the position come from the database that has the later report, the names and
 * key from the one that heard the node last, while the reception data of each radio (signal, hops, channel, route, last
 * heard) and what the user wrote (notes, favourite, ignore, mute) stay as they were in each database. The history of
 * telemetry, position, neighbours, traceroute and map reports of the node is completed in both directions, without
 * duplicates (a packet heard by both radios counts once).
 */
object DatabaseStatsSync {
    // Position, paxcounter, telemetry, traceroute, neighbour info and map report (PortNum values): everything the
    // other nodes report about themselves, so the map, the neighbours and the charts match on every radio.
    private val PORTS = listOf(3, 34, 67, 70, 71, 73)
    private const val CHUNK = 400

    /**
     * Synchronizes [a] and [b] both ways; returns how many rows were written (nodes + history entries). With
     * [requireSameRadioSettings] it only runs when both radios are known to be on the same radio settings (region,
     * preset, frequency): radios on different settings hear different things, so their data must not be mixed.
     */
    suspend fun sync(a: MeshtasticDatabase, b: MeshtasticDatabase, requireSameRadioSettings: Boolean = false): Int {
        if (requireSameRadioSettings && !sameRadioSettings(a, b)) {
            Logger.d { "Statistics sync paused: the two devices are not (known to be) on the same radio settings" }
            return 0
        }
        val nodesA = a.nodeInfoDao().getAllNodesSnapshot().associateBy { it.num }
        val nodesB = b.nodeInfoDao().getAllNodesSnapshot().associateBy { it.num }
        val common = nodesA.keys.intersect(nodesB.keys).toList()
        if (common.isEmpty()) return 0
        var written = 0
        val toA = ArrayList<NodeEntity>()
        val toB = ArrayList<NodeEntity>()
        for (num in common) {
            val na = nodesA.getValue(num)
            val nb = nodesB.getValue(num)
            // Only a row that really changes is written, so a repeated sync does not rewrite the same nodes again.
            merged(own = na, other = nb).takeIf { it != na }?.let(toA::add)
            merged(own = nb, other = na).takeIf { it != nb }?.let(toB::add)
        }
        if (toA.isNotEmpty()) a.nodeInfoDao().putAll(toA)
        if (toB.isNotEmpty()) b.nodeInfoDao().putAll(toB)
        written += toA.size + toB.size
        for (part in common.chunked(CHUNK)) {
            val logsA = a.meshLogDao().getLogsFromNodesOnPorts(part, PORTS)
            val logsB = b.meshLogDao().getLogsFromNodesOnPorts(part, PORTS)
            val keysA = logsA.mapTo(HashSet()) { key(it) }
            val keysB = logsB.mapTo(HashSet()) { key(it) }
            val missingInA = logsB.filter { key(it) !in keysA }
            val missingInB = logsA.filter { key(it) !in keysB }
            if (missingInA.isNotEmpty()) a.meshLogDao().insertIgnore(missingInA.map(::withoutReception))
            if (missingInB.isNotEmpty()) b.meshLogDao().insertIgnore(missingInB.map(::withoutReception))
            written += missingInA.size + missingInB.size
        }
        if (written > 0) Logger.i { "Statistics sync between two device databases: $written entries" }
        return written
    }

    private suspend fun sameRadioSettings(a: MeshtasticDatabase, b: MeshtasticDatabase): Boolean {
        val loraA = a.channelSetDao().get()?.channelSet?.lora_config ?: return false
        val loraB = b.channelSetDao().get()?.channelSet?.lora_config ?: return false
        return loraA.region == loraB.region &&
            loraA.use_preset == loraB.use_preset &&
            loraA.modem_preset == loraB.modem_preset &&
            loraA.bandwidth == loraB.bandwidth &&
            loraA.spread_factor == loraB.spread_factor &&
            loraA.coding_rate == loraB.coding_rate &&
            loraA.frequency_offset == loraB.frequency_offset &&
            loraA.override_frequency == loraB.override_frequency &&
            loraA.channel_num == loraB.channel_num
    }

    /** What one radio heard (signal strength and SNR) is never copied to another radio: it would falsify its charts. */
    private fun withoutReception(log: MeshLog): MeshLog {
        val packet = log.fromRadio.packet ?: return log
        val cleaned = packet.newBuilder().also {
            it.rx_rssi = null
            it.rx_snr = Float.NaN
        }.build()
        return log.copy(fromRadio = log.fromRadio.newBuilder().also { it.packet = cleaned }.build())
    }

    /** One packet is the same packet in both databases when sender, port and packet id agree. */
    private fun key(log: MeshLog): String {
        val id = log.meshPacket?.id ?: 0
        return if (id != 0) "${log.fromNum}|${log.portNum}|$id" else log.uuid
    }

    /**
     * [own] with whatever [other] knows newer. Each telemetry kind and the position are taken from the row that has the
     * later report time (a node heard more recently is not necessarily the one with the newer telemetry); the identity
     * (names, model, key, status) comes from the row that heard the node last. What belongs to how THIS radio heard the
     * node (signal, hops, channel, route, when it was heard) and what the user wrote in this database stays as it was,
     * so a radio keeps its own reception data exactly as the original app records it.
     */
    private fun merged(own: NodeEntity, other: NodeEntity): NodeEntity {
        val otherHeardLater = other.lastHeard > own.lastHeard
        val newerPosition = other.position.time > own.position.time
        val identity = if (otherHeardLater) other else own
        return own.copy(
            user = identity.user,
            longName = identity.longName,
            shortName = identity.shortName,
            publicKey = identity.publicKey,
            nodeStatus = identity.nodeStatus,
            signsPackets = identity.signsPackets,
            paxcounter = identity.paxcounter,
            position = if (newerPosition) other.position else own.position,
            latitude = if (newerPosition) other.latitude else own.latitude,
            longitude = if (newerPosition) other.longitude else own.longitude,
            deviceTelemetry = newer(own.deviceTelemetry, other.deviceTelemetry),
            environmentTelemetry = newer(own.environmentTelemetry, other.environmentTelemetry),
            powerTelemetry = newer(own.powerTelemetry, other.powerTelemetry),
            airQualityTelemetry = newer(own.airQualityTelemetry, other.airQualityTelemetry),
            soilWaterTelemetry = newer(own.soilWaterTelemetry, other.soilWaterTelemetry),
        )
    }

    private fun newer(own: Telemetry, other: Telemetry): Telemetry = if (other.time > own.time) other else own
}
