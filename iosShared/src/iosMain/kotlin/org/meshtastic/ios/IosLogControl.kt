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
@file:OptIn(ExperimentalForeignApi::class)

package org.meshtastic.ios

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.meshtastic.core.common.log.AppLogControl
import org.meshtastic.core.common.log.AppLogLevel
import org.meshtastic.core.common.log.ReportFile
import org.meshtastic.core.common.log.ReportTarget
import org.meshtastic.core.common.util.Crc32
import org.meshtastic.core.database.iosDataDir
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.lowPowerModeEnabled
import platform.Foundation.thermalState
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationDidReceiveMemoryWarningNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.UIKit.UIApplicationWillTerminateNotification
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientationDidChangeNotification
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.darwin.NSObject

/**
 * The settings switches, the folder choice and the report package for the iPhone and iPad log ([IosFileLog]). An iPhone
 * has no Downloads folder for apps, so the person chooses a folder in the Files app; a fresh zip of the logs is then
 * written there on its own (at start, every few minutes and when the app goes to the background), like the Downloads
 * folder of Android. The same package can be sent at any time with the report button.
 */
@Suppress("TooManyFunctions")
internal object IosLogControl : AppLogControl {
    private const val REPORT_DIR = "reports"
    private const val KB = 1024
    private const val VIEW_SIZE = 1.0
    private const val NAME_24H = "mt_sw_logi_24h.zip"
    private const val NAME_FULL = "mt_sw_logi_pelne.zip"
    private const val AUTO_EXPORT_EVERY_MS = 3 * 60 * 1000L
    private const val BOOKMARK_FILE = "folder.bookmark"
    private const val NAME_FILE = "folder.name"
    private const val LIFECYCLE = "AppLifecycle"
    private const val SAMPLE_EVERY_MS = 30_000L
    private const val PERCENT = 100
    private const val MILLIS_PER_SECOND = 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val folderName = MutableStateFlow<String?>(null)

    // The picker only keeps a weak reference to its delegate.
    private var pickerDelegate: FolderPickerDelegate? = null

    private val bookmarkPath: String
        get() = iosDataDir() + "/logs/" + BOOKMARK_FILE

    private val statePath: Path
        get() = (iosDataDir() + "/logs/last_state.txt").toPath()

    private val namePath: Path
        get() = (iosDataDir() + "/logs/" + NAME_FILE).toPath()

    override val level: StateFlow<AppLogLevel>
        get() = IosFileLog.level

    override fun setLevel(level: AppLogLevel) = IosFileLog.setLevel(level)

    override fun openFolder(): Boolean = false

    override val recent: StateFlow<Boolean>
        get() = IosFileLog.recent

    override fun setRecent(on: Boolean) = IosFileLog.setRecent(on)

    override val canChooseFolder: Boolean = true

    override val chosenFolderName: StateFlow<String?>
        get() = folderName

    override val canMakeReport: Boolean = true

    override val sharesFileToApp: Boolean = true

    /** Reads the remembered folder and starts the automatic saving. Called once at start-up. */
    fun start() {
        folderName.value = runCatching { FileSystem.SYSTEM.read(namePath) { readUtf8() } }.getOrNull()?.ifBlank { null }
        scope.launch {
            while (true) {
                runCatching { IosFileLog.pruneRecent() }
                exportNow()
                delay(AUTO_EXPORT_EVERY_MS)
            }
        }
        logPreviousExit()
        markState("foreground")
        observe(UIApplicationDidEnterBackgroundNotification) {
            markState("background")
            Logger.i(tag = LIFECYCLE) { "App process: ON_STOP (entered the background)" }
            exportNow()
        }
        observe(UIApplicationWillEnterForegroundNotification) {
            markState("foreground")
            Logger.i(tag = LIFECYCLE) { "App process: ON_START (back in the foreground)" }
        }
        observe(UIApplicationDidBecomeActiveNotification) { Logger.i(tag = LIFECYCLE) { "App process: ON_RESUME" } }
        observe(UIApplicationWillResignActiveNotification) { Logger.i(tag = LIFECYCLE) { "App process: ON_PAUSE" } }
        observe(UIApplicationDidReceiveMemoryWarningNotification) { Logger.w(tag = LIFECYCLE) { "Memory warning" } }
        observe(UIApplicationWillTerminateNotification) {
            markState("terminated")
            Logger.i(tag = LIFECYCLE) { "App will terminate" }
        }
        Logger.i(tag = LIFECYCLE) { "App start: ${IosFileLog.header()}; log level ${IosFileLog.level.value.key}" }
        UIDevice.currentDevice.batteryMonitoringEnabled = true
        UIDevice.currentDevice.beginGeneratingDeviceOrientationNotifications()
        observe(UIDeviceOrientationDidChangeNotification) {
            Logger.d(tag = LIFECYCLE) { "Configuration changed: orientation ${UIDevice.currentDevice.orientation}" }
        }
        val begin = IosFileLog.nowMillis()
        scope.launch {
            while (true) {
                delay(SAMPLE_EVERY_MS)
                val device = UIDevice.currentDevice
                val info = NSProcessInfo.processInfo
                Logger.d(tag = "Resources") {
                    val up = (IosFileLog.nowMillis() - begin) / MILLIS_PER_SECOND
                    "cpus ${info.activeProcessorCount}, up $up s, " +
                        "battery ${(device.batteryLevel * PERCENT).toInt()}%, thermal state ${info.thermalState}, " +
                        "low power mode ${info.lowPowerModeEnabled}"
                }
            }
        }
    }

    /** Remembers the last known state, so the next start can say how the earlier run ended. */
    private fun markState(state: String) {
        runCatching { FileSystem.SYSTEM.write(statePath) { writeUtf8("${IosFileLog.nowMillis()} $state") } }
    }

    /**
     * iOS does not tell an app why it was closed, so the last state written by the earlier run is read: a run that
     * ended in the background was most likely closed by the system, one that ended in the foreground crashed or was
     * swiped away.
     */
    private fun logPreviousExit() {
        val last = runCatching { FileSystem.SYSTEM.read(statePath) { readUtf8() } }.getOrNull() ?: return
        val state = last.substringAfter(' ', "?")
        val meaning =
            when (state) {
                "terminated" -> "closed normally"
                "background" -> "ended in the background (most likely closed by the system, memory or time)"
                else -> "ended in the foreground (crash or swiped away by the person)"
            }
        Logger.i(tag = LIFECYCLE) { "Earlier exit: last state $state at ${last.substringBefore(' ')} ms, $meaning" }
    }

    private fun observe(name: String?, block: () -> Unit) {
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = name,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            block()
        }
    }

    override fun chooseFolder() {
        val top = topViewController() ?: return
        val delegate = FolderPickerDelegate { url -> remember(url) }
        pickerDelegate = delegate
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeFolder))
        picker.delegate = delegate
        top.presentViewController(picker, animated = true, completion = null)
    }

    override fun clearChosenFolder() {
        runCatching {
            FileSystem.SYSTEM.delete(bookmarkPath.toPath())
            FileSystem.SYSTEM.delete(namePath)
        }
        folderName.value = null
    }

    /** Keeps the picked folder for later runs (a bookmark keeps the access) and writes the first zip into it. */
    private fun remember(url: NSURL?) {
        if (url == null) return
        val opened = url.startAccessingSecurityScopedResource()
        try {
            val data =
                url.bookmarkDataWithOptions(
                    0uL,
                    includingResourceValuesForKeys = null,
                    relativeToURL = null,
                    error = null,
                )
            if (data == null || !data.writeToFile(bookmarkPath, atomically = true)) return
            val name = url.lastPathComponent ?: "?"
            FileSystem.SYSTEM.write(namePath) { writeUtf8(name) }
            folderName.value = name
        } finally {
            if (opened) url.stopAccessingSecurityScopedResource()
        }
        for (marker in listOf("last_export_24h.txt", "last_export_full.txt")) {
            runCatching { FileSystem.SYSTEM.delete((iosDataDir() + "/logs/" + marker).toPath()) }
        }
        exportNow()
    }

    /**
     * Writes fresh zips into the chosen folder; does nothing without one: the logs of the last 24 hours (the automatic
     * log) and, apart from it, the full log (only while it is on).
     */
    fun exportNow() {
        val crash = IosFileLog.crashFiles().isNotEmpty()
        if (IosFileLog.recent.value || IosFileLog.level.value != AppLogLevel.OFF || crash) {
            exportOne(NAME_24H, "last_export_24h.txt", allTime = false)
        }
        if (IosFileLog.level.value != AppLogLevel.OFF) exportOne(NAME_FULL, "last_export_full.txt", allTime = true)
    }

    @Suppress("ReturnCount")
    private fun exportOne(name: String, markerName: String, allTime: Boolean) {
        runCatching {
            val since = if (allTime) 0L else IosFileLog.nowMillis() - IosFileLog.RECENT_WINDOW_MS
            val files = (IosFileLog.parts() + IosFileLog.crashFiles()).filter { IosFileLog.modified(it) >= since }
            val fingerprint =
                "${files.size}:${files.maxOfOrNull { IosFileLog.modified(it) } ?: 0L}:" +
                    "${files.sumOf { IosFileLog.size(it) }}"
            val marker = (iosDataDir() + "/logs/" + markerName).toPath()
            if (runCatching { FileSystem.SYSTEM.read(marker) { readUtf8() } }.getOrNull() == fingerprint) return
            val data = NSData.dataWithContentsOfFile(bookmarkPath) ?: return
            val url = NSURL.URLByResolvingBookmarkData(data, 0uL, null, null, null) ?: return
            val opened = url.startAccessingSecurityScopedResource()
            try {
                val folder = url.path ?: return
                val bytes = zip(reportEntries("", true, emptyList(), allTime))
                FileSystem.SYSTEM.write(folder.toPath() / name) { write(bytes) }
                FileSystem.SYSTEM.write(marker) { writeUtf8(fingerprint) }
            } finally {
                if (opened) url.stopAccessingSecurityScopedResource()
            }
        }
            .onFailure { Logger.w(it, tag = "Log") { "Saving the logs to the chosen folder failed" } }
    }

    private fun reportEntries(
        note: String,
        withLogs: Boolean,
        extras: List<Pair<String, ByteArray>>,
        allTime: Boolean = false,
    ): List<Pair<String, ByteArray>> {
        val now = IosFileLog.nowMillis()
        val since =
            when {
                !withLogs -> Long.MAX_VALUE
                allTime -> 0L
                else -> now - IosFileLog.RECENT_WINDOW_MS
            }
        val files = (IosFileLog.parts() + IosFileLog.crashFiles()).filter { IosFileLog.modified(it) >= since }
        val toc = StringBuilder("MT_SW log package, made ${IosFileLog.stamp()}\n${IosFileLog.header()}\n")
        if (withLogs && !allTime) toc.append("Only what was written in the last 24 hours\n")
        val all = listOf("opis_zgloszenia.txt" to note.encodeToByteArray()) + extras
        for ((name, bytes) in all) toc.append("$name: ${bytes.size / KB} KB\n")
        val logs = files.map { f -> (if (f.parent?.name == "crash") "crash/" else "") + f.name to read(f) }
        for ((name, bytes) in logs) {
            val lines = bytes.decodeToString().lines()
            val errors = lines.count { " E/" in it }
            val warnings = lines.count { " W/" in it }
            toc.append("$name: ${bytes.size / KB} KB, $errors errors, $warnings warnings\n")
        }
        val engine =
            if (!withLogs) {
                emptyList()
            } else {
                runCatching {
                    val dir = (iosDataDir() + "/engine-logs").toPath()
                    FileSystem.SYSTEM.list(dir)
                        .filter { it.name.endsWith(".log") }
                        .map { "silnik/${it.name}" to read(it) }
                }
                    .getOrDefault(emptyList())
            }
        for ((name, bytes) in engine) toc.append("$name: ${bytes.size / KB} KB\n")
        return listOf("spis_tresci.txt" to toc.toString().encodeToByteArray()) + all + engine + logs
    }

    override suspend fun buildReport(
        note: String,
        withLogs: Boolean,
        extras: List<Pair<String, ByteArray>>,
    ): ReportFile? = withContext(Dispatchers.Default) {
        runCatching {
            val now = IosFileLog.nowMillis()
            val folder = (NSTemporaryDirectory().trimEnd('/') + "/" + REPORT_DIR).toPath()
            FileSystem.SYSTEM.createDirectories(folder)
            FileSystem.SYSTEM.list(folder)
                .filter { IosFileLog.modified(it) < now - IosFileLog.RECENT_WINDOW_MS }
                .forEach { runCatching { FileSystem.SYSTEM.delete(it) } }
            val name = "mt_sw_zgloszenie_" + IosFileLog.stamp().replace(Regex("[^0-9]"), "") + ".zip"
            val file = folder / name
            val bytes = zip(reportEntries(note, withLogs, extras))
            FileSystem.SYSTEM.write(file) { write(bytes) }
            ReportFile(file.toString(), name, IosFileLog.size(file) / KB)
        }
            .onFailure { Logger.w(it, tag = "Log") { "Report could not be built" } }
            .getOrNull()
    }

    override fun shareReport(report: ReportFile, target: ReportTarget, text: String): Boolean {
        val top = topViewController() ?: return false
        val sheet =
            UIActivityViewController(
                activityItems = listOf(text, NSURL.fileURLWithPath(report.path)),
                applicationActivities = null,
            )
        // On the iPad the sheet needs a place to hang on.
        sheet.popoverPresentationController?.apply {
            sourceView = top.view
            sourceRect = CGRectMake(0.0, 0.0, VIEW_SIZE, VIEW_SIZE)
        }
        top.presentViewController(sheet, animated = true, completion = null)
        return true
    }

    private fun read(path: Path): ByteArray = FileSystem.SYSTEM.read(path) { readByteArray() }

    private fun topViewController(): UIViewController? {
        val scene =
            UIApplication.sharedApplication.connectedScenes.firstOrNull { it is UIWindowScene } as? UIWindowScene
        val window: UIWindow? =
            scene?.windows?.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow
                ?: scene?.windows?.firstOrNull() as? UIWindow
        var top = window?.rootViewController
        while (top?.presentedViewController != null) top = top.presentedViewController
        return top
    }

    private class FolderPickerDelegate(private val onPicked: (NSURL?) -> Unit) :
        NSObject(),
        UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            onPicked(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            onPicked(null)
        }
    }

    /** A zip without compression: enough for text logs, and no library is needed. */
    private fun zip(entries: List<Pair<String, ByteArray>>): ByteArray {
        val out = Buffer()
        val central = Buffer()
        for ((name, bytes) in entries) {
            val nameBytes = name.encodeToByteArray()
            val crc = Crc32.compute(bytes.toByteString()).toInt()
            val offset = out.size.toInt()
            out.writeIntLe(LOCAL_SIG).writeShortLe(VERSION).writeShortLe(UTF8_FLAG).writeShortLe(0)
            out.writeShortLe(0).writeShortLe(DOS_DATE).writeIntLe(crc)
            out.writeIntLe(bytes.size).writeIntLe(bytes.size).writeShortLe(nameBytes.size).writeShortLe(0)
            out.write(nameBytes).write(bytes)
            central.writeIntLe(CENTRAL_SIG).writeShortLe(VERSION).writeShortLe(VERSION).writeShortLe(UTF8_FLAG)
            central.writeShortLe(0).writeShortLe(0).writeShortLe(DOS_DATE).writeIntLe(crc)
            central.writeIntLe(bytes.size).writeIntLe(bytes.size).writeShortLe(nameBytes.size)
            central.writeShortLe(0).writeShortLe(0).writeShortLe(0).writeShortLe(0).writeIntLe(0).writeIntLe(offset)
            central.write(nameBytes)
        }
        val centralOffset = out.size.toInt()
        val centralSize = central.size.toInt()
        out.writeAll(central)
        out.writeIntLe(END_SIG).writeShortLe(0).writeShortLe(0)
        out.writeShortLe(entries.size).writeShortLe(entries.size)
        out.writeIntLe(centralSize).writeIntLe(centralOffset).writeShortLe(0)
        return out.readByteArray()
    }

    private const val LOCAL_SIG = 0x04034b50
    private const val CENTRAL_SIG = 0x02014b50
    private const val END_SIG = 0x06054b50
    private const val VERSION = 20
    private const val UTF8_FLAG = 0x0800
    private const val DOS_DATE = 0x0021
}
