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
package org.meshtastic.feature.node.health

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.ui.component.FastScrollSidebar
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.icon.Info
import org.meshtastic.core.ui.icon.List
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.util.isDesktopPlatform

private const val FOLD_MILLIS = 250
private const val FOLD_DRAG_SLOP_PX = 1f

@Suppress("LongMethod")
@Composable
fun NetworkHealthScreen(
    viewModel: NetworkHealthViewModel,
    onNodeClick: (Int, NetworkHealthMetric) -> Unit,
    onNeighborInfoNodeClick: (Int) -> Unit,
    onSummaryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showInfo by remember { mutableStateOf(false) }
    val heroLocked by viewModel.heroLocked.collectAsStateWithLifecycle()
    val heroLockedNow by rememberUpdatedState(heroLocked)

    // One swipe up folds the whole summary card away (search, tabs, sorting and list stay); one swipe down at the top of
    // the list unfolds it. The card animates fully open or fully closed, it never stops half way.
    var heroHeightPx by remember { mutableFloatStateOf(0f) }
    var heroFoldedPx by remember { mutableFloatStateOf(0f) }
    var foldTargetPx by remember { mutableFloatStateOf(0f) }
    val foldAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val foldTo: (Boolean) -> Unit = { folded ->
        val target = if (folded) heroHeightPx else 0f
        if (foldTargetPx != target) {
            foldTargetPx = target
            scope.launch {
                foldAnim.snapTo(heroFoldedPx)
                foldAnim.animateTo(target, tween(FOLD_MILLIS)) { heroFoldedPx = value }
            }
        }
    }
    val foldToNow by rememberUpdatedState(foldTo)
    LaunchedEffect(heroLocked) {
        if (heroLocked) {
            foldTargetPx = 0f
            heroFoldedPx = 0f
            foldAnim.snapTo(0f)
        }
    }
    val foldConnection =
        remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (heroLockedNow || available.y >= 0f || heroFoldedPx >= heroHeightPx) return Offset.Zero
                    foldToNow(true)
                    return Offset(0f, available.y)
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (heroLockedNow || available.y <= 0f || heroFoldedPx <= 0f) return Offset.Zero
                    foldToNow(false)
                    return Offset(0f, available.y)
                }
            }
        }
    // Dragging the summary card (or the sorting row) itself folds and unfolds it too, not only dragging the list.
    val foldDrag =
        Modifier.draggable(
            orientation = Orientation.Vertical,
            state =
            rememberDraggableState { delta ->
                if (!heroLockedNow) {
                    if (delta < -FOLD_DRAG_SLOP_PX) foldToNow(true) else if (delta > FOLD_DRAG_SLOP_PX) foldToNow(false)
                }
            },
        )
    if (showInfo) {
        NetworkHealthInfoDialog(
            metric = uiState.activeMetric,
            heroLocked = heroLocked,
            onHeroLockedChange = viewModel::setHeroLocked,
            onDismiss = { showInfo = false },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            MainAppBar(
                title = "Zdrowie sieci",
                ourNode = null,
                showNodeChip = false,
                canNavigateUp = false,
                onNavigateUp = {},
                onClickChip = {},
                actions = {
                    IconButton(onClick = { showInfo = true }) {
                        Icon(
                            imageVector = MeshtasticIcons.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = onSummaryClick) {
                        Icon(
                            imageVector = MeshtasticIcons.List,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(contentPadding).nestedScroll(foldConnection)) {
            androidx.compose.material3.OutlinedTextField(
                value = uiState.searchText,
                onValueChange = viewModel::setSearchText,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Szukaj węzła…") },
                singleLine = true,
            )

            Box(
                modifier =
                foldDrag.fillMaxWidth().clipToBounds().layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    if (heroHeightPx != placeable.height.toFloat()) heroHeightPx = placeable.height.toFloat()
                    val folded = heroFoldedPx.coerceIn(0f, placeable.height.toFloat()).toInt()
                    layout(placeable.width, placeable.height - folded) { placeable.placeRelative(0, -folded) }
                },
            ) {
                NetworkHealthHero(
                    uiState = uiState,
                    onLevelClick = viewModel::toggleLevelFilter,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }

            ScrollableTabRow(selectedTabIndex = uiState.activeMetric.ordinal) {
                NetworkHealthMetric.entries.forEach { metric ->
                    Tab(
                        selected = uiState.activeMetric == metric,
                        onClick = { viewModel.setActiveMetric(metric) },
                        text = {
                            NetworkHealthTabLabel(
                                metric = metric,
                                worst = uiState.worstLevelByMetric[metric] ?: HealthLevel.UNKNOWN,
                            )
                        },
                    )
                }
            }

            NetworkHealthControls(
                uiState = uiState,
                onSortFieldChange = viewModel::setSortField,
                onToggleDirection = viewModel::toggleSortDirection,
                onToggleFavoritesFirst = viewModel::toggleFavoritesFirst,
                onToggleHideWithoutData = viewModel::toggleHideWithoutData,
                onToggleProblemsOnly = viewModel::toggleProblemsOnly,
                modifier = foldDrag.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(end = if (isDesktopPlatform) 20.dp else 0.dp),
                    state = listState,
                ) {
                    items(uiState.nodes, key = { it.num }) { node ->
                        NodeHealthRow(
                            node = node,
                            metric = uiState.activeMetric,
                            onClick = {
                                if (uiState.activeMetric == NetworkHealthMetric.NEIGHBORS) {
                                    onNeighborInfoNodeClick(node.num)
                                } else {
                                    onNodeClick(node.num, uiState.activeMetric)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
                FastScrollSidebar(
                    listState = listState,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp),
                )
            }
        }
    }
}

internal fun formatMetricValue(metric: NetworkHealthMetric, value: Float?): String {
    if (value == null) return "—"
    return when (metric) {
        NetworkHealthMetric.POWER -> MetricFormatter.percent(value.toInt())
        NetworkHealthMetric.SIGNAL -> "${NumberFormatter.format(value, 1)} dB"
        NetworkHealthMetric.ETHER -> MetricFormatter.percent(value, decimalPlaces = 2)
        NetworkHealthMetric.ENVIRONMENT -> MetricFormatter.temperature(value, isFahrenheit = false)
        NetworkHealthMetric.TRAFFIC -> "${value.toInt()}"
        NetworkHealthMetric.NEIGHBORS -> "${value.toInt()}"
        NetworkHealthMetric.RESOURCES -> "${value.toInt()}%"
    }
}
