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
@file:Suppress("MagicNumber", "InjectDispatcher")

package org.meshtastic.desktop

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.meshtastic.core.common.AppDataRoot
import org.meshtastic.core.common.log.AppLogControl
import org.meshtastic.core.common.log.AppLogControls
import org.meshtastic.core.common.log.AppLogLevel
import org.meshtastic.core.common.log.InMemoryLogBuffer
import org.meshtastic.core.common.log.LogInsights
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
            LogInsights.writer,
        ),
    )
    MtSwLogging.init(logDir, headerText = ::desktopHeader) { level ->
        Logger.setMinSeverity(MtSwLogging.minSeverityFor(Severity.Info, level))
    }
    MtSwLogging.screenProvider = { windowExtras(withPicture = true) }
    val exportScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val control = DesktopLogControl(logDir, exportScope)
    AppLogControls.current = control
    // Like the Downloads folder of a phone: two zips kept fresh in the logs folder, the log of the last 24 hours and,
    // apart from it, the full log (only while it is on).
    exportScope.launch {
        while (true) {
            control.autoExport()
            delay(AUTO_EXPORT_EVERY_MS)
        }
    }
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
private const val AUTO_EXPORT_EVERY_MS = 3 * 60 * 1000L
private const val APP_FOLDER = "MT_SW"
private const val NAME_24H = "mt_sw_logi_24h.zip"
private const val NAME_FULL = "mt_sw_logi_pelne.zip"

/** The desktop side of the log switch: same files as on a phone, kept in the logs folder of the data folder. */
private class DesktopLogControl(private val dir: File, private val scope: CoroutineScope) : AppLogControl {
    override val level: StateFlow<AppLogLevel>
        get() = MtSwLogging.level

    override fun setLevel(level: AppLogLevel) = MtSwLogging.setLevel(level)

    override val folderHint: String
        get() = dir.absolutePath

    override val canOpenFolder: Boolean = true

    // The folder the person chose for the logs: its path in a small file next to the logs.
    private val folderFile = File(dir, "log_folder.txt")
    private val appFolderFile = File(dir, "log_folder_app.txt")
    private val folderName =
        kotlinx.coroutines.flow.MutableStateFlow(if (appFolderFile.exists()) APP_FOLDER else chosen()?.name)

    private fun chosen(): File? = runCatching {
        File(folderFile.readText().trim())
    }
        .getOrNull()
        ?.takeIf { it.path.isNotBlank() && it.isDirectory }

    override val canChooseFolder: Boolean = true

    override val chosenFolderName: StateFlow<String?>
        get() = folderName

    override fun onFolderPicked(uri: String) {
        val folder = runCatching { File(java.net.URI(uri)) }.getOrNull() ?: return
        if (!folder.isDirectory) return
        runCatching { folderFile.writeText(folder.absolutePath) }
            .onFailure { Logger.w(it) { "The log folder choice could not be kept" } }
        runCatching { appFolderFile.delete() }
        folderName.value = folder.name
        Logger.i { "The log folder was chosen (${folder.name})" }
        for (marker in listOf("last_export_24h.txt", "last_export_full.txt")) runCatching { File(dir, marker).delete() }
        scope.launch { autoExport() }
    }

    override fun useAppFolder() {
        runCatching {
            dir.mkdirs()
            folderFile.delete()
            appFolderFile.writeText(APP_FOLDER)
        }
            .onFailure { Logger.w(it) { "The app folder choice could not be kept" } }
        folderName.value = APP_FOLDER
        Logger.i { "The app's own folder was chosen for the logs" }
        for (marker in listOf("last_export_24h.txt", "last_export_full.txt")) runCatching { File(dir, marker).delete() }
        scope.launch { autoExport() }
    }

    override fun clearChosenFolder() {
        runCatching { folderFile.delete() }
        runCatching { appFolderFile.delete() }
        folderName.value = null
        Logger.i { "The log folder was cleared" }
    }

    override val recent: StateFlow<Boolean>
        get() = MtSwLogging.recent

    override fun setRecent(on: Boolean) = MtSwLogging.setRecent(on)

    override val canMakeReport: Boolean = true

    /** Refreshes the two zips in the logs folder when there is something new to put in them. */
    fun autoExport() {
        val crash = MtSwLogging.crashFiles().isNotEmpty()
        if (MtSwLogging.recent.value || MtSwLogging.level.value != AppLogLevel.OFF || crash) {
            exportIfNew(NAME_24H, "last_export_24h.txt", System.currentTimeMillis() - MtSwLogging.RECENT_WINDOW_MS)
        }
        if (MtSwLogging.level.value != AppLogLevel.OFF) exportIfNew(NAME_FULL, "last_export_full.txt", 0L)
    }

    private fun exportIfNew(name: String, markerName: String, since: Long) {
        runCatching {
            val files = (MtSwLogging.parts() + MtSwLogging.crashFiles()).filter { it.lastModified() >= since }
            val fingerprint =
                "${files.size}:${files.maxOfOrNull { it.lastModified() } ?: 0L}:${files.sumOf { it.length() }}"
            val marker = File(dir, markerName)
            if (runCatching { marker.readText() }.getOrNull() == fingerprint) return
            File(chosen() ?: dir, name).outputStream().use { MtSwLogging.writeReportZip(it, since) }
            marker.writeText(fingerprint)
        }
            .onFailure { Logger.w(it) { "Saving the log zip failed" } }
    }

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
            // A report always carries all the logs: the last 24 hours, the full log (up to 7 days) and the crash
            // records.
            val since = 0L
            val engineLogs =
                File(dir.parentFile, "engine-logs")
                    .listFiles { f -> f.isFile && (f.extension == "log" || f.extension == "txt") }
                    .orEmpty()
            file.outputStream().use { out ->
                MtSwLogging.writeReportZip(
                    out,
                    since,
                    extras = listOf("opis_zgloszenia.txt" to note.toByteArray()) + windowExtras(true) + extras,
                    extraFiles = engineLogs.map { "silnik/${it.name}" to it },
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

/**
 * What the window looks like, for a report about the layout: its size, place and screen (always) and a picture of it
 * (only together with the logs, because the picture can show messages). Best effort: when the system refuses the
 * picture (macOS without the screen recording permission), the report still gets the sizes.
 */
private fun windowExtras(withPicture: Boolean): List<Pair<String, ByteArray>> = runCatching {
    val windows = java.awt.Window.getWindows().filter { it.isShowing && it is java.awt.Frame }
    val window = windows.firstOrNull() ?: return@runCatching emptyList()
    val screens = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
    val info = buildString {
        appendLine("Okno: ${window.width} x ${window.height} px (położenie ${window.x}, ${window.y})")
        appendLine("Okno widoczne w całości: ${window.isShowing}")
        screens.forEachIndexed { index, device ->
            val mode = device.displayMode
            val scale = device.defaultConfiguration.defaultTransform.scaleX
            val bounds = device.defaultConfiguration.bounds
            appendLine("Ekran ${index + 1}: ${mode.width} x ${mode.height}, skala $scale, $bounds")
        }
    }
    val result = mutableListOf("okno.txt" to info.toByteArray())
    if (withPicture) {
        val image = java.awt.Robot().createScreenCapture(java.awt.Rectangle(window.locationOnScreen, window.size))
        val bytes = java.io.ByteArrayOutputStream()
        javax.imageio.ImageIO.write(image, "png", bytes)
        result += "okno.png" to bytes.toByteArray()
    }
    result
}
    .getOrDefault(emptyList())
