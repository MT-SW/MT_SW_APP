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

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.first
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.MeshLog
import org.meshtastic.core.model.util.decodeLocalStatsExtended
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.proto.PortNum
import org.meshtastic.proto.Telemetry
import kotlin.math.roundToInt

/**
 * Gives the assistant READ-ONLY access to what the app knows: the own node, the radio statistics and their trend, the
 * radio settings (without keys and passwords), the node list and the latest messages. It never changes a setting and
 * never sends anything. Only the parts that match the question are added, so the prompt stays short.
 */
class AssistantDataProvider(
    private val nodeRepository: NodeRepository,
    private val radioConfigRepository: RadioConfigRepository,
    private val meshLogRepository: MeshLogRepository,
    private val packetRepository: PacketRepository,
) {

    /** A text with the data that is useful for answering [question], or an empty text when nothing matches. */
    suspend fun contextFor(question: String, maxChars: Int = MAX_CONTEXT_CHARS): String {
        val q = question.lowercase()
        val mentioned = mentionedNodes(q)
        // Most useful parts first: the model only fits about 4000 tokens, so later parts are cut when space runs out.
        val parts = ArrayList<String>()
        parts += safe("overview") { overview() }
        for (node in mentioned) parts += safe("node") { nodeDetail(node) }
        if (q.hasAny(SENSOR_WORDS)) parts += safe("sensors") { sensors() }
        if (q.hasAny(SIGNAL_WORDS)) parts += safe("signal") { signalRanking(q) }
        val network = q.hasAny(NODE_WORDS) || q.hasAny(TREND_WORDS) || q.hasAny(RANK_WORDS) || q.hasAny(SIGNAL_WORDS)
        if (network && (q.hasAny(RANK_WORDS) || q.hasAny(TREND_WORDS) || !q.hasAny(SIGNAL_WORDS))) {
            parts += safe("ranking") { ranking(q) }
        }
        if (mentioned.isEmpty() && q.hasAny(TREND_WORDS)) {
            val myNum = nodeRepository.ourNodeInfo.value?.num
            if (myNum != null) parts += safe("trend") { trends(myNum, "own node", 0L) }
        }
        if (network) parts += safe("network") { networkSummary() }
        if (q.hasAny(SETTINGS_WORDS)) parts += safe("settings") { settings() }
        if (q.hasAny(SUMMARY_WORDS)) {
            parts += safe("channel") { toolChannelMessages(channelFromQuestion(q), SUMMARY_MESSAGES) }
        } else if (q.hasAny(MESSAGE_WORDS)) {
            parts += safe("messages") { messages() }
        }
        parts += safe("index") { nodeIndex() }
        // When the question has its own topic, the general overview shrinks so the topic data fits in the small window.
        if (parts.size > 2) parts[0] = parts[0].take(OVERVIEW_SHORT)
        val out = StringBuilder()
        for (part in parts.filter { it.isNotBlank() }) {
            val room = maxChars - out.length
            if (room < MIN_PART) break
            if (out.isNotEmpty()) out.append("\n\n")
            out.append(part.take(room - 2))
        }
        return out.toString()
    }

    /** Nodes whose long or short name appears in the question (the own node counts for words like "mój"). */
    private fun mentionedNodes(q: String): List<org.meshtastic.core.model.Node> {
        val all = nodeRepository.nodeDBbyNum.value.values
        val byName =
            all.filter { n ->
                val long = n.user.long_name.lowercase()
                val short = n.user.short_name.lowercase()
                (long.length >= MIN_NAME && q.contains(long)) || (short.length >= MIN_NAME && Regex("\\b${Regex.escape(short)}\\b").containsMatchIn(q))
            }
        val own = nodeRepository.ourNodeInfo.value
        val mine = if (own != null && q.hasAny(OWN_WORDS)) listOf(own) else emptyList()
        return (byName + mine).distinctBy { it.num }.take(MAX_NODES_DETAIL)
    }

    private suspend fun safe(what: String, block: suspend () -> String): String = try {
        block()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.w(tag = TAG, throwable = e) { "Assistant data '$what' unavailable" }
        ""
    }

    private suspend fun overview(): String {
        val me = nodeRepository.ourNodeInfo.value
        val stats = nodeRepository.localStats.value
        val extended = nodeRepository.localStatsExtended.value
        return buildString {
            appendLine("OWN NODE: ${me?.user?.long_name ?: "unknown"} (${me?.user?.short_name ?: "?"}), hardware ${me?.user?.hw_model}")
            if (me != null) appendLine("Device metrics: ${me.deviceMetrics.toString().pretty()}")
            appendLine("Nodes online/total: ${nodeRepository.onlineNodeCount.first()}/${nodeRepository.totalNodeCount.first()}")
            appendLine("Radio statistics: ${stats.toString().pretty()}")
            appendLine("Extended statistics (cpu, flash, psram, wifi): $extended")
        }
    }

    private suspend fun trends(num: Int, title: String, sinceMs: Long): String {
        // The own node's logs are stored under a special id, so ask the repository which id holds them.
        val myNum = nodeRepository.effectiveLogNodeId(num).first()
        val since = sinceMs
        val entries =
            meshLogRepository.getLogsFromNodeSince(myNum, since).first().mapNotNull { it.toSample() }.sortedBy { it.time }
        if (entries.isEmpty()) return "TRENDS of $title: no history recorded."
        val series =
            listOf(
                Triple("heap_used_percent (used internal RAM of the radio, lower is better)", "%", { s: Sample -> s.heapUsed }),
                Triple("psram_free_percent (free external PSRAM of the radio)", "%", { s: Sample -> s.psramFree }),
                Triple("cpu_usage_percent (radio)", "%", { s: Sample -> s.cpu }),
                Triple("flash_used_percent (radio)", "%", { s: Sample -> s.flashUsed }),
                Triple("battery_level", "%", { s: Sample -> s.battery }),
                Triple("channel_utilization", "%", { s: Sample -> s.channelUtil }),
                Triple("air_util_tx", "%", { s: Sample -> s.airUtil }),
                Triple("noise_floor", " dBm", { s: Sample -> s.noise }),
            )
        return buildString {
            appendLine("TRENDS of $title, whole recorded history (${entries.size} samples, from ${spanText(entries.first().time)} ago until ${spanText(entries.last().time)} ago):")
            for ((name, unit, pick) in series) {
                val values = entries.mapNotNull { e -> pick(e)?.let { e.time to it } }
                if (values.size < 2) continue
                val first = values.first().second
                val last = values.last().second
                val change = last - first
                val direction = if (kotlin.math.abs(change) < STABLE_DELTA) "stable" else if (change > 0) "rising" else "falling"
                appendLine(
                    "- $name: now ${fmt(last)}$unit, at start ${fmt(first)}$unit, min ${fmt(values.minOf { it.second })}, " +
                        "max ${fmt(values.maxOf { it.second })}, change ${fmt(change)} ($direction), samples ${values.size}",
                )
            }
        }
    }

    private suspend fun nodeDetail(n: org.meshtastic.core.model.Node, sinceMs: Long = 0L): String {
        val now = nowMillis / MILLIS_PER_SECOND
        val heard = if (n.lastHeard == 0) "never" else "${((now - n.lastHeard) / SECONDS_PER_MINUTE).coerceAtLeast(0)} min ago"
        val head = buildString {
            appendLine("NODE ${n.user.long_name} (${n.user.short_name}), id ${n.user.id}, hardware ${n.user.hw_model}, role ${n.user.role}")
            appendLine("Heard $heard; hops=${n.hopsAway}; SNR=${if (n.snr == Float.MAX_VALUE) "unknown" else fmt(n.snr) + " dB"}; RSSI=${if (n.rssi == Int.MAX_VALUE) "unknown" else n.rssi.toString() + " dBm"}; favourite=${n.isFavorite}; via MQTT=${n.viaMqtt}")
            appendLine("Position: ${n.position}")
            appendLine("Device metrics: ${n.deviceMetrics.toString().pretty()}")
            appendLine("Environment: ${n.environmentMetrics.toString().pretty()}")
            if (n.notes.isNotBlank()) appendLine("Notes: ${n.notes}")
        }
        return head + safe("node trends") { trends(n.num, n.user.long_name, sinceMs) }
    }

    /**
     * Which nodes report weather, air quality, power or other sensor data, and the values they last sent. Values come
     * from the node list and from the telemetry logs of the last 3 days (the newest reading of each kind wins).
     */
    private suspend fun collectSensors(): HashMap<Int, MutableMap<String, Pair<Long, String>>> {
        val found = HashMap<Int, MutableMap<String, Pair<Long, String>>>()
        fun put(num: Int, kind: String, time: Long, text: String) {
            if (text.endsWith("{}")) return
            val map = found.getOrPut(num) { HashMap() }
            val old = map[kind]
            if (old == null || time >= old.first) map[kind] = time to text
        }
        val own = nodeRepository.ourNodeInfo.value?.num ?: 0
        for (n in nodeRepository.nodeDBbyNum.value.values) {
            val time = n.lastHeard * MILLIS_PER_SECOND
            put(n.num, "weather/environment", time, n.environmentMetrics.toString())
            put(n.num, "air quality", time, n.airQualityMetrics.toString())
            put(n.num, "power", time, n.powerMetrics.toString())
            put(n.num, "soil/water", time, n.soilWaterMetrics.toString())
            if (n.paxcounter.wifi != 0 || n.paxcounter.ble != 0) put(n.num, "pax counter", time, n.paxcounter.toString())
        }
        val logs = meshLogRepository.getLogsByPortNumSince(PortNum.TELEMETRY_APP.value, nowMillis - RANK_WINDOW_MS).first()
        for (log in logs) {
            val payload = log.fromRadio.packet?.decoded?.payload ?: continue
            val t = runCatching { Telemetry.ADAPTER.decode(payload) }.getOrNull() ?: continue
            val num = if (log.fromNum == 0) own else log.fromNum
            t.environment_metrics?.let { put(num, "weather/environment", log.received_date, it.toString()) }
            t.air_quality_metrics?.let { put(num, "air quality", log.received_date, it.toString()) }
            t.power_metrics?.let { put(num, "power", log.received_date, it.toString()) }
        }
        return found
    }

    /**
     * A ready-made, exact sensor answer written in code (the model garbles digits when it copies long tables), with the
     * hottest/coldest/most humid nodes worked out here as well.
     */
    suspend fun sensorReport(): String {
        val found = collectSensors()
        val nowS = nowMillis / MILLIS_PER_SECOND
        class Row(val name: String, val ago: Long, val values: Map<String, Double>)
        val field = Regex("(\\w+)=(-?[0-9]+(?:\\.[0-9]+)?(?:E-?[0-9]+)?)")
        val rows =
            found.entries.mapNotNull { (num, kinds) ->
                val values = LinkedHashMap<String, Double>()
                for ((kind, v) in kinds) {
                    if (kind == "pax counter") continue
                    field.findAll(v.second).forEach { m ->
                        val key = m.groupValues[1]
                        val d = m.groupValues[2].toDoubleOrNull() ?: return@forEach
                        val range = PLAUSIBLE[key]
                        if (range == null || d in range) values[key] = d
                    }
                }
                values.keys.retainAll(REPORT_KEYS.keys)
                if (values.isEmpty()) return@mapNotNull null
                val n = nodeRepository.nodeDBbyNum.value[num]
                val name = n?.let { "${it.user.short_name} (${it.user.long_name})" } ?: "!${num.toUInt().toString(HEX)}"
                val ago = ((nowS - kinds.values.maxOf { it.first } / MILLIS_PER_SECOND) / SECONDS_PER_MINUTE).coerceAtLeast(0)
                Row(name, ago, values)
            }
        if (rows.isEmpty()) return "Żaden węzeł nie podał danych pogodowych ani z czujników."
        val sorted = rows.sortedByDescending { it.values.containsKey("temperature") }
        val sb = StringBuilder("Dane z czujników (${rows.size} węzłów, ostatnie odczyty; niemożliwe wartości pominięte):\n")
        for (r in sorted.take(REPORT_LIMIT)) {
            val text = r.values.entries.joinToString(", ") { (k, v) -> REPORT_KEYS.getValue(k).replace("%s", roundTo(v, if (k in TWO_DECIMALS) 2 else 1)) }
            sb.append("• ${r.name}: $text\n")
        }
        fun extreme(key: String, label: String, highest: Boolean) {
            val best = rows.filter { it.values.containsKey(key) }.let { l -> if (highest) l.maxByOrNull { it.values.getValue(key) } else l.minByOrNull { it.values.getValue(key) } }
            if (best != null) sb.append("$label: ${best.name} — ${REPORT_KEYS.getValue(key).replace("%s", roundTo(best.values.getValue(key), 1)).substringAfter(' ')}\n")
        }
        sb.append('\n')
        extreme("temperature", "Najcieplej", true)
        extreme("temperature", "Najzimniej", false)
        extreme("relative_humidity", "Największa wilgotność", true)
        extreme("barometric_pressure", "Najwyższe ciśnienie", true)
        return sb.toString().trim()
    }

    private suspend fun sensors(): String {
        val nowS = nowMillis / MILLIS_PER_SECOND
        val found = collectSensors()
        // node number -> kind -> (time in ms, values text)
        if (found.isEmpty()) return "SENSOR DATA: no node has reported weather, air quality, power or other sensor data."
        val lines =
            found.entries
                .sortedWith(compareByDescending<Map.Entry<Int, MutableMap<String, Pair<Long, String>>>> { e -> e.value.values.any { it.second.contains("temperature=") } }.thenByDescending { e -> e.value.values.maxOf { it.first } })
                .map { (num, kinds) ->
                    val n = nodeRepository.nodeDBbyNum.value[num]
                    val name = n?.let { "${it.user.short_name} (${it.user.long_name})" } ?: "!${num.toUInt().toString(HEX)}"
                    "- $name: " +
                        kinds.entries.joinToString("; ") { (kind, v) ->
                            val ago = ((nowS - v.first / MILLIS_PER_SECOND) / SECONDS_PER_MINUTE).coerceAtLeast(0)
                            "$kind (${ago} min ago): ${v.second.substringAfter('{').removeSuffix("}").pretty().replace("gas_resistance", "gas")}"
                        }
                }
        return "SENSOR DATA from ${lines.size} nodes (weather, air quality, power; latest values):\n" +
            lines.take(SENSOR_LIMIT).joinToString("\n")
    }

    /** Exact signal list written in code, in Polish, so the model never copies the numbers wrong. */
    fun signalReport(q: String): String {
        val me = nodeRepository.ourNodeInfo.value
        val now = nowMillis / MILLIS_PER_SECOND
        class Row(val n: org.meshtastic.core.model.Node, val meters: Int?)
        val rows = nodeRepository.nodeDBbyNum.value.values.filter { it.num != me?.num }.map { Row(it, me?.let { m -> it.distance(m) }) }
        val byDistance = q.hasAny(listOf("odległ", "odleg", "daleko", "dystans", "distance", "far"))
        val closest = q.hasAny(listOf("najbliż", "najbliz", "blisk", "closest", "nearest"))
        val withSignal = rows.filter { it.n.snr != Float.MAX_VALUE }
        val sorted =
            when {
                closest -> rows.filter { it.meters != null }.sortedBy { it.meters }
                byDistance -> rows.filter { it.meters != null }.sortedByDescending { it.meters }
                else -> withSignal.sortedWith(compareBy<Row> { it.n.hopsAway.let { h -> if (h < 0) Int.MAX_VALUE else h } }.thenByDescending { it.n.snr })
            }
        val title =
            when {
                closest -> "Najbliższe węzły (z ${rows.size}):"
                byDistance -> "Najdalsze węzły (z ${rows.size}):"
                else -> "Najlepszy zasięg — węzły słyszane bezpośrednio najpierw, od najwyższego SNR (${withSignal.size} z odczytem z ${rows.size}):"
            }
        val sb = StringBuilder(title).append('\n')
        for (r in sorted.take(REPORT_SIGNAL)) {
            val n = r.n
            val ago = if (n.lastHeard == 0) "nigdy" else "${((now - n.lastHeard) / SECONDS_PER_MINUTE).coerceAtLeast(0)} min temu"
            val snr = if (n.snr == Float.MAX_VALUE) "brak" else fmt(n.snr) + " dB"
            val rssi = if (n.rssi == Int.MAX_VALUE) "brak" else n.rssi.toString() + " dBm"
            val dist = r.meters?.let { m -> fmt(m / METERS_PER_KM) + " km" } ?: "brak"
            sb.append("• ${n.user.short_name} (${n.user.long_name}): skoki ${n.hopsAway}, SNR $snr, RSSI $rssi, odległość $dist, słyszany $ago\n")
        }
        return sb.toString().trim()
    }

    /** Link quality to every node we hear: SNR, RSSI, hops and the distance from our own node. */
    private fun signalRanking(q: String, top: Int = SIGNAL_TOP): String {
        val me = nodeRepository.ourNodeInfo.value
        val now = nowMillis / MILLIS_PER_SECOND
        class Row(val n: org.meshtastic.core.model.Node, val meters: Int?)
        val rows =
            nodeRepository.nodeDBbyNum.value.values
                .filter { it.num != me?.num }
                .map { Row(it, me?.let { m -> it.distance(m) }) }
        val heardWithSignal = rows.filter { it.n.snr != Float.MAX_VALUE }
        val byDistance = q.hasAny(listOf("odległ", "odleg", "daleko", "dystans", "distance", "far"))
        val closest = q.hasAny(listOf("najbliż", "najbliz", "blisk", "closest", "nearest"))
        val sorted =
            when {
                closest -> rows.filter { it.meters != null }.sortedBy { it.meters }
                byDistance -> rows.filter { it.meters != null }.sortedByDescending { it.meters }
                else -> heardWithSignal.sortedWith(compareBy<Row> { it.n.hopsAway.let { h -> if (h < 0) Int.MAX_VALUE else h } }.thenByDescending { it.n.snr })
            }
        fun line(r: Row): String {
            val n = r.n
            val ago = if (n.lastHeard == 0) "never" else "${((now - n.lastHeard) / SECONDS_PER_MINUTE).coerceAtLeast(0)} min ago"
            return "- ${n.user.short_name} (${n.user.long_name}): hops=${n.hopsAway}; " +
                "SNR=${if (n.snr == Float.MAX_VALUE) "unknown" else fmt(n.snr) + " dB"}; " +
                "RSSI=${if (n.rssi == Int.MAX_VALUE) "unknown" else n.rssi.toString() + " dBm"}; " +
                "distance=${r.meters?.let { m -> fmt(m / METERS_PER_KM) + " km" } ?: "unknown"}; heard $ago"
        }
        return buildString {
            appendLine(
                "SIGNAL / RANGE from the own node (${rows.size} nodes; ${heardWithSignal.size} with a signal reading). " +
                    "Higher SNR is better; RSSI closer to 0 is better; hops 0 means heard directly. " +
                    if (closest || byDistance) "Sorted by distance:" else "Best range = direct nodes (hops 0) with the highest SNR first:",
            )
            sorted.take(top).forEach { appendLine(line(it)) }
        }
    }

    private class NodeRow(val num: Int) {
        var statsTime = 0L
        var rx: Int? = null
        var tx: Int? = null
        var bad: Int? = null
        var dupe: Int? = null
        var uptime: Int? = null
        var heapUsed: Float? = null
        var devTime = 0L
        var battery: Int? = null
        var chUtil: Float? = null
        var airUtil: Float? = null
    }

    /** The latest telemetry every node reported (last 3 days), ranked by what the question is about. */
    private suspend fun ranking(q: String, top: Int = RANK_TOP): String {
        val logs = meshLogRepository.getLogsByPortNumSince(PortNum.TELEMETRY_APP.value, nowMillis - RANK_WINDOW_MS).first()
        val rows = HashMap<Int, NodeRow>()
        for (log in logs) {
            val payload = log.fromRadio.packet?.decoded?.payload ?: continue
            val t = runCatching { Telemetry.ADAPTER.decode(payload) }.getOrNull() ?: continue
            // Logs of the own node carry the id 0.
            val owner = if (log.fromNum == 0) (nodeRepository.ourNodeInfo.value?.num ?: 0) else log.fromNum
            val row = rows.getOrPut(owner) { NodeRow(owner) }
            t.local_stats?.let { ls ->
                if (log.received_date >= row.statsTime) {
                    row.statsTime = log.received_date
                    row.rx = ls.num_packets_rx
                    row.tx = ls.num_packets_tx
                    row.bad = ls.num_packets_rx_bad
                    row.dupe = ls.num_rx_dupe
                    row.uptime = ls.uptime_seconds
                    row.heapUsed = if ((ls.heap_total_bytes ?: 0) > 0) ((ls.heap_total_bytes!! - (ls.heap_free_bytes ?: 0)).coerceAtLeast(0)).toFloat() / ls.heap_total_bytes!! * PERCENT else null
                }
            }
            t.device_metrics?.let { dm ->
                if (log.received_date >= row.devTime) {
                    row.devTime = log.received_date
                    row.battery = dm.battery_level
                    row.chUtil = dm.channel_utilization
                    row.airUtil = dm.air_util_tx
                    if (row.uptime == null) row.uptime = dm.uptime_seconds
                }
            }
        }
        if (rows.isEmpty()) return "NODE RANKING: no telemetry from nodes in the last 3 days."
        data class Key(val label: String, val descending: Boolean, val pick: (NodeRow) -> Float?)
        val key =
            when {
                q.hasAny(listOf("wysł", "wysl", "nadan", " tx", "transmit", "sent")) -> Key("packets sent (tx)", true) { it.tx?.toFloat() }
                q.hasAny(listOf("duplik", "dupe")) -> Key("duplicate packets received", true) { it.dupe?.toFloat() }
                q.hasAny(listOf("błęd", "bled", "uszkodz", "bad ")) -> Key("bad packets received", true) { it.bad?.toFloat() }
                q.hasAny(listOf("heap", "pamię", "pamie", " ram")) -> Key("used heap percent (highest first)", true) { it.heapUsed }
                q.hasAny(listOf("bateri", "battery")) -> Key("battery percent (lowest first)", false) { it.battery?.toFloat() }
                q.hasAny(listOf("zajęt", "zajet", "kanał", "kanal", "channel", "utiliz")) -> Key("channel utilization percent", true) { it.chUtil }
                q.hasAny(listOf("uptime", "działa od", "dziala od", "restart")) -> Key("uptime", true) { it.uptime?.toFloat() }
                else -> Key("packets received (rx)", true) { it.rx?.toFloat() }
            }
        val withValue = rows.values.filter { key.pick(it) != null }
        val sorted = if (key.descending) withValue.sortedByDescending { key.pick(it) } else withValue.sortedBy { key.pick(it) }
        fun line(r: NodeRow): String {
            val n = nodeRepository.nodeDBbyNum.value[r.num]
            val name = n?.let { "${it.user.short_name} (${it.user.long_name}), id ${it.user.id}" } ?: "!${r.num.toUInt().toString(HEX)}"
            return "- $name: rx=${r.rx ?: "?"}, tx=${r.tx ?: "?"}, bad=${r.bad ?: "?"}, dupe=${r.dupe ?: "?"}, " +
                "heap_used=${r.heapUsed?.let { fmt(it) } ?: "?"}%, battery=${r.battery ?: "?"}%, ch_util=${r.chUtil?.let { fmt(it) } ?: "?"}%, " +
                "air_tx=${r.airUtil?.let { fmt(it) } ?: "?"}%, uptime=${r.uptime?.let { it / SECONDS_PER_HOUR } ?: "?"} h"
        }
        return buildString {
            appendLine(
                "NODE RANKING by ${key.label}: ${sorted.size} of ${rows.size} nodes reported. Counters are the node's own totals since " +
                    "it started. Best first:",
            )
            sorted.take(top).forEach { appendLine(line(it)) }
            if (sorted.size > top + RANK_BOTTOM) {
                appendLine("... Last ones:")
                sorted.takeLast(RANK_BOTTOM).forEach { appendLine(line(it)) }
            }
        }
    }

    private fun networkSummary(): String {
        val nodes = nodeRepository.nodeDBbyNum.value.values
        if (nodes.isEmpty()) return ""
        val now = nowMillis / MILLIS_PER_SECOND
        val recent = nodes.count { it.lastHeard != 0 && now - it.lastHeard < ONE_DAY_S }
        val byHops = nodes.groupingBy { it.hopsAway }.eachCount().toSortedMap().entries.joinToString { "${it.key}:${it.value}" }
        val low = nodes.filter { (it.deviceMetrics.battery_level ?: 100) in 1..LOW_BATTERY }.joinToString { it.user.short_name }
        val hardware = nodes.groupingBy { it.user.hw_model.toString() }.eachCount().entries.sortedByDescending { it.value }.take(HW_LIMIT).joinToString { "${it.key}:${it.value}" }
        return "NETWORK SUMMARY: ${nodes.size} nodes known, $recent heard in the last 24 h. Nodes by hops away (hops:count): $byHops. " +
            "Low battery (<= $LOW_BATTERY%): ${low.ifBlank { "none" }}. Hardware: $hardware."
    }

    /** Every known node in a few characters, so the assistant knows the whole network and can ask for details. */
    private fun nodeIndex(): String {
        val nodes = nodeRepository.nodeDBbyNum.value.values.sortedByDescending { it.lastHeard }
        return "ALL KNOWN NODES (${nodes.size}, short=long name): " +
            nodes.joinToString("; ") { "${it.user.short_name}=${it.user.long_name}" }.take(INDEX_CHARS) +
            ". Ask the user to name a node to see its details and full history."
    }

    private fun spanText(timeMs: Long): String {
        val minutes = ((nowMillis - timeMs) / MILLIS_PER_SECOND / SECONDS_PER_MINUTE).coerceAtLeast(0)
        return if (minutes >= MINUTES_PER_DAY) "${minutes / MINUTES_PER_DAY} d" else if (minutes >= MINUTES_PER_HOUR) "${minutes / MINUTES_PER_HOUR} h" else "$minutes min"
    }

    private suspend fun settings(): String {
        val local = radioConfigRepository.localConfigFlow.first().toString().redacted()
        val module = radioConfigRepository.moduleConfigFlow.first().toString().redacted()
        val channels = radioConfigRepository.channelSetFlow.first()
        return buildString {
            appendLine("RADIO SETTINGS (read only, keys and passwords removed):")
            appendLine(local.take(SETTINGS_CHARS))
            appendLine("Modules: " + module.take(SETTINGS_CHARS))
            appendLine("Channels: " + channels.settings.joinToString { it.name.ifBlank { "(default)" } })
            appendLine("LoRa: " + channels.lora_config.toString())
        }
    }

    private suspend fun messages(): String {
        val contacts = packetRepository.getContacts().first().entries.sortedByDescending { it.value.time }.take(CHAT_LIMIT)
        return buildString {
            appendLine("LATEST MESSAGES (one per conversation):")
            for ((key, packet) in contacts) {
                appendLine("- [$key] from ${packet.from}: ${packet.text.orEmpty().take(MESSAGE_CHARS)}")
            }
        }
    }

    private class Sample(
        val time: Long,
        val heapUsed: Float?,
        val psramFree: Float?,
        val cpu: Float?,
        val flashUsed: Float?,
        val battery: Float?,
        val channelUtil: Float?,
        val airUtil: Float?,
        val noise: Float?,
    )

    private fun MeshLog.toSample(): Sample? {
        val payload = fromRadio.packet?.decoded?.payload ?: return null
        val t = runCatching { Telemetry.ADAPTER.decode(payload) }.getOrNull() ?: return null
        val ext = t.decodeLocalStatsExtended()
        val heapTotal = t.local_stats?.heap_total_bytes ?: 0
        val heapFree = t.local_stats?.heap_free_bytes ?: 0
        return Sample(
            time = received_date,
            heapUsed = if (heapTotal > 0) (heapTotal - heapFree).coerceAtLeast(0).toFloat() / heapTotal * PERCENT else null,
            psramFree = ext?.takeIf { it.memoryPsramTotal > 0 }?.let { it.memoryPsramFree.toFloat() / it.memoryPsramTotal * PERCENT },
            cpu = ext?.cpuUsagePercent?.toFloat(),
            flashUsed = ext?.takeIf { it.flashTotalBytes > 0 }?.let { it.flashUsedBytes.toFloat() / it.flashTotalBytes * PERCENT },
            battery = t.device_metrics?.battery_level?.toFloat(),
            channelUtil = t.device_metrics?.channel_utilization,
            airUtil = t.device_metrics?.air_util_tx,
            noise = t.local_stats?.noise_floor?.takeUnless { it == 0 }?.toFloat(),
        )
    }

    // ---- Entry points for the assistant's tools (all read-only) ----

    suspend fun toolOverview(): String = overview() + "\n" + networkSummary()

    /** Details and recorded history of one node found by (part of) its name; [hours] 0 means the whole history. */
    suspend fun toolNode(name: String, hours: Int): String {
        val node = findNode(name) ?: return "No node found for \"$name\". Use the node list tool to see the names."
        val since = if (hours > 0) nowMillis - hours * MILLIS_PER_HOUR else 0L
        return nodeDetail(node, since)
    }

    /** All known nodes with short and long name. */
    fun toolNodeNames(): String {
        val nodes = nodeRepository.nodeDBbyNum.value.values.sortedByDescending { it.lastHeard }
        return "ALL KNOWN NODES (${nodes.size}, short=long name): " + nodes.joinToString("; ") { "${it.user.short_name}=${it.user.long_name}" }
    }

    /**
     * A ranked table of nodes. [sortBy]: rx, tx, dupe, bad, heap, battery, channel_util, uptime (telemetry); snr, rssi,
     * distance, closest, hops (link quality).
     */
    suspend fun toolList(sortBy: String, limit: Int): String {
        val top = limit.coerceIn(1, MAX_TOOL_LIST)
        return when (sortBy.trim().lowercase()) {
            "snr", "rssi", "signal", "range", "hops" -> signalReport("snr")
            "distance", "far", "farthest" -> signalReport("odległ")
            "closest", "nearest", "near" -> signalReport("najbliż")
            "tx" -> ranking("wysł", top)
            "dupe", "duplicates" -> ranking("dupe", top)
            "bad" -> ranking("bad ", top)
            "heap", "ram" -> ranking("heap", top)
            "battery" -> ranking("battery", top)
            "channel_util", "channel", "utilization" -> ranking("channel", top)
            "uptime" -> ranking("uptime", top)
            else -> ranking("odebran", top)
        }
    }

    suspend fun toolSensors(): String = sensorReport()

    suspend fun toolSettings(): String = settings()

    /** Messages containing [query] (all conversations); an empty [query] gives the newest ones. */
    suspend fun toolMessages(query: String, limit: Int): String {
        val top = limit.coerceIn(1, MAX_TOOL_LIST)
        val needle = query.trim().lowercase()
        val keys = packetRepository.getContacts().first().keys
        val found = ArrayList<Triple<Long, String, String>>()
        for (key in keys) {
            val list = packetRepository.getMessagesFrom(key, SEARCH_PER_CHAT) { id -> nodeRepository.getNode(id.orEmpty()) }.first()
            for (m in list) {
                val text = m.text
                if (needle.isNotEmpty() && !text.lowercase().contains(needle)) continue
                val who = if (m.fromLocal) "me" else m.node.user.short_name
                found += Triple(m.receivedTime, key, "$who: ${text.take(MESSAGE_CHARS)}")
            }
        }
        if (found.isEmpty()) return "No messages found."
        val now = nowMillis
        return "MESSAGES (newest first):\n" +
            found.sortedByDescending { it.first }.take(top).joinToString("\n") {
                "- [${it.second}] ${((now - it.first) / MILLIS_PER_MINUTE).coerceAtLeast(0)} min ago, ${it.third}"
            }
    }

    /**
     * The latest messages of one channel in the order they were written, ready to be summarised. [channel] is the
     * channel name (or its number); empty means every channel. Direct messages are not included.
     */
    suspend fun toolChannelMessages(channel: String, limit: Int): String {
        val top = limit.coerceIn(1, MAX_TOOL_LIST)
        val names = radioConfigRepository.channelSetFlow.first().settings.map { it.name }
        val wanted = channel.trim().lowercase()
        val index = wanted.toIntOrNull() ?: numberedChannel(wanted) ?: names.indexOfFirst { it.lowercase() == wanted }.takeIf { it >= 0 }
            ?: names.indexOfFirst { wanted.isNotEmpty() && it.lowercase().contains(wanted) }.takeIf { it >= 0 }
        val keys = packetRepository.getContacts().first().keys.filter { it.endsWith("^all") && (wanted.isEmpty() || index == null || it.startsWith(index.toString() + "^")) }
        if (keys.isEmpty() || (wanted.isNotEmpty() && index == null)) {
            return "No channel conversation found for \"$channel\". Channels: " +
                names.mapIndexed { i, n -> "$i=${n.ifBlank { "(default)" }}" }.joinToString()
        }
        val all = ArrayList<Triple<Long, String, String>>()
        for (key in keys) {
            val chName = key.substringBefore('^').toIntOrNull()?.let { names.getOrNull(it)?.ifBlank { "(default)" } } ?: key
            val list = packetRepository.getMessagesFrom(key, top) { id -> nodeRepository.getNode(id.orEmpty()) }.first()
            for (m in list) {
                val who = if (m.fromLocal) "me" else m.node.user.long_name.ifBlank { m.node.user.short_name }
                all += Triple(m.receivedTime, chName, "$who: ${m.text.take(MESSAGE_CHARS)}")
            }
        }
        if (all.isEmpty()) return "No messages in this channel yet."
        val now = nowMillis
        return "CHANNEL MESSAGES (oldest first, last ${minOf(top, all.size)}):\n" +
            all.sortedByDescending { it.first }.take(top).sortedBy { it.first }.joinToString("\n") {
                "- [${it.second}] ${((now - it.first) / MILLIS_PER_MINUTE).coerceAtLeast(0)} min ago, ${it.third}"
            }
    }

    /** A channel mentioned in the question (by name, or by number such as "kanał 1" / "kanał jeden"), as text, or empty. */
    private suspend fun channelFromQuestion(q: String): String {
        val names = radioConfigRepository.channelSetFlow.first().settings.map { it.name }
        return numberedChannel(q)?.toString() ?: names.firstOrNull { it.length >= MIN_NAME && q.contains(it.lowercase()) }.orEmpty()
    }

    private fun numberedChannel(text: String): Int? {
        val m = CHANNEL_NUMBER.find(text.lowercase()) ?: return null
        val word = m.groupValues[1]
        return word.toIntOrNull() ?: NUMBER_WORDS.entries.firstOrNull { word.startsWith(it.key) }?.value
    }

    /** How many messages every channel (and direct messages together) holds, written in code. */
    suspend fun channelReport(): String {
        val names = radioConfigRepository.channelSetFlow.first().settings.map { it.name }
        val counts = packetRepository.getMessageCounts()
        val perChannel = HashMap<Int, Int>()
        var direct = 0
        for ((key, count) in counts) {
            if (key.endsWith("^all")) {
                val i = key.substringBefore('^').toIntOrNull()
                if (i != null) perChannel[i] = (perChannel[i] ?: 0) + count else direct += count
            } else {
                direct += count
            }
        }
        if (perChannel.isEmpty() && direct == 0) return "Nie ma jeszcze żadnych zapisanych wiadomości."
        val sb = StringBuilder("Liczba zapisanych wiadomości na kanałach (od największej):\n")
        for ((i, c) in perChannel.entries.sortedByDescending { it.value }) {
            val label = names.getOrNull(i)?.ifBlank { null } ?: "domyślny"
            sb.append("• kanał $i ($label): $c\n")
        }
        sb.append("• wiadomości prywatne (razem): $direct")
        return sb.toString()
    }

    private fun findNode(name: String): org.meshtastic.core.model.Node? {
        val q = name.trim().lowercase()
        if (q.isEmpty()) return null
        val all = nodeRepository.nodeDBbyNum.value.values
        if (q in OWN_WORDS) return nodeRepository.ourNodeInfo.value
        return all.firstOrNull { it.user.short_name.lowercase() == q }
            ?: all.firstOrNull { it.user.long_name.lowercase() == q }
            ?: all.firstOrNull { it.user.long_name.lowercase().contains(q) }
            ?: all.firstOrNull { it.user.short_name.lowercase().contains(q) }
            ?: all.firstOrNull { it.user.id.lowercase() == q }
    }

    /**
     * Turns raw field dumps such as `temperature=21.43876, relative_humidity=55.1234` into readable values with units and
     * at most one or two decimals (`temperature=21.4 °C`), so the model does not have to guess units or copy long numbers.
     */
    private fun String.pretty(): String {
        val withFloats =
            FLOAT_FIELD.replace(this) { m ->
                val key = m.groupValues[1]
                val decimals = if (key in TWO_DECIMALS) 2 else 1
                val value = roundTo(m.groupValues[2].toDoubleOrNull() ?: return@replace m.value, decimals)
                "$key=$value${UNITS[key].orEmpty()}${plausibility(key, value.toDoubleOrNull())}"
            }
        return INT_FIELD.replace(withFloats) { m ->
            val key = m.groupValues[1]
            val number = m.groupValues[2]
            when {
                key.endsWith("uptime_seconds") -> number.toLongOrNull()?.let { "$key=$number (${duration(it)})" } ?: m.value
                key.endsWith("_bytes") -> number.toLongOrNull()?.let { "$key=$number (${it / BYTES_PER_KB} kB)" } ?: m.value
                else -> UNITS[key]?.let { "$key=$number$it${plausibility(key, number.toDoubleOrNull())}" } ?: m.value
            }
        }
    }

    /** Marks readings that cannot be real (a faulty or unplugged sensor) so the model says so instead of believing them. */
    private fun plausibility(key: String, value: Double?): String {
        val range = PLAUSIBLE[key] ?: return ""
        return if (value != null && (value < range.start || value > range.endInclusive)) {
            " [IMPLAUSIBLE: probably a faulty or missing sensor]"
        } else {
            ""
        }
    }

    private fun duration(seconds: Long): String {
        val days = seconds / SECONDS_PER_DAY
        val hours = seconds % SECONDS_PER_DAY / SECONDS_PER_HOUR
        val minutes = seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
        return if (days > 0) "${days} d ${hours} h" else if (hours > 0) "${hours} h ${minutes} min" else "${minutes} min"
    }

    private fun roundTo(value: Double, decimals: Int): String {
        val factor = if (decimals == 2) 100.0 else 10.0
        return (kotlin.math.round(value * factor) / factor).toString()
    }

    private fun String.hasAny(words: List<String>) = words.any { contains(it) }

    private fun String.redacted(): String = replace(SECRET, "$1=<hidden>")

    private fun fmt(value: Float): String = ((value * 10).roundToInt() / 10f).toString()

    private companion object {
        const val TAG = "AssistantData"
        const val MAX_CONTEXT_CHARS = 3000
        const val MIN_PART = 250
        val REPORT_KEYS =
            mapOf(
                "temperature" to "temperatura %s °C", "relative_humidity" to "wilgotność %s %",
                "barometric_pressure" to "ciśnienie %s hPa", "voltage" to "napięcie %s V", "current" to "prąd %s mA",
                "lux" to "światło %s lx", "wind_speed" to "wiatr %s m/s",
            )
        val PLAUSIBLE =
            mapOf(
                "temperature" to (-60.0..80.0), "soil_temperature" to (-60.0..80.0),
                "relative_humidity" to (0.0..100.0), "soil_moisture" to (0.0..100.0),
                "barometric_pressure" to (300.0..1100.0), "battery_level" to (0.0..101.0),
                "channel_utilization" to (0.0..100.0), "air_util_tx" to (0.0..100.0),
            )
        const val BYTES_PER_KB = 1024
        const val SECONDS_PER_DAY = 86_400L
        val FLOAT_FIELD = Regex("([a-z_0-9]+)=(-?\\d+\\.\\d+(?:E-?\\d+)?)")
        val INT_FIELD = Regex("([a-z_0-9]+)=(-?\\d+)(?![\\d.])")
        val TWO_DECIMALS = setOf("voltage", "current", "ch1_voltage", "ch2_voltage", "ch3_voltage", "ch1_current", "ch2_current", "ch3_current")
        val UNITS =
            mapOf(
                "temperature" to " °C", "soil_temperature" to " °C", "relative_humidity" to " %", "soil_moisture" to " %",
                "barometric_pressure" to " hPa", "voltage" to " V", "ch1_voltage" to " V", "ch2_voltage" to " V",
                "ch3_voltage" to " V", "current" to " mA", "ch1_current" to " mA", "ch2_current" to " mA",
                "ch3_current" to " mA", "lux" to " lx", "white_lux" to " lx", "ir_lux" to " lx", "uv_lux" to " lx",
                "wind_speed" to " m/s", "wind_gust" to " m/s", "wind_lull" to " m/s", "wind_direction" to " °",
                "gas_resistance" to " MOhm", "distance" to " mm", "weight" to " kg", "rainfall_1h" to " mm",
                "rainfall_24h" to " mm", "battery_level" to " %", "channel_utilization" to " %", "air_util_tx" to " %",
                "noise_floor" to " dBm", "pm10_standard" to " ug/m3", "pm25_standard" to " ug/m3",
                "pm100_standard" to " ug/m3", "pm10_environmental" to " ug/m3", "pm25_environmental" to " ug/m3",
                "pm100_environmental" to " ug/m3", "radiation" to " uR/h",
            )
        const val SUMMARY_MESSAGES = 20
        const val OVERVIEW_SHORT = 380
        const val MIN_NAME = 3
        const val RANK_WINDOW_MS = 3 * 24 * 60 * 60 * 1000L
        const val RANK_TOP = 8
        const val SIGNAL_TOP = 8
        const val REPORT_SIGNAL = 15
        const val SENSOR_LIMIT = 12
        const val REPORT_LIMIT = 40
        val CHANNEL_NUMBER = Regex("(?:kana[łl]\\w*|channel)\\s*(?:nr\\.?|numer\\w*|#|no\\.?)?\\s*(\\d+|[a-ząćęłńóśźż]+)")
        val NUMBER_WORDS =
            mapOf("zer" to 0, "jede" to 1, "jedyn" to 1, "one" to 1, "pierw" to 1, "dw" to 2, "two" to 2, "trz" to 3, "three" to 3, "czter" to 4, "four" to 4, "pię" to 5, "pie" to 5, "five" to 5, "sze" to 6, "six" to 6, "sied" to 7, "seven" to 7)
        const val MAX_TOOL_LIST = 40
        const val SEARCH_PER_CHAT = 60
        const val MILLIS_PER_HOUR = 3_600_000L
        const val MILLIS_PER_MINUTE = 60_000L
        const val METERS_PER_KM = 1000f
        const val RANK_BOTTOM = 3
        const val HEX = 16
        const val SECONDS_PER_HOUR = 3600
        const val MAX_NODES_DETAIL = 2
        const val ONE_DAY_S = 86_400L
        const val LOW_BATTERY = 20
        const val HW_LIMIT = 6
        const val INDEX_CHARS = 700
        const val MINUTES_PER_HOUR = 60L
        const val MINUTES_PER_DAY = 1440L
        const val SETTINGS_CHARS = 1000
        const val CHAT_LIMIT = 8
        const val MESSAGE_CHARS = 120
        const val PERCENT = 100f
        const val STABLE_DELTA = 1f
        const val MILLIS_PER_SECOND = 1000L
        const val SECONDS_PER_MINUTE = 60L
        val SECRET = Regex("(private_key|public_key|admin_key|wifi_psk|psk|password|[a-z_]*_key)=(\\[[^\\]]*\\]|[^,}]*)")
        val TREND_WORDS =
            listOf("ram", "heap", "pamię", "pamie", "memory", "cpu", "procesor", "flash", "psram", "bateri", "battery",
                "tendenc", "trend", "spada", "rośnie", "rosnie", "zajęt", "zajet", "utiliz", "szum", "noise", "wykres",
                "stat", "telemetr", "wydajn", "obciąż", "obciaz")
        val SETTINGS_WORDS =
            listOf("ustawien", "konfig", "setting", "config", "region", "preset", "kanał", "kanal", "channel", "lora",
                "rola", "role", "moc", "power", "wifi", "bluetooth", "mqtt", "hop")
        val NODE_WORDS = listOf("węz", "wez", "node", "sąsiad", "sasiad", "zasięg", "zasieg", "sieć", "siec", "mesh", "snr", "rssi", "kto")
        val SENSOR_WORDS = listOf("pogod", "temperatur", "wilgot", "ciśnien", "cisnien", "weather", "humidity", "pressure", "wiatr", "wind", "deszcz", "rain", "powietrz", "air quality", "pm2", "czujnik", "sensor", "środowisk", "srodowisk", "environment", "natężen", "napięci", "prąd", "prad", "voltage", "current", "gleb", "soil", "pax", "lux", "światł")
        val SIGNAL_WORDS = listOf("zasięg", "zasieg", "sygnał", "sygnal", "snr", "rssi", "odległ", "odleg", "daleko", "dystans", "blisk", "najbliż", "najbliz", "jakość", "jakosc", "range", "signal", "distance", "linku", "łącz", "polacz")
        val RANK_WORDS = listOf("ramek", "ramki", "pakiet", "odebran", "raportuj", "najwięc", "najwiec", "najmniej", "ranking", "który", "ktory", "najlep", "najgorz")
        val OWN_WORDS = listOf("mój", "moj", "mojego", "mojej", "mojemu", "my node", "own node", "mój węz", "moim")
        val SUMMARY_WORDS = listOf("streść", "streszcz", "podsumuj", "podsumowa", "summar", "o czym", "co pisz", "co się dzieje na", "co sie dzieje na")
        val MESSAGE_WORDS = listOf("wiadomo", "message", "czat", "chat", "napisa", "pisa", "rozmow", "odpowied")
    }
}
