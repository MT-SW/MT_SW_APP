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
@file:Suppress("MagicNumber", "ReturnCount", "NestedBlockDepth")

package org.meshtastic.feature.map.trip

import co.touchlab.kermit.Logger
import org.meshtastic.core.model.util.crc32

/**
 * A minimal ZIP archive for a hike with its pictures: files are stored without compression (the pictures are already
 * compressed JPEG files and the route file is small), so the same code works on every platform and the archive opens in
 * any ZIP program. Only archives written here are read back; an archive with compressed entries is refused.
 */
object TripArchive {
    private const val LOCAL_SIG = 0x04034b50
    private const val CENTRAL_SIG = 0x02014b50
    private const val END_SIG = 0x06054b50
    private const val VERSION = 20
    private const val FLAG_UTF8 = 0x0800
    private const val DOS_DATE_1980 = 33
    private const val END_SIZE = 22
    private const val MAX_COMMENT = 0xFFFF
    private const val MAX_ENTRIES = 200

    /** True when [bytes] start like a ZIP archive. */
    fun looksLikeZip(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()

    /** Packs [entries] (name to content) into one archive. */
    fun write(entries: List<Pair<String, ByteArray>>): ByteArray {
        val out = ByteWriter()
        val central = ByteWriter()
        entries.forEach { (name, data) ->
            val nameBytes = name.encodeToByteArray()
            val crc = crc32(data).toInt()
            val offset = out.size
            out.int(LOCAL_SIG)
            out.short(VERSION)
            out.short(FLAG_UTF8)
            out.short(0)
            out.short(0)
            out.short(DOS_DATE_1980)
            out.int(crc)
            out.int(data.size)
            out.int(data.size)
            out.short(nameBytes.size)
            out.short(0)
            out.bytes(nameBytes)
            out.bytes(data)
            central.int(CENTRAL_SIG)
            central.short(VERSION)
            central.short(VERSION)
            central.short(FLAG_UTF8)
            central.short(0)
            central.short(0)
            central.short(DOS_DATE_1980)
            central.int(crc)
            central.int(data.size)
            central.int(data.size)
            central.short(nameBytes.size)
            central.short(0)
            central.short(0)
            central.short(0)
            central.short(0)
            central.int(0)
            central.int(offset)
            central.bytes(nameBytes)
        }
        val centralOffset = out.size
        out.bytes(central.toByteArray())
        out.int(END_SIG)
        out.short(0)
        out.short(0)
        out.short(entries.size)
        out.short(entries.size)
        out.int(central.size)
        out.int(centralOffset)
        out.short(0)
        Logger.i { "Wędrówki: archiwum zapisane, plików ${entries.size}, ${out.size} B" }
        return out.toByteArray()
    }

    /**
     * Reads the archive into name to content, or null when it is not an archive written by [write]. An entry bigger
     * than [maxEntryBytes] makes the whole archive refused.
     */
    fun read(bytes: ByteArray, maxEntryBytes: Int): Map<String, ByteArray>? {
        val end = findEnd(bytes) ?: return fail("brak końca archiwum")
        val count = u16(bytes, end + 10)
        var pos = u32(bytes, end + 16)
        if (count > MAX_ENTRIES || pos < 0 || pos >= bytes.size) return fail("zła liczba plików")
        val result = linkedMapOf<String, ByteArray>()
        repeat(count) {
            if (pos + 46 > bytes.size || u32(bytes, pos) != CENTRAL_SIG) return fail("zły wpis spisu")
            val method = u16(bytes, pos + 10)
            val crc = u32(bytes, pos + 16)
            val size = u32(bytes, pos + 24)
            val nameLen = u16(bytes, pos + 28)
            val extraLen = u16(bytes, pos + 30)
            val commentLen = u16(bytes, pos + 32)
            val local = u32(bytes, pos + 42)
            if (pos + 46 + nameLen > bytes.size) return fail("zła nazwa pliku")
            val name = bytes.decodeToString(pos + 46, pos + 46 + nameLen)
            if (method != 0) return fail("plik $name jest skompresowany")
            if (size < 0 || size > maxEntryBytes) return fail("plik $name jest za duży")
            if (local < 0 || local + 30 > bytes.size || u32(bytes, local) != LOCAL_SIG) return fail("zły nagłówek")
            val start = local + 30 + u16(bytes, local + 26) + u16(bytes, local + 28)
            if (start + size > bytes.size) return fail("plik $name ucięty")
            val data = bytes.copyOfRange(start, start + size)
            if (crc32(data).toInt() != crc) return fail("plik $name uszkodzony")
            result[name] = data
            pos += 46 + nameLen + extraLen + commentLen
        }
        return result
    }

    private fun fail(reason: String): Map<String, ByteArray>? {
        Logger.w { "Wędrówki: archiwum odrzucone: $reason" }
        return null
    }

    private fun findEnd(bytes: ByteArray): Int? {
        val first = maxOf(0, bytes.size - END_SIZE - MAX_COMMENT)
        var i = bytes.size - END_SIZE
        while (i >= first) {
            if (u32(bytes, i) == END_SIG) return i
            i--
        }
        return null
    }

    private fun u16(b: ByteArray, at: Int): Int = (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8)

    private fun u32(b: ByteArray, at: Int): Int = u16(b, at) or (u16(b, at + 2) shl 16)

    private class ByteWriter {
        private var buffer = ByteArray(1024)
        var size = 0
            private set

        private fun ensure(extra: Int) {
            if (size + extra > buffer.size) buffer = buffer.copyOf(maxOf(buffer.size * 2, size + extra))
        }

        fun short(v: Int) {
            ensure(2)
            buffer[size++] = v.toByte()
            buffer[size++] = (v shr 8).toByte()
        }

        fun int(v: Int) {
            short(v and 0xFFFF)
            short((v shr 16) and 0xFFFF)
        }

        fun bytes(data: ByteArray) {
            ensure(data.size)
            data.copyInto(buffer, size)
            size += data.size
        }

        fun toByteArray(): ByteArray = buffer.copyOf(size)
    }
}
