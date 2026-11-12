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
        assertEquals(
            "\"data\",\"godzina\",\"rozmowa\",\"nadawca\",\"tekst\",\"czas_ms\",\"kanal\"," +
                "\"indeks_kanalu\",\"adres\",\"id_nadawcy\",\"moja\",\"id_wiadomosci\"",
            csv[0],
        )
        assertEquals(
            "\"2026-11-12\",\"16:05:10\",\"Kanał 0\",\"Jan\",\"Cześć \"\"wszystkim\"\"\"," +
                "\"0\",\"\",\"0\",\"\",\"\",\"0\",\"0\"",
            csv[1],
        )
    }

    @Test
    fun csvReadsBackWithTheChannelNameAndMultilineText() {
        val written =
            listOf(
                ArchiveEntry(
                    date = "2026-11-12",
                    time = "16:05:10",
                    conversation = "Główny",
                    sender = "Jan",
                    text = "Cześć \"wszystkim\", ok",
                    millis = 1_762_960_000_000L,
                    channelName = "Główny",
                    channelIndex = 0,
                    address = "^all",
                    senderId = "!aabbccdd",
                    fromLocal = false,
                    packetId = 77,
                ),
                ArchiveEntry(
                    date = "2026-11-12",
                    time = "16:06:00",
                    conversation = "Anna",
                    sender = "Ja",
                    text = "linia 1\nlinia 2",
                    millis = 1_762_960_060_000L,
                    channelName = "",
                    channelIndex = 8,
                    address = "!11223344",
                    senderId = "!99887766",
                    fromLocal = true,
                    packetId = 78,
                ),
            )
        val read = parseArchiveCsv(formatArchive(written, MessageArchiveConfig.FORMAT_CSV))
        assertEquals(2, read.size)
        assertEquals(
            ArchiveRecord(1_762_960_000_000L, "Cześć \"wszystkim\", ok", "Główny", 0, "^all", "!aabbccdd", false, 77),
            read[0],
        )
        assertEquals(
            ArchiveRecord(1_762_960_060_000L, "linia 1\nlinia 2", "", 8, "!11223344", "!99887766", true, 78),
            read[1],
        )
    }

    @Test
    fun csvFromBeforeRestoringGivesNothing() {
        val old =
            "\"data\",\"godzina\",\"rozmowa\",\"nadawca\",\"tekst\"\n" +
                "\"2026-11-12\",\"16:05:10\",\"Kanał 0\",\"Jan\",\"x\"\n"
        assertEquals(emptyList(), parseArchiveCsv(old))
    }

    @Test
    fun markdownMakesAList() {
        assertEquals(
            "- **16:05:10** · Kanał 0 · **Jan**: Cześć \"wszystkim\"\n" +
                "- **16:06:00** · Anna · **Anna**: linia 1 linia 2\n",
            formatArchive(entries, MessageArchiveConfig.FORMAT_MARKDOWN),
        )
    }

    @Test
    fun fileNamesAreSafe() {
        assertEquals("Kanał_0", safeFileName("Kanał 0"))
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

    @Test
    fun configWithPasswordReadsBackTheSame() {
        val config = MessageArchiveConfig(true, "content://x/tree/a|b", 60, 1, true, "hasło|z \"ąę\"")
        assertEquals(config, MessageArchiveConfig.decode(config.encode()))
        // Settings saved before the password existed have no prefix and give no password.
        val old = MessageArchiveConfig(true, "folder", 15, 0, false)
        assertEquals("", MessageArchiveConfig.decode(old.encode()).password)
        assertEquals(MessageArchiveConfig(), MessageArchiveConfig.decode("e|zz|1|15|0|0|x"))
    }
}
