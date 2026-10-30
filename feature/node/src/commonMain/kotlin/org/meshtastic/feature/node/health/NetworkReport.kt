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

package org.meshtastic.feature.node.health

import org.meshtastic.core.common.util.MetricFormatter
import kotlin.math.roundToInt

/** One "label: value" line of the exported summary. */
internal data class ReportLine(val label: String, val value: String)

/** One bar of a chart in the exported summary; [value] is only compared with the other bars of the same section. */
internal data class ReportBar(val label: String, val value: Float, val text: String)

/** A titled part of the exported summary. */
internal data class ReportSection(
    val title: String,
    val lines: List<ReportLine> = emptyList(),
    val bars: List<ReportBar> = emptyList(),
)

/** The whole summary as plain data, so it can be written out as PDF, HTML, Markdown, text or CSV. */
internal data class NetworkReport(val title: String, val generated: String, val sections: List<ReportSection>)

private const val MIN_PERCENT = 0f
private const val MAX_PERCENT = 100f
private const val UNKNOWN = "—"

/** Turns the live summary numbers into a [NetworkReport]. [generated] is the date and time shown under the title. */
internal fun NetworkSummaryUiState.toReport(generated: String): NetworkReport {
    val sections = buildList {
        if (attentionItems.isNotEmpty()) {
            add(ReportSection("Wymaga uwagi", lines = attentionItems.map { ReportLine(it.nodeName, it.reason) }))
        }
        add(
            ReportSection(
                title = "Ogólne",
                lines = listOf(ReportLine("Online", "$onlineCount / $totalCount")),
                bars = quietestNodes.map { ReportBar(it.nodeName, it.hoursSilent.toFloat(), "${it.hoursSilent} h") },
            ),
        )
        add(
            ReportSection(
                title = "Zasilanie",
                lines =
                listOf(
                    ReportLine("Średnia bateria", averageBatteryLevel?.let { "$it%" } ?: UNKNOWN),
                    ReportLine("Słaba bateria", "$lowBatteryCount"),
                    ReportLine("Zasilanie zewnętrzne", "$mainsPoweredCount"),
                ),
                bars = distributionBars(batteryDistribution),
            ),
        )
        add(
            ReportSection(
                title = "Sygnał",
                lines =
                listOf(
                    ReportLine("Słaby sygnał (węzły)", "$weakSignalCount"),
                    ReportLine("Średni szum", averageNoiseFloor?.let { "$it dB" } ?: UNKNOWN),
                ),
                bars = distributionBars(snrDistribution),
            ),
        )
        add(
            ReportSection(
                title = "Najlepszy sygnał",
                bars = topSignalNodes.map { ReportBar(it.nodeName, it.snr + SNR_OFFSET, MetricFormatter.snr(it.snr)) },
            ),
        )
        add(
            ReportSection(
                title = "Eter",
                bars =
                listOfNotNull(
                    averageChannelUtilization?.let { percentBar("Średnie zajęcie kanału", it) },
                    maxChannelUtilization?.let { percentBar("Największe zajęcie kanału", it) },
                    averageAirUtilTx?.let { percentBar("Średni czas nadawania", it) },
                ),
            ),
        )
        add(
            ReportSection(
                title = "Ruch",
                lines =
                listOfNotNull(
                    ReportLine("Pakiety wysłane (TX)", "$totalPacketsTx"),
                    ReportLine("Pakiety odebrane (RX)", "$totalPacketsRx"),
                    ReportLine("Duplikaty", "$totalDupes"),
                    ReportLine("Uszkodzone", "$totalBad"),
                    topRelayNodeName?.let { ReportLine("Najwięcej przekazał", "$it ($topRelayCount)") },
                ),
            ),
        )
        add(
            ReportSection(
                title = "Sąsiedzi",
                lines =
                listOf(
                    ReportLine("Węzły z raportem", "$neighborReportingCount"),
                    ReportLine(
                        "Średnio sąsiadów",
                        averageNeighborCount?.let { ((it * TENTHS).roundToInt() / TENTHS).toString() } ?: UNKNOWN,
                    ),
                ),
            ),
        )
        addRanking(
            "Najdłużej działające węzły",
            uptimeLeaders.map { ReportBar(it.nodeName, it.uptimeSeconds.toFloat(), uptimeText(it.uptimeSeconds)) },
        )
        addRanking(
            "Najwięcej pozycji",
            topPositionSenders.map { ReportBar(it.nodeName, it.positionCount.toFloat(), "${it.positionCount}") },
        )
        addRanking(
            "Najbliżej",
            closestNodes.map { ReportBar(it.nodeName, it.distanceMeters.toFloat(), distanceText(it.distanceMeters)) },
        )
        addRanking(
            "Najwięcej danych (telemetria)",
            topTelemetryVolumeSenders.map { ReportBar(it.nodeName, it.packetCount.toFloat(), "${it.packetCount}") },
        )
        addRanking(
            "Najwięcej danych (wszystkie pakiety)",
            topAllPacketVolumeSenders.map { ReportBar(it.nodeName, it.packetCount.toFloat(), "${it.packetCount}") },
        )
        add(
            ReportSection(
                title = "Wiadomości",
                lines = listOf(ReportLine("Tydzień", "$totalMessagesWeek"), ReportLine("Dziś", "$totalMessagesToday")),
                bars =
                messagesPerDay.mapIndexed { index, count ->
                    val daysAgo = messagesPerDay.size - 1 - index
                    ReportBar(if (daysAgo == 0) "Dziś" else "$daysAgo d temu", count.toFloat(), "$count")
                },
            ),
        )
        addRanking(
            "Najaktywniejsi (tydzień)",
            topMessageSendersWeek.map { ReportBar(it.nodeName, it.count.toFloat(), "${it.count}") },
        )
        addRanking(
            "Najaktywniejsi (dziś)",
            topMessageSendersToday.map { ReportBar(it.nodeName, it.count.toFloat(), "${it.count}") },
        )
    }
    return NetworkReport(title = "Podsumowanie sieci mesh", generated = generated, sections = sections)
}

private const val SNR_OFFSET = 30f
private const val TENTHS = 10f
private const val METERS_PER_KM = 1000
private const val SECONDS_PER_HOUR = 3600
private const val HOURS_PER_DAY = 24

private fun MutableList<ReportSection>.addRanking(title: String, bars: List<ReportBar>) {
    if (bars.isNotEmpty()) add(ReportSection(title, bars = bars))
}

private fun percentBar(label: String, value: Float) =
    ReportBar(label, value.coerceIn(MIN_PERCENT, MAX_PERCENT), MetricFormatter.percent(value, decimalPlaces = 1))

private fun distributionBars(counts: List<Int>): List<ReportBar> {
    val names = listOf("Dobrze", "Średnio", "Źle", "Bardzo źle", "Brak danych")
    return counts.mapIndexed { i, c -> ReportBar(names.getOrElse(i) { "?" }, c.toFloat(), "$c") }
}

internal fun uptimeText(seconds: Int): String {
    val hours = seconds / SECONDS_PER_HOUR
    val days = hours / HOURS_PER_DAY
    return if (days > 0) "${days}d ${hours % HOURS_PER_DAY}h" else "${hours}h"
}

internal fun distanceText(meters: Int): String = if (meters >= METERS_PER_KM) {
    "${(meters / METERS_PER_KM.toFloat() * TENTHS).roundToInt() / TENTHS} km"
} else {
    "$meters m"
}

private const val BAR_CHARS = 20
private const val BLOCK = '█'

/** A text bar such as "██████" for use in plain text and Markdown. */
private fun textBar(value: Float, max: Float): String {
    if (max <= 0f || value <= 0f) return ""
    return BLOCK.toString().repeat(((value / max) * BAR_CHARS).roundToInt().coerceIn(1, BAR_CHARS))
}

internal fun NetworkReport.toPlainText(): String = buildString {
    appendLine(title)
    appendLine(generated)
    sections.forEach { section ->
        appendLine()
        appendLine(section.title.uppercase())
        section.lines.forEach { appendLine("${it.label}: ${it.value}") }
        val max = section.bars.maxOfOrNull { it.value } ?: 0f
        section.bars.forEach { appendLine("${it.label}: ${it.text} ${textBar(it.value, max)}".trimEnd()) }
    }
}

internal fun NetworkReport.toMarkdown(): String = buildString {
    appendLine("# $title")
    appendLine()
    appendLine("_${generated}_")
    sections.forEach { section ->
        appendLine()
        appendLine("## ${section.title}")
        appendLine()
        section.lines.forEach { appendLine("- **${it.label}:** ${it.value}") }
        val max = section.bars.maxOfOrNull { it.value } ?: 0f
        section.bars.forEach { appendLine("- ${it.label}: **${it.text}** ${textBar(it.value, max)}".trimEnd()) }
    }
}

internal fun NetworkReport.toCsv(): String = buildString {
    appendLine("\"sekcja\",\"pozycja\",\"wartość\"")
    sections.forEach { section ->
        section.lines.forEach { appendLine(csvRow(section.title, it.label, it.value)) }
        section.bars.forEach { appendLine(csvRow(section.title, it.label, it.text)) }
    }
}

private fun csvRow(vararg cells: String) = cells.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }

private fun String.escapeHtml() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

private const val HTML_STYLE =
    "body{font-family:sans-serif;max-width:760px;margin:24px auto;padding:0 16px;color:#222}" +
        "h1{margin-bottom:0}.when{color:#777;margin-top:4px}" +
        "section{border:1px solid #ddd;border-radius:12px;padding:12px 16px;margin:16px 0;break-inside:avoid}" +
        "h2{margin:0 0 8px;font-size:18px;border-left:6px solid #d9a400;padding-left:8px}" +
        ".row{display:flex;justify-content:space-between;padding:2px 0}" +
        ".bar{display:flex;align-items:center;gap:8px;margin:3px 0}.bar span.l{width:38%}" +
        ".track{flex:1;background:#eee;border-radius:4px;height:10px}" +
        ".fill{background:#d9a400;border-radius:4px;height:10px}.bar span.v{width:15%;text-align:right}" +
        "@media print{body{margin:0}}"

internal fun NetworkReport.toHtml(): String = buildString {
    appendLine("<!doctype html><html lang=\"pl\"><head><meta charset=\"utf-8\">")
    appendLine("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
    appendLine("<title>${title.escapeHtml()}</title><style>$HTML_STYLE</style></head><body>")
    appendLine("<h1>${title.escapeHtml()}</h1><p class=\"when\">${generated.escapeHtml()}</p>")
    sections.forEach { section ->
        appendLine("<section><h2>${section.title.escapeHtml()}</h2>")
        section.lines.forEach {
            appendLine("<div class=\"row\"><span>${it.label.escapeHtml()}</span><b>${it.value.escapeHtml()}</b></div>")
        }
        val max = section.bars.maxOfOrNull { it.value } ?: 0f
        section.bars.forEach {
            val share = if (max > 0f) it.value / max else 0f
            val width = (share * MAX_PERCENT).roundToInt().coerceIn(0, MAX_PERCENT.toInt())
            appendLine(
                "<div class=\"bar\"><span class=\"l\">${it.label.escapeHtml()}</span>" +
                    "<div class=\"track\"><div class=\"fill\" style=\"width:$width%\"></div></div>" +
                    "<span class=\"v\">${it.text.escapeHtml()}</span></div>",
            )
        }
        appendLine("</section>")
    }
    appendLine("</body></html>")
}
