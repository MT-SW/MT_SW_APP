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
package org.meshtastic.core.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.navigation.DEEP_LINK_BASE_URI
import org.meshtastic.core.repository.SosAlarmCenter
import org.meshtastic.core.repository.SosReply
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.sos_alarm_confirm
import org.meshtastic.core.resources.sos_alarm_from
import org.meshtastic.core.resources.sos_alarm_map
import org.meshtastic.core.resources.sos_alarm_open
import org.meshtastic.core.resources.sos_alarm_test_note
import org.meshtastic.core.resources.sos_button
import org.meshtastic.core.resources.sos_reply_button_calling_help
import org.meshtastic.core.resources.sos_reply_button_coming
import org.meshtastic.core.ui.viewmodel.UIViewModel

/** A ready answer of the alarm window: white outline on the red, sent to the sender as a direct message. */
@Composable
private fun ReplyButton(label: StringResource, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(2.dp, Color.White),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
    ) {
        Text(text = stringResource(label), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

private val AlarmRed = Color(0xFFD32F2F)
private const val PULSE_MS = 600
private const val PULSE_MIN = 0.7f

/**
 * The alarm for an SOS that arrived: a big SOS over the whole app, with who sent it and what they wrote, pulsing until
 * the person confirms it. When it appears the conversation it came in is opened underneath, so that is where the person
 * lands after confirming. The sound is the platform's (see [org.meshtastic.core.repository.SosAlarmSound]).
 */
@Suppress("LongMethod", "ModifierMissing") // a full-screen overlay: it has no place to be given by a parent
@Composable
fun SosAlarmOverlay(uiViewModel: UIViewModel) {
    val center = koinInject<SosAlarmCenter>()
    val alert by center.alert.collectAsStateWithLifecycle()
    val current = alert ?: return
    val openConversation = {
        uiViewModel.handleDeepLink(CommonUri.parse("$DEEP_LINK_BASE_URI/messages/${current.contactKey}"))
    }
    LaunchedEffect(current.id) { if (!current.test) openConversation() }

    val pulse by
        rememberInfiniteTransition()
            .animateFloat(
                initialValue = PULSE_MIN,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(PULSE_MS), RepeatMode.Reverse),
            )
    Box(
        modifier =
        Modifier.fillMaxSize()
            .background(AlarmRed.copy(alpha = pulse))
            // Everything under the alarm is out of reach until it is confirmed.
            .pointerInput(Unit) { detectTapGestures {} }
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.sos_button),
                fontSize = 120.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Text(
                text = stringResource(Res.string.sos_alarm_from, current.senderName),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = current.text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            if (current.test) {
                Text(
                    text = stringResource(Res.string.sos_alarm_test_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
            Button(
                onClick = { center.acknowledge() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AlarmRed),
            ) {
                Text(
                    text = stringResource(Res.string.sos_alarm_confirm),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            val sender = current.fromNodeNum
            if (!current.test && sender != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ReplyButton(
                        label = Res.string.sos_reply_button_coming,
                        onClick = { center.reply(SosReply.COMING) },
                        modifier = Modifier.weight(1f),
                    )
                    ReplyButton(
                        label = Res.string.sos_reply_button_calling_help,
                        onClick = { center.reply(SosReply.CALLING_HELP) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (!current.test) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            center.acknowledge()
                            openConversation()
                        },
                    ) {
                        Text(text = stringResource(Res.string.sos_alarm_open), color = Color.White)
                    }
                    if (sender != null) {
                        TextButton(
                            onClick = {
                                center.acknowledge()
                                uiViewModel.handleDeepLink(CommonUri.parse("$DEEP_LINK_BASE_URI/map/focus/$sender"))
                            },
                        ) {
                            Text(text = stringResource(Res.string.sos_alarm_map), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
