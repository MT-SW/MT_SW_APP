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
import kotlin.test.assertTrue

class KnowledgeIndexBaseTest {

    private val base =
        """
        # Anteny

        ### Czy można włączyć radio bez anteny?
        Nie. Sygnał nadawany bez anteny może uszkodzić urządzenie.
        Słowa: spalenie, wzmacniacz mocy
        Źródło: https://example.org/a (sprawdzono 2026-10)

        ### Jak wgrać firmware na nRF52?
        Metodą przeciągnij i upuść, bez sterowników.
        Słowa: UF2, bootloader
        Źródło: https://example.org/b (sprawdzono 2026-10)
        """
            .trimIndent()

    private val manual = "# Podręcznik\n\n### Ustawienia\nAntena w aplikacji: kalkulator anten pokazuje straty toru kabla i złączy."

    private val index =
        KnowledgeIndex(
            listOf(
                KnowledgeIndex.Document("Podręcznik", manual),
                KnowledgeIndex.Document("Baza MT_SW: Anteny", base, base = true),
            ),
        )

    @Test
    fun baseIsFoundOnlyWhenAsked() {
        assertTrue(!index.lookup("czy bez anteny uszkodzę urządzenie spalenie", 2000).contains("Źródło"))
        assertTrue(index.lookup("czy bez anteny uszkodzę urządzenie", 2000, includeBase = true).contains("Źródło: https://example.org/a"))
    }

    @Test
    fun hiddenKeywordsFindEntryButAreNotShown() {
        val found = index.lookup("spalenie wzmacniacz", 2000, includeBase = true)
        assertTrue(found.contains("bez anteny"))
        assertTrue(!found.contains("Słowa"))
    }

    @Test
    fun sourceStaysWithItsEntry() {
        val found = index.lookup("bootloader UF2 nRF52", 2000, includeBase = true)
        assertTrue(found.contains("przeciągnij"))
        assertTrue(found.contains("https://example.org/b"))
    }
}
