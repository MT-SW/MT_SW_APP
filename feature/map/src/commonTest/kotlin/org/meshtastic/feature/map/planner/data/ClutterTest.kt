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

import org.meshtastic.feature.map.planner.Coverage
import org.meshtastic.feature.map.planner.CoverageInput
import org.meshtastic.feature.map.planner.GeoPoint
import org.meshtastic.feature.map.planner.Geodesy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClutterTest {

    private fun way(tags: String, vararg latLon: Double): String {
        val pts = (latLon.indices step 2).joinToString(",") { """{"lat":${latLon[it]},"lon":${latLon[it + 1]}}""" }
        return """{"type":"way","id":1,"tags":{$tags},"geometry":[$pts]}"""
    }

    private fun answer(vararg elements: String) = """{"version":0.6,"elements":[${elements.joinToString(",")}]}"""

    // a 0.002 x 0.002 degree square starting at (50.0, 20.0)
    private val square = doubleArrayOf(50.0, 20.0, 50.0, 20.002, 50.002, 20.002, 50.002, 20.0, 50.0, 20.0)

    @Test
    fun buildingHeightFromLevels() {
        val m = OsmClutterParser.parse(answer(way(""""building":"yes","building:levels":"3"""", *square)))
        assertEquals(1, m.stats.buildings)
        assertEquals(10.0, m.heightAt(50.001, 20.001), 1e-9)
        assertEquals(0.0, m.heightAt(50.01, 20.001), 1e-9)
    }

    @Test
    fun explicitHeightBeatsLevels() {
        val m = OsmClutterParser.parse(
            answer(way(""""building":"house","height":"12,5 m","building:levels":"2"""", *square)),
        )
        assertEquals(12.5, m.heightAt(50.001, 20.001), 1e-9)
    }

    @Test
    fun buildingWithoutHeightUsesTheAssumption() {
        val m = OsmClutterParser.parse(answer(way(""""building":"yes"""", *square)))
        assertEquals(8.0, m.heightAt(50.001, 20.001), 1e-9)
        assertEquals(20.0, m.heightAt(50.001, 20.001, ClutterHeights(buildingM = 20.0)), 1e-9)
    }

    @Test
    fun forestFollowsTheSetting() {
        val m = OsmClutterParser.parse(answer(way(""""natural":"wood"""", *square)))
        assertEquals(1, m.stats.forests)
        assertEquals(15.0, m.heightAt(50.001, 20.001), 1e-9)
        assertEquals(25.0, m.heightAt(50.001, 20.001, ClutterHeights(forestM = 25.0)), 1e-9)
    }

    @Test
    fun areasAndUnknownTags() {
        val m = OsmClutterParser.parse(
            answer(
                way(""""landuse":"residential"""", *square),
                way(""""landuse":"meadow"""", 51.0, 21.0, 51.0, 21.1, 51.1, 21.1, 51.1, 21.0, 51.0, 21.0),
            ),
        )
        assertEquals(1, m.polygons.size)
        assertEquals(1, m.stats.areas)
        assertEquals(8.0, m.heightAt(50.001, 20.001), 1e-9)
    }

    @Test
    fun tallestOverlappingObstacleWins() {
        val m = OsmClutterParser.parse(
            answer(
                way(""""natural":"wood"""", *square),
                way(""""building":"yes","height":"30"""", *square),
            ),
        )
        assertEquals(30.0, m.heightAt(50.001, 20.001), 1e-9)
    }

    @Test
    fun openWayIsIgnored() {
        val m = OsmClutterParser.parse(answer(way(""""building":"yes"""", 50.0, 20.0, 50.0, 20.002, 50.002, 20.002)))
        assertTrue(m.isEmpty())
    }

    @Test
    fun multipolygonJoinsPiecesAndHonoursHoles() {
        // outer ring in two pieces given in opposite directions, plus one inner ring (a clearing)
        val outerA = """{"type":"way","role":"outer","geometry":[
            {"lat":50.0,"lon":20.0},{"lat":50.0,"lon":20.01},{"lat":50.01,"lon":20.01}]}"""
        val outerB = """{"type":"way","role":"outer","geometry":[
            {"lat":50.0,"lon":20.0},{"lat":50.01,"lon":20.0},{"lat":50.01,"lon":20.01}]}"""
        val inner = """{"type":"way","role":"inner","geometry":[
            {"lat":50.004,"lon":20.004},{"lat":50.004,"lon":20.006},{"lat":50.006,"lon":20.006},
            {"lat":50.006,"lon":20.004},{"lat":50.004,"lon":20.004}]}"""
        val rel = """{"type":"relation","id":9,"tags":{"type":"multipolygon","landuse":"forest"},
            "members":[$outerA,$outerB,$inner]}"""
        val m = OsmClutterParser.parse(answer(rel))
        assertEquals(1, m.polygons.size)
        assertEquals(15.0, m.heightAt(50.002, 20.002), 1e-9)
        assertEquals(0.0, m.heightAt(50.005, 20.005), 1e-9)
        assertEquals(0.0, m.heightAt(50.02, 20.002), 1e-9)
    }

    @Test
    fun brokenRingIsDropped() {
        val piece = """{"type":"way","role":"outer","geometry":[{"lat":50.0,"lon":20.0},{"lat":50.0,"lon":20.01}]}"""
        val rel = """{"type":"relation","id":9,"tags":{"natural":"wood"},"members":[$piece]}"""
        assertTrue(OsmClutterParser.parse(answer(rel)).isEmpty())
    }

    @Test
    fun serverGivingUpIsAnError() {
        assertFailsWith<IllegalStateException> {
            OsmClutterParser.parse("""{"elements":[],"remark":"runtime error: Query timed out"}""")
        }
        assertFailsWith<Exception> { OsmClutterParser.parse("<html>busy</html>") }
        // a harmless remark with data is fine
        val ok = OsmClutterParser.parse(
            """{"elements":[${way(""""building":"yes"""", *square)}],"remark":"hello"}""",
        )
        assertEquals(1, ok.polygons.size)
    }

    @Test
    fun parseNumberAndHeightRules() {
        assertEquals(12.0, OsmClutterParser.parseNumber("12 m"))
        assertEquals(12.5, OsmClutterParser.parseNumber(" 12,5"))
        assertNull(OsmClutterParser.parseNumber("tall"))
        assertNull(OsmClutterParser.parseNumber(null))
        assertEquals(4.0, OsmClutterParser.buildingHeight(mapOf("building:levels" to "1")))
        assertNull(OsmClutterParser.buildingHeight(mapOf("building" to "yes")))
        assertEquals(200.0, OsmClutterParser.buildingHeight(mapOf("height" to "900")))
    }

    @Test
    fun largePolygonIsStillFound() {
        // 40 x 40 degrees of cells would not fit the index; it must still be answered
        val big = way(""""natural":"wood"""", 10.0, 10.0, 10.0, 50.0, 50.0, 50.0, 50.0, 10.0, 10.0, 10.0)
        val m = OsmClutterParser.parse(answer(big))
        assertEquals(15.0, m.heightAt(30.0, 30.0), 1e-9)
        assertEquals(0.0, m.heightAt(60.0, 30.0), 1e-9)
    }

    // ---- queries ----

    @Test
    fun shortLinkQueryAsksForBuildingsAlongTheWholePath() {
        val a = GeoPoint(50.8661, 20.6286)
        val b = GeoPoint(50.8661, 20.7)
        val q = OsmQueries.link(a, b)
        assertTrue(q.startsWith("[out:json]"))
        assertTrue(q.contains("""way["building"](around:30,"""))
        assertEquals(1, Regex("""way\["building"\]""").findAll(q).count())
        assertTrue(q.contains("""["natural"="wood"]"""))
        assertTrue(q.endsWith(");out tags geom;"))
    }

    @Test
    fun longLinkOnlyLoadsBuildingsNearTheEnds() {
        val a = GeoPoint(50.0, 20.0)
        val b = GeoPoint(50.0, 20.6) // about 43 km
        assertTrue(Geodesy.distanceM(a, b) > PlannerClutter.LONG_LINK_M)
        val q = OsmQueries.link(a, b)
        assertEquals(2, Regex("""way\["building"\]""").findAll(q).count())
        assertEquals(1, Regex("""way\["natural"="wood"\]""").findAll(q).count())
    }

    @Test
    fun polylineIsBounded() {
        val a = GeoPoint(50.0, 20.0)
        val b = GeoPoint(51.0, 22.0)
        val pts = OsmQueries.polyline(a, b, 0.0, 1.0).split(',').size / 2
        assertTrue(pts in 2..40, "pts=$pts")
        assertEquals(2, OsmQueries.polyline(a, a, 0.0, 1.0).split(',').size / 2)
    }

    @Test
    fun areaQueryIsABoxAndCapped() {
        val c = GeoPoint(50.0, 20.0)
        val q = OsmQueries.area(c, 300.0)
        assertTrue(q.contains("""["landuse"~"^(forest|residential|commercial|industrial|retail)$"]("""))
        // 30 km cap: about 0.27 degrees of latitude each way
        assertTrue(q.contains("49.72"), q)
        assertTrue(q.contains("50.27"), q)
        assertFalse(q.contains("47."))
    }

    // ---- profile helper and coverage ----

    @Test
    fun withClutterKeepsTheEndsOnTheGround() {
        val ground = DoubleArray(21) { 100.0 }
        val out = PlannerClutter.withClutter(ground, 50.0, { 20.0 }) // 1 km path, 100 m clear at each end
        assertEquals(100.0, out[0], 1e-9)
        assertEquals(100.0, out[1], 1e-9) // 50 m: inside the clear zone
        assertEquals(120.0, out[2], 1e-9) // exactly 100 m from the end: outside the clear zone
        assertEquals(120.0, out[10], 1e-9)
        assertEquals(120.0, out[18], 1e-9)
        assertEquals(100.0, out[19], 1e-9) // 50 m from the far end: clear
        assertEquals(100.0, out[20], 1e-9)
    }

    private fun input() = CoverageInput(
        center = GeoPoint(50.0, 20.0), groundAltM = 250.0, antennaHeightM = 10.0, txPowerDbm = 20.0,
        antennaGainDbi = 3.0, feederLossDb = 1.0, frequencyMHz = 868.0, bandwidthKhz = 125.0,
        spreadingFactor = 9, kFactor = 4.0 / 3.0, surfaceRefractivity = 301.0, maxRangeKm = 20.0,
        radials = 8, stepM = 200.0,
    )

    @Test
    fun coverageWithAWallBehindIsWorse() {
        val flat: (Double, Double) -> Double = { _, _ -> 250.0 }
        val bare = Coverage.compute(input(), flat)
        // a 60 m tall belt of forest 6..8 km east of the centre (radial 2 = 90 degrees)
        val c = GeoPoint(50.0, 20.0)
        val wall: (Double, Double) -> Double = { lat, lon ->
            val d = Geodesy.distanceM(c, GeoPoint(lat, lon))
            val east = lon > c.lon
            if (east && d in 6000.0..8000.0) 60.0 else 0.0
        }
        val blocked = Coverage.compute(input(), flat, wall)
        val last = bare.ringsM.size - 1
        assertTrue(blocked.marginDb[2][last] < bare.marginDb[2][last] - 3f, "${blocked.marginDb[2][last]} vs ${bare.marginDb[2][last]}")
        // nothing changes in the opposite direction or before the wall
        assertEquals(bare.marginDb[6][last], blocked.marginDb[6][last], 0.001f)
        assertEquals(bare.marginDb[2][0], blocked.marginDb[2][0], 0.001f)
    }

    @Test
    fun coverageForestTallerThanTheAntennaCostsMargin() {
        val flat: (Double, Double) -> Double = { _, _ -> 250.0 }
        val everywhere: (Double, Double) -> Double = { _, _ -> 15.0 }
        val bare = Coverage.compute(input(), flat)
        val forest = Coverage.compute(input(), flat, everywhere)
        val last = bare.ringsM.size - 1
        // the antenna is 10 m up, the trees 15 m: the whole area is shadowed
        assertTrue(forest.marginDb[0][last] < bare.marginDb[0][last] - 3f)
        assertTrue(forest.marginDb[0][0] < bare.marginDb[0][0])
    }

    @Test
    fun lowerTreesCostLessThanTallerOnes() {
        val flat: (Double, Double) -> Double = { _, _ -> 250.0 }
        val low = Coverage.compute(input(), flat, clutterAt = { _, _ -> 3.0 })
        val tall = Coverage.compute(input(), flat, clutterAt = { _, _ -> 15.0 })
        val bare = Coverage.compute(input(), flat)
        assertTrue(low.marginDb[0][0] <= bare.marginDb[0][0])
        assertTrue(low.marginDb[0][0] > tall.marginDb[0][0])
    }
}
