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

import androidx.compose.runtime.compositionLocalOf
import org.meshtastic.proto.Position

/** Second track drawn on the position map next to the node's own, for comparing two routes. */
data class TrackCompare(val nodeNum: Int, val label: String, val positions: List<Position>)

/**
 * What the position-log screen asks the track map to add. It travels as a composition local rather than as extra
 * parameters of the map provider, so the map hosts on every platform keep their signature.
 *
 * @param colorBySnr colour the track by the signal strength it was received with, instead of by age.
 * @param snrByTime `Position.time` to the SNR (dB) of the direct packet that carried that position.
 * @param compare the second track, or null.
 */
data class TrackOverlay(
    val colorBySnr: Boolean = false,
    val snrByTime: Map<Int, Float> = emptyMap(),
    val compare: TrackCompare? = null,
)

@Suppress("CompositionLocalAllowlist")
val LocalTrackOverlay = compositionLocalOf { TrackOverlay() }

/** Colour steps of the coverage track, weakest first: SNR in dB to 0xRRGGBB. */
private val SNR_STEPS = listOf(-20f to 0xD32F2F, -12f to 0xF57C00, -6f to 0xFBC02D, 0f to 0x9CCC65, 5f to 0x2E7D32)

/** A point with no measured SNR. */
const val SNR_UNKNOWN_RGB: Int = 0x9E9E9E

/** The SNR range the legend shows. */
val SNR_LEGEND: List<Pair<Float, Int>> = SNR_STEPS

/**
 * Colour for a received [snr], as 0xRRGGBB. Blends linearly between the steps and clamps at both ends; null (no direct
 * measurement) is grey.
 */
@Suppress("ReturnCount")
fun snrRgb(snr: Float?): Int {
    if (snr == null || snr.isNaN()) return SNR_UNKNOWN_RGB
    if (snr <= SNR_STEPS.first().first) return SNR_STEPS.first().second
    if (snr >= SNR_STEPS.last().first) return SNR_STEPS.last().second
    val upper = SNR_STEPS.indexOfFirst { it.first >= snr }
    val (lowSnr, lowRgb) = SNR_STEPS[upper - 1]
    val (highSnr, highRgb) = SNR_STEPS[upper]
    val share = (snr - lowSnr) / (highSnr - lowSnr)
    fun channel(shift: Int): Int {
        val a = (lowRgb shr shift) and CHANNEL_MASK
        val b = (highRgb shr shift) and CHANNEL_MASK
        return (a + (b - a) * share).toInt().coerceIn(0, CHANNEL_MASK)
    }
    return (channel(RED_SHIFT) shl RED_SHIFT) or (channel(GREEN_SHIFT) shl GREEN_SHIFT) or channel(0)
}

private const val CHANNEL_MASK = 0xFF
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
