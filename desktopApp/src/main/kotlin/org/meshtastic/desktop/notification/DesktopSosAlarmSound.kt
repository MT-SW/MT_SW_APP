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
import org.meshtastic.core.repository.SosSiren
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

/**
 * The alarm sound of a received SOS on a computer: the shared siren played in a loop until [stop]. The notification of
 * the system cannot ring again and again, so the app does it. A computer has no torch, so [start] ignores the flash.
 */
internal class DesktopSosAlarmSound : SosAlarmSound {
    private var clip: Clip? = null

    @Synchronized
    override fun start(flash: Boolean) {
        if (clip != null) return
        runCatching {
            val data = SosSiren.pcm(SIREN_SECONDS)
            val format = AudioFormat(SosSiren.SAMPLE_RATE.toFloat(), SAMPLE_BITS, 1, true, false)
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

    private companion object {
        const val SAMPLE_BITS = 16
        const val BYTES_PER_SAMPLE = 2
        const val SIREN_SECONDS = 1.4
    }
}
