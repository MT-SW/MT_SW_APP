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
import okio.ByteString.Companion.toByteString
import org.meshtastic.proto.Data

/**
 * Support for Meshtastic's "private app port" convention (see the `PRIVATE_APP = 256` comment in
 * portnums.proto): ports >= 256 are used by mutual agreement between firmware and client without adding a
 * named [org.meshtastic.proto.PortNum] enum entry, specifically so a client doesn't need to fork the official
 * protobufs just for one experimental port number (here: 354, used by the OnDemand/Sniffer protocol -- see
 * `ondemand.proto`).
 *
 * Wire's default `enum_class` codegen mode is a closed Kotlin enum: it cannot represent a `PortNum` value that
 * has no matching constant. On *decode*, Wire handles this gracefully already -- an out-of-range portnum is
 * redirected into the containing [Data] message's own [Data.unknownFields] (tag 1, varint) instead of throwing,
 * leaving `Data.portnum` at its default ([org.meshtastic.proto.PortNum.UNKNOWN_APP]). On *encode* there is no
 * built-in equivalent, so sending on a private port means constructing that same tag-1 varint by hand and
 * leaving `portnum` at its default (which proto3 omits from the wire, so there is no duplicate/conflicting tag).
 *
 * [Data.effectivePortNum] and [privatePortNumUnknownFields] are the read/write halves of that pattern.
 */
/** Port 354 -- OnDemand diagnostics + Sniffer control (`ondemand.proto`). No PortNum constant exists for it
  * (see the class doc above); this is what code should compare/send against instead. */
const val ON_DEMAND_PORT_NUM = 354

private const val DATA_PORTNUM_TAG = 1

/**
 * This packet's real port number, recovering it from [Data.unknownFields] when Wire couldn't represent it as a
 * [org.meshtastic.proto.PortNum] constant (see class doc above). Use this instead of `portnum.value` anywhere a
 * packet might be on a private app port -- logging/persistence in particular, since `portnum.value` alone would
 * silently collapse a private-port packet to 0 (UNKNOWN_APP).
 */
fun Data.effectivePortNum(): Int =
    portnum.value.takeIf { it != 0 } ?: unknownFields.readTopLevelVarintField(DATA_PORTNUM_TAG)?.toInt() ?: 0

/**
 * Builds the raw bytes for a private [Data.portnum] value, to pass as `Data(unknownFields = ..., ...)` when
 * sending on a port with no [org.meshtastic.proto.PortNum] constant (leave the `portnum` parameter itself at its
 * default). See the class doc above for why this works.
 */
fun privatePortNumUnknownFields(port: Int): ByteString = writeTopLevelVarintField(DATA_PORTNUM_TAG, port.toLong())

private fun writeTopLevelVarintField(tag: Int, value: Long): ByteString {
    val bytes = mutableListOf<Byte>()
    writeVarint(((tag shl 3) or 0).toLong(), bytes) // wire type 0 = varint
    writeVarint(value, bytes)
    return bytes.toByteArray().toByteString()
}

private fun writeVarint(value: Long, out: MutableList<Byte>) {
    var v = value
    while (true) {
        val b = (v and 0x7F).toInt()
        v = v ushr 7
        if (v == 0L) {
            out.add(b.toByte())
            return
        }
        out.add((b or 0x80).toByte())
    }
}

/** Reads the first top-level varint-typed field with the given [tag] out of these raw protobuf bytes. */
private fun ByteString.readTopLevelVarintField(tag: Int): Long? {
    val bytes = toByteArray()
    var i = 0
    while (i < bytes.size) {
        val (key, afterKey) = readVarint(bytes, i) ?: return null
        val fieldTag = (key shr 3).toInt()
        when ((key and 0x7).toInt()) {
            0 -> { // varint
                val (value, afterValue) = readVarint(bytes, afterKey) ?: return null
                if (fieldTag == tag) return value
                i = afterValue
            }
            1 -> i = afterKey + 8 // fixed64
            2 -> { // length-delimited
                val (len, afterLen) = readVarint(bytes, afterKey) ?: return null
                i = afterLen + len.toInt()
            }
            5 -> i = afterKey + 4 // fixed32
            else -> return null
        }
    }
    return null
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
