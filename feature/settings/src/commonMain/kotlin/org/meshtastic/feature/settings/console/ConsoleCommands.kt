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
@file:Suppress("LongMethod", "CyclomaticComplexMethod", "LongParameterList")

package org.meshtastic.feature.settings.console

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.effectiveLongName
import org.meshtastic.core.repository.RadioBridgeConfig
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.console_arg_off
import org.meshtastic.core.resources.console_arg_on
import org.meshtastic.core.resources.console_cmd_bridge_name
import org.meshtastic.core.resources.console_cmd_bridge_summary
import org.meshtastic.core.resources.console_cmd_bridge_usage
import org.meshtastic.core.resources.console_cmd_clear_name
import org.meshtastic.core.resources.console_cmd_clear_summary
import org.meshtastic.core.resources.console_cmd_clear_usage
import org.meshtastic.core.resources.console_cmd_disconnect_name
import org.meshtastic.core.resources.console_cmd_disconnect_summary
import org.meshtastic.core.resources.console_cmd_disconnect_usage
import org.meshtastic.core.resources.console_cmd_help_name
import org.meshtastic.core.resources.console_cmd_help_summary
import org.meshtastic.core.resources.console_cmd_help_usage
import org.meshtastic.core.resources.console_cmd_nodes_name
import org.meshtastic.core.resources.console_cmd_nodes_summary
import org.meshtastic.core.resources.console_cmd_nodes_usage
import org.meshtastic.core.resources.console_cmd_position_name
import org.meshtastic.core.resources.console_cmd_position_summary
import org.meshtastic.core.resources.console_cmd_position_usage
import org.meshtastic.core.resources.console_cmd_radio_name
import org.meshtastic.core.resources.console_cmd_radio_summary
import org.meshtastic.core.resources.console_cmd_radio_usage
import org.meshtastic.core.resources.console_cmd_radios_name
import org.meshtastic.core.resources.console_cmd_radios_summary
import org.meshtastic.core.resources.console_cmd_radios_usage
import org.meshtastic.core.resources.console_cmd_remove_name
import org.meshtastic.core.resources.console_cmd_remove_summary
import org.meshtastic.core.resources.console_cmd_remove_usage
import org.meshtastic.core.resources.console_cmd_status_name
import org.meshtastic.core.resources.console_cmd_status_summary
import org.meshtastic.core.resources.console_cmd_status_usage
import org.meshtastic.core.resources.console_out_bridge_changed
import org.meshtastic.core.resources.console_out_bridge_none
import org.meshtastic.core.resources.console_out_bridge_off
import org.meshtastic.core.resources.console_out_bridge_on
import org.meshtastic.core.resources.console_out_bridge_unselected
import org.meshtastic.core.resources.console_out_disconnecting
import org.meshtastic.core.resources.console_out_help_header
import org.meshtastic.core.resources.console_out_help_usage
import org.meshtastic.core.resources.console_out_no_radios
import org.meshtastic.core.resources.console_out_node_offline
import org.meshtastic.core.resources.console_out_node_online
import org.meshtastic.core.resources.console_out_nodes_more
import org.meshtastic.core.resources.console_out_nodes_none
import org.meshtastic.core.resources.console_out_position_bad
import org.meshtastic.core.resources.console_out_position_no_device
import org.meshtastic.core.resources.console_out_position_set
import org.meshtastic.core.resources.console_out_radio_line
import org.meshtastic.core.resources.console_out_radio_missing
import org.meshtastic.core.resources.console_out_radio_switching
import org.meshtastic.core.resources.console_out_removed
import org.meshtastic.core.resources.console_out_selected
import org.meshtastic.core.resources.console_out_state_connected
import org.meshtastic.core.resources.console_out_state_connecting
import org.meshtastic.core.resources.console_out_state_disconnected
import org.meshtastic.core.resources.console_out_state_sleep
import org.meshtastic.core.resources.console_out_status_bridge
import org.meshtastic.core.resources.console_out_status_bridge_none
import org.meshtastic.core.resources.console_out_status_bridge_unselected
import org.meshtastic.core.resources.console_out_status_connection
import org.meshtastic.core.resources.console_out_status_nodes
import org.meshtastic.core.resources.console_out_status_radio
import org.meshtastic.core.resources.console_out_unknown
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.ui.viewmodel.RadioChoice
import org.meshtastic.core.ui.viewmodel.RadioStatus
import org.meshtastic.feature.settings.mtsw.RadioBridgeState
import kotlin.math.roundToInt

/** What the console can look at and do; the screen fills it with the live state and the actions of the app. */
class ConsoleEnvironment(
    val radios: () -> List<RadioChoice>,
    val choose: (Int) -> Unit,
    val disconnect: (Int?) -> Unit,
    val remove: (Int) -> Unit,
    val connection: () -> ConnectionState,
    val nodes: () -> List<Node>,
    val bridge: () -> RadioBridgeState,
    val updateBridge: ((RadioBridgeConfig) -> RadioBridgeConfig) -> Unit,
    val setPosition: suspend (latitude: Double, longitude: Double, altitude: Int?) -> Boolean,
    val device: ConsoleDevice,
    /** Repeats a typed line every given number of seconds; 0 seconds stops it. */
    val loop: (seconds: Int, line: String) -> Unit,
)

/** The name, the description and the way of writing one command, in the language of the app. */
class ConsoleLabel(val name: String, val summary: String, val usage: String)

/** The labels of every command and of the on/off words. */
class ConsoleLabels(
    val help: ConsoleLabel,
    val clear: ConsoleLabel,
    val radios: ConsoleLabel,
    val radio: ConsoleLabel,
    val disconnect: ConsoleLabel,
    val remove: ConsoleLabel,
    val status: ConsoleLabel,
    val nodes: ConsoleLabel,
    val bridge: ConsoleLabel,
    val position: ConsoleLabel,
    val device: ConsoleDeviceLabels,
    val on: String,
    val off: String,
)

@Composable
private fun label(name: StringResource, summary: StringResource, usage: StringResource) =
    ConsoleLabel(stringResource(name), stringResource(summary), stringResource(usage))

@Composable
fun rememberConsoleLabels(): ConsoleLabels {
    val help =
        label(
            Res.string.console_cmd_help_name,
            Res.string.console_cmd_help_summary,
            Res.string.console_cmd_help_usage,
        )
    val clear =
        label(
            Res.string.console_cmd_clear_name,
            Res.string.console_cmd_clear_summary,
            Res.string.console_cmd_clear_usage,
        )
    val radios =
        label(
            Res.string.console_cmd_radios_name,
            Res.string.console_cmd_radios_summary,
            Res.string.console_cmd_radios_usage,
        )
    val radio =
        label(
            Res.string.console_cmd_radio_name,
            Res.string.console_cmd_radio_summary,
            Res.string.console_cmd_radio_usage,
        )
    val disconnect =
        label(
            Res.string.console_cmd_disconnect_name,
            Res.string.console_cmd_disconnect_summary,
            Res.string.console_cmd_disconnect_usage,
        )
    val remove =
        label(
            Res.string.console_cmd_remove_name,
            Res.string.console_cmd_remove_summary,
            Res.string.console_cmd_remove_usage,
        )
    val status =
        label(
            Res.string.console_cmd_status_name,
            Res.string.console_cmd_status_summary,
            Res.string.console_cmd_status_usage,
        )
    val nodes =
        label(
            Res.string.console_cmd_nodes_name,
            Res.string.console_cmd_nodes_summary,
            Res.string.console_cmd_nodes_usage,
        )
    val bridge =
        label(
            Res.string.console_cmd_bridge_name,
            Res.string.console_cmd_bridge_summary,
            Res.string.console_cmd_bridge_usage,
        )
    val position =
        label(
            Res.string.console_cmd_position_name,
            Res.string.console_cmd_position_summary,
            Res.string.console_cmd_position_usage,
        )
    val device = rememberConsoleDeviceLabels()
    val on = stringResource(Res.string.console_arg_on)
    val off = stringResource(Res.string.console_arg_off)
    return remember(help, clear, radios, radio, disconnect, remove, status, nodes, bridge, position, device, on, off) {
        ConsoleLabels(help, clear, radios, radio, disconnect, remove, status, nodes, bridge, position, device, on, off)
    }
}

private const val MAX_NODE_LINES = 20
private const val POSITION_MIN_ARGS = 2
private const val POSITION_MAX_ARGS = 3
private const val MAX_LATITUDE = 90.0
private const val MAX_LONGITUDE = 180.0

/** Builds the one registry of commands; every text comes from the string resources, so it follows the app language. */
fun buildConsoleRegistry(env: ConsoleEnvironment, labels: ConsoleLabels): ConsoleRegistry {
    lateinit var registry: ConsoleRegistry

    suspend fun stateText(status: RadioStatus): String = getStringSuspend(
        when (status) {
            RadioStatus.Connected -> Res.string.console_out_state_connected
            RadioStatus.Connecting -> Res.string.console_out_state_connecting
            RadioStatus.Disconnected -> Res.string.console_out_state_disconnected
        },
    )

    suspend fun connectionText(state: ConnectionState): String = getStringSuspend(
        when (state) {
            is ConnectionState.Connected -> Res.string.console_out_state_connected
            is ConnectionState.Connecting -> Res.string.console_out_state_connecting
            is ConnectionState.DeviceSleep -> Res.string.console_out_state_sleep
            else -> Res.string.console_out_state_disconnected
        },
    )

    /** The real reason there is no bridge to show: no saved device at all, or none of them chosen yet. */
    fun noDeviceText(state: RadioBridgeState) = if (state.hasSavedDevices) {
        Res.string.console_out_bridge_unselected
    } else {
        Res.string.console_out_bridge_none
    }

    /** A short state for the status line; the full sentence of [noDeviceText] belongs only to the bridge command. */
    suspend fun bridgeText(state: RadioBridgeState): String {
        val slotId =
            state.slotId
                ?: return getStringSuspend(
                    if (state.hasSavedDevices) {
                        Res.string.console_out_status_bridge_unselected
                    } else {
                        Res.string.console_out_status_bridge_none
                    },
                )
        return if (state.config.enabled) {
            getStringSuspend(Res.string.console_out_bridge_on, state.config.portFor(slotId))
        } else {
            getStringSuspend(Res.string.console_out_bridge_off)
        }
    }

    fun radioNamed(id: String?): RadioChoice? =
        id?.toIntOrNull()?.let { number -> env.radios().firstOrNull { it.id == number } }

    fun radioNumbers() = env.radios().map { it.id.toString() }

    val commands =
        listOf(
            ConsoleCommand(
                labels.help.name,
                labels.help.summary,
                labels.help.usage,
                complete = { args ->
                    if (args.isEmpty()) registry.commands.map { it.name } else emptyList()
                },
            ) { args ->
                val one = args.firstOrNull()?.let { registry.find(it) }
                if (one != null) {
                    ConsoleResult(listOf(one.summary, getStringSuspend(Res.string.console_out_help_usage, one.usage)))
                } else {
                    ConsoleResult(
                        listOf(getStringSuspend(Res.string.console_out_help_header)) +
                            registry.commands.map { "  ${it.usage} – ${it.summary}" },
                    )
                }
            },
            ConsoleCommand(labels.clear.name, labels.clear.summary, labels.clear.usage) { ConsoleResult(clear = true) },
            ConsoleCommand(labels.radios.name, labels.radios.summary, labels.radios.usage) {
                val radios = env.radios()
                if (radios.isEmpty()) {
                    ConsoleResult(listOf(getStringSuspend(Res.string.console_out_no_radios)))
                } else {
                    ConsoleResult(
                        radios.map { radio ->
                            getStringSuspend(
                                Res.string.console_out_radio_line,
                                radio.id,
                                radio.name?.takeIf { name -> name.isNotBlank() } ?: radio.address,
                                stateText(radio.status),
                                if (radio.selected) getStringSuspend(Res.string.console_out_selected) else "",
                            )
                        },
                    )
                }
            },
            ConsoleCommand(
                labels.radio.name,
                labels.radio.summary,
                labels.radio.usage,
                complete = { args ->
                    if (args.isEmpty()) radioNumbers() else emptyList()
                },
            ) { args ->
                val radio = radioNamed(args.firstOrNull())
                if (radio == null) {
                    val missing = getStringSuspend(Res.string.console_out_radio_missing, args.firstOrNull().orEmpty())
                    ConsoleResult(listOf(missing))
                } else {
                    env.choose(radio.id)
                    ConsoleResult(
                        listOf(
                            getStringSuspend(
                                Res.string.console_out_radio_switching,
                                radio.id,
                                radio.name?.takeIf { it.isNotBlank() } ?: radio.address,
                            ),
                        ),
                    )
                }
            },
            ConsoleCommand(
                labels.disconnect.name,
                labels.disconnect.summary,
                labels.disconnect.usage,
                complete = { args ->
                    if (args.isEmpty()) radioNumbers() else emptyList()
                },
            ) { args ->
                val given = args.firstOrNull()
                val radio = given?.let { radioNamed(it) }
                if (given != null && radio == null) {
                    ConsoleResult(listOf(getStringSuspend(Res.string.console_out_radio_missing, given)))
                } else {
                    env.disconnect(radio?.id)
                    ConsoleResult(listOf(getStringSuspend(Res.string.console_out_disconnecting)))
                }
            },
            ConsoleCommand(
                labels.remove.name,
                labels.remove.summary,
                labels.remove.usage,
                complete = { args ->
                    if (args.isEmpty()) radioNumbers() else emptyList()
                },
            ) { args ->
                val radio = radioNamed(args.firstOrNull())
                if (radio == null) {
                    val missing = getStringSuspend(Res.string.console_out_radio_missing, args.firstOrNull().orEmpty())
                    ConsoleResult(listOf(missing))
                } else {
                    env.remove(radio.id)
                    ConsoleResult(listOf(getStringSuspend(Res.string.console_out_removed, radio.id)))
                }
            },
            ConsoleCommand(labels.status.name, labels.status.summary, labels.status.usage) {
                val chosen = env.radios().firstOrNull { radio -> radio.selected }
                val nodes = env.nodes()
                ConsoleResult(
                    listOfNotNull(
                        getStringSuspend(Res.string.console_out_status_connection, connectionText(env.connection())),
                        chosen?.let { radio ->
                            getStringSuspend(
                                Res.string.console_out_status_radio,
                                radio.name?.takeIf { name -> name.isNotBlank() } ?: radio.address,
                            )
                        },
                        getStringSuspend(
                            Res.string.console_out_status_nodes,
                            nodes.size,
                            nodes.count { node -> node.isOnline },
                        ),
                        getStringSuspend(Res.string.console_out_status_bridge, bridgeText(env.bridge())),
                    ),
                )
            },
            ConsoleCommand(labels.nodes.name, labels.nodes.summary, labels.nodes.usage) { args ->
                val key = ConsoleText.fold(args.joinToString(" "))
                val found =
                    env.nodes()
                        .filter { node ->
                            key.isEmpty() ||
                                ConsoleText.fold(node.effectiveLongName).contains(key) ||
                                ConsoleText.fold(node.user.short_name).contains(key) ||
                                ConsoleText.fold(node.user.id).contains(key)
                        }
                        .sortedByDescending { it.lastHeard }
                if (found.isEmpty()) {
                    ConsoleResult(listOf(getStringSuspend(Res.string.console_out_nodes_none)))
                } else {
                    val lines =
                        found.take(MAX_NODE_LINES).map { node ->
                            val state =
                                getStringSuspend(
                                    if (node.isOnline) {
                                        Res.string.console_out_node_online
                                    } else {
                                        Res.string.console_out_node_offline
                                    },
                                )
                            "• ${node.effectiveLongName} (${node.user.short_name}) ${node.user.id} – $state"
                        }
                    val more =
                        if (found.size > MAX_NODE_LINES) {
                            listOf(getStringSuspend(Res.string.console_out_nodes_more, found.size - MAX_NODE_LINES))
                        } else {
                            emptyList()
                        }
                    ConsoleResult(lines + more)
                }
            },
            ConsoleCommand(
                labels.bridge.name,
                labels.bridge.summary,
                labels.bridge.usage,
                complete = { args ->
                    if (args.isEmpty()) listOf(labels.on, labels.off) else emptyList()
                },
            ) { args ->
                val state = env.bridge()
                val word = args.firstOrNull()?.let { ConsoleText.fold(it) }
                val change =
                    when (word) {
                        ConsoleText.fold(labels.on) -> true
                        ConsoleText.fold(labels.off) -> false
                        else -> null
                    }
                if (state.slotId == null) {
                    ConsoleResult(listOf(getStringSuspend(noDeviceText(state))))
                } else if (change == null) {
                    ConsoleResult(listOf(getStringSuspend(Res.string.console_out_status_bridge, bridgeText(state))))
                } else {
                    env.updateBridge { it.copy(enabled = change) }
                    val text = bridgeText(state.copy(config = state.config.copy(enabled = change)))
                    ConsoleResult(listOf(getStringSuspend(Res.string.console_out_bridge_changed, text)))
                }
            },
            ConsoleCommand(labels.position.name, labels.position.summary, labels.position.usage) { args ->
                val numbers = args.map { it.replace(',', '.').toDoubleOrNull() }
                val latitude = numbers.getOrNull(0)
                val longitude = numbers.getOrNull(1)
                val altitude = numbers.getOrNull(2)
                val valid =
                    args.size in POSITION_MIN_ARGS..POSITION_MAX_ARGS &&
                        numbers.none { it == null } &&
                        latitude != null &&
                        latitude in -MAX_LATITUDE..MAX_LATITUDE &&
                        longitude != null &&
                        longitude in -MAX_LONGITUDE..MAX_LONGITUDE
                if (!valid || latitude == null || longitude == null) {
                    ConsoleResult(listOf(getStringSuspend(Res.string.console_out_position_bad, labels.position.usage)))
                } else if (env.setPosition(latitude, longitude, altitude?.roundToInt())) {
                    ConsoleResult(listOf(getStringSuspend(Res.string.console_out_position_set, args[0], args[1])))
                } else {
                    ConsoleResult(listOf(getStringSuspend(Res.string.console_out_position_no_device)))
                }
            },
        )
    registry =
        ConsoleRegistry(commands + buildDeviceCommands(env, labels.device, labels.on, labels.off)) {
            getStringSuspend(Res.string.console_out_unknown, it)
        }
    return registry
}
