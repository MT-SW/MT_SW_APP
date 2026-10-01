/*
 * Copyright (c) 2025 MT_SW contributors
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
package org.meshtastic.feature.map.planner.export

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PngTest {
    private fun u32(b: ByteArray, o: Int) = ((b[o].toInt() and 255) shl 24) or ((b[o + 1].toInt() and 255) shl 16) or ((b[o + 2].toInt() and 255) shl 8) or (b[o + 3].toInt() and 255)

    private class Chunk(val type: String, val off: Int, val len: Int)

    private fun chunks(b: ByteArray): List<Chunk> {
        val sig = intArrayOf(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        for (i in sig.indices) assertEquals(sig[i], b[i].toInt() and 255)
        val out = ArrayList<Chunk>()
        var p = 8
        while (p < b.size) {
            val len = u32(b, p)
            val type = String(CharArray(4) { (b[p + 4 + it].toInt() and 255).toChar() })
            assertEquals(PlannerPng.crc32(b, p + 4, 4 + len), u32(b, p + 8 + len), "CRC of $type")
            out.add(Chunk(type, p + 8, len))
            p += 12 + len
        }
        assertEquals(b.size, p)
        return out
    }

    /** Inflate of stored blocks only; verifies the zlib header/Adler-32. */
    private fun inflateStored(b: ByteArray, off: Int, len: Int): ByteArray {
        assertEquals(0x78, b[off].toInt() and 255)
        assertEquals(0, ((b[off].toInt() and 255) * 256 + (b[off + 1].toInt() and 255)) % 31)
        var p = off + 2
        val out = ArrayList<Byte>()
        while (true) {
            val hdr = b[p++].toInt()
            assertEquals(0, (hdr shr 1) and 3, "stored block")
            val n = (b[p].toInt() and 255) or ((b[p + 1].toInt() and 255) shl 8)
            val nn = (b[p + 2].toInt() and 255) or ((b[p + 3].toInt() and 255) shl 8)
            assertEquals(n.inv() and 0xFFFF, nn)
            p += 4
            for (i in 0 until n) out.add(b[p + i])
            p += n
            if ((hdr and 1) == 1) break
        }
        val raw = out.toByteArray()
        assertEquals(PlannerPng.adler32(raw), u32(b, p), "Adler-32")
        assertEquals(off + len, p + 4)
        return raw
    }

    @Test
    fun knownChecksums() {
        val s = "123456789".encodeToByteArray()
        assertEquals(0xCBF43926.toInt(), PlannerPng.crc32(s))
        assertEquals(0x091E01DE, PlannerPng.adler32(s))
        assertEquals(1, PlannerPng.adler32(ByteArray(0)))
    }

    @Test
    fun roundTripSmall() {
        val w = 3; val h = 2
        val px = intArrayOf(0xFF112233.toInt(), 0x80445566.toInt(), 0x00778899, 0xFFAABBCC.toInt(), 0xFFDDEEFF.toInt(), 0x01020304)
        val png = PlannerPng.encode(w, h, px)
        val cs = chunks(png)
        assertEquals(listOf("IHDR", "IDAT", "IEND"), cs.map { it.type })
        val ih = cs[0]
        assertEquals(13, ih.len)
        assertEquals(w, u32(png, ih.off)); assertEquals(h, u32(png, ih.off + 4))
        assertEquals(8, png[ih.off + 8].toInt()); assertEquals(6, png[ih.off + 9].toInt())
        val raw = inflateStored(png, cs[1].off, cs[1].len)
        assertEquals(h * (1 + w * 4), raw.size)
        assertEquals(0, raw[0].toInt())
        assertEquals(0x11, raw[1].toInt()); assertEquals(0x22, raw[2].toInt()); assertEquals(0x33, raw[3].toInt() and 255); assertEquals(0xFF, raw[4].toInt() and 255)
        assertEquals(0x80, raw[8].toInt() and 255)
        assertEquals(0, cs[2].len)
    }

    @Test
    fun largeImageUsesMultipleBlocks() {
        val w = 300; val h = 300
        val png = PlannerPng.encode(w, h, IntArray(w * h) { it * 2654435761L.toInt() })
        val cs = chunks(png)
        val raw = inflateStored(png, cs[1].off, cs[1].len)
        assertEquals(h * (1 + w * 4), raw.size)
        assertTrue(raw.size > 65535)
    }

    @Test
    fun profileImage() {
        val bytes = PlannerProfileImage.render(SampleData.profile())
        val cs = chunks(bytes)
        assertEquals(1200, u32(bytes, cs[0].off)); assertEquals(600, u32(bytes, cs[0].off + 4))
        val raw = inflateStored(bytes, cs[1].off, cs[1].len)
        assertEquals(600 * (1 + 1200 * 4), raw.size)
        var nonWhite = 0
        for (i in 0 until 1200 * 600) {
            val o = 1 + (i / 1200) * (1 + 4800) + (i % 1200) * 4
            if (raw[o].toInt() != -1 || raw[o + 1].toInt() != -1) nonWhite++
        }
        assertTrue(nonWhite > 20000, "nonWhite=$nonWhite")
        assertEquals(bytes.toList(), PlannerProfileImage.render(SampleData.profile()).toList())
        val small = PlannerProfileImage.render(SampleData.profile(), 300, 200)
        chunks(small)
    }

    @Test
    fun fontCovers95Glyphs() {
        assertEquals(95, BitmapFont.count)
        assertEquals('a', BitmapFont.fold('ą'))
        assertEquals('Z', BitmapFont.fold('Ż'))
    }
}
