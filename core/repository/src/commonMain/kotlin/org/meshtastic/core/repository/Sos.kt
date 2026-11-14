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
import org.meshtastic.core.model.NodeAddress
import kotlin.time.Instant

/**
 * The settings of the SOS mode. The message goes to every channel in [channels] (as a broadcast on it) and as a direct
 * message to every node in [nodes], whether or not that node takes messages: a node that is marked as not monitored is
 * no exception. [message] empty means the default text; [repeatMinutes] 0 means send once, otherwise the message is
 * sent again every [repeatMinutes] minutes, up to [repeatCount] times in all ([UNLIMITED_REPEATS] = without a limit,
 * until a send is confirmed or the person stops it), and (when [stopWhenDelivered]) not any more once every send was
 * confirmed. [holdSeconds] is how long the button has to be held (0 = a single tap sends).
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
    /**
     * Own texts for single recipients, by the keys of [SosTargets] (a channel or a node); a recipient without one gets
     * [message]. Kept apart from [encode], which does not write them.
     */
    val targetMessages: Map<String, String> = emptyMap(),
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

        /** The value of [repeatCount] that means: send again and again, with no limit on the number of sends. */
        const val UNLIMITED_REPEATS = 0
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
                repeatCount = numbers[2].coerceAtLeast(UNLIMITED_REPEATS),
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

/** The keys of the recipients that can have a text of their own (see [SosConfig.targetMessages]). */
object SosTargets {
    private const val ENTRY_SEPARATOR = '\u001E'
    private const val FIELD_SEPARATOR = '\u001F'

    fun channelKey(index: Int): String = "c$index"

    fun nodeKey(num: Int): String = "n$num"

    /** Writes the texts; an empty text means none and is left out. */
    fun encode(messages: Map<String, String>): String = messages.entries
        .filter { it.value.isNotBlank() }
        .joinToString("$ENTRY_SEPARATOR") { "${it.key}$FIELD_SEPARATOR${it.value.filterNot(::isSeparator)}" }

    /** Reads what [encode] wrote; unreadable parts are skipped. */
    fun decode(text: String?): Map<String, String> = text
        .orEmpty()
        .split(ENTRY_SEPARATOR)
        .mapNotNull { entry ->
            val parts = entry.split(FIELD_SEPARATOR, limit = 2)
            if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotBlank()) parts[0] to parts[1] else null
        }
        .toMap()

    private fun isSeparator(c: Char): Boolean = c == ENTRY_SEPARATOR || c == FIELD_SEPARATOR
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
@Suppress("TooManyFunctions")
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

    /** The nodes whose SOS counts as the important one (see [alarmOnlyChosen] and [quietHours]); numbers of nodes. */
    val alarmNodes: StateFlow<Set<Int>>

    fun setAlarmNodes(nodes: Set<Int>)

    /** True when only an SOS from [alarmNodes] rings the full alarm; any other one stays an ordinary message. */
    val alarmOnlyChosen: StateFlow<Boolean>

    fun setAlarmOnlyChosen(on: Boolean)

    /** The quiet hours: in them only an SOS from [quietExceptions] still rings. */
    val quietHours: StateFlow<SosQuietHours>

    fun setQuietHours(hours: SosQuietHours)

    /** The nodes whose SOS rings the full alarm even in the quiet hours; a list of its own. */
    val quietExceptions: StateFlow<Set<Int>>

    fun setQuietExceptions(nodes: Set<Int>)

    /** The sound of the alarm for the nodes in [alarmNodes] that do not use the siren; nodes not here use the siren. */
    val alarmStyles: StateFlow<Map<Int, SosAlarmStyle>>

    fun setAlarmStyles(styles: Map<Int, SosAlarmStyle>)

    /** True when the full alarm goes off only for a second SOS from the same node (off by default). */
    val repeatGate: StateFlow<Boolean>

    fun setRepeatGate(on: Boolean)

    /** The minutes in which the second SOS has to come for [repeatGate]. */
    val repeatGateMinutes: StateFlow<Int>

    fun setRepeatGateMinutes(minutes: Int)

    /** True (the default) when repeating of our own SOS stops as soon as a recipient answers it. */
    val stopOnAnswer: StateFlow<Boolean>

    fun setStopOnAnswer(on: Boolean)

    /** True (the default) when a notification tells us that a recipient answered our SOS. */
    val notifyOnAnswer: StateFlow<Boolean>

    fun setNotifyOnAnswer(on: Boolean)
}

/** How the alarm of a received SOS sounds. */
enum class SosAlarmStyle {
    /** The loud siren, the strong vibration and the flashing torch. */
    SIREN,

    /** A gentler bell in a loop and the vibration, no torch. */
    BELL,

    /** Only the window on the screen: no sound, no vibration, no torch (the ordinary message notification stays). */
    SILENT,

    ;

    companion object {
        /** Reads a style by its name; anything unknown gives the siren. */
        fun parse(name: String?): SosAlarmStyle = entries.firstOrNull { it.name == name } ?: SIREN

        /** Writes the styles as `number:STYLE` pairs joined by commas; the siren is the default and is left out. */
        fun encodeMap(styles: Map<Int, SosAlarmStyle>): String =
            styles.filterValues { it != SIREN }.entries.joinToString(",") { "${it.key}:${it.value.name}" }

        /** Reads what [encodeMap] wrote; unreadable pairs are skipped. */
        fun decodeMap(text: String?): Map<Int, SosAlarmStyle> = text
            .orEmpty()
            .split(',')
            .mapNotNull { pair ->
                val parts = pair.split(':')
                val num = parts.getOrNull(0)?.trim()?.toIntOrNull()
                if (parts.size != 2 || num == null) null else num to parse(parts[1].trim())
            }
            .toMap()
    }
}

/**
 * The quiet hours of the SOS alarm, from [fromMinutes] to [toMinutes] (minutes since midnight, local time). When
 * [toMinutes] is not later than [fromMinutes] the span goes over midnight (22:00 to 07:00). Off by default.
 */
data class SosQuietHours(
    val enabled: Boolean = false,
    val fromMinutes: Int = DEFAULT_FROM,
    val toMinutes: Int = DEFAULT_TO,
) {
    /** True when [minuteOfDay] is inside the quiet hours; never when they are off or the span is empty. */
    fun contains(minuteOfDay: Int): Boolean = when {
        !enabled || fromMinutes == toMinutes -> false
        fromMinutes < toMinutes -> minuteOfDay in fromMinutes until toMinutes
        else -> minuteOfDay >= fromMinutes || minuteOfDay < toMinutes
    }

    fun encode(): String = "${if (enabled) 1 else 0}|$fromMinutes|$toMinutes"

    companion object {
        const val DEFAULT_FROM = 22 * 60
        const val DEFAULT_TO = 7 * 60
        const val DAY_MINUTES = 24 * 60
        private const val PARTS = 3

        /** Reads what [encode] wrote; anything unreadable gives the defaults (off). */
        fun decode(text: String?): SosQuietHours {
            val parts = text?.split('|').orEmpty()
            val from = parts.getOrNull(1)?.toIntOrNull()
            val to = parts.getOrNull(2)?.toIntOrNull()
            if (parts.size != PARTS || from == null || to == null) return SosQuietHours()
            return SosQuietHours(
                enabled = parts[0] == "1",
                fromMinutes = from.coerceIn(0, DAY_MINUTES - 1),
                toMinutes = to.coerceIn(0, DAY_MINUTES - 1),
            )
        }
    }
}

/** Decides whether an SOS that arrived rings the full alarm. Pure, so it is easy to test. */
object SosAlarmPolicy {
    /**
     * True when the alarm rings. A test always does; otherwise the main switch [receive] must be on, when [onlyChosen]
     * is on the sender must be one of [chosen], and in the [quiet] hours (at [minuteOfDay]) the sender must be one of
     * [quietExceptions].
     */
    @Suppress("LongParameterList")
    fun rings(
        test: Boolean,
        receive: Boolean,
        senderNum: Int?,
        onlyChosen: Boolean,
        chosen: Set<Int>,
        quiet: SosQuietHours,
        quietExceptions: Set<Int>,
        minuteOfDay: Int,
    ): Boolean {
        val blockedByChoice = onlyChosen && (senderNum == null || senderNum !in chosen)
        val blockedByQuiet = quiet.contains(minuteOfDay) && (senderNum == null || senderNum !in quietExceptions)
        return test || (receive && !blockedByChoice && !blockedByQuiet)
    }

    /**
     * True when this SOS counts as the second one: the same node sent one less than [windowMinutes] minutes ago
     * ([lastSeen] holds the time of each node's last SOS). A sender that is not known cannot be judged, so it counts.
     */
    fun isSecond(senderNum: Int?, lastSeen: Map<Int, Long>, now: Long, windowMinutes: Int): Boolean {
        val last = senderNum?.let { lastSeen[it] }
        return senderNum == null || (last != null && now - last <= windowMinutes * MILLIS_PER_MINUTE)
    }

    private const val MILLIS_PER_MINUTE = 60_000L

    /** The sound for an SOS from [senderNum]: the one set for a node in [chosen], else the siren. */
    fun styleFor(senderNum: Int?, chosen: Set<Int>, styles: Map<Int, SosAlarmStyle>): SosAlarmStyle =
        if (senderNum != null && senderNum in chosen) styles[senderNum] ?: SosAlarmStyle.SIREN else SosAlarmStyle.SIREN
}

/** One SOS that was received: when, from whom, what it said and where the sender last was. */
data class SosHistoryEntry(
    val time: Long,
    val senderNum: Int?,
    val senderName: String,
    val contactKey: String,
    val text: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** True when the full alarm rang for it; false when it was kept quiet by a setting. */
    val alarmed: Boolean = true,
)

/** The history of received SOS (never the tests), the newest first, at most [SosHistoryCodec.MAX_ENTRIES]. */
interface SosHistory {
    val entries: StateFlow<List<SosHistoryEntry>>

    fun add(entry: SosHistoryEntry)

    fun clear()

    /** Entries older than this many days are removed by themselves; 0 (the default) keeps them all. */
    val keepDays: StateFlow<Int>

    fun setKeepDays(days: Int)
}

/** Writes and reads the history as one text: entries apart by one separator, the fields of an entry by another. */
object SosHistoryCodec {
    const val MAX_ENTRIES = 50
    private const val ENTRY_SEPARATOR = '\u001E'
    private const val FIELD_SEPARATOR = '\u001F'
    private const val FIELDS = 8

    fun encode(entries: List<SosHistoryEntry>): String = entries.take(MAX_ENTRIES).joinToString("$ENTRY_SEPARATOR") {
        listOf(
            it.time.toString(),
            it.senderNum?.toString().orEmpty(),
            it.senderName,
            it.contactKey,
            it.text,
            it.latitude?.toString().orEmpty(),
            it.longitude?.toString().orEmpty(),
            if (it.alarmed) "1" else "0",
        )
            .joinToString("$FIELD_SEPARATOR") { field -> field.filterNot(::isSeparator) }
    }

    private fun isSeparator(c: Char): Boolean = c == ENTRY_SEPARATOR || c == FIELD_SEPARATOR

    /** [entries] without those older than [keepDays] days at [now]; [keepDays] 0 or less keeps them all. */
    fun prune(entries: List<SosHistoryEntry>, keepDays: Int, now: Long): List<SosHistoryEntry> =
        if (keepDays <= 0) entries else entries.filter { now - it.time <= keepDays * MILLIS_PER_DAY }

    private const val MILLIS_PER_DAY = 86_400_000L

    /** Entries that cannot be read are skipped; nothing readable gives an empty list. */
    fun decode(text: String?): List<SosHistoryEntry> {
        if (text.isNullOrEmpty()) return emptyList()
        return text.split(ENTRY_SEPARATOR).mapNotNull { entry ->
            val f = entry.split(FIELD_SEPARATOR)
            val time = f.getOrNull(0)?.toLongOrNull()
            if (f.size != FIELDS || time == null) {
                null
            } else {
                SosHistoryEntry(
                    time = time,
                    senderNum = f[1].toIntOrNull(),
                    senderName = f[2],
                    contactKey = f[3],
                    text = f[4],
                    latitude = f[5].toDoubleOrNull(),
                    longitude = f[6].toDoubleOrNull(),
                    alarmed = f[7] != "0",
                )
            }
        }
    }
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

/** What a recipient answered to an SOS: it was seen, they are coming, or they are calling for help. */
enum class SosAck(internal val bits: String) {
    SEEN("01001101"),
    COMING("01001110"),
    CALLING_HELP("01001011"),
}

/**
 * The hidden mark in front of the answers a recipient's copy of this app sends, so the sender's copy can tell who
 * confirmed (see [SosAck]). Like [SosMarker] it is no lock, only a way to tell these answers from an ordinary text.
 */
object SosAckMarker {
    private const val START = '\u2063'
    private const val END = '\u2064'
    private const val ZERO = '\u200B'
    private const val ONE = '\u200C'
    private const val SEARCH_LENGTH = 24

    /** The characters written in front of the answer [ack]. */
    fun tag(ack: SosAck): String = buildString {
        append(START)
        ack.bits.forEach { append(if (it == '1') ONE else ZERO) }
        append(END)
    }

    /** The answer that [text] opens with, or null when it is an ordinary text. */
    fun parse(text: String?): SosAck? {
        val head = text?.take(SEARCH_LENGTH) ?: return null
        return SosAck.entries.firstOrNull { head.contains(tag(it)) }
    }

    /** [text] without any mark. */
    fun clean(text: String): String = SosAck.entries.fold(text) { acc, ack -> acc.replace(tag(ack), "") }
}

/** One answer of a recipient to our SOS. */
data class SosAckEntry(val time: Long, val nodeNum: Int, val name: String, val ack: SosAck)

/** The answers to the SOS we sent last (cleared when a new SOS is started), the newest first. */
interface SosAcks {
    val entries: StateFlow<List<SosAckEntry>>

    fun add(entry: SosAckEntry)

    fun clear()
}

/** Writes and reads the answers as one text. */
object SosAckCodec {
    const val MAX_ENTRIES = 50
    private const val ENTRY_SEPARATOR = '\u001E'
    private const val FIELD_SEPARATOR = '\u001F'
    private const val FIELDS = 4
    private const val ACK_FIELD = 3

    fun encode(entries: List<SosAckEntry>): String = entries.take(MAX_ENTRIES).joinToString("$ENTRY_SEPARATOR") {
        listOf(it.time.toString(), it.nodeNum.toString(), it.name.filterNot(::isSeparator), it.ack.name)
            .joinToString("$FIELD_SEPARATOR")
    }

    fun decode(text: String?): List<SosAckEntry> {
        if (text.isNullOrEmpty()) return emptyList()
        return text.split(ENTRY_SEPARATOR).mapNotNull { entry ->
            val f = entry.split(FIELD_SEPARATOR)
            val time = f.getOrNull(0)?.toLongOrNull()
            val num = f.getOrNull(1)?.toIntOrNull()
            val ack = SosAck.entries.firstOrNull { it.name == f.getOrNull(ACK_FIELD) }
            if (f.size != FIELDS || time == null || num == null || ack == null) {
                null
            } else {
                SosAckEntry(time, num, f[2], ack)
            }
        }
    }

    private fun isSeparator(c: Char): Boolean = c == ENTRY_SEPARATOR || c == FIELD_SEPARATOR
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
    /** The sound to use; null lets the settings decide by the sender (a test of a sound sets it). */
    val style: SosAlarmStyle? = null,
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

    /** A recipient answered an SOS we sent (the answer carries [SosAckMarker]): it goes into the list of [SosAcks]. */
    fun ackReceived(fromNodeNum: Int, senderName: String, ack: SosAck)
}

/**
 * The looping sound of the alarm where the app has to make it itself (desktop, iPhone/iPad); Android uses the
 * notification, which rings again and again by itself.
 */
interface SosAlarmSound {
    /**
     * Starts the alarm in [style]; [flash] says whether the torch should flash too (where the device has one). A silent
     * alarm is never started here.
     */
    fun start(flash: Boolean, style: SosAlarmStyle = SosAlarmStyle.SIREN)

    fun stop()
}

/**
 * The way a button on a platform's SOS alarm notification reaches the alarm center without a dependency cycle between
 * the notifications and the center: the center puts itself here when it starts.
 */
object SosNotificationActions {
    /** The identifier of the button "alarm confirmed", and of the answers; see [reply]. */
    const val SEEN = "seen"
    const val COMING = "coming"
    const val CALLING_HELP = "calling_help"

    var current: SosAlarmCenter? = null

    /** True while an alarm of a received SOS is showing, so its notification may carry the answer buttons. */
    fun alarmShowing(): Boolean = current?.alert?.value != null

    /** Does what the button named [action] stands for: confirms the alarm and sends the answer, if any. */
    fun reply(action: String) {
        val center = current ?: return
        when (action) {
            COMING -> center.reply(SosReply.COMING)
            CALLING_HELP -> center.reply(SosReply.CALLING_HELP)
            else -> center.acknowledge()
        }
    }
}

/** The last answer of each node in [this] (the list is newest first), for marking them on the map. */
fun List<SosAckEntry>.latestByNode(): Map<Int, SosAck> {
    val latest = LinkedHashMap<Int, SosAck>()
    for (entry in this) if (entry.nodeNum !in latest) latest[entry.nodeNum] = entry.ack
    return latest
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
    /** True when a recipient answered and the repeating was stopped because of it. */
    val answered: Boolean = false,
)

/** Sends the SOS message and follows its delivery; keeps going when the screen is closed. */
interface SosController {
    val state: StateFlow<SosState>

    /** Starts sending with [config]; does nothing while a run is already going. */
    fun start(config: SosConfig)

    /** Stops a run in progress. */
    fun cancel()
}

/** The answers to our SOS as a text a spreadsheet opens: one line per answer, a header first. */
object SosAckExport {
    private const val BOM = "\uFEFF"
    private const val HEADER = "time_utc,name,node,answer"

    fun csv(entries: List<SosAckEntry>): String = BOM +
        HEADER +
        "\n" +
        entries.joinToString("") { e ->
            listOf(
                Instant.fromEpochMilliseconds(e.time).toString(),
                e.name,
                NodeAddress.numToDefaultId(e.nodeNum),
                e.ack.name.lowercase(),
            )
                .joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" } + "\n"
        }
}

/** The history as a text a spreadsheet opens: one line per SOS, a header first. */
object SosHistoryExport {
    private const val BOM = "\uFEFF"
    private const val HEADER = "time_utc,sender,sender_node,text,latitude,longitude,alarm"

    fun csv(entries: List<SosHistoryEntry>): String = BOM +
        HEADER +
        "\n" +
        entries.joinToString("") { e ->
            listOf(
                Instant.fromEpochMilliseconds(e.time).toString(),
                e.senderName,
                e.senderNum?.let { NodeAddress.numToDefaultId(it) }.orEmpty(),
                e.text,
                e.latitude?.toString().orEmpty(),
                e.longitude?.toString().orEmpty(),
                if (e.alarmed) "yes" else "no",
            )
                .joinToString(",") { quote(it) } + "\n"
        }

    private fun quote(field: String): String = "\"" + field.replace("\"", "\"\"") + "\""
}
