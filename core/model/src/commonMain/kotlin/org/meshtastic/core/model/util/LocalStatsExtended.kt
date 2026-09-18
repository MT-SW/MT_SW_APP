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
package org.meshtastic.core.model.util

import okio.ByteString
import org.meshtastic.proto.Telemetry

/**
 * Local device mesh statistics extension (heap, CPU, flash, PSRAM) beyond the standard [org.meshtastic.proto.LocalStats].
 *
 * Mirrors the `LocalStatsExtended` message (`local_stats_extended`, tag 20 in `Telemetry.variant`) sent by the fw+
 * firmware fork. This message is NOT part of the official `meshtastic/protobufs` artifact this app consumes, so
 * Wire's generated `Telemetry.ADAPTER` cannot parse it -- it is preserved as raw bytes in
 * [org.meshtastic.proto.Telemetry.unknownFields] instead. [decodeLocalStatsExtended] hand-decodes that byte range
 * rather than vendoring/forking the protobufs dependency.
 */
data class LocalStatsExtended(
    val memoryFreeCheap: Int = 0,
    val memoryTotal: Int = 0,
    val cpuUsagePercent: Int = 0,
    val flashUsedBytes: Int = 0,
    val flashTotalBytes: Int = 0,
    val memoryPsramFree: Int = 0,
    val memoryPsramTotal: Int = 0,
)

/** Convenience wrapper so existing call sites keep decoding straight off a [Telemetry] packet. */
fun Telemetry.decodeLocalStatsExtended(): LocalStatsExtended? = unknownFields.decodeLocalStatsExtended()

private const val LOCAL_STATS_EXTENDED_TAG = 20
private const val WIRE_TYPE_VARINT = 0
private const val WIRE_TYPE_LENGTH_DELIMITED = 2
private const val WIRE_TYPE_FIXED64 = 1
private const val WIRE_TYPE_FIXED32 = 5

/**
 * Scans these [ByteString] unknown fields (from [org.meshtastic.proto.Telemetry.unknownFields]) for the
 * `local_stats_extended` (tag 20) oneof entry and decodes its 7 uint32 sub-fields by hand. Returns null if the field
 * is absent (e.g. stock firmware) or malformed.
 */
fun ByteString.decodeLocalStatsExtended(): LocalStatsExtended? {
    val submessage = findLengthDelimitedField(toByteArray(), LOCAL_STATS_EXTENDED_TAG) ?: return null
    val fields = parseVarintFields(submessage)
    return LocalStatsExtended(
        memoryFreeCheap = fields[1]?.toInt() ?: 0,
        memoryTotal = fields[2]?.toInt() ?: 0,
        cpuUsagePercent = fields[3]?.toInt() ?: 0,
        flashUsedBytes = fields[4]?.toInt() ?: 0,
        flashTotalBytes = fields[5]?.toInt() ?: 0,
        memoryPsramFree = fields[6]?.toInt() ?: 0,
        memoryPsramTotal = fields[7]?.toInt() ?: 0,
    )
}

private fun readVarint(bytes: ByteArray, start: Int): Pair<Long, Int>? {
    var result = 0L
    var shift = 0
    var i = start
    while (i < bytes.size) {
        val b = bytes[i].toInt() and 0xFF
        result = result or ((b and 0x7F).toLong() shl shift)
        i++
        if (b and 0x80 == 0) return result to i
        shift += 7
        if (shift >= 64) return null
    }
    return null
}

private fun parseVarintFields(bytes: ByteArray): Map<Int, Long> {
    val result = mutableMapOf<Int, Long>()
    var i = 0
    while (i < bytes.size) {
        val (key, afterKey) = readVarint(bytes, i) ?: break
        val tag = (key shr 3).toInt()
        when ((key and 0x7).toInt()) {
            WIRE_TYPE_VARINT -> {
                val (value, afterValue) = readVarint(bytes, afterKey) ?: break
                result[tag] = value
                i = afterValue
            }
            WIRE_TYPE_LENGTH_DELIMITED -> {
                val (len, afterLen) = readVarint(bytes, afterKey) ?: break
                i = afterLen + len.toInt()
            }
            WIRE_TYPE_FIXED64 -> i = afterKey + 8
            WIRE_TYPE_FIXED32 -> i = afterKey + 4
            else -> break
        }
        if (i > bytes.size) break
    }
    return result
}

private fun findLengthDelimitedField(bytes: ByteArray, tag: Int): ByteArray? {
    var i = 0
    while (i < bytes.size) {
        val (key, afterKey) = readVarint(bytes, i) ?: return null
        val fieldTag = (key shr 3).toInt()
        when ((key and 0x7).toInt()) {
            WIRE_TYPE_VARINT -> {
                val (_, afterValue) = readVarint(bytes, afterKey) ?: return null
                i = afterValue
            }
            WIRE_TYPE_LENGTH_DELIMITED -> {
                val (len, afterLen) = readVarint(bytes, afterKey) ?: return null
                val end = afterLen + len.toInt()
                if (end > bytes.size) return null
                if (fieldTag == tag) return bytes.copyOfRange(afterLen, end)
                i = end
            }
            WIRE_TYPE_FIXED64 -> i = afterKey + 8
            WIRE_TYPE_FIXED32 -> i = afterKey + 4
            else -> return null
        }
    }
    return null
}
