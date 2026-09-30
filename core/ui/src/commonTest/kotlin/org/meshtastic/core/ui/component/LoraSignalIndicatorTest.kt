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
package org.meshtastic.core.ui.component

import org.meshtastic.core.model.snrLimit
import org.meshtastic.proto.Config.LoRaConfig.ModemPreset
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests for signal-quality rating: preset-relative bands (narrow: -3/-7/-12, lite: -5/-10/-15 dB); RSSI over noise floor can only lower the tier. */
class LoraSignalIndicatorTest {

    // Firmware spreading factors (MeshRadio.h) paired with literal Semtech demodulation floors.
    // Keep expected values independent of the production formula.
    private val expectedFloors =
        mapOf(
            ModemPreset.SHORT_TURBO to (7 to -7.5f),
            ModemPreset.SHORT_FAST to (7 to -7.5f),
            ModemPreset.SHORT_SLOW to (8 to -10f),
            ModemPreset.MEDIUM_FAST to (9 to -12.5f),
            ModemPreset.MEDIUM_SLOW to (10 to -15f),
            ModemPreset.MEDIUM_TURBO to (9 to -12.5f),
            ModemPreset.LONG_TURBO to (11 to -17.5f),
            ModemPreset.LONG_FAST to (11 to -17.5f),
            ModemPreset.LONG_MODERATE to (11 to -17.5f),
            ModemPreset.LONG_SLOW to (12 to -20f),
            ModemPreset.VERY_LONG_SLOW to (12 to -20f),
            ModemPreset.LITE_FAST to (9 to -12.5f),
            ModemPreset.LITE_SLOW to (10 to -15f),
            ModemPreset.NARROW_FAST to (7 to -7.5f),
            ModemPreset.NARROW_SLOW to (8 to -10f),
            ModemPreset.TINY_FAST to (7 to -7.5f),
            ModemPreset.TINY_SLOW to (8 to -10f),
        )

    @Test
    fun `every preset's snrLimit is its spreading factor's demod floor`() {
        expectedFloors.forEach { (preset, expected) ->
            val (sf, floor) = expected
            assertEquals(floor, preset.snrLimit, "$preset (SF$sf)")
        }
    }

    @Test
    fun `presets sharing a spreading factor share a floor regardless of bandwidth`() {
        // Bandwidth changes sensitivity in dBm, not the demodulation SNR floor.
        assertEquals(ModemPreset.LONG_FAST.snrLimit, ModemPreset.LONG_TURBO.snrLimit)
        assertEquals(ModemPreset.MEDIUM_FAST.snrLimit, ModemPreset.MEDIUM_TURBO.snrLimit)
        assertEquals(ModemPreset.SHORT_FAST.snrLimit, ModemPreset.SHORT_TURBO.snrLimit)
        assertEquals(ModemPreset.SHORT_FAST.snrLimit, ModemPreset.TINY_FAST.snrLimit)
    }

    @Test
    fun `every ModemPreset is covered by the firmware spreading-factor table`() {
        // A preset added to the proto without a row above would otherwise be rated against LongFast's floor silently.
        val uncovered =
            ModemPreset.entries
                .filter { it.name != "UNSET" && it.name != "UNRECOGNIZED" }
                .filter { it !in expectedFloors }
        assertEquals(emptyList(), uncovered, "presets missing from expectedFloors")
    }

    @Test
    fun `LongTurbo is rated at its SF11 floor rather than SF9`() {
        assertEquals(-17.5f, ModemPreset.LONG_TURBO.snrLimit)
    }

    @Test
    fun `LONG_SLOW uses physically-correct SF12 floor`() {
        assertEquals(-20f, ModemPreset.LONG_SLOW.snrLimit)
        assertEquals(-20f, ModemPreset.VERY_LONG_SLOW.snrLimit)
    }

    @Test
    fun `null preset falls back to the LongFast default limit`() {
        val noPreset: ModemPreset? = null
        assertEquals(-17.5f, noPreset.snrLimit)
    }

    @Test
    fun `default presets rate relative to the preset floor`() {
        val preset = ModemPreset.LONG_FAST // limit -17.5
        assertEquals(Quality.GOOD, determineSignalQuality(snr = -17f, modemPreset = preset))
        assertEquals(Quality.FAIR, determineSignalQuality(snr = -17.5f, modemPreset = preset))
        assertEquals(Quality.FAIR, determineSignalQuality(snr = -22f, modemPreset = preset))
        assertEquals(Quality.BAD, determineSignalQuality(snr = -23f, modemPreset = preset))
        assertEquals(Quality.NONE, determineSignalQuality(snr = -30f, modemPreset = preset))
    }

    @Test
    fun `the same SNR is rated relative to the preset`() {
        assertEquals(Quality.GOOD, determineSignalQuality(snr = -15f, modemPreset = ModemPreset.LONG_SLOW))
        assertEquals(Quality.BAD, determineSignalQuality(snr = -15f, modemPreset = ModemPreset.SHORT_FAST))
    }

    @Test
    fun `narrow presets use fixed -3 -7 -12 bands`() {
        for (preset in listOf(ModemPreset.NARROW_FAST, ModemPreset.NARROW_SLOW)) {
            assertEquals(Quality.GOOD, determineSignalQuality(snr = -2.9f, modemPreset = preset))
            assertEquals(Quality.FAIR, determineSignalQuality(snr = -3f, modemPreset = preset))
            assertEquals(Quality.FAIR, determineSignalQuality(snr = -6.9f, modemPreset = preset))
            assertEquals(Quality.BAD, determineSignalQuality(snr = -7f, modemPreset = preset))
            assertEquals(Quality.BAD, determineSignalQuality(snr = -12f, modemPreset = preset))
            assertEquals(Quality.NONE, determineSignalQuality(snr = -12.1f, modemPreset = preset))
        }
    }

    @Test
    fun `lite presets use fixed -5 -10 -15 bands`() {
        for (preset in listOf(ModemPreset.LITE_FAST, ModemPreset.LITE_SLOW)) {
            assertEquals(Quality.GOOD, determineSignalQuality(snr = -4.9f, modemPreset = preset))
            assertEquals(Quality.FAIR, determineSignalQuality(snr = -5f, modemPreset = preset))
            assertEquals(Quality.BAD, determineSignalQuality(snr = -10f, modemPreset = preset))
            assertEquals(Quality.BAD, determineSignalQuality(snr = -15f, modemPreset = preset))
            assertEquals(Quality.NONE, determineSignalQuality(snr = -15.1f, modemPreset = preset))
        }
    }

    @Test
    fun `RSSI alone or noise floor alone does not influence the rating`() {
        val preset = ModemPreset.NARROW_FAST
        assertEquals(Quality.GOOD, determineSignalQuality(snr = -2f, modemPreset = preset, rssi = -140))
        assertEquals(Quality.GOOD, determineSignalQuality(snr = -2f, modemPreset = preset, noiseFloor = -70))
    }

    @Test
    fun `RSSI over noise floor lowers the rating when worse than SNR`() {
        val preset = ModemPreset.NARROW_FAST
        assertEquals(Quality.BAD, determineSignalQuality(snr = -2f, modemPreset = preset, rssi = -90, noiseFloor = -80))
        assertEquals(Quality.FAIR, determineSignalQuality(snr = -2f, modemPreset = preset, rssi = -84, noiseFloor = -80))
    }

    @Test
    fun `a good RSSI margin never improves a worse SNR`() {
        val preset = ModemPreset.NARROW_FAST
        assertEquals(Quality.BAD, determineSignalQuality(snr = -9f, modemPreset = preset, rssi = -60, noiseFloor = -80))
    }

    @Test
    fun `a zero SNR reading is rated rather than treated as missing`() {
        assertEquals(Quality.GOOD, determineSignalQuality(snr = 0f, modemPreset = ModemPreset.LONG_FAST))
    }

    @Test
    fun `absent SNR is not a quality band`() {
        assertEquals(listOf(Quality.NONE, Quality.BAD, Quality.FAIR, Quality.GOOD), Quality.entries.toList())
    }

    @Test
    fun rssiQuality_usesFixedBandsForAllPresets() {
        assertEquals(Quality.GOOD, determineRssiQuality(-114))
        assertEquals(Quality.FAIR, determineRssiQuality(-115))
        assertEquals(Quality.FAIR, determineRssiQuality(-119))
        assertEquals(Quality.BAD, determineRssiQuality(-120))
        assertEquals(Quality.BAD, determineRssiQuality(-125))
        assertEquals(Quality.NONE, determineRssiQuality(-126))
    }

    @Test
    fun snrBands_followThePreset() {
        assertEquals(SnrBands(-3f, -7f, -12f), ModemPreset.NARROW_FAST.snrBands())
        assertEquals(SnrBands(-5f, -10f, -15f), ModemPreset.LITE_SLOW.snrBands())
        assertEquals(SnrBands(-17.5f, -23f, -25f), ModemPreset.LONG_FAST.snrBands())
    }

    @Test
    fun formatThreshold_dropsTrailingZero() {
        assertEquals("-3", formatThreshold(-3f))
        assertEquals("-7.5", formatThreshold(-7.5f))
    }
}
