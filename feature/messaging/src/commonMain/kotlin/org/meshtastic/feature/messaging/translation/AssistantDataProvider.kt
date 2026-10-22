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
        val parts = ArrayList<String>()
        parts += safe("overview") { overview() }
        if (q.hasAny(TREND_WORDS)) parts += safe("trend") { trends() }
        if (q.hasAny(SETTINGS_WORDS)) parts += safe("settings") { settings() }
        if (q.hasAny(NODE_WORDS)) parts += safe("nodes") { nodes() }
        if (q.hasAny(MESSAGE_WORDS)) parts += safe("messages") { messages() }
        return parts.filter { it.isNotBlank() }.joinToString("\n\n").take(MAX_CONTEXT_CHARS)
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

    private suspend fun trends(): String {
        val myNum = nodeRepository.ourNodeInfo.value?.num ?: return ""
        val since = nowMillis - WINDOW_MS
        val entries =
            meshLogRepository.getLogsFromNodeSince(myNum, since).first().mapNotNull { it.toSample() }.sortedBy { it.time }
        if (entries.isEmpty()) return "TRENDS: no history recorded for the last 24 hours."
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
            appendLine("TRENDS of the own node, last 24 hours (${entries.size} samples):")
            for ((name, unit, pick) in series) {
                val values = entries.mapNotNull { e -> pick(e)?.let { e.time to it } }
                if (values.size < 2) continue
                val first = values.first().second
                val last = values.last().second
                val change = last - first
                val direction = if (kotlin.math.abs(change) < STABLE_DELTA) "stable" else if (change > 0) "rising" else "falling"
                appendLine(
                    "- $name: now ${fmt(last)}$unit, 24h ago ${fmt(first)}$unit, min ${fmt(values.minOf { it.second })}, " +
                        "max ${fmt(values.maxOf { it.second })}, change ${fmt(change)} ($direction), samples ${values.size}",
                )
            }
        }
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

    private fun nodes(): String {
        val now = nowMillis / MILLIS_PER_SECOND
        val list = nodeRepository.nodeDBbyNum.value.values.sortedByDescending { it.lastHeard }.take(NODE_LIMIT)
        return buildString {
            appendLine("NODES (most recently heard first):")
            for (n in list) {
                val ago = if (n.lastHeard == 0) "never" else "${((now - n.lastHeard) / SECONDS_PER_MINUTE).coerceAtLeast(0)} min ago"
                appendLine(
                    "- ${n.user.long_name} (${n.user.short_name}), heard $ago, hops ${n.hopsAway}, " +
                        "snr ${if (n.snr == Float.MAX_VALUE) "?" else fmt(n.snr)}, battery ${n.deviceMetrics.battery_level}",
                )
            }
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
        const val WINDOW_MS = 24 * 60 * 60 * 1000L
        const val MAX_CONTEXT_CHARS = 5000
        const val SETTINGS_CHARS = 1800
        const val NODE_LIMIT = 15
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
        val MESSAGE_WORDS = listOf("wiadomo", "message", "czat", "chat", "napisa", "pisa", "rozmow", "odpowied")
    }
}
