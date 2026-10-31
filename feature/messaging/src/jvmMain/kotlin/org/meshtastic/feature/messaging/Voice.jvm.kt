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
package org.meshtastic.feature.messaging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger

/**
 * Reads with the speech of the operating system: `say` on macOS, the .NET speech synthesizer through PowerShell on
 * Windows, `espeak-ng` on Linux. The text goes in through the standard input, so no character needs escaping.
 */
private class DesktopSpeaker : Speaker {
    @Volatile private var process: Process? = null

    override fun speak(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        stop()
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val command =
            when {
                "mac" in os -> listOf("say")
                "win" in os ->
                    listOf(
                        "powershell",
                        "-NoProfile",
                        "-Command",
                        "[Console]::InputEncoding = [System.Text.Encoding]::UTF8; Add-Type -AssemblyName System.Speech; " +
                            "(New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak([Console]::In.ReadToEnd())",
                    )
                else -> listOf("espeak-ng", "--stdin")
            }
        Thread {
                try {
                    val started = ProcessBuilder(command).redirectErrorStream(true).start()
                    process = started
                    started.outputStream.use { it.write(clean.toByteArray(Charsets.UTF_8)) }
                    started.inputStream.readBytes()
                    started.waitFor()
                } catch (e: Exception) {
                    Logger.w(tag = "DesktopSpeaker", throwable = e) { "Reading aloud failed: ${e.message}" }
                }
            }
            .apply {
                isDaemon = true
                start()
            }
    }

    override fun stop() {
        process?.let { runCatching { it.destroy() } }
        process = null
    }
}

@Composable
actual fun rememberSpeaker(): Speaker {
    val speaker = remember { DesktopSpeaker() }
    DisposableEffect(speaker) { onDispose { speaker.stop() } }
    return speaker
}

/** Records the default microphone with javax.sound as 16 kHz mono PCM; the recording is capped at 30 seconds. */
private class DesktopMic(private val useHeadset: () -> Boolean) {
    private var line: javax.sound.sampled.TargetDataLine? = null
    private var thread: Thread? = null
    private val buffer = java.io.ByteArrayOutputStream()

    @Volatile private var running = false

    fun start() {
        val format = javax.sound.sampled.AudioFormat(RATE, 16, 1, true, false)
        val opened = openLine(format)
        opened.open(format)
        buffer.reset()
        line = opened
        running = true
        opened.start()
        thread =
            Thread {
                    val chunk = ByteArray(4096)
                    while (running && buffer.size() < RATE * 2 * 30) {
                        val read = opened.read(chunk, 0, chunk.size)
                        if (read > 0) synchronized(buffer) { buffer.write(chunk, 0, read) }
                    }
                }
                .apply {
                    isDaemon = true
                    start()
                }
    }

    fun stop(): ByteArray {
        running = false
        runCatching { line?.stop() }
        runCatching { thread?.join(1000) }
        runCatching { line?.close() }
        line = null
        thread = null
        return synchronized(buffer) { buffer.toByteArray() }
    }

    /**
     * The microphone line: with the headset setting on, a connected headset (Bluetooth, USB) is looked for among the
     * computer's sound inputs; otherwise, or when none is found, the system's default microphone.
     */
    private fun openLine(format: javax.sound.sampled.AudioFormat): javax.sound.sampled.TargetDataLine {
        if (useHeadset()) {
            val info = javax.sound.sampled.DataLine.Info(javax.sound.sampled.TargetDataLine::class.java, format)
            val inputs =
                javax.sound.sampled.AudioSystem.getMixerInfo().filter { mixer ->
                    runCatching { javax.sound.sampled.AudioSystem.getMixer(mixer).isLineSupported(info) }.getOrDefault(false)
                }
            val text = { m: javax.sound.sampled.Mixer.Info -> (m.name + " " + m.description).lowercase() }
            val pick =
                inputs.firstOrNull { HEADSET.containsMatchIn(text(it)) }
                    ?: inputs.firstOrNull { !BUILT_IN.containsMatchIn(text(it)) }
            if (pick != null) {
                runCatching {
                    Logger.i(tag = "DesktopMic") { "Dictation records from ${pick.name}" }
                    return javax.sound.sampled.AudioSystem.getMixer(pick).getLine(info) as javax.sound.sampled.TargetDataLine
                }
            }
        }
        return try {
            javax.sound.sampled.AudioSystem.getTargetDataLine(format)
        } catch (e: IllegalArgumentException) {
            // For the log: which sound inputs the system offers (none = no microphone connected or all are in use).
            val names = javax.sound.sampled.AudioSystem.getMixerInfo().joinToString("; ") { it.name }.ifBlank { "none" }
            Logger.w(tag = "DesktopMic") { "No microphone line for 16 kHz mono. Sound devices seen by Java: $names" }
            throw e
        }
    }

    private companion object {
        const val RATE = 16_000f
        val HEADSET = Regex("headset|bluetooth|hands-?free|usb|airpods|buds|słuchawk|jabra|plantronics|poly|bose|sony|jbl")
        val BUILT_IN = Regex("built-?in|wbudowan|internal|macbook|default|primary|stereo mix|port |line in")
    }
}

@Composable
actual fun rememberMicRecorder(): MicRecorder {
    val prefs = org.koin.compose.koinInject<org.meshtastic.core.repository.UiPrefs>()
    val mic = remember { DesktopMic(useHeadset = { prefs.dictationExternalMic.value }) }
    return remember(mic) { MicRecorder(permission = { true }, start = mic::start, stop = mic::stop) }
}
