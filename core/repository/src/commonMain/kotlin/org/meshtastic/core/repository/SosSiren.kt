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

import kotlin.math.PI
import kotlin.math.sin

/**
 * The siren of the SOS alarm, made in memory so no sound file has to ship: four tones in turn (high, low, high, low),
 * each with a short fade so a loop does not click. The computer and the iPhone/iPad play it in a loop; the iPhone/iPad
 * also hands it to the system as the sound of the alarm notifications.
 */
object SosSiren {
    const val SAMPLE_RATE = 44_100
    private const val BYTES_PER_SAMPLE = 2
    private const val TONE_SECONDS = 0.35
    private const val FADE_SECONDS = 0.01
    private const val AMPLITUDE = 0.9
    private const val WAV_HEADER = 44
    private val TONES = listOf(880.0, 660.0)

    /** [seconds] of siren (rounded to whole pairs of tones) as 16-bit little-endian mono samples. */
    fun pcm(seconds: Double): ByteArray {
        val samplesPerTone = (SAMPLE_RATE * TONE_SECONDS).toInt()
        val fade = (SAMPLE_RATE * FADE_SECONDS).toInt()
        val pairs = (seconds / (TONE_SECONDS * TONES.size)).toInt().coerceAtLeast(1)
        val out = ByteArray(pairs * TONES.size * samplesPerTone * BYTES_PER_SAMPLE)
        var index = 0
        repeat(pairs) {
            for (frequency in TONES) {
                for (n in 0 until samplesPerTone) {
                    val envelope = minOf(1.0, n / fade.toDouble(), (samplesPerTone - n) / fade.toDouble())
                    val wave = sin(2 * PI * frequency * n / SAMPLE_RATE)
                    val value = (wave * envelope * AMPLITUDE * Short.MAX_VALUE).toInt()
                    out[index++] = (value and 0xFF).toByte()
                    out[index++] = ((value shr 8) and 0xFF).toByte()
                }
            }
        }
        return out
    }

    /** The same siren as a WAV file (header and samples), for players that read a file. */
    fun wav(seconds: Double): ByteArray {
        val data = pcm(seconds)
        val out = ByteArray(WAV_HEADER + data.size)
        fun put(offset: Int, text: String) = text.forEachIndexed { i, c -> out[offset + i] = c.code.toByte() }
        fun putBytes(offset: Int, value: Int, count: Int) =
            repeat(count) { out[offset + it] = ((value shr (8 * it)) and 0xFF).toByte() }
        fun putInt(offset: Int, value: Int) = putBytes(offset, value, 4)
        fun putShort(offset: Int, value: Int) = putBytes(offset, value, 2)
        put(0, "RIFF")
        putInt(4, 36 + data.size)
        put(8, "WAVEfmt ")
        putInt(16, 16)
        putShort(20, 1)
        putShort(22, 1)
        putInt(24, SAMPLE_RATE)
        putInt(28, SAMPLE_RATE * BYTES_PER_SAMPLE)
        putShort(32, BYTES_PER_SAMPLE)
        putShort(34, 16)
        put(36, "data")
        putInt(40, data.size)
        data.copyInto(out, WAV_HEADER)
        return out
    }
}
