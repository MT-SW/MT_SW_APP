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
package org.meshtastic.core.data.manager

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Single
import org.meshtastic.core.common.di.ServiceScope
import org.meshtastic.core.model.ContactKey
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.PhoneLocationProvider
import org.meshtastic.core.repository.SosConfig
import org.meshtastic.core.repository.SosController
import org.meshtastic.core.repository.SosState
import org.meshtastic.core.repository.usecase.SendMessageOutcome
import org.meshtastic.core.repository.usecase.SendMessageUseCase
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.resources.sos_default_message
import kotlin.math.roundToLong

/**
 * Sends the SOS message to every chosen channel and as a direct message to every chosen node, with the position when
 * wanted, and watches the delivery status until every send is confirmed, refused or too long has passed. A chosen node
 * is written to without any check: one that is marked as not monitored or not messageable is no exception, because an
 * alarm must not be held back by a setting. With repeating on it sends again at the chosen interval. It runs in the
 * service scope, so closing the SOS screen does not stop it; only [cancel] does.
 */
@Single
class SosControllerImpl(
    private val sendMessage: SendMessageUseCase,
    private val packets: PacketRepository,
    private val nodes: NodeRepository,
    private val phone: PhoneLocationProvider,
    private val scope: ServiceScope,
) : SosController {
    private val mutableState = MutableStateFlow(SosState())
    override val state: StateFlow<SosState> = mutableState.asStateFlow()
    private var job: Job? = null

    override fun start(config: SosConfig) {
        if (job?.isActive == true) return
        job = scope.launch { run(config) }
    }

    override fun cancel() {
        job?.cancel()
        job = null
        mutableState.update { it.copy(running = false) }
    }

    private suspend fun run(config: SosConfig) {
        val total = if (config.repeatMinutes > 0) config.repeatCount.coerceAtLeast(1) else 1
        mutableState.value = SosState(running = true, total = total)
        var delivered = false
        try {
            for (attempt in 1..total) {
                val position = if (config.includePosition) currentPosition() else null
                mutableState.update {
                    it.copy(attempt = attempt, status = MessageStatus.QUEUED, withPosition = position != null)
                }
                val text = textFor(config, position)
                val queued =
                    targetKeys(config).mapNotNull { key ->
                        (sendMessage(text, key) as? SendMessageOutcome.Queued)?.packetId
                    }
                if (queued.isEmpty()) {
                    mutableState.update { it.copy(running = false, refused = true) }
                    return
                }
                if (waitForDelivery(queued)) delivered = true
                if (delivered && config.stopWhenDelivered) break
                if (attempt < total) delay(config.repeatMinutes * MILLIS_PER_MINUTE)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(e) { "SOS failed" }
        } finally {
            mutableState.update { it.copy(running = false, delivered = delivered) }
        }
    }

    /**
     * The conversation key of every send: a broadcast on each chosen channel, a direct message to each chosen node. A
     * direct message goes on the shared key slot when both radios have a public key, else on the channel the node was
     * heard on, exactly as the chat does.
     */
    private fun targetKeys(config: SosConfig): List<String> {
        val db = nodes.nodeDBbyNum.value
        val ourPkc = nodes.ourNodeInfo.value?.hasPKC == true
        // With nothing chosen at all the main channel is used, so the message never goes nowhere.
        val channels = if (config.channels.isEmpty() && config.nodes.isEmpty()) listOf(0) else config.channels
        val channelKeys = channels.map { ContactKey.broadcast(it).value }
        val nodeKeys =
            config.nodes.map { num ->
                val node = db[num]
                val slot = if (ourPkc && node?.hasPKC == true) NodeAddress.PKC_CHANNEL_INDEX else node?.channel ?: 0
                NodeAddress.ByNum(num).toContactKey(slot).value
            }
        return channelKeys + nodeKeys
    }

    /** True when the radio confirmed every send within the waiting time. */
    @Suppress("ReturnCount")
    private suspend fun waitForDelivery(packetIds: List<Int>): Boolean {
        var waited = 0L
        while (waited < DELIVERY_WAIT_MS) {
            val statuses = packetIds.map { packets.getPacketByPacketId(it)?.status }
            val known = statuses.filterNotNull()
            // The least advanced send is the one worth showing; once all are confirmed, the last one.
            (known.firstOrNull { !it.isConfirmed() } ?: known.lastOrNull())?.let { shown ->
                mutableState.update { it.copy(status = shown) }
            }
            if (statuses.all { it?.isConfirmed() == true }) return true
            // Nothing more will happen when every send is either confirmed or failed.
            if (statuses.all { it == MessageStatus.ERROR || it?.isConfirmed() == true }) return false
            delay(POLL_MS)
            waited += POLL_MS
        }
        return false
    }

    private fun MessageStatus.isConfirmed(): Boolean =
        this == MessageStatus.DELIVERED || this == MessageStatus.SFPP_ROUTING || this == MessageStatus.SFPP_CONFIRMED

    /** The phone's fix when it comes quickly, otherwise the position our own node reports; null when neither. */
    @Suppress("ReturnCount")
    private suspend fun currentPosition(): Pair<Double, Double>? {
        val fix =
            withTimeoutOrNull(LOCATION_WAIT_MS) { phone.locationUpdates().first { !it.permissionGranted || it.hasFix } }
                ?.location
        if (fix != null) return fix.latitude to fix.longitude
        val node = nodes.ourNodeInfo.value ?: return null
        return if (node.validPosition != null) node.latitude to node.longitude else null
    }

    private suspend fun textFor(config: SosConfig, position: Pair<Double, Double>?): String {
        val message = config.message.ifBlank { getStringSuspend(Res.string.sos_default_message) }
        val body =
            if (position == null) {
                message
            } else {
                val lat = rounded(position.first)
                val lon = rounded(position.second)
                "$message $lat,$lon https://maps.google.com/?q=$lat,$lon"
            }
        // The alarm bell, written like the bell button of the chat does: a radio with the external notification module
        // sounds its alert on the bell character, and other apps show the bell.
        return "$ALERT_BELL $body $ALERT_BELL_CHAR"
    }

    private fun rounded(value: Double): Double = (value * COORDINATE_SCALE).roundToLong() / COORDINATE_SCALE

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val POLL_MS = 2_000L
        const val DELIVERY_WAIT_MS = 90_000L
        const val LOCATION_WAIT_MS = 4_000L
        const val COORDINATE_SCALE = 100_000.0
        const val ALERT_BELL = "🔔"
        const val ALERT_BELL_CHAR = '\u0007'
    }
}
