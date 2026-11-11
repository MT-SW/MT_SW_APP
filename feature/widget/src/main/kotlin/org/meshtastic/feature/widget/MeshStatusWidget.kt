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
import androidx.compose.ui.graphics.Color
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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
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

private const val WIDGET_CORNER_DP = 16
private const val PADDING_DP = 8
private const val TWO_COLUMNS_FROM_DP = 220

/** How long one node is shown before the next one (favorites and the network are shown in turn). */
private const val ROTATION_MS = 60_000L

private const val TEXT_SP = 11f

/** A line of text is about this many times its size high (the system adds spacing above and below the letters). */
private const val LINE_FACTOR = 1.4f

/** An average letter is about this part of its size wide (digits and capitals are wider, small letters narrower). */
private const val CHAR_FACTOR = 0.58f
private const val OWN_HEADER_DP = 28f
private const val NODE_HEADER_DP = 35f
private const val COLUMN_GAP_DP = 4f
private const val MIN_NODE_LINES = 2

/** Room between two lines of one group, and before the next group — the iPhone widget's spacing. */
private const val LINE_GAP_DP = 2f
private const val GROUP_GAP_DP = 7f

/** Room above and below the line between two sections of the widget. */
private const val SECTION_GAP_DP = 6f
private const val DIVIDER_DP = 1f
private const val SECTION_BREAK_DP = 2 * SECTION_GAP_DP + DIVIDER_DP

/** The gold of the MT_SW theme: group headings, as on the iPhone widget. */
private val GOLD = Color(0xFFF5C738)

/**
 * Widget "Statystyki MT_SW": the same lines as the permanent notification in one or two columns. What is shown follows
 * the room there is: our node first, then (when the lines of all of them fit) the favorites and the rest of the
 * network, or else the second area chosen in the settings; the smallest size shows what is chosen in the settings
 * (our node, or favorites, or the whole network in turn, our node first). A line too long for a column is cut into
 * several rows, so nothing ends in "…".
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
                Column(
                    modifier =
                    GlanceModifier.fillMaxSize()
                        // A plain surface behind the text: on a busy wallpaper it was unreadable.
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(WIDGET_CORNER_DP.dp)
                        .padding(PADDING_DP.dp)
                        .clickable(actionStartActivity(open)),
                ) {
                    val step = (System.currentTimeMillis() / ROTATION_MS).toInt()
                    val metrics = Metrics(context.resources.configuration.fontScale, size.width.value)
                    val room = size.height.value - 2 * PADDING_DP
                    val own = ownNeedDp(snapshot, metrics)
                    val favorites = snapshot?.favorites.orEmpty()
                    val others = snapshot?.others.orEmpty()
                    val favoritesNeed = nodeNeedDp(favorites, step, metrics)
                    val othersNeed = nodeNeedDp(others, step, metrics)
                    val smallest = NODE_HEADER_DP * metrics.fontScale + MIN_NODE_LINES * metrics.lineDp
                    val three = own + favoritesNeed + othersNeed + 2 * SECTION_BREAK_DP
                    when {
                        room >= three -> {
                            // What is left over is shared evenly, so the sections and the lines between them
                            // are spread over the whole widget instead of leaving an empty strip at the bottom.
                            val extra = (room - three) / 3
                            Column(modifier = GlanceModifier.fillMaxSize()) {
                                OwnSection(snapshot, metrics, own + extra)
                                SectionDivider()
                                NodeSection(NodeKind.Favorite, favorites, step, metrics, favoritesNeed + extra)
                                SectionDivider()
                                NodeSection(NodeKind.Other, others, step, metrics, othersNeed + extra)
                            }
                        }

                        room >= own + smallest + SECTION_BREAK_DP ->
                            Column(modifier = GlanceModifier.fillMaxSize()) {
                                val ownHeight = minOf(own, room - smallest - SECTION_BREAK_DP)
                                OwnSection(snapshot, metrics, ownHeight)
                                SectionDivider()
                                val rest = room - ownHeight - SECTION_BREAK_DP
                                if (second == 0) {
                                    NodeSection(NodeKind.Favorite, favorites, step, metrics, rest)
                                } else {
                                    NodeSection(NodeKind.Network, snapshot?.network.orEmpty(), step, metrics, rest)
                                }
                            }

                        else -> SmallContent(snapshot, small, step, metrics, room)
                    }
                }
            }
        }
    }
}

/** The sizes the layout is worked out from: the width of the widget and how big the user's text is. */
private class Metrics(val fontScale: Float, widthDp: Float) {
    val lineDp: Float = TEXT_SP * LINE_FACTOR * fontScale + LINE_GAP_DP
    val columns: Int = if (widthDp >= TWO_COLUMNS_FROM_DP) 2 else 1

    /** How many letters of the readings fit in one column. */
    val charsPerLine: Int =
        (((widthDp - 2 * PADDING_DP) / columns - COLUMN_GAP_DP) / (TEXT_SP * CHAR_FACTOR * fontScale))
            .toInt()
            .coerceAtLeast(1)
}

/** The 4x2 widget: our node, or our node followed by the favorites / the whole network, one after another. */
@Composable
private fun SmallContent(snapshot: MeshStatsSnapshot?, small: Int, step: Int, metrics: Metrics, heightDp: Float) {
    val cards =
        when (small) {
            1 -> snapshot?.favorites.orEmpty()
            2 -> snapshot?.network.orEmpty()
            else -> emptyList()
        }
    val position = step % (cards.size + 1)
    if (position == 0) {
        OwnSection(snapshot, metrics, heightDp)
    } else {
        NodeSection(if (small == 1) NodeKind.Favorite else NodeKind.Network, cards, position - 1, metrics, heightDp)
    }
}

private enum class NodeKind {
    Favorite,
    Network,
    Other,
}

/** The readings of our node in columns, or null while there is nothing to show. */
private fun ownParts(snapshot: MeshStatsSnapshot?, metrics: Metrics): List<List<WidgetEntry>>? =
    if (snapshot == null || !snapshot.connected || snapshot.lines.isEmpty()) {
        null
    } else {
        layoutColumns(snapshot.entries(), metrics)
    }

/** The height all the readings of our node need. */
private fun ownNeedDp(snapshot: MeshStatsSnapshot?, metrics: Metrics): Float {
    val body = ownParts(snapshot, metrics)?.maxOfOrNull { it.heightDp(metrics) } ?: metrics.lineDp
    return OWN_HEADER_DP * metrics.fontScale + body
}

/** The height all the readings of the node shown now (one of [cards], by [step]) need. */
private fun nodeNeedDp(cards: List<MeshStatsNodeCard>, step: Int, metrics: Metrics): Float {
    val body =
        if (cards.isEmpty()) {
            metrics.lineDp
        } else {
            val lines = cards[step % cards.size].lines.map { WidgetEntry(it, header = false) }
            layoutColumns(lines, metrics).maxOfOrNull { it.heightDp(metrics) } ?: metrics.lineDp
        }
    return NODE_HEADER_DP * metrics.fontScale + body
}

/** Our node: the mark, the name, the chip and the statistics in columns. */
@Composable
private fun OwnSection(snapshot: MeshStatsSnapshot?, metrics: Metrics, heightDp: Float) {
    Column(modifier = GlanceModifier.fillMaxWidth().height(heightDp.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WidgetMark(16.dp)
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = snapshot?.title ?: "MT_SW",
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (snapshot != null) NodeChip(snapshot.shortName, snapshot.color, snapshot.textColor)
        }
        Spacer(GlanceModifier.height(2.dp))
        val parts = ownParts(snapshot, metrics)
        if (parts == null) {
            Text(
                text = stringResource(Res.string.disconnected),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
        } else {
            StatsColumns(parts, heightDp - OWN_HEADER_DP * metrics.fontScale, metrics)
        }
    }
}

/** One node of a list (a favorite, or any node of the network) shown for a while, the next one after it. */
@Composable
private fun NodeSection(kind: NodeKind, cards: List<MeshStatsNodeCard>, step: Int, metrics: Metrics, heightDp: Float) {
    val label =
        stringResource(
            when (kind) {
                NodeKind.Favorite -> Res.string.widget_label_favorite
                NodeKind.Network -> Res.string.widget_label_network
                NodeKind.Other -> Res.string.widget_label_other
            },
        )
    Column(modifier = GlanceModifier.fillMaxWidth().height(heightDp.dp)) {
        Text(
            text = label.uppercase(),
            maxLines = 1,
            style = TextStyle(color = ColorProvider(GOLD), fontSize = 10.sp, fontWeight = FontWeight.Bold),
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
                    TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold),
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
            val parts = layoutColumns(card.lines.map { WidgetEntry(it, header = false) }, metrics)
            StatsColumns(parts, heightDp - NODE_HEADER_DP * metrics.fontScale, metrics)
        }
    }
}

/** The line between two sections of the widget, with room above and below it. */
@Composable
private fun SectionDivider() {
    Spacer(GlanceModifier.height(SECTION_GAP_DP.dp))
    Spacer(GlanceModifier.fillMaxWidth().height(DIVIDER_DP.dp).background(GlanceTheme.colors.outline))
    Spacer(GlanceModifier.height(SECTION_GAP_DP.dp))
}

/** A hair of room is allowed, so an exact fit is not lost to rounding. */
private const val ROUNDING_DP = 1f

/** The readings of the widget in its columns, as many as fit in [budgetDp]; groups are set apart by a gap. */
@Composable
private fun StatsColumns(parts: List<List<WidgetEntry>>, budgetDp: Float, metrics: Metrics) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        parts.forEachIndexed { index, part ->
            val gap = if (index == 0) COLUMN_GAP_DP.dp else 0.dp
            Column(modifier = GlanceModifier.defaultWeight().padding(end = gap)) {
                part.fitting(budgetDp, metrics).forEachIndexed { row, entry ->
                    if (entry.header && row > 0) Spacer(GlanceModifier.height(GROUP_GAP_DP.dp))
                    Text(
                        text = if (entry.header) entry.text.uppercase() else entry.text,
                        maxLines = entry.rows,
                        style =
                        TextStyle(
                            color = if (entry.header) ColorProvider(GOLD) else GlanceTheme.colors.onSurface,
                            fontSize = TEXT_SP.sp,
                            fontWeight = if (entry.header) FontWeight.Bold else FontWeight.Normal,
                        ),
                        modifier = GlanceModifier.padding(top = LINE_GAP_DP.dp),
                    )
                }
            }
        }
    }
}

/**
 * One row of the widget: a group heading or a reading. [rows] is how many text lines it takes (a reading still too long
 * for the column after being cut is allowed two).
 */
private data class WidgetEntry(val text: String, val header: Boolean, val column: Int = 0, val rows: Int = 1)

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

/**
 * A reading longer than a column is cut at its separators (" | ", and before a bracket) into several rows, which are
 * joined again as far as they fit: "Ruch: TX 118 / RX 24908 (D: 170)" becomes two rows instead of ending in "…".
 */
private fun WidgetEntry.fitTo(chars: Int): List<WidgetEntry> {
    if (header || text.length <= chars) return listOf(this)
    val pieces =
        text.split(" | ").flatMap { piece ->
            if (piece.length <= chars) {
                listOf(piece)
            } else {
                piece.split(" (").mapIndexed { i, part -> if (i == 0) part else "($part" }
            }
        }
    val joined = ArrayList<String>()
    for (piece in pieces) {
        val last = joined.lastOrNull()
        if (last != null && !piece.startsWith("(") && last.length + SEPARATOR.length + piece.length <= chars) {
            joined[joined.lastIndex] = last + SEPARATOR + piece
        } else {
            joined.add(piece)
        }
    }
    return joined.map { copy(text = it, rows = if (it.length > chars) 2 else 1) }
}

private const val SEPARATOR = " | "

/** The entries in their columns, every one cut to fit the width. */
private fun layoutColumns(all: List<WidgetEntry>, metrics: Metrics): List<List<WidgetEntry>> {
    val fitted = all.flatMap { it.fitTo(metrics.charsPerLine) }
    // Our node's sections have their column fixed (network and radio left, the rest right); other lists split evenly.
    return if (metrics.columns == 2 && fitted.any { it.column == 1 }) {
        listOf(0, 1).map { column -> fitted.filter { it.column == column } }
    } else {
        fitted.splitColumns(metrics.columns)
    }
}

/** The height of a column of entries: its text lines, and the gap before every group but the first. */
private fun List<WidgetEntry>.heightDp(metrics: Metrics): Float {
    var total = 0f
    forEachIndexed { i, entry ->
        total += entry.rows * metrics.lineDp + if (entry.header && i > 0) GROUP_GAP_DP else 0f
    }
    return total
}

/** The first entries of a column that fit in [budgetDp]; a heading is never left alone at the bottom. */
private fun List<WidgetEntry>.fitting(budgetDp: Float, metrics: Metrics): List<WidgetEntry> {
    var used = 0f
    val taken = ArrayList<WidgetEntry>()
    for ((i, entry) in withIndex()) {
        used += entry.rows * metrics.lineDp + if (entry.header && i > 0) GROUP_GAP_DP else 0f
        if (used > budgetDp + ROUNDING_DP) break
        taken.add(entry)
    }
    return if (taken.lastOrNull()?.header == true) taken.dropLast(1) else taken
}

/** Splits into [columns] even columns (by text lines); a heading never stays alone at the bottom of a column. */
private fun List<WidgetEntry>.splitColumns(columns: Int): List<List<WidgetEntry>> {
    if (columns <= 1 || size < 2) return listOf(this)
    val total = sumOf { it.rows }
    val half = (total + columns - 1) / columns
    var used = 0
    var cut = indexOfFirst { entry ->
        used += entry.rows
        used > half
    }
    if (cut < 0) cut = size
    if (cut > 1 && this[cut - 1].header) cut -= 1
    return listOf(take(cut), drop(cut))
}
