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
import co.touchlab.kermit.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.meshtastic.core.common.util.nowMillis

/**
 * MT_SW: the app reads its own log as it is written and keeps a short summary of what went wrong since it started: the
 * warnings and errors grouped by what they say, how each device's link behaved, and how smooth the map was. Only the
 * texts the app itself logs are counted (they carry no names, positions or keys), and nothing leaves the device.
 */
object LogInsights {
    /** One kind of warning or error: [text] has its numbers replaced, so the same event counts together. */
    data class Group(val text: String, val severity: Severity, val count: Int, val lastMillis: Long)

    /** How the link to one device behaved ([id] is the address the log names it by). */
    data class Link(val id: String, val timeouts: Int, val closed: Int, val errors: Int, val lastMillis: Long)

    /** The map's frame measurements added together. */
    data class MapStats(val windows: Int = 0, val slowFrames: Int = 0, val longestMs: Int = 0)

    data class Summary(
        val warnings: Int = 0,
        val errors: Int = 0,
        val groups: Map<String, Group> = emptyMap(),
        val links: Map<String, Link> = emptyMap(),
        val map: MapStats = MapStats(),
        val sinceMillis: Long = nowMillis,
    )

    private val state = MutableStateFlow(Summary())
    val summary: StateFlow<Summary>
        get() = state

    /** Starts the count again from now. */
    fun clear() {
        state.value = Summary()
    }

    /** The writer to add to Kermit's writers. It looks at warnings, errors and the map's frame lines only. */
    val writer: LogWriter =
        object : LogWriter() {
            override fun isLoggable(tag: String, severity: Severity): Boolean = severity >= Severity.Info

            override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                record(severity, message)
            }
        }

    private val linkRegex = Regex("""^\w+\[([^\]]+)\]""")
    private val digits = Regex("""\d+""")
    private val mapRegex = Regex("""Mapa: klatek \d+, wolnych (\d+), najdłuższa przerwa (\d+) ms""")

    internal fun record(severity: Severity, message: String) {
        if (severity >= Severity.Warn) {
            recordProblem(severity, message)
        } else if (message.startsWith(MAP_PREFIX)) {
            mapRegex.find(message)?.let { found ->
                val slow = found.groupValues[1].toIntOrNull() ?: 0
                val longest = found.groupValues[2].toIntOrNull() ?: 0
                state.update { old ->
                    old.copy(
                        map =
                        MapStats(
                            windows = old.map.windows + 1,
                            slowFrames = old.map.slowFrames + slow,
                            longestMs = maxOf(old.map.longestMs, longest),
                        ),
                    )
                }
            }
        }
    }

    private fun recordProblem(severity: Severity, message: String) {
        val now = nowMillis
        val text = message.replace(digits, "N").take(MAX_TEXT)
        val link = linkRegex.find(message)?.groupValues?.get(1)
        state.update { old ->
            val before = old.groups[text]
            val group = Group(text, severity, (before?.count ?: 0) + 1, now)
            val groups = (old.groups + (text to group)).let { if (it.size > MAX_GROUPS) it.dropOldest() else it }
            val links =
                if (link == null) {
                    old.links
                } else {
                    val was = old.links[link] ?: Link(link, 0, 0, 0, now)
                    val next =
                        when {
                            message.contains("timed out") -> was.copy(timeouts = was.timeouts + 1)
                            message.contains("Closing after") -> was.copy(closed = was.closed + 1)
                            else -> was.copy(errors = was.errors + 1)
                        }
                    old.links + (link to next.copy(lastMillis = now))
                }
            old.copy(
                warnings = old.warnings + if (severity == Severity.Warn) 1 else 0,
                errors = old.errors + if (severity >= Severity.Error) 1 else 0,
                groups = groups,
                links = links,
            )
        }
    }

    private fun Map<String, Group>.dropOldest(): Map<String, Group> {
        val oldest = values.minByOrNull { it.lastMillis }?.text
        return if (oldest == null) this else this - oldest
    }

    private const val MAP_PREFIX = "Mapa: klatek"
    private const val MAX_TEXT = 140
    private const val MAX_GROUPS = 150
}
