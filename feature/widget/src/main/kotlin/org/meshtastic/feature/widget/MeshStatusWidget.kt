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
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.disconnected
import org.meshtastic.core.resources.widget_label_favorite
import org.meshtastic.core.resources.widget_label_network
import org.meshtastic.core.resources.widget_label_other
import org.meshtastic.core.resources.widget_no_nodes
import org.meshtastic.core.service.stats.MeshStatsNodeCard
import org.meshtastic.core.service.stats.MeshStatsSnapshot

private const val LINE_HEIGHT_DP = 15
private const val HEADER_DP = 30
private const val TWO_COLUMNS_FROM_DP = 220

/** How long one node is shown before the next one (favorites and the network are shown in turn). */
private const val ROTATION_MS = 300_000L
private const val SECOND_AREA_FROM_DP = 200
private const val THIRD_AREA_FROM_DP = 330
private const val OWN_SHARE_OF_THREE = 0.4f
private const val NODE_SHARE_OF_THREE = 0.3f
private const val OWN_SHARE_OF_TWO = 0.55f
private const val NODE_SHARE_OF_TWO = 0.45f

/**
 * Widget "Statystyki MT_SW": the same lines as the permanent notification in one or two columns. Three sizes, like on
 * iPhone: 4x2 shows what is chosen in the settings (our node, or favorites, or the whole network in turn, our node
 * first); 4x4 has our node on top and below it favorites or the whole network in turn; 4x6 has our node, favorites and,
 * under them, the rest of the network (without favorites and without our node).
 */
class MeshStatusWidget :
    GlanceAppWidget(),
    KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Exact

    private val stateProvider: MeshStatusWidgetStateProvider by inject()
    private val uiPrefs: UiPrefs by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val snapshot by stateProvider.state.collectAsState()
            val small by uiPrefs.androidWidgetSmall.collectAsState()
            val second by uiPrefs.androidWidgetSecond.collectAsState()
            Content(snapshot, small, second)
        }
    }

    @Composable
    private fun Content(snapshot: MeshStatsSnapshot?, small: Int, second: Int) {
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
                    val step = (System.currentTimeMillis() / ROTATION_MS).toInt()
                    val width = size.width.value
                    val height = size.height.value
                    when {
                        height >= THIRD_AREA_FROM_DP ->
                            Column(modifier = GlanceModifier.fillMaxSize()) {
                                OwnSection(snapshot, width, height * OWN_SHARE_OF_THREE)
                                NodeSection(
                                    NodeKind.Favorite,
                                    snapshot?.favorites.orEmpty(),
                                    step,
                                    width,
                                    height * NODE_SHARE_OF_THREE,
                                )
                                NodeSection(
                                    NodeKind.Other,
                                    snapshot?.others.orEmpty(),
                                    step,
                                    width,
                                    height * NODE_SHARE_OF_THREE,
                                )
                            }

                        height >= SECOND_AREA_FROM_DP ->
                            Column(modifier = GlanceModifier.fillMaxSize()) {
                                OwnSection(snapshot, width, height * OWN_SHARE_OF_TWO)
                                if (second == 0) {
                                    NodeSection(
                                        NodeKind.Favorite,
                                        snapshot?.favorites.orEmpty(),
                                        step,
                                        width,
                                        height * NODE_SHARE_OF_TWO,
                                    )
                                } else {
                                    NodeSection(
                                        NodeKind.Network,
                                        snapshot?.network.orEmpty(),
                                        step,
                                        width,
                                        height * NODE_SHARE_OF_TWO,
                                    )
                                }
                            }

                        else -> SmallContent(snapshot, small, step, width, height)
                    }
                }
            }
        }
    }
}

/** The 4x2 widget: our node, or our node followed by the favorites / the whole network, one after another. */
@Composable
private fun SmallContent(snapshot: MeshStatsSnapshot?, small: Int, step: Int, widthDp: Float, heightDp: Float) {
    val cards =
        when (small) {
            1 -> snapshot?.favorites.orEmpty()
            2 -> snapshot?.network.orEmpty()
            else -> emptyList()
        }
    val position = step % (cards.size + 1)
    if (position == 0) {
        OwnSection(snapshot, widthDp, heightDp)
    } else {
        NodeSection(if (small == 1) NodeKind.Favorite else NodeKind.Network, cards, position - 1, widthDp, heightDp)
    }
}

private enum class NodeKind {
    Favorite,
    Network,
    Other,
}

/** Our node: the mark, the name, the chip and the statistics in columns. */
@Composable
private fun OwnSection(snapshot: MeshStatsSnapshot?, widthDp: Float, heightDp: Float) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WidgetMark(16.dp)
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = snapshot?.title ?: "MT_SW",
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (snapshot != null) NodeChip(snapshot.shortName, snapshot.color, snapshot.textColor)
        }
        Spacer(GlanceModifier.width(2.dp))
        if (snapshot == null || !snapshot.connected || snapshot.lines.isEmpty()) {
            Text(
                text = stringResource(Res.string.disconnected),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
        } else {
            StatsColumns(snapshot.entries(), widthDp, heightDp)
        }
    }
}

/** One node of a list (a favorite, or any node of the network) shown for a while, the next one after it. */
@Composable
private fun NodeSection(kind: NodeKind, cards: List<MeshStatsNodeCard>, step: Int, widthDp: Float, heightDp: Float) {
    val label =
        stringResource(
            when (kind) {
                NodeKind.Favorite -> Res.string.widget_label_favorite
                NodeKind.Network -> Res.string.widget_label_network
                NodeKind.Other -> Res.string.widget_label_other
            },
        )
    Column(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            text = label.uppercase(),
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold),
        )
        if (cards.isEmpty()) {
            Text(
                text = stringResource(Res.string.widget_no_nodes),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
        } else {
            val index = step % cards.size
            val card = cards[index]
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = card.title,
                    maxLines = 1,
                    style =
                    TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.defaultWeight(),
                )
                NodeChip(card.shortName, card.color, card.textColor)
                if (cards.size > 1) {
                    Text(
                        text = " ${index + 1}/${cards.size}",
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp),
                    )
                }
            }
            StatsColumns(card.lines.map { WidgetEntry(it, header = false) }, widthDp, heightDp - NODE_HEADER_DP)
        }
    }
}

private const val NODE_HEADER_DP = 18

/** The readings of the widget in one or two columns, as many as fit in [widthDp] x [heightDp]. */
@Composable
private fun StatsColumns(all: List<WidgetEntry>, widthDp: Float, heightDp: Float) {
    val columns = if (widthDp >= TWO_COLUMNS_FROM_DP) 2 else 1
    val rows = ((heightDp - HEADER_DP) / LINE_HEIGHT_DP).toInt().coerceAtLeast(1)
    // Our node's sections have their column fixed (network and radio left, the rest right); other lists split evenly.
    val parts =
        if (columns == 2 && all.any { it.column == 1 }) {
            listOf(all.filter { it.column == 0 }.take(rows), all.filter { it.column == 1 }.take(rows))
        } else {
            all.take(rows * columns).splitColumns(columns)
        }
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        parts.forEachIndexed { index, part ->
            Column(modifier = GlanceModifier.defaultWeight().padding(end = if (index == 0) 4.dp else 0.dp)) {
                part.forEach { entry ->
                    Text(
                        text = entry.text,
                        maxLines = 1,
                        style =
                        TextStyle(
                            color =
                            if (entry.header) {
                                GlanceTheme.colors.primary
                            } else {
                                GlanceTheme.colors.onSurface
                            },
                            fontSize = 11.sp,
                            fontWeight = if (entry.header) FontWeight.Bold else FontWeight.Normal,
                        ),
                    )
                }
            }
        }
    }
}

/** One row of the widget: a group heading or a reading. */
private data class WidgetEntry(val text: String, val header: Boolean, val column: Int = 0)

/** Headings (when the snapshot has titled groups) followed by their readings, else just the flat lines. */
private fun MeshStatsSnapshot.entries(): List<WidgetEntry> = if (groups.any { it.title.isNotEmpty() }) {
    groups.flatMap { group ->
        buildList {
            if (group.title.isNotEmpty()) add(WidgetEntry(group.title, header = true, column = group.column))
            group.lines.forEach { add(WidgetEntry(it, header = false, column = group.column)) }
        }
    }
} else {
    lines.map { WidgetEntry(it, header = false) }
}

/** Splits into [columns] even columns; a heading never stays alone at the bottom of a column. */
private fun List<WidgetEntry>.splitColumns(columns: Int): List<List<WidgetEntry>> {
    if (columns <= 1 || size < 2) return listOf(this)
    var cut = (size + columns - 1) / columns
    if (cut > 1 && this[cut - 1].header) cut -= 1
    return listOf(take(cut), drop(cut))
}
