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

/** The computer has no common speech recognizer to call, so there is no dictation button on desktop. */
@Composable actual fun rememberDictation(onText: (String) -> Unit): Dictation = Dictation(available = false) {}
