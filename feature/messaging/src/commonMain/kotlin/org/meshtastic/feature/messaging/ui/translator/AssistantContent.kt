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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch
import org.meshtastic.core.resources.copy
import org.meshtastic.core.resources.resend
import org.meshtastic.core.ui.util.createClipEntry
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.meshtastic.core.ui.util.isDesktopPlatform
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import org.meshtastic.core.repository.UiPrefs
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.assistant_dictate
import org.meshtastic.core.resources.assistant_dictate_stop
import org.meshtastic.core.resources.assistant_dictate_working
import org.meshtastic.core.resources.assistant_disclaimer
import org.meshtastic.core.resources.assistant_memory_notice
import org.meshtastic.core.resources.assistant_disclaimer_web
import org.meshtastic.core.resources.read_aloud
import org.meshtastic.core.resources.stop_reading
import org.meshtastic.feature.messaging.DictationPhase
import org.meshtastic.feature.messaging.rememberDictation
import org.meshtastic.feature.messaging.rememberSpeaker
import org.meshtastic.core.resources.assistant_empty_hint
import org.meshtastic.core.resources.assistant_error
import org.meshtastic.core.resources.assistant_go_to
import org.meshtastic.core.resources.assistant_web_searched
import org.meshtastic.core.resources.assistant_web_sources
import org.meshtastic.core.resources.assistant_web_used
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
import org.meshtastic.core.ui.assistant.AssistantLinks
import org.meshtastic.core.ui.assistant.SettingHighlight
import org.meshtastic.feature.messaging.translation.AssistantActions
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
    val speaker = rememberSpeaker()
    val dictation = rememberDictation { text ->
        viewModel.setInput((viewModel.state.value.input + " " + text).trim())
    }

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
        val internetOn by koinInject<UiPrefs>().assistantInternetEnabled.collectAsStateWithLifecycle()
        val lastLength = state.lines.lastOrNull()?.text?.length ?: 0
        // Follows the end of the answer while it is written (a long answer is taller than the screen).
        LaunchedEffect(state.lines.size, lastLength) {
            if (state.lines.isNotEmpty()) listState.scrollToItem(state.lines.lastIndex, FOLLOW_END_OFFSET)
        }

        Text(
            text =
                stringResource(
                    if (internetOn) Res.string.assistant_disclaimer_web else Res.string.assistant_disclaimer,
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        val memoryOn by koinInject<UiPrefs>().assistantMemoryEnabled.collectAsStateWithLifecycle()
        if (memoryOn) {
            Text(
                text = stringResource(Res.string.assistant_memory_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.lines.isEmpty()) {
                item {
                    Bubble(fromUser = false, text = "", canRetry = false, onRetry = {}) {
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
            itemsIndexed(state.lines) { index, line ->
                val (shown, actions) = remember(line.text) { AssistantActions.parse(line.text) }
                val web = remember(line.text) { AssistantActions.web(line.text) }
                Bubble(
                    fromUser = line.fromUser,
                    text = shown,
                    canRetry = !state.busy && line.text.isNotEmpty(),
                    onRetry = { viewModel.retry(index) },
                    onReadAloud = if (!line.fromUser && shown.isNotBlank()) ({ speaker.speak(shown) }) else null,
                    onStopReading = if (!line.fromUser && shown.isNotBlank()) ({ speaker.stop() }) else null,
                ) {
                    if (line.text.isEmpty()) {
                        TypingDots()
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (shown.isNotBlank()) Text(text = shown, style = MaterialTheme.typography.bodyLarge)
                            if (web != null) {
                                // A small note in the app's main colour: this answer used the internet.
                                val details =
                                    listOfNotNull(
                                        web.queries.takeIf { it.isNotBlank() }?.let { stringResource(Res.string.assistant_web_searched, it) },
                                        web.sources.takeIf { it.isNotBlank() }?.let { stringResource(Res.string.assistant_web_sources, it) },
                                    )
                                Text(
                                    text = (listOf(stringResource(Res.string.assistant_web_used)) + details).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (actions.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    actions.forEach { action ->
                                        TextButton(
                                            onClick = {
                                                SettingHighlight.show(action.highlight)
                                                AssistantLinks.open(action.uri)
                                            },
                                        ) {
                                            Text(stringResource(Res.string.assistant_go_to, action.label))
                                        }
                                    }
                                }
                            }
                        }
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
            modifier =
            Modifier.fillMaxWidth().onPreviewKeyEvent { event ->
                // Computers: Enter sends, Shift+Enter makes a new line. The phone keeps its normal behaviour.
                if (isDesktopPlatform && event.key == Key.Enter && !event.isShiftPressed) {
                    if (event.type == KeyEventType.KeyUp && !state.busy && state.input.isNotBlank()) viewModel.send()
                    true
                } else {
                    false
                }
            },
        )
        // The buttons start from the right edge: "send" is the last one, right under the thumb.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        ) {
            if (state.lines.isNotEmpty() && !state.busy) {
                TextButton(onClick = viewModel::startNew) { Text(stringResource(Res.string.assistant_new)) }
            }
            if (dictation.available && !state.busy) {
                TextButton(onClick = dictation.toggle, enabled = dictation.phase != DictationPhase.WORKING) {
                    Text(
                        stringResource(
                            when (dictation.phase) {
                                DictationPhase.IDLE -> Res.string.assistant_dictate
                                DictationPhase.LISTENING -> Res.string.assistant_dictate_stop
                                DictationPhase.WORKING -> Res.string.assistant_dictate_working
                            },
                        ),
                    )
                }
            }
            if (state.busy) {
                OutlinedButton(onClick = viewModel::stop) { Text(stringResource(Res.string.assistant_stop)) }
            } else {
                Button(onClick = viewModel::send, enabled = state.input.isNotBlank()) {
                    Text(stringResource(Res.string.assistant_send))
                }
            }
        }
    }
}

/** Scroll offset larger than any bubble: the list stops at its real end, so the newest words stay in view. */
private const val FOLLOW_END_OFFSET = 100_000

/** A chat bubble: yours on the right, the assistant's on the left. Press and hold for copy and ask-again. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Bubble(
    fromUser: Boolean,
    text: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onReadAloud: (() -> Unit)? = null,
    onStopReading: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start) {
        // Wide bubbles: a long answer takes less height; yours and the assistant's may overlap in width.
        Box(modifier = Modifier.widthIn(max = 900.dp).fillMaxWidth(if (fromUser) 0.9f else 0.97f)) {
            Surface(
                shape = if (fromUser) UserBubbleShape else AssistantBubbleShape,
                color =
                if (fromUser) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier =
                Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { if (text.isNotEmpty()) menuOpen = true }),
            ) {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) { content() }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.copy)) },
                    onClick = {
                        menuOpen = false
                        scope.launch { clipboard.setClipEntry(createClipEntry(text, "")) }
                    },
                )
                if (onReadAloud != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.read_aloud)) },
                        onClick = {
                            menuOpen = false
                            onReadAloud()
                        },
                    )
                }
                if (onStopReading != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.stop_reading)) },
                        onClick = {
                            menuOpen = false
                            onStopReading()
                        },
                    )
                }
                if (canRetry) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.resend)) },
                        onClick = {
                            menuOpen = false
                            onRetry()
                        },
                    )
                }
            }
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
