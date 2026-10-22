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
package org.meshtastic.feature.messaging.ui.translator

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.assistant_disclaimer
import org.meshtastic.core.resources.assistant_empty_hint
import org.meshtastic.core.resources.assistant_error
import org.meshtastic.core.resources.assistant_greeting
import org.meshtastic.core.resources.assistant_input_hint
import org.meshtastic.core.resources.assistant_model_missing
import org.meshtastic.core.resources.assistant_new
import org.meshtastic.core.resources.assistant_off
import org.meshtastic.core.resources.assistant_send
import org.meshtastic.core.resources.assistant_stop
import org.meshtastic.core.resources.assistant_suggest_1
import org.meshtastic.core.resources.assistant_suggest_2
import org.meshtastic.core.resources.assistant_suggest_3
import org.meshtastic.core.resources.assistant_suggest_4
import org.meshtastic.feature.messaging.translation.AssistantViewModel

private val BubbleShape = RoundedCornerShape(20.dp)
private val UserBubbleShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
private val AssistantBubbleShape =
    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)

/** Chat with the offline assistant. Everything runs on this device. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssistantContent(viewModel: AssistantViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val modelReady by viewModel.modelReady.collectAsStateWithLifecycle()

    Column(modifier = modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!enabled || !modelReady) {
            Text(
                text = stringResource(if (!enabled) Res.string.assistant_off else Res.string.assistant_model_missing),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            return@Column
        }

        val listState = rememberLazyListState()
        val lastLength = state.lines.lastOrNull()?.text?.length ?: 0
        LaunchedEffect(state.lines.size, lastLength) {
            if (state.lines.isNotEmpty()) listState.scrollToItem(state.lines.lastIndex)
        }

        Text(
            text = stringResource(Res.string.assistant_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.lines.isEmpty()) {
                item {
                    Bubble(fromUser = false) {
                        Text(
                            text = stringResource(Res.string.assistant_greeting),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                item {
                    val suggestions =
                        listOf(
                            stringResource(Res.string.assistant_suggest_1),
                            stringResource(Res.string.assistant_suggest_2),
                            stringResource(Res.string.assistant_suggest_3),
                            stringResource(Res.string.assistant_suggest_4),
                        )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        suggestions.forEach { text ->
                            SuggestionChip(
                                onClick = {
                                    viewModel.setInput(text)
                                    viewModel.send()
                                },
                                label = { Text(text) },
                            )
                        }
                    }
                }
                item {
                    Text(
                        text = stringResource(Res.string.assistant_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            items(state.lines) { line ->
                Bubble(fromUser = line.fromUser) {
                    if (line.text.isEmpty()) {
                        TypingDots()
                    } else {
                        SelectionContainer { Text(text = line.text, style = MaterialTheme.typography.bodyLarge) }
                    }
                }
            }
        }

        if (state.failed) {
            Text(
                text = stringResource(Res.string.assistant_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        OutlinedTextField(
            value = state.input,
            onValueChange = viewModel::setInput,
            label = { Text(stringResource(Res.string.assistant_input_hint)) },
            minLines = 1,
            maxLines = 4,
            shape = BubbleShape,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        ) {
            if (state.busy) {
                OutlinedButton(onClick = viewModel::stop) { Text(stringResource(Res.string.assistant_stop)) }
            } else {
                Button(onClick = viewModel::send, enabled = state.input.isNotBlank()) {
                    Text(stringResource(Res.string.assistant_send))
                }
            }
            if (state.lines.isNotEmpty() && !state.busy) {
                TextButton(onClick = viewModel::startNew) { Text(stringResource(Res.string.assistant_new)) }
            }
        }
    }
}

/** A chat bubble: yours on the right, the assistant's on the left. */
@Composable
private fun Bubble(fromUser: Boolean, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start) {
        Surface(
            shape = if (fromUser) UserBubbleShape else AssistantBubbleShape,
            color =
            if (fromUser) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(if (fromUser) 0.85f else 0.92f),
        ) {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) { content() }
        }
    }
}

/** Three softly pulsing dots shown while the assistant is still thinking. */
@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition()
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp),
    ) {
        repeat(3) { index ->
            val alpha by
                transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 0.25f,
                    animationSpec =
                    infiniteRepeatable(
                        animation =
                        keyframes {
                            durationMillis = 1200
                            0.25f at 0
                            1f at 300
                            0.25f at 600
                        },
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset = androidx.compose.animation.core.StartOffset(index * 200),
                    ),
                )
            Box(
                modifier =
                Modifier.size(9.dp).alpha(alpha).background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
            )
        }
    }
}
