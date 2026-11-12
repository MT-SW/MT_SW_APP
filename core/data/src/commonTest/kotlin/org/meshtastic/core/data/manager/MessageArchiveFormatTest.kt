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

package org.meshtastic.core.data.manager

import org.meshtastic.core.repository.MessageArchiveConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageArchiveFormatTest {
    private val entries =
        listOf(
            ArchiveEntry("2026-11-12", "16:05:10", "Kanał 0", "Jan", "Cześć \"wszystkim\""),
            ArchiveEntry("2026-11-12", "16:06:00", "Anna", "Anna", "linia 1\nlinia 2"),
        )

    @Test
    fun textKeepsOneLinePerMessage() {
        assertEquals(
            "[2026-11-12 16:05:10] Kanał 0 | Jan: Cześć \"wszystkim\"\n" +
                "[2026-11-12 16:06:00] Anna | Anna: linia 1 linia 2\n",
            formatArchive(entries, MessageArchiveConfig.FORMAT_TEXT),
        )
    }

    @Test
    fun csvQuotesEveryCell() {
        val csv = formatArchive(entries, MessageArchiveConfig.FORMAT_CSV).lines()
        assertEquals("\"data\",\"godzina\",\"rozmowa\",\"nadawca\",\"tekst\"", csv[0])
        assertEquals("\"2026-11-12\",\"16:05:10\",\"Kanał 0\",\"Jan\",\"Cześć \"\"wszystkim\"\"\"", csv[1])
    }

    @Test
    fun markdownMakesAList() {
        assertEquals(
            "- **16:05:10** · Kanał 0 · **Jan**: Cześć \"wszystkim\"\n- **16:06:00** · Anna · **Anna**: linia 1 linia 2\n",
            formatArchive(entries, MessageArchiveConfig.FORMAT_MARKDOWN),
        )
    }

    @Test
    fun fileNamesAreSafe() {
        assertEquals("Kana__0".trim('_').ifEmpty { "x" }.let { safeFileName("Kanał 0") }, safeFileName("Kanał 0"))
        assertEquals("rozmowa", safeFileName("///"))
        assertEquals("csv", archiveExtension(MessageArchiveConfig.FORMAT_CSV))
        assertEquals("md", archiveExtension(MessageArchiveConfig.FORMAT_MARKDOWN))
        assertEquals("txt", archiveExtension(99))
    }

    @Test
    fun configReadsBackTheSame() {
        val config = MessageArchiveConfig(true, "content://x/tree/a|b", 60, MessageArchiveConfig.FORMAT_CSV, true)
        assertEquals(config, MessageArchiveConfig.decode(config.encode()))
        assertEquals(MessageArchiveConfig(), MessageArchiveConfig.decode("zle"))
    }
}
