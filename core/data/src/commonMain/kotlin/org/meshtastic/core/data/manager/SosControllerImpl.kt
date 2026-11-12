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
 * Sends the SOS message to the chosen channel, with the position when wanted, and watches the delivery status until it
 * is confirmed, refused or too long has passed. With repeating on it sends again at the chosen interval. It runs in the
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
                val outcome = sendMessage(textFor(config, position), ContactKey.broadcast(config.channel).value)
                if (outcome !is SendMessageOutcome.Queued) {
                    mutableState.update { it.copy(running = false, refused = true) }
                    return
                }
                if (waitForDelivery(outcome.packetId)) delivered = true
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

    /** True when the radio confirmed the send within the waiting time. */
    @Suppress("ReturnCount")
    private suspend fun waitForDelivery(packetId: Int): Boolean {
        var waited = 0L
        while (waited < DELIVERY_WAIT_MS) {
            val status = packets.getPacketByPacketId(packetId)?.status
            if (status != null) mutableState.update { it.copy(status = status) }
            when (status) {
                MessageStatus.DELIVERED,
                MessageStatus.SFPP_ROUTING,
                MessageStatus.SFPP_CONFIRMED,
                -> return true

                MessageStatus.ERROR -> return false

                else -> Unit
            }
            delay(POLL_MS)
            waited += POLL_MS
        }
        return false
    }

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
