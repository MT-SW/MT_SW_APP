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

import org.meshtastic.proto.Config.LoRaConfig.ModemPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlannerBandsTest {
    @Test
    fun defaultFrequencyFollowsThePreset() {
        assertEquals(869.44165, PlannerBands.defaultFrequencyMHz("868", 62.5), 1e-9)
        assertEquals(869.525, PlannerBands.defaultFrequencyMHz("868", 250.0), 1e-9)
        assertEquals(433.5, PlannerBands.defaultFrequencyMHz("433", 62.5), 1e-9)
    }

    @Test
    fun bandListHasAllBands() {
        val ids = PlannerBands.all.map { it.id }
        assertEquals(listOf("169", "433", "470", "868", "915", "923", "2400", "free"), ids)
        assertNotNull(PlannerBands.byId("868"))
    }

    @Test
    fun idForFrequency() {
        assertEquals("868", PlannerBands.idFor(869.525))
        assertEquals("433", PlannerBands.idFor(433.175))
        assertEquals("2400", PlannerBands.idFor(2425.0))
        assertEquals("free", PlannerBands.idFor(1200.0))
    }

    @Test
    fun clamp() {
        assertEquals(20.0, PlannerBands.clampMHz(5.0))
        assertEquals(20000.0, PlannerBands.clampMHz(99999.0))
    }

    @Test
    fun presetsMapToBandwidthAndSf() {
        assertEquals(PlannerRadio(250.0, 11), plannerRadioFromPreset(ModemPreset.LONG_FAST))
        assertEquals(PlannerRadio(62.5, 7), plannerRadioFromPreset(ModemPreset.NARROW_FAST))
        assertEquals(PlannerRadio(125.0, 10), plannerRadioFromPreset(ModemPreset.LITE_SLOW))
        assertEquals(PlannerRadio(15.625, 7), plannerRadioFromPreset(ModemPreset.TINY_FAST))
        assertEquals(PlannerRadio(250.0, 11), plannerRadioFromPreset(null))
    }

    @Test
    fun sensitivityWorksForNarrowAndLowSf() {
        // SF7 / 62.5 kHz: -174 + 10 log10(62500) + 6 - 7.5
        val s = Sensitivity.dbm(62.5, 7, 6.0)
        assertEquals(-174.0 + 10.0 * kotlin.math.log10(62500.0) + 6.0 - 7.5, s, 1e-9)
        assertEquals(-2.5, Sensitivity.snrLimitDb(5))
        assertEquals(-5.0, Sensitivity.snrLimitDb(6))
        assertEquals(-7.5, Sensitivity.snrLimitDb(7))
        assertTrue(Sensitivity.dbm(15.625, 8, 6.0) < Sensitivity.dbm(250.0, 8, 6.0))
    }
}
