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

import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8
import okio.IOException

/**
 * MT_SW: the file of "Kopia danych aplikacji". It holds all the app settings (the same JSON as the settings backup)
 * followed by the radio's database, so one file brings back everything. A plain database file from an older version
 * has no header and is still accepted everywhere a backup is read.
 */
object AppDataBundle {
    private val MAGIC = "MTSWBK01\n".encodeUtf8()
    private const val MAX_SETTINGS_BYTES = 16 * 1024 * 1024

    /** Writes the header with the settings; the database follows straight after it. */
    fun writeHeader(sink: BufferedSink, settingsJson: ByteArray) {
        sink.write(MAGIC)
        sink.writeInt(settingsJson.size)
        sink.write(settingsJson)
    }

    /**
     * Reads and removes the header from [source], leaving the database as the rest of it. Returns the settings, or
     * null when [source] is a plain database without a header (nothing is consumed then).
     */
    fun readHeader(source: BufferedSource): ByteArray? {
        if (!source.rangeEquals(0, MAGIC)) return null
        source.skip(MAGIC.size.toLong())
        val size = source.readInt()
        if (size < 0 || size > MAX_SETTINGS_BYTES) throw IOException("Damaged settings part of the backup")
        return source.readByteArray(size.toLong())
    }
}
