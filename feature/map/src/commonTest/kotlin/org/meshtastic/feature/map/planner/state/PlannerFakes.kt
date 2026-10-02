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
package org.meshtastic.feature.map.planner.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.meshtastic.feature.map.planner.Atmosphere
import org.meshtastic.feature.map.planner.AtmosphereSample
import org.meshtastic.feature.map.planner.GeoPoint
import org.meshtastic.feature.map.planner.PathProfile
import org.meshtastic.feature.map.planner.data.ClutterKind
import org.meshtastic.feature.map.planner.data.ClutterMap
import org.meshtastic.feature.map.planner.data.ClutterPolygon
import org.meshtastic.feature.map.planner.data.PlannerClutterException
import org.meshtastic.feature.map.planner.data.PlannerClutterFailure
import org.meshtastic.feature.map.planner.data.PlannerClutterSource
import org.meshtastic.feature.map.planner.data.PlannerElevationException
import org.meshtastic.feature.map.planner.data.PlannerElevationFailure
import org.meshtastic.feature.map.planner.data.PlannerElevationSource
import org.meshtastic.feature.map.planner.data.PlannerWeatherError
import org.meshtastic.feature.map.planner.data.PlannerWeatherResult
import org.meshtastic.feature.map.planner.data.PlannerWeatherSource
import org.meshtastic.feature.map.planner.data.PropagationConditions

/** Obstacle data that is empty, a fixed [map], or failing. Counts the requests. */
internal class FakeClutter(var map: ClutterMap = ClutterMap.EMPTY, var failure: PlannerClutterFailure? = null) :
    PlannerClutterSource {
    var linkCalls = 0
    var areaCalls = 0

    override suspend fun forLink(a: GeoPoint, b: GeoPoint): ClutterMap {
        linkCalls++
        failure?.let { throw PlannerClutterException(it) }
        return map
    }

    override suspend fun forArea(center: GeoPoint, radiusKm: Double): ClutterMap {
        areaCalls++
        failure?.let { throw PlannerClutterException(it) }
        return map
    }
}

/** A forest covering everything around the test link (the antennas' surroundings are cleared by the engine). */
internal fun forestAroundLink(): ClutterMap = ClutterMap(
    listOf(
        ClutterPolygon(
            ClutterKind.FOREST,
            null,
            doubleArrayOf(50.80, 20.55, 50.80, 20.98, 50.93, 20.98, 50.93, 20.55, 50.80, 20.55),
        ),
    ),
)

/** Flat terrain at [heightM]; optionally failing. */
internal class FakeElevation(var heightM: Double = 250.0, var failure: PlannerElevationFailure? = null) :
    PlannerElevationSource {
    var profileCalls = 0

    private fun check() {
        val f = failure
        if (f != null) throw PlannerElevationException(f)
    }

    override suspend fun profile(a: GeoPoint, b: GeoPoint): PathProfile {
        profileCalls++
        check()
        return PathProfile.fromSampler(a, b) { _, _ -> heightM }
    }

    override suspend fun prepareArea(center: GeoPoint, radiusKm: Double): (Double, Double) -> Double {
        check()
        val h = heightM
        return { _, _ -> h }
    }

    override suspend fun altitudeAt(lat: Double, lon: Double): Double? {
        check()
        return heightM
    }
}

internal class FakeWeather(var result: PlannerWeatherResult = PlannerWeatherResult.Failure(PlannerWeatherError.OFFLINE)) :
    PlannerWeatherSource {
    var calls = 0

    override suspend fun fetch(lat: Double, lon: Double): PlannerWeatherResult {
        calls++
        return result
    }
}

internal class FakeNodes(initial: List<PlannerNodeOption> = emptyList()) : PlannerNodeSource {
    val flow = MutableStateFlow(initial)
    override val nodes: Flow<List<PlannerNodeOption>> = flow
}

internal fun conditions(): PropagationConditions {
    val analysis = Atmosphere.analyzeProfile(
        listOf(AtmosphereSample(250.0, 10.0, 990.0, 70.0), AtmosphereSample(1250.0, 5.0, 880.0, 50.0)),
    )
    return PropagationConditions(
        fetchedAtIso = "2026-10-01 12:00",
        analysis = analysis,
        tempC = 10.0,
        pressureHpa = 990.0,
        rhPct = 70.0,
        rainMmH = 0.0,
        level = analysis.level,
    )
}

internal val POINT_A = GeoPoint(50.8661, 20.6286)
internal val POINT_B = GeoPoint(50.8661, 20.9)

internal fun endAt(p: GeoPoint, source: PlannerPointSource = PlannerPointSource.Manual, heightM: Double = 10.0) =
    PlannerEnd(pointSource = source, lat = p.lat, lon = p.lon, antennaHeightM = heightM)

internal fun nodeOption(
    num: Int,
    p: GeoPoint,
    ours: Boolean = false,
    snr: Float? = null,
    rssi: Int? = null,
    hops: Int = 0,
) = PlannerNodeOption(num, "Node $num", "N$num", p.lat, p.lon, null, snr, rssi, hops, ours)
