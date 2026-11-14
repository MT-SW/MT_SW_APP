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
package org.meshtastic.core.data.manager

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.meshtastic.core.common.di.ServiceScope
import org.meshtastic.core.repository.MeshNotificationManager
import org.meshtastic.core.repository.SosAlarmCenter
import org.meshtastic.core.repository.SosAlarmSounds
import org.meshtastic.core.repository.SosAlert
import org.meshtastic.core.repository.SosMarker
import org.meshtastic.core.repository.SosPrefs

/**
 * Rings the alarm for an SOS that arrived: the alert is shown on the whole screen, the platform's looping sound starts
 * (on Android the notification rings by itself, again and again, until it is cancelled) and everything stops when the
 * person confirms it. A newer SOS replaces the one on screen without stopping the sound.
 */
@Single
class SosAlarmCenterImpl(
    private val prefs: SosPrefs,
    private val notifications: MeshNotificationManager,
    private val scope: ServiceScope,
) : SosAlarmCenter {
    private val mutableAlert = MutableStateFlow<SosAlert?>(null)
    override val alert: StateFlow<SosAlert?> = mutableAlert.asStateFlow()

    override fun raise(alert: SosAlert) {
        // A test always rings, so the person can hear what the alarm is like; a real one follows the switch.
        if (!alert.test && !prefs.receiveAlarm.value) return
        val shown = alert.copy(text = SosMarker.clean(alert.text))
        val wasRinging = mutableAlert.value != null
        mutableAlert.value = shown
        Logger.i { "SOS alarm: raised from ${shown.senderName} test=${shown.test}" }
        if (!wasRinging) runCatching { SosAlarmSounds.current?.start() }
        scope.launch { notifications.showSosAlarmNotification(shown.contactKey, shown.senderName, shown.text) }
    }

    override fun acknowledge() {
        if (mutableAlert.value == null) return
        mutableAlert.value = null
        runCatching { SosAlarmSounds.current?.stop() }
        notifications.cancelSosAlarmNotification()
        Logger.i { "SOS alarm: confirmed" }
    }
}
