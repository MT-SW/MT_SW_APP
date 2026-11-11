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
@file:Suppress("MagicNumber", "ReturnCount", "SwallowedException", "TooGenericExceptionCaught", "ArrayInDataClass")

package org.meshtastic.feature.map.propagation

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.meshtastic.feature.map.planner.Atmosphere
import org.meshtastic.feature.map.planner.AtmosphereSample
import kotlin.math.abs

/** A rectangle on the map, in degrees. */
data class GeoBounds(val south: Double, val west: Double, val north: Double, val east: Double) {
    val latSpan: Double
        get() = north - south

    val lonSpan: Double
        get() = east - west

    fun contains(other: GeoBounds): Boolean =
        other.south >= south && other.north <= north && other.west >= west && other.east <= east
}

/**
 * A forecast of how strongly the air bends radio waves over [bounds], hour after hour.
 *
 * Every value is the sharpest vertical drop of radio refractivity (N-units per km) in the lowest 3 km of the air: about
 * -40 is normal, below -79 the signal carries further than usual, below -157 a duct can trap it. The grid has [rows]
 * rows (south to north) and [cols] columns (west to east); [gradients] holds [hours] such grids one after another.
 * [startMillis] is the start of the first forecast hour.
 */
class TropoGrid(
    val bounds: GeoBounds,
    val rows: Int,
    val cols: Int,
    val hours: Int,
    val startMillis: Long,
    val fetchedAtMillis: Long,
    val gradients: FloatArray,
) {
    /** Which forecast hour [nowMillis] falls in, kept inside the forecast. */
    fun hourAt(nowMillis: Long): Int = ((nowMillis - startMillis) / MS_PER_HOUR).toInt().coerceIn(0, hours - 1)

    /** The gradient at a place, blended between the four nearest grid points. */
    fun sample(hour: Int, lat: Double, lon: Double): Float {
        val fr = ((lat - bounds.south) / bounds.latSpan * (rows - 1)).coerceIn(0.0, (rows - 1).toDouble())
        val fc = ((lon - bounds.west) / bounds.lonSpan * (cols - 1)).coerceIn(0.0, (cols - 1).toDouble())
        val r0 = fr.toInt().coerceAtMost(rows - 2)
        val c0 = fc.toInt().coerceAtMost(cols - 2)
        val tr = (fr - r0).toFloat()
        val tc = (fc - c0).toFloat()
        val base = hour.coerceIn(0, hours - 1) * rows * cols
        fun at(r: Int, c: Int) = gradients[base + r * cols + c]
        val low = at(r0, c0) * (1 - tc) + at(r0, c0 + 1) * tc
        val high = at(r0 + 1, c0) * (1 - tc) + at(r0 + 1, c0 + 1) * tc
        return low * (1 - tr) + high * tr
    }

    /** One line of text per part, so the forecast can be kept on the device and read back without the internet. */
    fun encode(): String = buildString {
        append("v1 ${bounds.south} ${bounds.west} ${bounds.north} ${bounds.east} $rows $cols $hours ")
        append("$startMillis $fetchedAtMillis\n")
        append(gradients.joinToString(",") { ((it * 10).toInt() / 10f).toString() })
    }

    companion object {
        const val MS_PER_HOUR = 3_600_000L

        fun decode(text: String): TropoGrid? = try {
            val lines = text.lines()
            val head = lines[0].split(' ')
            if (head[0] != "v1") return null
            val bounds = GeoBounds(head[1].toDouble(), head[2].toDouble(), head[3].toDouble(), head[4].toDouble())
            val rows = head[5].toInt()
            val cols = head[6].toInt()
            val hours = head[7].toInt()
            val values = lines[1].split(',').map { it.toFloat() }.toFloatArray()
            if (values.size != rows * cols * hours || rows < 2 || cols < 2) {
                null
            } else {
                TropoGrid(bounds, rows, cols, hours, head[8].toLong(), head[9].toLong(), values)
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Downloads the forecast from Open-Meteo (https://open-meteo.com, data under CC BY 4.0; it serves national weather
 * services' models), for a grid of points over the visible map, and turns the temperature, humidity and pressure of the
 * air at several heights into refractivity gradients.
 */
class TropoForecastSource(private val httpClient: HttpClient) {

    suspend fun fetch(bounds: GeoBounds, nowMillis: Long, rows: Int = GRID, cols: Int = GRID): TropoGrid? {
        val lats = List(rows) { bounds.south + bounds.latSpan * it / (rows - 1) }
        val lons = List(cols) { bounds.west + bounds.lonSpan * it / (cols - 1) }
        val points = buildList { for (lat in lats) for (lon in lons) add(lat to lon) }
        val body =
            try {
                withTimeoutOrNull(TIMEOUT_MS) {
                    val response = httpClient.get(buildUrl(points))
                    if (response.status.isSuccess()) response.bodyAsText() else null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            } ?: return null
        val start = nowMillis / TropoGrid.MS_PER_HOUR * TropoGrid.MS_PER_HOUR
        return try {
            parse(body, bounds, rows, cols, start, nowMillis)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
        const val GRID = 6
        const val HOURS = 24
        const val TIMEOUT_MS = 30_000L
        private val LEVELS = listOf(1000, 975, 950, 925, 900, 850)
        private const val SURFACE_ABOVE_GROUND_M = 2.0
        private const val MIN_LAYER_M = 50.0
        private const val MIN_ABOVE_GROUND_M = 100.0
        private const val TOP_ABOVE_GROUND_M = 3000.0
        private const val STANDARD_GRADIENT = -39.2

        internal fun buildUrl(points: List<Pair<Double, Double>>): String {
            val sb = StringBuilder(BASE_URL)
            sb.append("?latitude=").append(points.joinToString(",") { round4(it.first) })
            sb.append("&longitude=").append(points.joinToString(",") { round4(it.second) })
            sb.append("&hourly=temperature_2m,relative_humidity_2m,surface_pressure")
            for (l in LEVELS) {
                sb.append(",temperature_").append(l).append("hPa")
                sb.append(",relative_humidity_").append(l).append("hPa")
                sb.append(",geopotential_height_").append(l).append("hPa")
            }
            sb.append("&forecast_hours=").append(HOURS).append("&timezone=GMT")
            return sb.toString()
        }

        private fun round4(v: Double): String = ((v * 10_000).toLong() / 10_000.0).toString()

        internal fun parse(
            body: String,
            bounds: GeoBounds,
            rows: Int,
            cols: Int,
            startMillis: Long,
            fetchedAtMillis: Long,
        ): TropoGrid? {
            val root: JsonElement = Json.parseToJsonElement(body)
            val locations: List<JsonObject> =
                if (root is JsonArray) root.map { it.jsonObject } else listOf(root.jsonObject)
            if (locations.size != rows * cols) return null
            val values = FloatArray(rows * cols * HOURS) { STANDARD_GRADIENT.toFloat() }
            locations.forEachIndexed { index, location ->
                val elevation = location["elevation"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val hourly = location["hourly"]?.jsonObject ?: return null
                for (h in 0 until HOURS) {
                    values[h * rows * cols + index] = gradientFor(hourly, h, elevation).toFloat()
                }
            }
            return TropoGrid(bounds, rows, cols, HOURS, startMillis, fetchedAtMillis, values)
        }

        private fun JsonObject.number(key: String, hour: Int): Double? =
            (this[key]?.jsonArray?.getOrNull(hour) as? JsonPrimitive)?.doubleOrNull

        /** The sharpest drop of refractivity between neighbouring heights of one place at one hour. */
        private fun gradientFor(hourly: JsonObject, hour: Int, elevationM: Double): Double {
            val samples = mutableListOf<AtmosphereSample>()
            val t2 = hourly.number("temperature_2m", hour)
            val rh2 = hourly.number("relative_humidity_2m", hour)
            val p0 = hourly.number("surface_pressure", hour)
            if (t2 != null && rh2 != null && p0 != null) {
                samples += AtmosphereSample(elevationM + SURFACE_ABOVE_GROUND_M, t2, p0, rh2)
            }
            for (level in LEVELS) {
                val t = hourly.number("temperature_${level}hPa", hour)
                val rh = hourly.number("relative_humidity_${level}hPa", hour)
                val gh = hourly.number("geopotential_height_${level}hPa", hour)
                if (t != null && rh != null && gh != null && gh > elevationM + MIN_ABOVE_GROUND_M) {
                    samples += AtmosphereSample(gh, t, level.toDouble(), rh)
                }
            }
            val sorted = samples.sortedBy { it.heightM }
            var worst: Double? = null
            for (i in 0 until sorted.size - 1) {
                val a = sorted[i]
                val b = sorted[i + 1]
                if (abs(b.heightM - a.heightM) < MIN_LAYER_M || b.heightM > elevationM + TOP_ABOVE_GROUND_M) continue
                val g =
                    Atmosphere.gradientNPerKm(
                        Atmosphere.refractivityN(a.tempC, a.pressureHpa, a.rhPct),
                        a.heightM,
                        Atmosphere.refractivityN(b.tempC, b.pressureHpa, b.rhPct),
                        b.heightM,
                    )
                worst = minOf(worst ?: g, g)
            }
            return (worst ?: STANDARD_GRADIENT).coerceIn(-500.0, 100.0)
        }
    }
}
