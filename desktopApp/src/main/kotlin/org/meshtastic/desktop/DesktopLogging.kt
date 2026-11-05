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

package org.meshtastic.desktop

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.meshtastic.core.common.AppDataRoot
import org.meshtastic.core.common.log.AppLogControl
import org.meshtastic.core.common.log.AppLogControls
import org.meshtastic.core.common.log.AppLogLevel
import org.meshtastic.core.common.log.InMemoryLogBuffer
import org.meshtastic.core.common.log.MinSeverityLogWriter
import org.meshtastic.core.common.log.MtSwLogging
import org.meshtastic.core.common.log.ReportFile
import org.meshtastic.core.common.log.ReportTarget
import org.meshtastic.feature.map.maplibre.installMapLibreLogging
import java.io.File

/**
 * Sends Kermit output to the console, to [InMemoryLogBuffer] (which the Debug screen views and exports) and to the log
 * files of [MtSwLogging]. The console and the buffer keep Info and above in every build: the per-packet Debug and
 * Verbose radio lines flooded them and hid the warnings and errors that matter. The log files are off until the user
 * switches them on in the MT_SW features, and then go as deep as the chosen level; the cause of a crash is always
 * written. [isDebug] is kept so a build can raise the level again for a hunt.
 */
@Suppress("UNUSED_PARAMETER")
internal fun installDesktopLogging(isDebug: Boolean) {
    val logDir = File(AppDataRoot.dir, "logs")
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
        "${rt.availableProcessors()} CPUs, heap max ${rt.maxMemory() / MB} MB, " +
        "version ${org.meshtastic.core.model.MtswRelease.displayName(DesktopBuildConfig.VERSION_NAME)} " +
        "(${DesktopBuildConfig.VERSION_NAME})"
}

private const val MB = 1024 * 1024

/** The desktop side of the log switch: same files as on a phone, kept in the logs folder of the data folder. */
private class DesktopLogControl(private val dir: File) : AppLogControl {
    override val level: StateFlow<AppLogLevel>
        get() = MtSwLogging.level

    override fun setLevel(level: AppLogLevel) = MtSwLogging.setLevel(level)

    override val folderHint: String
        get() = dir.absolutePath

    override val canOpenFolder: Boolean = true

    override val recent: StateFlow<Boolean>
        get() = MtSwLogging.recent

    override fun setRecent(on: Boolean) = MtSwLogging.setRecent(on)

    override val canMakeReport: Boolean = true

    override suspend fun buildReport(
        note: String,
        withLogs: Boolean,
        extras: List<Pair<String, ByteArray>>,
    ): ReportFile? = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val folder = File(dir.parentFile, "reports").also { it.mkdirs() }
            val name =
                "mt_sw_zgloszenie_" +
                    java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(java.util.Date(now)) +
                    ".zip"
            val file = File(folder, name)
            val since = if (withLogs) now - MtSwLogging.RECENT_WINDOW_MS else Long.MAX_VALUE
            val engineLogs =
                File(dir.parentFile, "engine-logs")
                    .listFiles { f -> f.isFile && (f.extension == "log" || f.extension == "txt") }
                    .orEmpty()
            file.outputStream().use { out ->
                MtSwLogging.writeReportZip(
                    out,
                    since,
                    extras = listOf("opis_zgloszenia.txt" to note.toByteArray()) + extras,
                    extraFiles = if (withLogs) engineLogs.map { "silnik/${it.name}" to it } else emptyList(),
                )
            }
            // A copy where the person looks first (Downloads, else Desktop, else the home folder), not six folders
            // deep.
            val home = File(System.getProperty("user.home").orEmpty())
            val shown =
                listOf(
                    File(home, "Downloads"),
                    File(home, "Pobrane"),
                    File(home, "Desktop"),
                    File(home, "Pulpit"),
                    home,
                )
                    .firstOrNull { it.isDirectory && it.canWrite() }
                    ?.let { runCatching { file.copyTo(File(it, name), overwrite = true) }.getOrNull() } ?: file
            ReportFile(shown.absolutePath, shown.absolutePath, shown.length() / 1024)
        }
            .onFailure { Logger.w(it) { "Report could not be built" } }
            .getOrNull()
    }

    /** On a computer the folder with the file opens, so the person can attach it in the group that opens next. */
    override fun shareReport(report: ReportFile, target: ReportTarget, text: String): Boolean = runCatching {
        val file = File(report.path)
        val os = System.getProperty("os.name").orEmpty().lowercase()
        // The file manager opens with the file already selected, so it can be dragged into the group that opens next.
        val selected = runCatching {
            when {
                "mac" in os -> ProcessBuilder("open", "-R", file.absolutePath).start()
                "win" in os -> ProcessBuilder("explorer.exe", "/select,", file.absolutePath).start()
                else -> null
            }
        }
            .getOrNull()
        if (selected == null) java.awt.Desktop.getDesktop().open(file.parentFile)
        true
    }
        .getOrDefault(false)

    override fun openFolder(): Boolean = runCatching {
        dir.mkdirs()
        java.awt.Desktop.getDesktop().open(dir)
        true
    }
        .getOrDefault(false)
}
