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
package org.meshtastic.feature.messaging.translation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NodeMetricsTest {
    private val rows =
        listOf(
            MetricRow("Alfa", mapOf("voltage" to 4.10f, "uptime" to 100f, "battery" to 80f, "cpu" to 30f)),
            MetricRow("Beta", mapOf("voltage" to 3.62f, "uptime" to 400f, "battery" to 20f, "cpu" to 90f)),
            MetricRow("Gamma", mapOf("voltage" to 3.90f, "uptime" to 12f)),
        )

    @Test
    fun `lowest voltage puts the lowest node first`() {
        val text = assertNotNull(NodeMetrics.answer("który węzeł ma najniższe napięcie?", rows, polish = true))
        val first = text.lines().first { it.startsWith("1.") }
        assertEquals("1. Beta — 3.62 V", first)
    }

    @Test
    fun `longest uptime puts the highest node first`() {
        val text = assertNotNull(NodeMetrics.answer("który węzeł najdłużej pracuje?", rows, polish = true))
        assertTrue(text.lines().first { it.startsWith("1.") }.startsWith("1. Beta"))
        assertTrue(text.contains("16 d 16 h"))
    }

    @Test
    fun `ram is a whole word and does not match program`() {
        assertNull(NodeMetrics.answer("który program jest najlepszy?", rows, polish = true))
    }

    @Test
    fun `a question about one node is not a ranking`() {
        assertNull(NodeMetrics.answer("jakie napięcie ma mój węzeł?", rows, polish = true))
    }

    @Test
    fun `two metrics are answered together`() {
        val text = assertNotNull(NodeMetrics.answer("który węzeł ma największe obciążenie procesora i najniższą baterię?", rows, polish = true))
        assertTrue(text.contains("obciążenie procesora"))
        assertTrue(text.contains("poziom baterii"))
    }

    @Test
    fun `summary lists extremes of every metric`() {
        val text = assertNotNull(NodeMetrics.answer("daj szybkie podsumowanie węzłów", rows, polish = true))
        assertTrue(text.contains("napięcie"))
    }
}
