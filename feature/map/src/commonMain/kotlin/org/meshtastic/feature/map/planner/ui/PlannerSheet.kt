/*
 * Copyright (c) 2025 MT_SW contributors
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
@file:OptIn(ExperimentalMaterial3Api::class)

package org.meshtastic.feature.map.planner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.planner_cancel
import org.meshtastic.core.resources.planner_close
import org.meshtastic.core.resources.planner_credits_button
import org.meshtastic.core.resources.planner_disclaimer
import org.meshtastic.core.resources.planner_pick_banner
import org.meshtastic.core.resources.planner_title_by
import org.meshtastic.core.resources.planner_title
import org.meshtastic.core.ui.icon.Close
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.feature.map.planner.state.PlannerNodeOption
import org.meshtastic.feature.map.planner.state.PlannerSide
import org.meshtastic.feature.map.planner.state.PlannerUiState
import org.meshtastic.feature.map.planner.state.PlannerViewModel

private const val NODE_WAIT_MS = 3_000L
private val CONTENT_MAX_WIDTH = 720.dp

/**
 * Bridge between the planner and the host map (supplied by each map flavour).
 *
 * The planner asks the user to tap a point on the host map by setting [pickingSide]; the host map shows
 * [PlannerMapPickBanner], and forwards the next tap to [deliverTap].
 */
@Stable
class PlannerMapBridge(
    /** Latitude / longitude of the current map centre, or null. */
    val mapCenter: () -> Pair<Double, Double>?,
    val moveTo: (lat: Double, lon: Double) -> Unit,
    val addGeoJsonLayer: (name: String, geoJson: String) -> Unit,
) {
    /** Non-null while the planner waits for a tap on the host map. Observed by the map (hint banner, tap capture). */
    var pickingSide: PlannerSide? by mutableStateOf(null)

    private var pickHandler: ((PlannerSide, Double, Double) -> Unit)? = null

    /** Called by the host map when the user taps the map while [pickingSide] != null. */
    fun deliverTap(lat: Double, lon: Double) {
        val side = pickingSide ?: return
        pickingSide = null
        pickHandler?.invoke(side, lat, lon)
    }

    /** Abandons a pending pick (host back press, banner cancel button). */
    fun cancelPick() {
        pickingSide = null
    }

    internal fun registerPickHandler(handler: ((PlannerSide, Double, Double) -> Unit)?) {
        pickHandler = handler
    }
}

/**
 * "Tap a point on the map" banner for the host map; draws nothing unless [PlannerMapBridge.pickingSide] is set.
 * Place it on top of the map (for example aligned to the top centre).
 */
@Composable
fun PlannerMapPickBanner(bridge: PlannerMapBridge, modifier: Modifier = Modifier) {
    val side = bridge.pickingSide ?: return
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.planner_pick_banner, sideName(side)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f, fill = false),
            )
            TextButton(onClick = { bridge.cancelPick() }) {
                Text(
                    text = stringResource(Res.string.planner_cancel),
                    color = MaterialTheme.colorScheme.inversePrimary,
                )
            }
        }
    }
}

/**
 * The MT_SW Planner as a full-height bottom sheet. While [PlannerMapBridge.pickingSide] is set the sheet is hidden
 * (its state lives in the [PlannerViewModel]) so the user can tap the map; the tap sets the point and the sheet returns.
 *
 * [nodeNum]: when given, side A is preset from that node every time the sheet opens; otherwise side A is preset from
 * the connected station on first use (when it has a position).
 */
@Composable
fun PlannerSheet(visible: Boolean, nodeNum: Int?, bridge: PlannerMapBridge, onDismiss: () -> Unit) {
    val vm: PlannerViewModel = koinViewModel()
    val state by vm.uiState.collectAsState()
    val nodes by vm.nodesWithPosition.collectAsState()

    DisposableEffect(bridge, vm) {
        bridge.registerPickHandler { side, lat, lon ->
            vm.selectSide(side)
            vm.setPointFromMap(side, lat, lon)
        }
        onDispose {
            bridge.registerPickHandler(null)
            bridge.cancelPick()
        }
    }

    LaunchedEffect(visible, nodeNum) {
        if (!visible) {
            bridge.cancelPick()
            return@LaunchedEffect
        }
        if (nodeNum != null) {
            val found = withTimeoutOrNull(NODE_WAIT_MS) { vm.nodesWithPosition.first { l -> l.any { it.num == nodeNum } } }
            vm.selectSide(PlannerSide.A)
            if (found != null) vm.setPointFromNode(PlannerSide.A, nodeNum)
        } else if (!vm.uiState.value.a.isComplete) {
            val found = withTimeoutOrNull(NODE_WAIT_MS) { vm.nodesWithPosition.first { l -> l.any { it.isOurs } } }
            if (found != null) vm.useStation(PlannerSide.A)
        }
    }

    if (visible && bridge.pickingSide == null) {
        val sheetState =
            rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            )
        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
            PlannerSheetContent(state = state, nodes = nodes, vm = vm, bridge = bridge, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun PlannerSheetContent(
    state: PlannerUiState,
    nodes: List<PlannerNodeOption>,
    vm: PlannerViewModel,
    bridge: PlannerMapBridge,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        PlannerHeader(onClose = onDismiss)
        HorizontalDivider()
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier =
                Modifier.widthIn(max = CONTENT_MAX_WIDTH)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SideSelector(state = state, vm = vm)
                PointSection(state = state, nodes = nodes, vm = vm, bridge = bridge)
                StationSection(state = state, vm = vm)
                RadioSection(state = state, vm = vm)
                ResultSection(state = state)
                ProfileChartSection(state = state)
                CoverageSection(state = state, vm = vm, bridge = bridge, onShownOnMap = onDismiss)
                ExportSection(state = state, vm = vm, bridge = bridge, onShownOnMap = onDismiss)
                CreditsFooter()
            }
        }
    }
}

@Composable
private fun PlannerHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(Res.string.planner_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(Res.string.planner_title_by),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        PlannerInfoButton(PlannerInfoTopic.GENERAL)
        IconButton(onClick = onClose) {
            Icon(MeshtasticIcons.Close, contentDescription = stringResource(Res.string.planner_close))
        }
    }
}

@Composable
private fun CreditsFooter() {
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.planner_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { open = true }) { Text(stringResource(Res.string.planner_credits_button)) }
    }
    if (open) PlannerInfoDialog(topic = PlannerInfoTopic.CREDITS, onDismiss = { open = false })
}
