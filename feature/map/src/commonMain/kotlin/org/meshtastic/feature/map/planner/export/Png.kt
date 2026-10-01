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
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Minimal PNG encoder: RGBA 8-bit, filter 0, zlib stored (uncompressed) deflate blocks. */
object PlannerPng {
    private val CRC_TABLE = IntArray(256) { n ->
        var c = n
        repeat(8) { c = if ((c and 1) != 0) (0xEDB88320.toInt() xor (c ushr 1)) else (c ushr 1) }
        c
    }

    fun crc32(data: ByteArray, off: Int = 0, len: Int = data.size - off): Int {
        var c = -1
        for (i in off until off + len) c = CRC_TABLE[(c xor data[i].toInt()) and 0xFF] xor (c ushr 8)
        return c.inv()
    }

    fun adler32(data: ByteArray, off: Int = 0, len: Int = data.size - off): Int {
        var a = 1
        var b = 0
        var i = off
        val end = off + len
        while (i < end) {
            val stop = min(end, i + 5000)
            while (i < stop) { a += data[i].toInt() and 0xFF; b += a; i++ }
            a %= 65521
            b %= 65521
        }
        return (b shl 16) or a
    }

    private fun ByteArrayBuilder.int32(v: Int) {
        add(v ushr 24); add(v ushr 16); add(v ushr 8); add(v)
    }

    internal class ByteArrayBuilder(cap: Int = 1024) {
        var buf = ByteArray(cap)
        var size = 0
        fun add(b: Int) {
            if (size == buf.size) buf = buf.copyOf(buf.size * 2)
            buf[size++] = b.toByte()
        }
        fun add(src: ByteArray, off: Int = 0, len: Int = src.size - off) {
            while (size + len > buf.size) buf = buf.copyOf(buf.size * 2)
            src.copyInto(buf, size, off, off + len)
            size += len
        }
        fun toByteArray(): ByteArray = buf.copyOf(size)
    }

    private fun chunk(out: ByteArrayBuilder, type: String, data: ByteArray) {
        out.int32(data.size)
        val td = ByteArray(4 + data.size)
        for (i in 0 until 4) td[i] = type[i].code.toByte()
        data.copyInto(td, 4)
        out.add(td)
        out.int32(crc32(td))
    }

    /** [argb] holds `width * height` pixels in 0xAARRGGBB, row-major. */
    fun encode(width: Int, height: Int, argb: IntArray): ByteArray {
        require(width > 0 && height > 0) { "empty image" }
        require(argb.size >= width * height) { "pixel buffer too small" }
        val stride = 1 + width * 4
        val raw = ByteArray(stride * height)
        var o = 0
        for (y in 0 until height) {
            raw[o++] = 0
            for (x in 0 until width) {
                val p = argb[y * width + x]
                raw[o++] = (p shr 16).toByte(); raw[o++] = (p shr 8).toByte(); raw[o++] = p.toByte(); raw[o++] = (p ushr 24).toByte()
            }
        }
        val z = ByteArrayBuilder(raw.size + raw.size / 60000 * 5 + 64)
        z.add(0x78); z.add(0x01)
        var pos = 0
        if (raw.isEmpty()) { z.add(1); z.add(0); z.add(0); z.add(0xFF); z.add(0xFF) }
        while (pos < raw.size) {
            val n = min(65535, raw.size - pos)
            val last = pos + n >= raw.size
            z.add(if (last) 1 else 0)
            z.add(n and 0xFF); z.add(n shr 8)
            z.add(n.inv() and 0xFF); z.add((n.inv() shr 8) and 0xFF)
            z.add(raw, pos, n)
            pos += n
        }
        z.int32(adler32(raw))

        val out = ByteArrayBuilder(z.size + 100)
        out.add(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        val ihdr = ByteArrayBuilder(13)
        ihdr.int32(width); ihdr.int32(height); ihdr.add(8); ihdr.add(6); ihdr.add(0); ihdr.add(0); ihdr.add(0)
        chunk(out, "IHDR", ihdr.toByteArray())
        chunk(out, "IDAT", z.toByteArray())
        chunk(out, "IEND", ByteArray(0))
        return out.toByteArray()
    }
}

/** Software rasteriser used for the profile chart image. */
internal class Canvas(val w: Int, val h: Int, bg: Int = 0xFFFFFFFF.toInt()) {
    val px = IntArray(w * h) { bg }

    fun blend(x: Int, y: Int, color: Int, cov: Double) {
        if (x < 0 || y < 0 || x >= w || y >= h) return
        val a = ((color ushr 24) / 255.0) * cov
        if (a <= 0) return
        val d = px[y * w + x]
        fun ch(s: Int, t: Int) = (t + (s - t) * a + 0.5).toInt().coerceIn(0, 255)
        val r = ch((color shr 16) and 255, (d shr 16) and 255)
        val g = ch((color shr 8) and 255, (d shr 8) and 255)
        val b = ch(color and 255, d and 255)
        px[y * w + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun rect(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
        for (y in max(0, y0) until min(h, y1)) for (x in max(0, x0) until min(w, x1)) blend(x, y, color, 1.0)
    }

    /** Anti-aliased thick segment. */
    fun line(x0: Double, y0: Double, x1: Double, y1: Double, width: Double, color: Int) {
        val hw = width / 2
        val bx0 = floor(min(x0, x1) - hw - 1).toInt(); val bx1 = floor(max(x0, x1) + hw + 1).toInt()
        val by0 = floor(min(y0, y1) - hw - 1).toInt(); val by1 = floor(max(y0, y1) + hw + 1).toInt()
        val dx = x1 - x0; val dy = y1 - y0
        val l2 = dx * dx + dy * dy
        for (y in max(0, by0)..min(h - 1, by1)) for (x in max(0, bx0)..min(w - 1, bx1)) {
            val px0 = x + 0.5; val py0 = y + 0.5
            val t = if (l2 == 0.0) 0.0 else (((px0 - x0) * dx + (py0 - y0) * dy) / l2).coerceIn(0.0, 1.0)
            val ex = x0 + t * dx - px0; val ey = y0 + t * dy - py0
            val d = sqrt(ex * ex + ey * ey)
            val cov = (hw + 0.5 - d).coerceIn(0.0, 1.0)
            if (cov > 0) blend(x, y, color, cov)
        }
    }

    /** Even-odd scanline polygon fill with 4 vertical sub-samples and exact horizontal coverage. */
    fun polygon(xs: DoubleArray, ys: DoubleArray, color: Int) {
        val n = xs.size
        if (n < 3) return
        var minY = Double.MAX_VALUE; var maxY = -Double.MAX_VALUE
        for (v in ys) { minY = min(minY, v); maxY = max(maxY, v) }
        val sub = 4
        val cov = DoubleArray(w)
        val xsec = DoubleArray(n)
        for (y in max(0, floor(minY).toInt())..min(h - 1, floor(maxY).toInt())) {
            cov.fill(0.0)
            var any = false
            for (s in 0 until sub) {
                val sy = y + (s + 0.5) / sub
                var k = 0
                for (i in 0 until n) {
                    val j = if (i == n - 1) 0 else i + 1
                    val ya = ys[i]; val yb = ys[j]
                    if ((ya <= sy && yb > sy) || (yb <= sy && ya > sy)) {
                        xsec[k++] = xs[i] + (sy - ya) / (yb - ya) * (xs[j] - xs[i])
                    }
                }
                xsec.sort(0, k)
                var m = 0
                while (m + 1 < k) {
                    val a = max(0.0, xsec[m]); val b = min(w.toDouble(), xsec[m + 1])
                    if (b > a) {
                        any = true
                        val ia = floor(a).toInt(); val ib = min(w - 1, floor(b).toInt())
                        for (x in ia..ib) {
                            val l = max(a, x.toDouble()); val r = min(b, x + 1.0)
                            if (r > l) cov[x] += (r - l) / sub
                        }
                    }
                    m += 2
                }
            }
            if (any) for (x in 0 until w) if (cov[x] > 0) blend(x, y, color, min(1.0, cov[x]))
        }
    }

    fun text(x: Int, y: Int, s: String, scale: Int, color: Int) {
        var cx = x
        for (ch in s) {
            val g = BitmapFont.glyph(ch)
            for (c in 0 until 5) for (r in 0 until 7) if (((g[c].toInt() shr r) and 1) != 0) rect(cx + c * scale, y + r * scale, cx + (c + 1) * scale, y + (r + 1) * scale, color)
            cx += 6 * scale
        }
    }

    /** Text rotated 90 degrees counter-clockwise (reads bottom to top); (x, y) is the bottom-left start. */
    fun textUp(x: Int, y: Int, s: String, scale: Int, color: Int) {
        var t = 0
        for (ch in s) {
            val g = BitmapFont.glyph(ch)
            for (c in 0 until 5) for (r in 0 until 7) if (((g[c].toInt() shr r) and 1) != 0) {
                val sx = x + r * scale
                val sy = y - (t + c + 1) * scale
                rect(sx, sy, sx + scale, sy + scale, color)
            }
            t += 6
        }
    }
}

internal object BitmapFont {
    /** Classic 5x7 font, ASCII 32..126, 5 columns per glyph, bit 0 = top row. */
    private const val DATA =
        "00,00,00,00,00;00,00,5F,00,00;00,07,00,07,00;14,7F,14,7F,14;24,2A,7F,2A,12;23,13,08,64,62;36,49,55,22,50;00,05,03,00,00;" +
            "00,1C,22,41,00;00,41,22,1C,00;14,08,3E,08,14;08,08,3E,08,08;00,50,30,00,00;08,08,08,08,08;00,60,60,00,00;20,10,08,04,02;" +
            "3E,51,49,45,3E;00,42,7F,40,00;42,61,51,49,46;21,41,45,4B,31;18,14,12,7F,10;27,45,45,45,39;3C,4A,49,49,30;01,71,09,05,03;" +
            "36,49,49,49,36;06,49,49,29,1E;00,36,36,00,00;00,56,36,00,00;08,14,22,41,00;14,14,14,14,14;00,41,22,14,08;02,01,51,09,06;" +
            "32,49,79,41,3E;7E,11,11,11,7E;7F,49,49,49,36;3E,41,41,41,22;7F,41,41,22,1C;7F,49,49,49,41;7F,09,09,09,01;3E,41,49,49,7A;" +
            "7F,08,08,08,7F;00,41,7F,41,00;20,40,41,3F,01;7F,08,14,22,41;7F,40,40,40,40;7F,02,0C,02,7F;7F,04,08,10,7F;3E,41,41,41,3E;" +
            "7F,09,09,09,06;3E,41,51,21,5E;7F,09,19,29,46;46,49,49,49,31;01,01,7F,01,01;3F,40,40,40,3F;1F,20,40,20,1F;3F,40,38,40,3F;" +
            "63,14,08,14,63;07,08,70,08,07;61,51,49,45,43;00,7F,41,41,00;02,04,08,10,20;00,41,41,7F,00;04,02,01,02,04;40,40,40,40,40;" +
            "00,01,02,04,00;20,54,54,54,78;7F,48,44,44,38;38,44,44,44,20;38,44,44,48,7F;38,54,54,54,18;08,7E,09,01,02;08,14,54,54,3C;" +
            "7F,08,04,04,78;00,44,7D,40,00;20,40,44,3D,00;7F,10,28,44,00;00,41,7F,40,00;7C,04,18,04,78;7C,08,04,04,78;38,44,44,44,38;" +
            "7C,14,14,14,08;08,14,14,18,7C;7C,08,04,04,08;48,54,54,54,20;04,3F,44,40,20;3C,40,40,20,7C;1C,20,40,20,1C;3C,40,30,40,3C;" +
            "44,28,10,28,44;0C,50,50,50,3C;44,64,54,4C,44;00,08,36,41,00;00,00,7F,00,00;00,41,36,08,00;10,08,08,10,08"

    private val glyphs: Array<ByteArray> = DATA.split(';').map { g -> g.split(',').map { it.toInt(16).toByte() }.toByteArray() }.toTypedArray()

    private const val FOLD_FROM = "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ"
    private const val FOLD_TO = "acelnoszzACELNOSZZ"

    fun glyph(ch: Char): ByteArray {
        val f = fold(ch)
        val code = f.code
        return if (code in 32..126) glyphs[code - 32] else glyphs['?'.code - 32]
    }

    fun fold(ch: Char): Char {
        val i = FOLD_FROM.indexOf(ch)
        return if (i >= 0) FOLD_TO[i] else if (ch == '–' || ch == '−') '-' else ch
    }

    val count: Int get() = glyphs.size
}

/** Rasterises the terrain chart (same content as in the PDF) to a PNG. */
object PlannerProfileImage {
    private const val BG = 0xFFFFFFFF.toInt()
    private const val INK = 0xFF222222.toInt()
    private const val GRID = 0xFFDDDDDD.toInt()
    private const val FRESNEL = 0xFFBFD9F2.toInt()
    private const val TERRAIN = 0xFFB89F78.toInt()
    private const val TERRAIN_LINE = 0xFF6B5636.toInt()
    private const val LOS = 0xFFD32F2F.toInt()

    fun render(profile: ReportProfile, width: Int = 1200, height: Int = 600): ByteArray {
        val cv = Canvas(width, height, BG)
        val s = if (width >= 900) 2 else 1
        val chart = ChartData(profile)
        val left = 12 * s * 3 + 10
        val right = 40
        val top = 22 * s + 14
        val bottom = 8 * s * 3 + 10
        val pw = max(10, width - left - right)
        val ph = max(10, height - top - bottom)
        val px0 = left.toDouble(); val py0 = top.toDouble()
        fun sx(v: Double) = px0 + (v - chart.xMin) / (chart.xMax - chart.xMin) * pw
        fun sy(v: Double) = py0 + ph - (v - chart.yMin) / (chart.yMax - chart.yMin) * ph

        val xt = chart.xTicks(); val yt = chart.yTicks()
        val xd = ChartData.decimalsFor(xt); val yd = ChartData.decimalsFor(yt)
        for (t in yt) cv.line(px0, sy(t), px0 + pw, sy(t), 1.0, GRID)
        for (t in xt) cv.line(sx(t), py0, sx(t), py0 + ph, 1.0, GRID)

        if (chart.valid) {
            val n = chart.n
            val fx = DoubleArray(2 * n); val fy = DoubleArray(2 * n)
            for (i in 0 until n) { fx[i] = sx(chart.x[i]); fy[i] = sy(chart.up[i]) }
            for (i in 0 until n) { fx[n + i] = sx(chart.x[n - 1 - i]); fy[n + i] = sy(chart.lo[n - 1 - i]) }
            cv.polygon(fx, fy, FRESNEL)
            val gx = DoubleArray(n + 2); val gy = DoubleArray(n + 2)
            for (i in 0 until n) { gx[i] = sx(chart.x[i]); gy[i] = sy(chart.ground[i]) }
            gx[n] = gx[n - 1]; gy[n] = py0 + ph
            gx[n + 1] = gx[0]; gy[n + 1] = py0 + ph
            cv.polygon(gx, gy, TERRAIN)
            for (i in 0 until n - 1) cv.line(gx[i], gy[i], gx[i + 1], gy[i + 1], 1.6, TERRAIN_LINE)
            for (i in 0 until n - 1) cv.line(sx(chart.x[i]), sy(chart.los[i]), sx(chart.x[i + 1]), sy(chart.los[i + 1]), 2.2, LOS)
        }

        // axes + ticks
        cv.line(px0, py0, px0, py0 + ph, 1.5, INK)
        cv.line(px0, py0 + ph, px0 + pw, py0 + ph, 1.5, INK)
        for (t in xt) {
            val x = sx(t)
            cv.line(x, py0 + ph, x, py0 + ph + 5, 1.5, INK)
            val lab = Num.fmt(t, xd)
            cv.text((x - lab.length * 6 * s / 2.0).toInt(), (py0 + ph + 9).toInt(), lab, s, INK)
        }
        for (t in yt) {
            val y = sy(t)
            cv.line(px0 - 5, y, px0, y, 1.5, INK)
            val lab = Num.fmt(t, yd)
            cv.text((px0 - 9 - lab.length * 6 * s).toInt(), (y - 3.5 * s).toInt(), lab, s, INK)
        }
        // axis labels
        val xl = profile.xAxisLabel
        cv.text((px0 + pw / 2 - xl.length * 6 * s / 2.0).toInt(), height - 7 * s - 8, xl, s, INK)
        val yl = profile.yAxisLabel
        cv.textUp(8, (py0 + ph / 2 + yl.length * 6 * s / 2.0).toInt(), yl, s, INK)
        // A / B end labels
        val ls = s + 1
        cv.text(px0.toInt(), (py0 - 7 * ls - 4).toInt(), profile.labelA, ls, INK)
        cv.text((px0 + pw - profile.labelB.length * 6 * ls).toInt(), (py0 - 7 * ls - 4).toInt(), profile.labelB, ls, INK)
        return PlannerPng.encode(width, height, cv.px)
    }
}
