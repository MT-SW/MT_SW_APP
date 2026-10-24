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
package org.meshtastic.feature.messaging.translation

import org.meshtastic.core.model.ChannelOption
import org.meshtastic.core.model.antenna.AntennaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AssistantCalculatorsTest {
    private fun pl(q: String) = AssistantCalculators.answer(q, polish = true)

    @Test
    fun antennaLengthsAreComputed() {
        val a = assertNotNull(pl("Jak zrobić prostą antenę dipol na 868 MHz?"))
        assertTrue("34,5 cm" in a, a)
        assertTrue("Dipol półfalowy" in a && "każde ramię" in a, a)
        assertTrue("Jak zrobić dipol" in a, a)
        val q = assertNotNull(pl("Jaka antena ćwierćfalowa na 433 MHz, ile cm?"))
        assertTrue("16,6 cm" in q, q)
    }

    @Test
    fun antennaWithoutFrequencyUsesOurNetwork() {
        val a = assertNotNull(pl("Ile ma mieć centymetrów antena 5/8?"))
        assertTrue("naszej sieci" in a && "20,5 cm" in a, a)
        assertEquals(AntennaType.FIVE_EIGHTHS, AssistantCalculators.lastAntenna?.first)
    }

    @Test
    fun powerConversions() {
        assertTrue("501 mW" in assertNotNull(pl("Ile to 27 dBm w mW?")))
        assertTrue("27 dBm" in assertNotNull(pl("0,5 W ile to dBm?")))
    }

    @Test
    fun arithmeticAndPercent() {
        assertEquals("2+2*3 = 8", pl("Policz 2+2*3"))
        assertTrue("= 30" in assertNotNull(pl("ile to 15% z 200?")))
    }

    @Test
    fun airtimeOfNarrowFast() {
        val a = assertNotNull(pl("Ile trwa wysłanie wiadomości 50 znaków na Narrow Fast?"))
        assertTrue("316 ms" in a, a)
        assertEquals(316, AssistantCalculators.airtimeMs(ChannelOption.NARROW_FAST, 70).toInt() + 1)
    }

    @Test
    fun ordinaryQuestionsAreNotCalculations() {
        assertNull(pl("Ile wiadomości było dzisiaj na kanale MT_SW?"))
        assertNull(pl("Ile mam teraz węzłów w sieci?"))
        assertNull(pl("Czy mam zasięg do DNL?"))
        assertNull(pl("Mam słaby zasięg — co sprawdzić i pokaż mi odpowiednie ustawienie."))
        assertNull(pl("Gdzie ustawić zysk anteny?"))
        assertNull(pl("Kto może nadawać na 868 MHz w Polsce?"))
    }

    @Test
    fun rangeAndPropagation() {
        assertTrue("km" in assertNotNull(pl("Jaki teoretyczny zasięg na Narrow Fast przy 27 dBm?")))
        assertTrue("dB" in assertNotNull(pl("Jakie tłumienie w wolnej przestrzeni na 10 km?")))
        assertTrue("m." in assertNotNull(pl("Jaka strefa Fresnela dla 20 km?")))
        assertTrue("km" in assertNotNull(pl("Jaki horyzont radiowy z masztu 30 m?")))
    }
}
