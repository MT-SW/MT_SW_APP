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
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import org.meshtastic.core.navigation.DEEP_LINK_BASE_URI

private const val CORNER_DP = 18
private const val BORDER_DP = 4
private const val MIN_TEXT_SP = 16f
private const val MAX_TEXT_SP = 56f
private const val HEIGHT_PART = 0.42f
private const val WIDTH_PART = 0.30f
private const val NOTE_PART = 0.45f

/** How long after a tap the next one still counts to the same run of quick taps. */
internal const val SOS_TAP_WINDOW_MS = 2_500L

/** How long the widget says "Sent" after the alarm went out. */
internal const val SOS_SENT_SHOW_MS = 6_000L

internal val SOS_TAP_COUNT_KEY = intPreferencesKey("sos_tap_count")
internal val SOS_LAST_TAP_KEY = longPreferencesKey("sos_last_tap")
internal val SOS_SENT_AT_KEY = longPreferencesKey("sos_sent_at")

private val SOS_RED = Color(0xFFD32F2F)
private val SOS_GOLD = Color(0xFFF5C839)

/**
 * The count of quick taps after a tap at [now]: the run goes on when the previous tap ([lastTap], with [previous] taps
 * counted) was no longer ago than [windowMs]; otherwise this tap starts a new run.
 */
internal fun nextSosTapCount(previous: Int, lastTap: Long, now: Long, windowMs: Long = SOS_TAP_WINDOW_MS): Int =
    if (previous > 0 && now - lastTap in 0..windowMs) previous + 1 else 1

/**
 * Widget "SOS": a red plate with a gold border and big white letters, the same look as the SOS button in the app's top
 * bar. It cannot be held (the launcher then starts editing it), so the alarm is sent by several quick taps in a row:
 * as many as are set in the SOS settings (never fewer than three, so a pocket cannot send it). While the taps are
 * counted the widget shows how many are done; once the alarm has gone out it says so, and a tap then opens the SOS
 * screen of the app to follow the answers.
 */
class SosWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content() }
    }

    @Composable
    private fun Content() {
        val context = LocalContext.current
        val size = LocalSize.current
        val state = currentState<Preferences>()
        val count = state[SOS_TAP_COUNT_KEY] ?: 0
        val sent = (state[SOS_SENT_AT_KEY] ?: 0L) > 0L
        val open = Intent(Intent.ACTION_VIEW, "$DEEP_LINK_BASE_URI/sos".toUri()).setPackage(context.packageName)
        val onClick: Action = if (sent) actionStartActivity(open) else actionRunCallback<SosTapAction>(actionParametersOf())
        val textSp =
            minOf(size.height.value * HEIGHT_PART, size.width.value * WIDTH_PART).coerceIn(MIN_TEXT_SP, MAX_TEXT_SP)
        val note =
            when {
                sent -> context.getString(R.string.widget_sos_sent)
                count > 0 -> "$count/${SosTapSettings.current()}"
                else -> null
            }
        Box(
            modifier =
            GlanceModifier.fillMaxSize()
                .background(ColorProvider(SOS_GOLD))
                .cornerRadius(CORNER_DP.dp)
                .padding(BORDER_DP.dp)
                .clickable(onClick),
        ) {
            Box(
                modifier =
                GlanceModifier.fillMaxSize()
                    .background(ColorProvider(SOS_RED))
                    .cornerRadius((CORNER_DP - BORDER_DP).dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        // The same in every language, so it needs no translation.
                        text = "SOS",
                        style =
                        TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = textSp.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    if (note != null) {
                        Text(
                            text = note,
                            style =
                            TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = (textSp * NOTE_PART).sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }
            }
        }
    }
}
