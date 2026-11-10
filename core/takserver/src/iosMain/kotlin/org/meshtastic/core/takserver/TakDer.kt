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
package org.meshtastic.core.takserver

/**
 * Minimal DER writer for the per-install TAK PKI on iOS (X.509 certificates and PKCS#12 containers). Pure Kotlin: iOS
 * has no public API that builds certificates, and BouncyCastle (used on Android and the computer) is JVM only.
 */
internal object Der {
    fun concat(vararg parts: ByteArray): ByteArray {
        val out = ByteArray(parts.sumOf { it.size })
        var pos = 0
        for (p in parts) {
            p.copyInto(out, pos)
            pos += p.size
        }
        return out
    }

    private fun length(n: Int): ByteArray = when {
        n < 0x80 -> byteArrayOf(n.toByte())
        n < 0x100 -> byteArrayOf(0x81.toByte(), n.toByte())
        n < 0x10000 -> byteArrayOf(0x82.toByte(), (n shr 8).toByte(), n.toByte())
        else -> byteArrayOf(0x83.toByte(), (n shr 16).toByte(), (n shr 8).toByte(), n.toByte())
    }

    fun tlv(tag: Int, content: ByteArray): ByteArray = concat(byteArrayOf(tag.toByte()), length(content.size), content)

    fun seq(vararg parts: ByteArray): ByteArray = tlv(0x30, concat(*parts))

    fun set(vararg parts: ByteArray): ByteArray = tlv(0x31, concat(*parts))

    fun octet(bytes: ByteArray): ByteArray = tlv(0x04, bytes)

    fun bitString(bytes: ByteArray, unusedBits: Int = 0): ByteArray = tlv(0x03, concat(byteArrayOf(unusedBits.toByte()), bytes))

    fun nullValue(): ByteArray = byteArrayOf(0x05, 0x00)

    fun bool(value: Boolean): ByteArray = tlv(0x01, byteArrayOf(if (value) 0xFF.toByte() else 0x00))

    fun utf8(text: String): ByteArray = tlv(0x0C, text.encodeToByteArray())

    /** BMPString (UTF-16BE), used for the PKCS#12 friendly name. */
    fun bmp(text: String): ByteArray {
        val out = ByteArray(text.length * 2)
        text.forEachIndexed { i, c ->
            out[i * 2] = (c.code shr 8).toByte()
            out[i * 2 + 1] = c.code.toByte()
        }
        return tlv(0x1E, out)
    }

    fun int(value: Int): ByteArray = unsigned(byteArrayOf((value shr 24).toByte(), (value shr 16).toByte(), (value shr 8).toByte(), value.toByte()))

    /** A non-negative big-endian number: leading zeros dropped, one zero added when the top bit is set. */
    fun unsigned(bytes: ByteArray): ByteArray {
        var start = 0
        while (start < bytes.size - 1 && bytes[start] == 0.toByte()) start++
        val trimmed = bytes.copyOfRange(start, bytes.size)
        val body = if (trimmed[0].toInt() and 0x80 != 0) concat(byteArrayOf(0), trimmed) else trimmed
        return tlv(0x02, body)
    }

    fun oid(dotted: String): ByteArray {
        val parts = dotted.split('.').map { it.toLong() }
        val out = ArrayList<Byte>()
        out += (parts[0] * 40 + parts[1]).toByte()
        for (part in parts.drop(2)) {
            val chunk = ArrayList<Byte>()
            var v = part
            chunk += (v and 0x7F).toByte()
            v = v shr 7
            while (v > 0) {
                chunk += ((v and 0x7F) or 0x80).toByte()
                v = v shr 7
            }
            out.addAll(chunk.reversed())
        }
        return tlv(0x06, out.toByteArray())
    }

    /** Context-specific constructed tag `[n]`. */
    fun explicit(n: Int, content: ByteArray): ByteArray = tlv(0xA0 or n, content)

    /** Context-specific primitive tag `[n]` (implicit). */
    fun implicit(n: Int, content: ByteArray): ByteArray = tlv(0x80 or n, content)

    /** X.509 time: UTCTime up to the year 2049, GeneralizedTime after. */
    fun time(epochSeconds: Long): ByteArray {
        val days = epochSeconds.floorDiv(86_400L)
        val secs = (epochSeconds - days * 86_400L).toInt()
        // Civil date from days since 1970-01-01 (Howard Hinnant's algorithm).
        val z = days + 719_468
        val era = z.floorDiv(146_097L)
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
        val year = (yoe + era * 400 + if (month <= 2) 1 else 0).toInt()
        fun two(n: Int) = n.toString().padStart(2, '0')
        val clock = two(secs / 3600) + two(secs % 3600 / 60) + two(secs % 60) + "Z"
        return if (year < 2050) {
            tlv(0x17, (two(year % 100) + two(month) + two(day) + clock).encodeToByteArray())
        } else {
            tlv(0x18, (year.toString() + two(month) + two(day) + clock).encodeToByteArray())
        }
    }
}
