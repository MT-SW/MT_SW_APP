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
package org.meshtastic.feature.messaging.translation

import kotlin.math.roundToInt

/**
 * One node with every number the assistant can rank or average, by metric id. Built by the data provider; the program
 * (not the model) answers questions such as "which node has the lowest voltage", so the numbers are always exact.
 */
internal class MetricRow(val name: String, val values: Map<String, Float>)

/** How a metric value is written for the user. */
internal enum class MetricKind { NUMBER, HOURS, METRES, MINUTES_AGO }

/**
 * A thing that can be asked about every node. [words] are the beginnings of words (in any form) that name it in a
 * question; a word ending in `$` must be the whole word. [highFirst] is the order used when the question gives none.
 * A new metric is a new row here and a value in the provider; nothing else needs to change.
 */
internal class MetricDef(
    val id: String,
    val pl: String,
    val en: String,
    val unit: String,
    val words: List<String>,
    val highFirst: Boolean = true,
    val kind: MetricKind = MetricKind.NUMBER,
    val decimals: Int = 1,
)

internal object NodeMetrics {
    val all: List<MetricDef> =
        listOf(
            MetricDef("voltage", "napięcie", "voltage", "V", listOf("napię", "napie", "volt"), decimals = 2),
            MetricDef("battery", "poziom baterii", "battery level", "%", listOf("bateri", "battery", "akumulat", "naładowan", "naladowan"), highFirst = false, decimals = 0),
            MetricDef("uptime", "czas pracy", "uptime", "", listOf("uptime", "czas pracy", "czasie pracy", "działa od", "dziala od", "pracuje", "pracując", "pracujac"), kind = MetricKind.HOURS),
            MetricDef("chutil", "obciążenie kanału", "channel utilization", "%", listOf("obciążenie kanał", "obciazenie kanal", "obciążenia kanał", "zajętość kanał", "zajetosc kanal", "zajęty kanał", "channel util", "channel load", "utiliz")),
            MetricDef("airtx", "czas nadawania", "airtime", "%", listOf("airtime", "air tx", "czas nadawani", "czasu nadawani", "nadawani")),
            MetricDef("cpu", "obciążenie procesora (CPU)", "CPU load", "%", listOf("cpu", "procesor"), decimals = 0),
            MetricDef("ram", "zajęty RAM", "RAM used", "%", listOf("ram$", "heap", "stert"), decimals = 0),
            MetricDef("flash", "zajęty flash", "flash used", "%", listOf("flash"), decimals = 0),
            MetricDef("psram", "zajęty PSRAM", "PSRAM used", "%", listOf("psram"), decimals = 0),
            MetricDef("wifi", "sygnał Wi-Fi", "Wi-Fi signal", "dBm", listOf("wifi", "wi-fi"), decimals = 0),
            MetricDef("rx", "odebrane pakiety", "packets received", "", listOf("odebran", "otrzyman", "received", "rx$"), decimals = 0),
            MetricDef("tx", "wysłane pakiety", "packets sent", "", listOf("wysłan", "wyslan", "nadan", "sent", "tx$"), decimals = 0),
            MetricDef("bad", "uszkodzone pakiety", "bad packets", "", listOf("uszkodz", "błędn", "bledn", "bad packets"), decimals = 0),
            MetricDef("dupe", "duplikaty pakietów", "duplicate packets", "", listOf("duplik", "dupe"), decimals = 0),
            MetricDef("relayed", "przekazane pakiety", "relayed packets", "", listOf("przekaz", "relay"), decimals = 0),
            MetricDef("dropped", "odrzucone pakiety", "dropped packets", "", listOf("odrzuc", "porzuc", "dropped"), decimals = 0),
            MetricDef("snr", "SNR", "SNR", "dB", listOf("snr")),
            MetricDef("rssi", "RSSI", "RSSI", "dBm", listOf("rssi"), decimals = 0),
            MetricDef("hops", "liczba przeskoków", "hops away", "", listOf("przeskok", "hops$", "hop$"), highFirst = false, decimals = 0),
            MetricDef("distance", "odległość od Twojego węzła", "distance from your node", "", listOf("odległ", "odleg", "dalek", "distance", "najbliż", "najbliz", "closest", "nearest", "farthest"), highFirst = false, kind = MetricKind.METRES),
            MetricDef("lastheard", "ostatnio słyszany", "last heard", "", listOf("słyszan", "slyszan", "heard", "aktywn", "nieaktyw", "ostatnio", "milcz"), highFirst = false, kind = MetricKind.MINUTES_AGO),
            MetricDef("temperature", "temperatura", "temperature", "°C", listOf("temperatur")),
            MetricDef("humidity", "wilgotność", "humidity", "%", listOf("wilgotn", "humidity"), decimals = 0),
            MetricDef("pressure", "ciśnienie", "pressure", "hPa", listOf("ciśnien", "cisnien", "pressure")),
            MetricDef("altitude", "wysokość", "altitude", "m", listOf("wysokoś", "wysokos", "n.p.m", "altitude"), decimals = 0),
        )

    private val LOW =
        listOf(
            "najmniej", "najniż", "najniz", "najsłab", "najslab", "najgorz", "najkrócej", "najkrocej", "najbliż", "najbliz",
            "najmniejsz", "najcichsz", "najstarsz", "lowest", "least", "fewest", "worst", "shortest", "closest", "nearest", "minimum",
        )
    private val HIGH =
        listOf(
            "najwięc", "najwiec", "najwyż", "najwyz", "najlepsz", "najdłuż", "najdluz", "najsilniej", "najdalej", "najdalsz",
            "najbardziej", "największ", "najwiekszy", "najgorętsz", "highest", "most", "best", "longest", "farthest", "maximum",
        )
    private val RANKING_WORDS =
        listOf("który", "która", "które", "ktory", "ktora", "ktore", "ranking", "top ", "średni", "srednia", "średnia", "average", "which", "lista", "list of")
    private val SUMMARY_WORDS = listOf("podsumowan", "zestawien", "summary", "wszystkie statystyki", "szybki przegląd", "szybki przeglad")
    private val NODE_WORDS = listOf("węzł", "wezl", "węzły", "nod", "sieci", "sieć", "siec", "mesh")
    private const val DEFAULT_TOP = 5
    private const val MAX_TOP = 20
    private const val PERCENT = 100f

    private fun String.hasWord(word: String): Boolean {
        val exact = word.endsWith("$")
        val core = word.removeSuffix("$")
        var from = 0
        while (true) {
            val at = indexOf(core, from)
            if (at < 0) return false
            val before = if (at == 0) ' ' else this[at - 1]
            val after = if (at + core.length >= length) ' ' else this[at + core.length]
            if (!before.isLetterOrDigit() && (!exact || !after.isLetterOrDigit())) return true
            from = at + 1
        }
    }

    private fun String.hasAnyWord(words: List<String>) = words.any { hasWord(it) }

    /** The metrics the question names, in the order they appear. */
    fun match(question: String): List<MetricDef> {
        val q = question.lowercase()
        return all
            .mapNotNull { def ->
                val first = def.words.mapNotNull { w -> q.indexOfWord(w).takeIf { it >= 0 } }.minOrNull() ?: return@mapNotNull null
                first to def
            }
            .sortedBy { it.first }
            .map { it.second }
    }

    private fun String.indexOfWord(word: String): Int {
        val exact = word.endsWith("$")
        val core = word.removeSuffix("$")
        var from = 0
        while (true) {
            val at = indexOf(core, from)
            if (at < 0) return -1
            val before = if (at == 0) ' ' else this[at - 1]
            val after = if (at + core.length >= length) ' ' else this[at + core.length]
            if (!before.isLetterOrDigit() && (!exact || !after.isLetterOrDigit())) return at
            from = at + 1
        }
    }

    /** True when the question asks for a ranking, an extreme or an average across the nodes rather than about one node. */
    fun asksAcrossNodes(question: String): Boolean {
        val q = question.lowercase()
        return q.hasAnyWord(LOW) || q.hasAnyWord(HIGH) || q.hasAnyWord(RANKING_WORDS) || q.hasAnyWord(SUMMARY_WORDS)
    }

    private fun wantsHigh(q: String, def: MetricDef): Boolean {
        val low = q.hasAnyWord(LOW)
        val high = q.hasAnyWord(HIGH)
        return when {
            low && !high -> false
            high && !low -> true
            else -> def.highFirst
        }
    }

    private fun topCount(q: String): Int {
        val number = Regex("""(?:top\s*|pierwsz\w*\s*|najlepsz\w*\s*)(\d{1,2})|(\d{1,2})\s*(?:węzł|wezl|node)""").find(q)
        val n = number?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }?.toIntOrNull()
        return (n ?: DEFAULT_TOP).coerceIn(1, MAX_TOP)
    }

    fun format(def: MetricDef, value: Float, polish: Boolean): String = when (def.kind) {
        MetricKind.HOURS -> {
            val minutes = (value * 60).roundToInt()
            val d = minutes / (24 * 60)
            val h = minutes % (24 * 60) / 60
            val m = minutes % 60
            when {
                d > 0 -> "$d d $h h"
                h > 0 -> "$h h $m min"
                else -> "$m min"
            }
        }

        MetricKind.METRES -> if (value >= 1000f) "${number(value / 1000f, 1)} km" else "${value.roundToInt()} m"

        MetricKind.MINUTES_AGO -> {
            val minutes = value.roundToInt()
            val ago = if (polish) "temu" else "ago"
            val text =
                when {
                    minutes >= 24 * 60 -> "${minutes / (24 * 60)} d ${minutes % (24 * 60) / 60} h"
                    minutes >= 60 -> "${minutes / 60} h ${minutes % 60} min"
                    else -> "$minutes min"
                }
            "$text $ago"
        }

        MetricKind.NUMBER -> number(value, def.decimals) + if (def.unit.isNotEmpty()) (if (def.unit == "%" || def.unit == "°C") def.unit else " ${def.unit}") else ""
    }

    private fun number(value: Float, decimals: Int): String {
        if (decimals <= 0) return value.roundToInt().toString()
        var factor = 1
        repeat(decimals) { factor *= 10 }
        val scaled = (value * factor).roundToInt()
        val whole = scaled / factor
        val fraction = kotlin.math.abs(scaled % factor).toString().padStart(decimals, '0')
        return "${if (scaled < 0 && whole == 0) "-" else ""}$whole.$fraction"
    }

    /** The answer for [question] over [rows], or null when the question names no metric and asks for no overview. */
    fun answer(question: String, rows: List<MetricRow>, polish: Boolean): String? {
        val q = question.lowercase()
        if (rows.isEmpty()) return null
        val metrics = match(q)
        if (metrics.isEmpty()) {
            return if (q.hasAnyWord(SUMMARY_WORDS) && q.hasAnyWord(NODE_WORDS)) summary(rows, polish) else null
        }
        if (!asksAcrossNodes(q)) return null
        val top = topCount(q)
        val parts = metrics.take(MAX_METRICS).mapNotNull { def -> single(q, def, rows, top, polish) }
        return parts.joinToString("\n\n").takeIf { it.isNotBlank() }
    }

    private fun single(q: String, def: MetricDef, rows: List<MetricRow>, top: Int, polish: Boolean): String? {
        val withValue = rows.mapNotNull { r -> r.values[def.id]?.let { r to it } }
        val label = if (polish) def.pl else def.en
        if (withValue.isEmpty()) {
            return if (polish) "Żaden węzeł nie podał jeszcze wartości: $label." else "No node has reported a value yet: $label."
        }
        val high = wantsHigh(q, def)
        val sorted = if (high) withValue.sortedByDescending { it.second } else withValue.sortedBy { it.second }
        val order =
            if (polish) (if (high) "od największej wartości" else "od najmniejszej wartości")
            else (if (high) "highest first" else "lowest first")
        val head =
            if (polish) "$label — $order (${withValue.size} z ${rows.size} węzłów podało wartość):"
            else "$label — $order (${withValue.size} of ${rows.size} nodes reported a value):"
        val lines = sorted.take(top).mapIndexed { i, (row, value) -> "${i + 1}. ${row.name} — ${format(def, value, polish)}" }
        val values = withValue.map { it.second }
        val stats =
            if (values.size >= 2) {
                val avg = values.average().toFloat()
                if (polish) "Średnia: ${format(def, avg, true)}; najmniejsza: ${format(def, values.min(), true)}; największa: ${format(def, values.max(), true)}."
                else "Average: ${format(def, avg, false)}; lowest: ${format(def, values.min(), false)}; highest: ${format(def, values.max(), false)}."
            } else {
                ""
            }
        return (listOf(head) + lines + listOfNotNull(stats.takeIf { it.isNotEmpty() })).joinToString("\n")
    }

    /** For each metric that at least two nodes reported: who has the highest and who the lowest value. */
    private fun summary(rows: List<MetricRow>, polish: Boolean): String {
        val lines =
            all.mapNotNull { def ->
                val withValue = rows.mapNotNull { r -> r.values[def.id]?.let { r to it } }
                if (withValue.size < 2) return@mapNotNull null
                val max = withValue.maxBy { it.second }
                val min = withValue.minBy { it.second }
                val label = if (polish) def.pl else def.en
                if (polish) "- $label: największa ${max.first.name} (${format(def, max.second, true)}), najmniejsza ${min.first.name} (${format(def, min.second, true)})"
                else "- $label: highest ${max.first.name} (${format(def, max.second, false)}), lowest ${min.first.name} (${format(def, min.second, false)})"
            }
        val title = if (polish) "Szybkie podsumowanie węzłów (${rows.size}):" else "Quick summary of the nodes (${rows.size}):"
        return (listOf(title) + lines).joinToString("\n")
    }

    private const val MAX_METRICS = 3
}
