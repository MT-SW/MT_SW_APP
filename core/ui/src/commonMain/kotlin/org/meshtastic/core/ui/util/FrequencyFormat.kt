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
package org.meshtastic.core.ui.util

import org.meshtastic.core.common.util.NumberFormatter

private const val MAX_FREQUENCY_DECIMALS = 5
private const val MIN_FREQUENCY_DECIMALS = 3

/**
 * [mhz] as the LoRa frequency fields show it (MT_SW: full resolution, up to five decimals, so 869.44165 is not rounded
 * to 869.442; at least three decimals, a dot separator, no grouping).
 */
fun formatFrequencyMhz(mhz: Float): String {
    // Go through the float's own shortest text, so 869.4f does not turn into 869.40002 when widened to a double.
    val widened = mhz.toString().toDoubleOrNull() ?: mhz.toDouble()
    val exact = NumberFormatter.formatInvariant(widened, MAX_FREQUENCY_DECIMALS)
    val dot = exact.indexOf('.')
    if (dot < 0) return exact
    val trimmed = exact.trimEnd('0')
    return trimmed.padEnd(dot + 1 + MIN_FREQUENCY_DECIMALS, '0')
}
