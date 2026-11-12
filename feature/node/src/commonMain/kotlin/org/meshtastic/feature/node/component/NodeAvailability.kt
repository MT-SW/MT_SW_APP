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
@file:Suppress("MatchingDeclarationName", "MagicNumber")

package org.meshtastic.feature.node.component

/** One choice of the availability chart: how far back it looks and how long each bar is, both in seconds. */
internal data class AvailabilityRange(val rangeSeconds: Long, val bucketSeconds: Long) {
    val bucketCount: Int
        get() = (rangeSeconds / bucketSeconds).toInt()
}

private const val HOUR = 3_600L
private const val DAY = 24 * HOUR

/** From a day (bars of half an hour) up to a month (bars of eight hours). */
internal val AVAILABILITY_RANGES =
    listOf(
        AvailabilityRange(DAY, HOUR / 2),
        AvailabilityRange(3 * DAY, HOUR),
        AvailabilityRange(7 * DAY, 2 * HOUR),
        AvailabilityRange(14 * DAY, 4 * HOUR),
        AvailabilityRange(30 * DAY, 8 * HOUR),
    )

/**
 * How many packets from the node fall into each bar; the last bar ends at [nowMs], the first starts
 * [AvailabilityRange.rangeSeconds] before it. [receivedMs] are the packet receive times in epoch milliseconds.
 */
internal fun availabilityCounts(receivedMs: List<Long>, nowMs: Long, range: AvailabilityRange): IntArray {
    val counts = IntArray(range.bucketCount)
    val bucketMs = range.bucketSeconds * 1000
    val startMs = nowMs - range.rangeSeconds * 1000
    for (t in receivedMs) {
        val index = ((t - startMs) / bucketMs).toInt()
        if (t >= startMs && index in counts.indices) counts[index]++
    }
    return counts
}

/**
 * The longest time between two packets from the node, counting the time from the last packet up to [nowMs]; 0 when
 * there is no packet. The time before the first packet is not counted: the log may simply not reach back that far.
 */
internal fun longestSilenceSeconds(receivedMs: List<Long>, nowMs: Long): Long {
    if (receivedMs.isEmpty()) return 0L
    var longest = (nowMs - receivedMs.last()).coerceAtLeast(0L)
    for (i in 1 until receivedMs.size) longest = maxOf(longest, receivedMs[i] - receivedMs[i - 1])
    return longest / 1000
}
