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
package org.meshtastic.ios.widget

import co.touchlab.kermit.Logger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.resources.widget_updated
import org.meshtastic.core.service.stats.MeshStatsFeed
import org.meshtastic.core.service.stats.MeshStatsNodeCard
import org.meshtastic.core.service.stats.MeshStatsSnapshot
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToURL
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** The shared folder of the app and its widget (App Group). */
internal const val WIDGET_APP_GROUP = "group.pl.sw.mt"

/** The file in that folder the widget reads. */
private const val WIDGET_FILE = "widget-stats.json"

/** Name of the in-process notification the Swift side listens to, to reload the widget. */
private const val WIDGET_REFRESH = "MTSWWidgetRefresh"

private val MIN_PUBLISH_INTERVAL = 30.seconds

/** The widget is asked to reload at most this often: iOS limits the reloads and ignores them once they run out. */
private val MIN_RELOAD_INTERVAL = 5.minutes

/**
 * Publishes the statistics for the widget of iPhone/iPad: the same lines as the permanent notification on Android (the
 * shared [MeshStatsFeed]). Without the App Group (for example a build signed without it) nothing is written and the
 * widget shows a hint to open the app.
 */
class IosWidgetPublisher(private val feed: MeshStatsFeed) {
    private var lastReload = Clock.System.now() - MIN_RELOAD_INTERVAL

    /** Runs for as long as the caller lives: writes a new file whenever the statistics change (at most every 30 s). */
    suspend fun run() {
        feed.snapshots(withNodes = true).conflate().collect { snapshot ->
            runCatching {
                // The words on the widget follow the language of the app, not of the phone: the app writes them.
                val updated = getStringSuspend(Res.string.widget_updated) + " " + clockTime()
                write(json(snapshot, updated))
            }
                .onFailure { Logger.w(it) { "Could not publish the widget statistics" } }
            delay(MIN_PUBLISH_INTERVAL)
        }
    }

    private fun clockTime(): String = NSDateFormatter().apply { dateFormat = "HH:mm" }.stringFromDate(NSDate())

    private fun json(snapshot: MeshStatsSnapshot, updatedText: String): String =
        "{\"title\":${quote(snapshot.title)},\"chip\":${quote(snapshot.chip)},\"connected\":${snapshot.connected}," +
            "\"updated\":${Clock.System.now().epochSeconds},\"updatedText\":${quote(updatedText)}," +
            "\"groups\":[${snapshot.groups.joinToString(",") { group ->
                "{\"title\":${quote(group.title)},\"lines\":${lines(group.lines)}}"
            }}]," +
            "\"favorites\":${cards(
                snapshot.favorites,
            )},\"others\":${cards(snapshot.others)},\"network\":${cards(snapshot.network)}}"

    private fun lines(lines: List<String>): String = "[${lines.joinToString(",") { quote(it) }}]"

    private fun cards(cards: List<MeshStatsNodeCard>): String =
        "[${cards.joinToString(",") { "{\"title\":${quote(it.title)},\"lines\":${lines(it.lines)}}" }}]"

    private fun quote(text: String): String = buildString {
        append('"')
        for (c in text) {
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c == '\n' -> append("\\n")
                c.code < FIRST_PRINTABLE -> append(' ')
                else -> append(c)
            }
        }
        append('"')
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun write(json: String) {
        val folder = NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(WIDGET_APP_GROUP)
        if (folder == null) {
            Logger.w { "The widget folder (App Group) is not available" }
            return
        }
        val url: NSURL = folder.URLByAppendingPathComponent(WIDGET_FILE) ?: return
        NSString.create(string = json).writeToURL(url, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        val now = Clock.System.now()
        if (now - lastReload >= MIN_RELOAD_INTERVAL) {
            lastReload = now
            Logger.i { "Widżet: zapisano statystyki i poproszono system o odświeżenie" }
            NSNotificationCenter.defaultCenter.postNotificationName(WIDGET_REFRESH, null)
        }
    }
}

private const val FIRST_PRINTABLE = 0x20
