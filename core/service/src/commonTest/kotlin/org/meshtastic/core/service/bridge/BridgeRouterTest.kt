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
package org.meshtastic.core.service.bridge

import org.meshtastic.core.repository.HandshakeConstants
import org.meshtastic.core.repository.RadioBridgeConfig
import org.meshtastic.proto.Data
import org.meshtastic.proto.FromRadio
import org.meshtastic.proto.MeshPacket
import org.meshtastic.proto.MyNodeInfo
import org.meshtastic.proto.NodeInfo
import org.meshtastic.proto.PortNum
import org.meshtastic.proto.ToRadio
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BridgeRouterTest {
    private fun myInfo() = FromRadio.Builder().also { it.my_info = MyNodeInfo.Builder().build() }.build().encode()

    private fun complete(id: Int) = FromRadio.Builder().also { it.config_complete_id = id }.build().encode()

    private fun packetFrom(port: PortNum, requestId: Int = 0, id: Int = 7): ByteArray = FromRadio.Builder()
        .also {
            it.packet =
                MeshPacket.Builder()
                    .also { p ->
                        p.id = id
                        p.decoded =
                            Data.Builder()
                                .also { d ->
                                    d.portnum = port
                                    d.request_id = requestId
                                }
                                .build()
                    }
                    .build()
        }
        .build()
        .encode()

    private fun toRadio(port: PortNum, id: Int) = ToRadio.Builder()
        .also {
            it.packet =
                MeshPacket.Builder()
                    .also { p ->
                        p.id = id
                        p.decoded = Data.Builder().also { d -> d.portnum = port }.build()
                    }
                    .build()
        }
        .build()
        .encode()

    private fun wantConfig(nonce: Int) = ToRadio.Builder().also { it.want_config_id = nonce }.build().encode()

    private fun ready(router: BridgeRouter) {
        router.onFromRadio(myInfo(), emptySet())
        router.onFromRadio(complete(1), emptySet())
    }

    @Test
    fun `a client gets the copy of the configuration closed with its own number and nothing reaches the radio`() {
        val router = BridgeRouter()
        ready(router)

        val result = router.onClientFrame(1, wantConfig(99))

        assertNull(result.toRadio)
        assertEquals(2, result.replies.size)
        assertEquals(99, FromRadio.ADAPTER.decode(result.replies.last().payload).config_complete_id)
    }

    @Test
    fun `a client that asks early gets the configuration when it is complete`() {
        val router = BridgeRouter()
        router.onFromRadio(myInfo(), emptySet())
        assertTrue(router.onClientFrame(1, wantConfig(5)).replies.isEmpty())

        val out = router.onFromRadio(complete(1), setOf(1))

        assertEquals(2, out.size)
        assertTrue(out.all { it.clientId == 1 })
    }

    @Test
    fun `an answer to a client packet goes to that client only`() {
        val router = BridgeRouter()
        ready(router)
        router.onClientFrame(2, toRadio(PortNum.ADMIN_APP, id = 42))

        val out = router.onFromRadio(packetFrom(PortNum.ADMIN_APP, requestId = 42), setOf(1, 2))

        assertEquals(listOf(2), out.map { it.clientId })
    }

    @Test
    fun `administration that is not an answer to a client is never shown to clients`() {
        val router = BridgeRouter()
        ready(router)

        assertTrue(router.onFromRadio(packetFrom(PortNum.ADMIN_APP, requestId = 5), setOf(1, 2)).isEmpty())
    }

    @Test
    fun `an ordinary packet goes to every client`() {
        val router = BridgeRouter()
        ready(router)

        val out = router.onFromRadio(packetFrom(PortNum.TEXT_MESSAGE_APP), setOf(1, 2))

        assertEquals(setOf(1, 2), out.map { it.clientId }.toSet())
    }

    @Test
    fun `restrictions are off by default and each one blocks only what it names`() {
        val router = BridgeRouter()
        ready(router)
        assertNotNull(router.onClientFrame(1, toRadio(PortNum.ADMIN_APP, 1)).toRadio)
        assertNotNull(router.onClientFrame(1, toRadio(PortNum.TEXT_MESSAGE_APP, 2)).toRadio)

        router.config = RadioBridgeConfig(blockAdmin = true)
        assertNull(router.onClientFrame(1, toRadio(PortNum.ADMIN_APP, 3)).toRadio)
        assertNotNull(router.onClientFrame(1, toRadio(PortNum.TEXT_MESSAGE_APP, 4)).toRadio)

        router.config = RadioBridgeConfig(blockMessages = true)
        assertNotNull(router.onClientFrame(1, toRadio(PortNum.ADMIN_APP, 5)).toRadio)
        assertNull(router.onClientFrame(1, toRadio(PortNum.TEXT_MESSAGE_APP, 6)).toRadio)

        router.config = RadioBridgeConfig(readOnly = true)
        assertNull(router.onClientFrame(1, toRadio(PortNum.TEXT_MESSAGE_APP, 7)).toRadio)
    }

    @Test
    fun `a new handshake of the app starts the copy again`() {
        val router = BridgeRouter()
        ready(router)
        router.onFromRadio(myInfo(), setOf(1))

        assertTrue(!router.hasConfiguration)
    }

    @Test
    fun `a client that asks before any handshake was seen is reported as waiting without a handshake`() {
        val router = BridgeRouter()
        router.onClientFrame(1, wantConfig(5))

        assertTrue(router.waitingWithoutHandshake)

        router.onFromRadio(myInfo(), setOf(1))
        assertTrue(!router.waitingWithoutHandshake)
    }

    @Test
    fun `reset forgets the copy and the waiting clients`() {
        val router = BridgeRouter()
        ready(router)
        router.reset()

        assertTrue(!router.hasConfiguration)
        assertTrue(!router.waitingWithoutHandshake)
    }

    private fun nodeFrame(num: Int) =
        FromRadio.Builder().also { it.node_info = NodeInfo.Builder().also { n -> n.num = num }.build() }.build().encode()

    private fun stagedReady(router: BridgeRouter) {
        router.onFromRadio(myInfo(), emptySet())
        router.onFromRadio(complete(HandshakeConstants.CONFIG_NONCE), emptySet())
        router.onFromRadio(nodeFrame(10), emptySet())
        router.onFromRadio(nodeFrame(11), emptySet())
        router.onFromRadio(complete(HandshakeConstants.NODE_INFO_NONCE), emptySet())
    }

    @Test
    fun `the first stage gets the configuration without nodes and the second stage the nodes without configuration`() {
        val router = BridgeRouter()
        stagedReady(router)
        assertTrue(router.hasConfiguration)

        val first = router.onClientFrame(1, wantConfig(HandshakeConstants.CONFIG_NONCE)).replies
        assertEquals(2, first.size)
        assertNotNull(FromRadio.ADAPTER.decode(first.first().payload).my_info)

        val second = router.onClientFrame(1, wantConfig(HandshakeConstants.NODE_INFO_NONCE)).replies
        assertEquals(3, second.size)
        assertTrue(second.dropLast(1).all { FromRadio.ADAPTER.decode(it.payload).node_info != null })
        assertEquals(
            HandshakeConstants.NODE_INFO_NONCE,
            FromRadio.ADAPTER.decode(second.last().payload).config_complete_id,
        )
    }

    @Test
    fun `the end of the first stage alone does not make the copy whole`() {
        val router = BridgeRouter()
        router.onFromRadio(myInfo(), emptySet())
        router.onFromRadio(complete(HandshakeConstants.CONFIG_NONCE), emptySet())

        assertTrue(!router.hasConfiguration)
    }
}
