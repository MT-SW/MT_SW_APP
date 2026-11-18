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
package org.meshtastic.feature.settings.console

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConsoleEngineTest {
    private val registry =
        ConsoleRegistry(
            listOf(
                ConsoleCommand("pomoc", "pomoc", "pomoc", run = { ConsoleResult(listOf("pomoc ${it.size}")) }),
                ConsoleCommand(
                    name = "połącz",
                    summary = "łączy",
                    usage = "połącz <n>",
                    complete = { if (it.isEmpty()) listOf("1", "2") else emptyList() },
                    run = { ConsoleResult(listOf("łączę ${it.firstOrNull()}")) },
                ),
                ConsoleCommand(
                    name = "most",
                    summary = "most",
                    usage = "most",
                    complete = { listOf("włącz", "wyłącz") },
                    run = { ConsoleResult() },
                ),
            ),
        ) {
            "nie ma: $it"
        }

    @Test
    fun `words are split on spaces and quotes keep a text together`() {
        assertEquals(listOf("a", "b c", "d"), ConsoleText.split("  a \"b c\"  d "))
        assertEquals(emptyList(), ConsoleText.split("   "))
    }

    @Test
    fun `a command is found without accents and in any case`() = runTest {
        assertEquals(listOf("łączę 2"), registry.execute("POLACZ 2").lines)
    }

    @Test
    fun `an unknown word gives the unknown text and an empty line gives nothing`() = runTest {
        assertEquals(listOf("nie ma: xyz"), registry.execute("xyz").lines)
        assertEquals(emptyList(), registry.execute("  ").lines)
    }

    @Test
    fun `the first word is completed from the commands`() {
        assertEquals(listOf("pomoc"), registry.suggestions("pom"))
        assertEquals(listOf("połącz"), registry.suggestions("polac"))
        assertEquals(emptyList(), registry.suggestions("pomoc"))
    }

    @Test
    fun `an argument is completed from the command and keeps what was typed`() {
        assertEquals(listOf("polacz 1", "polacz 2"), registry.suggestions("polacz "))
        assertEquals(listOf("most wyłącz"), registry.suggestions("most wyl"))
    }

    @Test
    fun `history goes back and forth and forgets repeats`() {
        val history = ConsoleHistory()
        history.add("a")
        history.add("a")
        history.add("b")

        assertEquals("b", history.previous())
        assertEquals("a", history.previous())
        assertNull(history.previous())
        assertEquals("b", history.next())
        assertEquals("", history.next())
        assertNull(history.next())
    }
}
