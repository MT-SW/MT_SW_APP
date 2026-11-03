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
package org.meshtastic.feature.messaging.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.repository.TranslatorModelManager
import org.meshtastic.core.repository.TranslatorModelState
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.translate
import org.meshtastic.core.resources.translator_busy
import org.meshtastic.core.resources.translator_cancel
import org.meshtastic.core.resources.translator_error_failed
import org.meshtastic.core.resources.translator_error_same
import org.meshtastic.core.resources.translator_long_warning
import org.meshtastic.core.resources.translator_outgoing_insert
import org.meshtastic.core.resources.translator_outgoing_original
import org.meshtastic.core.resources.translator_outgoing_send
import org.meshtastic.core.resources.translator_outgoing_title
import org.meshtastic.core.resources.translator_to
import org.meshtastic.core.ui.icon.Language
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.feature.messaging.translation.MessageTranslationService
import org.meshtastic.feature.messaging.translation.TranslationResult
import org.meshtastic.feature.messaging.translation.TranslatorLanguages

private const val ONE_PACKET_CHARS = 200

/**
 * Translate-before-sending button for the message composer. Shown only when the offline translator is switched on and
 * its model is on the device. The translation is shown next to the original first; the user then sends it, puts it in
 * the field to edit, or cancels.
 */
@Suppress("LongMethod")
@Composable
internal fun OutgoingTranslateAction(
    textFieldState: TextFieldState,
    isEnabled: Boolean,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiPrefs = koinInject<UiPrefs>()
    val manager = koinInject<TranslatorModelManager>()
    val service = koinInject<MessageTranslationService>()
    val translatorOn by uiPrefs.translatorEnabled.collectAsStateWithLifecycle()
    val modelState by manager.state.collectAsStateWithLifecycle()
    if (!translatorOn || modelState != TranslatorModelState.Ready) return

    var open by remember { mutableStateOf(false) }
    var original by remember { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("eng_Latn") }
    var result by remember { mutableStateOf<String?>(null) }
    var failure by remember { mutableStateOf<TranslationFailure?>(null) }
    var busy by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(open, target, original) {
        if (!open) return@LaunchedEffect
        busy = true
        result = null
        failure = null
        when (val r = service.translateBetween(original, null, target)) {
            is TranslationResult.Success -> result = r.translatedText
            is TranslationResult.NotRequired -> failure = TranslationFailure.Same
            else -> failure = TranslationFailure.Failed
        }
        busy = false
    }

    // A small grey chip above the field, only while there is text to translate: no extra row sits under the field.
    if (textFieldState.text.isNotBlank()) {
        Row(modifier = modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.Start) {
            AssistChip(
                onClick = {
                    original = textFieldState.text.toString().trim()
                    open = true
                },
                enabled = isEnabled,
                label = { Text(stringResource(Res.string.translate), style = MaterialTheme.typography.labelMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = MeshtasticIcons.Language,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                colors =
                AssistChipDefaults.assistChipColors(
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }

    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(Res.string.translator_outgoing_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.translator_to), style = MaterialTheme.typography.labelMedium)
                        Column {
                            OutlinedButton(onClick = { menuOpen = true }) {
                                Text(TranslatorLanguages.fromNllb(target)?.polishName ?: target)
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                TranslatorLanguages.all.forEach { language ->
                                    DropdownMenuItem(
                                        text = { Text(language.polishName) },
                                        onClick = {
                                            menuOpen = false
                                            target = language.nllb
                                        },
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = stringResource(Res.string.translator_outgoing_original),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(original, style = MaterialTheme.typography.bodyMedium)
                    when {
                        busy -> Text(stringResource(Res.string.translator_busy), style = MaterialTheme.typography.bodyMedium)

                        result != null -> {
                            Text(
                                text = TranslatorLanguages.fromNllb(target)?.polishName ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(result.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                            if (result.orEmpty().length > ONE_PACKET_CHARS) {
                                Text(
                                    stringResource(Res.string.translator_long_warning),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        failure != null ->
                            Text(
                                stringResource(
                                    if (failure == TranslationFailure.Same) {
                                        Res.string.translator_error_same
                                    } else {
                                        Res.string.translator_error_failed
                                    },
                                ),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                    }
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        enabled = result != null,
                        onClick = {
                            textFieldState.setTextAndPlaceCursorAtEnd(result.orEmpty())
                            open = false
                        },
                    ) {
                        Text(stringResource(Res.string.translator_outgoing_insert))
                    }
                    TextButton(
                        enabled = result != null,
                        onClick = {
                            textFieldState.setTextAndPlaceCursorAtEnd(result.orEmpty())
                            open = false
                            onSendMessage()
                        },
                    ) {
                        Text(stringResource(Res.string.translator_outgoing_send))
                    }
                }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(Res.string.translator_cancel)) } },
        )
    }
}

private enum class TranslationFailure {
    Same,
    Failed,
}
