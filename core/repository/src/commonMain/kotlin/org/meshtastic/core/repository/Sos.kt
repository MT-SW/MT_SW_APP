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
    /** How long the SOS button of the top bars has to be held: never less than [BAR_MIN_HOLD_SECONDS]. */
    val barHoldSeconds: Int
        get() = holdSeconds.coerceAtLeast(BAR_MIN_HOLD_SECONDS)

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

        /** The shortest hold of the bar button, so it never fires by accident; a shorter setting is raised to it. */
        const val BAR_MIN_HOLD_SECONDS = 2
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

    /** True (the default) when a received SOS from another copy of this app raises the full-screen alarm. */
    val receiveAlarm: StateFlow<Boolean>

    fun setReceiveAlarm(on: Boolean)

    /** True (the default) when the alarm also flashes the phone's torch. */
    val alarmFlash: StateFlow<Boolean>

    fun setAlarmFlash(on: Boolean)

    /** True (the default) when confirming an alarm also tells the sender that the SOS was seen. */
    val confirmToSender: StateFlow<Boolean>

    fun setConfirmToSender(on: Boolean)
}

/**
 * The hidden mark that an SOS message was sent by this app, so that the alarm on the receiving side is raised only by
 * these and never by a message that merely says "SOS". It is a run of invisible characters (not shown by the chat)
 * placed right after the bell. It keeps accidents and ordinary texts away; it is not a lock, because the app is open
 * source and anyone who is on the channel can write the same characters.
 */
object SosMarker {
    private const val START = '\u2063'
    private const val END = '\u2064'
    private const val ZERO = '\u200B'
    private const val ONE = '\u200C'
    private const val PATTERN = "10110010"
    private const val SEARCH_LENGTH = 24

    /** The characters written into the message. */
    val TAG: String = buildString {
        append(START)
        PATTERN.forEach { append(if (it == '1') ONE else ZERO) }
        append(END)
    }

    /** True when [text] opens with the mark (a long message split in parts keeps it in the first one). */
    fun isSos(text: String?): Boolean = text != null && text.take(SEARCH_LENGTH).contains(TAG)

    /** [text] without the mark. */
    fun clean(text: String): String = text.replace(TAG, "")
}

/** An SOS that arrived (or a test of the alarm, [test]); [contactKey] is the conversation it came in. */
data class SosAlert(
    val id: Long,
    val contactKey: String,
    val senderName: String,
    val text: String,
    val test: Boolean = false,
    /** The node that sent it, when it is known: replies and the map go there. */
    val fromNodeNum: Int? = null,
)

/** The ready answers of the alarm window; each is sent to the sender as a direct message. */
enum class SosReply {
    /** "I am coming to you." */
    COMING,

    /** "I am calling for help." */
    CALLING_HELP,
}

/**
 * The alarm for an SOS that arrived from another copy of this app: it holds the alert that is ringing (shown on the
 * whole screen) until the person confirms it, and starts and stops the sound.
 */
interface SosAlarmCenter {
    /** The alert that is ringing now, or null. */
    val alert: StateFlow<SosAlert?>

    /** Starts the alarm (does nothing when the person turned the alarm off in the SOS screen, except for a test). */
    fun raise(alert: SosAlert)

    /**
     * The person confirmed it: the sound and the notification stop, and (when the setting is on) the sender is told
     * that the SOS was seen.
     */
    fun acknowledge()

    /** Answers the sender with a ready message and confirms the alarm. Does nothing but confirm for a test. */
    fun reply(reply: SosReply)
}

/**
 * The looping sound of the alarm where the app has to make it itself (desktop, iPhone/iPad); Android uses the
 * notification, which rings again and again by itself.
 */
interface SosAlarmSound {
    /** Starts the alarm; [flash] says whether the torch should flash too (where the device has one). */
    fun start(flash: Boolean)

    fun stop()
}

/** Holder of the platform's [SosAlarmSound]; null where the platform's notification rings by itself. */
object SosAlarmSounds {
    var current: SosAlarmSound? = null
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
