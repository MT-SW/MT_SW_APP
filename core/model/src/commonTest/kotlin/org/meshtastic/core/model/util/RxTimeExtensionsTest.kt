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

import org.meshtastic.proto.MeshPacket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RxTimeExtensionsTest {

    private fun loraPacket(rxTime: Int?) = MeshPacket.Builder()
        .also { wb ->
            wb.rx_time = rxTime
            wb.hop_start = 3
            wb.hop_limit = 3
            wb.transport_mechanism = MeshPacket.TransportMechanism.TRANSPORT_LORA
        }
        .build()

    @Test
    fun `rxTimeOrNull returns the stamped time`() {
        assertEquals(1_700_000_000, loraPacket(1_700_000_000).rxTimeOrNull())
    }

    @Test
    fun `rxTimeOrNull treats an absent field as unknown`() {
        assertNull(loraPacket(null).rxTimeOrNull())
    }

    @Test
    fun `rxTimeOrNull treats an old-firmware zero as unknown`() {
        assertNull(loraPacket(0).rxTimeOrNull())
    }

    @Test
    fun `isDirectSignal requires a known arrival time`() {
        assertTrue(loraPacket(1_700_000_000).isDirectSignal())
        assertFalse(loraPacket(null).isDirectSignal())
        assertFalse(loraPacket(0).isDirectSignal())
    }

    private fun packet(hopStart: Int, hopLimit: Int, transport: MeshPacket.TransportMechanism?) = MeshPacket.Builder()
        .also { wb ->
            wb.hop_start = hopStart
            wb.hop_limit = hopLimit
            if (transport != null) wb.transport_mechanism = transport
        }
        .build()

    @Test
    fun `isMeasuredDirect accepts a packet without a transport or an arrival time`() {
        assertTrue(packet(3, 3, null).isMeasuredDirect())
        assertTrue(packet(3, 3, MeshPacket.TransportMechanism.TRANSPORT_LORA).isMeasuredDirect())
        assertFalse(packet(3, 3, null).isDirectSignal())
    }

    @Test
    fun `isMeasuredDirect leaves out relayed packets and other transports`() {
        assertFalse(packet(3, 2, null).isMeasuredDirect())
        assertFalse(packet(0, 0, null).isMeasuredDirect())
        assertFalse(packet(3, 3, MeshPacket.TransportMechanism.TRANSPORT_MQTT).isMeasuredDirect())
        assertFalse(packet(3, 3, MeshPacket.TransportMechanism.TRANSPORT_API).isMeasuredDirect())
    }
}
