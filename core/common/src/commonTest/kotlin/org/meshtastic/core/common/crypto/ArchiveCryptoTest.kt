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
package org.meshtastic.core.common.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ArchiveCryptoTest {
    private fun ByteArray.hex() = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    @Test
    fun pbkdf2MatchesTheRfc7914Vector() {
        val key = pbkdf2Sha256("passwd".encodeToByteArray(), "salt".encodeToByteArray(), 1, 64)
        assertEquals(
            "55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc" +
                "49ca9cccf179b645991664b39d77ef317c71b845b1e30bd509112041d3a19783",
            key.hex(),
        )
    }

    @Test
    fun encryptedFileReadsBackWithThePassword() {
        val text = "Cześć, to jest wiadomość 123\nlinia 2".encodeToByteArray()
        val encrypted = ArchiveCrypto.encrypt("tajne", text)
        assertEquals("Salted__", encrypted.copyOfRange(0, 8).decodeToString())
        assertEquals(16 + text.size, encrypted.size)
        assertFalse(encrypted.copyOfRange(16, encrypted.size).contentEquals(text))
        assertContentEquals(text, ArchiveCrypto.decrypt("tajne", encrypted))
    }

    @Test
    fun wrongPasswordGivesOtherBytesAndEveryFileGetsItsOwnSalt() {
        val text = "wiadomość".encodeToByteArray()
        val first = ArchiveCrypto.encrypt("tajne", text)
        val second = ArchiveCrypto.encrypt("tajne", text)
        assertFalse(first.contentEquals(second))
        assertFalse(text.contentEquals(ArchiveCrypto.decrypt("inne", first)))
        assertNotNull(ArchiveCrypto.decrypt("tajne", second))
    }

    @Test
    fun aFileWithoutTheHeaderIsNotDecrypted() {
        assertNull(ArchiveCrypto.decrypt("tajne", "zwykły tekst bez nagłówka".encodeToByteArray()))
        assertNull(ArchiveCrypto.decrypt("tajne", ByteArray(3)))
    }
}
