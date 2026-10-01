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
package org.meshtastic.feature.map.planner.ui

/** Platform neutral UTC time formatting for report time stamps (no date-time library needed). */
object PlannerClock {
    private const val MS_PER_DAY = 86_400_000L
    private const val MS_PER_MINUTE = 60_000L
    private const val MINUTES_PER_HOUR = 60L

    /** "yyyy-MM-dd HH:mm" (UTC) of [epochMillis]. */
    fun formatUtcMinute(epochMillis: Long): String {
        val (y, m, d) = civil(epochMillis)
        val minuteOfDay = epochMillis.mod(MS_PER_DAY) / MS_PER_MINUTE
        return "${pad(y, 4)}-${pad(m, 2)}-${pad(d, 2)} ${pad(minuteOfDay / MINUTES_PER_HOUR, 2)}:${pad(minuteOfDay % MINUTES_PER_HOUR, 2)}"
    }

    /** "yyyyMMdd_HHmm" (UTC) for file names. */
    fun fileStamp(epochMillis: Long): String =
        formatUtcMinute(epochMillis).replace("-", "").replace(" ", "_").replace(":", "")

    /** Days since 1970-01-01 to (year, month, day), proleptic Gregorian (Howard Hinnant's algorithm). */
    @Suppress("MagicNumber")
    private fun civil(epochMillis: Long): Triple<Long, Long, Long> {
        val z = epochMillis.floorDiv(MS_PER_DAY) + 719_468L
        val era = z.floorDiv(146_097L)
        val doe = z - era * 146_097L
        val yoe = (doe - doe / 1_460L + doe / 36_524L - doe / 146_096L) / 365L
        val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
        val mp = (5L * doy + 2L) / 153L
        val d = doy - (153L * mp + 2L) / 5L + 1L
        val m = if (mp < 10L) mp + 3L else mp - 9L
        val y = yoe + era * 400L + if (m <= 2L) 1L else 0L
        return Triple(y, m, d)
    }

    private fun pad(v: Long, width: Int): String = v.toString().padStart(width, '0')
}
