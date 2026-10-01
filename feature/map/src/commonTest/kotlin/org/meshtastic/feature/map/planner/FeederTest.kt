/*
 * Copyright (c) 2025 MT_SW contributors
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
package org.meshtastic.feature.map.planner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FeederTest {
    private val freqs = listOf(20.0, 50.0, 100.0, 144.0, 433.0, 868.0, 915.0, 1500.0, 2400.0, 5800.0, 10000.0, 20000.0)

    @Test
    fun allRequiredCablesPresent() {
        val ids = listOf(
            "rg174", "rg178", "rg316", "rg58", "rg59", "rg8x", "rg213", "lmr100", "lmr195", "lmr200", "lmr240",
            "lmr400", "lmr600", "h155", "h1000", "aircell5", "aircell7", "ecoflex10", "ecoflex15", "semirigid085",
            "semirigid141", "ldf4_50a",
        )
        for (id in ids) assertNotNull(CableDb.cable(id), id)
        assertEquals(ids.size, CableDb.cables.size)
        assertEquals(CableDb.cables.size, CableDb.cables.map { it.id }.toSet().size)
    }

    @Test
    fun datasheetPointsReproducedExactly() {
        for (c in CableDb.cables) {
            for (p in c.points) {
                assertEquals(p.dbPer100m / 100.0, c.lossDbPerM(p.fMHz), 1e-9, "${c.id}@${p.fMHz}")
            }
        }
        val lmr = CableDb.cable("lmr400")!!
        assertEquals(0.128, lmr.lossDbPerM(900.0), 1e-9)
        assertEquals(0.2231, lmr.lossDbPerM(2500.0), 1e-9)
        assertEquals(0.124, CableDb.cable("ecoflex15")!!.lossDbPerM(1500.0), 1e-9)
        assertEquals(0.236, CableDb.cable("ecoflex10")!!.lossDbPerM(2400.0), 1e-9)
        assertEquals(0.87, CableDb.cable("rg316")!!.lossDbPerM(1000.0), 1e-9)
        // per-100-ft source converted: RG8X 3.1 dB/100ft at 100 MHz
        assertEquals(3.1 / 30.48, CableDb.cable("rg8x")!!.lossDbPerM(100.0), 1e-9)
    }

    @Test
    fun dataIsStrictlyIncreasingAndSorted() {
        for (c in CableDb.cables) {
            for (i in 1 until c.points.size) {
                assertTrue(c.points[i].fMHz > c.points[i - 1].fMHz, "${c.id} f order")
                assertTrue(c.points[i].dbPer100m > c.points[i - 1].dbPer100m, "${c.id} loss order")
            }
        }
    }

    @Test
    fun monotonicInFrequency() {
        for (c in CableDb.cables) {
            var prev = -1.0
            var f = 20.0
            while (f <= 20000.0) {
                val l = c.lossDbPerM(f)
                assertTrue(l > 0.0, "${c.id} positive @$f")
                assertTrue(l >= prev, "${c.id} monotonic @$f: $l < $prev")
                prev = l
                f *= 1.02
            }
        }
    }

    @Test
    fun clampAndExtrapolationSane() {
        val c = CableDb.cable("lmr400")!!
        assertEquals(c.lossDbPerM(20.0), c.lossDbPerM(1.0), 1e-12)
        assertEquals(c.lossDbPerM(20000.0), c.lossDbPerM(1e6), 1e-12)
        assertEquals(c.lossDbPerM(20.0), c.lossDbPerM(Double.NaN), 1e-12)
        // below first point: sqrt scaling
        assertEquals(c.lossDbPerM(50.0) * kotlin.math.sqrt(25.0 / 50.0), c.lossDbPerM(25.0), 1e-9)
        // above range: between sqrt and linear scaling of last point (RG174 ends at 1 GHz)
        val r = CableDb.cable("rg174")!!
        val l1 = r.lossDbPerM(1000.0)
        val l24 = r.lossDbPerM(2400.0)
        assertTrue(l24 >= l1 * kotlin.math.sqrt(2.4) - 1e-9 && l24 <= l1 * 2.4 + 1e-9)
        assertTrue(r.approximate)
    }

    @Test
    fun lmr400At868() {
        val r = FeederBuilder.compute(FeederConfig(listOf("sma", "n"), listOf(FeederSection("lmr400", 10.0))), 868.0)
        val cable = r.lines[1].lossDb
        // 12.8 dB/100 m at 900 MHz, 8.86 at 450 -> ~12.4 dB/100 m at 868 MHz -> ~1.24 dB per 10 m
        assertEquals(1.24, cable, 0.05)
        assertEquals(cable + 0.03 + 0.03, r.totalDb, 0.01)
        assertEquals("LMR-400 · 10 m", r.lines[1].label)
        assertEquals("SMA", r.lines[0].label)
    }

    @Test
    fun connectorsNeverNegativeAndGrowWithFrequency() {
        for (c in CableDb.connectors) {
            var prev = 0.0
            for (f in freqs) {
                val l = c.lossDb(f)
                assertTrue(l >= 0.0, "${c.id}@$f")
                assertTrue(l >= prev - 1e-12, "${c.id} monotonic @$f")
                prev = l
            }
            assertEquals(c.lossDb900, c.lossDb(900.0), 1e-9)
            assertEquals(c.lossDb2400, c.lossDb(2400.0), 1e-9)
        }
        val weird = ConnectorType("x", "X", 0.0, -0.1, true)
        assertTrue(weird.lossDb(900.0) >= 0.0 && weird.lossDb(2400.0) >= 0.0)
        assertTrue(CableDb.connector("uhf")!!.lossDb(2400.0) > CableDb.connector("uhf")!!.lossDb(433.0) * 2)
        assertNotNull(CableDb.connector("ufl"))
        assertEquals(null, CableDb.connector("nope"))
    }

    @Test
    fun edgeCasesZeroAndOneConnector() {
        val r0 = FeederBuilder.compute(FeederConfig(emptyList(), emptyList()), 868.0)
        assertEquals(0.0, r0.totalDb, 0.0)
        assertTrue(r0.lines.isEmpty() && r0.warnings.isEmpty() && !r0.approximate)
        val r1 = FeederBuilder.compute(FeederConfig(listOf("sma"), emptyList()), 868.0)
        assertEquals(1, r1.lines.size)
        assertEquals(CableDb.connector("sma")!!.lossDb(868.0), r1.totalDb, 1e-12)
        assertTrue(r1.warnings.isEmpty())
        val direct = FeederBuilder.presets.first { it.id == "direct" }
        assertEquals(0.0, FeederBuilder.compute(direct.config, 868.0).totalDb, 0.0)
    }

    @Test
    fun customCable() {
        val cfg = FeederConfig(listOf("sma", "n"), listOf(FeederSection(FeederBuilder.CUSTOM_ID, 5.0, 0.3)))
        val r = FeederBuilder.compute(cfg, 868.0)
        assertEquals(1.5, r.lines[1].lossDb, 1e-12)
        assertEquals("custom · 5 m", r.lines[1].label)
        assertTrue(!r.approximate)
        val bad = FeederBuilder.compute(
            FeederConfig(listOf("sma", "n"), listOf(FeederSection(FeederBuilder.CUSTOM_ID, 5.0, null))), 868.0,
        )
        assertTrue("CUSTOM_LOSS_MISSING" in bad.warnings)
    }

    @Test
    fun mismatchedSizesTolerated() {
        val extra = FeederConfig(listOf("sma", "n"), listOf(FeederSection("rg316", 1.0), FeederSection("lmr400", 10.0)))
        val r = FeederBuilder.compute(extra, 868.0)
        assertTrue("SECTION_COUNT_MISMATCH" in r.warnings)
        assertEquals(3, r.lines.size)
        val missing = FeederBuilder.compute(FeederConfig(listOf("sma", "n", "n"), listOf(FeederSection("rg316", 1.0))), 868.0)
        assertTrue("SECTION_COUNT_MISMATCH" in missing.warnings)
        val none = FeederBuilder.compute(FeederConfig(emptyList(), listOf(FeederSection("rg316", 1.0))), 868.0)
        assertEquals(0.0, none.totalDb, 0.0)
        assertTrue("SECTION_COUNT_MISMATCH" in none.warnings)
        val unk = FeederBuilder.compute(FeederConfig(listOf("zzz", "sma"), listOf(FeederSection("qqq", 1.0))), 868.0)
        assertTrue("UNKNOWN_CONNECTOR:zzz" in unk.warnings && "UNKNOWN_CABLE:qqq" in unk.warnings && unk.approximate)
        val neg = FeederBuilder.compute(FeederConfig(listOf("sma", "n"), listOf(FeederSection("rg316", -2.0))), 868.0)
        assertTrue("INVALID_LENGTH" in neg.warnings)
        val many = FeederBuilder.compute(FeederConfig(List(10) { "sma" }, List(9) { FeederSection("rg316", 0.1) }), 868.0)
        assertTrue("CONNECTOR_COUNT_EXCEEDS_MAX" in many.warnings)
    }

    @Test
    fun resizeKeepsData() {
        val base = FeederConfig(listOf("ufl", "sma", "n"), listOf(FeederSection("rg316", 0.2), FeederSection("lmr400", 10.0)))
        val bigger = FeederBuilder.resize(base, 5)
        assertEquals(listOf("ufl", "sma", "n", "sma", "sma"), bigger.connectorIds)
        assertEquals(4, bigger.sections.size)
        assertEquals(base.sections, bigger.sections.take(2))
        assertEquals(FeederSection("rg316", 0.2), bigger.sections[3])
        val smaller = FeederBuilder.resize(base, 2)
        assertEquals(listOf("ufl", "sma"), smaller.connectorIds)
        assertEquals(base.sections.take(1), smaller.sections)
        val one = FeederBuilder.resize(base, 1)
        assertEquals(1, one.connectorIds.size)
        assertTrue(one.sections.isEmpty())
        val zero = FeederBuilder.resize(base, 0)
        assertTrue(zero.connectorIds.isEmpty() && zero.sections.isEmpty())
        assertEquals(8, FeederBuilder.resize(base, 99).connectorIds.size)
        assertEquals(0, FeederBuilder.resize(base, -3).connectorIds.size)
        val fromEmpty = FeederBuilder.resize(FeederConfig(emptyList(), emptyList()), 3)
        assertEquals(3, fromEmpty.connectorIds.size)
        assertEquals(2, fromEmpty.sections.size)
    }

    @Test
    fun presetsAreConsistent() {
        assertTrue(FeederBuilder.presets.size in 4..6)
        assertEquals(FeederBuilder.presets.size, FeederBuilder.presets.map { it.id }.toSet().size)
        for (p in FeederBuilder.presets) {
            val n = p.config.connectorIds.size
            assertEquals(if (n > 0) n - 1 else 0, p.config.sections.size, p.id)
            val r = FeederBuilder.compute(p.config, 868.0)
            assertTrue(r.warnings.isEmpty(), p.id + " " + r.warnings)
            assertTrue(r.totalDb >= 0.0)
        }
        val pc = FeederBuilder.compute(FeederBuilder.presets.first { it.id == "pigtail_cable" }.config, 868.0)
        assertTrue(pc.totalDb > 1.2 && pc.totalDb < 2.5, "pigtail_cable ${pc.totalDb}")
    }
}
