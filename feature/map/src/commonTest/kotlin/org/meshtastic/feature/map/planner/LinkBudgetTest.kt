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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LinkBudgetTest {
    private val pa = GeoPoint(50.0, 20.0)
    private val pb = Geodesy.destination(pa, 90.0, 10000.0)

    private fun end(p: GeoPoint, h: Double = 10.0, tx: Double = 20.0) = LinkEnd(p, 0.0, h, tx, 2.0, 0.5)

    private fun input(sf: Int = 12, bw: Double = 125.0, ea: LinkEnd = end(pa), eb: LinkEnd = end(pb)) =
        LinkInput(ea, eb, 868.0, bw, sf)

    private fun flat(d: Double = 10000.0, n: Int = 100) = PathProfile(d / n, DoubleArray(n + 1) { 0.0 })

    @Test
    fun sensitivityValues() {
        assertEquals(-137.0, Sensitivity.dbm(125.0, 12, 6.0), 0.1)
        assertEquals(-174.0 + 10 * kotlin.math.log10(250000.0) + 6 - 17.5, Sensitivity.dbm(250.0, 11, 6.0), 1e-9)
    }

    @Test
    fun verdictThresholds() {
        assertEquals(LinkVerdict.EXCELLENT, LinkVerdict.fromMargin(20.0))
        assertEquals(LinkVerdict.GOOD, LinkVerdict.fromMargin(19.99))
        assertEquals(LinkVerdict.GOOD, LinkVerdict.fromMargin(10.0))
        assertEquals(LinkVerdict.MARGINAL, LinkVerdict.fromMargin(3.0))
        assertEquals(LinkVerdict.WEAK, LinkVerdict.fromMargin(0.0))
        assertEquals(LinkVerdict.NO_LINK, LinkVerdict.fromMargin(-0.1))
    }

    @Test
    fun fresnelAndFreeSpace() {
        assertEquals(29.4, fresnelRadiusM(1, 5000.0, 5000.0, 868.0), 0.1)
        assertEquals(111.2, LinkBudget.freeSpaceLossDb(10000.0, 868.0), 0.1)
        val p = flat()
        assertEquals(10000.0, p.distanceM, 1e-6)
        assertEquals(0.0, p.bulgeM(0, 1.33), 1e-12)
        // 10 km, k=4/3: bulge = 5000*5000/(2*1.3333*6371008.8) = 1.47 m
        assertEquals(1.47, p.bulgeM(50, 4.0 / 3.0), 0.01)
    }

    @Test
    fun flatPathLink() {
        val r = LinkBudget.analyze(input(), flat())
        assertEquals(10000.0, r.distanceM, 1e-6)
        assertEquals(90.0, r.bearingAToBDeg, 0.1)
        assertEquals(270.0, r.bearingBToADeg, 0.5)
        assertTrue(r.itmLossDb >= r.freeSpaceLossDb - 1.0 && r.itmLossDb < r.freeSpaceLossDb + 25.0, "itm=${r.itmLossDb} fs=${r.freeSpaceLossDb}")
        assertTrue(r.lineOfSightClear)
        assertTrue(r.worstFresnelRatio > 0.0, "ratio=${r.worstFresnelRatio}")
        assertEquals(r.totalPathLossDb, r.itmLossDb, 1e-9)
        assertTrue(r.aToB.verdict == LinkVerdict.EXCELLENT, "m=${r.aToB.marginDb}")
        // rx = 20 + 2 - 0.5 - loss + 2 - 0.5
        assertEquals(23.0 - r.totalPathLossDb, r.aToB.rxPowerDbm, 1e-9)
        assertEquals(r.sensitivityDbm, -137.0, 0.1)
    }

    @Test
    fun symmetricAndAsymmetric() {
        val sym = LinkBudget.analyze(input(), flat())
        assertEquals(sym.aToB.rxPowerDbm, sym.bToA.rxPowerDbm, 1e-9)
        val asym = LinkBudget.analyze(input(ea = end(pa, tx = 30.0), eb = end(pb, tx = 14.0)), flat())
        assertEquals(16.0, asym.aToB.rxPowerDbm - asym.bToA.rxPowerDbm, 1e-9)
        // swapping ends gives mirrored directions
        val swapped = LinkBudget.analyze(input(ea = end(pb, tx = 14.0), eb = end(pa, tx = 30.0)), flat())
        assertEquals(asym.aToB.rxPowerDbm, swapped.bToA.rxPowerDbm, 1e-6)
        assertEquals(asym.itmLossDb, swapped.itmLossDb, 0.5)
    }

    @Test
    fun blockedByHill() {
        val g = DoubleArray(101) { i -> val x = (i - 50) / 10.0; 300.0 * kotlin.math.exp(-x * x) }
        g[0] = 0.0; g[100] = 0.0
        val p = PathProfile(100.0, g)
        val r = LinkBudget.analyze(input(sf = 7, bw = 250.0), p)
        assertFalse(r.lineOfSightClear)
        assertTrue(r.worstFresnelClearanceM < 0 && r.worstFresnelRatio < 0)
        val flatR = LinkBudget.analyze(input(sf = 7, bw = 250.0), flat())
        assertTrue(r.itmLossDb > flatR.itmLossDb + 20.0, "hill=${r.itmLossDb} flat=${flatR.itmLossDb}")
        assertTrue(r.aToB.verdict == LinkVerdict.NO_LINK, "m=${r.aToB.marginDb}")
        assertTrue(r.aToB.marginDb < 0)
    }

    @Test
    fun extraLossAndFadeMargin() {
        val base = LinkBudget.analyze(input(), flat())
        val more = LinkBudget.analyze(input().copy(extraLossDb = 10.0, fadeMarginDb = 5.0), flat())
        assertEquals(10.0, more.totalPathLossDb - base.totalPathLossDb, 1e-9)
        assertEquals(base.aToB.marginDb - 15.0, more.aToB.marginDb, 1e-9)
    }

    @Test
    fun seriesAndSampler() {
        val inp = input()
        val s = LinkBudget.series(inp, flat())
        assertEquals(101, s.distancesM.size)
        assertEquals(10.0, s.losM[0], 1e-9)
        assertEquals(10.0, s.losM[100], 1e-9)
        assertEquals(0.0, s.fresnelUpperM[0] - s.losM[0], 1e-9)
        assertEquals(29.4, s.fresnelUpperM[50] - s.losM[50], 0.1)
        assertTrue(s.groundM[50] > 1.4)
        val prof = PathProfile.fromSampler(pa, pb) { _, _ -> 120.0 }
        assertEquals(334, prof.intervals)
        assertEquals(10000.0, prof.distanceM, 1.0)
        val coarse = PathProfile.fromSampler(pa, Geodesy.destination(pa, 0.0, 100000.0), maxSamples = 400) { _, _ -> 0.0 }
        assertEquals(399, coarse.intervals)
    }
}
