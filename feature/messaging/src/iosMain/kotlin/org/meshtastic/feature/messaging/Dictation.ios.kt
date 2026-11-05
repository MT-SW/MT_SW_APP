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

package org.meshtastic.feature.messaging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVLinearPCMBitDepthKey
import platform.AVFAudio.AVLinearPCMIsBigEndianKey
import platform.AVFAudio.AVLinearPCMIsFloatKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFAudio.setActive
import platform.CoreAudioTypes.kAudioFormatLinearPCM
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfFile
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Takes the raw samples out of a WAV file (looks for the "data" block). */
private fun wavSamples(wav: ByteArray): ByteArray {
    var pos = 12
    while (pos + 8 <= wav.size) {
        val id = CharArray(4) { (wav[pos + it].toInt() and 0xFF).toChar() }.concatToString()
        var size = 0
        for (i in 3 downTo 0) size = (size shl 8) or (wav[pos + 4 + i].toInt() and 0xFF)
        if (id == "data") {
            val end = if (size <= 0 || pos + 8 + size > wav.size) wav.size else pos + 8 + size
            return wav.copyOfRange(pos + 8, end)
        }
        pos += 8 + size + (size and 1)
    }
    return ByteArray(0)
}

@OptIn(ExperimentalForeignApi::class)
private class IosMic {
    private var recorder: AVAudioRecorder? = null
    private val path = NSTemporaryDirectory() + "mt_sw_dictation.wav"

    suspend fun permission(): Boolean = suspendCoroutine { cont ->
        AVAudioSession.sharedInstance().requestRecordPermission { granted -> cont.resume(granted) }
    }

    fun start() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayAndRecord, error = null)
        session.setActive(true, error = null)
        val settings: Map<Any?, Any?> =
            mapOf(
                AVFormatIDKey to kAudioFormatLinearPCM.toInt(),
                AVSampleRateKey to 16000.0,
                AVNumberOfChannelsKey to 1,
                AVLinearPCMBitDepthKey to 16,
                AVLinearPCMIsFloatKey to false,
                AVLinearPCMIsBigEndianKey to false,
            )
        val rec = AVAudioRecorder(uRL = NSURL.fileURLWithPath(path), settings = settings, error = null)
        rec.record()
        recorder = rec
    }

    fun stop(): ByteArray {
        val rec = recorder ?: return ByteArray(0)
        rec.stop()
        recorder = null
        AVAudioSession.sharedInstance().setActive(false, error = null)
        val data: NSData? = NSData.dataWithContentsOfFile(path)
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
        return if (data == null) ByteArray(0) else wavSamples(data.toByteArray())
    }
}

/** iOS: records from the microphone (PCM 16 bit, 16 kHz, mono) after the user allows it. */
@Composable
actual fun rememberMicRecorder(): MicRecorder = remember {
    val mic = IosMic()
    MicRecorder(permission = { mic.permission() }, start = { mic.start() }, stop = { mic.stop() })
}
