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
package org.meshtastic.core.repository

import kotlinx.coroutines.flow.StateFlow
import org.meshtastic.core.model.MessageStatus

/**
 * The settings of the SOS mode. The message goes to every channel in [channels] (as a broadcast on it) and as a direct
 * message to every node in [nodes], whether or not that node takes messages: a node that is marked as not monitored is
 * no exception. [message] empty means the default text; [repeatMinutes] 0 means send once, otherwise the message is
 * sent again every [repeatMinutes] minutes, up to [repeatCount] times in all, and (when [stopWhenDelivered]) not any
 * more once every send was confirmed. [holdSeconds] is how long the button has to be held (0 = a single tap sends).
 */
data class SosConfig(
    val channels: List<Int> = listOf(0),
    val nodes: List<Int> = emptyList(),
    val message: String = "",
    val includePosition: Boolean = true,
    val repeatMinutes: Int = 0,
    val repeatCount: Int = DEFAULT_REPEAT_COUNT,
    val stopWhenDelivered: Boolean = true,
    val holdSeconds: Int = DEFAULT_HOLD_SECONDS,
) {
    /**
     * Written as `c<channels>|position|repeatMinutes|repeatCount|stopWhenDelivered|holdSeconds|<nodes>|message`, the
     * channels and the node numbers each separated by commas. The leading `c` tells it from the older form that held a
     * single channel number and no nodes.
     */
    fun encode(): String = listOf(
        "c" + channels.joinToString(","),
        if (includePosition) 1 else 0,
        repeatMinutes,
        repeatCount,
        if (stopWhenDelivered) 1 else 0,
        holdSeconds,
        nodes.joinToString(","),
    )
        .joinToString("|") + "|" + message.replace('\n', ' ')

    companion object {
        const val DEFAULT_REPEAT_COUNT = 3
        const val DEFAULT_HOLD_SECONDS = 2
        private const val FIELDS = 8
        private const val OLD_FIELDS = 7
        private const val CHANNEL_MARK = "c"

        /** Reads what [encode] wrote, or the older single-channel form; anything unreadable gives the defaults. */
        @Suppress("ReturnCount")
        fun decode(text: String?): SosConfig {
            val raw = text ?: return SosConfig()
            val parts = raw.split('|', limit = FIELDS)
            if (parts.size >= FIELDS && parts[0].startsWith(CHANNEL_MARK)) return decodeNew(parts)
            return decodeOld(raw.split('|', limit = OLD_FIELDS))
        }

        @Suppress("ReturnCount")
        private fun decodeNew(parts: List<String>): SosConfig {
            val numbers = parts.subList(1, FIELDS - 2).map { it.toIntOrNull() ?: return SosConfig() }
            val nodes = numberList(parts[FIELDS - 2])
            // Only nodes is fine (direct messages alone); with nothing chosen at all the main channel stays.
            val chosen = channelList(parts[0].removePrefix(CHANNEL_MARK))
            val channels = if (chosen.isEmpty() && nodes.isEmpty()) listOf(0) else chosen
            return SosConfig(
                channels = channels,
                nodes = nodes,
                includePosition = numbers[0] != 0,
                repeatMinutes = numbers[1].coerceAtLeast(0),
                repeatCount = numbers[2].coerceAtLeast(1),
                stopWhenDelivered = numbers[3] != 0,
                holdSeconds = numbers[4].coerceIn(0, MAX_HOLD_SECONDS),
                message = parts[FIELDS - 1],
            )
        }

        @Suppress("ReturnCount")
        private fun decodeOld(parts: List<String>): SosConfig {
            if (parts.size < OLD_FIELDS) return SosConfig()
            val numbers = parts.take(OLD_FIELDS - 1).map { it.toIntOrNull() ?: return SosConfig() }
            return SosConfig(
                channels = listOf(numbers[0].coerceIn(0, MAX_CHANNEL)),
                includePosition = numbers[1] != 0,
                repeatMinutes = numbers[2].coerceAtLeast(0),
                repeatCount = numbers[3].coerceAtLeast(1),
                stopWhenDelivered = numbers[4] != 0,
                holdSeconds = numbers[5].coerceIn(0, MAX_HOLD_SECONDS),
                message = parts[OLD_FIELDS - 1],
            )
        }

        private fun numberList(text: String): List<Int> =
            text.split(',').mapNotNull { it.trim().toIntOrNull() }.distinct()

        private fun channelList(text: String): List<Int> =
            numberList(text).map { it.coerceIn(0, MAX_CHANNEL) }.distinct().sorted()

        private const val MAX_CHANNEL = 7
        private const val MAX_HOLD_SECONDS = 10
    }
}

/**
 * The cards (screens) whose top bar carries the red SOS button, named by the keys below. All of them by default; none
 * means the button is off everywhere.
 */
object SosBarScreens {
    const val CONNECTIONS = "connections"
    const val MESSAGES = "messages"
    const val NODES = "nodes"
    const val MAP = "map"
    const val SETTINGS = "settings"
    const val AI = "ai"
    const val HEALTH = "health"
    const val OTHER = "other"

    val ALL = listOf(CONNECTIONS, MESSAGES, NODES, MAP, SETTINGS, AI, HEALTH, OTHER)
}

/** Where the SOS settings are kept. */
interface SosPrefs {
    val config: StateFlow<SosConfig>

    fun setConfig(config: SosConfig)

    /** The keys (see [SosBarScreens]) of the cards that show the SOS button in their top bar. */
    val barScreens: StateFlow<Set<String>>

    fun setBarScreens(screens: Set<String>)
}

/**
 * What the SOS mode is doing: [attempt] of [total] sends, [status] of the last one, and, when [running] is false and
 * [attempt] is not 0, whether any send was confirmed ([delivered]).
 */
data class SosState(
    val running: Boolean = false,
    val attempt: Int = 0,
    val total: Int = 0,
    val status: MessageStatus? = null,
    val delivered: Boolean = false,
    /** True when the last message carried the position. */
    val withPosition: Boolean = false,
    /** True when no message could be sent at all (for example every channel is gone from the radio). */
    val refused: Boolean = false,
)

/** Sends the SOS message and follows its delivery; keeps going when the screen is closed. */
interface SosController {
    val state: StateFlow<SosState>

    /** Starts sending with [config]; does nothing while a run is already going. */
    fun start(config: SosConfig)

    /** Stops a run in progress. */
    fun cancel()
}
