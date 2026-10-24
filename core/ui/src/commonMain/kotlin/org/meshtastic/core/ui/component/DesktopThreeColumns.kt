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
@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package org.meshtastic.core.ui.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/**
 * Which column a screen takes in the list / detail / extra layout. The library's own layout draws its width slider only
 * between two columns, so on a wide desktop window (three columns) [DesktopThreeColumnSceneStrategy] lays the columns out
 * itself and needs to know the role of each screen. The library's own metadata is added too, so everywhere else the
 * screens behave exactly as before.
 */
object MeshtasticPanes {
    private const val ROLE_KEY = "org.meshtastic.pane.role"
    private const val LIST = "list"
    private const val DETAIL = "detail"
    private const val EXTRA = "extra"

    fun list(): Map<String, Any> = ListDetailSceneStrategy.listPane() + (ROLE_KEY to LIST)

    fun detail(): Map<String, Any> = ListDetailSceneStrategy.detailPane() + (ROLE_KEY to DETAIL)

    fun extra(): Map<String, Any> = ListDetailSceneStrategy.extraPane() + (ROLE_KEY to EXTRA)

    internal fun roleOf(entry: NavEntry<*>): String? = entry.metadata[ROLE_KEY] as? String

    internal fun isList(entry: NavEntry<*>) = roleOf(entry) == LIST

    internal fun isDetail(entry: NavEntry<*>) = roleOf(entry) == DETAIL

    internal fun isExtra(entry: NavEntry<*>) = roleOf(entry) == EXTRA
}

/** The widths of the three columns, as shares of the window width; they live as long as the navigation host. */
internal class ThreeColumnWidths {
    var listShare by mutableFloatStateOf(DEFAULT_LIST_SHARE)
    var extraShare by mutableFloatStateOf(DEFAULT_EXTRA_SHARE)

    /** Moves the divider between the list and the detail by [deltaDp] (the detail gets the rest). */
    fun moveListDivider(deltaDp: Float, totalDp: Float) {
        if (totalDp <= 0f) return
        val max = 1f - extraShare - MIN_DETAIL_DP / totalDp
        listShare = (listShare + deltaDp / totalDp).coerceIn(MIN_PANE_DP / totalDp, maxOf(MIN_PANE_DP / totalDp, max))
    }

    /** Moves the divider between the detail and the extra column by [deltaDp]. */
    fun moveExtraDivider(deltaDp: Float, totalDp: Float) {
        if (totalDp <= 0f) return
        val max = 1f - listShare - MIN_DETAIL_DP / totalDp
        extraShare = (extraShare - deltaDp / totalDp).coerceIn(MIN_PANE_DP / totalDp, maxOf(MIN_PANE_DP / totalDp, max))
    }

    companion object {
        const val DEFAULT_LIST_SHARE = 0.24f
        const val DEFAULT_EXTRA_SHARE = 0.30f
        const val MIN_PANE_DP = 220f
        const val MIN_DETAIL_DP = 320f
    }
}

/**
 * Three columns (list, detail, extra) with two draggable dividers, for the desktop app on a window wide enough for three
 * columns. It handles a back stack that ends with an extra screen, with a detail and a list before it; everything else
 * (also when [allowed] is false) is left to the library's layout.
 */
internal class DesktopThreeColumnSceneStrategy(
    private val widths: ThreeColumnWidths,
    private val allowed: Boolean,
) : SceneStrategy<NavKey> {

    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        if (!allowed) return null
        val extra = entries.lastOrNull()?.takeIf { MeshtasticPanes.isExtra(it) } ?: return null
        val detail = entries.dropLast(1).lastOrNull { MeshtasticPanes.isDetail(it) } ?: return null
        val detailIndex = entries.indexOf(detail)
        val list = entries.take(detailIndex).lastOrNull { MeshtasticPanes.isList(it) } ?: return null
        return ThreeColumnScene(list, detail, extra, entries.dropLast(1), widths)
    }
}

private class ThreeColumnScene(
    private val list: NavEntry<NavKey>,
    private val detail: NavEntry<NavKey>,
    private val extra: NavEntry<NavKey>,
    override val previousEntries: List<NavEntry<NavKey>>,
    private val widths: ThreeColumnWidths,
) : Scene<NavKey> {
    // The same key as the library's own list-detail scene (its default scene key): when the window changes between two
    // and three columns the navigation host then swaps the content in place. A different key would show both scenes
    // for a moment, and the same screen cannot be shown twice ("Key ... was used multiple times").
    override val key: Any = Unit
    override val entries: List<NavEntry<NavKey>> = listOf(list, detail, extra)
    override val content: @Composable () -> Unit = { ThreeColumns(list, detail, extra, widths) }

    override fun equals(other: Any?): Boolean =
        other is ThreeColumnScene && other.entries == entries && other.previousEntries == previousEntries

    override fun hashCode(): Int = 31 * entries.hashCode() + previousEntries.hashCode()
}

@Composable
private fun ThreeColumns(
    list: NavEntry<NavKey>,
    detail: NavEntry<NavKey>,
    extra: NavEntry<NavKey>,
    widths: ThreeColumnWidths,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val total = maxWidth
        val density = LocalDensity.current
        val dividerWidth = DIVIDER_WIDTH
        // The columns may not leave the detail narrower than its minimum, whatever the window width.
        val usable = total - dividerWidth * 2
        val listWidth = (usable * widths.listShare).coerceAtLeast(ThreeColumnWidths.MIN_PANE_DP.dp)
        val extraWidth = (usable * widths.extraShare).coerceAtLeast(ThreeColumnWidths.MIN_PANE_DP.dp)
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(listWidth).fillMaxHeight()) { list.Content() }
            ColumnDivider { deltaPx -> widths.moveListDivider(deltaPx / density.density, usable.value) }
            Box(Modifier.weight(1f).fillMaxHeight()) { detail.Content() }
            ColumnDivider { deltaPx -> widths.moveExtraDivider(deltaPx / density.density, usable.value) }
            Box(Modifier.width(extraWidth).fillMaxHeight()) { extra.Content() }
        }
    }
}

private val DIVIDER_WIDTH: Dp = 24.dp

/** A draggable divider between two columns; [onDrag] gets the horizontal movement in pixels. */
@Composable
private fun ColumnDivider(onDrag: (Float) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
        Modifier.width(DIVIDER_WIDTH)
            .fillMaxHeight()
            .draggable(
                state = rememberDraggableState { delta -> onDrag(delta) },
                orientation = Orientation.Horizontal,
                interactionSource = interactionSource,
            ),
        contentAlignment = Alignment.Center,
    ) {
        VerticalDragHandle(interactionSource = interactionSource)
    }
}
