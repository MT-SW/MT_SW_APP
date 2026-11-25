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
@file:Suppress("LoopWithTooManyJumpStatements", "MagicNumber", "MaxLineLength", "TooManyFunctions")

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
 * - Everything the app logs through Kermit goes to the files (as far down as the chosen level), so one level covers all
 *   modules.
 * - A log is a series of parts of about [PART_BYTES] each (`mt_sw_log_<session>_<nnn>.txt`). Every part opens with its
 *   own header, so it can be read alone.
 * - The oldest parts are deleted once all of them together pass [MAX_TOTAL_BYTES] or hold more than [FULL_WINDOW_MS]
 *   (seven days) of recorded time.
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

    /**
     * The automatic log of the last day (on by default): how far back it reaches, how big it may grow, how often it is
     * trimmed.
     */
    const val RECENT_WINDOW_MS: Long = 24L * 60 * 60 * 1000
    private const val RECENT_MAX_BYTES: Long = 100L * 1024 * 1024
    private const val PRUNE_EVERY_MS = 5L * 60 * 1000

    /** How much recorded time a log the person switched on by hand keeps (a pause in recording is not counted). */
    const val FULL_WINDOW_MS: Long = 7L * 24 * 60 * 60 * 1000
    private const val SCREEN_DIR = "screenshots"
    private const val MAX_SCREEN_FILES = 60
    private const val SCREEN_MIN_GAP_MS = 30_000L
    private const val PART_NUMBER_WIDTH = 3
    private const val BYTES_IN_MB = 1024 * 1024

    private val levelFlow = MutableStateFlow(AppLogLevel.OFF)
    val level: StateFlow<AppLogLevel>
        get() = levelFlow

    /**
     * The automatic log of the last 24 hours. It is always on and cannot be switched off (a report without a log cannot
     * be diagnosed): the app keeps a detailed rolling log of the last day on the device, whether or not full logging is
     * on. It stays on the device unless the person sends it.
     */
    private val recentFlow = MutableStateFlow(true)
    val recent: StateFlow<Boolean>
        get() = recentFlow

    /**
     * What is really written: the chosen level, or the detailed level while only the automatic log of the last day is
     * on (it is always on). Detailed means events, states, decisions and the reasons of failures, never message texts,
     * names, positions or keys.
     */
    private fun effectiveLevel(): AppLogLevel =
        levelFlow.value.let { if (it == AppLogLevel.OFF && recentFlow.value) AppLogLevel.DETAILED else it }

    @Volatile private var currentName: String? = null

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
                val min = effectiveLevel().severity ?: return false
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
        val conf = runCatching { File(logDir, CONF_NAME).readLines() }.getOrDefault(emptyList())
        levelFlow.value = AppLogLevel.fromKey(conf.firstOrNull())
        // The automatic log of the last day is always on; an old "recent=off" in the file is ignored.
        recentFlow.value = true
        installCrashRecorder()
        startBackgroundThread()
        installShutdownHook()
        runCatching { pruneRecent() }
        onLevelChanged(effectiveLevel())
    }

    /** Kermit's global minimum severity: the lower of [base] (what the build normally keeps) and the chosen level. */
    fun minSeverityFor(base: Severity, level: AppLogLevel = levelFlow.value): Severity {
        val chosen = level.severity ?: return base
        return if (chosen < base) chosen else base
    }

    fun setLevel(level: AppLogLevel) {
        val before = levelFlow.value
        levelFlow.value = level
        saveConf()
        if (effectiveLevel() == AppLogLevel.OFF) synchronized(lock) { closeCurrent() }
        onLevelChanged(effectiveLevel())
        if (before != level) Logger.i(tag = TAG) { "Log level: ${before.key} -> ${level.key}" }
    }

    /**
     * The automatic log of the last 24 hours cannot be switched off any more; kept so that older callers still build.
     */
    @Suppress("UNUSED_PARAMETER")
    fun setRecent(on: Boolean) {
        Logger.i(tag = TAG) { "The automatic log of the last day is always on (request to change it ignored)" }
    }

    private fun saveConf() {
        val folder = dir ?: return
        runCatching {
            File(folder, CONF_NAME).writeText(levelFlow.value.key + "\nrecent=on")
        }
    }

    /**
     * Set by each platform: what to save when the interface goes wrong (name to bytes: the size of the window and of
     * the screen, a picture of the window). Empty when nothing can be taken. Used only while the full log is on.
     */
    @Volatile var screenProvider: (() -> List<Pair<String, ByteArray>>)? = null

    @Volatile private var lastScreenAt = 0L

    /** The saved window pictures and sizes, oldest first. */
    fun screenFiles(): List<File> =
        dir?.let { File(it, SCREEN_DIR) }?.listFiles { f -> f.isFile }?.sortedBy { it.name }.orEmpty()

    /**
     * Saves the size of the window and a picture of it when the interface went wrong ([reason]). Only while the full
     * log (switched on by hand) is on, never for the automatic log of the last day, and at most once in
     * [SCREEN_MIN_GAP_MS]. The files follow the same seven recorded days as the log and are never more than
     * [MAX_SCREEN_FILES].
     */
    fun captureScreen(reason: String) {
        val folder = dir ?: return
        if (levelFlow.value == AppLogLevel.OFF) return
        val now = System.currentTimeMillis()
        val tooSoon =
            synchronized(lock) {
                (now - lastScreenAt < SCREEN_MIN_GAP_MS).also { if (!it) lastScreenAt = now }
            }
        val items = if (tooSoon) emptyList() else runCatching { screenProvider?.invoke() }.getOrNull().orEmpty()
        if (items.isNotEmpty()) {
            val target = File(folder, SCREEN_DIR).also { it.mkdirs() }
            val name = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(now))
            runCatching {
                File(target, "${name}_powod.txt").writeText("Reason: $reason\nTime: ${stamp()}\n${header()}\n")
                for ((suffix, bytes) in items) File(target, "${name}_$suffix").writeBytes(bytes)
                Logger.i(tag = TAG) { "Window picture saved ($reason)" }
                screenFiles().dropLast(MAX_SCREEN_FILES).forEach { runCatching { it.delete() } }
            }
        }
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
    fun writeZip(target: OutputStream) = writeReportZip(target, 0L)

    /**
     * Like [writeZip], but only with the parts written since [sinceMillis] (the last 24 hours for a report), plus
     * [extras] (name to text, put at the top of the zip: the user's note, the hardware report...) and [extraFiles]
     * (name in the zip to file: the engine's own logs).
     */
    fun writeReportZip(
        target: OutputStream,
        sinceMillis: Long,
        extras: List<Pair<String, ByteArray>> = emptyList(),
        extraFiles: List<Pair<String, File>> = emptyList(),
    ) {
        flush()
        ZipOutputStream(target).use { zip ->
            val toc = StringBuilder("MT_SW log package, made ${stamp()}\n").append(header()).append('\n')
            if (sinceMillis > 0L) toc.append("Only what was written since ${Date(sinceMillis)}\n")
            val files = (parts() + crashFiles()).filter { it.lastModified() >= sinceMillis }
            for ((name, bytes) in extras) toc.append("$name: ${bytes.size / 1024} KB\n")
            for ((name, file) in extraFiles) if (file.isFile) toc.append("$name: ${file.length() / 1024} KB\n")
            for (f in files) {
                val stats = scan(f)
                toc.append("${f.name}: ${f.length() / 1024} KB, ${stats.first} errors, ${stats.second} warnings\n")
            }
            zip.putNextEntry(ZipEntry("spis_tresci.txt"))
            zip.write(toc.toString().toByteArray())
            zip.closeEntry()
            for ((name, bytes) in extras) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
            for ((name, file) in extraFiles) {
                if (!file.isFile) continue
                zip.putNextEntry(ZipEntry(name))
                runCatching { file.inputStream().use { it.copyTo(zip) } }
                zip.closeEntry()
            }
            for (f in files) {
                zip.putNextEntry(ZipEntry((if (f.parentFile?.name == CRASH_DIR) "crash/" else "") + f.name))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            // Window pictures exist only from a full log, so the package of the automatic log never has them.
            if (levelFlow.value != AppLogLevel.OFF) {
                for (f in screenFiles().filter { it.lastModified() >= sinceMillis }) {
                    zip.putNextEntry(ZipEntry("zrzuty/" + f.name))
                    runCatching { f.inputStream().use { it.copyTo(zip) } }
                    zip.closeEntry()
                }
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
                            'E',
                            'A',
                            -> errors++

                            'W' -> warnings++
                        }
                    }
                }
            }
        }
        return errors to warnings
    }

    private fun append(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val line = buildString {
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
        currentName = name
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

    /**
     * While only the automatic log of the last day runs, the parts older than a day go, and the parts together stay
     * under a size limit. Parts of a log the person switched on by hand are never trimmed by age.
     */
    private fun pruneRecent() {
        if (levelFlow.value != AppLogLevel.OFF) {
            pruneFullByRecordedTime()
            return
        }
        val cutoff = System.currentTimeMillis() - RECENT_WINDOW_MS
        val all = parts()
        var total = all.sumOf { it.length() }
        for (f in all) {
            if (f.name == currentName) continue
            if (f.lastModified() < cutoff || total > RECENT_MAX_BYTES) {
                total -= f.length()
                runCatching { f.delete() }
            }
        }
    }

    /**
     * A log switched on by hand keeps the last [FULL_WINDOW_MS] of RECORDED time, not of calendar time: how long a part
     * took is the time between its start and its last line, so a pause of days between two sessions costs nothing and
     * recording simply goes on until seven days are filled. Then the oldest parts go as new lines come.
     */
    private fun pruneFullByRecordedTime() {
        val all = parts()
        if (all.isEmpty()) return
        val sessionFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        val spans = ArrayList<Long>(all.size)
        val starts = ArrayList<Long>(all.size)
        var previous: File? = null
        for (f in all) {
            val sessionOf = f.name.removePrefix("mt_sw_log_").substringBefore('_')
            val start =
                if (previous != null && previous.name.removePrefix("mt_sw_log_").substringBefore('_') == sessionOf) {
                    previous.lastModified()
                } else {
                    runCatching { sessionFormat.parse(sessionOf)?.time }.getOrNull() ?: f.lastModified()
                }
            starts += start
            spans += (f.lastModified() - start).coerceAtLeast(0L)
            previous = f
        }
        var kept = 0L
        var drop = false
        var oldestKeptStart = 0L
        for (i in all.indices.reversed()) {
            if (drop && all[i].name != currentName) runCatching { all[i].delete() }
            if (!drop) oldestKeptStart = starts[i]
            kept += spans[i]
            if (kept > FULL_WINDOW_MS) drop = true
        }
        // The window pictures go with the parts they belong to.
        if (drop) screenFiles().filter { it.lastModified() < oldestKeptStart }.forEach { runCatching { it.delete() } }
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
            Runtime.getRuntime()
                .addShutdownHook(
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
            runCatching { captureScreen("Uncaught exception: ${error.javaClass.simpleName}") }
            runCatching { writeCrashRecord(thread, error) }
            flush()
            previous?.uncaughtException(thread, error)
        }
    }

    private fun writeCrashRecord(thread: Thread, error: Throwable) {
        val folder = File(dir ?: return, CRASH_DIR).also { it.mkdirs() }
        val rt = Runtime.getRuntime()
        val text = buildString {
            append("Crash at ${stamp()}, ${(System.currentTimeMillis() - started) / 1000} s after start\n")
            append(header()).append('\n')
            append("Thread: ${thread.name}\n")
            append(
                "Memory: used ${(rt.totalMemory() - rt.freeMemory()) / BYTES_IN_MB} MB, max ${rt.maxMemory() / BYTES_IN_MB} MB\n",
            )
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
                var sincePrune = 0L
                while (true) {
                    runCatching { Thread.sleep(FLUSH_EVERY_MS) }
                    if (effectiveLevel() == AppLogLevel.OFF) continue
                    sincePrune += FLUSH_EVERY_MS
                    if (sincePrune >= PRUNE_EVERY_MS) {
                        sincePrune = 0L
                        runCatching { pruneRecent() }
                    }
                    flush()
                    sinceSample += FLUSH_EVERY_MS
                    if (sinceSample >= SAMPLE_EVERY_MS) {
                        sinceSample = 0L
                        runCatching { sampleResources() }
                    }
                }
            },
            "mtsw-log-flush",
        )
            .apply {
                isDaemon = true
                start()
            }
    }

    /** One line of the app's resource state, written every [SAMPLE_EVERY_MS] while logging is on. */
    private fun sampleResources() {
        val rt = Runtime.getRuntime()
        val used = (rt.totalMemory() - rt.freeMemory()) / BYTES_IN_MB
        Logger.d(tag = "Resources") {
            "heap $used/${rt.maxMemory() / BYTES_IN_MB} MB, threads ${Thread.getAllStackTraces().size}, " +
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
