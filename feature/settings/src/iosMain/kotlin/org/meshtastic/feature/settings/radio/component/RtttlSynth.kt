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

@file:Suppress("MagicNumber")

package org.meshtastic.feature.settings.radio.component

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Turns an RTTTL ringtone (`name:d=4,o=5,b=63:8c,8d,...`) into a mono 16-bit WAV, because iOS cannot play RTTTL
 * itself. Pure Kotlin, so it can be checked without a device.
 */
internal object RtttlSynth {
    private const val SAMPLE_RATE = 22_050
    private const val MAX_SECONDS = 30
    private const val AMPLITUDE = 0.3
    private const val GAP_SHARE = 0.08
    private const val WAV_HEADER_SIZE = 44
    private const val WHOLE_NOTE_MILLIS_AT_ONE_BPM = 240_000
    private const val DEFAULT_DURATION = 4
    private const val DEFAULT_OCTAVE = 6
    private const val DEFAULT_BPM = 63
    private const val MIDI_A4 = 69
    private const val A4_HZ = 440.0
    private const val SEMITONES_PER_OCTAVE = 12
    private val NOTE_REGEX = Regex("^(\\d+)?([a-gp])(#)?(\\.)?(\\d)?(\\.)?$", RegexOption.IGNORE_CASE)
    private val SEMITONES = mapOf('c' to 0, 'd' to 2, 'e' to 4, 'f' to 5, 'g' to 7, 'a' to 9, 'b' to 11)

    private class Defaults(
        var duration: Int = DEFAULT_DURATION,
        var octave: Int = DEFAULT_OCTAVE,
        var bpm: Int = DEFAULT_BPM,
    )

    private class Tone(val frequencyHz: Double, val millis: Int)

    /** The WAV file for [rtttl], or null when the text is not a usable RTTTL tune. */
    fun toWav(rtttl: String): ByteArray? {
        val tones = parse(rtttl)
        val pcm = if (tones.isNullOrEmpty()) null else render(tones)
        return pcm?.let { wrap(it) }
    }

    @Suppress("ReturnCount")
    private fun parse(text: String): List<Tone>? {
        val parts = text.trim().split(':')
        if (parts.size < 3) return null
        val defaults = Defaults()
        parts[1].split(',').forEach { applySetting(defaults, it) }
        if (defaults.duration <= 0 || defaults.bpm <= 0) return null
        return parts[2].split(',').mapNotNull { parseNote(it.trim(), defaults) }
    }

    private fun applySetting(defaults: Defaults, setting: String) {
        val key = setting.substringBefore('=', "").trim().lowercase()
        val value = setting.substringAfter('=', "").trim().toIntOrNull() ?: return
        when (key) {
            "d" -> defaults.duration = value
            "o" -> defaults.octave = value
            "b" -> defaults.bpm = value
        }
    }

    @Suppress("ReturnCount")
    private fun parseNote(token: String, defaults: Defaults): Tone? {
        val match = NOTE_REGEX.matchEntire(token) ?: return null
        val groups = match.groupValues
        val duration = groups[1].toIntOrNull() ?: defaults.duration
        if (duration <= 0) return null
        val name = groups[2].lowercase()[0]
        val dotted = groups[4].isNotEmpty() || groups[6].isNotEmpty()
        val octave = groups[5].toIntOrNull() ?: defaults.octave
        val base = WHOLE_NOTE_MILLIS_AT_ONE_BPM / defaults.bpm / duration
        val millis = if (dotted) base * 3 / 2 else base
        val frequency =
            if (name == 'p') {
                0.0
            } else {
                val sharp = if (groups[3].isNotEmpty()) 1 else 0
                val midi = SEMITONES_PER_OCTAVE * (octave + 1) + (SEMITONES[name] ?: 0) + sharp
                A4_HZ * 2.0.pow((midi - MIDI_A4).toDouble() / SEMITONES_PER_OCTAVE)
            }
        return Tone(frequency, millis)
    }

    private fun render(tones: List<Tone>): ShortArray {
        val maxSamples = SAMPLE_RATE * MAX_SECONDS
        val out = ArrayList<Short>()
        for (tone in tones) {
            val count = tone.millis.toLong() * SAMPLE_RATE / 1000
            val soundLength = (count * (1 - GAP_SHARE)).toInt()
            for (i in 0 until count.toInt()) {
                val sample =
                    if (tone.frequencyHz > 0 && i < soundLength) {
                        sin(2 * PI * tone.frequencyHz * i / SAMPLE_RATE) * AMPLITUDE * Short.MAX_VALUE
                    } else {
                        0.0
                    }
                out.add(sample.roundToInt().toShort())
            }
            if (out.size >= maxSamples) break
        }
        return ShortArray(minOf(out.size, maxSamples)) { out[it] }
    }

    private fun wrap(pcm: ShortArray): ByteArray {
        val dataSize = pcm.size * 2
        val bytes = ByteArray(WAV_HEADER_SIZE + dataSize)
        fun putText(offset: Int, text: String) = text.forEachIndexed { i, c -> bytes[offset + i] = c.code.toByte() }
        fun putInt(offset: Int, value: Int) = repeat(4) { bytes[offset + it] = (value shr (8 * it)).toByte() }
        fun putShort(offset: Int, value: Int) = repeat(2) { bytes[offset + it] = (value shr (8 * it)).toByte() }
        putText(0, "RIFF")
        putInt(4, 36 + dataSize)
        putText(8, "WAVEfmt ")
        putInt(16, 16)
        putShort(20, 1)
        putShort(22, 1)
        putInt(24, SAMPLE_RATE)
        putInt(28, SAMPLE_RATE * 2)
        putShort(32, 2)
        putShort(34, 16)
        putText(36, "data")
        putInt(40, dataSize)
        pcm.forEachIndexed { i, s -> putShort(WAV_HEADER_SIZE + i * 2, s.toInt()) }
        return bytes
    }
}
