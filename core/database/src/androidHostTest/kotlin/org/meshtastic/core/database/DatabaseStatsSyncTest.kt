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

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.meshtastic.core.database.entity.MeshLog
import org.meshtastic.core.database.entity.NodeEntity
import org.meshtastic.proto.FromRadio
import org.meshtastic.proto.MeshPacket
import org.meshtastic.proto.Telemetry
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DatabaseStatsSyncTest {
    private lateinit var a: MeshtasticDatabase
    private lateinit var b: MeshtasticDatabase

    @Before
    fun create() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        fun open() =
            Room.inMemoryDatabaseBuilder<MeshtasticDatabase>(
                context = context,
                factory = { MeshtasticDatabaseConstructor.initialize() },
            )
                .setDriver(BundledSQLiteDriver())
                .build()
        a = open()
        b = open()
    }

    @After
    fun close() {
        a.close()
        b.close()
    }

    private fun telemetry(time: Int) = Telemetry.Builder().also { it.time = time }.build()

    private fun log(id: Int, from: Int, port: Int) = MeshLog(
        uuid = "u$from-$id-${port}x${System.nanoTime()}",
        message_type = "Packet",
        received_date = 1L,
        raw_message = "",
        fromNum = from,
        portNum = port,
        fromRadio =
        FromRadio.Builder().also { it.packet = MeshPacket.Builder().also { p -> p.id = id }.build() }.build(),
    )

    @Test
    fun `each telemetry kind comes from the database with the later report, in both directions`() = runTest {
        // A heard the node last, but B has the newer device telemetry; A has the newer environment telemetry.
        a.nodeInfoDao()
            .putAll(
                listOf(
                    NodeEntity(
                        num = 7,
                        lastHeard = 200,
                        deviceTelemetry = telemetry(10),
                        environmentTelemetry = telemetry(90),
                    ),
                ),
            )
        b.nodeInfoDao()
            .putAll(
                listOf(
                    NodeEntity(
                        num = 7,
                        lastHeard = 100,
                        deviceTelemetry = telemetry(50),
                        environmentTelemetry = telemetry(20),
                    ),
                ),
            )

        DatabaseStatsSync.sync(a, b)

        val nodeA = a.nodeInfoDao().getAllNodesSnapshot().single()
        val nodeB = b.nodeInfoDao().getAllNodesSnapshot().single()
        assertEquals(50, nodeA.deviceTelemetry.time)
        assertEquals(90, nodeA.environmentTelemetry.time)
        assertEquals(50, nodeB.deviceTelemetry.time)
        assertEquals(90, nodeB.environmentTelemetry.time)
        // Each radio keeps when it heard the node.
        assertEquals(200, nodeA.lastHeard)
        assertEquals(100, nodeB.lastHeard)
    }

    @Test
    fun `a node known to one database only is not copied and the history is completed without duplicates`() = runTest {
        a.nodeInfoDao().putAll(listOf(NodeEntity(num = 1, lastHeard = 5), NodeEntity(num = 2, lastHeard = 5)))
        b.nodeInfoDao().putAll(listOf(NodeEntity(num = 1, lastHeard = 5)))
        a.meshLogDao().insertIgnore(listOf(log(id = 11, from = 1, port = 67), log(id = 12, from = 1, port = 67)))
        b.meshLogDao().insertIgnore(listOf(log(id = 12, from = 1, port = 67), log(id = 13, from = 1, port = 67)))

        DatabaseStatsSync.sync(a, b)
        val written = DatabaseStatsSync.sync(a, b)

        assertEquals(setOf(1), b.nodeInfoDao().getAllNodesSnapshot().map { it.num }.toSet())
        assertEquals(3, a.meshLogDao().getLogsFromNodesOnPorts(listOf(1), listOf(67)).size)
        assertEquals(3, b.meshLogDao().getLogsFromNodesOnPorts(listOf(1), listOf(67)).size)
        assertEquals(0, written)
    }
}
