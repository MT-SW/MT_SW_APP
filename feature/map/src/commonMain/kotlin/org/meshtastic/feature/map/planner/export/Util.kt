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
package org.meshtastic.feature.map.planner.export

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Locale independent number formatting (no String.format, never scientific notation). */
object Num {
    /** Fixed [decimals] (0..9) with '.' as decimal separator. NaN/Infinity render as "0". */
    fun fmt(value: Double, decimals: Int): String {
        if (value.isNaN() || value.isInfinite()) return if (decimals > 0) "0." + "0".repeat(decimals.coerceAtMost(9)) else "0"
        val d = decimals.coerceIn(0, 9)
        var p = 1.0
        repeat(d) { p *= 10.0 }
        val a = abs(value)
        if (a * p >= 9.0e15) {
            val s = if (a >= 9.0e18) Long.MAX_VALUE.toString() else a.toLong().toString()
            return (if (value < 0) "-" else "") + s + (if (d > 0) "." + "0".repeat(d) else "")
        }
        val scaled = floor(a * p + 0.5).toLong()
        val pl = p.toLong()
        val ip = scaled / pl
        val fp = scaled % pl
        val sb = StringBuilder()
        if (value < 0 && scaled != 0L) sb.append('-')
        sb.append(ip)
        if (d > 0) {
            sb.append('.')
            sb.append(fp.toString().padStart(d, '0'))
        }
        return sb.toString()
    }

    /** Like [fmt] but trailing zeros (and a dangling '.') are removed. */
    fun fmtTrim(value: Double, decimals: Int): String {
        val s = fmt(value, decimals)
        if (!s.contains('.')) return s
        return s.trimEnd('0').trimEnd('.')
    }

    fun fmt(value: Float, decimals: Int): String = fmt(value.toDouble(), decimals)

    internal fun hex2(v: Int): String {
        val h = "0123456789abcdef"
        return "" + h[(v shr 4) and 15] + h[v and 15]
    }
}

internal object Xml {
    fun escape(s: String): String {
        val sb = StringBuilder(s.length + 8)
        for (c in s) {
            when {
                c == '&' -> sb.append("&amp;")
                c == '<' -> sb.append("&lt;")
                c == '>' -> sb.append("&gt;")
                c == '"' -> sb.append("&quot;")
                c == '\'' -> sb.append("&apos;")
                c.code < 0x20 && c != '\n' && c != '\r' && c != '\t' -> {}
                c.code == 0xFFFE || c.code == 0xFFFF -> {}
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}

internal object Json {
    fun str(s: String): String {
        val sb = StringBuilder(s.length + 2)
        sb.append('"')
        for (c in s) {
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c == '\b' -> sb.append("\\b")
                c.code == 12 -> sb.append("\\f")
                c.code < 0x20 || c.code == 0x2028 || c.code == 0x2029 -> {
                    sb.append("\\u")
                    val h = "0123456789abcdef"
                    val v = c.code
                    sb.append(h[(v shr 12) and 15]).append(h[(v shr 8) and 15]).append(h[(v shr 4) and 15]).append(h[v and 15])
                }
                else -> sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString()
    }
}

internal object CoverageBins {
    private const val FALLBACK = 0xFF607D8B.toInt()

    fun index(c: ReportCoverage, margin: Float): Int {
        val k = c.legend.size
        if (k == 0) return -1
        for (i in 0 until k - 1) {
            val t = if (i < c.thresholdsDb.size && c.thresholdsDb.size >= k - 1) c.thresholdsDb[i] else 20f - 10f * i
            if (margin >= t) return i
        }
        return k - 1
    }

    fun color(c: ReportCoverage, idx: Int): Int {
        val raw = if (idx in c.legend.indices) c.legend[idx].second else FALLBACK
        return if ((raw ushr 24) == 0) raw or (0xFF shl 24) else raw
    }

    /** Equirectangular offsets from the centre in km: x east, y north. */
    fun xKm(c: ReportCoverage, lon: Double): Double = (lon - c.centerLon) * cos(c.centerLat * 0.017453292519943295) * 111.320
    fun yKm(c: ReportCoverage, lat: Double): Double = (lat - c.centerLat) * 110.574

    /** Median nearest-neighbour distance (km) of (a subset of) the points; 0 if undeterminable. */
    fun cellKm(xs: DoubleArray, ys: DoubleArray): Double {
        val n = xs.size
        if (n < 2) return 0.0
        val step = max(1, n / 150)
        val nn = ArrayList<Double>()
        var i = 0
        while (i < n) {
            var best = Double.MAX_VALUE
            for (j in 0 until n) {
                if (j == i) continue
                val dx = xs[j] - xs[i]
                val dy = ys[j] - ys[i]
                val d = dx * dx + dy * dy
                if (d > 1e-12 && d < best) best = d
            }
            if (best < Double.MAX_VALUE) nn.add(sqrt(best))
            i += step
        }
        if (nn.isEmpty()) return 0.0
        nn.sort()
        return nn[nn.size / 2]
    }
}

/** Shared geometry of the terrain chart (used by the PDF and the PNG renderers). */
internal class ChartData(p: ReportProfile) {
    val n: Int = minOf(p.distancesM.size, p.groundM.size, p.losM.size, p.fresnelUpperM.size, p.fresnelLowerM.size)
    val valid: Boolean get() = n >= 2
    val x = p.distancesM
    val ground = p.groundM
    val los = p.losM
    val up = p.fresnelUpperM
    val lo = p.fresnelLowerM
    var xMin = 0.0
    var xMax = 1.0
    var yMin = 0.0
    var yMax = 1.0

    init {
        if (n >= 1) {
            var x0 = Double.MAX_VALUE; var x1 = -Double.MAX_VALUE
            var y0 = Double.MAX_VALUE; var y1 = -Double.MAX_VALUE
            for (i in 0 until n) {
                if (x[i].isFinite()) { x0 = min(x0, x[i]); x1 = max(x1, x[i]) }
                for (v in doubleArrayOf(ground[i], los[i], up[i], lo[i])) if (v.isFinite()) { y0 = min(y0, v); y1 = max(y1, v) }
            }
            if (x0 <= x1) { xMin = x0; xMax = x1 }
            if (y0 <= y1) { yMin = y0; yMax = y1 }
        }
        if (xMax - xMin < 1e-9) { xMax = xMin + 1.0 }
        val pad = max((yMax - yMin) * 0.06, 1.0)
        yMin -= pad
        yMax += pad
    }

    fun xTicks(): DoubleArray = niceTicks(xMin, xMax, 8)
    fun yTicks(): DoubleArray = niceTicks(yMin, yMax, 6)

    companion object {
        fun niceTicks(lo: Double, hi: Double, target: Int): DoubleArray {
            val range = hi - lo
            if (!(range > 0)) return doubleArrayOf(lo)
            val raw = range / target
            val mag = pow10(floor(log10(raw)).toInt())
            val f = raw / mag
            val step = (if (f <= 1.0) 1.0 else if (f <= 2.0) 2.0 else if (f <= 5.0) 5.0 else 10.0) * mag
            val out = ArrayList<Double>()
            var v = ceil(lo / step - 1e-9) * step
            var guard = 0
            while (v <= hi + step * 1e-9 && guard++ < 200) { out.add(v); v += step }
            return out.toDoubleArray()
        }

        fun pow10(e: Int): Double {
            var r = 1.0
            if (e >= 0) repeat(e) { r *= 10.0 } else repeat(-e) { r /= 10.0 }
            return r
        }

        /** Decimals needed to print ticks spaced by [step]. */
        fun decimalsFor(ticks: DoubleArray): Int {
            if (ticks.size < 2) return 0
            val step = abs(ticks[1] - ticks[0])
            if (step >= 1.0 - 1e-9) return 0
            return min(4, ceil(-log10(step) - 1e-9).toInt())
        }
    }
}
