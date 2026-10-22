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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import org.meshtastic.core.resources.assistant_name
import org.meshtastic.core.resources.assistant_new
import org.meshtastic.core.resources.assistant_off
import org.meshtastic.core.resources.assistant_send
import org.meshtastic.core.resources.assistant_stop
import org.meshtastic.core.resources.assistant_you
import org.meshtastic.feature.messaging.translation.AssistantViewModel

/** Chat with the offline assistant. Everything runs on this device. */
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.lines.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(Res.string.assistant_name),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(Res.string.assistant_greeting),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
                item {
                    Text(
                        text = stringResource(Res.string.assistant_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            items(state.lines) { line ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                    CardDefaults.cardColors(
                        containerColor =
                        if (line.fromUser) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text =
                            stringResource(if (line.fromUser) Res.string.assistant_you else Res.string.assistant_name),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(text = line.text.ifEmpty { "…" }, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
