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
 * The settings of the SOS mode. [message] empty means the default text; [repeatMinutes] 0 means send once, otherwise
 * the message is sent again every [repeatMinutes] minutes, up to [repeatCount] times in all, and (when
 * [stopWhenDelivered]) not any more once a send was confirmed. [holdSeconds] is how long the button has to be held
 * (0 = a single tap sends).
 */
data class SosConfig(
    val channel: Int = 0,
    val message: String = "",
    val includePosition: Boolean = true,
    val repeatMinutes: Int = 0,
    val repeatCount: Int = DEFAULT_REPEAT_COUNT,
    val stopWhenDelivered: Boolean = true,
    val holdSeconds: Int = DEFAULT_HOLD_SECONDS,
) {
    /** Written as `channel|position|repeatMinutes|repeatCount|stopWhenDelivered|holdSeconds|message`. */
    fun encode(): String = listOf(
        channel,
        if (includePosition) 1 else 0,
        repeatMinutes,
        repeatCount,
        if (stopWhenDelivered) 1 else 0,
        holdSeconds,
    )
        .joinToString("|") + "|" + message.replace('\n', ' ')

    companion object {
        const val DEFAULT_REPEAT_COUNT = 3
        const val DEFAULT_HOLD_SECONDS = 2
        private const val FIELDS = 7

        /** Reads what [encode] wrote; anything unreadable gives the defaults. */
        fun decode(text: String?): SosConfig {
            val parts = text?.split('|', limit = FIELDS) ?: return SosConfig()
            if (parts.size < FIELDS) return SosConfig()
            val numbers = parts.take(FIELDS - 1).map { it.toIntOrNull() ?: return SosConfig() }
            return SosConfig(
                channel = numbers[0].coerceIn(0, MAX_CHANNEL),
                includePosition = numbers[1] != 0,
                repeatMinutes = numbers[2].coerceAtLeast(0),
                repeatCount = numbers[3].coerceAtLeast(1),
                stopWhenDelivered = numbers[4] != 0,
                holdSeconds = numbers[5].coerceIn(0, MAX_HOLD_SECONDS),
                message = parts[FIELDS - 1],
            )
        }

        private const val MAX_CHANNEL = 7
        private const val MAX_HOLD_SECONDS = 10
    }
}

/** Where the SOS settings are kept. */
interface SosPrefs {
    val config: StateFlow<SosConfig>

    fun setConfig(config: SosConfig)
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
    /** True when the message could not be sent at all (for example the channel is gone from the radio). */
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
