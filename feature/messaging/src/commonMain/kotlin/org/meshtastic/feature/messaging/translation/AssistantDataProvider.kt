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
import org.jetbrains.compose.resources.StringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.resources.ai_net_roles
import org.meshtastic.core.resources.ai_net_hops
import org.meshtastic.core.resources.ai_net_low_battery
import org.meshtastic.core.resources.ai_net_none
import org.meshtastic.core.resources.ai_hops_direct
import org.meshtastic.core.resources.ai_bat_line
import org.meshtastic.core.resources.ai_bat_line_no_voltage
import org.meshtastic.core.resources.ai_bat_unknown
import org.meshtastic.core.resources.ai_far_one
import org.meshtastic.core.resources.ai_near_one
import org.meshtastic.core.resources.ai_dist_next
import org.meshtastic.core.resources.ai_nodes_recent
import org.meshtastic.core.resources.ai_dm_title
import org.meshtastic.core.resources.ai_dm_none
import org.meshtastic.core.resources.ai_radio_title
import org.meshtastic.core.resources.ai_radio_uptime
import org.meshtastic.core.resources.ai_radio_util
import org.meshtastic.core.resources.ai_radio_packets
import org.meshtastic.core.resources.ai_radio_relay
import org.meshtastic.core.resources.ai_radio_nodes
import org.meshtastic.core.resources.ai_radio_heap
import org.meshtastic.core.resources.ai_radio_noise
import org.meshtastic.core.resources.ai_radio_ext
import org.meshtastic.core.resources.ai_radio_none
import org.meshtastic.core.resources.ai_snr_title
import org.meshtastic.core.resources.ai_snr_last
import org.meshtastic.core.resources.ai_snr_avg
import org.meshtastic.core.resources.ai_snr_no_direct
import org.meshtastic.core.resources.ai_net_count
import org.meshtastic.core.resources.ai_sensor_none
import org.meshtastic.core.resources.ai_sensor_title
import org.meshtastic.core.resources.ai_sensor_temperature
import org.meshtastic.core.resources.ai_sensor_humidity
import org.meshtastic.core.resources.ai_sensor_pressure
import org.meshtastic.core.resources.ai_sensor_voltage
import org.meshtastic.core.resources.ai_sensor_current
import org.meshtastic.core.resources.ai_sensor_light
import org.meshtastic.core.resources.ai_sensor_wind
import org.meshtastic.core.resources.ai_sensor_warmest
import org.meshtastic.core.resources.ai_sensor_coldest
import org.meshtastic.core.resources.ai_sensor_most_humid
import org.meshtastic.core.resources.ai_sensor_highest_pressure
import org.meshtastic.core.resources.ai_chan_none
import org.meshtastic.core.resources.ai_chan_title
import org.meshtastic.core.resources.ai_chan_line
import org.meshtastic.core.resources.ai_chan_private
import org.meshtastic.core.resources.ai_ago_d
import org.meshtastic.core.resources.ai_ago_h
import org.meshtastic.core.resources.ai_ago_min
import org.meshtastic.core.resources.ai_ago_now
import org.meshtastic.core.resources.ai_battery_ext
import org.meshtastic.core.resources.ai_battery_pct
import org.meshtastic.core.resources.ai_battery_unknown
import org.meshtastic.core.resources.ai_cnt_line
import org.meshtastic.core.resources.ai_cnt_top
import org.meshtastic.core.resources.ai_conn_connected
import org.meshtastic.core.resources.ai_conn_connecting
import org.meshtastic.core.resources.ai_conn_disconnected
import org.meshtastic.core.resources.ai_conn_sleep
import org.meshtastic.core.resources.ai_conn_unknown
import org.meshtastic.core.resources.ai_found_n
import org.meshtastic.core.resources.ai_found_none
import org.meshtastic.core.resources.ai_found_none_for
import org.meshtastic.core.resources.ai_heard
import org.meshtastic.core.resources.ai_low_battery_title
import org.meshtastic.core.resources.ai_more
import org.meshtastic.core.resources.ai_msg_delivered
import org.meshtastic.core.resources.ai_msg_enroute
import org.meshtastic.core.resources.ai_msg_error
import org.meshtastic.core.resources.ai_msg_other
import org.meshtastic.core.resources.ai_msg_queued
import org.meshtastic.core.resources.ai_neigh_hears
import org.meshtastic.core.resources.ai_neigh_nobody
import org.meshtastic.core.resources.ai_neigh_none
import org.meshtastic.core.resources.ai_neigh_none_node
import org.meshtastic.core.resources.ai_neigh_title
import org.meshtastic.core.resources.ai_never
import org.meshtastic.core.resources.ai_no_channel
import org.meshtastic.core.resources.ai_nodes_heard_title
import org.meshtastic.core.resources.ai_nodes_match
import org.meshtastic.core.resources.ai_nodes_none
import org.meshtastic.core.resources.ai_none_value
import org.meshtastic.core.resources.ai_private_chat
import org.meshtastic.core.resources.ai_scope_all
import org.meshtastic.core.resources.ai_scope_channel
import org.meshtastic.core.resources.ai_scope_default
import org.meshtastic.core.resources.ai_sig_best
import org.meshtastic.core.resources.ai_sig_closest
import org.meshtastic.core.resources.ai_sig_farthest
import org.meshtastic.core.resources.ai_sig_line
import org.meshtastic.core.resources.ai_sig_position
import org.meshtastic.core.resources.ai_status_conn
import org.meshtastic.core.resources.ai_status_own_title
import org.meshtastic.core.resources.ai_status_title
import org.meshtastic.core.resources.ai_status_unread
import org.meshtastic.core.resources.ai_status_unread_none
import org.meshtastic.core.resources.ai_trace_back
import org.meshtastic.core.resources.ai_trace_hops
import org.meshtastic.core.resources.ai_trace_none
import org.meshtastic.core.resources.ai_trace_none_node
import org.meshtastic.core.resources.ai_trace_there
import org.meshtastic.core.resources.ai_trace_title
import org.meshtastic.core.resources.ai_unknown_node
import org.meshtastic.core.resources.ai_unknown_node_maybe
import org.meshtastic.core.resources.ai_win_all
import org.meshtastic.core.resources.ai_win_last_day
import org.meshtastic.core.resources.ai_win_last_hour
import org.meshtastic.core.resources.ai_win_last_month
import org.meshtastic.core.resources.ai_win_last_n_d
import org.meshtastic.core.resources.ai_win_last_n_h
import org.meshtastic.core.resources.ai_win_last_n_m
import org.meshtastic.core.resources.ai_win_last_n_min
import org.meshtastic.core.resources.ai_win_last_n_w
import org.meshtastic.core.resources.ai_win_last_n_y
import org.meshtastic.core.resources.ai_win_last_week
import org.meshtastic.core.resources.ai_win_last_year
import org.meshtastic.core.resources.ai_win_today
import org.meshtastic.core.resources.ai_win_yesterday
import org.meshtastic.core.resources.ai_wp_from
import org.meshtastic.core.resources.ai_wp_noname
import org.meshtastic.core.resources.ai_wp_none
import org.meshtastic.core.resources.ai_wp_title
import kotlinx.coroutines.flow.first
import org.meshtastic.core.model.ChannelOption
import kotlinx.datetime.offsetAt
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.common.util.systemTimeZone
import org.meshtastic.core.model.MeshLog
import org.meshtastic.core.model.fullRouteDiscovery
import org.meshtastic.core.model.util.decodeLocalStatsExtended
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.proto.PortNum
import org.meshtastic.proto.Telemetry
import kotlin.math.roundToInt
import kotlinx.coroutines.sync.withLock

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
    private val connection: org.meshtastic.core.repository.ConnectionStateProvider? = null,
) {

    /** A text with the data that is useful for answering [question], or an empty text when nothing matches. */
    suspend fun contextFor(question: String, maxChars: Int = MAX_CONTEXT_CHARS): String {
        val q = question.lowercase()
        // No data topic in the question: no data at all, so the model has nothing to dump into an unrelated answer.
        if (!hasTopic(question)) return ""
        // A summary of a channel needs the messages and nothing else: they go first and alone, so the small window of
        // the model is filled with the conversation, not with the network overview.
        if (q.hasAny(SUMMARY_WORDS)) {
            val messages = safe("channel") { toolChannelMessages(channelFromQuestion(q), SUMMARY_MESSAGES) }
            if (messages.isNotBlank()) return messages.take(maxChars)
        }
        val mentioned = mentionedNodes(q)
        // Most useful parts first: the model only fits about 4000 tokens, so later parts are cut when space runs out.
        val parts = ArrayList<String>()
        val priority = ArrayList<String>()
        parts += safe("overview") { overview() }
        for (node in mentioned) parts += safe("node") { nodeDetail(node) }
        if (q.hasAny(SENSOR_WORDS)) parts += safe("sensors") { sensors() }
        if (q.hasAny(SIGNAL_WORDS)) parts += safe("signal") { signalRanking(q) }
        if (q.hasAny(ACTIVITY_WORDS)) parts += safe("activity") { activity(q) }
        if (q.hasAny(STATUS_WORDS)) priority += safe("status") { toolAppStatus() }
        if (q.hasAny(WAYPOINT_WORDS)) priority += safe("waypoints") { toolWaypoints(TRACE_SHOWN) }
        if (q.hasAny(ROUTE_WORDS)) {
            val who = mentioned.firstOrNull()?.user?.long_name.orEmpty()
            priority += safe("traceroute") { toolTraceroutes(who, TRACE_SHOWN) }
            priority += safe("neighbors") { toolNeighbors(who, TRACE_SHOWN) }
        }
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
            if (q.hasAny(COUNT_WORDS)) {
                val w = timeWindow(q) ?: Win(todayStart(nowMillis), Long.MAX_VALUE, Res.string.ai_win_today)
                priority += safe("count") { messageCountReport(channelFromQuestion(q), w) }
            }
            QUOTED.find(question)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let { phrase ->
                priority += safe("search") { toolMessages(phrase, SEARCH_SHOWN, channelFromQuestion(q)) }
            }
            parts += safe("messages") { messages() }
        }
        parts += safe("index") { nodeIndex() }
        // When the question has its own topic, the general overview shrinks so the topic data fits in the small window.
        if (parts.size > 2 || priority.isNotEmpty()) parts[0] = parts[0].take(OVERVIEW_SHORT)
        parts.addAll(0, priority)
        val out = StringBuilder()
        for (part in parts.filter { it.isNotBlank() }) {
            val room = maxChars - out.length
            if (room < MIN_PART) break
            if (out.isNotEmpty()) out.append("\n\n")
            out.append(part.take(room - 2))
        }
        return out.toString()
    }

    /**
     * Questions whose answer is exact data (counts, searches, lists of nodes, routes, status) are answered by code alone,
     * in the language of the app: the small model only garbles such answers. Null for every other question.
     */
    suspend fun directAnswer(question: String): String? {
        val q = question.lowercase()
        if (q.hasAny(SUMMARY_WORDS)) return null
        val channel = channelFromQuestion(q)
        val mentionedName = mentionedNodes(q).firstOrNull()?.user?.long_name.orEmpty()
        fun ok(text: String): String? = text.takeIf { it.isNotBlank() }
        // 1. Word or phrase search in messages.
        val phrase = QUOTED.find(question)?.groupValues?.get(1) ?: WORD_AFTER.find(q)?.groupValues?.get(1)
        if (phrase != null && phrase.isNotBlank() && (q.hasAny(MESSAGE_WORDS) || q.hasAny(SEARCH_WORDS))) {
            return ok(safe("search") { toolMessages(phrase, SEARCH_SHOWN, channel) })
        }
        // 2. Message counts and who writes most.
        if (q.hasAny(MESSAGE_WORDS) && q.hasAny(COUNT_WORDS)) {
            val window = timeWindow(q)
            if (window != null || q.hasAny(TOP_WORDS)) {
                return ok(safe("count") { messageCountReport(channel, window ?: Win(0L, Long.MAX_VALUE, Res.string.ai_win_all)) })
            }
        }
        // 3. Waypoints, connection, unread and delivery.
        if (q.hasAny(WAYPOINT_WORDS)) return ok(safe("waypoints") { toolWaypoints(WAYPOINTS_SHOWN) })
        if (q.hasAny(STATUS_WORDS)) return ok(safe("status") { toolAppStatus() })
        // 4. Routes and neighbours.
        if (q.hasAny(ROUTE_WORDS)) {
            val who = mentionedName
            return if (q.hasAny(NEIGHBOUR_WORDS)) ok(safe("neighbors") { toolNeighbors(who, TRACE_SHOWN) })
            else ok(safe("traceroute") { toolTraceroutes(who, TRACE_SHOWN) })
        }
        val aboutNodes = q.hasAny(NODE_WORDS) || q.hasAny(ROLE_WORDS)
        // 5. Nearest / farthest nodes (also of one role, for example the nearest router).
        if (aboutNodes && q.hasAny(DISTANCE_WORDS)) return ok(safe("signal") { signalReport(q) })
        // 6. Low battery.
        if (q.hasAny(listOf("bateri", "battery")) && q.hasAny(LOW_WORDS)) {
            return ok(safe("battery") { aiString(Res.string.ai_low_battery_title, LOW_BATTERY) + "\n" + toolFindNodes(0, "", 0, LOW_BATTERY, 0, REPORT_SIGNAL) })
        }
        // 7. How many nodes were heard in a time window.
        if (q.hasAny(listOf("ile ", "how many", "liczb")) && q.hasAny(listOf("węzł", "wezl", "node"))) {
            val w = timeWindow(q) ?: Win(nowMillis - MILLIS_PER_DAY, Long.MAX_VALUE, Res.string.ai_win_last_day)
            return ok(
                safe("nodecount") {
                    val all = nodeRepository.nodeDBbyNum.value.values
                    val heard = all.count { it.lastHeard != 0 && it.lastHeard * MILLIS_PER_SECOND >= w.since && it.lastHeard * MILLIS_PER_SECOND < w.until }
                    aiString(Res.string.ai_nodes_heard_title, w.title(), heard, all.size)
                },
            )
        }
        return null
    }

    /** True when the question names a topic the data parts know; a bare follow-up ("a jakie to wartości?") does not. */
    fun hasTopic(question: String): Boolean {
        val q = question.lowercase()
        return mentionedNodes(q).isNotEmpty() ||
            listOf(SENSOR_WORDS, SIGNAL_WORDS, NODE_WORDS, TREND_WORDS, RANK_WORDS, SETTINGS_WORDS, SUMMARY_WORDS, MESSAGE_WORDS, ACTIVITY_WORDS, ROUTE_WORDS, STATUS_WORDS, WAYPOINT_WORDS)
                .any { q.hasAny(it) }
    }

    private class TelemetryEntry(val owner: Int, val time: Long, val telemetry: Telemetry)

    private val telemetryLock = kotlinx.coroutines.sync.Mutex()
    private var telemetryCache: Pair<Long, List<TelemetryEntry>>? = null

    /**
     * The telemetry packets of the last 3 days, decoded once and kept for a short time. A single question can need them
     * several times (data parts, then a tool), and reading and decoding them is what used the most memory.
     */
    private suspend fun telemetry(): List<TelemetryEntry> = telemetryLock.withLock {
        telemetryCache?.takeIf { nowMillis - it.first < TELEMETRY_CACHE_MS }?.second
            ?: run {
                val ownNum = nodeRepository.ourNodeInfo.value?.num ?: 0
                val logs = meshLogRepository.getLogsByPortNumSince(PortNum.TELEMETRY_APP.value, nowMillis - RANK_WINDOW_MS).first()
                val entries =
                    logs.mapNotNull { log ->
                        val payload = log.fromRadio.packet?.decoded?.payload ?: return@mapNotNull null
                        val t = runCatching { Telemetry.ADAPTER.decode(payload) }.getOrNull() ?: return@mapNotNull null
                        // Logs of the own node carry the id 0.
                        TelemetryEntry(if (log.fromNum == 0) ownNum else log.fromNum, log.received_date, t)
                    }
                telemetryCache = nowMillis to entries
                entries
            }
    }

    /** Who was heard most recently, or longest ago, written with exact ages. */
    private fun activity(q: String): String {
        val now = nowMillis / MILLIS_PER_SECOND
        val me = nodeRepository.ourNodeInfo.value?.num
        val nodes = nodeRepository.nodeDBbyNum.value.values.filter { it.num != me }
        val oldFirst = q.hasAny(listOf("offline", "nieaktyw", "dawno", "najdłuż", "najduz", "milcz", "cisz", "zniknęł", "zniknel"))
        val sorted = if (oldFirst) nodes.filter { it.lastHeard != 0 }.sortedBy { it.lastHeard } else nodes.sortedByDescending { it.lastHeard }
        fun age(n: org.meshtastic.core.model.Node): String {
            if (n.lastHeard == 0) return "never"
            val min = ((now - n.lastHeard) / SECONDS_PER_MINUTE).coerceAtLeast(0)
            return if (min >= MINUTES_PER_DAY) "${min / MINUTES_PER_DAY} d ${(min % MINUTES_PER_DAY) / MINUTES_PER_HOUR} h ago" else if (min >= MINUTES_PER_HOUR) "${min / MINUTES_PER_HOUR} h ${min % MINUTES_PER_HOUR} min ago" else "$min min ago"
        }
        return buildString {
            appendLine(if (oldFirst) "NODES NOT HEARD FOR THE LONGEST TIME (longest silence first):" else "NODES HEARD MOST RECENTLY (newest first):")
            sorted.take(ACTIVITY_TOP).forEach { appendLine("- ${it.user.short_name} (${it.user.long_name}): heard ${age(it)}") }
        }
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
        // A place or name in another Polish case ("na Łysicy" for a node called "Łysica"): the word without its ending
        // is looked up in the names; a word that fits many nodes (router, node) says nothing and is skipped.
        val byStem =
            if (byName.isNotEmpty()) {
                emptyList()
            } else {
                q.split(Regex("[^\\p{L}\\p{N}]+")).map { it.fold() }.filter { it.length >= MIN_STEM_WORD && it !in GENERIC_WORDS }
                    .distinct().flatMap { word ->
                        val stem = if (word.length >= MIN_STEM_WORD + 1) word.dropLast(1) else word
                        val hits = all.filter { it.user.long_name.fold().contains(stem) || it.user.short_name.fold() == word }
                        if (hits.size in 1..MAX_STEM_HITS) hits else emptyList()
                    }
            }
        val own = nodeRepository.ourNodeInfo.value
        val mine = if (own != null && q.hasAny(OWN_WORDS)) listOf(own) else emptyList()
        return (byName + byStem + mine).distinctBy { it.num }.take(MAX_NODES_DETAIL)
    }

    /** True when the text names (or clearly points at) a known node. */
    fun mentionsNode(text: String): Boolean = mentionedNodes(text.lowercase()).any { it.num != nodeRepository.ourNodeInfo.value?.num }

    /** The long name of the first node the text points at, or null. */
    fun mentionedNodeName(text: String): String? =
        mentionedNodes(text.lowercase()).firstOrNull()?.let { it.user.long_name.ifBlank { it.user.short_name } }

    /** The tools take "all" for "no filter" (the model cannot write an empty text); this turns it back into empty. */
    private fun String.anyToEmpty(): String =
        trim().lowercase().takeUnless { it in ANY_WORDS || it.all { c -> !c.isLetterOrDigit() } }.orEmpty()

    private fun String.fold(): String =
        lowercase().map { c -> FOLD[c] ?: c }.joinToString("")

    private suspend fun safe(what: String, block: suspend () -> String): String = try {
        block()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.w(tag = TAG, throwable = e) { "Assistant data '$what' unavailable" }
        ""
    }

    /** True when the user writes Polish: the fixed labels of the reports are then Polish too, so the model copies Polish. */
    @Volatile var polish: Boolean = false

    private suspend fun overview(): String {
        val me = nodeRepository.ourNodeInfo.value
        val stats = nodeRepository.localStats.value
        val extended = nodeRepository.localStatsExtended.value
        if (polish) {
            return buildString {
                appendLine("MÓJ WĘZEŁ: ${me?.user?.long_name ?: "nieznany"} (${me?.user?.short_name ?: "?"}), sprzęt ${me?.user?.hw_model}")
                if (me != null) appendLine("Parametry urządzenia: ${me.deviceMetrics.toString().pretty()}")
                appendLine("Węzły aktywne/wszystkie: ${nodeRepository.onlineNodeCount.first()}/${nodeRepository.totalNodeCount.first()}")
                appendLine("Statystyki radia: ${stats.toString().pretty()}")
                appendLine("Statystyki rozszerzone (cpu, flash, psram, wifi): $extended")
            }
        }
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
                Triple("psram_used_percent (used external PSRAM of the radio)", "%", { s: Sample -> s.psramUsed }),
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
        for (entry in telemetry()) {
            val t = entry.telemetry
            val num = if (entry.owner == 0) own else entry.owner
            t.environment_metrics?.let { put(num, "weather/environment", entry.time, it.toString()) }
            t.air_quality_metrics?.let { put(num, "air quality", entry.time, it.toString()) }
            t.power_metrics?.let { put(num, "power", entry.time, it.toString()) }
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
        if (rows.isEmpty()) return aiString(Res.string.ai_sensor_none)
        // The same sensor saved twice under one name: only its newest reading.
        val fresh = rows.filter { it.ago <= STALE_SENSOR_MIN }
        // Old readings (a sensor silent for days) only when there is nothing fresh.
        val sorted = fresh.ifEmpty { rows }.sortedBy { it.ago }.distinctBy { it.name }.sortedWith(compareByDescending<Row> { it.values.containsKey("temperature") }.thenBy { it.ago })
        val sb = StringBuilder(aiString(Res.string.ai_sensor_title, sorted.size)).append('\n')
        for (r in sorted.take(REPORT_LIMIT)) {
            val parts = ArrayList<String>()
            for ((k, v) in r.values) parts += aiString(REPORT_KEYS.getValue(k), roundTo(v, if (k in TWO_DECIMALS) 2 else 1))
            sb.append("• ${r.name} (${ago(r.ago)}): ${parts.joinToString(", ")}\n")
        }
        suspend fun extreme(key: String, label: StringResource, highest: Boolean) {
            val best = sorted.filter { it.values.containsKey(key) }.let { l -> if (highest) l.maxByOrNull { it.values.getValue(key) } else l.minByOrNull { it.values.getValue(key) } }
            if (best != null) {
                val value = aiString(REPORT_KEYS.getValue(key), roundTo(best.values.getValue(key), 1)).substringAfter(' ')
                sb.append("${aiString(label)}: ${best.name} — $value\n")
            }
        }
        sb.append('\n')
        extreme("temperature", Res.string.ai_sensor_warmest, true)
        extreme("temperature", Res.string.ai_sensor_coldest, false)
        extreme("relative_humidity", Res.string.ai_sensor_most_humid, true)
        extreme("barometric_pressure", Res.string.ai_sensor_highest_pressure, true)
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

    /** Exact signal list written in code, in the language of the app, so the model never copies the numbers wrong. */
    suspend fun signalReport(q: String): String {
        val me = nodeRepository.ourNodeInfo.value
        val now = nowMillis / MILLIS_PER_SECOND
        class Row(val n: org.meshtastic.core.model.Node, val meters: Int?)
        val roleOnly = roleFilter(q)
        val rows = nodeRepository.nodeDBbyNum.value.values.filter { it.num != me?.num && (roleOnly?.invoke(it) ?: true) }.map { Row(it, me?.let { m -> it.distance(m) }) }
        val byDistance = q.hasAny(listOf("odległ", "odleg", "daleko", "dystans", "distance", "far", "najdal"))
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
                closest -> aiString(Res.string.ai_sig_closest, rows.size)
                byDistance -> aiString(Res.string.ai_sig_farthest, rows.size)
                else -> aiString(Res.string.ai_sig_best, withSignal.size, rows.size)
            }
        val none = aiString(Res.string.ai_none_value)
        val sb = StringBuilder(title).append('\n')
        for (r in sorted.take(SIGNAL_SHOWN)) {
            val n = r.n
            val heard = if (n.lastHeard == 0) aiString(Res.string.ai_never) else ago(((now - n.lastHeard) / SECONDS_PER_MINUTE).coerceAtLeast(0))
            val snr = if (n.snr == Float.MAX_VALUE) none else fmt(n.snr) + " dB"
            val rssi = if (n.rssi == Int.MAX_VALUE) none else n.rssi.toString() + " dBm"
            val dist = r.meters?.let { m -> fmt(m / METERS_PER_KM) + " km" } ?: none
            val where = position(n)?.let { aiString(Res.string.ai_sig_position, it) }.orEmpty()
            sb.append(aiString(Res.string.ai_sig_line, n.user.short_name, n.user.long_name, n.user.role.toString(), n.hopsAway.toString(), snr, rssi, dist, where, heard)).append('\n')
        }
        return sb.toString().trim()
    }

    /** Link quality to every node we hear: SNR, RSSI, hops and the distance from our own node. */
    private fun signalRanking(q: String, top: Int = SIGNAL_TOP): String {
        val me = nodeRepository.ourNodeInfo.value
        val now = nowMillis / MILLIS_PER_SECOND
        class Row(val n: org.meshtastic.core.model.Node, val meters: Int?)
        val roleOnly = roleFilter(q)
        val rows =
            nodeRepository.nodeDBbyNum.value.values
                .filter { it.num != me?.num && (roleOnly?.invoke(it) ?: true) }
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
            return "- ${n.user.short_name} (${n.user.long_name}), role ${n.user.role}: hops=${n.hopsAway}; " +
                "SNR=${if (n.snr == Float.MAX_VALUE) "unknown" else fmt(n.snr) + " dB"}; " +
                "RSSI=${if (n.rssi == Int.MAX_VALUE) "unknown" else n.rssi.toString() + " dBm"}; " +
                "distance=${r.meters?.let { m -> fmt(m / METERS_PER_KM) + " km" } ?: "unknown"}; position=${position(n) ?: "unknown"}; heard $ago"
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
        val rows = HashMap<Int, NodeRow>()
        for (entry in telemetry()) {
            val t = entry.telemetry
            val owner = entry.owner
            val row = rows.getOrPut(owner) { NodeRow(owner) }
            t.local_stats?.let { ls ->
                if (entry.time >= row.statsTime) {
                    row.statsTime = entry.time
                    row.rx = ls.num_packets_rx
                    row.tx = ls.num_packets_tx
                    row.bad = ls.num_packets_rx_bad
                    row.dupe = ls.num_rx_dupe
                    row.uptime = ls.uptime_seconds
                    row.heapUsed = if ((ls.heap_total_bytes ?: 0) > 0) ((ls.heap_total_bytes!! - (ls.heap_free_bytes ?: 0)).coerceAtLeast(0)).toFloat() / ls.heap_total_bytes!! * PERCENT else null
                }
            }
            t.device_metrics?.let { dm ->
                if (entry.time >= row.devTime) {
                    row.devTime = entry.time
                    row.battery = dm.battery_level
                    row.chUtil = dm.channel_utilization
                    row.airUtil = dm.air_util_tx
                    if (row.uptime == null) row.uptime = dm.uptime_seconds
                }
            }
        }
        if (rows.isEmpty()) return "NODE RANKING: no telemetry from nodes in the last 3 days."
        data class Key(val label: String, val descending: Boolean, val pick: (NodeRow) -> Float?)
        val rxKey = Key("packets received (rx)", true) { it.rx?.toFloat() }
        val txKey = Key("packets sent (tx)", true) { it.tx?.toFloat() }
        val wantsTx = q.hasAny(listOf("wysł", "wysl", "nadan", " tx", "transmit", "sent"))
        val wantsRx = q.hasAny(listOf("odebr", "otrzym", "przyjęt", "przyjet", " rx", "receiv"))
        // "najmniej / najniższy / najsłabszy" asks for the other end of the list.
        val wantsLow = q.hasAny(listOf("najmniej", "najniż", "najniz", "najsłab", "najslab", "najgorz", "lowest", "least", "fewest", "worst"))
        val wantsHigh = q.hasAny(listOf("najwięc", "najwiec", "najwyż", "najwyz", "najlepsz", "highest", "most ", "best"))
        val keys =
            when {
                wantsTx && wantsRx -> listOf(rxKey, txKey)
                wantsTx -> listOf(txKey)
                q.hasAny(listOf("duplik", "dupe")) -> listOf(Key("duplicate packets received", true) { it.dupe?.toFloat() })
                q.hasAny(listOf("błęd", "bled", "uszkodz", "bad ")) -> listOf(Key("bad packets received", true) { it.bad?.toFloat() })
                q.hasAny(listOf("heap", "pamię", "pamie", " ram")) -> listOf(Key("used heap percent (highest first)", true) { it.heapUsed })
                q.hasAny(listOf("bateri", "battery")) -> listOf(Key("battery percent (lowest first)", false) { it.battery?.toFloat() })
                q.hasAny(listOf("zajęt", "zajet", "kanał", "kanal", "channel", "utiliz")) -> listOf(Key("channel utilization percent", true) { it.chUtil })
                q.hasAny(listOf("uptime", "działa od", "dziala od", "restart")) -> listOf(Key("uptime", true) { it.uptime?.toFloat() })
                else -> listOf(rxKey)
            }.map { k ->
                // Battery is listed lowest first by default; everything else highest first.
                val high = if (k.label.startsWith("battery")) wantsHigh && !wantsLow else !wantsLow
                if (high == k.descending) k else k.copy(descending = high, label = k.label.replace(" (lowest first)", "").replace(" (highest first)", ""))
            }
        fun line(r: NodeRow): String {
            val n = nodeRepository.nodeDBbyNum.value[r.num]
            val name = n?.let { "${it.user.short_name} (${it.user.long_name}), id ${it.user.id}" } ?: "!${r.num.toUInt().toString(HEX)}"
            return "- $name: rx=${r.rx ?: "?"}, tx=${r.tx ?: "?"}, bad=${r.bad ?: "?"}, dupe=${r.dupe ?: "?"}, " +
                "heap_used=${r.heapUsed?.let { fmt(it) } ?: "?"}%, battery=${r.battery ?: "?"}%, ch_util=${r.chUtil?.let { fmt(it) } ?: "?"}%, " +
                "air_tx=${r.airUtil?.let { fmt(it) } ?: "?"}%, uptime=${r.uptime?.let { it / SECONDS_PER_HOUR } ?: "?"} h"
        }
        // With two keys (received AND sent) each list is shorter, so both fit in the small window.
        val limit = if (keys.size > 1) maxOf(top / 2, 3) else top
        return keys.joinToString("\n") { key ->
            val withValue = rows.values.filter { key.pick(it) != null }
            val sorted = if (key.descending) withValue.sortedByDescending { key.pick(it) } else withValue.sortedBy { key.pick(it) }
            buildString {
                appendLine(
                    "NODE RANKING by ${key.label}: ${sorted.size} of ${rows.size} nodes reported. Counters are the node's own totals " +
                        "since it started. Every value in a line belongs to THAT node only. In this order:",
                )
                sorted.firstOrNull()?.let { top1 ->
                    val n = nodeRepository.nodeDBbyNum.value[top1.num]
                    val who = n?.let { "${it.user.short_name} (${it.user.long_name})" } ?: "!${top1.num.toUInt().toString(HEX)}"
                    appendLine("FIRST IN THIS LIST: $who = ${fmt(key.pick(top1) ?: 0f)}")
                }
                sorted.take(limit).forEach { appendLine(line(it)) }
                if (sorted.size > limit + RANK_BOTTOM) {
                    appendLine("... Last ones:")
                    sorted.takeLast(RANK_BOTTOM).forEach { appendLine(line(it)) }
                }
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
        val roles = nodes.groupingBy { it.user.role.toString() }.eachCount().entries.sortedByDescending { it.value }.joinToString { "${it.key}:${it.value}" }
        val favourites = nodes.filter { it.isFavorite }.joinToString { it.user.short_name }
        val mqtt = nodes.count { it.viaMqtt }
        if (polish) {
            val hops =
                nodes.groupingBy { it.hopsAway }.eachCount().toSortedMap().entries.joinToString("; ") {
                    val where = when (it.key) {
                        -1 -> "odległość nieznana"
                        0 -> "bezpośrednio"
                        else -> "${it.key} przeskoków"
                    }
                    "$where – ${it.value} węzłów"
                }
            val hw = nodes.groupingBy { it.user.hw_model.toString() }.eachCount().entries.sortedByDescending { it.value }.take(HW_LIMIT).filter { it.value > 0 }.joinToString("; ") { "${it.key} – ${it.value}" }
            val rl = nodes.groupingBy { it.user.role.toString() }.eachCount().entries.sortedByDescending { it.value }.joinToString("; ") { "${it.key} – ${it.value}" }
            return "PODSUMOWANIE SIECI: znanych węzłów ${nodes.size}, słyszanych w ostatnich 24 h $recent. Węzły według odległości: $hops. " +
                "Słaba bateria (do $LOW_BATTERY%): ${low.ifBlank { "brak" }}. Sprzęt: $hw. Role: $rl. " +
                "Ulubione: ${favourites.ifBlank { "brak" }}. Słyszane przez MQTT: $mqtt."
        }
        return "NETWORK SUMMARY: ${nodes.size} nodes known, $recent heard in the last 24 h. Nodes by hops away (hops:count): $byHops. " +
            "Low battery (<= $LOW_BATTERY%): ${low.ifBlank { "none" }}. Hardware: $hardware. Roles: $roles. Favourites: ${favourites.ifBlank { "none" }}. Heard via MQTT: $mqtt."
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
            val names = radioConfigRepository.channelSetFlow.first().settings.map { it.name }
            for ((key, packet) in contacts) {
                val where =
                    if (key.endsWith("^all")) {
                        key.substringBefore('^').toIntOrNull()?.let { "channel $it (${names.getOrNull(it)?.ifBlank { null } ?: "default"})" } ?: key
                    } else {
                        "private chat"
                    }
                val sender = findNode(packet.from.orEmpty())?.let { it.user.long_name.ifBlank { it.user.short_name } } ?: packet.from
                appendLine("- [$where] $sender: ${packet.text.orEmpty().take(MESSAGE_CHARS)}")
            }
        }
    }

    private class Sample(
        val time: Long,
        val heapUsed: Float?,
        val psramUsed: Float?,
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
            psramUsed = ext?.takeIf { it.memoryPsramTotal > 0 }?.let { (it.memoryPsramTotal - it.memoryPsramFree).coerceAtLeast(0).toFloat() / it.memoryPsramTotal * PERCENT },
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
    suspend fun toolList(sortBy: String, limit: Int, role: String = ""): String {
        val top = limit.coerceIn(1, MAX_TOOL_LIST)
        return when (sortBy.trim().lowercase()) {
            "snr", "rssi", "signal", "range", "hops" -> signalReport("snr $role")
            "distance", "far", "farthest" -> signalReport("odległ $role")
            "closest", "nearest", "near" -> signalReport("najbliż $role")
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

    /**
     * Messages containing [query] (case and Polish accents ignored) in all conversations, or only in [channel] (name or
     * number) or in the direct messages with a node called [channel]; an empty [query] gives the newest ones.
     */
    suspend fun toolMessages(query: String, limit: Int, channel: String = ""): String {
        val top = limit.coerceIn(1, MAX_TOOL_LIST)
        val needle = query.anyToEmpty().fold()
        val names = radioConfigRepository.channelSetFlow.first().settings.map { it.name }
        val wanted = channel.anyToEmpty()
        var keys = packetRepository.getContacts().first().keys.toList()
        if (wanted.isNotEmpty()) {
            val index = wanted.toIntOrNull() ?: numberedChannel(wanted) ?: names.indexOfFirst { it.lowercase() == wanted }.takeIf { it >= 0 }
                ?: names.indexOfFirst { it.lowercase().contains(wanted) }.takeIf { it >= 0 }
            keys =
                if (index != null) {
                    keys.filter { it.endsWith("^all") && it.startsWith("$index^") }
                } else {
                    val id = findNode(channel)?.user?.id.orEmpty()
                    if (id.isBlank()) return "No channel or node found for \"$channel\"."
                    keys.filter { it.endsWith(id) }
                }
        }
        val found = ArrayList<Triple<Long, String, String>>()
        val seen = ArrayList<Pair<Triple<Long, String, String>, String>>()
        val defaultName = aiString(Res.string.ai_scope_default)
        for (key in keys) {
            val label =
                if (key.endsWith("^all")) key.substringBefore('^').toIntOrNull()?.let { names.getOrNull(it)?.ifBlank { defaultName } } ?: key
                else aiString(Res.string.ai_private_chat)
            val list = packetRepository.getMessagesFrom(key, SEARCH_PER_CHAT) { id -> nodeRepository.getNode(id.orEmpty()) }.first()
            for (m in list) {
                val who = if (m.fromLocal) "me" else m.node.user.short_name
                seen += Triple(m.receivedTime, label, "$who: ${m.text.replace(Regex("\\s*\\n\\s*"), " ").take(MESSAGE_CHARS)}") to m.text.lowercase().fold()
            }
        }
        // The word as asked, then without its ending (the Polish "pogodzie" finds "pogoda", "pogody"): the same message
        // text is searched, only the needle gets shorter.
        // "pogodzie" -> "pogod" needs three letters off (d -> dz), so the longer words are also tried shorter by three.
        val shortened = listOfNotNull(needle.takeIf { it.length >= STEM_MIN_SEARCH }?.dropLast(2), needle.takeIf { it.length > STEM_MIN_SEARCH }?.dropLast(3))
        for (n in (listOf(needle) + shortened).distinct()) {
            found += seen.filter { n.isEmpty() || it.second.contains(n) }.map { it.first }
            if (found.isNotEmpty()) break
        }
        if (found.isEmpty()) {
            return if (needle.isNotEmpty()) aiString(Res.string.ai_found_none_for, query.trim()) else aiString(Res.string.ai_found_none)
        }
        val now = nowMillis
        return aiString(Res.string.ai_found_n, found.size) + "\n" +
            found.sortedByDescending { it.first }.take(top).map {
                "• ${it.second}, ${ago(((now - it.first) / MILLIS_PER_MINUTE).coerceAtLeast(0))}, ${it.third}"
            }.joinToString("\n")
    }

    /**
     * The latest messages of one channel in the order they were written, ready to be summarised. [channel] is the
     * channel name (or its number); empty means every channel. Direct messages are not included.
     */
    suspend fun toolChannelMessages(channel: String, limit: Int): String {
        val top = limit.coerceIn(1, MAX_TOOL_LIST)
        val names = radioConfigRepository.channelSetFlow.first().settings.map { it.name }
        val wanted = channel.anyToEmpty()
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

    /** A time window: [since, until) in epoch milliseconds and the text that names it (with [n] when it has a number). */
    private class Win(val since: Long, val until: Long, val label: StringResource, val n: Int = 0)

    private suspend fun Win.title(): String = if (n > 0) aiString(label, n) else aiString(label)

    /**
     * How many messages were received on a channel (the word all = every channel and direct messages) in a time
     * window, counted in code. [hours] 0 means "today" (since local midnight); 1 to 8784 means the last that many hours;
     * -1 means all saved messages.
     */
    suspend fun toolMessageCount(channel: String, hours: Int): String {
        val now = nowMillis
        val w =
            when {
                hours < 0 -> Win(0L, Long.MAX_VALUE, Res.string.ai_win_all)
                hours > 0 -> Win(now - hours.toLong().coerceAtMost(MAX_COUNT_HOURS) * MILLIS_PER_HOUR, Long.MAX_VALUE, Res.string.ai_win_last_n_h, hours)
                else -> Win(todayStart(now), Long.MAX_VALUE, Res.string.ai_win_today)
            }
        return messageCountReport(channel, w)
    }

    /** Start of the local day that contains [now], in epoch milliseconds. */
    private fun todayStart(now: Long): Long {
        val offsetMs = systemTimeZone.offsetAt(kotlin.time.Instant.fromEpochMilliseconds(now)).totalSeconds * MILLIS_PER_SECOND
        return ((now + offsetMs) / MILLIS_PER_DAY) * MILLIS_PER_DAY - offsetMs
    }

    /** A time window written in a question (minutes, hours, days, week, month, year, today, yesterday), or null. */
    private fun timeWindow(q: String): Win? {
        val now = nowMillis
        val today = todayStart(now)
        if (q.contains("wczoraj") || q.contains("yesterday")) return Win(today - MILLIS_PER_DAY, today, Res.string.ai_win_yesterday)
        val unit = Regex("(\\d+)\\s*(minut|min\\b|godz|h\\b|hour|dni|dzie|dob|day|tydz|tygod|week|miesi|month|rok|lat|year)").find(q)
        if (unit != null) {
            val n = unit.groupValues[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
            val (ms, res) =
                when (unit.groupValues[2]) {
                    "minut", "min" -> MILLIS_PER_MINUTE to Res.string.ai_win_last_n_min
                    "godz", "h", "hour" -> MILLIS_PER_HOUR to Res.string.ai_win_last_n_h
                    "dni", "dzie", "dob", "day" -> MILLIS_PER_DAY to Res.string.ai_win_last_n_d
                    "tydz", "tygod", "week" -> 7 * MILLIS_PER_DAY to Res.string.ai_win_last_n_w
                    "miesi", "month" -> 30 * MILLIS_PER_DAY to Res.string.ai_win_last_n_m
                    else -> 365 * MILLIS_PER_DAY to Res.string.ai_win_last_n_y
                }
            return Win(now - n * ms, Long.MAX_VALUE, res, n)
        }
        return when {
            q.hasAny(TODAY_WORDS) -> Win(today, Long.MAX_VALUE, Res.string.ai_win_today)
            q.contains("godzin") || q.contains("hour") -> Win(now - MILLIS_PER_HOUR, Long.MAX_VALUE, Res.string.ai_win_last_hour)
            q.contains("tydzie") || q.contains("tygodn") || q.contains("week") -> Win(now - 7 * MILLIS_PER_DAY, Long.MAX_VALUE, Res.string.ai_win_last_week)
            q.contains("miesi") || q.contains("month") -> Win(now - 30 * MILLIS_PER_DAY, Long.MAX_VALUE, Res.string.ai_win_last_month)
            q.contains("rok") || q.contains("year") -> Win(now - 365 * MILLIS_PER_DAY, Long.MAX_VALUE, Res.string.ai_win_last_year)
            q.contains("dobę") || q.contains("doba") || q.contains("dobe") -> Win(now - MILLIS_PER_DAY, Long.MAX_VALUE, Res.string.ai_win_last_day)
            else -> null
        }
    }

    private suspend fun messageCountReport(channel: String, window: Win): String {
        val names = radioConfigRepository.channelSetFlow.first().settings.map { it.name }
        val wanted = channel.anyToEmpty()
        val index =
            if (wanted.isEmpty()) null
            else wanted.toIntOrNull() ?: numberedChannel(wanted) ?: names.indexOfFirst { it.lowercase() == wanted }.takeIf { it >= 0 }
                ?: names.indexOfFirst { it.lowercase().contains(wanted) }.takeIf { it >= 0 }
        if (wanted.isNotEmpty() && index == null) {
            return aiString(Res.string.ai_no_channel, channel, names.mapIndexed { i, n -> "$i = ${n.ifBlank { "-" }}" }.joinToString())
        }
        val keys = packetRepository.getContacts().first().keys.filter { key ->
            if (index == null) true else key.endsWith("^all") && key.startsWith("$index^")
        }
        val ownName = nodeRepository.ourNodeInfo.value?.user?.let { it.long_name.ifBlank { it.short_name } }.orEmpty()
        var total = 0
        var mine = 0
        val perSender = HashMap<String, Int>()
        for (key in keys) {
            val list = packetRepository.getMessagesFrom(key, COUNT_PER_CHAT) { id -> nodeRepository.getNode(id.orEmpty()) }.first()
            for (m in list) {
                if (m.receivedTime < window.since || m.receivedTime >= window.until) continue
                total++
                val who = if (m.fromLocal) ownName.ifBlank { "me" } else m.node.user.long_name.ifBlank { m.node.user.short_name }
                if (m.fromLocal) mine++
                perSender.merge(who, 1, Int::plus)
            }
        }
        val scope =
            if (index == null) aiString(Res.string.ai_scope_all)
            else aiString(Res.string.ai_scope_channel, names.getOrNull(index)?.ifBlank { null } ?: aiString(Res.string.ai_scope_default), index)
        val sb = StringBuilder(aiString(Res.string.ai_cnt_line, window.title(), scope, total, mine))
        if (perSender.isNotEmpty()) {
            sb.append('\n').append(aiString(Res.string.ai_cnt_top))
            perSender.entries.sortedByDescending { it.value }.take(TOP_SENDERS).forEach { sb.append("\n• ${it.key} — ${it.value}") }
        }
        return sb.toString()
    }

    /**
     * Nodes that match all given filters, counted and listed in code. [activeHours] > 0 keeps nodes heard in that many
     * hours; [hardware] is part of the hardware name; [onlyFavorites] 1 keeps favourites; [maxBattery] in 1..100 keeps
     * nodes with that battery level or less; [withPosition] 1 keeps nodes that have a position.
     */
    suspend fun toolFindNodes(activeHours: Int, hardware: String, onlyFavorites: Int, maxBattery: Int, withPosition: Int, limit: Int): String {
        val top = limit.coerceIn(1, MAX_TOOL_LIST)
        val nowS = nowMillis / MILLIS_PER_SECOND
        val hw = hardware.anyToEmpty()
        val all = nodeRepository.nodeDBbyNum.value.values
        val found =
            all.filter { n ->
                (activeHours <= 0 || (n.lastHeard != 0 && nowS - n.lastHeard <= activeHours * SECONDS_PER_MINUTE * 60L)) &&
                    (hw.isEmpty() || n.user.hw_model.toString().lowercase().contains(hw)) &&
                    (onlyFavorites != 1 || n.isFavorite) &&
                    (maxBattery !in 1..100 || (n.deviceMetrics.battery_level ?: Int.MAX_VALUE) <= maxBattery) &&
                    (withPosition != 1 || n.validPosition != null)
            }.sortedByDescending { it.lastHeard }
        if (found.isEmpty()) return aiString(Res.string.ai_nodes_none, all.size)
        val sb = StringBuilder(aiString(Res.string.ai_nodes_match, found.size, all.size))
        for (n in found.take(top)) {
            val heard = if (n.lastHeard == 0) aiString(Res.string.ai_never) else ago(((nowS - n.lastHeard) / SECONDS_PER_MINUTE).coerceAtLeast(0))
            val bat =
                n.deviceMetrics.battery_level?.let { if (it > 100) aiString(Res.string.ai_battery_ext) else aiString(Res.string.ai_battery_pct, it) }
                    ?: aiString(Res.string.ai_battery_unknown)
            sb.append("\n• ${n.user.long_name} (${n.user.short_name}), ${n.user.hw_model}, ${n.user.role}, ${aiString(Res.string.ai_heard, heard)}, $bat")
        }
        if (found.size > top) sb.append("\n").append(aiString(Res.string.ai_more, found.size - top))
        return sb.toString()
    }

    /** The latest direct messages with one node (both directions), oldest first. */
    suspend fun toolDirectMessages(node: String, limit: Int): String {
        val n = findNode(node) ?: return "No node found for \"$node\"."
        val top = limit.coerceIn(1, MAX_TOOL_LIST)
        val id = n.user.id
        val keys = packetRepository.getContacts().first().keys.filter { id.isNotBlank() && it.endsWith(id) }
        if (keys.isEmpty()) return "No direct messages with ${n.user.long_name}."
        val all = ArrayList<Pair<Long, String>>()
        for (key in keys) {
            val list = packetRepository.getMessagesFrom(key, top) { nid -> nodeRepository.getNode(nid.orEmpty()) }.first()
            for (m in list) all += m.receivedTime to "${if (m.fromLocal) "me" else n.user.long_name}: ${m.text.take(MESSAGE_CHARS)}"
        }
        if (all.isEmpty()) return "No direct messages with ${n.user.long_name}."
        val now = nowMillis
        return "DIRECT MESSAGES with ${n.user.long_name} (oldest first):\n" +
            all.sortedByDescending { it.first }.take(top).sortedBy { it.first }.joinToString("\n") {
                "- ${((now - it.first) / MILLIS_PER_MINUTE).coerceAtLeast(0)} min ago, ${it.second}"
            }
    }

    private suspend fun ago(minutes: Long): String =
        when {
            minutes < 1 -> aiString(Res.string.ai_ago_now)
            minutes < MINUTES_PER_HOUR -> aiString(Res.string.ai_ago_min, minutes.toInt())
            minutes < MINUTES_PER_DAY -> aiString(Res.string.ai_ago_h, (minutes / MINUTES_PER_HOUR).toInt())
            else -> aiString(Res.string.ai_ago_d, (minutes / MINUTES_PER_DAY).toInt())
        }

    private fun nameOf(num: Int): String = nodeNameFor(num).ifBlank { "!" + num.toUInt().toString(16) }

    /** The newest traceroute results (path there and back, signal of each hop), for all nodes or one [node]. */
    suspend fun toolTraceroutes(node: String, limit: Int): String {
        val top = limit.coerceIn(1, MAX_TOOL_LIST)
        val wanted = node.anyToEmpty()
        val num = if (wanted.isEmpty()) null else (findNode(node)?.num ?: return unknownNode(node))
        val logs = meshLogRepository.getLogsByPortNumSince(PortNum.TRACEROUTE_APP.value, nowMillis - TRACE_WINDOW_MS).first()
        val now = nowMillis
        val there = aiString(Res.string.ai_trace_there)
        val back = aiString(Res.string.ai_trace_back)
        val lines = ArrayList<String>()
        for (log in logs.sortedByDescending { it.received_date }) {
            val rd = log.fromRadio.packet?.fullRouteDiscovery ?: continue
            if (num != null && num !in rd.route && num !in rd.route_back) continue
            fun path(route: List<Int>, snr: List<Int>) =
                route.mapIndexed { i, n ->
                    val q = snr.getOrNull(i - 1)?.takeIf { it != Int.MIN_VALUE }
                    nameOf(n) + (if (q != null && i > 0) " (${fmt(q / 4f)} dB)" else "")
                }.joinToString(" → ")
            val hops = aiString(Res.string.ai_trace_hops, (rd.route.size - 2).coerceAtLeast(0))
            lines += buildString {
                append("• ${ago(((now - log.received_date) / MILLIS_PER_MINUTE).coerceAtLeast(0))}, $hops\n   $there: ${path(rd.route, rd.snr_towards)}")
                if (rd.route_back.isNotEmpty()) append("\n   $back: ${path(rd.route_back, rd.snr_back)}")
            }
            if (lines.size >= top) break
        }
        if (lines.isEmpty()) return aiString(if (num != null) Res.string.ai_trace_none_node else Res.string.ai_trace_none)
        return aiString(Res.string.ai_trace_title) + "\n" + lines.joinToString("\n")
    }

    /** Who each node reported hearing directly (neighbour info), newest report per node; one [node] or all. */
    suspend fun toolNeighbors(node: String, limit: Int): String {
        val top = limit.coerceIn(1, MAX_TOOL_LIST)
        val wanted = node.anyToEmpty()
        val num = if (wanted.isEmpty()) null else (findNode(node)?.num ?: return unknownNode(node))
        val logs = meshLogRepository.getLogsByPortNumSince(PortNum.NEIGHBORINFO_APP.value, nowMillis - TRACE_WINDOW_MS).first()
        val now = nowMillis
        val newest = HashMap<Int, Pair<Long, org.meshtastic.proto.NeighborInfo>>()
        for (log in logs) {
            val payload = log.fromRadio.packet?.decoded?.payload ?: continue
            val info = runCatching { org.meshtastic.proto.NeighborInfo.ADAPTER.decode(payload) }.getOrNull() ?: continue
            val owner = info.node_id.takeIf { it != 0 } ?: log.fromNum
            if (num != null && owner != num && info.neighbors.none { it.node_id == num }) continue
            if ((newest[owner]?.first ?: -1L) < log.received_date) newest[owner] = log.received_date to info
        }
        if (newest.isEmpty()) return aiString(if (num != null) Res.string.ai_neigh_none_node else Res.string.ai_neigh_none)
        val nobody = aiString(Res.string.ai_neigh_nobody)
        return aiString(Res.string.ai_neigh_title) + "\n" +
            newest.entries.sortedByDescending { it.value.first }.take(top).map { (owner, v) ->
                val whenText = ago(((now - v.first) / MILLIS_PER_MINUTE).coerceAtLeast(0))
                "• ${aiString(Res.string.ai_neigh_hears, nameOf(owner), whenText)} " +
                    v.second.neighbors.joinToString { "${nameOf(it.node_id)} (${fmt(it.snr)} dB)" }.ifBlank { nobody }
            }.joinToString("\n")
    }

    /** Connection to the radio, unread messages per conversation and the state of the newest own messages. */
    suspend fun toolAppStatus(): String {
        val names = radioConfigRepository.channelSetFlow.first().settings.map { it.name }
        val sb = StringBuilder(aiString(Res.string.ai_status_title)).append('\n')
        val conn =
            when (connection?.connectionState?.value) {
                org.meshtastic.core.model.ConnectionState.Connected -> Res.string.ai_conn_connected
                org.meshtastic.core.model.ConnectionState.Disconnected -> Res.string.ai_conn_disconnected
                org.meshtastic.core.model.ConnectionState.Connecting -> Res.string.ai_conn_connecting
                org.meshtastic.core.model.ConnectionState.DeviceSleep -> Res.string.ai_conn_sleep
                else -> Res.string.ai_conn_unknown
            }
        sb.append(aiString(Res.string.ai_status_conn, aiString(conn))).append('\n')
        val unread = packetRepository.getUnreadCounts().filterValues { it > 0 }
        if (unread.isEmpty()) {
            sb.append(aiString(Res.string.ai_status_unread_none)).append('\n')
        } else {
            sb.append(aiString(Res.string.ai_status_unread, unread.values.sum())).append('\n')
            unread.entries.sortedByDescending { it.value }.take(UNREAD_SHOWN).forEach { (key, n) ->
                val label =
                    if (key.endsWith("^all")) key.substringBefore('^').toIntOrNull()?.let { "${names.getOrNull(it).orEmpty().ifBlank { "#$it" }}" } ?: key
                    else findNode("!" + key.substringAfter('!', ""))?.user?.long_name ?: aiString(Res.string.ai_private_chat)
                sb.append("• $label: $n\n")
            }
        }
        val own = ArrayList<Triple<Long, org.meshtastic.core.model.MessageStatus?, String>>()
        for (key in packetRepository.getContacts().first().keys) {
            val list = packetRepository.getMessagesFrom(key, OWN_STATUS_PER_CHAT) { id -> nodeRepository.getNode(id.orEmpty()) }.first()
            for (m in list) if (m.fromLocal) own += Triple(m.receivedTime, m.status, m.text.take(MESSAGE_CHARS))
        }
        if (own.isNotEmpty()) {
            val now = nowMillis
            sb.append(aiString(Res.string.ai_status_own_title)).append('\n')
            own.sortedByDescending { it.first }.take(OWN_STATUS_SHOWN).forEach {
                val state =
                    when (it.second) {
                        org.meshtastic.core.model.MessageStatus.DELIVERED, org.meshtastic.core.model.MessageStatus.SFPP_CONFIRMED -> Res.string.ai_msg_delivered
                        org.meshtastic.core.model.MessageStatus.ENROUTE, org.meshtastic.core.model.MessageStatus.SFPP_ROUTING -> Res.string.ai_msg_enroute
                        org.meshtastic.core.model.MessageStatus.QUEUED -> Res.string.ai_msg_queued
                        org.meshtastic.core.model.MessageStatus.ERROR -> Res.string.ai_msg_error
                        else -> Res.string.ai_msg_other
                    }
                sb.append("• ${ago(((now - it.first) / MILLIS_PER_MINUTE).coerceAtLeast(0))}: ${aiString(state)} — ${it.third}\n")
            }
        }
        return sb.toString().trim()
    }

    /** The waypoints (map pins) the app has received or created, newest first. */
    suspend fun toolWaypoints(limit: Int): String {
        val top = limit.coerceIn(1, MAX_TOOL_LIST)
        val list = packetRepository.getWaypoints().first().sortedByDescending { it.time }
        if (list.isEmpty()) return aiString(Res.string.ai_wp_none)
        val now = nowMillis
        val noName = aiString(Res.string.ai_wp_noname)
        return aiString(Res.string.ai_wp_title, list.size) + "\n" +
            list.take(top).map { p ->
                val w = p.waypoint
                val sender = findNode(p.from.orEmpty())?.let { it.user.long_name.ifBlank { it.user.short_name } } ?: p.from.orEmpty()
                val lat = (w?.latitude_i ?: 0) / WAYPOINT_SCALE
                val lon = (w?.longitude_i ?: 0) / WAYPOINT_SCALE
                "• ${w?.name.orEmpty().ifBlank { noName }}${w?.description.orEmpty().take(MESSAGE_CHARS).let { d -> if (d.isBlank()) "" else " — $d" }} " +
                    "(${fmt(lat.toFloat())}, ${fmt(lon.toFloat())}), ${aiString(Res.string.ai_wp_from, sender)}, ${ago(((now - p.time) / MILLIS_PER_MINUTE).coerceAtLeast(0))}"
            }.joinToString("\n")
    }

    // ---- Entry points for the question router: the model only says WHAT is asked, the code finds and counts ----

    /** A real node name for the test questions: the most recently heard other node with a position. */
    fun sampleNodeName(): String {
        val me = nodeRepository.ourNodeInfo.value?.num
        val others = nodeRepository.nodeDBbyNum.value.values.filter { it.num != me && it.user.long_name.isNotBlank() }
        return (others.filter { it.validPosition != null }.maxByOrNull { it.lastHeard } ?: others.maxByOrNull { it.lastHeard })
            ?.user?.long_name.orEmpty().ifBlank { "?" }
    }

    /** A real channel name for the test questions. */
    suspend fun sampleChannelName(): String = channelNames().firstOrNull { it.isNotBlank() } ?: "0"

    /** The names of the channels in their order (an empty name is the default channel). */
    suspend fun channelNames(): List<String> = radioConfigRepository.channelSetFlow.first().settings.map { it.name }

    /**
     * The node the router named, found by its name, a part of it or its word stem ("Łysicy" finds "Łysica"); words like
     * "me" mean the own node. When that fails, the node the user's question itself points at. Null when nothing fits.
     */
    fun resolveNodeNum(name: String, question: String): Int? {
        val wanted = name.anyToEmpty()
        val own = nodeRepository.ourNodeInfo.value
        if (wanted in SELF_WORDS) return own?.num
        if (wanted.length >= MIN_NAME - 1) {
            findNode(wanted)?.let { return it.num }
            mentionedNodes(" $wanted ").firstOrNull()?.let { return it.num }
        }
        val inQuestion = mentionedNodes(question.lowercase())
        return (inQuestion.firstOrNull { it.num != own?.num } ?: inQuestion.firstOrNull())?.num
    }

    /** The nearest (or farthest) node with a known position, of one [role] or any, with its distance in metres. */
    fun nearestNode(role: String, farthest: Boolean): Pair<Int, Int>? {
        val me = nodeRepository.ourNodeInfo.value ?: return null
        val roleOnly = roleFilter(role.lowercase())
        val rows =
            nodeRepository.nodeDBbyNum.value.values
                .filter { it.num != me.num && (roleOnly?.invoke(it) ?: true) }
                .mapNotNull { n -> n.distance(me)?.let { n.num to it } }
        return if (farthest) rows.maxByOrNull { it.second } else rows.minByOrNull { it.second }
    }

    /**
     * The nearest or farthest node as one short sentence, and the next few after it with only their distance: a long
     * table with every value of every node was too much for such a simple question.
     */
    private suspend fun distanceAnswer(role: String, farthest: Boolean): String? {
        val me = nodeRepository.ourNodeInfo.value ?: return null
        val roleOnly = roleFilter(role.lowercase())
        val rows =
            nodeRepository.nodeDBbyNum.value.values
                .filter { it.num != me.num && (roleOnly?.invoke(it) ?: true) }
                .mapNotNull { n -> n.distance(me)?.let { n to it } }
                // The same radio saved twice (new number after a reflash): the same names at the same place, shown once.
                .sortedByDescending { it.first.lastHeard }
                .distinctBy { (n, m) -> Triple(n.user.long_name, n.user.short_name, m / SAME_PLACE_M) }
                .let { list -> if (farthest) list.sortedByDescending { it.second } else list.sortedBy { it.second } }
        val (first, meters) = rows.firstOrNull() ?: return null
        val nowS = nowMillis / MILLIS_PER_SECOND
        val heard = if (first.lastHeard == 0) aiString(Res.string.ai_never) else ago(((nowS - first.lastHeard) / SECONDS_PER_MINUTE).coerceAtLeast(0))
        val sb =
            StringBuilder(
                aiString(
                    if (farthest) Res.string.ai_far_one else Res.string.ai_near_one,
                    first.user.long_name.ifBlank { first.user.short_name }, first.user.short_name, kilometres(meters), heard,
                ),
            )
        val next = rows.drop(1).take(NEXT_SHOWN)
        if (next.isNotEmpty()) {
            sb.append("\n\n").append(aiString(Res.string.ai_dist_next, "").trim())
            for ((n, m) in next) {
                sb.append("\n• ").append(n.user.long_name.ifBlank { n.user.short_name })
                if (n.user.short_name.isNotBlank() && n.user.short_name != n.user.long_name) sb.append(" (").append(n.user.short_name).append(")")
                sb.append(" — ").append(kilometres(m)).append(" km")
            }
        }
        return sb.toString()
    }

    /** A distance in metres written in kilometres with one decimal. */
    fun kilometres(meters: Int): String = fmt(meters / METERS_PER_KM)

    /**
     * The signal of one node, worked out in code: its last packet and, from the saved packets of the last days that
     * came straight from it (not through other nodes), the average, lowest and highest SNR and the average RSSI.
     */
    suspend fun nodeSignal(num: Int): String {
        val n = nodeRepository.nodeDBbyNum.value[num] ?: return aiString(Res.string.ai_unknown_node, num.toString())
        val none = aiString(Res.string.ai_none_value)
        val nowS = nowMillis / MILLIS_PER_SECOND
        val heard = if (n.lastHeard == 0) aiString(Res.string.ai_never) else ago(((nowS - n.lastHeard) / SECONDS_PER_MINUTE).coerceAtLeast(0))
        val snr = if (n.snr == Float.MAX_VALUE) none else fmt(n.snr) + " dB"
        val rssi = if (n.rssi == Int.MAX_VALUE) none else n.rssi.toString() + " dBm"
        val sb = StringBuilder(aiString(Res.string.ai_snr_title, n.user.long_name.ifBlank { n.user.short_name }, n.user.short_name))
        sb.append("\n• ").append(aiString(Res.string.ai_snr_last, snr, rssi, n.hopsAway.toString(), heard))
        val days = (SIGNAL_WINDOW_MS / MILLIS_PER_DAY).toInt()
        val direct =
            safeList {
                meshLogRepository.getLogsFromNodeSince(num, nowMillis - SIGNAL_WINDOW_MS).first().mapNotNull { log ->
                    val p = log.fromRadio.packet ?: return@mapNotNull null
                    val snr: Float = p.rx_snr ?: 0f
                    val rssi: Int = p.rx_rssi ?: 0
                    if (p.hop_start == 0 || p.hop_start != p.hop_limit || snr == 0f) null else snr to rssi
                }
            }
        if (direct.isEmpty()) {
            sb.append("\n• ").append(aiString(Res.string.ai_snr_no_direct, days))
        } else {
            val snrs: List<Float> = direct.map { it.first }
            val rssis: List<Int> = direct.map { it.second }.filter { it != 0 }
            sb.append("\n• ").append(
                aiString(
                    Res.string.ai_snr_avg, days, fmt(snrs.average().toFloat()), fmt(snrs.min()), fmt(snrs.max()),
                    if (rssis.isEmpty()) none else rssis.average().roundToInt().toString(), direct.size,
                ),
            )
        }
        return sb.toString()
    }

    private suspend fun <T> safeList(block: suspend () -> List<T>): List<T> = try {
        block()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.w(tag = TAG, throwable = e) { "Assistant list unavailable" }
        emptyList()
    }

    /**
     * The statistics of the user's own radio, written in code with plain labels (the model mixed up the raw field
     * names and digits): uptime, channel use, packets, relaying, nodes online, memory, noise, processor and flash.
     */
    suspend fun radioStats(): String {
        val ls = nodeRepository.localStats.value
        val ext = nodeRepository.localStatsExtended.value
        val uptime = (ls.uptime_seconds ?: 0).toLong()
        val rx = (ls.num_packets_rx ?: 0)
        val tx = (ls.num_packets_tx ?: 0)
        if (uptime == 0L && rx == 0 && tx == 0) return aiString(Res.string.ai_radio_none)
        val me = nodeRepository.ourNodeInfo.value?.user
        val name = me?.let { it.long_name.ifBlank { it.short_name } }.orEmpty()
        val sb = StringBuilder(aiString(Res.string.ai_radio_title, name).trim())
        fun line(text: String) = sb.append("\n• ").append(text)
        line(aiString(Res.string.ai_radio_uptime, duration(uptime)))
        line(aiString(Res.string.ai_radio_util, fmt(ls.channel_utilization ?: 0f), fmt(ls.air_util_tx ?: 0f)))
        line(aiString(Res.string.ai_radio_packets, tx.toString(), rx.toString(), (ls.num_packets_rx_bad ?: 0).toString(), (ls.num_rx_dupe ?: 0).toString()))
        line(aiString(Res.string.ai_radio_relay, (ls.num_tx_relay ?: 0).toString(), (ls.num_tx_relay_canceled ?: 0).toString(), (ls.num_tx_dropped ?: 0).toString()))
        line(aiString(Res.string.ai_radio_nodes, (ls.num_online_nodes ?: 0).toString(), (ls.num_total_nodes ?: 0).toString()))
        val heapTotal = ls.heap_total_bytes ?: 0
        val heapFree = ls.heap_free_bytes ?: 0
        if (heapTotal > 0) {
            val used = (heapTotal - heapFree).coerceAtLeast(0)
            line(aiString(Res.string.ai_radio_heap, fmt(used.toFloat() / heapTotal * PERCENT), (used / BYTES_PER_KB).toString(), (heapTotal / BYTES_PER_KB).toString()))
        }
        val noise = ls.noise_floor ?: 0
        if (noise != 0) line(aiString(Res.string.ai_radio_noise, noise.toString()))
        if (ext.flashTotalBytes > 0 || ext.cpuUsagePercent > 0) {
            val flash = if (ext.flashTotalBytes > 0) fmt(ext.flashUsedBytes.toFloat() / ext.flashTotalBytes * PERCENT) else aiString(Res.string.ai_none_value)
            line(aiString(Res.string.ai_radio_ext, ext.cpuUsagePercent.toString(), flash))
        }
        return sb.toString()
    }

    /** Who wrote to the user privately lately: the newest message from each person, newest first, in code. */
    suspend fun recentToMe(limit: Int): String {
        val keys = packetRepository.getContacts().first().keys.filter { !it.endsWith("^all") }
        val found = ArrayList<Triple<Long, String, String>>()
        for (key in keys) {
            val list = packetRepository.getMessagesFrom(key, RECENT_PER_CHAT) { id -> nodeRepository.getNode(id.orEmpty()) }.first()
            val newest = list.filter { !it.fromLocal }.maxByOrNull { it.receivedTime } ?: continue
            val who = newest.node.user.long_name.ifBlank { newest.node.user.short_name }
            found += Triple(newest.receivedTime, who, newest.text.take(MESSAGE_CHARS))
        }
        if (found.isEmpty()) return aiString(Res.string.ai_dm_none)
        val now = nowMillis
        val sb = StringBuilder(aiString(Res.string.ai_dm_title))
        for ((time, who, text) in found.sortedByDescending { it.first }.take(limit)) {
            sb.append("\n• ").append(who).append(", ").append(ago(((now - time) / MILLIS_PER_MINUTE).coerceAtLeast(0))).append(": ").append(text)
        }
        return sb.toString()
    }

    /** The battery of one node (the own one when [num] is null), exactly as the node reported it. */
    suspend fun nodeBattery(num: Int?): String {
        val own = nodeRepository.ourNodeInfo.value
        val n = (num?.let { nodeRepository.nodeDBbyNum.value[it] } ?: own) ?: return aiString(Res.string.ai_radio_none)
        // The own node's newest values live in its own record, which the app refreshes first.
        val node = if (own != null && n.num == own.num) own else n
        val name = node.user.long_name.ifBlank { node.user.short_name }
        val level = node.deviceMetrics.battery_level ?: 0
        val voltage = node.deviceMetrics.voltage ?: 0f
        if (level <= 0 && voltage <= 0f) return aiString(Res.string.ai_bat_unknown, name)
        val state = if (level > 100) aiString(Res.string.ai_battery_ext) else aiString(Res.string.ai_battery_pct, level)
        val nowS = nowMillis / MILLIS_PER_SECOND
        val heard = if (node.lastHeard == 0) aiString(Res.string.ai_never) else ago(((nowS - node.lastHeard) / SECONDS_PER_MINUTE).coerceAtLeast(0))
        return if (voltage > 0f) {
            aiString(Res.string.ai_bat_line, name, state, roundTo(voltage.toDouble(), 2), heard)
        } else {
            aiString(Res.string.ai_bat_line_no_voltage, name, state, heard)
        }
    }

    /**
     * A summary of the whole network written in code (a small model garbled the numbers when it retold them): the
     * counts, how far the nodes are in hops, their roles and who is low on battery.
     */
    suspend fun networkOverview(): String {
        val nodes = nodeRepository.nodeDBbyNum.value.values
        val sb = StringBuilder(networkCount())
        if (nodes.isEmpty()) return sb.toString()
        val direct = aiString(Res.string.ai_hops_direct)
        val hops =
            nodes.filter { it.hopsAway >= 0 }.groupingBy { it.hopsAway }.eachCount().toSortedMap().entries
                .joinToString(", ") { (h, c) -> "${if (h == 0) direct else h.toString()}: $c" }
        val roles = nodes.groupingBy { it.user.role.toString() }.eachCount().entries.sortedByDescending { it.value }.joinToString(", ") { "${it.key}: ${it.value}" }
        val low = nodes.filter { (it.deviceMetrics.battery_level ?: 100) in 1..LOW_BATTERY }.joinToString(", ") { it.user.long_name.ifBlank { it.user.short_name } }
        if (hops.isNotBlank()) sb.append("\n• ").append(aiString(Res.string.ai_net_hops, hops))
        sb.append("\n• ").append(aiString(Res.string.ai_net_roles, roles))
        sb.append("\n• ").append(aiString(Res.string.ai_net_low_battery, LOW_BATTERY, low.ifBlank { aiString(Res.string.ai_net_none) }))
        return sb.toString()
    }

    /** The radio's preset and transmit power, for calculations whose question does not give them. */
    suspend fun calcDefaults(): AssistantCalculators.Defaults {
        val lora = radioConfigRepository.localConfigFlow.first().lora ?: return AssistantCalculators.Defaults()
        val preset = if (lora.use_preset) ChannelOption.from(lora.modem_preset) else null
        return AssistantCalculators.Defaults(preset, lora.tx_power)
    }

    /**
     * "My range is weak, what should I check": the user's own radio checked in code — region, preset, power, hop limit,
     * transmitting on, the nodes heard directly and their best signal against the preset's limit, noise and channel use —
     * then the usual things to check by hand.
     */
    suspend fun rangeCheck(): String {
        fun s(pl: String, en: String) = if (polish) pl else en
        val lora = radioConfigRepository.localConfigFlow.first().lora
        val ls = nodeRepository.localStats.value
        val sb = StringBuilder(s("Sprawdziłem Twoje radio:", "I checked your radio:"))
        fun line(text: String) = sb.append("\n• ").append(text)
        if (lora == null) {
            line(s("brak ustawień LoRa — połącz się z radiem.", "no LoRa settings — connect to the radio."))
        } else {
            val option = if (lora.use_preset) ChannelOption.from(lora.modem_preset) else null
            val region = lora.region.name
            val presetName = option?.name ?: s("własne ustawienia", "custom settings")
            val ours = region == "EU_N_868" && option == ChannelOption.NARROW_FAST
            line(
                s("region $region, preset $presetName", "region $region, preset $presetName") +
                    if (ours) s(" — tak jak w naszej sieci", " — the same as our network")
                    else s(" — inne niż w naszej sieci (Narrow Fast, EU_N_868): inne radia Cię nie usłyszą", " — not what our network uses (Narrow Fast, EU_N_868): other radios will not hear you"),
            )
            if (!lora.tx_enabled) line(s("nadawanie jest WYŁĄCZONE — radio tylko słucha", "transmitting is OFF — the radio only listens"))
            line(
                if (lora.tx_power == 0) s("moc nadawania: maksymalna dozwolona w regionie", "transmit power: the maximum the region allows")
                else s("moc nadawania: ${lora.tx_power} dBm", "transmit power: ${lora.tx_power} dBm") +
                    if (lora.tx_power in 1..16) s(" — dość mało, możesz ją zwiększyć", " — fairly low, you can raise it") else "",
            )
            line(s("limit przeskoków: ${lora.hop_limit}", "hop limit: ${lora.hop_limit}") + if (lora.hop_limit in 1..2) s(" — mało, wiadomości nie dojdą daleko", " — low, messages will not travel far") else "")
            val nowS = nowMillis / MILLIS_PER_SECOND
            val me = nodeRepository.ourNodeInfo.value?.num
            val direct =
                nodeRepository.nodeDBbyNum.value.values.filter {
                    it.num != me && it.hopsAway == 0 && it.lastHeard != 0 && nowS - it.lastHeard < ONE_DAY_S
                }
            val limit = option?.let { -7.5f - 2.5f * (it.spreadingFactor - 7) }
            val best = direct.mapNotNull { n -> n.snr.takeIf { it != Float.MAX_VALUE }?.let { n to it } }.maxByOrNull { it.second }
            if (direct.isEmpty()) {
                line(s("przez ostatnią dobę radio nie słyszało bezpośrednio żadnego węzła — antena, położenie albo ustawienia", "in the last day the radio heard no node directly — antenna, location or settings"))
            } else {
                line(s("węzły słyszane bezpośrednio przez ostatnią dobę: ${direct.size}", "nodes heard directly in the last day: ${direct.size}"))
                if (best != null) {
                    val name = best.first.user.long_name.ifBlank { best.first.user.short_name }
                    val extra =
                        limit?.let { l ->
                            val margin = best.second - l
                            s(" (zapas ${fmt(margin)} dB nad granicą odbioru ${fmt(l)} dB)", " (${fmt(margin)} dB above the reception limit of ${fmt(l)} dB)")
                        }.orEmpty()
                    line(s("najlepszy sygnał: $name, SNR ${fmt(best.second)} dB", "best signal: $name, SNR ${fmt(best.second)} dB") + extra)
                }
            }
            val noise = ls.noise_floor ?: 0
            if (noise != 0) {
                line(s("szum: $noise dBm", "noise: $noise dBm") + if (noise > -100) s(" — wysoki, coś w pobliżu zakłóca (zasilacz, komputer, inne radio)", " — high, something nearby interferes (power supply, computer, another radio)") else s(" — w normie", " — normal"))
            }
            val util = ls.channel_utilization ?: 0f
            if (util > 0f) line(s("zajętość kanału: ${fmt(util)} %", "channel use: ${fmt(util)} %") + if (util > 25f) s(" — kanał jest zatłoczony", " — the channel is crowded") else "")
        }
        sb.append("\n\n").append(
            s(
                "Sprawdź też sam: antena na właściwe pasmo (868 MHz), dobrze dokręcona, pionowo, jak najwyżej i z dala od metalu; krótki i dobry kabel; wyżej położone radio słyszy dużo dalej. Konkretne łącze do węzła sprawdzisz w planerze łącza.",
                "Also check yourself: an antenna for the right band (868 MHz), screwed on firmly, vertical, as high as possible and away from metal; a short, good cable; a radio placed higher hears much further. Check a link to a node in the link planner.",
            ),
        )
        return sb.toString()
    }

    /** How many nodes are known, heard lately and heard directly: a short count instead of a long list. */
    suspend fun networkCount(): String {
        val all = nodeRepository.nodeDBbyNum.value.values
        val nowS = nowMillis / MILLIS_PER_SECOND
        val day = all.count { it.lastHeard != 0 && nowS - it.lastHeard < ONE_DAY_S }
        val hour = all.count { it.lastHeard != 0 && nowS - it.lastHeard < SECONDS_PER_HOUR }
        val direct = all.count { it.hopsAway == 0 }
        return aiString(Res.string.ai_net_count, all.size, day, hour, direct)
    }

    /** The list of nodes by distance ([order] nearest / farthest) or by signal (anything else), for one role or all. */
    suspend fun signalListFor(role: String, order: String): String {
        if (order == "nearest" || order == "farthest") distanceAnswer(role, order == "farthest")?.let { return it }
        val words =
            when (order) {
                "farthest" -> "najdal"
                "nearest" -> "najbliż"
                else -> "snr"
            }
        return signalReport("$words ${role.anyToEmpty()}")
    }

    /** A time window from the router's [period] (today, yesterday, minutes, hours, days, weeks, months, years, all). */
    private fun periodWindow(period: String, amount: Int): Win? {
        val now = nowMillis
        val today = todayStart(now)
        val n = amount.coerceIn(1, MAX_PERIOD_AMOUNT)
        return when (period.trim().lowercase()) {
            "today" -> Win(today, Long.MAX_VALUE, Res.string.ai_win_today)
            "yesterday" -> Win(today - MILLIS_PER_DAY, today, Res.string.ai_win_yesterday)
            "minutes" -> Win(now - n * MILLIS_PER_MINUTE, Long.MAX_VALUE, Res.string.ai_win_last_n_min, n)
            "hours" ->
                if (n == 1) Win(now - MILLIS_PER_HOUR, Long.MAX_VALUE, Res.string.ai_win_last_hour)
                else Win(now - n * MILLIS_PER_HOUR, Long.MAX_VALUE, Res.string.ai_win_last_n_h, n)
            "days" ->
                if (n == 1) Win(now - MILLIS_PER_DAY, Long.MAX_VALUE, Res.string.ai_win_last_day)
                else Win(now - n * MILLIS_PER_DAY, Long.MAX_VALUE, Res.string.ai_win_last_n_d, n)
            "weeks" ->
                if (n == 1) Win(now - 7 * MILLIS_PER_DAY, Long.MAX_VALUE, Res.string.ai_win_last_week)
                else Win(now - n * 7 * MILLIS_PER_DAY, Long.MAX_VALUE, Res.string.ai_win_last_n_w, n)
            "months" ->
                if (n == 1) Win(now - 30 * MILLIS_PER_DAY, Long.MAX_VALUE, Res.string.ai_win_last_month)
                else Win(now - n * 30 * MILLIS_PER_DAY, Long.MAX_VALUE, Res.string.ai_win_last_n_m, n)
            "years" ->
                if (n == 1) Win(now - 365 * MILLIS_PER_DAY, Long.MAX_VALUE, Res.string.ai_win_last_year)
                else Win(now - n * 365 * MILLIS_PER_DAY, Long.MAX_VALUE, Res.string.ai_win_last_n_y, n)
            "all" -> Win(0L, Long.MAX_VALUE, Res.string.ai_win_all)
            else -> null
        }
    }

    /** Message counts and who wrote most, for one channel or everything, in the period the router named. */
    suspend fun countMessagesFor(channel: String, period: String, amount: Int): String =
        messageCountReport(channel, periodWindow(period, amount) ?: Win(0L, Long.MAX_VALUE, Res.string.ai_win_all))

    /** Nodes heard in a period, nodes with low battery or the favourites, listed in code. */
    suspend fun findNodesFor(period: String, amount: Int, lowBattery: Boolean, favorites: Boolean): String {
        val window = periodWindow(period, amount)
        if (window != null && !lowBattery && !favorites) {
            val all = nodeRepository.nodeDBbyNum.value.values
            val heard =
                all.filter { it.lastHeard != 0 && it.lastHeard * MILLIS_PER_SECOND >= window.since && it.lastHeard * MILLIS_PER_SECOND < window.until }
                    .sortedByDescending { it.lastHeard }
            val sb = StringBuilder(aiString(Res.string.ai_nodes_heard_title, window.title(), heard.size, all.size))
            if (heard.isNotEmpty()) {
                // A short list, one node per line, newest first; the rest only as a number.
                val nowS = nowMillis / MILLIS_PER_SECOND
                sb.append("\n\n").append(aiString(Res.string.ai_nodes_recent, "").trim())
                for (n in heard.take(NAMES_SHOWN)) {
                    val heardAgo = ago(((nowS - n.lastHeard) / SECONDS_PER_MINUTE).coerceAtLeast(0))
                    sb.append("\n• ").append(n.user.long_name.ifBlank { n.user.short_name }).append(" — ").append(heardAgo)
                }
                if (heard.size > NAMES_SHOWN) sb.append('\n').append(aiString(Res.string.ai_more, heard.size - NAMES_SHOWN))
            }
            return sb.toString()
        }
        val hours = window?.takeIf { it.since > 0 }?.let { ((nowMillis - it.since + MILLIS_PER_HOUR - 1) / MILLIS_PER_HOUR).toInt() } ?: 0
        val list = toolFindNodes(hours, "", if (favorites) 1 else 0, if (lowBattery) LOW_BATTERY else 0, 0, MAX_TOOL_LIST)
        return if (lowBattery) aiString(Res.string.ai_low_battery_title, LOW_BATTERY) + "\n" + list else list
    }

    /**
     * Data for a question the model answers in its own words, chosen by the router's [topic] instead of by words: one
     * node's details and history, a ranking by [metric], the radio settings, recent activity or the overview. Anything
     * else falls back to the parts the question's words point at.
     */
    suspend fun dataFor(topic: String, nodeNum: Int?, metric: String, question: String, maxChars: Int): String {
        val node = nodeNum?.let { num -> nodeRepository.nodeDBbyNum.value[num] ?: nodeRepository.ourNodeInfo.value?.takeIf { it.num == num } }
        val text =
            when (topic) {
                "node" -> if (node != null) safe("node") { nodeDetail(node) } else ""
                "ranking" -> safe("ranking") { toolList(metric.anyToEmpty().ifBlank { "rx" }, RANK_TOP) }
                "settings" -> safe("settings") { settings() }
                "activity" -> safe("activity") { activity(question.lowercase()) }
                "overview" -> safe("overview") { overview() + "\n" + networkSummary() }
                else -> ""
            }
        return text.ifBlank { contextFor(question, maxChars) }.take(maxChars)
    }

    /** The latest messages of a channel, or of the private chat with a node when [target] names a node, for a summary. */
    suspend fun conversationFor(target: String, limit: Int): String {
        val wanted = target.anyToEmpty()
        if (wanted.isNotEmpty()) {
            val names = channelNames()
            val isChannel =
                wanted.toIntOrNull() != null || numberedChannel(wanted) != null ||
                    names.any { it.isNotBlank() && it.lowercase().contains(wanted) }
            if (!isChannel) findNode(wanted)?.let { return toolDirectMessages(it.user.long_name.ifBlank { it.user.short_name }, limit) }
        }
        return toolChannelMessages(wanted, limit)
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
        if (perChannel.isEmpty() && direct == 0) return aiString(Res.string.ai_chan_none)
        val sb = StringBuilder(aiString(Res.string.ai_chan_title)).append('\n')
        val fallback = aiString(Res.string.ai_scope_default)
        for ((i, c) in perChannel.entries.sortedByDescending { it.value }) {
            val label = names.getOrNull(i)?.ifBlank { null } ?: fallback
            sb.append("• ").append(aiString(Res.string.ai_chan_line, i, label, c)).append('\n')
        }
        sb.append("• ").append(aiString(Res.string.ai_chan_private, direct))
        return sb.toString()
    }

    /** The number of the node called [name] (short name, long name, id or "me"), or null. */
    fun nodeNumFor(name: String): Int? = findNode(name)?.num

    /** A readable name of node [num]. */
    fun nodeRoleFor(num: Int): String = nodeRepository.nodeDBbyNum.value[num]?.user?.role?.toString().orEmpty()

    fun nodeNameFor(num: Int): String =
        nodeRepository.nodeDBbyNum.value[num]?.let { it.user.long_name.ifBlank { it.user.short_name } }.orEmpty()

    /**
     * Where a message to [target] goes, as the key of the conversation and its readable name: a channel (by name or
     * number, "all" or empty means the main channel) or a node (direct message). Null when nothing fits.
     */
    suspend fun contactFor(target: String): Pair<String, String>? {
        val wanted = target.trim().lowercase()
        val names = radioConfigRepository.channelSetFlow.first().settings.map { it.name }
        if (wanted.isEmpty() || wanted in MAIN_CHANNEL_WORDS) {
            return "0${org.meshtastic.core.model.NodeAddress.ID_BROADCAST}" to names.firstOrNull().orEmpty().ifBlank { "0" }
        }
        val index =
            numberedChannel(wanted)?.takeIf { it in names.indices }
                ?: names.indexOfFirst { it.isNotBlank() && it.lowercase() == wanted }.takeIf { it >= 0 }
        if (index != null) {
            return "$index${org.meshtastic.core.model.NodeAddress.ID_BROADCAST}" to names[index].ifBlank { "$index" }
        }
        val node = findNode(target) ?: return null
        if (node.user.id.isBlank()) return null
        return "${org.meshtastic.core.model.NodeAddress.PKC_CHANNEL_INDEX}${node.user.id}" to
            node.user.long_name.ifBlank { node.user.short_name }
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
            ?: all.firstOrNull { it.user.long_name.fold().contains(q.fold()) }
            ?: nearNodes(q, loose = false).firstOrNull()
    }

    /** Words of a node's names that a spoken name can be compared with (the whole names and the words inside them). */
    private fun nameWords(n: org.meshtastic.core.model.Node): List<String> =
        (listOf(n.user.long_name, n.user.short_name) + n.user.long_name.split(' ', '_', '-', '/', '.').filter { it.length >= NEAR_MIN_WORD })
            .map { it.fold().trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    /**
     * Nodes whose name is close to [q] although it is not written the same way: another Polish ending ("Karczówce" for
     * "Karczowka", "Starachowicach" for "Starachowice") or a small slip. Closest first; [loose] allows more difference
     * (used only to suggest names).
     */
    private fun nearNodes(q: String, loose: Boolean): List<org.meshtastic.core.model.Node> {
        val asked = (listOf(q) + q.split(' ', '-', '_').filter { it.length >= NEAR_MIN_WORD }).map { it.fold().trim() }.filter { it.length >= NEAR_MIN_WORD }.distinct()
        if (asked.isEmpty()) return emptyList()
        return nodeRepository.nodeDBbyNum.value.values
            .mapNotNull { n ->
                val best = nameWords(n).flatMap { w -> asked.map { a -> nameDistance(a, w, loose) } }.filterNotNull().minOrNull()
                best?.let { n to it }
            }
            .sortedBy { it.second }
            .map { it.first }
    }

    /** How far two names are apart (0 = same), or null when they are too different to be the same name. */
    private fun nameDistance(a: String, b: String, loose: Boolean): Int? {
        val shortest = minOf(a.length, b.length)
        if (shortest < NEAR_MIN_WORD) return null
        // The same beginning with only an ending different: "starachowicach" / "starachowice".
        if (shortest >= NEAR_STEM_MIN && (a.startsWith(b.dropLast(2)) || b.startsWith(a.dropLast(2)))) return 1
        val allowed = if (loose) maxOf(2, shortest / 3) else maxOf(1, shortest / 4)
        val d = editDistance(a, b)
        return d.takeIf { it <= allowed }
    }

    private fun editDistance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            }
            previous = current
        }
        return previous[b.length]
    }

    /** "I do not know node X", with the names it may have been when some are close ("Did you mean: ..."). */
    fun unknownNode(name: String): String {
        val q = name.trim().lowercase()
        val close = if (q.isEmpty()) emptyList() else nearNodes(q, loose = true).take(SUGGESTED_NODES).map { it.user.long_name.ifBlank { it.user.short_name } }.distinct()
        val base = aiString(Res.string.ai_unknown_node, name)
        return if (close.isEmpty()) base else base + "\n" + aiString(Res.string.ai_unknown_node_maybe, close.joinToString(", "))
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

    /** "najbliższy router", "najbliższy czujnik": keeps only the nodes whose role fits the word in the question. */
    private fun roleFilter(q: String): ((org.meshtastic.core.model.Node) -> Boolean)? {
        val roles =
            when {
                q.hasAny(listOf("router", "ruter")) -> listOf("ROUTER")
                q.hasAny(listOf("repeater", "przekaźnik", "przekaznik", "retransmit")) -> listOf("REPEATER", "ROUTER")
                q.hasAny(listOf("czujnik", "sensor")) -> listOf("SENSOR")
                q.hasAny(listOf("tracker")) -> listOf("TRACKER")
                else -> return null
            }
        return { n -> roles.any { n.user.role.toString().uppercase().contains(it) } }
    }

    private fun position(n: org.meshtastic.core.model.Node): String? =
        n.validPosition?.let { "${(kotlin.math.round(n.latitude * COORD_SCALE) / COORD_SCALE)}, ${(kotlin.math.round(n.longitude * COORD_SCALE) / COORD_SCALE)}" }

    private fun String.redacted(): String = replace(SECRET, "$1=<hidden>")

    private fun fmt(value: Float): String = ((value * 10).roundToInt() / 10f).toString()

    private companion object {
        const val TAG = "AssistantData"
        const val MAX_PERIOD_AMOUNT = 1000
        const val RECENT_PER_CHAT = 20
        const val SIGNAL_WINDOW_MS = 3L * 86_400_000L
        val SELF_WORDS = setOf("me", "ja", "mnie", "mój", "moj", "mój węzeł", "moj wezel", "mine", "my node", "own", "own node", "self")
        const val MAX_CONTEXT_CHARS = 3000
        const val MIN_PART = 250
        val REPORT_KEYS: Map<String, StringResource> =
            mapOf(
                "temperature" to Res.string.ai_sensor_temperature, "relative_humidity" to Res.string.ai_sensor_humidity,
                "barometric_pressure" to Res.string.ai_sensor_pressure, "voltage" to Res.string.ai_sensor_voltage,
                "current" to Res.string.ai_sensor_current, "lux" to Res.string.ai_sensor_light,
                "wind_speed" to Res.string.ai_sensor_wind,
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
        const val REPORT_LIMIT = 12
        const val SAME_PLACE_M = 200
        const val STEM_MIN_SEARCH = 6
        const val NEAR_MIN_WORD = 4
        const val NEAR_STEM_MIN = 6
        const val SUGGESTED_NODES = 3
        const val STALE_SENSOR_MIN = 24 * 60L
        const val SIGNAL_SHOWN = 6
        const val NEXT_SHOWN = 3
        const val NAMES_SHOWN = 8
        val CHANNEL_NUMBER = Regex("(?:kana[łl]\\w*|channel)\\s*(?:nr\\.?|numer\\w*|#|no\\.?)?\\s*(\\d+|[a-ząćęłńóśźż]+)")
        val NUMBER_WORDS =
            mapOf("zer" to 0, "jede" to 1, "jedyn" to 1, "one" to 1, "pierw" to 1, "dw" to 2, "two" to 2, "trz" to 3, "three" to 3, "czter" to 4, "four" to 4, "pię" to 5, "pie" to 5, "five" to 5, "sze" to 6, "six" to 6, "sied" to 7, "seven" to 7)
        const val MAX_TOOL_LIST = 40
        const val SEARCH_PER_CHAT = 3000
        const val SEARCH_SHOWN = 15
        val QUOTED = Regex("[\"„”«]([^\"„”»]{2,60})[\"”»]")
        const val HOURS_WORD_DEFAULT = 1
        const val TRACE_WINDOW_MS = 30L * 86_400_000L
        const val TRACE_SHOWN = 6
        const val UNREAD_SHOWN = 6
        const val OWN_STATUS_PER_CHAT = 5
        const val OWN_STATUS_SHOWN = 6
        const val WAYPOINT_SCALE = 1e7
        const val COUNT_PER_CHAT = 3000
        const val MAX_COUNT_HOURS = 8784L
        const val MILLIS_PER_DAY = 86_400_000L
        const val TOP_SENDERS = 8
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
        const val COORD_SCALE = 100000.0
        const val MIN_STEM_WORD = 5
        const val MAX_STEM_HITS = 3
        val GENERIC_WORDS = setOf("router", "routera", "node", "gdzie", "mapie", "znajde", "podac", "lokalizacje", "wezel", "wezla", "wezly", "siec", "sieci", "ktory", "ktora", "jakie", "jakis", "mozesz", "pokaz", "pokazac", "powiedz")
        val FOLD = mapOf('ą' to 'a', 'ć' to 'c', 'ę' to 'e', 'ł' to 'l', 'ń' to 'n', 'ó' to 'o', 'ś' to 's', 'ź' to 'z', 'ż' to 'z')
        val NODE_WORDS = listOf("węz", "wez", "node", "sąsiad", "sasiad", "zasięg", "zasieg", "sieć", "mesh", "snr", "rssi")
        val SENSOR_WORDS = listOf("pogod", "temperatur", "wilgot", "ciśnien", "cisnien", "weather", "humidity", "pressure", "wiatr", "wind", "deszcz", "rain", "powietrz", "air quality", "pm2", "czujnik", "sensor", "środowisk", "srodowisk", "environment", "natężen", "napięci", "prąd", "prad", "voltage", "current", "gleb", "soil", "pax", "lux", "światł")
        val SIGNAL_WORDS = listOf("zasięg", "zasieg", "sygnał", "sygnal", "snr", "rssi", "odległ", "odleg", "daleko", "dystans", "blisk", "najbliż", "najbliz", "jakość", "jakosc", "range", "signal", "distance", "linku", "łącz", "polacz")
        val ACTIVITY_WORDS = listOf("ostatnio słysz", "ostatnio slysz", "kto ostatnio", "ostatnio odezw", "offline", "nieaktyw", "dawno", "milcz", "ostatni raz", "kiedy ostatnio", "najświeżs", "last heard", "zniknęł", "zniknel")
        const val ACTIVITY_TOP = 10
        const val TELEMETRY_CACHE_MS = 60_000L
        val RANK_WORDS = listOf("ramek", "ramki", "pakiet", "odebran", "raportuj", "najwięc", "najwiec", "najmniej", "ranking", "który", "ktory", "najlep", "najgorz")
        val MAIN_CHANNEL_WORDS = setOf("all", "wszyscy", "wszystkich", "główny", "glowny", "primary", "main", "kanał główny")
        val OWN_WORDS = listOf("mój", "moj", "mojego", "mojej", "mojemu", "my node", "own node", "mój węz", "moim")
        val SUMMARY_WORDS = listOf("streść", "streszcz", "podsumuj", "podsumowa", "summar", "o czym", "co pisz", "co się dzieje na", "co sie dzieje na")
        val ANY_WORDS = setOf("all", "any", "wszystkie", "wszystko", "everything", "null", "none", "*")
        val COUNT_WORDS = listOf("najwięcej", "najwiecej", "kto napisa", "kto pisze", "ile ", "ilo", "liczb", "how many", "count", "licz")
        val TODAY_WORDS = listOf("dzisiaj", "dziś", "dzis", "today", "od rana")
        val STATUS_WORDS = listOf("nieprzeczyta", "unread", "połączon", "polaczon", "connected", "dostarczon", "doszła", "doszla", "delivered", "niewysłan", "nie wysłan", "nie wyslan")
        val WAYPOINT_WORDS = listOf("waypoint", "punkt na mapie", "punkty na mapie", "znacznik", "pinezk")
        val SEARCH_WORDS = listOf("szuk", "znajd", "wyszuk", "pojaw", "wspomin", "search", "find", "mention", "czy ktoś pisał", "czy ktos pisal")
        val WORD_AFTER = Regex("(?:słowem|słowo|słowie|słowa|frazą|frazę|fraza|frazie|word|phrase)\\s+[\"„”]?([\\p{L}\\p{N}_-]{2,})")
        val TOP_WORDS = listOf("najwięcej", "najwiecej", "kto napisa", "kto pisze", "who wrote", "most messages")
        val NEIGHBOUR_WORDS = listOf("sąsiad", "sasiad", "neighbo", "słyszy", "slyszy")
        val ROLE_WORDS = listOf("router", "ruter", "repeater", "przekaźnik", "przekaznik", "czujnik", "sensor", "tracker")
        val DISTANCE_WORDS = listOf("najbliż", "najbliz", "najdal", "najlepszy zasięg", "najlepszy zasieg", "closest", "nearest", "farthest")
        val LOW_WORDS = listOf("mało", "malo", "niski", "niską", "nisk", "słab", "slab", "rozładow", "rozladow", "low", "najniż", "najniz")
        const val WAYPOINTS_SHOWN = 10
        val ROUTE_WORDS = listOf("traceroute", "trasa", "trasy", "trasę", "trase", "neighbo", "słyszy", "slyszy", "przez jakie", "przez które")
        val MESSAGE_WORDS = listOf("wiadomo", "message", "czat", "chat", "napisa", "pisa", "rozmow", "odpowied")
    }
}
