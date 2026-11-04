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
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import org.meshtastic.core.database.iosDataDir

/**
 * Keeps the app's log in `ios-app.log` in the app's folder (the previous run in `ios-app.old.log`), including the
 * crash that ends the app, so a problem can be read after the fact.
 */
internal object IosFileLog : LogWriter() {
    private val file by lazy { (iosDataDir() + "/ios-app.log").toPath() }

    fun install() {
        runCatching {
            val old = (iosDataDir() + "/ios-app.old.log").toPath()
            if (FileSystem.SYSTEM.exists(file)) FileSystem.SYSTEM.atomicMove(file, old)
        }
        Logger.addLogWriter(this)
        @OptIn(ExperimentalNativeApi::class)
        setUnhandledExceptionHook { error -> append("CRASH", error.stackTraceToString()) }
    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        append("${severity.name.first()}/$tag", message + (throwable?.let { "\n" + it.stackTraceToString() } ?: ""))
    }

    private fun append(tag: String, text: String) {
        runCatching {
            val sink = FileSystem.SYSTEM.appendingSink(file).buffer()
            try {
                sink.writeUtf8("$tag: $text\n")
            } finally {
                sink.close()
            }
        }
    }
}
