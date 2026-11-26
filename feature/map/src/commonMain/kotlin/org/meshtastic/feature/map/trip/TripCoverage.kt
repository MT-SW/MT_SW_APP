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
package org.meshtastic.feature.map.trip

import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.SignalSample
import org.meshtastic.feature.map.survey.toSurveyCsv

private const val MS_PER_SECOND = 1000

/** The signal saved on a hike as readings (the same kind the signal survey keeps). */
fun Trip.coverageSamples(): List<SignalSample> = signals.map {
    SignalSample(
        timeMillis = it.t,
        latitude = it.lat,
        longitude = it.lon,
        accuracyM = null,
        fromNum = it.node,
        rssi = it.rssi,
        snr = it.snr,
        direct = !it.relayed,
    )
}

/**
 * The signal of a hike as CSV, with the columns of the signal survey, so the file can be brought back (as a survey
 * session, shown on the map as a heatmap) or opened in a spreadsheet or a map program. Null when nothing was saved.
 */
fun Trip.coverageCsv(): String? = coverageSamples().takeIf { it.isNotEmpty() }?.toSurveyCsv()

/**
 * The signal of a hike as GeoJSON: one point for every reading (time, node, RSSI, SNR, whether it was heard directly)
 * and, when the hike has a route, its line. Opens in QGIS, geojson.io and most map programs. Null when nothing was
 * saved.
 */
fun Trip.coverageGeoJson(): String? {
    if (signals.isEmpty()) return null
    return buildString {
        append("{\"type\":\"FeatureCollection\",\"features\":[")
        val parts = mutableListOf<String>()
        if (points.size >= 2) {
            val line = points.joinToString(",") { "[${it.lon},${it.lat}]" }
            parts +=
                "{\"type\":\"Feature\",\"properties\":{\"kind\":\"route\"},\"geometry\":" +
                "{\"type\":\"LineString\",\"coordinates\":[$line]}}"
        }
        signals.forEach {
            parts +=
                "{\"type\":\"Feature\",\"properties\":{\"kind\":\"signal\",\"time\":${it.t / MS_PER_SECOND}," +
                "\"node\":\"${NodeAddress.numToDefaultId(it.node)}\",\"rssi\":${it.rssi},\"snr\":${it.snr}," +
                "\"direct\":${!it.relayed}},\"geometry\":{\"type\":\"Point\"," +
                "\"coordinates\":[${it.lon},${it.lat}]}}"
        }
        append(parts.joinToString(","))
        append("]}")
    }
}
