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
package org.meshtastic.core.ui.util

import org.meshtastic.core.common.util.systemTimeZone
import org.meshtastic.core.common.util.toInstant

/** What a picture says about itself: when it was taken (epoch ms) and where (degrees). Any part may be null. */
data class ExifInfo(val takenMillis: Long?, val latitude: Double?, val longitude: Double?)

/**
 * MT_SW: reads the time and the GPS position out of a JPEG's EXIF block. Written here in plain Kotlin so it works the
 * same on every platform and needs no library. A picture without EXIF, or a damaged one, gives an empty result.
 */
fun readExif(bytes: ByteArray): ExifInfo =
    try {
        ExifParser(bytes).parse()
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        ExifInfo(null, null, null)
    }

private const val TAG_EXIF_IFD = 0x8769
private const val TAG_GPS_IFD = 0x8825
private const val TAG_DATE_TIME = 0x0132
private const val TAG_DATE_TIME_ORIGINAL = 0x9003
private const val TYPE_RATIONAL = 5
private const val SEC_PER_DAY = 86_400L
private const val MILLIS = 1000L

private class ExifParser(private val b: ByteArray) {
    private var tiff = 0
    private var little = false

    private fun u8(o: Int) = b[o].toInt() and 0xFF

    private fun u16(o: Int) = if (little) u8(o) or (u8(o + 1) shl 8) else (u8(o) shl 8) or u8(o + 1)

    private fun u32(o: Int): Long =
        if (little) {
            u16(o).toLong() or (u16(o + 2).toLong() shl 16)
        } else {
            (u16(o).toLong() shl 16) or u16(o + 2).toLong()
        }

    private class Entry(val tag: Int, val type: Int, val count: Int, val valueOffset: Int)

    private fun entries(ifd: Int): List<Entry> {
        val at = tiff + ifd
        val n = u16(at)
        return (0 until n).map { i ->
            val e = at + 2 + i * ENTRY_SIZE
            Entry(u16(e), u16(e + 2), u32(e + 4).toInt(), e + 8)
        }
    }

    /** Where the value of [e] is kept: inline when it fits in four bytes, else behind the offset. */
    private fun dataAt(e: Entry, unit: Int): Int =
        if (e.count * unit <= 4) e.valueOffset else tiff + u32(e.valueOffset).toInt()

    private fun ascii(e: Entry): String = buildString {
        val at = dataAt(e, 1)
        for (i in 0 until e.count) {
            val c = u8(at + i)
            if (c == 0) break
            append(c.toChar())
        }
    }

    private fun rational(at: Int): Double {
        val den = u32(at + 4)
        return if (den == 0L) 0.0 else u32(at).toDouble() / den
    }

    private fun degrees(e: Entry): Double? {
        if (e.type != TYPE_RATIONAL || e.count < 3) return null
        val at = dataAt(e, RATIONAL_SIZE)
        return rational(at) + rational(at + RATIONAL_SIZE) / 60.0 + rational(at + 2 * RATIONAL_SIZE) / 3600.0
    }

    fun parse(): ExifInfo {
        var i = 2
        if (b.size < 4 || u8(0) != 0xFF || u8(1) != 0xD8) return ExifInfo(null, null, null)
        while (i + 4 < b.size) {
            if (u8(i) != 0xFF) break
            val marker = u8(i + 1)
            val len = (u8(i + 2) shl 8) or u8(i + 3)
            if (marker == 0xE1 && ascii6(i + 4) == "Exif") {
                tiff = i + 10
                little = u8(tiff) == 'I'.code
                return readTiff()
            }
            if (marker == 0xDA) break
            i += 2 + len
        }
        return ExifInfo(null, null, null)
    }

    private fun ascii6(at: Int) = String(CharArray(4) { b[at + it].toInt().toChar() })

    private fun readTiff(): ExifInfo {
        val ifd0 = entries(u32(tiff + 4).toInt())
        val exif = ifd0.firstOrNull { it.tag == TAG_EXIF_IFD }?.let { entries(u32(it.valueOffset).toInt()) }.orEmpty()
        val gps = ifd0.firstOrNull { it.tag == TAG_GPS_IFD }?.let { entries(u32(it.valueOffset).toInt()) }.orEmpty()
        val text =
            exif.firstOrNull { it.tag == TAG_DATE_TIME_ORIGINAL }?.let(::ascii)
                ?: ifd0.firstOrNull { it.tag == TAG_DATE_TIME }?.let(::ascii)
        var lat = gps.firstOrNull { it.tag == 2 }?.let(::degrees)
        var lon = gps.firstOrNull { it.tag == 4 }?.let(::degrees)
        if (gps.firstOrNull { it.tag == 1 }?.let(::ascii) == "S") lat = lat?.let { -it }
        if (gps.firstOrNull { it.tag == 3 }?.let(::ascii) == "W") lon = lon?.let { -it }
        if (lat == 0.0 && lon == 0.0) {
            lat = null
            lon = null
        }
        return ExifInfo(text?.let(::localTextToMillis), lat, lon)
    }

    /** "2026:11:26 09:12:30" is the phone's local time; it becomes epoch milliseconds in the zone this device uses. */
    private fun localTextToMillis(text: String): Long? {
        val p = text.trim().split(' ', ':')
        if (p.size < DATE_PARTS) return null
        val n = p.take(DATE_PARTS).map { it.toIntOrNull() ?: return null }
        val days = daysFromCivil(n[0], n[1], n[2])
        val localSeconds = days * SEC_PER_DAY + n[3] * SECONDS_IN_HOUR + n[4] * SECONDS_IN_MINUTE + n[5]
        val offset = systemTimeZone.offsetAt((localSeconds * MILLIS).toInstant()).totalSeconds
        return (localSeconds - offset) * MILLIS
    }

    /** Days since 1970-01-01 for a calendar date (proleptic Gregorian). */
    private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
        val y = (if (month <= 2) year - 1 else year).toLong()
        val era = (if (y >= 0) y else y - 399) / ERA_YEARS
        val yoe = y - era * ERA_YEARS
        val mp = (month + 9) % 12
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * DAYS_PER_ERA + doe - EPOCH_SHIFT
    }

    private companion object {
        const val ENTRY_SIZE = 12
        const val RATIONAL_SIZE = 8
        const val DATE_PARTS = 6
        const val SECONDS_IN_HOUR = 3600L
        const val SECONDS_IN_MINUTE = 60L
        const val ERA_YEARS = 400L
        const val DAYS_PER_ERA = 146_097L
        const val EPOCH_SHIFT = 719_468L
    }
}
