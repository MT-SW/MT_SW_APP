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
@file:Suppress("ComplexCondition", "MagicNumber")

package org.meshtastic.feature.map.survey

import org.meshtastic.core.model.SignalSample

private const val NODE_ID_DIGITS = 8
private const val HEX = 16

internal const val SURVEY_CSV_HEADER = "time_ms,latitude,longitude,accuracy_m,from,rssi,snr,direct"

/** One row per reading. Carries the phone's route, so it is only ever written where the user chooses. */
internal fun List<SignalSample>.toSurveyCsv(): String = buildString {
    appendLine(SURVEY_CSV_HEADER)
    this@toSurveyCsv.forEach { sample ->
        val from = "!" + sample.fromNum.toUInt().toString(HEX).padStart(NODE_ID_DIGITS, '0')
        appendLine(
            listOf(
                sample.timeMillis,
                sample.latitude,
                sample.longitude,
                sample.accuracyM ?: "",
                from,
                sample.rssi,
                sample.snr ?: "",
                sample.direct,
            )
                .joinToString(","),
        )
    }
}

private const val CSV_COLUMNS = 8

/**
 * The readings in [text], which is the file [toSurveyCsv] writes (a hike's exported signal has the same columns). A row
 * that does not fit is skipped, so a file edited by hand or cut short still brings in what is readable.
 */
internal fun parseSurveyCsv(text: String): List<SignalSample> = text
    .lineSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("time_ms") }
    .mapNotNull { line ->
        val c = line.split(',')
        if (c.size < CSV_COLUMNS) return@mapNotNull null
        val node = c[4].trim().removePrefix("!").toUIntOrNull(HEX)?.toInt()
        val time = c[0].trim().toLongOrNull()
        val lat = c[1].trim().toDoubleOrNull()
        val lon = c[2].trim().toDoubleOrNull()
        val rssi = c[5].trim().toIntOrNull()
        if (node == null || time == null || lat == null || lon == null || rssi == null) return@mapNotNull null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return@mapNotNull null
        SignalSample(
            timeMillis = time,
            latitude = lat,
            longitude = lon,
            accuracyM = c[3].trim().toFloatOrNull(),
            fromNum = node,
            rssi = rssi,
            snr = c[6].trim().toFloatOrNull(),
            direct = c[7].trim() != "false",
        )
    }
    .toList()
