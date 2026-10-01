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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.meshtastic.feature.map.planner.FeederBuilder
import org.meshtastic.proto.Config.LoRaConfig.ModemPreset
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModelTest {
    private val elevation = FakeElevation()
    private val weather = FakeWeather()
    private val nodes = FakeNodes(
        listOf(
            nodeOption(1, POINT_A, ours = true),
            nodeOption(2, POINT_B, snr = 4.0f, rssi = -105),
        ),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm(): PlannerViewModel {
        val vm = PlannerViewModel(elevation, weather, nodes)
        vm.debounceMs = 0L
        vm.computer = PlannerComputer(elevation, weather, Dispatchers.Unconfined, { 0L })
        return vm
    }

    private val PlannerViewModel.state get() = uiState.value

    @Test
    fun defaults() {
        val s = vm().state
        assertEquals(20.0, s.a.txPowerDbm)
        assertEquals(2.15, s.a.antennaGainDbi)
        assertEquals(2.0, s.a.antennaHeightM)
        assertEquals(0.0, s.a.feederLossDb)
        assertEquals(6.0, s.noiseFigureDb)
        assertEquals("direct", s.a.feederPresetId)
        assertEquals(listOf(PlannerHint.POINT_MISSING), s.hintsBySide[PlannerSide.A])
        assertFalse(s.isLinkReady)
    }

    @Test
    fun nodesAreExposed() {
        val v = vm()
        assertEquals(2, v.nodesWithPosition.value.size)
        assertTrue(v.nodesWithPosition.value.first { it.num == 1 }.isOurs)
    }

    @Test
    fun bandSelectsFrequencyAndBack() {
        val v = vm()
        v.setBand("433")
        assertEquals(433.03125, v.state.frequencyMHz)
        assertEquals("433", v.state.bandId)
        v.setBand("free")
        assertEquals("free", v.state.bandId)
        assertEquals(433.03125, v.state.frequencyMHz)
        v.setFrequency(868.2)
        assertEquals("868", v.state.bandId)
        v.setFrequency(5.0)
        assertEquals(20.0, v.state.frequencyMHz)
    }

    @Test
    fun presetSetsBandwidthAndSpreadingFactor() {
        val v = vm()
        v.setRadioFromPreset(ModemPreset.NARROW_FAST)
        assertEquals(62.5, v.state.bandwidthKhz)
        assertEquals(7, v.state.spreadingFactor)
        assertFalse(v.state.radioOverride)
        assertEquals(ModemPreset.NARROW_FAST, v.state.modemPreset)
        v.setSpreadingFactor(9)
        assertTrue(v.state.radioOverride)
        assertNull(v.state.modemPreset)
        v.setRadioFromPreset(ModemPreset.LITE_SLOW)
        assertEquals(125.0, v.state.bandwidthKhz)
        assertEquals(10, v.state.spreadingFactor)
        v.setSpreadingFactor(3)
        assertEquals(5, v.state.spreadingFactor)
    }

    @Test
    fun copyAToBCopiesOnlyEndParameters() {
        val v = vm()
        v.setPointManual(PlannerSide.A, POINT_A.lat, POINT_A.lon)
        v.setPointManual(PlannerSide.B, POINT_B.lat, POINT_B.lon)
        v.setTxPowerDbm(PlannerSide.A, 14.0)
        v.setAntennaGain(PlannerSide.A, 5.0)
        v.setAntennaHeight(PlannerSide.A, 12.0)
        v.setFeederManualDb(PlannerSide.A, 1.5)
        v.applyFeederPreset(PlannerSide.A, "pigtail")
        v.setName(PlannerSide.B, "B station")
        v.copyAToB()
        val a = v.state.a
        val b = v.state.b
        assertEquals(14.0, b.txPowerDbm)
        assertEquals(5.0, b.antennaGainDbi)
        assertEquals(12.0, b.antennaHeightM)
        assertEquals(1.5, b.feederManualDb)
        assertTrue(b.feederPrecise)
        assertEquals(a.feederConfig, b.feederConfig)
        assertEquals(a.feederResult, b.feederResult)
        // untouched
        assertEquals(POINT_B.lon, b.lon)
        assertEquals("B station", b.name)
    }

    @Test
    fun feederModeToggleKeepsManualValue() {
        val v = vm()
        v.setFeederManualDb(PlannerSide.A, 2.0)
        v.setFeederPrecise(PlannerSide.A, true)
        assertEquals(0.0, v.state.a.feederLossDb) // direct config = 0 dB
        v.applyFeederPreset(PlannerSide.A, "heliax_mast")
        assertTrue(v.state.a.feederLossDb > 0.0)
        v.setFeederPrecise(PlannerSide.A, false)
        assertEquals(2.0, v.state.a.feederManualDb)
        assertEquals(2.0, v.state.a.feederLossDb)
        v.resizeFeederConnectors(PlannerSide.A, 3)
        assertEquals(3, v.state.a.feederConfig.connectorIds.size)
        assertEquals(2, v.state.a.feederConfig.sections.size)
        assertTrue(FeederBuilder.presets.isNotEmpty())
    }

    @Test
    fun txPowerWattsAreStoredAsDbm() {
        val v = vm()
        v.setTxPowerW(PlannerSide.A, 1.0)
        assertEquals(30.0, v.state.a.txPowerDbm, 1e-9)
        assertEquals(1.0, v.state.a.txPowerW, 1e-9)
    }

    @Test
    fun fullLinkFromStationAndNode() {
        val v = vm()
        v.useStation(PlannerSide.A)
        v.setPointFromNode(PlannerSide.B, 2)
        v.recompute()
        val s = v.state
        assertEquals(PlannerPointSource.Station, s.a.pointSource)
        assertEquals(PlannerPointSource.Node(2), s.b.pointSource)
        assertEquals("Node 2", s.b.name)
        assertNotNull(s.link)
        assertNotNull(s.series)
        assertFalse(s.computing)
        assertEquals(250.0, s.a.groundAltM)
        assertEquals(4.0 / 3.0, s.kFactor, 1e-12)
        val cmp = assertNotNull(s.comparison)
        assertEquals(PlannerSide.B, cmp.nodeSide)
        assertEquals(-105, cmp.measuredRssiDbm)
        assertTrue(s.hintsBySide.values.all { it.isEmpty() })
    }

    @Test
    fun nodeUpdateRefreshesComparisonWithoutRecompute() {
        val v = vm()
        v.useStation(PlannerSide.A)
        v.setPointFromNode(PlannerSide.B, 2)
        v.recompute()
        val calls = elevation.profileCalls
        nodes.flow.value = listOf(nodeOption(1, POINT_A, ours = true), nodeOption(2, POINT_B, snr = 9.0f, rssi = -90))
        assertEquals(-90, v.state.comparison?.measuredRssiDbm)
        assertEquals(calls, elevation.profileCalls)
    }

    @Test
    fun stationWithoutPositionMarksHint() {
        nodes.flow.value = emptyList()
        val v = vm()
        v.useStation(PlannerSide.A)
        val hints = v.state.hintsBySide[PlannerSide.A].orEmpty()
        assertContains(hints, PlannerHint.STATION_NO_POSITION)
        assertContains(hints, PlannerHint.POINT_MISSING)
    }

    @Test
    fun weatherToggle() {
        weather.result = org.meshtastic.feature.map.planner.data.PlannerWeatherResult.Success(conditions())
        val v = vm()
        v.setPointManual(PlannerSide.A, POINT_A.lat, POINT_A.lon)
        v.setPointManual(PlannerSide.B, POINT_B.lat, POINT_B.lon)
        v.setUseWeather(true)
        assertTrue(v.state.weather is PlannerWeatherStatus.Ready)
        assertEquals(conditions().analysis.kFactor, v.state.kFactor, 1e-12)
        v.setUseWeather(false)
        assertEquals(PlannerWeatherStatus.Idle, v.state.weather)
        assertEquals(4.0 / 3.0, v.state.kFactor, 1e-12)
    }

    @Test
    fun clutterPresetSetsExtraLoss() {
        val v = vm()
        v.setClutterPreset(PlannerClutterPreset.FOREST_DENSE)
        assertEquals(10.0, v.state.extraLossDb)
        v.setExtraLossDb(7.0)
        assertEquals(PlannerClutterPreset.CUSTOM, v.state.clutterPreset)
    }

    @Test
    fun swapExchangesEnds() {
        val v = vm()
        v.setPointManual(PlannerSide.A, POINT_A.lat, POINT_A.lon)
        v.setPointManual(PlannerSide.B, POINT_B.lat, POINT_B.lon)
        v.swapAB()
        assertEquals(POINT_B.lon, v.state.a.lon)
        assertEquals(POINT_A.lon, v.state.b.lon)
    }

    @Test
    fun invalidCoordinatesAreIgnored() {
        val v = vm()
        v.setPointManual(PlannerSide.A, 123.0, 20.0)
        assertFalse(v.state.a.isComplete)
    }

    @Test
    fun pngNeedsAProfile() = runTest {
        assertNull(vm().exportPng(PlannerReportStrings()))
    }

    @Test
    fun coverageAndExports() = runTest {
        val v = vm()
        v.setPointManual(PlannerSide.A, POINT_A.lat, POINT_A.lon)
        v.setPointManual(PlannerSide.B, POINT_B.lat, POINT_B.lon)
        v.recompute()
        v.setCoverageMaxRangeKm(4.0)
        v.setCoverageRadials(8)
        v.computeCoverage()
        assertNotNull(v.state.coverage)
        assertFalse(v.state.coverageComputing)

        val strings = PlannerReportStrings(title = "Raport testowy")
        val report = v.buildReport(strings)
        assertEquals("Raport testowy", report.title)
        assertNotNull(report.profile)
        assertNotNull(report.coverage)
        assertEquals(2, report.points.size)
        assertTrue(report.notes.containsAll(strings.credits))

        val pdf = v.exportPdf(strings)
        assertEquals('%'.code.toByte(), pdf[0])
        assertTrue(v.exportKml(strings).contains("<kml"))
        assertTrue(v.exportGeoJson(strings).contains("FeatureCollection"))
        assertTrue(v.exportCsvProfile(strings).startsWith("distance_m;"))
        assertTrue(v.exportCsvSummary(strings).contains("Latitude;"))
        val png = assertNotNull(v.exportPng(strings))
        assertEquals(0x89.toByte(), png[0])

        v.clearCoverage()
        assertNull(v.state.coverage)
    }
}
