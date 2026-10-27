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

import co.touchlab.kermit.Severity
import kotlinx.coroutines.flow.StateFlow

/** How much the app writes to its log files. [OFF] is the default; nothing is written to disk then. */
enum class AppLogLevel(val severity: Severity?, val key: String) {
    OFF(null, "off"),
    BASIC(Severity.Info, "basic"),
    DETAILED(Severity.Debug, "detailed"),
    FULL(Severity.Verbose, "full"),
    ;

    companion object {
        fun fromKey(key: String?): AppLogLevel = entries.firstOrNull { it.key == key?.trim() } ?: OFF
    }
}

/**
 * What the settings screen needs from the platform's log files. Set once at start-up by the platform code (Android or
 * desktop) in [AppLogControls.current]; the common settings code only sees this interface.
 */
interface AppLogControl {
    val level: StateFlow<AppLogLevel>

    fun setLevel(level: AppLogLevel)

    /**
     * Gathers every log part (and the crash records) into one zip the user can send, and returns a short, readable
     * place where it was saved, or null when nothing could be written.
     */
    suspend fun exportForSending(): String?

    /** True where [openFolder] can work (the desktop). */
    val canOpenFolder: Boolean get() = false

    /** Opens the folder holding the logs, on platforms that can; false when this is not possible. */
    fun openFolder(): Boolean
}

/** Holder of the platform's [AppLogControl]; null until the platform code installs it. */
object AppLogControls {
    var current: AppLogControl? = null
}
