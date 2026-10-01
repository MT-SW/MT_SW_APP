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
package org.meshtastic.feature.map.planner.data

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
import kotlinx.serialization.json.jsonObject
import org.meshtastic.feature.map.planner.Atmosphere
import org.meshtastic.feature.map.planner.AtmosphereAnalysis
import org.meshtastic.feature.map.planner.AtmosphereSample
import org.meshtastic.feature.map.planner.DuctingLevel
import org.meshtastic.feature.map.planner.export.Num

/**
 * Propagation-relevant weather at a point. [fetchedAtIso] is the UTC time of the data, "yyyy-MM-dd HH:mm".
 * [analysis] holds k-factor, ITM sea-level refractivity and ducting level derived from the vertical profile.
 */
data class PropagationConditions(
    val fetchedAtIso: String,
    val source: String = "Open-Meteo",
    val analysis: AtmosphereAnalysis,
    val tempC: Double,
    val pressureHpa: Double,
    val rhPct: Double,
    val rainMmH: Double,
    val level: DuctingLevel,
)

/** Why weather could not be obtained (the planner then falls back to k = 4/3, N0 = 301). */
enum class PlannerWeatherError {
    /** No connection or timeout. */
    OFFLINE,

    /** The server answered with an HTTP error. */
    HTTP_ERROR,

    /** The answer could not be understood. */
    BAD_RESPONSE,
}

sealed interface PlannerWeatherResult {
    data class Success(val conditions: PropagationConditions) : PlannerWeatherResult

    data class Failure(val error: PlannerWeatherError) : PlannerWeatherResult
}

/** Weather provider for the planner (fake in tests). */
interface PlannerWeatherSource {
    suspend fun fetch(lat: Double, lon: Double): PlannerWeatherResult
}

/** Open-Meteo (https://open-meteo.com, CC BY 4.0) forecast API client. */
class PlannerWeather(
    private val httpClient: HttpClient,
    private val baseUrl: String = BASE_URL,
    private val timeoutMs: Long = TIMEOUT_MS,
) : PlannerWeatherSource {

    override suspend fun fetch(lat: Double, lon: Double): PlannerWeatherResult {
        val url = buildUrl(baseUrl, lat, lon)
        val body: String? = try {
            withTimeoutOrNull(timeoutMs) {
                val response = httpClient.get(url)
                if (response.status.isSuccess()) response.bodyAsText() else null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (body == null) return PlannerWeatherResult.Failure(PlannerWeatherError.OFFLINE)
        val parsed = try {
            parseOpenMeteo(body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        return if (parsed == null) {
            PlannerWeatherResult.Failure(PlannerWeatherError.BAD_RESPONSE)
        } else {
            PlannerWeatherResult.Success(parsed)
        }
    }

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
        const val TIMEOUT_MS = 15_000L

        /** Pressure levels (hPa) requested in addition to the surface values. */
        val PRESSURE_LEVELS: List<Int> = listOf(925, 850, 700)

        internal fun buildUrl(baseUrl: String, lat: Double, lon: Double): String {
            val sb = StringBuilder(baseUrl)
            sb.append("?latitude=").append(Num.fmt(lat, 4))
            sb.append("&longitude=").append(Num.fmt(lon, 4))
            sb.append("&current=temperature_2m,relative_humidity_2m,surface_pressure,precipitation")
            sb.append("&hourly=precipitation")
            for (l in PRESSURE_LEVELS) {
                sb.append(",temperature_").append(l).append("hPa")
                sb.append(",relative_humidity_").append(l).append("hPa")
                sb.append(",geopotential_height_").append(l).append("hPa")
            }
            sb.append("&timezone=GMT&forecast_days=1")
            return sb.toString()
        }
    }
}

private const val MIN_LEVEL_ABOVE_GROUND_M = 20.0

private fun JsonElement?.num(): Double? {
    val p = this as? JsonPrimitive ?: return null
    return p.doubleOrNull
}

private fun JsonObject.number(key: String): Double? = this[key].num()

private fun JsonObject.hourly(key: String, index: Int): Double? {
    val arr = this[key] as? JsonArray ?: return null
    if (index < 0 || index >= arr.size) return null
    return arr[index].num()
}

/**
 * Parses an Open-Meteo forecast answer (request with `timezone=GMT`, `current=...`, hourly pressure-level variables
 * for 925/850/700 hPa) into [PropagationConditions]; null when the surface values are missing or the JSON is invalid.
 *
 * The hourly entry used is the one matching the hour of `current.time`; pressure levels lying below ground
 * (geopotential height under station height + 20 m) or with missing values are ignored, and without any usable level
 * the standard atmosphere is assumed by [Atmosphere.analyzeProfile].
 */
fun parseOpenMeteo(json: String): PropagationConditions? {
    val root = try {
        Json.parseToJsonElement(json).jsonObject
    } catch (e: Exception) {
        return null
    }
    val current = root["current"] as? JsonObject ?: return null
    val temp = current.number("temperature_2m") ?: return null
    val pressure = current.number("surface_pressure") ?: return null
    val rh = current.number("relative_humidity_2m") ?: return null
    val timeRaw = (current["time"] as? JsonPrimitive)?.content ?: return null
    if (timeRaw.length < 16) return null
    val timeIso = timeRaw.substring(0, 16).replace('T', ' ')
    val hourKey = timeRaw.substring(0, 13) + ":00"
    val station = root.number("elevation") ?: 0.0

    val hourlyObj = root["hourly"] as? JsonObject
    var index = -1
    if (hourlyObj != null) {
        val times = hourlyObj["time"] as? JsonArray
        if (times != null) {
            for (i in 0 until times.size) {
                val t = (times[i] as? JsonPrimitive)?.content
                if (t == hourKey) {
                    index = i
                    break
                }
            }
        }
    }

    val rain = (if (hourlyObj != null && index >= 0) hourlyObj.hourly("precipitation", index) else null)
        ?: current.number("precipitation")
        ?: 0.0

    val samples = ArrayList<AtmosphereSample>()
    samples.add(AtmosphereSample(station, temp, pressure, rh))
    if (hourlyObj != null && index >= 0) {
        for (level in PlannerWeather.PRESSURE_LEVELS) {
            val t = hourlyObj.hourly("temperature_${level}hPa", index)
            val r = hourlyObj.hourly("relative_humidity_${level}hPa", index)
            val z = hourlyObj.hourly("geopotential_height_${level}hPa", index)
            if (t == null || r == null || z == null) continue
            if (z < station + MIN_LEVEL_ABOVE_GROUND_M) continue
            samples.add(AtmosphereSample(z, t, level.toDouble(), r))
        }
    }
    val analysis = Atmosphere.analyzeProfile(samples)
    return PropagationConditions(
        fetchedAtIso = timeIso,
        analysis = analysis,
        tempC = temp,
        pressureHpa = pressure,
        rhPct = rh,
        rainMmH = rain,
        level = analysis.level,
    )
}
