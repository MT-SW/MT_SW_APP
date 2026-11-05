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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * How much the app writes to its log files. [OFF] is the default; nothing is written to disk then. The settings switch
 * uses only [OFF] and [FULL]; the other levels stay for logs saved by earlier versions.
 */
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

    /** A short, readable place where the logs are kept (a folder path), or null when it is not worth showing. */
    val folderHint: String?
        get() = null

    /** True where [openFolder] can work (the desktop). */
    val canOpenFolder: Boolean
        get() = false

    /** Opens the folder holding the logs, on platforms that can; false when this is not possible. */
    fun openFolder(): Boolean

    /**
     * The automatic log of the last 24 hours, on by default: even with the full logging switch off the app keeps the
     * basic log of the last day on the device, so a problem can be reported with its log at once.
     */
    val recent: StateFlow<Boolean>
        get() = MutableStateFlow(false)

    fun setRecent(on: Boolean) {}

    /** True where a report package can be made ([buildReport]). */
    val canMakeReport: Boolean
        get() = false

    /** True where the user can pick a folder that gets a fresh report zip automatically. */
    val canChooseFolder: Boolean
        get() = false

    /** Name of the chosen folder, or null when none is chosen. */
    val chosenFolderName: StateFlow<String?>
        get() = MutableStateFlow(null)

    /** Opens the folder picker. */
    fun chooseFolder() {}

    /** Forgets the chosen folder. */
    fun clearChosenFolder() {}

    /**
     * True where [shareReport] hands the file itself to the chosen app (a phone); on a computer the person attaches it.
     */
    val sharesFileToApp: Boolean
        get() = false

    /**
     * Builds a report zip: the logs of the last 24 hours when [withLogs], the engine's own logs, the person's [note]
     * and the [extras] (name to content, for example a hardware report). Returns where the file is, or null when it
     * failed.
     */
    suspend fun buildReport(
        note: String,
        withLogs: Boolean,
        extras: List<Pair<String, ByteArray>> = emptyList(),
    ): ReportFile? = null

    /** Hands [report] and [text] to the app of [target]; false when this platform cannot. */
    fun shareReport(report: ReportFile, target: ReportTarget, text: String): Boolean = false
}

/** A finished report package: its [path] on the device, the place to show to the person, and its size. */
data class ReportFile(val path: String, val shownPath: String, val sizeKb: Long)

/** The three places where reports are taken; [packageName] is the phone app that receives the file. */
enum class ReportTarget(val packageName: String) {
    DISCORD("com.discord"),
    WHATSAPP("com.whatsapp"),
    SIGNAL("org.thoughtcrime.securesms"),
}

/** Holder of the platform's [AppLogControl]; null until the platform code installs it. */
object AppLogControls {
    var current: AppLogControl? = null
}
