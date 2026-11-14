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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Single
import org.meshtastic.core.common.di.ServiceScope
import org.meshtastic.core.common.util.nowInstant
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.common.util.systemTimeZone
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.repository.MeshNotificationManager
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.SosAck
import org.meshtastic.core.repository.SosAckEntry
import org.meshtastic.core.repository.SosAckMarker
import org.meshtastic.core.repository.SosAcks
import org.meshtastic.core.repository.SosAlarmCenter
import org.meshtastic.core.repository.SosAlarmPolicy
import org.meshtastic.core.repository.SosAlarmSounds
import org.meshtastic.core.repository.SosAlarmStyle
import org.meshtastic.core.repository.SosAlert
import org.meshtastic.core.repository.SosHistory
import org.meshtastic.core.repository.SosHistoryEntry
import org.meshtastic.core.repository.SosMarker
import org.meshtastic.core.repository.SosNotificationActions
import org.meshtastic.core.repository.SosPrefs
import org.meshtastic.core.repository.SosReply
import org.meshtastic.core.repository.usecase.SendMessageUseCase
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.resources.sos_ack_coming
import org.meshtastic.core.resources.sos_ack_help
import org.meshtastic.core.resources.sos_ack_seen
import org.meshtastic.core.resources.sos_reply_calling_help
import org.meshtastic.core.resources.sos_reply_coming
import org.meshtastic.core.resources.sos_reply_seen

/**
 * Rings the alarm for an SOS that arrived: the alert is shown on the whole screen, the platform's looping sound starts
 * (on Android the notification rings by itself, again and again, until it is cancelled) and everything stops when the
 * person confirms it. A newer SOS replaces the one on screen without stopping the sound. Confirming (or one of the
 * ready answers) sends the sender a direct message, so they know the SOS was seen.
 */
@Single
class SosAlarmCenterImpl(
    private val prefs: SosPrefs,
    private val history: SosHistory,
    private val acks: SosAcks,
    private val notifications: MeshNotificationManager,
    private val sendMessage: SendMessageUseCase,
    private val nodes: NodeRepository,
    private val scope: ServiceScope,
) : SosAlarmCenter {
    private val mutableAlert = MutableStateFlow<SosAlert?>(null)

    /** The sound that is going now (null when none); a silent alarm leaves it null. */
    private var sounding: SosAlarmStyle? = null

    /** When each node's last SOS came (for the "second SOS" option); kept in memory only. */
    private val lastSeen = mutableMapOf<Int, Long>()
    override val alert: StateFlow<SosAlert?> = mutableAlert.asStateFlow()

    init {
        // The buttons of the alarm notification (iPhone and iPad) reach this class through here.
        SosNotificationActions.current = this
    }

    override fun raise(alert: SosAlert) {
        val shown = alert.copy(text = SosMarker.clean(alert.text))
        // A test always rings, so the person can hear what the alarm is like; a real one follows the settings.
        val allowed =
            SosAlarmPolicy.rings(
                test = shown.test,
                receive = prefs.receiveAlarm.value,
                senderNum = shown.fromNodeNum,
                onlyChosen = prefs.alarmOnlyChosen.value,
                chosen = prefs.alarmNodes.value,
                quiet = prefs.quietHours.value,
                quietExceptions = prefs.quietExceptions.value,
                minuteOfDay = minuteOfDay(),
            )
        // Noted for every real SOS, also when it does not ring, so the next one counts as the second.
        val waits = !shown.test && waitsForSecond(shown)
        val rings = allowed && !waits
        if (!shown.test) record(shown, rings)
        if (!rings) {
            Logger.i { "SOS alarm: kept quiet (settings) for ${shown.senderName}" }
            return
        }
        val style =
            shown.style ?: SosAlarmPolicy.styleFor(shown.fromNodeNum, prefs.alarmNodes.value, prefs.alarmStyles.value)
        mutableAlert.value = shown
        Logger.i { "SOS alarm: raised from ${shown.senderName} test=${shown.test} style=$style" }
        // A silent alarm is only the window; the ordinary message notification stays. A louder sound replaces a
        // gentler one that is already going (a siren is never replaced by a bell).
        val louder =
            style != SosAlarmStyle.SILENT &&
                (sounding == null || (sounding == SosAlarmStyle.BELL && style == SosAlarmStyle.SIREN))
        if (!louder) return
        if (sounding != null) runCatching { SosAlarmSounds.current?.stop() }
        sounding = style
        val flash = prefs.alarmFlash.value && style == SosAlarmStyle.SIREN
        runCatching { SosAlarmSounds.current?.start(flash, style) }
        scope.launch { notifications.showSosAlarmNotification(shown.contactKey, shown.senderName, shown.text, style) }
    }

    /**
     * True when the option "alarm only after a second SOS" is on and this is the first one from that node in the chosen
     * time. Every real SOS is noted, so the next one from the same node counts as the second.
     */
    private fun waitsForSecond(alert: SosAlert): Boolean {
        val now = nowMillis
        val second = SosAlarmPolicy.isSecond(alert.fromNodeNum, lastSeen, now, prefs.repeatGateMinutes.value)
        alert.fromNodeNum?.let { lastSeen[it] = now }
        return prefs.repeatGate.value && !second
    }

    override fun ackReceived(fromNodeNum: Int, senderName: String, ack: SosAck) {
        Logger.i { "SOS alarm: $senderName answered $ack" }
        acks.add(SosAckEntry(time = nowMillis, nodeNum = fromNodeNum, name = senderName, ack = ack))
        if (!prefs.notifyOnAnswer.value) return
        scope.launch {
            val text =
                getStringSuspend(
                    when (ack) {
                        SosAck.SEEN -> Res.string.sos_ack_seen
                        SosAck.COMING -> Res.string.sos_ack_coming
                        SosAck.CALLING_HELP -> Res.string.sos_ack_help
                    },
                )
            // Someone calling for help gets the bell sound of the alarm; the other answers a quiet alert.
            try {
                if (ack == SosAck.CALLING_HELP) {
                    notifications.showSosAlarmNotification(directKey(fromNodeNum), senderName, text, SosAlarmStyle.BELL)
                } else {
                    notifications.showAlertNotification(directKey(fromNodeNum), senderName, text)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w(e) { "SOS alarm: the notification of the answer could not be shown" }
            }
        }
    }

    /** Local time of day, in minutes since midnight. */
    private fun minuteOfDay(): Int {
        val time = nowInstant.toLocalDateTime(systemTimeZone)
        return time.hour * MINUTES_PER_HOUR + time.minute
    }

    /** Writes the SOS into the history, with the last known position of the sender. */
    private fun record(alert: SosAlert, rang: Boolean) {
        val node = alert.fromNodeNum?.let { nodes.nodeDBbyNum.value[it] }?.takeIf { it.validPosition != null }
        history.add(
            SosHistoryEntry(
                time = nowMillis,
                senderNum = alert.fromNodeNum,
                senderName = alert.senderName,
                contactKey = alert.contactKey,
                text = alert.text,
                latitude = node?.latitude,
                longitude = node?.longitude,
                alarmed = rang,
            ),
        )
    }

    override fun acknowledge() {
        val ringing = stop() ?: return
        if (prefs.confirmToSender.value) {
            sendToSender(ringing, SosAck.SEEN) { getStringSuspend(Res.string.sos_reply_seen) }
        }
    }

    override fun reply(reply: SosReply) {
        val ringing = stop() ?: return
        val ack = if (reply == SosReply.COMING) SosAck.COMING else SosAck.CALLING_HELP
        sendToSender(ringing, ack) {
            getStringSuspend(
                when (reply) {
                    SosReply.COMING -> Res.string.sos_reply_coming
                    SosReply.CALLING_HELP -> Res.string.sos_reply_calling_help
                },
            )
        }
    }

    /** Ends the alarm (sound, notification, window); returns what was ringing, or null when nothing was. */
    private fun stop(): SosAlert? {
        val ringing = mutableAlert.value ?: return null
        mutableAlert.value = null
        sounding = null
        runCatching { SosAlarmSounds.current?.stop() }
        notifications.cancelSosAlarmNotification()
        Logger.i { "SOS alarm: confirmed" }
        return ringing
    }

    /** Sends [text] to the sender with the hidden mark of [ack], so the sender's copy can list who answered. */
    private fun sendToSender(ringing: SosAlert, ack: SosAck, text: suspend () -> String) {
        val num = ringing.fromNodeNum ?: return
        if (ringing.test) return
        scope.launch {
            try {
                sendMessage(SosAckMarker.tag(ack) + text(), directKey(num))
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w(e) { "SOS alarm: the answer to the sender could not be sent" }
            }
        }
    }

    /** The conversation key of a direct message to [num], as the chat and the SOS send do it. */
    private fun directKey(num: Int): String {
        val node = nodes.nodeDBbyNum.value[num]
        val ourPkc = nodes.ourNodeInfo.value?.hasPKC == true
        val slot = if (ourPkc && node?.hasPKC == true) NodeAddress.PKC_CHANNEL_INDEX else node?.channel ?: 0
        return NodeAddress.ByNum(num).toContactKey(slot).value
    }

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}
