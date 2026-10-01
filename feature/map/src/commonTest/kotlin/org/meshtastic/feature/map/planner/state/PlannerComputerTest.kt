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
package org.meshtastic.feature.map.planner.state

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.meshtastic.feature.map.planner.Geodesy
import org.meshtastic.feature.map.planner.LinkVerdict
import org.meshtastic.feature.map.planner.data.PlannerElevationFailure
import org.meshtastic.feature.map.planner.data.PlannerWeatherError
import org.meshtastic.feature.map.planner.data.PlannerWeatherResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlannerComputerTest {
    private val elevation = FakeElevation()
    private val weather = FakeWeather()
    private val computer = PlannerComputer(elevation, weather, Dispatchers.Unconfined, { 0L })

    private fun readyState() = PlannerUiState(a = endAt(POINT_A), b = endAt(POINT_B))

    @Test
    fun completePipelineOnFlatTerrain() = runTest {
        val r = computer.compute(readyState())
        val link = assertNotNull(r.link)
        assertNotNull(r.series)
        assertNull(r.error)
        assertEquals(250.0, r.groundAltA)
        assertEquals(250.0, r.groundAltB)
        assertEquals(Geodesy.distanceM(POINT_A, POINT_B), link.distanceM, 100.0)
        assertTrue(link.aToB.marginDb > 10.0, "margin=${link.aToB.marginDb}")
        assertTrue(link.aToB.verdict != LinkVerdict.NO_LINK)
        // symmetric parameters on both ends
        assertEquals(link.aToB.rxPowerDbm, link.bToA.rxPowerDbm, 1e-9)
    }

    @Test
    fun weatherOffUsesStandardAtmosphere() = runTest {
        val r = computer.compute(readyState())
        assertEquals(4.0 / 3.0, r.kFactor, 1e-12)
        assertEquals(301.0, r.surfaceRefractivity, 1e-12)
        assertEquals(PlannerWeatherStatus.Idle, r.weather)
        assertEquals(0, weather.calls)
    }

    @Test
    fun weatherOnSuccessUsesAnalysis() = runTest {
        val c = conditions()
        weather.result = PlannerWeatherResult.Success(c)
        val r = computer.compute(readyState().copy(useWeather = true))
        assertEquals(c.analysis.kFactor, r.kFactor, 1e-12)
        assertEquals(c.analysis.seaLevelN, r.surfaceRefractivity, 1e-12)
        assertEquals(PlannerWeatherStatus.Ready(c), r.weather)
        assertNotNull(r.link)
        // cached: a second compute does not download again
        computer.compute(readyState().copy(useWeather = true))
        assertEquals(1, weather.calls)
    }

    @Test
    fun weatherFailureFallsBackToStandard() = runTest {
        weather.result = PlannerWeatherResult.Failure(PlannerWeatherError.OFFLINE)
        val r = computer.compute(readyState().copy(useWeather = true))
        assertEquals(PlannerWeatherStatus.Failed(PlannerWeatherError.OFFLINE), r.weather)
        assertEquals(4.0 / 3.0, r.kFactor, 1e-12)
        assertNotNull(r.link)
    }

    @Test
    fun incompleteEndGivesNoLinkAndHints() = runTest {
        val st = PlannerUiState(a = endAt(POINT_A))
        val r = computer.compute(st)
        assertNull(r.link)
        assertNull(r.error)
        assertEquals(250.0, r.groundAltA)
        assertNull(r.groundAltB)
        val hints = PlannerViewModel.normalize(st).hintsBySide
        assertEquals(emptyList(), hints[PlannerSide.A])
        assertEquals(listOf(PlannerHint.POINT_MISSING), hints[PlannerSide.B])
    }

    @Test
    fun elevationFailureIsTyped() = runTest {
        elevation.failure = PlannerElevationFailure.NETWORK
        val r = computer.compute(readyState())
        assertNull(r.link)
        assertEquals(PlannerError.ELEVATION_OFFLINE, r.error)
    }

    @Test
    fun samePointIsRejected() = runTest {
        val r = computer.compute(PlannerUiState(a = endAt(POINT_A), b = endAt(POINT_A)))
        assertEquals(PlannerError.POINTS_TOO_CLOSE, r.error)
        assertEquals(0, elevation.profileCalls)
    }

    @Test
    fun manualGroundAltitudeReplacesEndSample() = runTest {
        val st = readyState().copy(a = endAt(POINT_A).copy(groundAltM = 300.0, groundAltManual = true))
        val r = computer.compute(st)
        val link = assertNotNull(r.link)
        assertEquals(300.0, link.profile.groundM[0])
        assertEquals(250.0, r.groundAltA)
    }

    @Test
    fun feederAndExtraLossLowerTheMargin() = runTest {
        val base = computer.compute(readyState()).link!!
        val lossy = computer.compute(
            readyState().copy(
                a = endAt(POINT_A).copy(feederManualDb = 3.0),
                extraLossDb = 6.0,
            ),
        ).link!!
        assertEquals(base.aToB.marginDb - 9.0, lossy.aToB.marginDb, 1e-6)
        // the feeder at A (3 dB) acts on both directions: as transmit loss and as receive loss
        assertEquals(base.bToA.marginDb - 9.0, lossy.bToA.marginDb, 1e-6)
    }

    @Test
    fun comparisonWithMeasuredNode() = runTest {
        val nodes = listOf(
            nodeOption(1, POINT_A, ours = true),
            nodeOption(2, POINT_B, snr = 5.5f, rssi = -100),
        )
        val st = PlannerUiState(
            a = endAt(POINT_A, PlannerPointSource.Station),
            b = endAt(POINT_B, PlannerPointSource.Node(2)),
        )
        val r = computer.compute(st, nodes)
        val link = assertNotNull(r.link)
        val cmp = assertNotNull(r.comparison)
        assertEquals(2, cmp.nodeNum)
        assertEquals(PlannerSide.B, cmp.nodeSide)
        assertTrue(cmp.direct)
        assertEquals(link.bToA.rxPowerDbm, cmp.predictedRssiDbm, 1e-9)
        val floor = -174.0 + 10.0 * kotlin.math.log10(250000.0) + 6.0
        assertEquals(floor, cmp.noiseFloorDbm, 1e-9)
        assertEquals(link.bToA.rxPowerDbm - floor, cmp.predictedSnrDb, 1e-9)
        assertEquals(-100 - link.bToA.rxPowerDbm, assertNotNull(cmp.rssiDeltaDb), 1e-9)
        assertEquals(5.5 - cmp.predictedSnrDb, assertNotNull(cmp.snrDeltaDb), 1e-6)
    }

    @Test
    fun noComparisonWithoutOurStation() = runTest {
        val nodes = listOf(nodeOption(2, POINT_B, snr = 5.5f, rssi = -100))
        val st = PlannerUiState(a = endAt(POINT_A), b = endAt(POINT_B, PlannerPointSource.Node(2)))
        assertNull(computer.compute(st, nodes).comparison)
    }

    @Test
    fun coverageUsesSelectedSide() = runTest {
        val st = readyState().copy(coverageMaxRangeKm = 5.0, coverageRadials = 8)
        var progress = 0f
        val cov = computer.computeCoverage(st) { progress = it }
        assertEquals(8, cov.radials)
        assertEquals(POINT_A, cov.center)
        assertEquals(1f, progress)
        assertTrue(cov.marginDb[0][0] > 0f)
    }
}
