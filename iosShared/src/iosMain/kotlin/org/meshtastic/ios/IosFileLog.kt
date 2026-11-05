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

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.meshtastic.core.database.iosDataDir
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSLock
import platform.Foundation.timeIntervalSince1970
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook

/**
 * The app's log in the `logs` folder of the app's folder, built like the log of the other platforms: every line starts
 * with the date and time (to the millisecond), and the log is a series of parts of about [PART_BYTES] each
 * (`mt_sw_log_<session>_<nnn>.txt`), so a problem can be found and read piece by piece. The crash that ends the app is
 * written as well. Parts older than a day, and the oldest ones above [MAX_TOTAL_BYTES], are deleted.
 */
internal object IosFileLog : LogWriter() {
    private const val PART_BYTES = 400L * 1024
    private const val MAX_TOTAL_BYTES = 60L * 1024 * 1024
    private const val KEEP_MILLIS = 24L * 60 * 60 * 1000
    private const val PART_NUMBER_WIDTH = 3
    private const val MILLIS = 1000.0

    private val dir: Path by lazy { (iosDataDir() + "/logs").toPath() }
    private val lock = NSLock()
    private var part = 0
    private var partBytes = 0L
    private var session = ""

    private fun format(pattern: String, date: NSDate = NSDate()): String =
        NSDateFormatter()
            .apply {
                locale = NSLocale(localeIdentifier = "en_US_POSIX")
                dateFormat = pattern
            }
            .stringFromDate(date)

    fun install() {
        runCatching {
            FileSystem.SYSTEM.createDirectories(dir)
            // The single-file logs of the earlier versions are replaced by the parts.
            for (name in listOf("ios-app.log", "ios-app.old.log")) {
                val legacy = (iosDataDir() + "/" + name).toPath()
                if (FileSystem.SYSTEM.exists(legacy)) FileSystem.SYSTEM.delete(legacy)
            }
            session = format("yyyyMMdd-HHmmss")
            prune()
        }
        Logger.addLogWriter(this)
        @OptIn(ExperimentalNativeApi::class)
        setUnhandledExceptionHook { error -> append("CRASH", error.stackTraceToString()) }
    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        append("${severity.name.first()}/$tag", message + (throwable?.let { "\n" + it.stackTraceToString() } ?: ""))
    }

    private fun partFile(number: Int): Path =
        dir / "mt_sw_log_${session}_${number.toString().padStart(PART_NUMBER_WIDTH, '0')}.txt"

    private fun append(tag: String, text: String) {
        lock.lock()
        try {
            runCatching {
                val line = "${format("MM-dd HH:mm:ss.SSS")} $tag: $text\n"
                val header =
                    if (part == 0 || partBytes >= PART_BYTES) {
                        part++
                        partBytes = 0L
                        val opened = format("yyyy-MM-dd HH:mm:ss.SSS")
                        "=== MT_SW log (iOS), part $part, session $session, opened $opened ===\n"
                    } else {
                        ""
                    }
                val sink = FileSystem.SYSTEM.appendingSink(partFile(part)).buffer()
                try {
                    sink.writeUtf8(header + line)
                } finally {
                    sink.close()
                }
                partBytes += (header.length + line.length).toLong()
                if (header.isNotEmpty() && part > 1) prune()
            }
        } finally {
            lock.unlock()
        }
    }

    private fun prune() {
        val cutoff = (NSDate().timeIntervalSince1970 * MILLIS).toLong() - KEEP_MILLIS
        val files =
            FileSystem.SYSTEM.list(dir)
                .filter { it.name.startsWith("mt_sw_log_") }
                .map { it to FileSystem.SYSTEM.metadata(it) }
                .sortedBy { it.first.name }
        var total = files.sumOf { it.second.size ?: 0L }
        for ((file, meta) in files) {
            if (file == partFile(part)) continue
            val modified = meta.lastModifiedAtMillis ?: Long.MAX_VALUE
            if (modified < cutoff || total > MAX_TOTAL_BYTES) {
                total -= meta.size ?: 0L
                FileSystem.SYSTEM.delete(file)
            }
        }
    }
}
