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

import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.model.fixOrNull
import org.meshtastic.core.model.util.GeoConstants
import org.meshtastic.core.ui.util.snrRgb
import org.meshtastic.proto.Position
import kotlin.math.roundToInt
import kotlin.time.Instant

/**
 * A KML file with the node's track coloured by the SNR each position was heard with, for loading as a map layer.
 *
 * Every leg is a line in the colour of its two ends' mean SNR, and every position a dot with its value, so the file
 * shows where the signal was strong and where it fell away. A position with no direct measurement is grey.
 *
 * Coordinates and values are formatted invariantly: KML is XML another program parses, and a comma decimal breaks it.
 * Styles are shared per whole dB, so a long track does not write one style per point.
 */
internal fun buildCoverageKml(positions: List<Position>, snrByTime: Map<Int, Float>, name: String): String {
    val points =
        positions
            .sortedBy { it.time }
            .mapNotNull { pos ->
                val (latI, lonI) = pos.fixOrNull() ?: return@mapNotNull null
                KmlPoint(
                    lat = latI * GeoConstants.DEG_D,
                    lon = lonI * GeoConstants.DEG_D,
                    time = pos.time,
                    snr = snrByTime[pos.time],
                )
            }
    val buckets =
        (points.map { snrBucket(it.snr) } + points.zipWithNext { a, b -> snrBucket(legSnr(a, b)) }).distinct()
    return buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n<Document>\n")
        append("  <name>${(name.ifBlank { "SNR" } + " - SNR").xmlEscaped()}</name>\n")
        for (bucket in buckets) {
            val color = kmlColor(snrRgb(bucket?.toFloat()))
            append("  <Style id=\"${styleId(bucket)}\">")
            append("<LineStyle><color>$color</color><width>4</width></LineStyle>")
            append("<IconStyle><color>$color</color><scale>0.6</scale>")
            append("<Icon><href>http://maps.google.com/mapfiles/kml/shapes/shaded_dot.png</href></Icon></IconStyle>")
            append("</Style>\n")
        }
        append("  <Folder><name>SNR dB</name>\n")
        for (i in 1 until points.size) {
            val (a, b) = points[i - 1] to points[i]
            append("    <Placemark><styleUrl>#${styleId(snrBucket(legSnr(a, b)))}</styleUrl>")
            append("<LineString><tessellate>1</tessellate><coordinates>${a.coordinates()} ${b.coordinates()}")
            append("</coordinates></LineString></Placemark>\n")
        }
        append("  </Folder>\n  <Folder><name>GPS</name>\n")
        for (p in points) {
            val label = p.snr?.let { "${NumberFormatter.formatInvariant(it, 1)} dB" } ?: "-"
            append("    <Placemark><name>$label</name>")
            if (p.time > 0) append("<TimeStamp><when>${Instant.fromEpochSeconds(p.time.toLong())}</when></TimeStamp>")
            append("<styleUrl>#${styleId(snrBucket(p.snr))}</styleUrl>")
            append("<Point><coordinates>${p.coordinates()}</coordinates></Point></Placemark>\n")
        }
        append("  </Folder>\n</Document>\n</kml>\n")
    }
}

/** One node's measured positions for [buildNetworkCoverageKml]. */
internal class CoverageTrack(val name: String, val positions: List<Position>, val snrByTime: Map<Int, Float>)

/**
 * A KML file with the measurements of every node together: one folder per node, one dot per position the radio heard
 * directly, coloured by SNR. Loaded as a map layer it shows where the network is heard well and where it is not.
 * Positions without a measurement are left out, since a grey dot says nothing about coverage.
 */
internal fun buildNetworkCoverageKml(tracks: List<CoverageTrack>, name: String): String {
    val perNode =
        tracks.map { track ->
            track.name to
                track.positions
                    .sortedBy { it.time }
                    .mapNotNull { pos ->
                        val snr = track.snrByTime[pos.time] ?: return@mapNotNull null
                        val (latI, lonI) = pos.fixOrNull() ?: return@mapNotNull null
                        KmlPoint(lat = latI * GeoConstants.DEG_D, lon = lonI * GeoConstants.DEG_D, time = pos.time, snr = snr)
                    }
        }
    val buckets = perNode.flatMap { (_, points) -> points.map { snrBucket(it.snr) } }.distinct()
    return buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n<Document>\n")
        append("  <name>${name.xmlEscaped()}</name>\n")
        for (bucket in buckets) {
            val color = kmlColor(snrRgb(bucket?.toFloat()))
            append("  <Style id=\"${styleId(bucket)}\"><IconStyle><color>$color</color><scale>0.6</scale>")
            append("<Icon><href>http://maps.google.com/mapfiles/kml/shapes/shaded_dot.png</href></Icon></IconStyle></Style>\n")
        }
        for ((nodeName, points) in perNode) {
            if (points.isEmpty()) continue
            append("  <Folder><name>${nodeName.xmlEscaped()}</name>\n")
            for (p in points) {
                val label = p.snr?.let { "${NumberFormatter.formatInvariant(it, 1)} dB" } ?: "-"
                append("    <Placemark><name>$label</name>")
                if (p.time > 0) append("<TimeStamp><when>${Instant.fromEpochSeconds(p.time.toLong())}</when></TimeStamp>")
                append("<styleUrl>#${styleId(snrBucket(p.snr))}</styleUrl>")
                append("<Point><coordinates>${p.coordinates()}</coordinates></Point></Placemark>\n")
            }
            append("  </Folder>\n")
        }
        append("</Document>\n</kml>\n")
    }
}

private class KmlPoint(val lat: Double, val lon: Double, val time: Int, val snr: Float?) {
    fun coordinates(): String {
        val x = NumberFormatter.formatInvariant(lon, COORDINATE_DECIMALS)
        val y = NumberFormatter.formatInvariant(lat, COORDINATE_DECIMALS)
        return "$x,$y,0"
    }
}

/** A leg takes the mean of its two ends, or the one end that has a measurement. */
private fun legSnr(a: KmlPoint, b: KmlPoint): Float? =
    if (a.snr != null && b.snr != null) (a.snr + b.snr) / 2f else a.snr ?: b.snr

private fun snrBucket(snr: Float?): Int? = snr?.takeUnless { it.isNaN() }?.roundToInt()

private fun styleId(bucket: Int?): String =
    if (bucket == null) "snr_none" else "snr_${bucket.toString().replace('-', 'm')}"

/** KML colours are `aabbggrr`, not `rrggbb`. */
internal fun kmlColor(rgb: Int): String {
    val r = (rgb shr RED_SHIFT) and CHANNEL_MASK
    val g = (rgb shr GREEN_SHIFT) and CHANNEL_MASK
    val b = rgb and CHANNEL_MASK
    return "ff" + hex2(b) + hex2(g) + hex2(r)
}

private fun hex2(value: Int): String = value.toString(HEX_RADIX).padStart(2, '0')

private fun String.xmlEscaped(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

private const val COORDINATE_DECIMALS = 7
private const val CHANNEL_MASK = 0xFF
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val HEX_RADIX = 16
