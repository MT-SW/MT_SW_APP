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
package org.meshtastic.feature.settings.console

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.HamName
import org.meshtastic.core.model.Message
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.Position
import org.meshtastic.core.model.TelemetryType
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.repository.RadioController
import org.meshtastic.core.repository.RadioPrefs
import org.meshtastic.core.repository.RadioSlotsPrefs
import org.meshtastic.core.repository.ServiceRepository
import org.meshtastic.core.repository.usecase.SendMessageUseCase
import org.meshtastic.proto.Channel
import org.meshtastic.proto.ChannelSettings
import org.meshtastic.proto.HamParameters
import org.meshtastic.proto.LocalConfig
import org.meshtastic.proto.LocalModuleConfig

/** What the console screen shows and remembers: the printed lines, the typed line and the earlier lines. */
@Suppress("LongParameterList", "TooManyFunctions")
@KoinViewModel
class ConsoleViewModel(
    private val nodeRepository: NodeRepository,
    private val serviceRepository: ServiceRepository,
    private val radioController: RadioController,
    private val sendMessageUseCase: SendMessageUseCase,
    private val radioConfigRepository: RadioConfigRepository,
    private val packetRepository: PacketRepository,
    private val radioPrefs: RadioPrefs,
    private val slotsPrefs: RadioSlotsPrefs,
) : ViewModel(),
    ConsoleDevice {
    private val history = ConsoleHistory()
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    fun nodes(): List<Node> = nodeRepository.nodeDBbyNum.value.values.toList()

    fun connection(): ConnectionState = serviceRepository.connectionState.value

    /** Sets the fixed position of the connected device; false when no device is connected yet. */
    suspend fun setPosition(latitude: Double, longitude: Double, altitude: Int?): Boolean = onDevice { num ->
        radioController.setFixedPosition(num, Position(latitude, longitude, altitude))
    }

    /** Runs [block] with the number of the connected device; false when none is connected or the call failed. */
    private suspend fun onDevice(block: suspend (Int) -> Unit): Boolean {
        val num = nodeRepository.myNodeInfo.value?.myNodeNum ?: return false
        return try {
            block(num)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(e) { "Console command failed" }
            false
        }
    }

    private fun packetId() = radioController.generatePacketId()

    override fun ownNode(): Node? = nodeRepository.ourNodeInfo.value

    override suspend fun send(text: String, channel: Int, destination: Node?): Boolean = onDevice {
        sendMessageUseCase(text, "$channel${destination?.user?.id ?: NodeAddress.ID_BROADCAST}")
    }

    override suspend fun reboot() = onDevice { radioController.reboot(it, packetId()) }

    override suspend fun shutdown() = onDevice { radioController.shutdown(it, packetId()) }

    override suspend fun factoryReset() = onDevice { radioController.factoryReset(it, packetId()) }

    override suspend fun resetNodes(keepFavorites: Boolean) = onDevice {
        radioController.nodedbReset(it, packetId(), keepFavorites)
    }

    override suspend fun removeNode(node: Node) = onDevice { radioController.removeByNodenum(packetId(), node.num) }

    override suspend fun setFavorite(node: Node, on: Boolean) = onDevice { radioController.setFavorite(node.num, on) }

    override suspend fun setIgnored(node: Node, on: Boolean) = onDevice { radioController.setIgnored(node.num, on) }

    override suspend fun traceroute(node: Node) = onDevice { radioController.requestTraceroute(packetId(), node.num) }

    override suspend fun requestPosition(node: Node) = onDevice {
        radioController.requestPosition(node.num, Position(0.0, 0.0, 0), packetId())
    }

    override suspend fun telemetry(node: Node, type: TelemetryType) = onDevice {
        radioController.requestTelemetry(packetId(), node.num, type.ordinal)
    }

    override suspend fun setOwner(longName: String, shortName: String?): Boolean {
        val own = nodeRepository.ourNodeInfo.value ?: return false
        val user =
            own.user
                .newBuilder()
                .also { b ->
                    b.long_name = longName
                    b.short_name = shortName ?: own.user.short_name
                }
                .build()
        return onDevice { radioController.setOwner(it, user, packetId()) }
    }

    override suspend fun setTime() = onDevice { radioController.setTime(it, packetId()) }

    override suspend fun ringtone(rtttl: String?) = onDevice {
        if (rtttl == null) radioController.playRingtone(it) else radioController.setRingtone(it, rtttl)
    }

    override suspend fun canned(messages: String) = onDevice { radioController.setCannedMessages(it, messages) }

    override suspend fun gpioRead(mask: Long): Long? =
        nodeRepository.myNodeInfo.value?.myNodeNum?.let { radioController.readGpio(it, mask) }

    override suspend fun settings(): Pair<LocalConfig, LocalModuleConfig>? {
        if (nodeRepository.myNodeInfo.value == null) return null
        return radioConfigRepository.localConfigFlow.first() to radioConfigRepository.moduleConfigFlow.first()
    }

    override suspend fun apply(changes: List<ConsoleChange>) = onDevice { num ->
        radioController.editSettings(num) {
            changes.forEach { change ->
                when (change) {
                    is ConsoleChange.Cfg -> setConfig(change.config)
                    is ConsoleChange.Module -> setModuleConfig(change.config)
                }
            }
        }
    }

    override suspend fun channels(): List<ChannelSettings> = if (nodeRepository.myNodeInfo.value == null) {
        emptyList()
    } else {
        radioConfigRepository.channelSetFlow.first().settings
    }

    override suspend fun setChannel(index: Int, settings: ChannelSettings, enabled: Boolean) = onDevice {
        val role =
            when {
                index == 0 -> Channel.Role.PRIMARY
                enabled -> Channel.Role.SECONDARY
                else -> Channel.Role.DISABLED
            }
        radioController.setLocalChannel(
            Channel.Builder()
                .also { b ->
                    b.index = index
                    b.role = role
                    b.settings = settings
                }
                .build(),
        )
    }

    override suspend fun setHam(callSign: String, longName: String?): Boolean {
        val own = nodeRepository.ourNodeInfo.value ?: return false
        val params =
            HamParameters.Builder()
                .also { b ->
                    b.call_sign = callSign
                    b.short_name = own.user.short_name
                    b.long_name = longName ?: HamName.split(own.user.long_name).second
                }
                .build()
        return onDevice { radioController.setHamMode(it, params, packetId()) }
    }

    override suspend fun recentMessages(contactKey: String, limit: Int): List<Message> = packetRepository
        .getMessagesFrom(contactKey, limit) { id -> nodeRepository.getNode(id.orEmpty()) }
        .first()
        .sortedBy { it.receivedTime }
        .takeLast(limit)

    override suspend fun search(text: String): List<Message> =
        packetRepository.searchMessages(text) { id -> nodeRepository.getNode(id.orEmpty()) }.first()

    override suspend fun setMuted(node: Node, muted: Boolean) = onDevice {
        if (node.isMuted != muted) radioController.toggleMuted(node.num)
    }

    override suspend fun connectInBackground(address: String): Boolean {
        val full =
            when {
                address.length == MAC_LENGTH && address.count { it == ':' } == MAC_COLONS -> "x$address"
                address.length > 1 && address[0] == 't' && address[1].isDigit() -> address
                else -> "t$address"
            }
        val shown = radioPrefs.devAddr.value
        val id =
            slotsPrefs.slots.value.firstOrNull { it.address == full }?.id ?: slotsPrefs.put(full, null) ?: return false
        slotsPrefs.setOff(id, false)
        if (shown.isNullOrBlank() || shown == NO_DEVICE) {
            slotsPrefs.select(id)
            radioController.setDeviceAddress(full)
        }
        return true
    }

    private var loopJob: Job? = null

    /** Repeats [line] with [registry] every [seconds]; 0 seconds stops it. */
    fun loop(seconds: Int, line: String, registry: ConsoleRegistry?) {
        loopJob?.cancel()
        loopJob = null
        if (seconds <= 0 || registry == null) return
        loopJob = viewModelScope.launch {
            while (true) {
                val result = registry.execute(line)
                _lines.update { old -> (old + "> $line" + result.lines).takeLast(MAX_LINES) }
                delay(seconds * MILLIS_PER_SECOND)
            }
        }
    }

    override suspend fun gpioWrite(mask: Long, value: Long): Boolean =
        nodeRepository.myNodeInfo.value?.myNodeNum?.let { radioController.writeGpio(it, mask, value) } ?: false

    fun setInput(text: String) {
        _input.value = text
    }

    fun previous() {
        history.previous()?.let { _input.value = it }
    }

    fun next() {
        history.next()?.let { _input.value = it }
    }

    /** Runs the typed line with [registry] and prints the line and what the command answered. */
    fun run(registry: ConsoleRegistry) {
        val line = _input.value.trim()
        if (line.isEmpty()) return
        history.add(line)
        _input.value = ""
        viewModelScope.launch {
            val result = registry.execute(line)
            _lines.update { old ->
                val all = if (result.clear) result.lines else old + "> $line" + result.lines
                all.takeLast(MAX_LINES)
            }
        }
    }

    private companion object {
        const val MAX_LINES = 500
        const val MAC_LENGTH = 17
        const val MAC_COLONS = 5
        const val NO_DEVICE = "n"
        const val MILLIS_PER_SECOND = 1000L
    }
}
