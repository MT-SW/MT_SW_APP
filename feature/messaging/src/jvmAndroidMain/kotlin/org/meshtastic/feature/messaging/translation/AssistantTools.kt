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
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * The read-only tools the assistant can call to look at the data of the app. None of them changes a setting or sends a
 * message. Results are cut to a size the model can take and cleaned of characters its native part cannot handle.
 */
@Suppress("FunctionName")
class AssistantTools(private val data: AssistantDataProvider) : ToolSet {

    @Tool(description = "Own node, radio statistics (packets, heap, cpu, flash), node counts and a summary of the whole network.")
    fun network_overview(): String = run("network_overview") { data.toolOverview() }

    @Tool(description = "Names of all known nodes (short name and long name). Use it to find the exact name of a node.")
    fun list_node_names(): String = run("list_node_names") { data.toolNodeNames() }

    @Tool(
        description =
        "Details of one node: hardware, position, signal, battery, sensors and its recorded history with trends " +
            "(heap, cpu, flash, battery, channel use, noise). Works for any node and for the own node (name \"me\").",
    )
    fun node_details(
        @ToolParam(description = "Short or long name of the node, or \"me\" for the own node") name: String,
        @ToolParam(description = "How many hours of history to use; 0 means the whole recorded history") hours: Int,
    ): String = run("node_details") { data.toolNode(name, hours) }

    @Tool(
        description =
        "Ranked table of nodes. sort_by is one of: rx (packets received), tx (packets sent), dupe (duplicates), bad " +
            "(bad packets), heap (used memory in percent, highest first), battery (lowest first), channel_util, uptime, snr " +
            "(best link first), rssi, distance (farthest first), closest (nearest first, with position), hops. " +
            "Use role = router, repeater, sensor or tracker to list only nodes of that role (for example the nearest router).",
    )
    fun rank_nodes(
        @ToolParam(description = "Metric to sort by") sort_by: String,
        @ToolParam(description = "How many nodes to return, 1 to 40") limit: Int,
        @ToolParam(description = "Only nodes of this role: router, repeater, sensor, tracker, or empty for all") role: String,
    ): String = run("rank_nodes") { data.toolList(sort_by, limit, role) }

    @Tool(
        description =
        "WEATHER statistics of the nodes: temperature, humidity, air pressure, wind, rain, light, air quality, " +
            "voltage and current from the nodes' sensors. Call this for any question about weather, temperature, " +
            "humidity, pressure or sensors.",
    )
    fun weather_and_sensor_data(): String = run("weather_and_sensor_data") { data.toolSensors() }

    @Tool(description = "The radio settings of the own node (device, LoRa, channels, modules). Keys and passwords are hidden.")
    fun radio_settings(): String = run("radio_settings") { data.toolSettings() }

    @Tool(
        description =
        "The latest messages written on a channel, oldest first. Use it to summarise, describe or answer " +
            "questions about what is being said on a channel; it needs no keywords. Give the channel name or " +
            "number, or an empty text for all channels.",
    )
    fun channel_messages(
        @ToolParam(description = "Channel name or number, or empty for all channels") channel: String,
        @ToolParam(description = "How many of the latest messages to read, 1 to 40") limit: Int,
    ): String = run("channel_messages") { data.toolChannelMessages(channel, limit) }

    @Tool(description = "How many messages each channel holds (and direct messages). Use it to find which channel has the most messages.")
    fun channel_message_counts(): String = run("channel_message_counts") { data.channelReport() }

    @Tool(description = "Find messages that contain a specific word or phrase in all conversations (only when the user asks for a specific word).")
    fun search_messages(
        @ToolParam(description = "Text to look for, or empty for the newest messages") query: String,
        @ToolParam(description = "How many messages to return, 1 to 40") limit: Int,
    ): String = run("search_messages") { data.toolMessages(query, limit) }

    /** Longest result a tool may return; the assistant raises it for models with a bigger window. */
    @Volatile var resultLimit: Int = DEFAULT_RESULT_CHARS

    private fun run(name: String, block: suspend () -> String): String {
        Logger.i(tag = TAG) { "Tool $name" }
        val text =
            try {
                runBlocking(Dispatchers.Default) { block() }
            } catch (e: Exception) {
                Logger.w(tag = TAG, throwable = e) { "Tool $name failed" }
                "The data is not available right now."
            }
        val result = text.take(resultLimit).stripUnsafeChars()
        Logger.i(tag = TAG) { "Tool $name result: ${result.take(LOG_CHARS).replace("\n", " | ")}" }
        return result
    }

    private companion object {
        const val TAG = "AssistantTools"
        const val DEFAULT_RESULT_CHARS = 2200
        const val LOG_CHARS = 1500
    }
}

/** Drops emoji and other characters the native part of the model library cannot take, and control characters. */
internal fun String.stripUnsafeChars(): String {
    val out = StringBuilder(length)
    for (c in this) {
        when {
            c.isSurrogate() -> Unit
            c.code < 0x20 && c != '\n' && c != '\t' -> Unit
            c.code == 0xFFFE || c.code == 0xFFFF -> Unit
            else -> out.append(c)
        }
    }
    return out.toString()
}
