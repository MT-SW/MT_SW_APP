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

package org.meshtastic.feature.node.health

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkReportNodesTest {
    @Test
    fun nodeListAndPositionSketchAreInTheReport() {
        val state =
            NetworkSummaryUiState(
                isLoaded = true,
                nodeRows = listOf(NodeRow("Kielce", snr = 6.5f, batteryPercent = 80, lastHeardSeconds = 0, distanceMeters = 2500)),
                mapPoints = listOf(MapPoint("Kielce", 50.87, 20.63, isMe = true), MapPoint("Busko", 50.47, 20.72, isMe = false)),
            )
        val report = state.toReport("teraz")
        val list = report.sections.first { it.title.startsWith("Lista węzłów") }
        assertEquals("Kielce", list.lines.single().label)
        assertTrue("bateria 80%" in list.lines.single().value)
        assertTrue("2.5 km" in list.lines.single().value)
        assertEquals(2, report.sections.first { it.title.startsWith("Położenie") }.points.size)
        // Every text format carries the coordinates; the HTML one also draws them.
        assertTrue("50.87, 20.63" in report.toPlainText())
        assertTrue("<svg" in report.toHtml())
    }

    @Test
    fun placedPointsKeepNorthUpAndStayInsideTheBox() {
        val points =
            listOf(
                ReportPoint("north", 51.0, 20.0, isMe = false),
                ReportPoint("south", 50.0, 20.0, isMe = false),
                ReportPoint("east", 50.5, 21.0, isMe = true),
            )
        val placed = placePoints(points, width = 400f, height = 300f, pad = 10f).associateBy { it.point.label }
        assertTrue(placed.getValue("north").y < placed.getValue("south").y)
        assertTrue(placed.getValue("east").x > placed.getValue("north").x)
        placed.values.forEach {
            assertTrue(it.x in 10f..390f)
            assertTrue(it.y in 10f..290f)
        }
    }

    @Test
    fun singlePointIsCentred() {
        val placed = placePoints(listOf(ReportPoint("a", 50.0, 20.0, isMe = false)), 400f, 300f, 10f).single()
        assertEquals(200f, placed.x)
        assertEquals(150f, placed.y)
    }
}
