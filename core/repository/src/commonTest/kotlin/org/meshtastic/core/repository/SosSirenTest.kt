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
package org.meshtastic.core.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SosSirenTest {
    @Test
    fun pcmHasWholePairsOfTonesInSixteenBitSamples() {
        val oneSecond = SosSiren.pcm(1.4)
        // 4 tones x 0.35 s at 44100 Hz, two bytes a sample.
        assertEquals(2 * 15_435 * 2, oneSecond.size)
        assertTrue(oneSecond.any { it.toInt() != 0 })
    }

    @Test
    fun wavStartsWithAValidHeader() {
        val wav = SosSiren.wav(1.4)
        assertEquals("RIFF", wav.copyOfRange(0, 4).decodeToString())
        assertEquals("WAVEfmt ", wav.copyOfRange(8, 16).decodeToString())
        assertEquals("data", wav.copyOfRange(36, 40).decodeToString())
        assertEquals(SosSiren.pcm(1.4).size + 44, wav.size)
    }

    @Test
    fun bellHasWholeCyclesAndEndsInSilence() {
        val bell = SosSiren.bellPcm(2.0)
        assertEquals(88_200 * 2, bell.size)
        assertTrue(bell.any { it.toInt() != 0 })
        // The last tenth of a second is a pause, so a loop has no click.
        assertTrue(bell.copyOfRange(bell.size - 8_820, bell.size).all { it.toInt() == 0 })
        assertEquals(bell.size + 44, SosSiren.bellWav(2.0).size)
    }
}
