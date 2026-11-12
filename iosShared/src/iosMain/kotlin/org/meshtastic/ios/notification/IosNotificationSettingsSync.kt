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
package org.meshtastic.ios.notification

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.meshtastic.core.repository.NotificationPrefs
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDefaultsDidChangeNotification

/**
 * Keeps the notification switches in the iPhone settings (Settings → MT_SW, from `Settings.bundle`) and the app's own
 * notification settings the same: a switch changed in the iPhone settings is taken over by the app, and a change made
 * by the app itself (for example the automatic switch-off of new node notifications at an event) is shown there.
 */
class IosNotificationSettingsSync(private val prefs: NotificationPrefs) {
    private class Item(val key: String, val flow: StateFlow<Boolean>, val set: (Boolean) -> Unit)

    private val items =
        listOf(
            Item(KEY_MESSAGES, prefs.messagesEnabled, prefs::setMessagesEnabled),
            Item(KEY_NODE_EVENTS, prefs.nodeEventsEnabled, prefs::setNodeEventsEnabled),
            Item(KEY_LOW_BATTERY, prefs.lowBatteryEnabled, prefs::setLowBatteryEnabled),
            Item(KEY_REACTIONS, prefs.reactionsEnabled, prefs::setReactionsEnabled),
            Item(KEY_WAYPOINTS, prefs.newWaypointsEnabled, prefs::setNewWaypointsEnabled),
            Item(KEY_SOUND_WHILE_OPEN, prefs.soundWhileOpen, prefs::setSoundWhileOpen),
        )

    suspend fun run(): Unit = coroutineScope {
        val defaults = NSUserDefaults.standardUserDefaults
        // The saved settings load in a moment; this waits for them so the app's real values are not replaced by the
        // defaults they start with.
        delay(START_DELAY_MS)
        for (item in items) {
            if (defaults.objectForKey(item.key) != null) {
                // A value set in the iPhone settings wins.
                val fromSettings = defaults.boolForKey(item.key)
                if (fromSettings != item.flow.value) item.set(fromSettings)
            } else {
                defaults.setBool(item.flow.value, forKey = item.key)
            }
        }
        // App -> iPhone settings.
        for (item in items) {
            launch(start = CoroutineStart.UNDISPATCHED) {
                item.flow.collect { value ->
                    if (defaults.objectForKey(item.key) == null || defaults.boolForKey(item.key) != value) {
                        defaults.setBool(value, forKey = item.key)
                    }
                }
            }
        }
        // iPhone settings -> app.
        val observer =
            NSNotificationCenter.defaultCenter.addObserverForName(
                name = NSUserDefaultsDidChangeNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue,
            ) { _ ->
                for (item in items) {
                    if (defaults.objectForKey(item.key) == null) continue
                    val fromSettings = defaults.boolForKey(item.key)
                    if (fromSettings != item.flow.value) item.set(fromSettings)
                }
            }
        try {
            awaitCancellation()
        } finally {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }

    private companion object {
        const val START_DELAY_MS = 1_000L
        const val KEY_MESSAGES = "mtsw_notify_messages"
        const val KEY_NODE_EVENTS = "mtsw_notify_node_events"
        const val KEY_LOW_BATTERY = "mtsw_notify_low_battery"
        const val KEY_REACTIONS = "mtsw_notify_reactions"
        const val KEY_WAYPOINTS = "mtsw_notify_waypoints"
        const val KEY_SOUND_WHILE_OPEN = "mtsw_notify_sound_while_open"
    }
}
