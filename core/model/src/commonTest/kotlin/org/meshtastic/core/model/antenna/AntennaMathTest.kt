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
package org.meshtastic.core.model.antenna

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AntennaMathTest {
    private fun near(a: Double, b: Double, tol: Double) = assertTrue(abs(a - b) <= tol, "$a vs $b")

    @Test
    fun thinWireShortensAboutFivePercent() {
        val l = AntennaMath.wavelength(869.44165)
        near(AntennaMath.thicknessFactor(l, 0.0015), 0.952, 0.002)
        assertTrue(AntennaMath.thicknessFactor(l, 0.010) < AntennaMath.thicknessFactor(l, 0.001))
    }

    @Test
    fun quarterWaveAndDipole() {
        val gp = AntennaMath.design(AntennaInput(type = AntennaType.GROUND_PLANE))
        near(gp.dims.first { it.kind == DimKind.RADIATOR }.meters, 0.0821, 0.0005)
        val dp = AntennaMath.design(AntennaInput(type = AntennaType.DIPOLE))
        near(dp.dims.first { it.kind == DimKind.TOTAL }.meters, 0.1642, 0.001)
    }

    @Test
    fun moxonMatchesCebik() {
        // Cebik's equations for a 0.001 λ wire: A ≈ 0.361 λ, E = B + C + D.
        val l = AntennaMath.wavelength(869.44165)
        val m = AntennaMath.design(AntennaInput(type = AntennaType.MOXON, diameterMm = l * 1000 * 0.001))
        val a = m.dims.first { it.letter == "A" }.meters / l
        near(a, -0.0008571428571 * 9 + 0.009571428571 * 3 + 0.3398571429, 1e-6)
        val e = m.dims.first { it.letter == "E" }.meters
        near(e, m.dims.filter { it.letter in setOf("B", "C", "D") }.sumOf { it.meters }, 1e-9)
    }

    @Test
    fun cableLossFitsTheTable() {
        near(Cable.RG58.lossPer100m(432.0), 40.0, 0.01)
        near(Cable.RG58.lossPer100m(1296.0), 90.0, 0.01)
        val at868 = Cable.H155.lossPer100m(868.0)
        assertTrue(at868 > 19.8 && at868 < 34.9, "$at868")
    }

    @Test
    fun feedLineAndLimits() {
        near(AntennaMath.mismatchLossDb(1.0), 0.0, 1e-9)
        near(AntennaMath.mismatchLossDb(2.0), 0.512, 0.01)
        val r = AntennaMath.feed(869.5, 27.0, null, 0.0, emptyMap(), 1.0, 2.15)
        near(r.erpDbm, 27.0, 1e-9)
        assertTrue(r.overLimit == false)
        assertTrue(AntennaMath.feed(869.5, 27.0, null, 0.0, emptyMap(), 1.0, 6.0).overLimit == true)
    }

    @Test
    fun feedLossUsesAnyFeederLine() {
        val cfg = FeederConfig(listOf("sma", "n"), listOf(FeederSection("lmr400", 10.0)))
        val f = FeederBuilder.compute(cfg, 869.5)
        val r = AntennaMath.feedLoss(869.5, 27.0, f.totalDb, f.connectorsDb, 1.0, 2.15)
        near(r.totalDb, f.totalDb, 1e-9)
        near(r.connectorsDb, f.connectorsDb, 1e-9)
        near(r.cableDb + r.connectorsDb, f.totalDb, 1e-9)
        near(r.atAntennaDbm, 27.0 - f.totalDb, 1e-9)
        assertTrue(f.connectorsDb > 0.0 && f.totalDb > f.connectorsDb)
    }

    @Test
    fun savedAntennasSurviveTheRoundTrip() {
        val setup = AntennaSetup(
            id = "a1",
            name = "Dach",
            input = AntennaInput(type = AntennaType.YAGI, elements = 7),
            txDbm = 22.0,
            swr = 1.3,
            gainOverrideDbi = 9.5,
            feeder = FeederConfig(listOf("sma", "n"), listOf(FeederSection("lmr400", 10.0))),
        )
        val back = AntennaSetups.decode(AntennaSetups.encode(AntennaSetups.upsert(emptyList(), setup)))
        assertEquals(listOf(setup), back)
        near(back.first().gainDbi, 9.5, 1e-9)
        assertTrue(AntennaSetups.decode("not json").isEmpty())
        assertTrue(AntennaSetups.remove(back, "a1").isEmpty())
    }

    @Test
    fun yagiHasAllElements() {
        val y = AntennaMath.design(AntennaInput(type = AntennaType.YAGI, elements = 7))
        assertTrue(y.dims.count { it.kind == DimKind.DIRECTOR } == 5)
        assertTrue(y.gainDbi > 9 && y.gainDbi < 14, "${y.gainDbi}")
    }
}
