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
@file:Suppress("LongMethod", "CyclomaticComplexMethod", "LongParameterList", "TooManyFunctions")

package org.meshtastic.feature.settings.console

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.TelemetryType
import org.meshtastic.core.model.effectiveLongName
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.console_arg_channel
import org.meshtastic.core.resources.console_arg_confirm
import org.meshtastic.core.resources.console_arg_keep
import org.meshtastic.core.resources.console_arg_read
import org.meshtastic.core.resources.console_arg_telemetry_types
import org.meshtastic.core.resources.console_arg_to
import org.meshtastic.core.resources.console_arg_write
import org.meshtastic.core.resources.console_cmd_canned_name
import org.meshtastic.core.resources.console_cmd_canned_summary
import org.meshtastic.core.resources.console_cmd_canned_usage
import org.meshtastic.core.resources.console_cmd_factory_name
import org.meshtastic.core.resources.console_cmd_factory_summary
import org.meshtastic.core.resources.console_cmd_factory_usage
import org.meshtastic.core.resources.console_cmd_favorite_name
import org.meshtastic.core.resources.console_cmd_favorite_summary
import org.meshtastic.core.resources.console_cmd_favorite_usage
import org.meshtastic.core.resources.console_cmd_gpio_name
import org.meshtastic.core.resources.console_cmd_gpio_summary
import org.meshtastic.core.resources.console_cmd_gpio_usage
import org.meshtastic.core.resources.console_cmd_ignored_name
import org.meshtastic.core.resources.console_cmd_ignored_summary
import org.meshtastic.core.resources.console_cmd_ignored_usage
import org.meshtastic.core.resources.console_cmd_info_name
import org.meshtastic.core.resources.console_cmd_info_summary
import org.meshtastic.core.resources.console_cmd_info_usage
import org.meshtastic.core.resources.console_cmd_owner_name
import org.meshtastic.core.resources.console_cmd_owner_summary
import org.meshtastic.core.resources.console_cmd_owner_usage
import org.meshtastic.core.resources.console_cmd_reboot_name
import org.meshtastic.core.resources.console_cmd_reboot_summary
import org.meshtastic.core.resources.console_cmd_reboot_usage
import org.meshtastic.core.resources.console_cmd_remove_node_name
import org.meshtastic.core.resources.console_cmd_remove_node_summary
import org.meshtastic.core.resources.console_cmd_remove_node_usage
import org.meshtastic.core.resources.console_cmd_request_position_name
import org.meshtastic.core.resources.console_cmd_request_position_summary
import org.meshtastic.core.resources.console_cmd_request_position_usage
import org.meshtastic.core.resources.console_cmd_reset_nodes_name
import org.meshtastic.core.resources.console_cmd_reset_nodes_summary
import org.meshtastic.core.resources.console_cmd_reset_nodes_usage
import org.meshtastic.core.resources.console_cmd_ringtone_name
import org.meshtastic.core.resources.console_cmd_ringtone_summary
import org.meshtastic.core.resources.console_cmd_ringtone_usage
import org.meshtastic.core.resources.console_cmd_send_name
import org.meshtastic.core.resources.console_cmd_send_summary
import org.meshtastic.core.resources.console_cmd_send_usage
import org.meshtastic.core.resources.console_cmd_shutdown_name
import org.meshtastic.core.resources.console_cmd_shutdown_summary
import org.meshtastic.core.resources.console_cmd_shutdown_usage
import org.meshtastic.core.resources.console_cmd_telemetry_name
import org.meshtastic.core.resources.console_cmd_telemetry_summary
import org.meshtastic.core.resources.console_cmd_telemetry_usage
import org.meshtastic.core.resources.console_cmd_time_name
import org.meshtastic.core.resources.console_cmd_time_summary
import org.meshtastic.core.resources.console_cmd_time_usage
import org.meshtastic.core.resources.console_cmd_traceroute_name
import org.meshtastic.core.resources.console_cmd_traceroute_summary
import org.meshtastic.core.resources.console_cmd_traceroute_usage
import org.meshtastic.core.resources.console_out_confirm
import org.meshtastic.core.resources.console_out_done
import org.meshtastic.core.resources.console_out_gpio_value
import org.meshtastic.core.resources.console_out_info_node
import org.meshtastic.core.resources.console_out_info_nodes
import org.meshtastic.core.resources.console_out_no_answer
import org.meshtastic.core.resources.console_out_node_not_found
import org.meshtastic.core.resources.console_out_position_no_device
import org.meshtastic.core.resources.console_out_usage_bad
import org.meshtastic.core.resources.getStringSuspend

/** What the console may do on the connected device; every call answers false when there is no connected device. */
interface ConsoleDevice {
    fun ownNode(): Node?

    suspend fun send(text: String, channel: Int, destination: Node?): Boolean

    suspend fun reboot(): Boolean

    suspend fun shutdown(): Boolean

    suspend fun factoryReset(): Boolean

    suspend fun resetNodes(keepFavorites: Boolean): Boolean

    suspend fun removeNode(node: Node): Boolean

    suspend fun setFavorite(node: Node, on: Boolean): Boolean

    suspend fun setIgnored(node: Node, on: Boolean): Boolean

    suspend fun traceroute(node: Node): Boolean

    suspend fun requestPosition(node: Node): Boolean

    suspend fun telemetry(node: Node, type: TelemetryType): Boolean

    suspend fun setOwner(longName: String, shortName: String?): Boolean

    suspend fun setTime(): Boolean

    /** Plays the ringtone when [rtttl] is null, otherwise sets it. */
    suspend fun ringtone(rtttl: String?): Boolean

    suspend fun canned(messages: String): Boolean

    suspend fun gpioRead(mask: Long): Long?

    suspend fun gpioWrite(mask: Long, value: Long): Boolean
}

/** The labels of the commands that act on the device, and of the words they take. */
class ConsoleDeviceLabels(
    val info: ConsoleLabel,
    val send: ConsoleLabel,
    val reboot: ConsoleLabel,
    val shutdown: ConsoleLabel,
    val factory: ConsoleLabel,
    val resetNodes: ConsoleLabel,
    val removeNode: ConsoleLabel,
    val favorite: ConsoleLabel,
    val ignored: ConsoleLabel,
    val traceroute: ConsoleLabel,
    val requestPosition: ConsoleLabel,
    val telemetry: ConsoleLabel,
    val owner: ConsoleLabel,
    val time: ConsoleLabel,
    val ringtone: ConsoleLabel,
    val canned: ConsoleLabel,
    val gpio: ConsoleLabel,
    val argChannel: String,
    val argTo: String,
    val argConfirm: String,
    val argKeep: String,
    val argRead: String,
    val argWrite: String,
    val telemetryTypes: List<String>,
)

@Composable
private fun label(name: StringResource, summary: StringResource, usage: StringResource) =
    ConsoleLabel(stringResource(name), stringResource(summary), stringResource(usage))

@Composable
fun rememberConsoleDeviceLabels(): ConsoleDeviceLabels {
    val info = label(Res.string.console_cmd_info_name, Res.string.console_cmd_info_summary, Res.string.console_cmd_info_usage)
    val send = label(Res.string.console_cmd_send_name, Res.string.console_cmd_send_summary, Res.string.console_cmd_send_usage)
    val reboot =
        label(Res.string.console_cmd_reboot_name, Res.string.console_cmd_reboot_summary, Res.string.console_cmd_reboot_usage)
    val shutdown =
        label(
            Res.string.console_cmd_shutdown_name,
            Res.string.console_cmd_shutdown_summary,
            Res.string.console_cmd_shutdown_usage,
        )
    val factory =
        label(
            Res.string.console_cmd_factory_name,
            Res.string.console_cmd_factory_summary,
            Res.string.console_cmd_factory_usage,
        )
    val resetNodes =
        label(
            Res.string.console_cmd_reset_nodes_name,
            Res.string.console_cmd_reset_nodes_summary,
            Res.string.console_cmd_reset_nodes_usage,
        )
    val removeNode =
        label(
            Res.string.console_cmd_remove_node_name,
            Res.string.console_cmd_remove_node_summary,
            Res.string.console_cmd_remove_node_usage,
        )
    val favorite =
        label(
            Res.string.console_cmd_favorite_name,
            Res.string.console_cmd_favorite_summary,
            Res.string.console_cmd_favorite_usage,
        )
    val ignored =
        label(
            Res.string.console_cmd_ignored_name,
            Res.string.console_cmd_ignored_summary,
            Res.string.console_cmd_ignored_usage,
        )
    val traceroute =
        label(
            Res.string.console_cmd_traceroute_name,
            Res.string.console_cmd_traceroute_summary,
            Res.string.console_cmd_traceroute_usage,
        )
    val requestPosition =
        label(
            Res.string.console_cmd_request_position_name,
            Res.string.console_cmd_request_position_summary,
            Res.string.console_cmd_request_position_usage,
        )
    val telemetry =
        label(
            Res.string.console_cmd_telemetry_name,
            Res.string.console_cmd_telemetry_summary,
            Res.string.console_cmd_telemetry_usage,
        )
    val owner =
        label(Res.string.console_cmd_owner_name, Res.string.console_cmd_owner_summary, Res.string.console_cmd_owner_usage)
    val time = label(Res.string.console_cmd_time_name, Res.string.console_cmd_time_summary, Res.string.console_cmd_time_usage)
    val ringtone =
        label(
            Res.string.console_cmd_ringtone_name,
            Res.string.console_cmd_ringtone_summary,
            Res.string.console_cmd_ringtone_usage,
        )
    val canned =
        label(
            Res.string.console_cmd_canned_name,
            Res.string.console_cmd_canned_summary,
            Res.string.console_cmd_canned_usage,
        )
    val gpio = label(Res.string.console_cmd_gpio_name, Res.string.console_cmd_gpio_summary, Res.string.console_cmd_gpio_usage)
    val argChannel = stringResource(Res.string.console_arg_channel)
    val argTo = stringResource(Res.string.console_arg_to)
    val argConfirm = stringResource(Res.string.console_arg_confirm)
    val argKeep = stringResource(Res.string.console_arg_keep)
    val argRead = stringResource(Res.string.console_arg_read)
    val argWrite = stringResource(Res.string.console_arg_write)
    val types = stringResource(Res.string.console_arg_telemetry_types)
    return remember(
        info, send, reboot, shutdown, factory, resetNodes, removeNode, favorite, ignored, traceroute, requestPosition,
        telemetry, owner, time, ringtone, canned, gpio, argChannel, argTo, argConfirm, argKeep, argRead, argWrite, types,
    ) {
        ConsoleDeviceLabels(
            info, send, reboot, shutdown, factory, resetNodes, removeNode, favorite, ignored, traceroute,
            requestPosition, telemetry, owner, time, ringtone, canned, gpio, argChannel, argTo, argConfirm, argKeep,
            argRead, argWrite, types.split(',').map { it.trim() },
        )
    }
}

private const val MAX_CHANNEL = 7

/** Finds a node by `!id`, number, short name, or (when only one fits) a part of the long name. */
internal fun findNode(nodes: List<Node>, text: String): Node? {
    val key = ConsoleText.fold(text)
    if (key.isEmpty()) return null
    nodes.firstOrNull { ConsoleText.fold(it.user.id) == key }?.let { return it }
    key.toLongOrNull()?.let { number -> nodes.firstOrNull { it.num.toLong() and 0xFFFFFFFFL == number }?.let { return it } }
    nodes.firstOrNull { ConsoleText.fold(it.user.short_name) == key }?.let { return it }
    nodes.firstOrNull { ConsoleText.fold(it.effectiveLongName) == key }?.let { return it }
    return nodes.filter { ConsoleText.fold(it.effectiveLongName).contains(key) }.singleOrNull()
}

/** The commands that act on the connected device; the same set as the commands of the Meshtastic command line. */
fun buildDeviceCommands(env: ConsoleEnvironment, labels: ConsoleDeviceLabels, on: String, off: String): List<ConsoleCommand> {
    val device = env.device

    suspend fun bad(label: ConsoleLabel) = ConsoleResult(listOf(getStringSuspend(Res.string.console_out_usage_bad, label.usage)))

    suspend fun done(ok: Boolean) =
        ConsoleResult(listOf(getStringSuspend(if (ok) Res.string.console_out_done else Res.string.console_out_position_no_device)))

    suspend fun missing(text: String) = ConsoleResult(listOf(getStringSuspend(Res.string.console_out_node_not_found, text)))

    suspend fun confirmNeeded(label: ConsoleLabel) =
        ConsoleResult(listOf(getStringSuspend(Res.string.console_out_confirm, labels.argConfirm, label.usage)))

    fun isWord(text: String?, word: String) = text != null && ConsoleText.fold(text) == ConsoleText.fold(word)

    fun nodeNames() = env.nodes().map { it.user.short_name.ifBlank { it.user.id } }

    /** A command that takes one node and does one thing to it. */
    fun nodeCommand(label: ConsoleLabel, action: suspend (Node) -> Boolean) =
        ConsoleCommand(label.name, label.summary, label.usage, complete = { args -> if (args.isEmpty()) nodeNames() else emptyList() }) { args ->
            val text = args.firstOrNull()
            val node = text?.let { findNode(env.nodes(), it) }
            when {
                text == null -> bad(label)
                node == null -> missing(text)
                else -> done(action(node))
            }
        }

    /** A command that takes one node and a word for on or off. */
    fun switchCommand(label: ConsoleLabel, action: suspend (Node, Boolean) -> Boolean) =
        ConsoleCommand(
            label.name,
            label.summary,
            label.usage,
            complete = { args ->
                when (args.size) {
                    0 -> nodeNames()
                    1 -> listOf(on, off)
                    else -> emptyList()
                }
            },
        ) { args ->
            val text = args.firstOrNull()
            val node = text?.let { findNode(env.nodes(), it) }
            val enable = !isWord(args.getOrNull(1), off)
            when {
                text == null -> bad(label)
                node == null -> missing(text)
                else -> done(action(node, enable))
            }
        }

    /** A command that needs the confirming word, because it cannot be undone. */
    fun guarded(label: ConsoleLabel, action: suspend () -> Boolean) =
        ConsoleCommand(label.name, label.summary, label.usage, complete = { args -> if (args.isEmpty()) listOf(labels.argConfirm) else emptyList() }) { args ->
            if (isWord(args.firstOrNull(), labels.argConfirm)) done(action()) else confirmNeeded(label)
        }

    return listOf(
        ConsoleCommand(labels.info.name, labels.info.summary, labels.info.usage) {
            val own = device.ownNode()
            if (own == null) {
                ConsoleResult(listOf(getStringSuspend(Res.string.console_out_position_no_device)))
            } else {
                ConsoleResult(
                    listOf(
                        getStringSuspend(
                            Res.string.console_out_info_node,
                            own.effectiveLongName,
                            own.user.short_name,
                            own.user.id,
                            own.num.toLong() and 0xFFFFFFFFL,
                        ),
                        getStringSuspend(Res.string.console_out_info_nodes, env.nodes().size),
                    ),
                )
            }
        },
        ConsoleCommand(labels.send.name, labels.send.summary, labels.send.usage) { args ->
            var rest = args
            var channel = 0
            var destination: Node? = null
            var wrong: String? = null
            while (rest.size >= 2 && wrong == null) {
                val word = rest.first()
                if (isWord(word, labels.argChannel)) {
                    val number = rest[1].toIntOrNull()
                    if (number == null || number !in 0..MAX_CHANNEL) wrong = rest[1] else channel = number
                } else if (isWord(word, labels.argTo)) {
                    destination = findNode(env.nodes(), rest[1])
                    if (destination == null) wrong = rest[1]
                } else {
                    break
                }
                rest = rest.drop(2)
            }
            val text = rest.joinToString(" ")
            when {
                wrong != null -> missing(wrong)
                text.isBlank() -> bad(labels.send)
                else -> done(device.send(text, channel, destination))
            }
        },
        guarded(labels.reboot) { device.reboot() },
        guarded(labels.shutdown) { device.shutdown() },
        guarded(labels.factory) { device.factoryReset() },
        ConsoleCommand(
            labels.resetNodes.name,
            labels.resetNodes.summary,
            labels.resetNodes.usage,
            complete = { args -> if (args.isEmpty()) listOf(labels.argConfirm) else listOf(labels.argKeep) },
        ) { args ->
            if (isWord(args.firstOrNull(), labels.argConfirm)) {
                done(device.resetNodes(isWord(args.getOrNull(1), labels.argKeep)))
            } else {
                confirmNeeded(labels.resetNodes)
            }
        },
        nodeCommand(labels.removeNode) { device.removeNode(it) },
        switchCommand(labels.favorite) { node, enabled -> device.setFavorite(node, enabled) },
        switchCommand(labels.ignored) { node, enabled -> device.setIgnored(node, enabled) },
        nodeCommand(labels.traceroute) { device.traceroute(it) },
        nodeCommand(labels.requestPosition) { device.requestPosition(it) },
        ConsoleCommand(
            labels.telemetry.name,
            labels.telemetry.summary,
            labels.telemetry.usage,
            complete = { args ->
                when (args.size) {
                    0 -> nodeNames()
                    1 -> labels.telemetryTypes
                    else -> emptyList()
                }
            },
        ) { args ->
            val text = args.firstOrNull()
            val node = text?.let { findNode(env.nodes(), it) }
            val typeWord = args.getOrNull(1)
            val index = if (typeWord == null) 0 else labels.telemetryTypes.indexOfFirst { isWord(typeWord, it) }
            val type = TelemetryType.entries.getOrNull(index)
            when {
                text == null || type == null -> bad(labels.telemetry)
                node == null -> missing(text)
                else -> done(device.telemetry(node, type))
            }
        },
        ConsoleCommand(labels.owner.name, labels.owner.summary, labels.owner.usage) { args ->
            val parts = args.joinToString(" ").split(';')
            val longName = parts.first().trim()
            val shortName = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
            if (longName.isEmpty()) bad(labels.owner) else done(device.setOwner(longName, shortName))
        },
        ConsoleCommand(labels.time.name, labels.time.summary, labels.time.usage) { done(device.setTime()) },
        ConsoleCommand(labels.ringtone.name, labels.ringtone.summary, labels.ringtone.usage) { args ->
            done(device.ringtone(args.joinToString(" ").trim().ifEmpty { null }))
        },
        ConsoleCommand(labels.canned.name, labels.canned.summary, labels.canned.usage) { args ->
            val text = args.joinToString(" ").trim()
            if (text.isEmpty()) bad(labels.canned) else done(device.canned(text))
        },
        ConsoleCommand(
            labels.gpio.name,
            labels.gpio.summary,
            labels.gpio.usage,
            complete = { args -> if (args.isEmpty()) listOf(labels.argRead, labels.argWrite) else emptyList() },
        ) { args ->
            val mask = args.getOrNull(1)?.let { parseNumber(it) }
            when {
                isWord(args.firstOrNull(), labels.argRead) && args.size == 2 && mask != null -> {
                    val value = device.gpioRead(mask)
                    if (value == null) {
                        ConsoleResult(listOf(getStringSuspend(Res.string.console_out_no_answer)))
                    } else {
                        ConsoleResult(listOf(getStringSuspend(Res.string.console_out_gpio_value, args[1], value.toString())))
                    }
                }
                isWord(args.firstOrNull(), labels.argWrite) && args.size == 3 && mask != null -> {
                    val value = parseNumber(args[2])
                    if (value == null) bad(labels.gpio) else done(device.gpioWrite(mask, value))
                }
                else -> bad(labels.gpio)
            }
        },
    )
}

/** A number written in decimal or as `0x…`. */
private fun parseNumber(text: String): Long? = if (text.startsWith("0x", ignoreCase = true)) {
    text.drop(2).toLongOrNull(16)
} else {
    text.toLongOrNull()
}
