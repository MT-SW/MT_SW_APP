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
@file:Suppress("MagicNumber", "LongMethod")

package org.meshtastic.core.takserver

/**
 * SHA-1, HMAC-SHA-1 and the PKCS#12 key derivation (RFC 7292 appendix B). Pure Kotlin; only used to write containers.
 */
internal object TakHash {
    private const val BLOCK = 64

    fun sha1(data: ByteArray): ByteArray {
        var h0 = 0x67452301
        var h1 = 0xEFCDAB89L.toInt()
        var h2 = 0x98BADCFEL.toInt()
        var h3 = 0x10325476
        var h4 = 0xC3D2E1F0L.toInt()
        val total = ((data.size + 9 + BLOCK - 1) / BLOCK) * BLOCK
        val msg = ByteArray(total)
        data.copyInto(msg)
        msg[data.size] = 0x80.toByte()
        val bits = data.size.toLong() * 8
        for (i in 0 until 8) msg[total - 1 - i] = (bits shr (8 * i)).toByte()
        val w = IntArray(80)
        for (chunk in 0 until total step BLOCK) {
            for (i in 0 until 16) {
                val p = chunk + i * 4
                w[i] =
                    ((msg[p].toInt() and 0xFF) shl 24) or
                    ((msg[p + 1].toInt() and 0xFF) shl 16) or
                    ((msg[p + 2].toInt() and 0xFF) shl 8) or
                    (msg[p + 3].toInt() and 0xFF)
            }
            for (i in 16 until 80) w[i] = (w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]).rotateLeft(1)
            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4
            for (i in 0 until 80) {
                val f: Int
                val k: Int
                when {
                    i < 20 -> {
                        f = (b and c) or (b.inv() and d)
                        k = 0x5A827999
                    }

                    i < 40 -> {
                        f = b xor c xor d
                        k = 0x6ED9EBA1
                    }

                    i < 60 -> {
                        f = (b and c) or (b and d) or (c and d)
                        k = 0x8F1BBCDCL.toInt()
                    }

                    else -> {
                        f = b xor c xor d
                        k = 0xCA62C1D6L.toInt()
                    }
                }
                val temp = a.rotateLeft(5) + f + e + k + w[i]
                e = d
                d = c
                c = b.rotateLeft(30)
                b = a
                a = temp
            }
            h0 += a
            h1 += b
            h2 += c
            h3 += d
            h4 += e
        }
        val out = ByteArray(20)
        intArrayOf(h0, h1, h2, h3, h4).forEachIndexed { i, h ->
            out[i * 4] = (h shr 24).toByte()
            out[i * 4 + 1] = (h shr 16).toByte()
            out[i * 4 + 2] = (h shr 8).toByte()
            out[i * 4 + 3] = h.toByte()
        }
        return out
    }

    fun hmacSha1(key: ByteArray, data: ByteArray): ByteArray {
        val k = (if (key.size > BLOCK) sha1(key) else key).copyOf(BLOCK)
        val inner = ByteArray(BLOCK) { (k[it].toInt() xor 0x36).toByte() }
        val outer = ByteArray(BLOCK) { (k[it].toInt() xor 0x5C).toByte() }
        return sha1(outer + sha1(inner + data))
    }

    /** PKCS#12 password: BMPString with a trailing null character. */
    fun bmpPassword(password: String): ByteArray {
        val out = ByteArray(password.length * 2 + 2)
        password.forEachIndexed { i, c ->
            out[i * 2] = (c.code shr 8).toByte()
            out[i * 2 + 1] = c.code.toByte()
        }
        return out
    }

    /** RFC 7292 B.2 with SHA-1: [id] 1 = cipher key, 2 = IV, 3 = MAC key. */
    fun pkcs12Kdf(password: ByteArray, salt: ByteArray, iterations: Int, id: Int, length: Int): ByteArray {
        val u = 20
        val d = ByteArray(BLOCK) { id.toByte() }

        fun expand(src: ByteArray): ByteArray =
            if (src.isEmpty()) src else ByteArray(BLOCK * ((src.size + BLOCK - 1) / BLOCK)) { src[it % src.size] }

        val i = expand(salt) + expand(password)
        val rounds = (length + u - 1) / u
        val out = ByteArray(rounds * u)
        for (round in 0 until rounds) {
            var a = sha1(d + i)
            repeat(iterations - 1) { a = sha1(a) }
            a.copyInto(out, round * u)
            val b = ByteArray(BLOCK) { a[it % u] }
            for (j in 0 until i.size / BLOCK) {
                var carry = 1
                for (x in BLOCK - 1 downTo 0) {
                    val idx = j * BLOCK + x
                    val sum = (i[idx].toInt() and 0xFF) + (b[x].toInt() and 0xFF) + carry
                    i[idx] = sum.toByte()
                    carry = sum shr 8
                }
            }
        }
        return out.copyOf(length)
    }
}
