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
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
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

private val SOS_RED = Color(0xFFD32F2F)
private val SOS_GOLD = Color(0xFFF5C839)

/**
 * Widget "SOS": a red plate with a gold border and big white letters, the same look as the SOS button in the app's top
 * bar. A tap only opens the SOS screen of the app (like the quick settings tile): the alarm itself is sent by holding
 * the button there, so a touch of the widget in a pocket cannot send a false alarm.
 */
class SosWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content() }
    }

    @Composable
    private fun Content() {
        val context = LocalContext.current
        val size = LocalSize.current
        val open =
            Intent(Intent.ACTION_VIEW, "$DEEP_LINK_BASE_URI/sos".toUri()).setPackage(context.packageName)
        val textSp =
            minOf(size.height.value * HEIGHT_PART, size.width.value * WIDTH_PART).coerceIn(MIN_TEXT_SP, MAX_TEXT_SP)
        Box(
            modifier =
            GlanceModifier.fillMaxSize()
                .background(ColorProvider(SOS_GOLD))
                .cornerRadius(CORNER_DP.dp)
                .padding(BORDER_DP.dp)
                .clickable(actionStartActivity(open)),
        ) {
            Box(
                modifier =
                GlanceModifier.fillMaxSize()
                    .background(ColorProvider(SOS_RED))
                    .cornerRadius((CORNER_DP - BORDER_DP).dp),
                contentAlignment = Alignment.Center,
            ) {
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
            }
        }
    }
}
