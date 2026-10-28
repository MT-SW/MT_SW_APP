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
package org.meshtastic.feature.messaging.translation

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.io.File
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * A model program (llama.cpp or whisper.cpp server) that the app started on this computer. It listens only on the
 * local address (127.0.0.1) on a free port, writes its messages to a log file, and is stopped when the model is
 * unloaded or the app ends.
 */
internal class ModelServerProcess private constructor(
    private val process: Process,
    val port: Int,
    private val tail: ArrayDeque<String>,
) {
    val isAlive: Boolean get() = process.isAlive

    val baseUrl: String get() = "http://127.0.0.1:$port"

    /** The last lines the program wrote (for the log when it fails). */
    fun logTail(): String = synchronized(tail) { tail.joinToString("\n") }

    /**
     * Waits until `GET [path]` answers 200 (with [anyAnswer]: until the program answers at all); fails when the program
     * ends or [timeoutMs] runs out.
     */
    suspend fun awaitReady(path: String, timeoutMs: Long, anyAnswer: Boolean = false) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            coroutineContext.ensureActive()
            if (!process.isAlive) error("The model program stopped (exit code ${process.exitValue()})")
            if (System.currentTimeMillis() > deadline) error("The model program did not become ready in ${timeoutMs / 1000} s")
            if (ping(path, anyAnswer)) return
            delay(POLL_MS)
        }
    }

    private fun ping(path: String, anyAnswer: Boolean): Boolean = try {
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = PING_TIMEOUT_MS
            connection.readTimeout = PING_TIMEOUT_MS
            val code = connection.responseCode
            if (anyAnswer) code in 100..599 else code == HttpURLConnection.HTTP_OK
        } finally {
            connection.disconnect()
        }
    } catch (e: java.io.IOException) {
        false
    }

    fun stop() {
        runCatching {
            process.destroy()
            if (!process.waitFor(STOP_WAIT_SECONDS, TimeUnit.SECONDS)) process.destroyForcibly().waitFor(STOP_WAIT_SECONDS, TimeUnit.SECONDS)
        }
        live.remove(process)
    }

    companion object {
        private const val POLL_MS = 300L
        private const val PING_TIMEOUT_MS = 1500
        private const val STOP_WAIT_SECONDS = 5L
        private const val TAIL_LINES = 120

        /** The process number, read in a way that works on every Java and Android version; null when unknown. */
        private fun pidOf(process: Process): Long? = runCatching {
            (Process::class.java.getMethod("pid").invoke(process) as Long)
        }.recoverCatching {
            val field = process.javaClass.getDeclaredField("pid")
            field.isAccessible = true
            (field.get(process) as Number).toLong()
        }.getOrNull()

        /** Names of the model programs the app starts; only these are ever stopped as leftovers. */
        private val PROGRAMS = listOf("llama-server", "whisper-server")

        /** Programs started by this run of the app, all stopped when the app ends (one hook for all of them). */
        private val live = java.util.concurrent.ConcurrentHashMap.newKeySet<Process>()

        private val hookAdded = java.util.concurrent.atomic.AtomicBoolean(false)

        /**
         * Stops a program left over from an earlier run of the app (the pid file names it): an app that was killed or
         * crashed cannot stop its programs, and each one keeps gigabytes of memory. It is stopped only when its command
         * line still names a model program, so a number reused by something else is never touched. Works on Android,
         * Linux, macOS and Windows.
         */
        fun stopLeftovers(pidFile: File) {
            runCatching {
                if (!pidFile.isFile) return
                val pid = pidFile.readText().trim().substringBefore(' ').toLongOrNull() ?: return
                pidFile.delete()
                val command = commandOf(pid) ?: return
                if (PROGRAMS.none { command.contains(it) }) return
                Logger.w(tag = "ModelServer") { "Stopping a model program left from an earlier run (process $pid)" }
                if (!destroyByHandle(pid)) {
                    ProcessBuilder("kill", "-9", pid.toString()).start().waitFor(STOP_WAIT_SECONDS, TimeUnit.SECONDS)
                }
            }
        }

        /** The command line of process [pid]: from /proc on Android and Linux, otherwise from the Java process API. */
        private fun commandOf(pid: Long): String? {
            val proc = File("/proc/$pid/cmdline")
            if (proc.isFile) return runCatching { proc.readText() }.getOrNull()
            return runCatching {
                handleOf(pid)?.let { handle ->
                    val info = Class.forName("java.lang.ProcessHandle").getMethod("info").invoke(handle)
                    val command = Class.forName("java.lang.ProcessHandle\$Info").getMethod("command").invoke(info) as java.util.Optional<*>
                    command.orElse(null)?.toString()
                }
            }.getOrNull()
        }

        /** Stops process [pid] through the Java process API (computers); false where that API does not exist (Android). */
        private fun destroyByHandle(pid: Long): Boolean {
            val handle = runCatching { handleOf(pid) }.getOrNull() ?: return false
            return runCatching {
                Class.forName("java.lang.ProcessHandle").getMethod("destroyForcibly").invoke(handle)
                true
            }.getOrDefault(false)
        }

        /** The Java process API's handle of [pid] (computers only; Android has no such API, then this throws). */
        private fun handleOf(pid: Long): Any? {
            val type = Class.forName("java.lang.ProcessHandle")
            val found = type.getMethod("of", Long::class.javaPrimitiveType).invoke(null, pid) as java.util.Optional<*>
            return found.orElse(null)
        }

        /** A free local port. */
        fun freePort(): Int = ServerSocket(0).use { it.localPort }

        /**
         * Starts [command] in [directory]. Every line the program writes goes to [logFile] (replaced at each start)
         * and to [onLine]. The libraries next to the program are found through the library path variables.
         */
        fun start(
            command: List<String>,
            directory: File,
            port: Int,
            logFile: File,
            pidFile: File,
            onLine: (String) -> Unit,
        ): ModelServerProcess {
            val builder = ProcessBuilder(command).directory(directory).redirectErrorStream(true)
            val env = builder.environment()
            val libs = directory.absolutePath
            env["LD_LIBRARY_PATH"] = listOfNotNull(libs, env["LD_LIBRARY_PATH"]).joinToString(File.pathSeparator)
            env["DYLD_LIBRARY_PATH"] = listOfNotNull(libs, env["DYLD_LIBRARY_PATH"]).joinToString(File.pathSeparator)
            logFile.parentFile?.mkdirs()
            val process = builder.start()
            pidOf(process)?.let { runCatching { pidFile.writeText("$it ${directory.name}") } }
            val tail = ArrayDeque<String>()
            val reader = Thread {
                runCatching {
                    logFile.bufferedWriter().use { out ->
                        process.inputStream.bufferedReader().forEachLine { line ->
                            out.appendLine(line)
                            out.flush()
                            synchronized(tail) {
                                tail.addLast(line)
                                if (tail.size > TAIL_LINES) tail.removeFirst()
                            }
                            runCatching { onLine(line) }
                        }
                    }
                }.onFailure { if (it !is CancellationException) Logger.d(tag = "ModelServer") { "Log reader ended: ${it.message}" } }
            }
            reader.name = "model-server-log"
            reader.isDaemon = true
            reader.start()
            val handle = ModelServerProcess(process, port, tail)
            // A program the app started must never outlive the app.
            live.add(process)
            if (hookAdded.compareAndSet(false, true)) {
                Runtime.getRuntime().addShutdownHook(Thread { live.forEach { runCatching { it.destroyForcibly() } } })
            }
            return handle
        }
    }
}
