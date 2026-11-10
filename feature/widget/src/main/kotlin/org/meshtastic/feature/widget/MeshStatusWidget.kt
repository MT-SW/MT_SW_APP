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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.jetbrains.compose.resources.stringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.disconnected
import org.meshtastic.core.service.stats.MeshStatsSnapshot

private const val LINE_HEIGHT_DP = 15
private const val HEADER_DP = 30
private const val TWO_COLUMNS_FROM_DP = 220

/**
 * Widget "Statystyki MT_SW" (4x2): the same lines as the permanent notification (the whole mesh, or one tracked node
 * when chosen in the adaptive notifications), in one or two columns, as many as fit.
 */
class MeshStatusWidget :
    GlanceAppWidget(),
    KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Exact

    private val stateProvider: MeshStatusWidgetStateProvider by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val snapshot by stateProvider.state.collectAsState()
            Content(snapshot)
        }
    }

    @Composable
    private fun Content(snapshot: MeshStatsSnapshot?) {
        val context = LocalContext.current
        val size = LocalSize.current
        WidgetResourceEnvironment {
            GlanceTheme {
                val open =
                    Intent(
                        context.packageManager.getLaunchIntentForPackage(context.packageName)
                            ?: Intent().setClassName(context.packageName, "org.meshtastic.app.MainActivity"),
                    )
                Column(modifier = GlanceModifier.fillMaxSize().padding(8.dp).clickable(actionStartActivity(open))) {
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        WidgetMark(16.dp)
                        Spacer(GlanceModifier.width(6.dp))
                        Text(
                            text = snapshot?.title ?: "MT_SW",
                            maxLines = 1,
                            style =
                            TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        if (!snapshot?.chip.isNullOrEmpty()) {
                            Text(
                                text = snapshot?.chip.orEmpty(),
                                maxLines = 1,
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                            )
                        }
                    }
                    Spacer(GlanceModifier.width(2.dp))
                    if (snapshot == null || !snapshot.connected || snapshot.lines.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.disconnected),
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                        )
                    } else {
                        val columns = if (size.width.value >= TWO_COLUMNS_FROM_DP) 2 else 1
                        val rows = ((size.height.value - HEADER_DP) / LINE_HEIGHT_DP).toInt().coerceAtLeast(1)
                        val shown = snapshot.lines.take(rows * columns)
                        val perColumn = (shown.size + columns - 1) / columns
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            shown.chunked(perColumn).forEachIndexed { index, part ->
                                Column(
                                    modifier =
                                    GlanceModifier.defaultWeight().padding(end = if (index == 0) 4.dp else 0.dp),
                                ) {
                                    part.forEach { line ->
                                        Text(
                                            text = line,
                                            maxLines = 1,
                                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 11.sp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
