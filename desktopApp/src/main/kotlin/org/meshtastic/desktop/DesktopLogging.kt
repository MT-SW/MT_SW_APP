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

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.meshtastic.core.common.log.AppLogControl
import org.meshtastic.core.common.log.AppLogControls
import org.meshtastic.core.common.log.AppLogLevel
import org.meshtastic.core.common.log.InMemoryLogBuffer
import org.meshtastic.core.common.log.MinSeverityLogWriter
import org.meshtastic.core.common.log.MtSwLogging
import org.meshtastic.feature.map.maplibre.installMapLibreLogging
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sends Kermit output to the console, to [InMemoryLogBuffer] (which the Debug screen views and exports) and to the log
 * files of [MtSwLogging]. The console and the buffer keep Info and above in every build: the per-packet Debug and
 * Verbose radio lines flooded them and hid the warnings and errors that matter. The log files are off until the user
 * switches them on in the MT_SW features, and then go as deep as the chosen level; the cause of a crash is always
 * written. [isDebug] is kept so a build can raise the level again for a hunt.
 */
@Suppress("UNUSED_PARAMETER")
internal fun installDesktopLogging(isDebug: Boolean) {
    val logDir = File(System.getProperty("user.home"), ".meshtastic/logs")
    Logger.setMinSeverity(Severity.Info)
    Logger.setLogWriters(
        listOf(
            MinSeverityLogWriter(platformLogWriter(), Severity.Info),
            MinSeverityLogWriter(InMemoryLogBuffer, Severity.Info),
            MtSwLogging.writer,
        ),
    )
    MtSwLogging.init(logDir, headerText = ::desktopHeader) { level ->
        Logger.setMinSeverity(MtSwLogging.minSeverityFor(Severity.Info, level))
    }
    AppLogControls.current = DesktopLogControl(logDir)
    installMapLibreLogging()
    Logger.i { "Log folder: ${logDir.absolutePath}; level ${MtSwLogging.level.value.key}; ${desktopHeader()}" }
}

private fun desktopHeader(): String {
    val rt = Runtime.getRuntime()
    return "Java ${System.getProperty("java.version")} (${System.getProperty("java.vendor")}), " +
        "${System.getProperty("os.name")} ${System.getProperty("os.version")} ${System.getProperty("os.arch")}, " +
        "${rt.availableProcessors()} CPUs, heap max ${rt.maxMemory() / MB} MB, version ${DesktopBuildConfig.VERSION_NAME}"
}

private const val MB = 1024 * 1024

/** The desktop side of the log switch: same files as on a phone, kept in `~/.meshtastic/logs`. */
private class DesktopLogControl(private val dir: File) : AppLogControl {
    override val level: StateFlow<AppLogLevel> get() = MtSwLogging.level

    override fun setLevel(level: AppLogLevel) = MtSwLogging.setLevel(level)

    override suspend fun exportForSending(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val folder = File(dir, "export").also { it.mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val target = File(folder, "mt_sw_logi_$stamp.zip")
            target.outputStream().use { MtSwLogging.writeZip(it) }
            target.absolutePath
        }
            .onFailure { Logger.w(it) { "Log export failed" } }
            .getOrNull()
    }

    override val canOpenFolder: Boolean = true

    override fun openFolder(): Boolean = runCatching {
        dir.mkdirs()
        java.awt.Desktop.getDesktop().open(dir)
        true
    }.getOrDefault(false)
}
