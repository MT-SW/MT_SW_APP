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
package org.meshtastic.core.model

private const val MAX_BATTERY_PERCENT = 100

/** Open-circuit voltage (volts) of a single Li-ion/LiPo cell at 100%, 90% ... 0%, the same table the firmware uses. */
private val CELL_VOLTAGE_STEPS =
    floatArrayOf(4.19f, 4.05f, 3.99f, 3.89f, 3.80f, 3.72f, 3.63f, 3.55f, 3.45f, 3.30f, 3.10f)
private const val PERCENT_PER_STEP = 10

/** Rough charge level (1..100) of a single cell from its [volts], or null when there is no usable voltage. */
fun batteryPercentFromVoltage(volts: Float): Int? {
    if (volts.isNaN() || volts <= 0f) return null
    if (volts >= CELL_VOLTAGE_STEPS.first()) return MAX_BATTERY_PERCENT
    if (volts <= CELL_VOLTAGE_STEPS.last()) return 1
    for (i in 0 until CELL_VOLTAGE_STEPS.lastIndex) {
        val upper = CELL_VOLTAGE_STEPS[i]
        val lower = CELL_VOLTAGE_STEPS[i + 1]
        if (volts <= upper && volts > lower) {
            val fraction = (volts - lower) / (upper - lower)
            val percent = MAX_BATTERY_PERCENT - (i + 1) * PERCENT_PER_STEP + (fraction * PERCENT_PER_STEP).toInt()
            return percent.coerceIn(1, MAX_BATTERY_PERCENT)
        }
    }
    return null
}

/**
 * Display-only swap for a power monitor wired the wrong way round: what it reports as "powered" (level above 100) is
 * really running on battery, and a percentage is really "charging". A real percentage is estimated from [voltage];
 * without a usable voltage the level is unknown (null).
 */
fun swapBatteryPolarity(level: Int?, voltage: Float?): Int? = when {
    level == null || level < 0 -> level
    level > MAX_BATTERY_PERCENT -> voltage?.let(::batteryPercentFromVoltage)
    else -> MAX_BATTERY_PERCENT + 1
}
