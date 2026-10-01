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
package org.meshtastic.feature.map.planner

import kotlin.math.roundToLong

/** A cable run between two connectors. [cableId] is a [CableDb] id or [FeederBuilder.CUSTOM_ID]. */
data class FeederSection(val cableId: String, val lengthM: Double, val customDbPerM: Double? = null)

/** Feeder description: N connectors joined by N-1 cable sections (see [FeederBuilder.resize]). */
data class FeederConfig(val connectorIds: List<String>, val sections: List<FeederSection>)

/** One line of the loss breakdown; [label] is a neutral identifier (e.g. "SMA", "RG316 · 0.5 m"). */
data class FeederBreakdownLine(val label: String, val lossDb: Double)

/**
 * Result of [FeederBuilder.compute]. [warnings] are stable machine codes (not prose), e.g.
 * `SECTION_COUNT_MISMATCH`, `CONNECTOR_COUNT_EXCEEDS_MAX`, `UNKNOWN_CONNECTOR:<id>`, `UNKNOWN_CABLE:<id>`,
 * `CUSTOM_LOSS_MISSING`, `INVALID_LENGTH`, `FREQUENCY_CLAMPED`; the UI localizes them.
 */
data class FeederResult(
    val totalDb: Double,
    val lines: List<FeederBreakdownLine>,
    val approximate: Boolean,
    val warnings: List<String>,
)

data class FeederPreset(val id: String, val config: FeederConfig)

object FeederBuilder {
    const val CUSTOM_ID = "custom"

    /** Section without a cable: the connectors are joined directly (an adapter / barrel), only their losses count. */
    const val NONE_ID = "none"
    const val MAX_CONNECTORS = 8
    private const val DEFAULT_CONNECTOR = "sma"
    private const val DEFAULT_CABLE = "rg316"
    private const val DEFAULT_LENGTH_M = 0.2

    val presets: List<FeederPreset> =
        listOf(
            FeederPreset("direct", FeederConfig(emptyList(), emptyList())),
            FeederPreset(
                "pigtail",
                FeederConfig(listOf("ufl", "sma"), listOf(FeederSection("rg316", 0.2))),
            ),
            FeederPreset("adapter_sma_n", FeederConfig(listOf("sma", "n"), listOf(FeederSection(NONE_ID, 0.0)))),
            FeederPreset("adapter_n_n", FeederConfig(listOf("n", "n"), listOf(FeederSection(NONE_ID, 0.0)))),
            FeederPreset(
                "pigtail_cable",
                FeederConfig(
                    listOf("ufl", "sma", "n"),
                    listOf(FeederSection("rg316", 0.2), FeederSection("lmr400", 10.0)),
                ),
            ),
            FeederPreset(
                "long_lmr400",
                FeederConfig(
                    listOf("ufl", "sma", "n"),
                    listOf(FeederSection("rg316", 0.2), FeederSection("lmr400", 20.0)),
                ),
            ),
            FeederPreset(
                "ecoflex_roof",
                FeederConfig(
                    listOf("ufl", "sma", "n"),
                    listOf(FeederSection("rg316", 0.2), FeederSection("ecoflex10", 15.0)),
                ),
            ),
            FeederPreset(
                "heliax_mast",
                FeederConfig(
                    listOf("sma", "n", "n"),
                    listOf(FeederSection("lmr195", 1.0), FeederSection("ldf4_50a", 40.0)),
                ),
            ),
        )

    /**
     * Loss of the feeder at [fMHz]. Order: connector 0, section 0, connector 1, section 1, ... Mismatched sizes
     * are tolerated: sections beyond N-1 are ignored, missing sections count as 0 dB; both add a warning.
     */
    fun compute(config: FeederConfig, fMHz: Double): FeederResult {
        val warnings = mutableListOf<String>()
        val lines = mutableListOf<FeederBreakdownLine>()
        var approx = false
        var total = 0.0

        if (fMHz.isNaN() || fMHz < CableType.MIN_F || fMHz > CableType.MAX_F) warnings += "FREQUENCY_CLAMPED"

        var n = config.connectorIds.size
        if (n > MAX_CONNECTORS) {
            warnings += "CONNECTOR_COUNT_EXCEEDS_MAX"
            n = MAX_CONNECTORS
        }
        val expectedSections = if (n > 0) n - 1 else 0
        if (config.sections.size != expectedSections) warnings += "SECTION_COUNT_MISMATCH"

        for (i in 0 until n) {
            val id = config.connectorIds[i]
            val c = CableDb.connector(id)
            if (c == null) {
                warnings += "UNKNOWN_CONNECTOR:$id"
                approx = true
                lines += FeederBreakdownLine(id, 0.0)
            } else {
                val l = c.lossDb(fMHz)
                total += l
                if (c.approximate) approx = true
                lines += FeederBreakdownLine(c.name, l)
            }
            if (i < expectedSections && i < config.sections.size) {
                val s = config.sections[i]
                var len = s.lengthM
                if (len.isNaN() || len < 0.0) {
                    warnings += "INVALID_LENGTH"
                    len = 0.0
                }
                val lenLabel = fmt(len)
                if (s.cableId == NONE_ID) {
                    // adapter only: no cable loss and no breakdown line
                } else if (s.cableId == CUSTOM_ID) {
                    val perM = s.customDbPerM
                    if (perM == null || perM.isNaN() || perM < 0.0) {
                        warnings += "CUSTOM_LOSS_MISSING"
                        lines += FeederBreakdownLine("$CUSTOM_ID · $lenLabel m", 0.0)
                    } else {
                        val l = perM * len
                        total += l
                        lines += FeederBreakdownLine("$CUSTOM_ID · $lenLabel m", l)
                    }
                } else {
                    val cable = CableDb.cable(s.cableId)
                    if (cable == null) {
                        warnings += "UNKNOWN_CABLE:${s.cableId}"
                        approx = true
                        lines += FeederBreakdownLine("${s.cableId} · $lenLabel m", 0.0)
                    } else {
                        val l = cable.lossDbPerM(fMHz) * len
                        total += l
                        if (cable.approximate) approx = true
                        lines += FeederBreakdownLine("${cable.name} · $lenLabel m", l)
                    }
                }
            }
        }
        return FeederResult(total, lines, approx, warnings)
    }

    /**
     * Resizes to [connectorCount] (0..8) connectors and max(N-1,0) sections, keeping existing entries; new
     * connectors are SMA, new sections RG316 0.2 m.
     */
    fun resize(config: FeederConfig, connectorCount: Int): FeederConfig {
        val n = connectorCount.coerceIn(0, MAX_CONNECTORS)
        val sectionCount = if (n > 0) n - 1 else 0
        val connectors = List(n) { config.connectorIds.getOrNull(it) ?: DEFAULT_CONNECTOR }
        val sections = List(sectionCount) { config.sections.getOrNull(it) ?: FeederSection(DEFAULT_CABLE, DEFAULT_LENGTH_M) }
        return FeederConfig(connectors, sections)
    }

    /** Number with at most 2 decimals, no trailing zeros, no locale or String.format. */
    private fun fmt(v: Double): String {
        val r = (v.coerceAtMost(1.0e6) * 100.0).roundToLong() / 100.0
        val s = r.toString()
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }
}
