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
@file:Suppress("ModifierMissing")

package org.meshtastic.feature.map.maplibre.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.trips_chart_height
import org.meshtastic.core.resources.trips_chart_speed
import org.meshtastic.feature.map.trip.Trip
import org.meshtastic.feature.map.trip.TripStore

private const val MAX_CHART_POINTS = 200
private const val CHART_HEIGHT_DP = 80
private const val MS_PER_HOUR = 3_600_000.0
private const val METERS_PER_KM = 1000.0

/** MT_SW: two small charts of a hike along the route: the height (when it was recorded) and the speed. */
@Composable
internal fun TripCharts(trip: Trip) {
    val heights = remember(trip.id, trip.points.size) { sample(trip.points.map { it.ele }) }
    val speeds =
        remember(trip.id, trip.points.size) {
            sample(
                trip.points.mapIndexed { i, p ->
                    if (i == 0) {
                        null
                    } else {
                        val q = trip.points[i - 1]
                        val dt = p.t - q.t
                        if (dt <= 0L) {
                            null
                        } else {
                            TripStore.meters(q.lat, q.lon, p.lat, p.lon) / METERS_PER_KM / (dt / MS_PER_HOUR)
                        }
                    }
                },
            )
        }
    if (heights.size >= 2) Chart(stringResource(Res.string.trips_chart_height), heights, "m")
    if (speeds.size >= 2) Chart(stringResource(Res.string.trips_chart_speed), speeds, "km/h")
}

/** At most [MAX_CHART_POINTS] values: the missing ones are skipped and a long route is thinned out. */
private fun sample(values: List<Double?>): List<Double> {
    val present = values.filterNotNull()
    if (present.size <= MAX_CHART_POINTS) return present
    val step = present.size.toDouble() / MAX_CHART_POINTS
    return List(MAX_CHART_POINTS) { present[(it * step).toInt()] }
}

@Composable
private fun Chart(title: String, values: List<Double>, unit: String) {
    val low = values.min()
    val high = values.max()
    val colour = MaterialTheme.colorScheme.primary
    Column {
        Text(
            text = "$title: ${NumberFormatter.format(low, 0)}–${NumberFormatter.format(high, 0)} $unit",
            style = MaterialTheme.typography.bodySmall,
        )
        Canvas(modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT_DP.dp)) {
            val span = (high - low).coerceAtLeast(1e-6)
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = size.width * i / (values.size - 1)
                val y = size.height * (1f - ((v - low) / span).toFloat())
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawLine(colour.copy(alpha = 0.3f), Offset(0f, size.height), Offset(size.width, size.height))
            drawPath(path, colour, style = Stroke(width = 2.dp.toPx()))
        }
    }
}
