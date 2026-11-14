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
package org.meshtastic.desktop.notification

import co.touchlab.kermit.Logger
import org.meshtastic.core.repository.SosAlarmSound
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import kotlin.math.PI
import kotlin.math.sin

/**
 * The alarm sound of a received SOS on a computer: a two-tone siren made in memory and played in a loop until
 * [stop], so it needs no sound file. The notification of the system cannot ring again and again, so the app does it.
 */
internal class DesktopSosAlarmSound : SosAlarmSound {
    private var clip: Clip? = null

    @Synchronized
    override fun start() {
        if (clip != null) return
        runCatching {
            val data = siren()
            val format = AudioFormat(SAMPLE_RATE, SAMPLE_BITS, 1, true, false)
            val stream = AudioInputStream(ByteArrayInputStream(data), format, (data.size / BYTES_PER_SAMPLE).toLong())
            val newClip = AudioSystem.getClip()
            newClip.open(stream)
            newClip.loop(Clip.LOOP_CONTINUOUSLY)
            clip = newClip
        }
            .onFailure { Logger.w(it) { "SOS alarm: the sound could not be started" } }
    }

    @Synchronized
    override fun stop() {
        clip?.let {
            it.stop()
            it.close()
        }
        clip = null
    }

    /** Four tones in turn (high, low, high, low), each with a short fade so the loop does not click. */
    private fun siren(): ByteArray {
        val samplesPerTone = (SAMPLE_RATE * TONE_SECONDS).toInt()
        val fade = (SAMPLE_RATE * FADE_SECONDS).toInt()
        val out = ByteArray(samplesPerTone * TONES.size * BYTES_PER_SAMPLE)
        var index = 0
        for (frequency in TONES) {
            for (n in 0 until samplesPerTone) {
                val envelope = minOf(1.0, n / fade.toDouble(), (samplesPerTone - n) / fade.toDouble())
                val value = (sin(2 * PI * frequency * n / SAMPLE_RATE) * envelope * AMPLITUDE * Short.MAX_VALUE).toInt()
                out[index++] = (value and 0xFF).toByte()
                out[index++] = ((value shr 8) and 0xFF).toByte()
            }
        }
        return out
    }

    private companion object {
        const val SAMPLE_RATE = 44_100f
        const val SAMPLE_BITS = 16
        const val BYTES_PER_SAMPLE = 2
        const val TONE_SECONDS = 0.35
        const val FADE_SECONDS = 0.01
        const val AMPLITUDE = 0.9
        val TONES = listOf(880.0, 660.0, 880.0, 660.0)
    }
}
