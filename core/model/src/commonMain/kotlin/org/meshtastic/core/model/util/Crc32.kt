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
package org.meshtastic.core.model.util

/**
 * Standard CRC-32 (IEEE 802.3 / zlib polynomial 0xEDB88320, init 0xFFFFFFFF) — the same algorithm as
 * java.util.zip.CRC32, Python's zlib.crc32, and mvgrind's mv_crc32_core. Pure Kotlin so it works on every KMP target
 * without an expect/actual; byte-at-a-time (no lookup table) is plenty fast for a single 32-byte public key.
 *
 * Check value: crc32("123456789".encodeToByteArray()) == 0xCBF43926.
 */
fun crc32(bytes: ByteArray): Long {
    var crc = 0xFFFFFFFFL
    for (b in bytes) {
        crc = crc xor (b.toLong() and 0xFFL)
        repeat(8) {
            val mask = -(crc and 1L)
            crc = (crc ushr 1) xor (0xEDB88320L and mask)
        }
    }
    return crc.inv() and 0xFFFFFFFFL
}