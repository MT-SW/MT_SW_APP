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
package org.meshtastic.feature.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.common.di.ApplicationCoroutineScope
import org.meshtastic.core.repository.AiLicense
import org.meshtastic.core.repository.AiModelInfo
import org.meshtastic.core.repository.AiModelStore
import org.meshtastic.core.repository.AiModels
import org.meshtastic.core.repository.AiRole
import org.meshtastic.core.repository.TranslatorModelState
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ai_assistant_summary
import org.meshtastic.core.resources.ai_assistant_title
import org.meshtastic.core.resources.ai_assistant_unavailable
import org.meshtastic.core.resources.ai_assistant_warning_title
import org.meshtastic.core.resources.ai_internet_enable
import org.meshtastic.core.resources.ai_internet_on_indicator
import org.meshtastic.core.resources.ai_internet_summary
import org.meshtastic.core.resources.ai_internet_title
import org.meshtastic.core.resources.ai_internet_warning_body
import org.meshtastic.core.resources.ai_internet_warning_title
import org.meshtastic.core.resources.ai_license_cc
import org.meshtastic.core.resources.ai_license_gemma
import org.meshtastic.core.resources.ai_level_light
import org.meshtastic.core.resources.ai_level_standard
import org.meshtastic.core.resources.ai_level_strong
import org.meshtastic.core.resources.ai_model_assistant_title
import org.meshtastic.core.resources.ai_model_cancel
import org.meshtastic.core.resources.ai_model_delete
import org.meshtastic.core.resources.ai_model_download
import org.meshtastic.core.resources.ai_model_failed
import org.meshtastic.core.resources.ai_model_none_ready
import org.meshtastic.core.resources.ai_model_roles_both
import org.meshtastic.core.resources.ai_model_roles_translator
import org.meshtastic.core.resources.ai_model_soon
import org.meshtastic.core.resources.ai_model_specs
import org.meshtastic.core.resources.ai_model_translator_title
import org.meshtastic.core.resources.ai_models_title
import org.meshtastic.core.resources.ai_privacy
import org.meshtastic.core.resources.ai_warning_body
import org.meshtastic.core.resources.translator_cancel
import org.meshtastic.core.resources.translator_downloading
import org.meshtastic.core.resources.translator_ready
import org.meshtastic.core.resources.translator_summary
import org.meshtastic.core.resources.translator_title
import org.meshtastic.core.resources.translator_warning_enable
import org.meshtastic.core.resources.translator_warning_title
import org.meshtastic.core.ui.component.SwitchPreference

private enum class PendingSwitch {
    TRANSLATOR,
    ASSISTANT,
}

/**
 * The offline translator and assistant: two independent switches (either one can be on without the other), the list
 * of models with download and delete for each, and the choice of which downloaded model each mode uses.
 */
@Composable
internal fun AiSettings() {
    val uiPrefs = koinInject<UiPrefs>()
    val store = koinInject<AiModelStore>()
    val appScope = koinInject<ApplicationCoroutineScope>()
    val translatorOn by uiPrefs.translatorEnabled.collectAsStateWithLifecycle()
    val assistantOn by uiPrefs.assistantEnabled.collectAsStateWithLifecycle()
    val translatorModelId by uiPrefs.translatorModelId.collectAsStateWithLifecycle()
    val assistantModelId by uiPrefs.assistantModelId.collectAsStateWithLifecycle()
    val states by store.states.collectAsStateWithLifecycle()
    val internetOn by uiPrefs.assistantInternetEnabled.collectAsStateWithLifecycle()
    var askInternet by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<PendingSwitch?>(null) }

    fun isReady(id: String) = states[id] == TranslatorModelState.Ready

    val assistantPossible = AiModels.ASSISTANT_SCREEN_READY && AiModels.all.any { it.engineReady && it.canDo(AiRole.ASSISTANT) }

    SwitchPreference(
        title = stringResource(Res.string.translator_title),
        summary = stringResource(Res.string.translator_summary),
        checked = translatorOn,
        enabled = true,
        onCheckedChange = { on ->
            if (!on) {
                uiPrefs.setTranslatorEnabled(false)
            } else if (AiModels.all.any { isReady(it.id) && it.canDo(AiRole.TRANSLATOR) }) {
                uiPrefs.setTranslatorEnabled(true)
            } else {
                pending = PendingSwitch.TRANSLATOR
            }
        },
    )

    SwitchPreference(
        title = stringResource(Res.string.ai_assistant_title),
        summary =
        if (assistantPossible) {
            stringResource(Res.string.ai_assistant_summary)
        } else {
            stringResource(Res.string.ai_assistant_unavailable)
        },
        checked = assistantOn && assistantPossible,
        enabled = assistantPossible,
        onCheckedChange = { on ->
            if (!on) {
                uiPrefs.setAssistantEnabled(false)
            } else if (AiModels.all.any { isReady(it.id) && it.canDo(AiRole.ASSISTANT) }) {
                uiPrefs.setAssistantEnabled(true)
            } else {
                pending = PendingSwitch.ASSISTANT
            }
        },
    )

    if (assistantOn && assistantPossible) {
        SwitchPreference(
            title = stringResource(Res.string.ai_internet_title),
            summary = stringResource(Res.string.ai_internet_summary),
            checked = internetOn,
            enabled = true,
            onCheckedChange = { on -> if (on) askInternet = true else uiPrefs.setAssistantInternetEnabled(false) },
        )
        if (internetOn) {
            Text(
                text = stringResource(Res.string.ai_internet_on_indicator),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }

    if (askInternet) {
        AlertDialog(
            onDismissRequest = { askInternet = false },
            title = { Text(stringResource(Res.string.ai_internet_warning_title)) },
            text = { Text(stringResource(Res.string.ai_internet_warning_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        askInternet = false
                        uiPrefs.setAssistantInternetEnabled(true)
                    },
                ) {
                    Text(stringResource(Res.string.ai_internet_enable))
                }
            },
            dismissButton = {
                TextButton(onClick = { askInternet = false }) { Text(stringResource(Res.string.translator_cancel)) }
            },
        )
    }

    if (translatorOn || assistantOn) {
        SectionLabel(stringResource(Res.string.ai_models_title))
        AiModels.all.forEach { model ->
            ModelRow(
                model = model,
                state = states[model.id] ?: TranslatorModelState.NotDownloaded,
                onDownload = { appScope.launch { store.download(model.id) } },
                onCancel = { store.cancelDownload(model.id) },
                onDelete = { appScope.launch { store.delete(model.id) } },
            )
        }

        if (translatorOn) {
            ModelChoice(
                title = stringResource(Res.string.ai_model_translator_title),
                models = AiModels.all.filter { it.canDo(AiRole.TRANSLATOR) && isReady(it.id) },
                selectedId = translatorModelId,
                onSelect = { uiPrefs.setTranslatorModelId(it) },
            )
        }
        if (assistantOn) {
            ModelChoice(
                title = stringResource(Res.string.ai_model_assistant_title),
                models = AiModels.all.filter { it.canDo(AiRole.ASSISTANT) && isReady(it.id) },
                selectedId = assistantModelId,
                onSelect = { uiPrefs.setAssistantModelId(it) },
            )
        }
    }

    Text(
        text = stringResource(Res.string.ai_privacy),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )

    pending?.let { which ->
        val role = if (which == PendingSwitch.TRANSLATOR) AiRole.TRANSLATOR else AiRole.ASSISTANT
        // Offer the lightest model that can do the job and whose engine is available.
        val model = AiModels.all.firstOrNull { it.engineReady && it.canDo(role) } ?: AiModels.Nllb600
        AlertDialog(
            onDismissRequest = { pending = null },
            title = {
                Text(
                    stringResource(
                        if (which == PendingSwitch.TRANSLATOR) {
                            Res.string.translator_warning_title
                        } else {
                            Res.string.ai_assistant_warning_title
                        },
                    ),
                )
            },
            text = {
                Text(stringResource(Res.string.ai_warning_body, model.sizeMb, model.ramMb, model.minDeviceRamGb))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pending = null
                        if (which == PendingSwitch.TRANSLATOR) {
                            uiPrefs.setTranslatorEnabled(true)
                            uiPrefs.setTranslatorModelId(model.id)
                        } else {
                            uiPrefs.setAssistantEnabled(true)
                            uiPrefs.setAssistantModelId(model.id)
                        }
                        if (!isReady(model.id)) appScope.launch { store.download(model.id) }
                    },
                ) {
                    Text(stringResource(Res.string.translator_warning_enable))
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text(stringResource(Res.string.translator_cancel)) }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun levelName(model: AiModelInfo): String = stringResource(
    when (model.id) {
        AiModels.NLLB_600M_ID -> Res.string.ai_level_light
        AiModels.GEMMA_E2B_ID -> Res.string.ai_level_light
        AiModels.GEMMA_E4B_ID -> Res.string.ai_level_standard
        else -> Res.string.ai_level_strong
    },
)

@Composable
private fun ModelRow(
    model: AiModelInfo,
    state: TranslatorModelState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(text = "${levelName(model)}: ${model.name}", style = MaterialTheme.typography.bodyLarge)
        Text(
            text =
            stringResource(
                if (model.canDo(AiRole.ASSISTANT)) {
                    Res.string.ai_model_roles_both
                } else {
                    Res.string.ai_model_roles_translator
                },
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = stringResource(Res.string.ai_model_specs, model.sizeMb, model.ramMb, model.minDeviceRamGb),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text =
            stringResource(
                if (model.license == AiLicense.CC_BY_NC_4_0) Res.string.ai_license_cc else Res.string.ai_license_gemma,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            !model.engineReady -> Text(stringResource(Res.string.ai_model_soon), style = MaterialTheme.typography.bodySmall)

            else ->
                when (state) {
                    TranslatorModelState.NotDownloaded ->
                        TextButton(onClick = onDownload) { Text(stringResource(Res.string.ai_model_download)) }

                    is TranslatorModelState.Downloading -> {
                        Text(
                            text = stringResource(Res.string.translator_downloading, (state.progress * 100).toInt()),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                        TextButton(onClick = onCancel) { Text(stringResource(Res.string.ai_model_cancel)) }
                    }

                    TranslatorModelState.Ready -> {
                        Text(stringResource(Res.string.translator_ready), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onDelete) { Text(stringResource(Res.string.ai_model_delete)) }
                    }

                    is TranslatorModelState.Failed -> {
                        Text(stringResource(Res.string.ai_model_failed), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onDownload) { Text(stringResource(Res.string.ai_model_download)) }
                    }
                }
        }
    }
}

@Composable
private fun ModelChoice(title: String, models: List<AiModelInfo>, selectedId: String, onSelect: (String) -> Unit) {
    SectionLabel(title)
    if (models.isEmpty()) {
        Text(
            text = stringResource(Res.string.ai_model_none_ready),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        return
    }
    // When the stored choice is not among the downloaded models, the first one is the one in use.
    val effective = models.firstOrNull { it.id == selectedId }?.id ?: models.first().id
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        models.forEach { model ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = model.id == effective, onClick = { onSelect(model.id) })
                Text(text = "${levelName(model)}: ${model.name}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
