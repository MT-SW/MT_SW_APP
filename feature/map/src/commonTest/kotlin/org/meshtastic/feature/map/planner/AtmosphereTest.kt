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

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AtmosphereTest {
    @Test
    fun standardAtmosphereRefractivity() {
        val n = Atmosphere.refractivityN(15.0, 1013.25, 60.0)
        assertTrue(n in 315.0..325.0, "N=$n")
        assertTrue(Atmosphere.refractivityN(30.0, 1000.0, 90.0) > n)
    }

    @Test
    fun kFactor() {
        assertEquals(1.333, Atmosphere.kFactorFromGradient(-39.2), 0.005)
        assertEquals(20.0, Atmosphere.kFactorFromGradient(-200.0), 1e-9)
        assertEquals(0.5, Atmosphere.kFactorFromGradient(1e5), 1e-9)
        assertEquals(-39.2, Atmosphere.gradientNPerKm(320.0, 0.0, 280.8, 1000.0), 1e-9)
    }

    @Test
    fun classification() {
        assertEquals(DuctingLevel.NORMAL, Atmosphere.classify(-20.0))
        assertEquals(DuctingLevel.NORMAL, Atmosphere.classify(-60.0))
        assertEquals(DuctingLevel.ELEVATED, Atmosphere.classify(-100.0))
        assertEquals(DuctingLevel.POSSIBLE_DUCT, Atmosphere.classify(-200.0))
    }

    @Test
    fun seaLevelReduction() {
        assertEquals(300.0, Atmosphere.surfaceRefractivityForItm(300.0, 0.0), 1e-9)
        assertTrue(Atmosphere.surfaceRefractivityForItm(300.0, 500.0) > 300.0)
        assertEquals(400.0, Atmosphere.surfaceRefractivityForItm(500.0), 1e-9)
        assertEquals(250.0, Atmosphere.surfaceRefractivityForItm(100.0), 1e-9)
    }

    @Test
    fun analyzeProfile() {
        val std = listOf(
            AtmosphereSample(0.0, 15.0, 1013.25, 60.0),
            AtmosphereSample(1000.0, 8.5, 898.7, 60.0),
        )
        val a = Atmosphere.analyzeProfile(std)
        assertTrue(a.surfaceN in 315.0..325.0)
        assertTrue(a.gradientNPerKm < -20 && a.gradientNPerKm > -80, "g=${a.gradientNPerKm}")
        assertEquals(DuctingLevel.NORMAL, a.level)
        // strong inversion with dry air above
        val inv = listOf(
            AtmosphereSample(0.0, 15.0, 1013.0, 95.0),
            AtmosphereSample(200.0, 25.0, 990.0, 10.0),
        )
        assertEquals(DuctingLevel.POSSIBLE_DUCT, Atmosphere.analyzeProfile(inv).level)
        assertEquals(DuctingLevel.NORMAL, Atmosphere.analyzeProfile(emptyList()).level)
    }

    @Test
    fun gasAndRain() {
        assertEquals(0.0, Atmosphere.gasAttenuationDbPerKm(868.0, 15.0, 1013.0, 60.0), 1e-12)
        val g24 = Atmosphere.gasAttenuationDbPerKm(2400.0, 15.0, 1013.0, 60.0)
        assertTrue(g24 in 0.003..0.02, "g24=$g24")
        val g10 = Atmosphere.gasAttenuationDbPerKm(10000.0, 15.0, 1013.0, 60.0)
        assertTrue(g10 in 0.01..0.1, "g10=$g10")
        val g22 = Atmosphere.gasAttenuationDbPerKm(22235.0, 15.0, 1013.0, 60.0)
        assertTrue(g22 > 0.1 && g22 > g10 * 3, "g22=$g22")
        val g60 = Atmosphere.gasAttenuationDbPerKm(60000.0, 15.0, 1013.0, 60.0)
        assertTrue(g60 > 10.0, "g60=$g60")

        assertEquals(0.0, Atmosphere.rainAttenuationDbPerKm(868.0, 50.0), 1e-12)
        val r = Atmosphere.rainAttenuationDbPerKm(10000.0, 25.0, vertical = false)
        assertEquals(0.01217 * 25.0.pow(1.2571), r, 1e-6)
        assertTrue(Atmosphere.rainAttenuationDbPerKm(5000.0, 25.0) in 0.01..3.0)
        assertTrue(Atmosphere.rainAttenuationDbPerKm(20000.0, 25.0) > r)
    }
}
