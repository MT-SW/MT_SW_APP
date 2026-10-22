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
    suspend fun contextFor(question: String): String {
        val q = question.lowercase()
        val mentioned = mentionedNodes(q)
        val parts = ArrayList<String>()
        parts += safe("overview") { overview() }
        for (node in mentioned) parts += safe("node") { nodeDetail(node) }
        if (mentioned.isEmpty() && q.hasAny(TREND_WORDS)) {
            val myNum = nodeRepository.ourNodeInfo.value?.num
            if (myNum != null) parts += safe("trend") { trends(myNum, "own node", 0L) }
        }
        if (q.hasAny(SETTINGS_WORDS)) parts += safe("settings") { settings() }
        if (q.hasAny(SIGNAL_WORDS)) parts += safe("signal") { signalRanking(q) }
        if (q.hasAny(NODE_WORDS) || q.hasAny(TREND_WORDS) || q.hasAny(RANK_WORDS) || q.hasAny(SIGNAL_WORDS)) {
            parts += safe("ranking") { ranking(q) }
            parts += safe("network") { networkSummary() }
        }
        parts += safe("index") { nodeIndex() }
        if (q.hasAny(MESSAGE_WORDS)) parts += safe("messages") { messages() }
        return parts.filter { it.isNotBlank() }.joinToString("\n\n").take(MAX_CONTEXT_CHARS)
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
            if (me != null) appendLine("Device metrics: ${me.deviceMetrics}")
            appendLine("Nodes online/total: ${nodeRepository.onlineNodeCount.first()}/${nodeRepository.totalNodeCount.first()}")
            appendLine("Radio statistics: $stats")
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
                Triple("heap_free_percent (free internal RAM of the radio, higher is better)", "%", { s: Sample -> s.heapFree }),
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

    private suspend fun nodeDetail(n: org.meshtastic.core.model.Node): String {
        val now = nowMillis / MILLIS_PER_SECOND
        val heard = if (n.lastHeard == 0) "never" else "${((now - n.lastHeard) / SECONDS_PER_MINUTE).coerceAtLeast(0)} min ago"
        val head = buildString {
            appendLine("NODE ${n.user.long_name} (${n.user.short_name}), id ${n.user.id}, hardware ${n.user.hw_model}, role ${n.user.role}")
            appendLine("Heard $heard; hops=${n.hopsAway}; SNR=${if (n.snr == Float.MAX_VALUE) "unknown" else fmt(n.snr) + " dB"}; RSSI=${if (n.rssi == Int.MAX_VALUE) "unknown" else n.rssi.toString() + " dBm"}; favourite=${n.isFavorite}; via MQTT=${n.viaMqtt}")
            appendLine("Position: ${n.position}")
            appendLine("Device metrics: ${n.deviceMetrics}")
            appendLine("Environment: ${n.environmentMetrics}")
            if (n.notes.isNotBlank()) appendLine("Notes: ${n.notes}")
        }
        return head + safe("node trends") { trends(n.num, n.user.long_name, 0L) }
    }

    /** Link quality to every node we hear: SNR, RSSI, hops and the distance from our own node. */
    private fun signalRanking(q: String): String {
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
            sorted.take(SIGNAL_TOP).forEach { appendLine(line(it)) }
        }
    }

    private class NodeRow(val num: Int) {
        var statsTime = 0L
        var rx: Int? = null
        var tx: Int? = null
        var bad: Int? = null
        var dupe: Int? = null
        var uptime: Int? = null
        var heapFree: Float? = null
        var devTime = 0L
        var battery: Int? = null
        var chUtil: Float? = null
        var airUtil: Float? = null
    }

    /** The latest telemetry every node reported (last 3 days), ranked by what the question is about. */
    private suspend fun ranking(q: String): String {
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
                    row.heapFree = if ((ls.heap_total_bytes ?: 0) > 0) (ls.heap_free_bytes ?: 0).toFloat() / ls.heap_total_bytes!! * PERCENT else null
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
                q.hasAny(listOf("heap", "pamię", "pamie", " ram")) -> Key("free heap percent (lowest first)", false) { it.heapFree }
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
                "heap_free=${r.heapFree?.let { fmt(it) } ?: "?"}%, battery=${r.battery ?: "?"}%, ch_util=${r.chUtil?.let { fmt(it) } ?: "?"}%, " +
                "air_tx=${r.airUtil?.let { fmt(it) } ?: "?"}%, uptime=${r.uptime?.let { it / SECONDS_PER_HOUR } ?: "?"} h"
        }
        return buildString {
            appendLine(
                "NODE RANKING by ${key.label}: ${sorted.size} of ${rows.size} nodes reported. Counters are the node's own totals since " +
                    "it started. Best first:",
            )
            sorted.take(RANK_TOP).forEach { appendLine(line(it)) }
            if (sorted.size > RANK_TOP + RANK_BOTTOM) {
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
        val heapFree: Float?,
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
            heapFree = if (heapTotal > 0) heapFree.toFloat() / heapTotal * PERCENT else null,
            psramFree = ext?.takeIf { it.memoryPsramTotal > 0 }?.let { it.memoryPsramFree.toFloat() / it.memoryPsramTotal * PERCENT },
            cpu = ext?.cpuUsagePercent?.toFloat(),
            flashUsed = ext?.takeIf { it.flashTotalBytes > 0 }?.let { it.flashUsedBytes.toFloat() / it.flashTotalBytes * PERCENT },
            battery = t.device_metrics?.battery_level?.toFloat(),
            channelUtil = t.device_metrics?.channel_utilization,
            airUtil = t.device_metrics?.air_util_tx,
            noise = t.local_stats?.noise_floor?.takeUnless { it == 0 }?.toFloat(),
        )
    }

    private fun String.hasAny(words: List<String>) = words.any { contains(it) }

    private fun String.redacted(): String = replace(SECRET, "$1=<hidden>")

    private fun fmt(value: Float): String = ((value * 10).roundToInt() / 10f).toString()

    private companion object {
        const val TAG = "AssistantData"
        const val MAX_CONTEXT_CHARS = 8000
        const val MIN_NAME = 3
        const val RANK_WINDOW_MS = 3 * 24 * 60 * 60 * 1000L
        const val RANK_TOP = 12
        const val SIGNAL_TOP = 12
        const val METERS_PER_KM = 1000f
        const val RANK_BOTTOM = 4
        const val HEX = 16
        const val SECONDS_PER_HOUR = 3600
        const val MAX_NODES_DETAIL = 2
        const val ONE_DAY_S = 86_400L
        const val LOW_BATTERY = 20
        const val HW_LIMIT = 6
        const val INDEX_CHARS = 2200
        const val MINUTES_PER_HOUR = 60L
        const val MINUTES_PER_DAY = 1440L
        const val SETTINGS_CHARS = 1800
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
        val SIGNAL_WORDS = listOf("zasięg", "zasieg", "sygnał", "sygnal", "snr", "rssi", "odległ", "odleg", "daleko", "dystans", "blisk", "najbliż", "najbliz", "jakość", "jakosc", "range", "signal", "distance", "linku", "łącz", "polacz")
        val RANK_WORDS = listOf("ramek", "ramki", "pakiet", "odebran", "raportuj", "najwięc", "najwiec", "najmniej", "ranking", "który", "ktory", "najlep", "najgorz")
        val OWN_WORDS = listOf("mój", "moj", "mojego", "mojej", "mojemu", "my node", "own node", "mój węz", "moim")
        val MESSAGE_WORDS = listOf("wiadomo", "message", "czat", "chat", "napisa", "pisa", "rozmow", "odpowied")
    }
}
