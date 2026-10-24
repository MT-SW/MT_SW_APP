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
package org.meshtastic.feature.settings.antenna

import org.meshtastic.core.model.antenna.AntennaDesign
import org.meshtastic.core.model.antenna.AntennaInput
import org.meshtastic.core.model.antenna.AntennaMath
import org.meshtastic.core.ui.util.JsonText
import org.meshtastic.core.ui.util.SimplePdf

/** The file kinds the calculator's result can be saved as. */
internal enum class AntennaSaveFormat(val label: String, val extension: String, val mime: String) {
    PDF("PDF", ".pdf", "application/pdf"),
    MARKDOWN("Markdown", ".md", "text/markdown"),
    TEXT("TXT", ".txt", "text/plain"),
    CSV("CSV", ".csv", "text/csv"),
    JSON("JSON", ".json", "application/json"),
}

/** One dimension of the design as written in the table: its letter, its name and its length. */
internal data class AntennaDimRow(val letter: String, val name: String, val meters: Double)

/** The feed line as it is on the screen: the inputs and the results, as text and as numbers. */
internal data class AntennaFeedData(
    val inputLines: List<String>,
    val resultLines: List<String>,
    val txDbm: Double,
    /** The feed line in words, e.g. "SMA — H155 5 m — SMA". */
    val feederText: String,
    /** Each connector and cable section with its loss in dB, in the order they are in the line. */
    val feederLines: List<Pair<String, Double>>,
    val swr: Double,
    val gainDbi: Double,
    val result: AntennaMath.FeedResult,
)

/** Everything on the calculator's screen, ready to be written to a file. */
internal class AntennaReport(
    val title: String,
    val stamp: String,
    val note: String,
    val sections: List<Pair<String, List<String>>>,
    val dims: List<AntennaDimRow>,
    val json: String,
) {
    fun text(): String =
        buildString {
            append(title).append('\n').append(stamp).append("\n\n")
            sections.forEach { (heading, lines) ->
                append(heading).append('\n')
                lines.forEach { append(it).append('\n') }
                append('\n')
            }
            append(note).append('\n')
        }

    fun markdown(): String =
        buildString {
            append("# ").append(title).append("\n\n_").append(stamp).append("_\n\n")
            sections.forEach { (heading, lines) ->
                append("## ").append(heading).append("\n\n")
                lines.forEach { append(if (it.startsWith("• ")) "- " + it.removePrefix("• ") else "- $it").append('\n') }
                append('\n')
            }
            append("_").append(note).append("_\n")
        }

    /** The dimension table: letter, name and length in millimetres, semicolon separated (decimal point). */
    fun csv(): String =
        buildString {
            append("letter;name;length_mm\r\n")
            dims.forEach { d ->
                append(d.letter).append(';').append(quote(d.name)).append(';')
                append(((d.meters * 10000).let { kotlin.math.round(it) } / 10.0).toString()).append("\r\n")
            }
        }

    fun pdf(): ByteArray = SimplePdf.render(title, stamp, sections, note, "MT_SW")

    fun bytes(format: AntennaSaveFormat): ByteArray =
        when (format) {
            AntennaSaveFormat.PDF -> pdf()
            AntennaSaveFormat.MARKDOWN -> markdown().encodeToByteArray()
            AntennaSaveFormat.TEXT -> text().encodeToByteArray()
            // A byte order mark so a spreadsheet reads the Polish letters right.
            AntennaSaveFormat.CSV -> ("﻿" + csv()).encodeToByteArray()
            AntennaSaveFormat.JSON -> json.encodeToByteArray()
        }

    private fun quote(s: String): String = if (s.any { it == ';' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    companion object {
        /** All numbers of the design and of the feed line as JSON (lengths in metres). */
        fun json(
            title: String,
            stamp: String,
            input: AntennaInput,
            design: AntennaDesign,
            typeLabel: String,
            dims: List<AntennaDimRow>,
            feed: AntennaFeedData?,
        ): String =
            buildString {
                val q = JsonText::quote
                append("{\n  \"title\": ").append(q(title)).append(",\n  \"saved\": ").append(q(stamp))
                append(",\n  \"antenna\": {\"type\": ").append(q(input.type.name)).append(", \"name\": ").append(q(typeLabel))
                append(", \"mhz\": ").append(input.mhz).append(", \"wavelengthM\": ").append(AntennaMath.wavelength(input.mhz))
                append(", \"wireDiameterMm\": ").append(input.diameterMm).append(", \"insulation\": ").append(q(input.insulation.name))
                append(", \"shortening\": ").append(design.shortening)
                design.impedanceOhm?.let { append(", \"impedanceOhm\": ").append(it) }
                append(", \"gainDbi\": ").append(design.gainDbi).append(", \"polarization\": ").append(q(design.polarization.name))
                append("},\n  \"dimensions\": [")
                dims.forEachIndexed { i, d ->
                    append(if (i == 0) "\n" else ",\n")
                    append("    {\"letter\": ").append(q(d.letter)).append(", \"name\": ").append(q(d.name)).append(", \"meters\": ").append(d.meters).append("}")
                }
                append(if (dims.isEmpty()) "],\n" else "\n  ],\n")
                append("  \"feed\": ")
                if (feed == null) {
                    append("null")
                } else {
                    val r = feed.result
                    append("{\"txDbm\": ").append(feed.txDbm)
                    append(", \"feeder\": {\"description\": ").append(q(feed.feederText)).append(", \"parts\": [")
                    append(feed.feederLines.joinToString(", ") { "{\"part\": " + q(it.first) + ", \"lossDb\": " + it.second + "}" })
                    append("]}")
                    append(", \"swr\": ").append(feed.swr).append(", \"antennaGainDbi\": ").append(feed.gainDbi)
                    append(",\n    \"result\": {\"cableDb\": ").append(r.cableDb).append(", \"connectorsDb\": ").append(r.connectorsDb)
                    append(", \"mismatchDb\": ").append(r.mismatchDb).append(", \"totalDb\": ").append(r.totalDb)
                    append(", \"atAntennaDbm\": ").append(r.atAntennaDbm).append(", \"eirpDbm\": ").append(r.eirpDbm)
                    append(", \"erpDbm\": ").append(r.erpDbm).append(", \"rangeFactor\": ").append(r.rangeFactor)
                    append(", \"band\": ").append(r.band?.name?.let(q) ?: "null")
                    append(", \"overLimit\": ").append(r.overLimit ?: "null").append("}}")
                }
                append("\n}\n")
            }
    }
}
