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
package org.meshtastic.desktop

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import org.meshtastic.core.common.log.InMemoryLogBuffer
import org.meshtastic.feature.map.maplibre.installMapLibreLogging
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Sends Kermit output to the console and to [InMemoryLogBuffer], which the Debug screen views and exports. Every build
 * keeps Info and above, as Android release does: the per-packet Debug and Verbose radio lines flooded the console and
 * hid the warnings and errors that matter. [isDebug] is kept so a build can raise the level again for a hunt.
 */
@Suppress("UNUSED_PARAMETER")
internal fun installDesktopLogging(isDebug: Boolean) {
    Logger.setMinSeverity(Severity.Info)
    val logDir = File(System.getProperty("user.home"), ".meshtastic/logs")
    Logger.setLogWriters(listOf(platformLogWriter(), InMemoryLogBuffer, FileLogWriter(logDir)))
    installMapLibreLogging()
    // A crash of the app is written to the log file too, so it can be read after the window is gone.
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
        runCatching { Logger.e(error) { "Uncaught exception in thread ${thread.name}" } }
        previous?.uncaughtException(thread, error)
    }
    Logger.i {
        "Log file: ${logDir.absolutePath}; Java ${System.getProperty("java.version")}, " +
            "${System.getProperty("os.name")} ${System.getProperty("os.version")} ${System.getProperty("os.arch")}"
    }
}

/**
 * Writes every log line (Info and above) and every crash to a file on the disk, so it can be read after the app has
 * closed or crashed: `~/.meshtastic/logs/mt_sw_desktop.log`. At 5 MB the file is moved to `.1.log` (and the older ones
 * one step further, three are kept), so the folder never grows beyond about 20 MB. Writing never throws.
 */
internal class FileLogWriter(private val dir: File) : LogWriter() {
    private val file = File(dir, NAME)
    private val lock = Any()
    private val time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    init {
        runCatching { dir.mkdirs() }
        runCatching { rotate() }
    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val line =
            buildString {
                append(LocalDateTime.now().format(time)).append(' ').append(severity.name.first()).append('/').append(tag)
                append(": ").append(message).append('\n')
                throwable?.let { append(StringWriter().also { w -> it.printStackTrace(PrintWriter(w)) }).append('\n') }
            }
        synchronized(lock) {
            runCatching {
                if (file.length() > MAX_BYTES) rotate()
                file.appendText(line)
            }
        }
    }

    private fun rotate() {
        if (!file.exists() || file.length() <= MAX_BYTES) return
        File(dir, "mt_sw_desktop.${KEEP - 1}.log").delete()
        for (i in KEEP - 2 downTo 1) File(dir, "mt_sw_desktop.$i.log").renameTo(File(dir, "mt_sw_desktop.${i + 1}.log"))
        file.renameTo(File(dir, "mt_sw_desktop.1.log"))
    }

    private companion object {
        const val NAME = "mt_sw_desktop.log"
        const val MAX_BYTES = 5L * 1024 * 1024
        const val KEEP = 4
    }
}
