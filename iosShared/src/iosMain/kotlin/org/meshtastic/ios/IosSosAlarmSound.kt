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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.meshtastic.core.repository.SosAlarmSound
import platform.AudioToolbox.AudioServicesPlaySystemSound

/**
 * The alarm sound of a received SOS while the app is open on an iPhone or iPad: the system alarm tone and the vibration
 * repeated every second and a half until [stop]. The system does not let an app sound an alarm in the background
 * without a special permission from Apple, so with the app closed only the notification sounds.
 */
internal object IosSosAlarmSound : SosAlarmSound {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    override fun start() {
        if (job?.isActive == true) return
        job =
            scope.launch {
                while (isActive) {
                    AudioServicesPlaySystemSound(ALARM_SOUND_ID)
                    AudioServicesPlaySystemSound(VIBRATE_ID)
                    delay(REPEAT_MS)
                }
            }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    private const val ALARM_SOUND_ID = 1005u
    private const val VIBRATE_ID = 4095u
    private const val REPEAT_MS = 1_500L
}
