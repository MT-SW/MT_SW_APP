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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.meshtastic.core.common.AppDataRoot
import org.meshtastic.core.common.background.SingleInstanceControl
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption
import kotlin.concurrent.thread

/**
 * Keeps the program to one copy. The first copy holds a lock on a file in the settings folder and listens on a local
 * port; a second copy that finds the lock taken tells the first one to show its window and ends. Any failure here lets
 * the program start as usual: a broken check must never stop it from opening.
 */
internal object SingleInstance {
    private const val TAG = "SingleInstance"
    private const val SHOW = "show"
    private const val CONNECT_TRIES = 20
    private const val CONNECT_WAIT_MS = 100L
    private const val TIMEOUT_MS = 1_000

    // Kept for as long as the program runs: closing the channel or the socket would give the lock up.
    private var channel: FileChannel? = null
    private var lock: FileLock? = null
    private var server: ServerSocket? = null

    private val shows = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when a second copy was started and asked the window of this one to come forward. */
    val showRequests: SharedFlow<Unit> = shows

    /** True when this copy may go on running; false when another copy is running and was asked to show itself. */
    fun claim(): Boolean {
        if (AppDataRoot.allowsSeveralInstances()) return true
        return runCatching {
            val dir = AppDataRoot.defaultDir.also { it.mkdirs() }
            val lockPath = File(dir, AppDataRoot.INSTANCE_LOCK).toPath()
            val opened = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            val taken = runCatching { opened.tryLock() }.getOrNull()
            if (taken == null) {
                opened.close()
                // Nobody answers: the other copy is still starting or is stuck, so this one goes on.
                !askRunningCopyToShow(File(dir, AppDataRoot.INSTANCE_PORT))
            } else {
                channel = opened
                lock = taken
                listen(File(dir, AppDataRoot.INSTANCE_PORT))
                true
            }
        }
            .onFailure { Logger.w(it) { "The one-copy check failed; the program starts anyway" } }
            .getOrDefault(true)
    }

    private fun listen(portFile: File) {
        val socket = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
        server = socket
        portFile.writeText(socket.localPort.toString())
        thread(name = "single-instance", isDaemon = true) {
            while (!socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                runCatching {
                    client.soTimeout = TIMEOUT_MS
                    client.use { c ->
                        if (c.getInputStream().bufferedReader().readLine() == SHOW) shows.tryEmit(Unit)
                    }
                }
            }
        }
    }

    /** True when the running copy took the request. */
    private fun askRunningCopyToShow(portFile: File): Boolean {
        repeat(CONNECT_TRIES) {
            val port = runCatching { portFile.readText().trim().toInt() }.getOrNull()
            if (port != null) {
                val sent = runCatching {
                    Socket(InetAddress.getLoopbackAddress(), port).use { socket ->
                        socket.soTimeout = TIMEOUT_MS
                        socket.getOutputStream().apply {
                            write("$SHOW\n".toByteArray())
                            flush()
                        }
                    }
                }
                    .isSuccess
                if (sent) return true
            }
            Thread.sleep(CONNECT_WAIT_MS)
        }
        return false
    }
}

/** The settings switch for [SingleInstance]: off (one copy only) until the person allows a second copy. */
internal object DesktopSingleInstanceControl : SingleInstanceControl {
    private val state = MutableStateFlow(AppDataRoot.allowsSeveralInstances())
    override val allowSeveral: StateFlow<Boolean> = state

    override fun setAllowSeveral(on: Boolean) {
        AppDataRoot.setAllowSeveralInstances(on)
        state.value = AppDataRoot.allowsSeveralInstances()
    }
}
