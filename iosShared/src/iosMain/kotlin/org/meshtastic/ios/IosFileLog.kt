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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.meshtastic.core.common.log.AppLogLevel
import org.meshtastic.core.database.iosDataDir
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSLock
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSThread
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIDevice
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import kotlin.native.setUnhandledExceptionHook

/**
 * The fork's log files on iPhone and iPad, built exactly like [org.meshtastic.core.common.log.AppLogControl]'s other
 * platforms (Android and desktop):
 * - Off by default; the choice and the switch of the automatic log of the last 24 hours are remembered in a small
 *   file (`logging.conf`) next to the logs.
 * - A log is a series of parts of about [PART_BYTES] each (`mt_sw_log_<session>_<nnn>.txt`) in the `logs` folder; every
 *   part opens with its own header and every line starts with the date and time to the millisecond.
 * - While only the automatic log of the last day runs (on by default), parts older than a day go and the parts stay
 *   under [RECENT_MAX_BYTES]. The oldest parts always go once all of them pass [MAX_TOTAL_BYTES].
 * - The cause of a crash is written to its own small file in `crash/` always: the stack trace and nothing else.
 */
internal object IosFileLog : LogWriter() {
    const val PART_BYTES = 400L * 1024
    const val RECENT_WINDOW_MS = 24L * 60 * 60 * 1000
    private const val MAX_TOTAL_BYTES = 300L * 1024 * 1024
    private const val RECENT_MAX_BYTES = 40L * 1024 * 1024
    private const val MAX_CRASH_FILES = 20
    private const val PART_NUMBER_WIDTH = 3
    private const val MILLIS = 1000.0
    private const val MILLIS_PER_SECOND = 1000L
    private const val BYTES_IN_MB = 1024UL * 1024UL
    private const val CONF_NAME = "logging.conf"
    private const val RECENT_OFF = "recent=off"

    private val dir: Path by lazy { (iosDataDir() + "/logs").toPath() }
    private val crashDir: Path by lazy { dir / "crash" }
    private val lock = NSLock()
    private var part = 0
    private var partBytes = 0L
    private var open = false
    private var currentName = ""
    private var session = ""
    private var startedAt = 0L

    private val levelFlow = MutableStateFlow(AppLogLevel.OFF)
    val level: StateFlow<AppLogLevel>
        get() = levelFlow

    private val recentFlow = MutableStateFlow(true)
    val recent: StateFlow<Boolean>
        get() = recentFlow

    /** What is really written: the chosen level, or the basic level while only the log of the last day is on. */
    private fun effectiveLevel(): AppLogLevel =
        levelFlow.value.let { if (it == AppLogLevel.OFF && recentFlow.value) AppLogLevel.BASIC else it }

    fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * MILLIS).toLong()

    private fun format(pattern: String): String =
        NSDateFormatter()
            .apply {
                locale = NSLocale(localeIdentifier = "en_US_POSIX")
                dateFormat = pattern
            }
            .stringFromDate(NSDate())

    private fun threadName(): String =
        if (NSThread.isMainThread) "main" else NSThread.currentThread.name?.takeIf { it.isNotBlank() } ?: "worker"

    fun stamp(): String = format("MM-dd HH:mm:ss.SSS")

    /** Repeated at the top of every part and of the report. */
    fun header(): String =
        "MT_SW (iOS), ${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}, " +
            UIDevice.currentDevice.model

    fun install() {
        runCatching {
            FileSystem.SYSTEM.createDirectories(crashDir)
            // The single-file logs of the earlier versions are replaced by the parts.
            for (name in listOf("ios-app.log", "ios-app.old.log")) {
                val legacy = (iosDataDir() + "/" + name).toPath()
                if (FileSystem.SYSTEM.exists(legacy)) FileSystem.SYSTEM.delete(legacy)
            }
            session = format("yyyyMMdd-HHmmss")
            startedAt = nowMillis()
            val conf = runCatching { FileSystem.SYSTEM.read(dir / CONF_NAME) { readUtf8() }.lines() }.getOrNull()
            levelFlow.value = AppLogLevel.fromKey(conf?.firstOrNull())
            recentFlow.value = conf?.getOrNull(1)?.trim() != RECENT_OFF
            pruneRecent()
        }
        applySeverity()
        Logger.addLogWriter(this)
        @OptIn(ExperimentalNativeApi::class)
        setUnhandledExceptionHook { error ->
            runCatching { Logger.e(error, tag = "Log") { "Uncaught exception in thread ${threadName()}" } }
            writeCrash(error.stackTraceToString())
        }
    }

    fun setLevel(level: AppLogLevel) {
        val before = levelFlow.value
        levelFlow.value = level
        saveConf()
        if (effectiveLevel() == AppLogLevel.OFF) closeCurrent()
        applySeverity()
        if (before != level) Logger.i(tag = "Log") { "Log level: ${before.key} -> ${level.key}" }
    }

    fun setRecent(on: Boolean) {
        if (recentFlow.value == on) return
        recentFlow.value = on
        saveConf()
        if (effectiveLevel() == AppLogLevel.OFF) closeCurrent()
        applySeverity()
        Logger.i(tag = "Log") { "Automatic log of the last day: ${if (on) "on" else "off"}" }
    }

    /**
     * Kermit's lowest severity, as on Android: what the build normally keeps (Debug in a debug build, Info otherwise),
     * or lower while the full log is on, so the per-packet messages are not even built when nobody reads them.
     */
    @OptIn(ExperimentalNativeApi::class)
    private fun applySeverity() {
        val base = if (Platform.isDebugBinary) Severity.Debug else Severity.Info
        val chosen = levelFlow.value.severity
        Logger.setMinSeverity(if (chosen != null && chosen < base) chosen else base)
    }

    private fun saveConf() {
        runCatching {
            FileSystem.SYSTEM.write(dir / CONF_NAME) {
                writeUtf8(levelFlow.value.key + "\n" + (if (recentFlow.value) "recent=on" else RECENT_OFF))
            }
        }
    }

    override fun isLoggable(tag: String, severity: Severity): Boolean {
        val min = effectiveLevel().severity ?: return false
        return severity >= min
    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val line = buildString {
            append(stamp()).append(' ').append(severity.name.first()).append('/').append(tag)
            append(" [").append(threadName()).append("] ")
            append(message).append('\n')
            throwable?.let { append(it.stackTraceToString()).append('\n') }
        }
        lock.lock()
        try {
            runCatching {
                val head =
                    if (!open) {
                        part++
                        partBytes = 0L
                        open = true
                        currentName = partName(part)
                        "=== MT_SW log, part $part, session $session, opened ${stamp()} ===\n" +
                            header() +
                            "\n=== line: MM-dd HH:mm:ss.SSS L/tag [thread] message ===\n"
                    } else {
                        ""
                    }
                val sink = FileSystem.SYSTEM.appendingSink(dir / currentName).buffer()
                try {
                    sink.writeUtf8(head + line)
                } finally {
                    sink.close()
                }
                partBytes += head.length + line.length
                if (head.isNotEmpty()) pruneOld()
                if (partBytes >= PART_BYTES) open = false
            }
        } finally {
            lock.unlock()
        }
    }

    private fun closeCurrent() {
        lock.lock()
        open = false
        lock.unlock()
    }

    private fun partName(number: Int) =
        "mt_sw_log_${session}_${number.toString().padStart(PART_NUMBER_WIDTH, '0')}.txt"

    /** All log parts, oldest first. */
    fun parts(): List<Path> =
        runCatching {
            FileSystem.SYSTEM.list(dir).filter { it.name.startsWith("mt_sw_log_") }.sortedBy { it.name }
        }
            .getOrDefault(emptyList())

    fun crashFiles(): List<Path> =
        runCatching { FileSystem.SYSTEM.list(crashDir).filter { it.name.startsWith("crash_") }.sortedBy { it.name } }
            .getOrDefault(emptyList())

    fun size(path: Path): Long = FileSystem.SYSTEM.metadataOrNull(path)?.size ?: 0L

    fun modified(path: Path): Long = FileSystem.SYSTEM.metadataOrNull(path)?.lastModifiedAtMillis ?: 0L

    /** Drops the parts older than a day while only the automatic log runs; also called every few minutes. */
    fun pruneRecent() {
        if (levelFlow.value != AppLogLevel.OFF) return
        val cutoff = nowMillis() - RECENT_WINDOW_MS
        val all = parts()
        var total = all.sumOf { size(it) }
        for (f in all) {
            if (f.name == currentName) continue
            if (modified(f) < cutoff || total > RECENT_MAX_BYTES) {
                total -= size(f)
                runCatching { FileSystem.SYSTEM.delete(f) }
            }
        }
    }

    private fun pruneOld() {
        val all = parts()
        var total = all.sumOf { size(it) }
        for (f in all) {
            if (total <= MAX_TOTAL_BYTES) break
            if (f.name.contains(session)) continue
            total -= size(f)
            runCatching { FileSystem.SYSTEM.delete(f) }
        }
    }

    /** The cause of a crash: the stack trace and nothing else (no messages, no positions, no keys). */
    private fun writeCrash(trace: String) {
        runCatching {
            FileSystem.SYSTEM.createDirectories(crashDir)
            val name = "crash_" + format("yyyyMMdd-HHmmss") + ".txt"
            FileSystem.SYSTEM.write(crashDir / name) { writeUtf8(
                    "Crash at ${stamp()}, ${(nowMillis() - startedAt) / MILLIS_PER_SECOND} s after start\n" +
                        header() +
                        "\nThread: ${threadName()}\n" +
                        "Memory: device ${NSProcessInfo.processInfo.physicalMemory / BYTES_IN_MB} MB\n\n$trace\n",
                ) }
            crashFiles().dropLast(MAX_CRASH_FILES).forEach { runCatching { FileSystem.SYSTEM.delete(it) } }
        }
    }
}
