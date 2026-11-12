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
package org.meshtastic.feature.map.maplibre.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.util.SNR_LEGEND

/**
 * Key to the track map: the SNR colour ramp when the track is coloured by signal, and which colour is which node when a
 * second track is compared. Small and translucent, so the map under it stays readable.
 */
@Composable
internal fun TrackLegend(
    showSnr: Boolean,
    mainName: String,
    mainColor: Color,
    compareName: String?,
    compareColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(LEGEND_WIDTH.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = LEGEND_ALPHA),
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (compareName != null) {
                LegendRow(color = mainColor, name = mainName)
                LegendRow(color = compareColor, name = compareName)
            }
            if (showSnr) {
                Text(text = "SNR (dB)", style = MaterialTheme.typography.labelSmall)
                val ramp = SNR_LEGEND.map { (_, rgb) -> Color(OPAQUE or rgb) }
                Box(
                    modifier =
                    Modifier.fillMaxWidth()
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(Brush.horizontalGradient(ramp)),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val labelStyle = MaterialTheme.typography.labelSmall
                    Text(text = SNR_LEGEND.first().first.toInt().toString(), style = labelStyle)
                    Text(text = "+" + SNR_LEGEND.last().first.toInt(), style = labelStyle)
                }
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, name: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val LEGEND_WIDTH = 140
private const val LEGEND_ALPHA = 0.85f
private const val OPAQUE = 0xFF000000.toInt()
