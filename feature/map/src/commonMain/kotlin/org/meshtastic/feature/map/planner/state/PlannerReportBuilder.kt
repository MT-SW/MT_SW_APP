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

import org.meshtastic.feature.map.planner.DuctingLevel
import org.meshtastic.feature.map.planner.Geodesy
import org.meshtastic.feature.map.planner.LinkVerdict
import org.meshtastic.feature.map.planner.export.Num
import org.meshtastic.feature.map.planner.export.PlannerReport
import org.meshtastic.feature.map.planner.export.ReportCoverage
import org.meshtastic.feature.map.planner.export.ReportCoverageCell
import org.meshtastic.feature.map.planner.export.ReportKeyValue
import org.meshtastic.feature.map.planner.export.ReportPoint
import org.meshtastic.feature.map.planner.export.ReportProfile
import org.meshtastic.feature.map.planner.export.ReportSection

/**
 * Every localizable text of the exported report. The UI fills it from Compose string resources (they are
 * composable-only) and hands it to [PlannerViewModel.buildReport] / the export functions. The defaults are English so
 * tests and non-UI callers work without any setup. Values (numbers) are formatted by the builder, locale independent.
 */
@Suppress("LongParameterList")
data class PlannerReportStrings(
    val title: String = "MT_SW Planner - link report",
    val footer: String = "MT_SW Planner",
    /** Already formatted generation time, e.g. "Generated: 2026-10-01 12:00". */
    val generatedAt: String = "",
    // sections
    val sectionStationA: String = "Station A",
    val sectionStationB: String = "Station B",
    val sectionRadio: String = "Radio parameters",
    val sectionLink: String = "Path",
    val sectionResults: String = "Results",
    val sectionWeather: String = "Weather and propagation",
    val sectionMeasured: String = "Prediction vs measurement",
    // station rows
    val defaultNameA: String = "Station A",
    val defaultNameB: String = "Station B",
    val lblName: String = "Name",
    val lblLatitude: String = "Latitude",
    val lblLongitude: String = "Longitude",
    val lblGroundAlt: String = "Ground height",
    val lblAntennaHeight: String = "Antenna height above ground",
    val lblTxPower: String = "Transmit power",
    val lblAntennaGain: String = "Antenna gain",
    val lblFeederLoss: String = "Feeder and connector loss",
    val lblFeederApprox: String = "Feeder loss is approximate",
    // radio rows
    val lblFrequency: String = "Frequency",
    val lblBandwidth: String = "Bandwidth",
    val lblSpreadingFactor: String = "Spreading factor",
    val lblNoiseFigure: String = "Receiver noise figure",
    val lblNoiseFloor: String = "Noise floor",
    val lblSensitivity: String = "Receiver sensitivity",
    // path rows
    val lblDistance: String = "Distance",
    val lblAzimuthAB: String = "Azimuth A to B (true north)",
    val lblAzimuthBA: String = "Azimuth B to A (true north)",
    val lblElevationAngleA: String = "Elevation angle at A",
    val lblElevationAngleB: String = "Elevation angle at B",
    val lblFspl: String = "Free-space path loss",
    val lblItmLoss: String = "ITM path loss",
    val lblExtraLoss: String = "Extra loss (clutter, gas, rain)",
    val lblTotalLoss: String = "Total path loss",
    val lblLineOfSight: String = "Line of sight clear",
    val lblFresnel: String = "Worst first Fresnel zone clearance",
    val lblKFactor: String = "Effective earth radius factor k",
    val lblRefractivity: String = "Surface refractivity N0",
    // results rows
    val lblRxPowerAB: String = "Received power A to B",
    val lblMarginAB: String = "Link margin A to B",
    val lblVerdictAB: String = "Verdict A to B",
    val lblRxPowerBA: String = "Received power B to A",
    val lblMarginBA: String = "Link margin B to A",
    val lblVerdictBA: String = "Verdict B to A",
    // verdicts
    val verdictExcellent: String = "Excellent",
    val verdictGood: String = "Good",
    val verdictMarginal: String = "Marginal",
    val verdictWeak: String = "Weak",
    val verdictNoLink: String = "No link",
    val yes: String = "yes",
    val no: String = "no",
    // weather rows
    val lblWeatherTime: String = "Data time (UTC)",
    val lblWeatherSource: String = "Source",
    val lblTemperature: String = "Temperature",
    val lblPressure: String = "Surface pressure",
    val lblHumidity: String = "Relative humidity",
    val lblRain: String = "Precipitation",
    val lblDucting: String = "Ducting",
    val ductingNormal: String = "Normal",
    val ductingElevated: String = "Super-refraction",
    val ductingPossible: String = "Possible duct",
    // measured rows
    val lblMeasuredNode: String = "Node",
    val lblPredictedRssi: String = "Predicted RSSI",
    val lblMeasuredRssi: String = "Measured RSSI (LNA corrected)",
    val lblRssiDelta: String = "RSSI difference (measured - predicted)",
    val lblPredictedSnr: String = "Predicted SNR",
    val lblMeasuredSnr: String = "Measured SNR",
    val lblSnrDelta: String = "SNR difference (measured - predicted)",
    val noteNotDirect: String = "The node is not a direct neighbour; the measurement covers several hops.",
    // units
    val unitM: String = "m",
    val unitKm: String = "km",
    val unitDb: String = "dB",
    val unitDbm: String = "dBm",
    val unitMHz: String = "MHz",
    val unitKHz: String = "kHz",
    val unitW: String = "W",
    val unitDeg: String = "deg",
    val unitHpa: String = "hPa",
    val unitCelsius: String = "C",
    val unitPercent: String = "%",
    val unitMmH: String = "mm/h",
    val unitDbi: String = "dBi",
    // chart
    val chartLabelA: String = "A",
    val chartLabelB: String = "B",
    val chartXAxis: String = "Distance [m]",
    val chartYAxis: String = "Height ASL [m]",
    // coverage legend (best to worst) and title
    val coverageTitle: String = "Coverage prediction",
    val legendExcellent: String = ">= 20 dB",
    val legendGood: String = "10 to 20 dB",
    val legendMarginal: String = "0 to 10 dB",
    val legendWeak: String = "-10 to 0 dB (no coverage)",
    val legendNone: String = "< -10 dB",
    // notes
    val disclaimer: String = "Results are estimates only and no guarantee of coverage.",
    val noteWeatherUsed: String = "Weather data used:",
    val noteWeatherNotUsed: String = "Weather not used: standard atmosphere (k = 4/3).",
    val noteWeatherFailed: String = "Weather unavailable: standard atmosphere (k = 4/3) was used.",
    /** Licence and attribution lines (MeshMap Planner GPLv3, Meshtastic Site Planner, SPLAT!, NTIA ITM, Mapterhorn, Open-Meteo, OpenStreetMap). */
    val credits: List<String> = listOf(
        "Concepts: MeshMap Planner (GPL v3), Meshtastic Site Planner, SPLAT!",
        "Propagation model: NTIA Irregular Terrain Model (public domain)",
        "Terrain data: Mapterhorn",
        "Weather: Open-Meteo (CC BY 4.0)",
    ),
)

/** Pure conversion of a [PlannerUiState] into the export model. */
object PlannerReportBuilder {

    private val COLORS = intArrayOf(
        0xFF1B5E20.toInt(),
        0xFF66BB6A.toInt(),
        0xFFFFEB3B.toInt(),
        0xFFEF6C00.toInt(),
        0xFFB71C1C.toInt(),
    )

    fun build(state: PlannerUiState, s: PlannerReportStrings): PlannerReport {
        val sections = ArrayList<ReportSection>()
        sections += ReportSection(s.sectionStationA, endRows(state.a, s, s.defaultNameA, state.frequencyMHz))
        sections += ReportSection(s.sectionStationB, endRows(state.b, s, s.defaultNameB, state.frequencyMHz))
        sections += ReportSection(s.sectionRadio, radioRows(state, s))
        val link = state.link
        if (link != null) {
            val pathRows = ArrayList<ReportKeyValue>()
            pathRows += kv(s.lblDistance, Num.fmt(link.distanceM / 1000.0, 3) + " " + s.unitKm)
            pathRows += kv(s.lblAzimuthAB, Num.fmt(link.bearingAToBDeg, 1) + " " + s.unitDeg)
            pathRows += kv(s.lblAzimuthBA, Num.fmt(link.bearingBToADeg, 1) + " " + s.unitDeg)
            pathRows += kv(s.lblElevationAngleA, Num.fmt(link.elevationAAngleDeg, 2) + " " + s.unitDeg)
            pathRows += kv(s.lblElevationAngleB, Num.fmt(link.elevationBAngleDeg, 2) + " " + s.unitDeg)
            pathRows += kv(s.lblFspl, db(link.freeSpaceLossDb, s))
            pathRows += kv(s.lblItmLoss, db(link.itmLossDb, s))
            pathRows += kv(s.lblExtraLoss, db(link.totalPathLossDb - link.itmLossDb, s))
            pathRows += kv(s.lblTotalLoss, db(link.totalPathLossDb, s))
            pathRows += kv(s.lblLineOfSight, if (link.lineOfSightClear) s.yes else s.no)
            pathRows += kv(
                s.lblFresnel,
                Num.fmt(link.worstFresnelClearanceM, 1) + " " + s.unitM + " (" + Num.fmt(link.worstFresnelRatio * 100.0, 0) +
                    " " + s.unitPercent + ")",
            )
            pathRows += kv(s.lblKFactor, Num.fmt(state.kFactor, 3))
            pathRows += kv(s.lblRefractivity, Num.fmt(state.surfaceRefractivity, 1))
            sections += ReportSection(s.sectionLink, pathRows)

            val res = ArrayList<ReportKeyValue>()
            res += kv(s.lblRxPowerAB, Num.fmt(link.aToB.rxPowerDbm, 1) + " " + s.unitDbm)
            res += kv(s.lblMarginAB, db(link.aToB.marginDb, s))
            res += kv(s.lblVerdictAB, verdict(link.aToB.verdict, s))
            res += kv(s.lblRxPowerBA, Num.fmt(link.bToA.rxPowerDbm, 1) + " " + s.unitDbm)
            res += kv(s.lblMarginBA, db(link.bToA.marginDb, s))
            res += kv(s.lblVerdictBA, verdict(link.bToA.verdict, s))
            sections += ReportSection(s.sectionResults, res)
        }

        val notes = ArrayList<String>()
        notes += s.disclaimer
        val ws = state.weather
        if (state.useWeather && ws is PlannerWeatherStatus.Ready) {
            val c = ws.conditions
            val rows = ArrayList<ReportKeyValue>()
            rows += kv(s.lblWeatherTime, c.fetchedAtIso)
            rows += kv(s.lblWeatherSource, c.source)
            rows += kv(s.lblTemperature, Num.fmt(c.tempC, 1) + " " + s.unitCelsius)
            rows += kv(s.lblPressure, Num.fmt(c.pressureHpa, 1) + " " + s.unitHpa)
            rows += kv(s.lblHumidity, Num.fmt(c.rhPct, 0) + " " + s.unitPercent)
            rows += kv(s.lblRain, Num.fmt(c.rainMmH, 1) + " " + s.unitMmH)
            rows += kv(s.lblDucting, ducting(c.level, s))
            rows += kv(s.lblKFactor, Num.fmt(c.analysis.kFactor, 3))
            rows += kv(s.lblRefractivity, Num.fmt(c.analysis.seaLevelN, 1))
            sections += ReportSection(s.sectionWeather, rows)
            notes += s.noteWeatherUsed + " " + c.source + ", " + c.fetchedAtIso + " UTC"
        } else if (state.useWeather) {
            notes += s.noteWeatherFailed
        } else {
            notes += s.noteWeatherNotUsed
        }

        val cmp = state.comparison
        if (cmp != null) {
            val rows = ArrayList<ReportKeyValue>()
            rows += kv(s.lblMeasuredNode, cmp.nodeName)
            rows += kv(s.lblPredictedRssi, Num.fmt(cmp.predictedRssiDbm, 1) + " " + s.unitDbm)
            if (cmp.measuredRssiDbm != null) {
                rows += kv(s.lblMeasuredRssi, cmp.measuredRssiDbm.toString() + " " + s.unitDbm)
            }
            if (cmp.rssiDeltaDb != null) rows += kv(s.lblRssiDelta, db(cmp.rssiDeltaDb, s))
            rows += kv(s.lblPredictedSnr, Num.fmt(cmp.predictedSnrDb, 1) + " " + s.unitDb)
            if (cmp.measuredSnrDb != null) {
                rows += kv(s.lblMeasuredSnr, Num.fmt(cmp.measuredSnrDb.toDouble(), 1) + " " + s.unitDb)
            }
            if (cmp.snrDeltaDb != null) rows += kv(s.lblSnrDelta, db(cmp.snrDeltaDb, s))
            sections += ReportSection(s.sectionMeasured, rows)
            if (!cmp.direct) notes += s.noteNotDirect
        }
        notes.addAll(s.credits)

        val nameA = state.a.name.ifBlank { s.defaultNameA }
        val nameB = state.b.name.ifBlank { s.defaultNameB }
        val points = ArrayList<ReportPoint>()
        pointOf(state.a, nameA, s)?.let { points += it }
        pointOf(state.b, nameB, s)?.let { points += it }

        val series = state.series
        val profile = if (series != null) {
            ReportProfile(
                distancesM = series.distancesM,
                groundM = series.groundM,
                losM = series.losM,
                fresnelUpperM = series.fresnelUpperM,
                fresnelLowerM = series.fresnelLowerM,
                labelA = s.chartLabelA + ": " + nameA,
                labelB = s.chartLabelB + ": " + nameB,
                xAxisLabel = s.chartXAxis,
                yAxisLabel = s.chartYAxis,
            )
        } else {
            null
        }

        val subtitle = nameA + " - " + nameB + " | " + Num.fmtTrim(state.frequencyMHz, 5) + " " + s.unitMHz + " | " +
            Num.fmtTrim(state.bandwidthKhz, 3) + " " + s.unitKHz + " | SF" + state.spreadingFactor

        return PlannerReport(
            title = s.title,
            subtitle = subtitle,
            generatedAt = s.generatedAt,
            sections = sections,
            profile = profile,
            coverage = coverage(state, s),
            points = points,
            notes = notes,
            footer = s.footer,
        )
    }

    private fun coverage(state: PlannerUiState, s: PlannerReportStrings): ReportCoverage? {
        val cov = state.coverage ?: return null
        val cells = ArrayList<ReportCoverageCell>(cov.radials * cov.ringsM.size)
        for (r in 0 until cov.radials) {
            val bearing = 360.0 * r / cov.radials
            for (j in cov.ringsM.indices) {
                val p = Geodesy.destination(cov.center, bearing, cov.ringsM[j])
                cells += ReportCoverageCell(p.lat, p.lon, cov.marginDb[r][j])
            }
        }
        val legend = listOf(
            s.legendExcellent to COLORS[0],
            s.legendGood to COLORS[1],
            s.legendMarginal to COLORS[2],
            s.legendWeak to COLORS[3],
            s.legendNone to COLORS[4],
        )
        val rangeM = if (cov.ringsM.isEmpty()) 0.0 else cov.ringsM.last()
        return ReportCoverage(
            centerLat = cov.center.lat,
            centerLon = cov.center.lon,
            samples = cells,
            maxRangeKm = rangeM / 1000.0,
            legend = legend,
            title = s.coverageTitle,
            thresholdsDb = listOf(20f, 10f, 0f, -10f),
        )
    }

    private fun pointOf(e: PlannerEnd, name: String, s: PlannerReportStrings): ReportPoint? {
        val lat = e.lat ?: return null
        val lon = e.lon ?: return null
        val desc = Num.fmtTrim(e.txPowerDbm, 1) + " " + s.unitDbm + ", " + Num.fmtTrim(e.antennaGainDbi, 2) + " " +
            s.unitDbi + ", " + Num.fmtTrim(e.antennaHeightM, 1) + " " + s.unitM
        return ReportPoint(name, lat, lon, desc)
    }

    private fun endRows(e: PlannerEnd, s: PlannerReportStrings, defaultName: String, fMHz: Double): List<ReportKeyValue> {
        val rows = ArrayList<ReportKeyValue>()
        rows += kv(s.lblName, e.name.ifBlank { defaultName })
        val lat = e.lat
        val lon = e.lon
        if (lat != null && lon != null) {
            rows += kv(s.lblLatitude, Num.fmt(lat, 6))
            rows += kv(s.lblLongitude, Num.fmt(lon, 6))
        }
        val g = e.groundAltM
        if (g != null) rows += kv(s.lblGroundAlt, Num.fmt(g, 1) + " " + s.unitM)
        rows += kv(s.lblAntennaHeight, Num.fmtTrim(e.antennaHeightM, 1) + " " + s.unitM)
        rows += kv(
            s.lblTxPower,
            Num.fmtTrim(e.txPowerDbm, 1) + " " + s.unitDbm + " (" + Num.fmtTrim(e.txPowerW, 3) + " " + s.unitW + ")",
        )
        rows += kv(s.lblAntennaGain, Num.fmtTrim(e.antennaGainDbi, 2) + " " + s.unitDbi)
        rows += kv(s.lblFeederLoss, db(e.feederLossDb, s))
        if (e.feederPrecise) {
            val res = org.meshtastic.feature.map.planner.FeederBuilder.compute(e.feederConfig, fMHz)
            for (line in res.lines) rows += kv(line.label, db(line.lossDb, s))
            if (res.approximate) rows += kv(s.lblFeederApprox, s.yes)
        }
        return rows
    }

    private fun radioRows(state: PlannerUiState, s: PlannerReportStrings): List<ReportKeyValue> {
        val sens = org.meshtastic.feature.map.planner.Sensitivity.dbm(
            state.bandwidthKhz,
            state.spreadingFactor,
            state.noiseFigureDb,
        )
        return listOf(
            kv(s.lblFrequency, Num.fmtTrim(state.frequencyMHz, 5) + " " + s.unitMHz),
            kv(s.lblBandwidth, Num.fmtTrim(state.bandwidthKhz, 3) + " " + s.unitKHz),
            kv(s.lblSpreadingFactor, "SF" + state.spreadingFactor),
            kv(s.lblNoiseFigure, db(state.noiseFigureDb, s)),
            kv(s.lblNoiseFloor, Num.fmt(state.noiseFloorDbm, 1) + " " + s.unitDbm),
            kv(s.lblSensitivity, Num.fmt(sens, 1) + " " + s.unitDbm),
        )
    }

    private fun kv(label: String, value: String) = ReportKeyValue(label, value)

    private fun db(v: Double, s: PlannerReportStrings) = Num.fmt(v, 1) + " " + s.unitDb

    private fun verdict(v: LinkVerdict, s: PlannerReportStrings): String = when (v) {
        LinkVerdict.EXCELLENT -> s.verdictExcellent
        LinkVerdict.GOOD -> s.verdictGood
        LinkVerdict.MARGINAL -> s.verdictMarginal
        LinkVerdict.WEAK -> s.verdictWeak
        LinkVerdict.NO_LINK -> s.verdictNoLink
    }

    private fun ducting(l: DuctingLevel, s: PlannerReportStrings): String = when (l) {
        DuctingLevel.NORMAL -> s.ductingNormal
        DuctingLevel.ELEVATED -> s.ductingElevated
        DuctingLevel.POSSIBLE_DUCT -> s.ductingPossible
    }
}
