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

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal object SampleData {
    fun profile(n: Int = 120): ReportProfile {
        val d = DoubleArray(n) { it * 35000.0 / (n - 1) }
        val g = DoubleArray(n) { 260 + 70 * sin(it / 9.0) + 40 * cos(it / 4.3) + (if (it in 60..80) 60.0 else 0.0) }
        val los = DoubleArray(n) { 300 + (330 - 300) * it / (n - 1.0) + 25 }
        val r = DoubleArray(n) { 12 * sqrt(d[it] * (35000 - d[it])) / 1100 }
        return ReportProfile(
            d, g, los, DoubleArray(n) { los[it] + r[it] }, DoubleArray(n) { los[it] - r[it] },
            "A: Łysa Góra", "B: Święty Krzyż", "Odległość [m]", "Wysokość [m n.p.m.]",
        )
    }

    fun coverage(): ReportCoverage {
        val lat0 = 50.8661; val lon0 = 20.6286
        val cells = ArrayList<ReportCoverageCell>()
        for (i in -20..20) for (j in -20..20) {
            val dxKm = j * 1.0; val dyKm = i * 1.0
            val r = sqrt(dxKm * dxKm + dyKm * dyKm)
            if (r > 20) continue
            val m = (32 - 1.6 * r + 9 * sin(dxKm / 2.5) * cos(dyKm / 3.1)).toFloat()
            cells.add(ReportCoverageCell(lat0 + dyKm / 110.574, lon0 + dxKm / (111.32 * cos(lat0 * PI / 180)), m))
        }
        return ReportCoverage(
            lat0, lon0, cells, 20.0,
            listOf("≥ 20 dB (bardzo dobry)" to 0xFF1B5E20.toInt(), "10 do 20 dB" to 0xFF66BB6A.toInt(), "0 do 10 dB" to 0xFFFFEB3B.toInt(),
                "-10 do 0 dB (brak zasięgu)" to 0xFFEF6C00.toInt(), "< -10 dB" to 0xFFB71C1C.toInt()),
            "Plan pokrycia: Kielce",
        )
    }

    fun report(rows: Int = 8, withProfile: Boolean = true, withCoverage: Boolean = true): PlannerReport {
        val secs = listOf(
            ReportSection("Parametry łącza", List(rows) { ReportKeyValue("Parametr ąćęłńóśźż $it", "Wartość $it („cudzysłów”) & <b> \\ (nawias)") } +
                ReportKeyValue("Bardzo długa wartość", "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ".repeat(3))),
            ReportSection("Wyniki", listOf(ReportKeyValue("Margines", "12,5 dB"), ReportKeyValue("Strata \"FSPL\"", "130.2; dB"))),
        )
        return PlannerReport(
            "Raport planera MT_SW: Łysa Góra – Święty Krzyż", "Łącze LoRa 869,525 MHz, ĄĆĘŁŃÓŚŹŻ", "Wygenerowano: 2026-10-01 12:00", secs,
            if (withProfile) profile() else null, if (withCoverage) coverage() else null,
            listOf(ReportPoint("Łysa Góra", 50.8661, 20.6286, "Węzeł A, 595 m n.p.m."), ReportPoint("Święty Krzyż <B>", 50.8604, 21.0, "Węzeł \"B\" & co")),
            listOf("Wyniki mają charakter orientacyjny i nie stanowią gwarancji zasięgu.", "Model ITM (domena publiczna), dane terenu SRTM. Pogoda z 2026-09-30.".repeat(2)),
            "Planer MT_SW – meshtastic-swietokrzyskie.pl",
        )
    }
}

internal fun bytesToLatin1(b: ByteArray): String {
    val sb = StringBuilder(b.size)
    for (x in b) sb.append((x.toInt() and 0xFF).toChar())
    return sb.toString()
}
