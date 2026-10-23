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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.repository.TranslatorModelState
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.translator_action
import org.meshtastic.core.resources.translator_auto
import org.meshtastic.core.resources.translator_busy
import org.meshtastic.core.resources.translator_error_failed
import org.meshtastic.core.resources.translator_error_same
import org.meshtastic.core.resources.translator_error_unknown
import org.meshtastic.core.resources.translator_from
import org.meshtastic.core.resources.translator_input_hint
import org.meshtastic.core.resources.translator_loading_hint
import org.meshtastic.core.resources.translator_long_warning
import org.meshtastic.core.resources.translator_model_missing
import org.meshtastic.core.resources.translator_off
import org.meshtastic.core.resources.translator_result
import org.meshtastic.core.resources.translator_swap
import org.meshtastic.core.resources.translator_to
import org.meshtastic.core.ui.component.CopyIconButton
import org.meshtastic.core.resources.assistant_dictate
import org.meshtastic.core.resources.assistant_dictate_stop
import org.meshtastic.core.resources.assistant_dictate_working
import org.meshtastic.feature.messaging.DictationPhase
import org.meshtastic.feature.messaging.rememberDictation
import org.meshtastic.feature.messaging.translation.TranslatorError
import org.meshtastic.feature.messaging.translation.TranslatorLanguages
import org.meshtastic.feature.messaging.translation.TranslatorViewModel

/** The translator itself: type a text, pick the languages, translate and copy. Works fully offline. */
@Composable
fun TranslatorContent(viewModel: TranslatorViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val modelState by viewModel.modelState.collectAsStateWithLifecycle()
    val dictation = rememberDictation(translator = true) { text -> viewModel.setInput((viewModel.state.value.input + " " + text).trim()) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!enabled) {
            Text(stringResource(Res.string.translator_off), style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        if (modelState != TranslatorModelState.Ready) {
            Text(stringResource(Res.string.translator_model_missing), style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            LanguagePicker(
                label = stringResource(Res.string.translator_from),
                selected = state.sourceNllb,
                allowAuto = true,
                onSelect = viewModel::setSource,
                modifier = Modifier.weight(1f),
            )
            LanguagePicker(
                label = stringResource(Res.string.translator_to),
                selected = state.targetNllb,
                allowAuto = false,
                onSelect = { it?.let(viewModel::setTarget) },
                modifier = Modifier.weight(1f),
            )
        }
        TextButton(onClick = viewModel::swapLanguages) { Text(stringResource(Res.string.translator_swap)) }

        OutlinedTextField(
            value = state.input,
            onValueChange = viewModel::setInput,
            label = { Text(stringResource(Res.string.translator_input_hint)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
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
        Button(onClick = viewModel::translate, enabled = !state.busy && state.input.isNotBlank()) {
            Text(
                stringResource(if (state.busy) Res.string.translator_busy else Res.string.translator_action),
            )
        }

        if (state.busy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(Res.string.translator_loading_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        state.error?.let { error ->
            Text(
                text =
                stringResource(
                    when (error) {
                        TranslatorError.UnknownLanguage -> Res.string.translator_error_unknown
                        TranslatorError.SameLanguage -> Res.string.translator_error_same
                        TranslatorError.ModelMissing -> Res.string.translator_model_missing
                        TranslatorError.Failed -> Res.string.translator_error_failed
                    },
                ),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (state.output.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(Res.string.translator_result),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        CopyIconButton(valueToCopy = state.output)
                    }
                    Text(text = state.output, style = MaterialTheme.typography.bodyLarge)
                    if (viewModel.isTooLongForOnePacket(state.output)) {
                        Text(
                            text = stringResource(Res.string.translator_long_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguagePicker(
    label: String,
    selected: String?,
    allowAuto: Boolean,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val name = selected?.let { TranslatorLanguages.fromNllb(it)?.polishName } ?: stringResource(Res.string.translator_auto)
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text(name) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (allowAuto) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.translator_auto)) },
                    onClick = {
                        open = false
                        onSelect(null)
                    },
                )
            }
            TranslatorLanguages.all.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.polishName) },
                    onClick = {
                        open = false
                        onSelect(language.nllb)
                    },
                )
            }
        }
    }
}
