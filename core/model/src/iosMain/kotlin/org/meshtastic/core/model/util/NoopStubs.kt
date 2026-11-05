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
@file:Suppress("MagicNumber", "TooManyFunctions")

package org.meshtastic.core.model.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault

actual fun getShortDateTime(time: Long): String {
    val formatter = NSDateFormatter()
    formatter.locale = NSLocale.currentLocale
    val within24h = NSDate().timeIntervalSince1970 * 1000.0 - time <= 86_400_000.0
    formatter.dateStyle = if (within24h) NSDateFormatterNoStyle else NSDateFormatterShortStyle
    formatter.timeStyle = NSDateFormatterShortStyle
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(time / 1000.0))
}

/** Random bytes from the system generator (used for channel keys). */
@OptIn(ExperimentalForeignApi::class)
actual fun platformRandomBytes(size: Int): ByteArray {
    val out = ByteArray(size)
    if (size > 0) {
        val status = out.usePinned { SecRandomCopyBytes(kSecRandomDefault, size.convert(), it.addressOf(0)) }
        check(status == 0) { "SecRandomCopyBytes failed: $status" }
    }
    return out
}

/** Public key for a private X25519 key, computed in plain Kotlin (algorithm from RFC 7748 / TweetNaCl). */
actual fun x25519PublicKey(privateKey: ByteArray): ByteArray {
    require(privateKey.size == 32) { "X25519 private key must be 32 bytes" }
    val base = ByteArray(32).also { it[0] = 9 }
    return scalarMult(privateKey, base)
}

private val C121665 =
    LongArray(16).also {
        it[0] = 0xDB41
        it[1] = 1
    }

private fun car25519(o: LongArray) {
    for (i in 0 until 16) {
        o[i] += 1L shl 16
        val c = o[i] shr 16
        if (i < 15) o[i + 1] += c - 1 else o[0] += 38 * (c - 1)
        o[i] -= c shl 16
    }
}

private fun sel25519(p: LongArray, q: LongArray, b: Int) {
    val c = (b - 1).toLong().inv()
    for (i in 0 until 16) {
        val t = c and (p[i] xor q[i])
        p[i] = p[i] xor t
        q[i] = q[i] xor t
    }
}

private fun pack25519(n: LongArray): ByteArray {
    val t = n.copyOf()
    val m = LongArray(16)
    repeat(3) { car25519(t) }
    repeat(2) {
        m[0] = t[0] - 0xffed
        for (i in 1 until 15) {
            m[i] = t[i] - 0xffff - ((m[i - 1] shr 16) and 1)
            m[i - 1] = m[i - 1] and 0xffff
        }
        m[15] = t[15] - 0x7fff - ((m[14] shr 16) and 1)
        val b = ((m[15] shr 16) and 1).toInt()
        m[14] = m[14] and 0xffff
        sel25519(t, m, 1 - b)
    }
    val o = ByteArray(32)
    for (i in 0 until 16) {
        o[2 * i] = (t[i] and 0xff).toByte()
        o[2 * i + 1] = (t[i] shr 8).toByte()
    }
    return o
}

private fun unpack25519(n: ByteArray): LongArray {
    val o = LongArray(16)
    for (i in 0 until 16) o[i] = (n[2 * i].toLong() and 0xff) + ((n[2 * i + 1].toLong() and 0xff) shl 8)
    o[15] = o[15] and 0x7fff
    return o
}

private fun fieldAdd(o: LongArray, a: LongArray, b: LongArray) {
    for (i in 0 until 16) o[i] = a[i] + b[i]
}

private fun fieldSub(o: LongArray, a: LongArray, b: LongArray) {
    for (i in 0 until 16) o[i] = a[i] - b[i]
}

private fun fieldMul(o: LongArray, a: LongArray, b: LongArray) {
    val t = LongArray(31)
    for (i in 0 until 16) for (j in 0 until 16) t[i + j] += a[i] * b[j]
    for (i in 0 until 15) t[i] += 38 * t[i + 16]
    for (i in 0 until 16) o[i] = t[i]
    car25519(o)
    car25519(o)
}

private fun fieldInv(i: LongArray): LongArray {
    val c = i.copyOf()
    for (a in 253 downTo 0) {
        fieldMul(c, c, c)
        if (a != 2 && a != 4) fieldMul(c, c, i)
    }
    return c
}

@Suppress("LongMethod")
private fun scalarMult(n: ByteArray, p: ByteArray): ByteArray {
    val z = n.copyOf()
    z[31] = ((n[31].toInt() and 127) or 64).toByte()
    z[0] = (n[0].toInt() and 248).toByte()
    val x = unpack25519(p)
    val a = LongArray(16)
    val b = x.copyOf()
    val c = LongArray(16)
    val d = LongArray(16)
    val e = LongArray(16)
    val f = LongArray(16)
    a[0] = 1
    d[0] = 1
    for (i in 254 downTo 0) {
        val r = (z[i ushr 3].toInt() ushr (i and 7)) and 1
        sel25519(a, b, r)
        sel25519(c, d, r)
        fieldAdd(e, a, c)
        fieldSub(a, a, c)
        fieldAdd(c, b, d)
        fieldSub(b, b, d)
        fieldMul(d, e, e)
        fieldMul(f, a, a)
        fieldMul(a, c, a)
        fieldMul(c, b, e)
        fieldAdd(e, a, c)
        fieldSub(a, a, c)
        fieldMul(b, a, a)
        fieldSub(c, d, f)
        fieldMul(a, c, C121665)
        fieldAdd(a, a, d)
        fieldMul(c, c, a)
        fieldMul(a, d, f)
        fieldMul(d, b, x)
        fieldMul(b, e, e)
        sel25519(a, b, r)
        sel25519(c, d, r)
    }
    val inv = fieldInv(c)
    fieldMul(a, a, inv)
    return pack25519(a)
}
