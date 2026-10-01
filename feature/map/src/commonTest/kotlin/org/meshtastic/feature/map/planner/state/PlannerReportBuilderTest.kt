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
import org.meshtastic.feature.map.planner.FeederBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlannerReportBuilderTest {
    private val computer = PlannerComputer(FakeElevation(), FakeWeather(), Dispatchers.Unconfined, { 0L })

    private suspend fun stateWithLink(extra: (PlannerUiState) -> PlannerUiState = { it }): PlannerUiState {
        val st = extra(PlannerUiState(a = endAt(POINT_A), b = endAt(POINT_B)))
        val r = computer.compute(st)
        return st.copy(link = r.link, series = r.series, weather = r.weather)
    }

    @Test
    fun reportContainsAllSectionsAndTexts() = runTest {
        val s = PlannerReportStrings(title = "T", sectionLink = "Trasa", lblDistance = "Odleglosc", verdictExcellent = "Doskonale")
        val report = PlannerReportBuilder.build(stateWithLink(), s)
        assertEquals("T", report.title)
        val titles = report.sections.map { it.title }
        assertTrue("Trasa" in titles)
        assertTrue(report.sections.first { it.title == "Trasa" }.rows.any { it.label == "Odleglosc" && it.value.endsWith(" km") })
        assertTrue(report.sections.first { it.title == s.sectionResults }.rows.any { it.value == "Doskonale" })
        assertTrue(report.notes.contains(s.noteWeatherNotUsed))
        assertTrue(report.notes.containsAll(s.credits))
        assertEquals(2, report.points.size)
        assertEquals(null, report.coverage)
    }

    @Test
    fun weatherUsedIsReportedWithDataTime() = runTest {
        val c = conditions()
        val st = stateWithLink { it.copy(useWeather = true) }.copy(weather = PlannerWeatherStatus.Ready(c))
        val report = PlannerReportBuilder.build(st, PlannerReportStrings())
        assertTrue(report.notes.any { it.contains("2026-10-01 12:00") && it.contains("Open-Meteo") })
        assertTrue(report.sections.any { it.title == PlannerReportStrings().sectionWeather })
    }

    @Test
    fun preciseFeederListsBreakdown() = runTest {
        val preset = FeederBuilder.presets.first { it.id == "pigtail" }
        val st = stateWithLink {
            it.copy(a = it.a.copy(feederConfig = preset.config, feederPrecise = true))
        }
        val report = PlannerReportBuilder.build(st, PlannerReportStrings())
        val rows = report.sections.first().rows
        assertTrue(rows.size > 7, "rows=${rows.size}")
    }
}
