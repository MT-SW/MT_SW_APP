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
package org.meshtastic.feature.node.metrics

import org.meshtastic.core.common.util.latLongToMeter
import org.meshtastic.core.model.fixOrNull
import org.meshtastic.core.model.util.GeoConstants
import org.meshtastic.proto.Position

/** Length of the track in metres: the legs between its fixes, oldest to newest. Positions with no fix are skipped. */
internal fun trackLengthMeters(positions: List<Position>): Double {
    val fixes = positions.sortedBy { it.time }.mapNotNull { it.fixOrNull() }
    var total = 0.0
    for (i in 1 until fixes.size) {
        val (a, b) = fixes[i - 1] to fixes[i]
        total +=
            latLongToMeter(
                a.first * GeoConstants.DEG_D,
                a.second * GeoConstants.DEG_D,
                b.first * GeoConstants.DEG_D,
                b.second * GeoConstants.DEG_D,
            )
    }
    return total
}
