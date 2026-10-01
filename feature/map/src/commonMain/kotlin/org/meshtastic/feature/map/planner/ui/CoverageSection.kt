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
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package org.meshtastic.feature.map.planner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.planner_cancel
import org.meshtastic.core.resources.planner_coverage_clear
import org.meshtastic.core.resources.planner_coverage_compute
import org.meshtastic.core.resources.planner_coverage_extent
import org.meshtastic.core.resources.planner_coverage_need_point
import org.meshtastic.core.resources.planner_coverage_radials
import org.meshtastic.core.resources.planner_coverage_range
import org.meshtastic.core.resources.planner_coverage_rx_gain
import org.meshtastic.core.resources.planner_coverage_rx_height
import org.meshtastic.core.resources.planner_coverage_side
import org.meshtastic.core.resources.planner_layer_name
import org.meshtastic.core.resources.planner_legend_excellent
import org.meshtastic.core.resources.planner_legend_good
import org.meshtastic.core.resources.planner_legend_marginal
import org.meshtastic.core.resources.planner_legend_none
import org.meshtastic.core.resources.planner_legend_weak
import org.meshtastic.core.resources.planner_section_coverage
import org.meshtastic.core.resources.planner_show_on_map
import org.meshtastic.core.resources.planner_unit_dbi
import org.meshtastic.core.resources.planner_unit_km
import org.meshtastic.core.resources.planner_unit_m
import org.meshtastic.feature.map.planner.CoverageResult
import org.meshtastic.feature.map.planner.marginAt
import org.meshtastic.feature.map.planner.state.PlannerSide
import org.meshtastic.feature.map.planner.state.PlannerUiState
import org.meshtastic.feature.map.planner.state.PlannerViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt

private const val GRID = 80
private const val METERS_PER_DEG_LAT = 110_574.0
private const val METERS_PER_DEG_LON_EQUATOR = 111_320.0
private const val MAX_RANGE_KM = 100.0
private const val MAX_RADIALS = 180
private const val MIN_RADIALS = 8
private const val MAX_RX_HEIGHT_M = 100.0
private const val MIN_RX_GAIN = -10.0
private const val MAX_RX_GAIN = 30.0
private const val ALPHA = 0.85f

/** Margin classes of the plan, best first (same thresholds and colours as the exported reports). */
private val THRESHOLDS_DB = floatArrayOf(20f, 10f, 0f, -10f)
private val CLASS_COLORS =
    intArrayOf(0xFF1B5E20.toInt(), 0xFF66BB6A.toInt(), 0xFFFFEB3B.toInt(), 0xFFEF6C00.toInt(), 0xFFB71C1C.toInt())

private fun classOf(marginDb: Float): Int {
    for (i in THRESHOLDS_DB.indices) if (marginDb >= THRESHOLDS_DB[i]) return i
    return THRESHOLDS_DB.size
}

/** Coverage prediction: parameters, calculation with progress, plan, legend. */
@Composable
internal fun CoverageSection(
    state: PlannerUiState,
    vm: PlannerViewModel,
    bridge: PlannerMapBridge,
    onShownOnMap: () -> Unit,
) {
    val side = state.coverageSide
    PlannerCard(title = stringResource(Res.string.planner_section_coverage), info = PlannerInfoTopic.COVERAGE) {
        Text(
            text = stringResource(Res.string.planner_coverage_side),
            style = MaterialTheme.typography.labelLarge,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PlannerSide.entries.forEachIndexed { index, s ->
                SegmentedButton(
                    selected = side == s,
                    onClick = { vm.setCoverageSide(s) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = PlannerSide.entries.size),
                ) {
                    Text(sideName(s))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            PlannerNumberField(
                label = stringResource(Res.string.planner_coverage_range),
                value = state.coverageMaxRangeKm,
                onValue = { vm.setCoverageMaxRangeKm(it) },
                modifier = Modifier.weight(1f),
                decimals = 1,
                suffix = stringResource(Res.string.planner_unit_km),
                min = 1.0,
                max = MAX_RANGE_KM,
            )
            PlannerNumberField(
                label = stringResource(Res.string.planner_coverage_radials),
                value = state.coverageRadials.toDouble(),
                onValue = { vm.setCoverageRadials(it.roundToInt()) },
                modifier = Modifier.weight(1f),
                decimals = 0,
                min = MIN_RADIALS.toDouble(),
                max = MAX_RADIALS.toDouble(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            PlannerNumberField(
                label = stringResource(Res.string.planner_coverage_rx_height),
                value = state.coverageRxHeightM,
                onValue = { vm.setCoverageRxHeightM(it) },
                modifier = Modifier.weight(1f),
                decimals = 1,
                suffix = stringResource(Res.string.planner_unit_m),
                min = 0.0,
                max = MAX_RX_HEIGHT_M,
            )
            PlannerNumberField(
                label = stringResource(Res.string.planner_coverage_rx_gain),
                value = state.coverageRxGainDbi,
                onValue = { vm.setCoverageRxGainDbi(it) },
                modifier = Modifier.weight(1f),
                decimals = 1,
                suffix = stringResource(Res.string.planner_unit_dbi),
                min = MIN_RX_GAIN,
                max = MAX_RX_GAIN,
            )
        }
        if (!state.end(side).isComplete) {
            PlannerNotice(text = stringResource(Res.string.planner_coverage_need_point, sideName(side)))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { vm.computeCoverage() }, enabled = !state.coverageComputing) {
                Text(stringResource(Res.string.planner_coverage_compute))
            }
            if (state.coverageComputing || state.coverage != null) {
                OutlinedButton(onClick = { vm.clearCoverage() }) {
                    Text(
                        stringResource(
                            if (state.coverageComputing) Res.string.planner_cancel else Res.string.planner_coverage_clear,
                        ),
                    )
                }
            }
        }
        if (state.coverageComputing) {
            LinearProgressIndicator(progress = { state.coverageProgress }, modifier = Modifier.fillMaxWidth())
        }
        state.coverageError?.let { PlannerNotice(text = errorText(it), isError = true) }
        val coverage = state.coverage
        if (coverage != null && !state.coverageComputing) {
            CoveragePlan(coverage = coverage)
            CoverageLegend()
            ShowOnMapButton(vm = vm, bridge = bridge, onShownOnMap = onShownOnMap)
        }
    }
}

@Composable
private fun CoverageLegend() {
    val labels =
        listOf(
            stringResource(Res.string.planner_legend_excellent),
            stringResource(Res.string.planner_legend_good),
            stringResource(Res.string.planner_legend_marginal),
            stringResource(Res.string.planner_legend_weak),
            stringResource(Res.string.planner_legend_none),
        )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEachIndexed { i, label -> LegendDot(Color(CLASS_COLORS[i]), label) }
    }
}

/** Button that exports the current plan as GeoJSON and hands it to the host map. */
@Composable
internal fun ShowOnMapButton(vm: PlannerViewModel, bridge: PlannerMapBridge, onShownOnMap: () -> Unit) {
    val strings = rememberPlannerReportStrings()
    val prefix = plannerGeneratedPrefix()
    val layerName = stringResource(Res.string.planner_layer_name)
    val scope = rememberCoroutineScope()
    Button(
        onClick = {
            scope.launch {
                showOnMap(vm, strings.copy(generatedAt = plannerGeneratedAt(prefix)), bridge, layerName)
                onShownOnMap()
            }
        },
    ) {
        Text(stringResource(Res.string.planner_show_on_map))
    }
}

/** Adds the GeoJSON of the current report as a map layer and moves the map there. */
internal suspend fun showOnMap(
    vm: PlannerViewModel,
    strings: org.meshtastic.feature.map.planner.state.PlannerReportStrings,
    bridge: PlannerMapBridge,
    layerName: String,
) {
    val json = vm.exportGeoJson(strings)
    bridge.addGeoJsonLayer(layerName, json)
    val st = vm.uiState.value
    val target = st.coverage?.center ?: st.a.point ?: st.b.point
    if (target != null) bridge.moveTo(target.lat, target.lon)
}

/** Colour of every cell of the plan (0 = outside the calculated range), computed from [CoverageResult.marginAt]. */
private fun coverageGrid(coverage: CoverageResult): IntArray {
    val out = IntArray(GRID * GRID)
    val radiusM = coverage.ringsM.lastOrNull() ?: return out
    val lat0 = coverage.center.lat
    val lon0 = coverage.center.lon
    val metersPerDegLon = max(1.0, METERS_PER_DEG_LON_EQUATOR * cos(lat0 * PI / 180.0))
    for (j in 0 until GRID) {
        val northM = radiusM - (j + 0.5) / GRID * 2.0 * radiusM
        for (i in 0 until GRID) {
            val eastM = (i + 0.5) / GRID * 2.0 * radiusM - radiusM
            val m = coverage.marginAt(lat0 + northM / METERS_PER_DEG_LAT, lon0 + eastM / metersPerDegLon)
            if (m != null) out[j * GRID + i] = CLASS_COLORS[classOf(m)]
        }
    }
    return out
}

/** Square plan view (north up) of a coverage result, with range rings, centre marker and scale bar. */
@Composable
internal fun CoveragePlan(coverage: CoverageResult, modifier: Modifier = Modifier) {
    val grid = remember(coverage) { coverageGrid(coverage) }
    val radiusKm = (coverage.ringsM.lastOrNull() ?: 0.0) / 1000.0
    val unitKm = stringResource(Res.string.planner_unit_km)
    val textMeasurer = rememberTextMeasurer()
    // The plan uses fixed light colours so that the black marks stay readable in the dark theme too.
    val onSurface = Color.Black
    val background = Color(0xFFEFEFEF)
    val labelStyle = TextStyle(fontSize = 10.sp, color = onSurface)
    val scaleKm = ChartMath.niceStep(radiusKm * 0.4)
    val scaleLabel = fmt(scaleKm, ChartMath.decimalsFor(scaleKm)) + " " + unitKm
    val ringKm = ChartMath.niceTicks(0.0, radiusKm, 4).filter { it > 0.0 }
    val ringLabels = ringKm.map { fmt(it, ChartMath.decimalsFor(ringKm.firstOrNull() ?: 1.0)) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
            drawRect(background)
            val cell = size.width / GRID
            for (j in 0 until GRID) {
                for (i in 0 until GRID) {
                    val c = grid[j * GRID + i]
                    if (c != 0) {
                        drawRect(
                            Color(c).copy(alpha = ALPHA),
                            topLeft = Offset(i * cell, j * cell),
                            size = Size(cell + 0.6f, cell + 0.6f),
                        )
                    }
                }
            }
            val centre = Offset(size.width / 2f, size.height / 2f)
            val pxPerKm = if (radiusKm > 0.0) (size.width / 2f) / radiusKm.toFloat() else 0f
            ringKm.forEachIndexed { i, km ->
                val r = km.toFloat() * pxPerKm
                drawCircle(onSurface.copy(alpha = 0.35f), radius = r, center = centre, style = Stroke(width = 1.dp.toPx()))
                val layout = textMeasurer.measure(text = ringLabels[i], style = labelStyle)
                drawText(layout, topLeft = Offset(centre.x + 2.dp.toPx(), centre.y - r - layout.size.height))
            }
            // Centre marker.
            drawCircle(Color.White, radius = 6.dp.toPx(), center = centre)
            drawCircle(Color.Black, radius = 6.dp.toPx(), center = centre, style = Stroke(width = 2.dp.toPx()))
            // Scale bar (bottom left).
            val barLen = scaleKm.toFloat() * pxPerKm
            val y = size.height - 14.dp.toPx()
            val x0 = 10.dp.toPx()
            drawLine(Color.Black, Offset(x0, y), Offset(x0 + barLen, y), strokeWidth = 3.dp.toPx())
            drawLine(Color.Black, Offset(x0, y - 4.dp.toPx()), Offset(x0, y + 4.dp.toPx()), strokeWidth = 2.dp.toPx())
            drawLine(Color.Black, Offset(x0 + barLen, y - 4.dp.toPx()), Offset(x0 + barLen, y + 4.dp.toPx()), strokeWidth = 2.dp.toPx())
            val label = textMeasurer.measure(text = scaleLabel, style = labelStyle.copy(color = Color.Black))
            drawText(label, topLeft = Offset(x0, y - label.size.height - 6.dp.toPx()))
        }
        Text(
            text = stringResource(Res.string.planner_coverage_extent, fmt(radiusKm, 1) + " " + unitKm),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
