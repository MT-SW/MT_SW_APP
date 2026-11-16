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
package org.meshtastic.feature.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.mp.KoinPlatform
import org.meshtastic.core.repository.SosConfig
import org.meshtastic.core.repository.SosController
import org.meshtastic.core.repository.SosPrefs

/** The number of quick taps the SOS widget needs, read from the SOS settings. */
internal object SosTapSettings {
    fun current(): Int = KoinPlatform.getKoin().get<SosPrefs>().widgetTaps.value.coerceAtLeast(SosConfig.WIDGET_MIN_TAPS)
}

/**
 * One tap on the SOS widget. Taps within [SOS_TAP_WINDOW_MS] of each other add up; when they reach the number set in
 * the SOS settings the alarm is sent with the settings of the SOS screen and the widget says "Sent" for a while. A run
 * that stops short is forgotten after the window, so single touches of a pocket never add up over time.
 */
class SosTapAction :
    ActionCallback,
    KoinComponent {

    private val sosController: SosController by inject()
    private val sosPrefs: SosPrefs by inject()

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val now = System.currentTimeMillis()
        val needed = sosPrefs.widgetTaps.value.coerceAtLeast(SosConfig.WIDGET_MIN_TAPS)
        var reached = false
        updateAppWidgetState(context, glanceId) { prefs ->
            val count = nextSosTapCount(prefs[SOS_TAP_COUNT_KEY] ?: 0, prefs[SOS_LAST_TAP_KEY] ?: 0L, now)
            if (count >= needed) {
                reached = true
                prefs[SOS_TAP_COUNT_KEY] = 0
                prefs[SOS_SENT_AT_KEY] = now
            } else {
                prefs[SOS_TAP_COUNT_KEY] = count
            }
            prefs[SOS_LAST_TAP_KEY] = now
        }
        if (reached) {
            Logger.i { "SosTapAction: $needed quick taps, sending the SOS" }
            sosController.start(sosPrefs.config.value)
        }
        SosWidget().update(context, glanceId)
        if (reached) forgetAfter(context, glanceId, SOS_SENT_SHOW_MS) { it[SOS_SENT_AT_KEY] == now }
        else forgetAfter(context, glanceId, SOS_TAP_WINDOW_MS) { it[SOS_LAST_TAP_KEY] == now }
    }

    /** After [waitMs], clears the counted taps and the "Sent" note unless a newer tap has come ([stillMine] false). */
    private suspend fun forgetAfter(
        context: Context,
        glanceId: GlanceId,
        waitMs: Long,
        stillMine: (androidx.datastore.preferences.core.Preferences) -> Boolean,
    ) {
        delay(waitMs)
        var cleared = false
        updateAppWidgetState(context, glanceId) { prefs ->
            if (stillMine(prefs)) {
                prefs[SOS_TAP_COUNT_KEY] = 0
                prefs.remove(SOS_SENT_AT_KEY)
                cleared = true
            }
        }
        if (cleared) SosWidget().update(context, glanceId)
    }
}
