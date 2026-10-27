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
package org.meshtastic.core.common.log

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The fork's log files, shared by Android and desktop. Off by default; once switched on in the settings the choice is
 * remembered in a small file next to the logs, so it survives restarts and crashes.
 *
 * - Everything the app logs through Kermit goes to the files (as far down as the chosen level), so one level covers
 *   all modules.
 * - A log is a series of parts of about [PART_BYTES] each (`mt_sw_log_<session>_<nnn>.txt`). Every part opens with its
 *   own header, so it can be read alone.
 * - The oldest parts are deleted once all of them together pass [MAX_TOTAL_BYTES].
 * - The cause of a crash is written to its own small file in `crash/` always, whatever the level: the stack trace and
 *   the memory state, nothing else (no messages, no positions, no keys).
 */
object MtSwLogging {
    const val PART_BYTES: Long = 400L * 1024
    const val MAX_TOTAL_BYTES: Long = 300L * 1024 * 1024
    private const val MAX_CRASH_FILES = 20
    private const val CONF_NAME = "logging.conf"
    private const val CRASH_DIR = "crash"
    private const val FLUSH_EVERY_MS = 1000L
    private const val SAMPLE_EVERY_MS = 30_000L
    private const val PART_NUMBER_WIDTH = 3
    private const val BYTES_IN_MB = 1024 * 1024

    private val levelFlow = MutableStateFlow(AppLogLevel.OFF)
    val level: StateFlow<AppLogLevel> get() = levelFlow

    @Volatile private var dir: File? = null

    @Volatile private var header: () -> String = { "" }

    @Volatile private var extraSample: () -> String = { "" }

    @Volatile private var onLevelChanged: (AppLogLevel) -> Unit = {}

    private val lock = Any()
    private var out: BufferedWriter? = null
    private var counter: CountingStream? = null
    private var part = 0
    private val session = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    private val lineTime = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
    private val started = System.currentTimeMillis()
    private val fmtLock = Any()

    private fun stamp(): String = synchronized(fmtLock) { lineTime.format(Date()) }

    /** The writer to add to Kermit's writers. It is cheap while the level is [AppLogLevel.OFF]. */
    val writer: LogWriter =
        object : LogWriter() {
            override fun isLoggable(tag: String, severity: Severity): Boolean {
                val min = levelFlow.value.severity ?: return false
                return severity >= min
            }

            override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                append(severity, message, tag, throwable)
            }
        }

    /**
     * Prepares the log folder and reads the remembered level. [headerText] is repeated at the top of every part (app
     * version, device); [onChange] is told whenever the level changes, so the platform can adjust Kermit's global
     * severity with [minSeverityFor]. Safe to call more than once.
     */
    fun init(logDir: File, headerText: () -> String, sample: () -> String = { "" }, onChange: (AppLogLevel) -> Unit) {
        dir = logDir
        header = headerText
        extraSample = sample
        onLevelChanged = onChange
        runCatching { logDir.mkdirs() }
        levelFlow.value = AppLogLevel.fromKey(runCatching { File(logDir, CONF_NAME).readText() }.getOrNull())
        installCrashRecorder()
        startBackgroundThread()
        installShutdownHook()
        onLevelChanged(levelFlow.value)
    }

    /** Kermit's global minimum severity: the lower of [base] (what the build normally keeps) and the chosen level. */
    fun minSeverityFor(base: Severity, level: AppLogLevel = levelFlow.value): Severity {
        val chosen = level.severity ?: return base
        return if (chosen < base) chosen else base
    }

    fun setLevel(level: AppLogLevel) {
        val folder = dir ?: return
        runCatching { File(folder, CONF_NAME).writeText(level.key) }
        val before = levelFlow.value
        levelFlow.value = level
        if (level == AppLogLevel.OFF) synchronized(lock) { closeCurrent() }
        onLevelChanged(level)
        if (before != level) Logger.i(tag = TAG) { "Log level: ${before.key} -> ${level.key}" }
    }

    /** All log parts, oldest first. */
    fun parts(): List<File> =
        dir?.listFiles { f -> f.isFile && f.name.startsWith("mt_sw_log_") && f.name.endsWith(".txt") }
            ?.sortedBy { it.name }
            .orEmpty()

    /** The crash records, oldest first. */
    fun crashFiles(): List<File> =
        dir?.let { File(it, CRASH_DIR) }?.listFiles { f -> f.isFile }?.sortedBy { it.name }.orEmpty()

    /** Writes a zip with every part, the crash records and a table of contents, and flushes the open part first. */
    fun writeZip(target: OutputStream) {
        flush()
        ZipOutputStream(target).use { zip ->
            val toc = StringBuilder("MT_SW log package, made ${stamp()}\n").append(header()).append('\n')
            val files = parts() + crashFiles()
            for (f in files) {
                val stats = scan(f)
                toc.append("${f.name}: ${f.length() / 1024} KB, ${stats.first} errors, ${stats.second} warnings\n")
            }
            zip.putNextEntry(ZipEntry("spis_tresci.txt"))
            zip.write(toc.toString().toByteArray())
            zip.closeEntry()
            for (f in files) {
                zip.putNextEntry(ZipEntry((if (f.parentFile?.name == CRASH_DIR) "crash/" else "") + f.name))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /** Pushes buffered text to the disk. */
    fun flush() {
        synchronized(lock) { runCatching { out?.flush() } }
    }

    /** Where the logs are written, or null before [init]. */
    fun folder(): File? = dir

    // ---- internals ----

    private fun scan(file: File): Pair<Int, Int> {
        var errors = 0
        var warnings = 0
        runCatching {
            file.useLines { lines ->
                for (line in lines) {
                    if (line.length > LEVEL_COLUMN && line[LEVEL_COLUMN - 1] == ' ') {
                        when (line[LEVEL_COLUMN]) {
                            'E', 'A' -> errors++
                            'W' -> warnings++
                        }
                    }
                }
            }
        }
        return errors to warnings
    }

    private fun append(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val line =
            buildString {
                append(stamp()).append(' ')
                append(severity.name.first()).append('/').append(tag)
                append(" [").append(Thread.currentThread().name).append("] ")
                append(message).append('\n')
                throwable?.let { append(it.stackTraceToString()).append('\n') }
            }
        synchronized(lock) {
            runCatching {
                val w = out ?: openNext()
                w.write(line)
                if (severity >= Severity.Warn) w.flush()
                if ((counter?.count ?: 0L) >= PART_BYTES) {
                    w.flush()
                    closeCurrent()
                }
            }
        }
    }

    private fun openNext(): BufferedWriter {
        val folder = requireNotNull(dir)
        part++
        val name = "mt_sw_log_${session}_${part.toString().padStart(PART_NUMBER_WIDTH, '0')}.txt"
        val stream = CountingStream(FileOutputStream(File(folder, name), true))
        val writer = BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8), 64 * 1024)
        counter = stream
        out = writer
        writer.write("=== MT_SW log, part $part, session $session, opened ${stamp()} ===\n")
        writer.write(header())
        writer.write("\n=== line: MM-dd HH:mm:ss.SSS L/tag [thread] message ===\n")
        pruneOld(folder)
        return writer
    }

    private fun closeCurrent() {
        runCatching { out?.flush() }
        runCatching { out?.close() }
        out = null
        counter = null
    }

    private fun pruneOld(folder: File) {
        val all = parts()
        var total = all.sumOf { it.length() }
        for (f in all) {
            if (total <= MAX_TOTAL_BYTES) break
            if (f.parentFile == folder && f.name.contains(session)) continue
            total -= f.length()
            runCatching { f.delete() }
        }
    }

    private fun installShutdownHook() {
        if (shutdownHookInstalled) return
        shutdownHookInstalled = true
        runCatching {
            Runtime.getRuntime().addShutdownHook(
                Thread(
                    {
                        runCatching { Logger.i(tag = TAG) { "Process is shutting down" } }
                        flush()
                    },
                    "mtsw-log-shutdown",
                ),
            )
        }
    }

    private fun installCrashRecorder() {
        if (crashRecorderInstalled) return
        crashRecorderInstalled = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { Logger.e(error, tag = TAG) { "Uncaught exception in thread ${thread.name}" } }
            runCatching { writeCrashRecord(thread, error) }
            flush()
            previous?.uncaughtException(thread, error)
        }
    }

    private fun writeCrashRecord(thread: Thread, error: Throwable) {
        val folder = File(dir ?: return, CRASH_DIR).also { it.mkdirs() }
        val rt = Runtime.getRuntime()
        val text =
            buildString {
                append("Crash at ${stamp()}, ${(System.currentTimeMillis() - started) / 1000} s after start\n")
                append(header()).append('\n')
                append("Thread: ${thread.name}\n")
                append("Memory: used ${(rt.totalMemory() - rt.freeMemory()) / BYTES_IN_MB} MB, max ${rt.maxMemory() / BYTES_IN_MB} MB\n")
                append("Threads: ${Thread.getAllStackTraces().size}\n\n")
                append(error.stackTraceToString())
            }
        File(folder, "crash_${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.txt").writeText(text)
        folder.listFiles()?.sortedBy { it.name }?.dropLast(MAX_CRASH_FILES)?.forEach { runCatching { it.delete() } }
    }

    private fun startBackgroundThread() {
        if (backgroundStarted) return
        backgroundStarted = true
        Thread(
            {
                var sinceSample = 0L
                while (true) {
                    runCatching { Thread.sleep(FLUSH_EVERY_MS) }
                    if (levelFlow.value == AppLogLevel.OFF) continue
                    flush()
                    sinceSample += FLUSH_EVERY_MS
                    if (sinceSample >= SAMPLE_EVERY_MS) {
                        sinceSample = 0L
                        runCatching { sampleResources() }
                    }
                }
            },
            "mtsw-log-flush",
        ).apply {
            isDaemon = true
            start()
        }
    }

    /** One line of the app's resource state, written every [SAMPLE_EVERY_MS] while logging is on. */
    private fun sampleResources() {
        val rt = Runtime.getRuntime()
        val used = (rt.totalMemory() - rt.freeMemory()) / BYTES_IN_MB
        Logger.d(tag = "Resources") {
            "heap ${used}/${rt.maxMemory() / BYTES_IN_MB} MB, threads ${Thread.getAllStackTraces().size}, " +
                "cpus ${rt.availableProcessors()}, up ${(System.currentTimeMillis() - started) / 1000} s ${extraSample()}"
        }
    }

    private class CountingStream(private val inner: OutputStream) : OutputStream() {
        @Volatile var count = 0L

        override fun write(b: Int) {
            inner.write(b)
            count++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            inner.write(b, off, len)
            count += len
        }

        override fun flush() = inner.flush()

        override fun close() = inner.close()
    }

    @Volatile private var crashRecorderInstalled = false

    @Volatile private var backgroundStarted = false

    @Volatile private var shutdownHookInstalled = false

    private const val TAG = "MtSwLog"

    /** Column of the level letter in a line: `MM-dd HH:mm:ss.SSS ` is 19 characters long. */
    private const val LEVEL_COLUMN = 19
}
