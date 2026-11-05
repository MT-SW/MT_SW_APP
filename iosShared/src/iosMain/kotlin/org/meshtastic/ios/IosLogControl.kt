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
package org.meshtastic.ios

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
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
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

/** The settings switches and the report package for the iPhone and iPad log ([IosFileLog]). */
internal object IosLogControl : AppLogControl {
    private const val REPORT_DIR = "reports"
    private const val KB = 1024
    private const val VIEW_SIZE = 1.0

    override val level: StateFlow<AppLogLevel>
        get() = IosFileLog.level

    override fun setLevel(level: AppLogLevel) = IosFileLog.setLevel(level)

    override fun openFolder(): Boolean = false

    override val recent: StateFlow<Boolean>
        get() = IosFileLog.recent

    override fun setRecent(on: Boolean) = IosFileLog.setRecent(on)

    override val canMakeReport: Boolean = true

    override val sharesFileToApp: Boolean = true

    override suspend fun buildReport(
        note: String,
        withLogs: Boolean,
        extras: List<Pair<String, ByteArray>>,
    ): ReportFile? =
        withContext(Dispatchers.Default) {
            runCatching {
                val now = IosFileLog.nowMillis()
                val folder = (NSTemporaryDirectory().trimEnd('/') + "/" + REPORT_DIR).toPath()
                FileSystem.SYSTEM.createDirectories(folder)
                FileSystem.SYSTEM.list(folder)
                    .filter { IosFileLog.modified(it) < now - IosFileLog.RECENT_WINDOW_MS }
                    .forEach { runCatching { FileSystem.SYSTEM.delete(it) } }
                val since = if (withLogs) now - IosFileLog.RECENT_WINDOW_MS else Long.MAX_VALUE
                val files = (IosFileLog.parts() + IosFileLog.crashFiles()).filter { IosFileLog.modified(it) >= since }
                val entries = mutableListOf<Pair<String, ByteArray>>()
                val toc = StringBuilder("MT_SW log package, made ${IosFileLog.stamp()}\n${IosFileLog.header()}\n")
                if (withLogs) toc.append("Only what was written in the last 24 hours\n")
                val all = listOf("opis_zgloszenia.txt" to note.encodeToByteArray()) + extras
                for ((name, bytes) in all) toc.append("$name: ${bytes.size / KB} KB\n")
                val logs = files.map { f -> (if (f.parent?.name == "crash") "crash/" else "") + f.name to read(f) }
                for ((name, bytes) in logs) {
                    val text = bytes.decodeToString()
                    val errors = text.lines().count { " E/" in it }
                    val warnings = text.lines().count { " W/" in it }
                    toc.append("$name: ${bytes.size / KB} KB, $errors errors, $warnings warnings\n")
                }
                entries += "spis_tresci.txt" to toc.toString().encodeToByteArray()
                entries += all
                entries += logs
                val name = "mt_sw_zgloszenie_" + IosFileLog.stamp().replace(Regex("[^0-9]"), "") + ".zip"
                val file = folder / name
                FileSystem.SYSTEM.write(file) { write(zip(entries)) }
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
