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

import okio.ByteString.Companion.toByteString
import org.meshtastic.proto.Data
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Foreign or broken packets (e.g. from other firmware) must never crash or hang the hand-written readers. */
class BrokenPacketSafetyTest {

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }.toByteString()

    // Tag 20, length-delimited, with a gigantic (overflowing) length.
    private val hugeLength = bytes(0xA2, 0x01, 0xFF, 0xFF, 0xFF, 0xFF, 0x0F, 0x01)

    @Test
    fun hugeLengthInExtendedStatsGivesNull() {
        assertNull(hugeLength.decodeLocalStatsExtended())
    }

    @Test
    fun negativeLengthInExtendedStatsGivesNull() {
        // length varint = 2^63 .. (10-byte) -> negative as Long
        val negative = bytes(0xA2, 0x01, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x01)
        assertNull(negative.decodeLocalStatsExtended())
    }

    @Test
    fun hugeLengthBeforePortNumGivesZero() {
        // field 2 (length-delimited) with an overflowing length, so the port number tag is never reached.
        val data = Data(unknownFields = bytes(0x12, 0xFF, 0xFF, 0xFF, 0xFF, 0x0F, 0x01))
        assertEquals(0, data.effectivePortNum())
    }

    @Test
    fun portNumStillReadFromUnknownFields() {
        val data = Data(unknownFields = privatePortNumUnknownFields(ON_DEMAND_PORT_NUM))
        assertEquals(ON_DEMAND_PORT_NUM, data.effectivePortNum())
    }
}
