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
@file:Suppress("LongMethod", "TooManyFunctions")

package org.meshtastic.feature.node.health

import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.common.util.nowMillis
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** One "label: value" line of the exported summary. */
internal data class ReportLine(val label: String, val value: String)

/** One bar of a chart in the exported summary; [value] is only compared with the other bars of the same section. */
internal data class ReportBar(val label: String, val value: Float, val text: String)

/** One node of a position sketch in the exported summary; [isMe] marks the local node. */
internal data class ReportPoint(val label: String, val latitude: Double, val longitude: Double, val isMe: Boolean)

/** A titled part of the exported summary. */
internal data class ReportSection(
    val title: String,
    val lines: List<ReportLine> = emptyList(),
    val bars: List<ReportBar> = emptyList(),
    val points: List<ReportPoint> = emptyList(),
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
        if (nodeRows.isNotEmpty()) {
            val nowSeconds = (nowMillis / MILLIS_PER_SECOND).toInt()
            add(ReportSection("Lista węzłów (od ostatnio słyszanych)", lines = nodeRows.map { it.toLine(nowSeconds) }))
        }
        if (mapPoints.isNotEmpty()) {
            add(
                ReportSection(
                    title = "Położenie węzłów (schemat, północ u góry)",
                    points = mapPoints.map { ReportPoint(it.nodeName, it.latitude, it.longitude, it.isMe) },
                ),
            )
        }
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

private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60
private const val MAINS_LEVEL = 100

private fun NodeRow.toLine(nowSeconds: Int): ReportLine {
    val parts =
        listOfNotNull(
            snr?.let { "sygnał ${MetricFormatter.snr(it)}" },
            batteryPercent?.let { if (it > MAINS_LEVEL) "zasilanie zewn." else "bateria $it%" },
            if (lastHeardSeconds == 0) "nie słyszany" else ageText(nowSeconds - lastHeardSeconds),
            distanceMeters?.let { distanceText(it) },
        )
    return ReportLine(nodeName, parts.joinToString(" · "))
}

private fun ageText(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return when {
        s < SECONDS_PER_HOUR -> "${s / SECONDS_PER_MINUTE} min temu"
        s < SECONDS_PER_HOUR * HOURS_PER_DAY -> "${s / SECONDS_PER_HOUR} h temu"
        else -> "${s / (SECONDS_PER_HOUR * HOURS_PER_DAY)} d temu"
    }
}

/** A [ReportPoint] placed in a drawing box: [x] and [y] count from the top left corner. */
internal data class PlacedPoint(val point: ReportPoint, val x: Float, val y: Float)

/**
 * Places [points] in a box of [width] × [height] with [pad] around it, north up and the true proportions kept (the
 * east-west distances are shortened by the cosine of the latitude), centred in the box.
 */
internal fun placePoints(points: List<ReportPoint>, width: Float, height: Float, pad: Float): List<PlacedPoint> {
    if (points.isEmpty()) return emptyList()
    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLon = points.minOf { it.longitude }
    val maxLon = points.maxOf { it.longitude }
    val k = cos((minLat + maxLat) / 2 * PI / 180.0)
    val spanX = ((maxLon - minLon) * k).toFloat()
    val spanY = (maxLat - minLat).toFloat()
    val availW = width - 2 * pad
    val availH = height - 2 * pad
    val scaleX = if (spanX > 0f) availW / spanX else Float.MAX_VALUE
    val scaleY = if (spanY > 0f) availH / spanY else Float.MAX_VALUE
    val scale = if (spanX <= 0f && spanY <= 0f) 0f else min(scaleX, scaleY)
    val offsetX = pad + (availW - spanX * scale) / 2
    val offsetY = pad + (availH - spanY * scale) / 2
    return points.map {
        PlacedPoint(
            point = it,
            x = offsetX + ((it.longitude - minLon) * k).toFloat() * scale,
            y = offsetY + (maxLat - it.latitude).toFloat() * scale,
        )
    }
}

/** The coordinates written out for the text formats, e.g. `50.87654, 20.63284`. */
internal fun ReportPoint.coordinates(): String = "${rounded(latitude)}, ${rounded(longitude)}"

private const val COORDINATE_SCALE = 100_000.0

private fun rounded(value: Double): Double = (value * COORDINATE_SCALE).roundToLong() / COORDINATE_SCALE

/** Names are drawn next to the dots only while there are few enough of them to stay readable. */
internal const val MAX_LABELLED_POINTS = 30

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
        section.points.forEach { appendLine("${it.label}: ${it.coordinates()}") }
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
        section.points.forEach { appendLine("- ${it.label}: ${it.coordinates()}") }
    }
}

internal fun NetworkReport.toCsv(): String = buildString {
    appendLine("\"sekcja\",\"pozycja\",\"wartość\"")
    sections.forEach { section ->
        section.lines.forEach { appendLine(csvRow(section.title, it.label, it.value)) }
        section.bars.forEach { appendLine(csvRow(section.title, it.label, it.text)) }
        section.points.forEach { appendLine(csvRow(section.title, it.label, it.coordinates())) }
    }
}

private fun csvRow(vararg cells: String) = cells.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }

private const val SVG_WIDTH = 720f
private const val SVG_HEIGHT = 420f
private const val SVG_PAD = 28f

/** The position sketch as an inline SVG: a dot per node, the local node larger, names when there are few nodes. */
private fun List<ReportPoint>.toSvg(): String = buildString {
    append("<svg viewBox=\"0 0 ${SVG_WIDTH.toInt()} ${SVG_HEIGHT.toInt()}\" style=\"width:100%;height:auto\">")
    append("<rect width=\"${SVG_WIDTH.toInt()}\" height=\"${SVG_HEIGHT.toInt()}\" fill=\"#f6f6f6\" rx=\"8\"/>")
    val labelled = size <= MAX_LABELLED_POINTS
    placePoints(this@toSvg, SVG_WIDTH, SVG_HEIGHT, SVG_PAD).forEach { placed ->
        val x = placed.x.roundToInt()
        val y = placed.y.roundToInt()
        val name = placed.point.label.escapeHtml()
        if (placed.point.isMe) {
            append("<circle cx=\"$x\" cy=\"$y\" r=\"7\" fill=\"#222\"><title>$name</title></circle>")
        } else {
            append("<circle cx=\"$x\" cy=\"$y\" r=\"4\" fill=\"#d9a400\"><title>$name</title></circle>")
        }
        if (labelled || placed.point.isMe) {
            append("<text x=\"${x + 8}\" y=\"${y + 4}\" font-size=\"11\" fill=\"#444\">$name</text>")
        }
    }
    append("</svg>")
}

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
        if (section.points.isNotEmpty()) appendLine(section.points.toSvg())
        appendLine("</section>")
    }
    appendLine("</body></html>")
}
