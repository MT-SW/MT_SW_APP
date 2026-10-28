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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.common.di.ApplicationCoroutineScope
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
import org.meshtastic.core.resources.ai_dictation_note
import org.meshtastic.core.resources.ai_internet_enable
import org.meshtastic.core.resources.ai_internet_on_indicator
import org.meshtastic.core.resources.ai_internet_summary
import org.meshtastic.core.resources.ai_internet_title
import org.meshtastic.core.resources.ai_internet_warning_body
import org.meshtastic.core.resources.ai_internet_warning_title
import org.meshtastic.core.resources.ai_legacy_freed
import org.meshtastic.core.resources.ai_model_alternative
import org.meshtastic.core.resources.ai_model_cancel
import org.meshtastic.core.resources.ai_model_choose_downloading
import org.meshtastic.core.resources.ai_model_choose_red
import org.meshtastic.core.resources.ai_model_delete
import org.meshtastic.core.resources.ai_model_download
import org.meshtastic.core.resources.ai_model_failed
import org.meshtastic.core.resources.ai_model_hearing
import org.meshtastic.core.resources.ai_model_in_use
import org.meshtastic.core.resources.ai_model_license_line
import org.meshtastic.core.resources.ai_model_page
import org.meshtastic.core.resources.ai_model_recommended
import org.meshtastic.core.resources.ai_model_soon
import org.meshtastic.core.resources.ai_model_specs
import org.meshtastic.core.resources.ai_model_use
import org.meshtastic.core.resources.ai_models_empty
import org.meshtastic.core.resources.ai_models_hint
import org.meshtastic.core.resources.ai_unload_loaded
import org.meshtastic.core.resources.ai_unload_none
import org.meshtastic.core.resources.ai_unload_title
import org.meshtastic.core.resources.ai_models_tab_assistant
import org.meshtastic.core.resources.ai_models_tab_dictation
import org.meshtastic.core.resources.ai_models_tab_translator
import org.meshtastic.core.resources.ai_models_title
import org.meshtastic.core.resources.ai_privacy
import org.meshtastic.core.resources.dictation_external_mic_summary
import org.meshtastic.core.resources.dictation_external_mic_title
import org.meshtastic.core.resources.translator_cancel
import org.meshtastic.core.resources.translator_downloading
import org.meshtastic.core.resources.translator_ready
import org.meshtastic.core.resources.translator_summary
import org.meshtastic.core.resources.translator_title
import org.meshtastic.core.ui.component.RegularPreference
import org.meshtastic.core.ui.component.SwitchPreference

/**
 * The offline translator and assistant: two independent switches, and below them the models, one tab per function
 * (assistant, translator, dictation). Nothing is downloaded by itself: after a function is switched on, a small red
 * text asks for a model until one is installed.
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
    val dictationModelId by uiPrefs.dictationModelId.collectAsStateWithLifecycle()
    val states by store.states.collectAsStateWithLifecycle()
    val freedMb by store.legacyFreedMb.collectAsStateWithLifecycle()
    val internetOn by uiPrefs.assistantInternetEnabled.collectAsStateWithLifecycle()
    var askInternet by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var freedNoticeShown by remember { mutableStateOf(true) }

    fun isReady(id: String) = states[id] == TranslatorModelState.Ready

    fun readyFor(role: AiRole) = AiModels.forRole(role).filter { isReady(it.id) }

    fun downloadingFor(role: AiRole) =
        AiModels.forRole(role).any { states[it.id] is TranslatorModelState.Downloading }

    val assistantPossible = AiModels.ASSISTANT_SCREEN_READY && AiModels.forRole(AiRole.ASSISTANT).isNotEmpty()

    // The model in use for a function: the stored choice when it is installed, otherwise the first installed one.
    fun effectiveFor(role: AiRole, stored: String): AiModelInfo? {
        val ready = readyFor(role)
        return ready.firstOrNull { it.id == stored } ?: ready.firstOrNull()
    }

    val effAssistant = effectiveFor(AiRole.ASSISTANT, assistantModelId)
    val effTranslator = effectiveFor(AiRole.TRANSLATOR, translatorModelId)
    val effDictation = effectiveFor(AiRole.DICTATION, dictationModelId)
    // Keeps the stored choices equal to what is really used (the first model a person installs becomes the chosen one).
    LaunchedEffect(effAssistant?.id) { effAssistant?.let { if (it.id != assistantModelId) uiPrefs.setAssistantModelId(it.id) } }
    LaunchedEffect(effTranslator?.id) { effTranslator?.let { if (it.id != translatorModelId) uiPrefs.setTranslatorModelId(it.id) } }
    LaunchedEffect(effDictation?.id) { effDictation?.let { if (it.id != dictationModelId) uiPrefs.setDictationModelId(it.id) } }

    if (freedMb > 0 && freedNoticeShown) {
        AlertDialog(
            onDismissRequest = { freedNoticeShown = false },
            text = { Text(stringResource(Res.string.ai_legacy_freed, freedMb)) },
            confirmButton = { TextButton(onClick = { freedNoticeShown = false }) { Text("OK") } },
        )
    }

    SwitchPreference(
        title = stringResource(Res.string.translator_title),
        summary = stringResource(Res.string.translator_summary),
        checked = translatorOn,
        enabled = true,
        onCheckedChange = { uiPrefs.setTranslatorEnabled(it) },
    )
    if (translatorOn && effTranslator == null) {
        ChooseModelHint(downloading = downloadingFor(AiRole.TRANSLATOR)) { tab = 1 }
    }

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
        onCheckedChange = { uiPrefs.setAssistantEnabled(it) },
    )
    if (assistantOn && assistantPossible && effAssistant == null) {
        ChooseModelHint(downloading = downloadingFor(AiRole.ASSISTANT)) { tab = 0 }
    }

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
        AssistantMemorySettings()
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

    // The model that is running right now and a way to switch it off at once (otherwise it stays while it is used and
    // is swapped as soon as another model is chosen, or freed when its function is switched off).
    val loaded by store.loadedModels.collectAsStateWithLifecycle()
    val loadedNames = loaded.map { id -> AiModels.byId(id)?.name ?: id }.sorted().joinToString(", ")
    RegularPreference(
        title = stringResource(Res.string.ai_unload_title),
        subtitle =
        if (loaded.isEmpty()) {
            stringResource(Res.string.ai_unload_none)
        } else {
            stringResource(Res.string.ai_unload_loaded, loadedNames)
        },
        onClick = { appScope.launch { store.unloadModels() } },
        enabled = loaded.isNotEmpty(),
    )

    SectionLabel(stringResource(Res.string.ai_models_title))
    val tabs =
        listOf(
            AiRole.ASSISTANT to stringResource(Res.string.ai_models_tab_assistant),
            AiRole.TRANSLATOR to stringResource(Res.string.ai_models_tab_translator),
            AiRole.DICTATION to stringResource(Res.string.ai_models_tab_dictation),
        )
    // Tab 0 = assistant, 1 = translator, 2 = dictation.
    PrimaryTabRow(selectedTabIndex = tab) {
        tabs.forEachIndexed { index, (_, title) ->
            Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
        }
    }
    val role = tabs[tab].first
    val (current, onChoose) =
        when (role) {
            AiRole.ASSISTANT -> effAssistant to { id: String -> uiPrefs.setAssistantModelId(id) }
            AiRole.TRANSLATOR -> effTranslator to { id: String -> uiPrefs.setTranslatorModelId(id) }
            AiRole.DICTATION -> effDictation to { id: String -> uiPrefs.setDictationModelId(id) }
        }
    if (role == AiRole.DICTATION) {
        Text(
            text = stringResource(Res.string.ai_dictation_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        val externalMic by uiPrefs.dictationExternalMic.collectAsStateWithLifecycle()
        SwitchPreference(
            title = stringResource(Res.string.dictation_external_mic_title),
            summary = stringResource(Res.string.dictation_external_mic_summary),
            checked = externalMic,
            enabled = true,
            onCheckedChange = { uiPrefs.setDictationExternalMic(it) },
        )
    }
    val models = AiModels.forRole(role)
    if (models.isEmpty()) {
        Text(
            text = stringResource(Res.string.ai_models_empty),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    // A second, smaller row of tabs: one per model family (Gemma, Qwen, Bielik, ...), so only one family is on screen.
    val families = models.map { it.family }.distinct()
    var familyIndex by rememberSaveable(role) { mutableIntStateOf(0) }
    val shownIndex = familyIndex.coerceIn(0, (families.size - 1).coerceAtLeast(0))
    if (families.size > 1) {
        SecondaryScrollableTabRow(selectedTabIndex = shownIndex, edgePadding = 0.dp) {
            families.forEachIndexed { index, family ->
                // A dot marks the family of the model that is in use.
                val label = if (current?.family == family) "$family •" else family
                Tab(selected = shownIndex == index, onClick = { familyIndex = index }, text = { Text(label) })
            }
        }
    }
    families.getOrNull(shownIndex)?.let { family ->
        models.filter { it.family == family }.forEach { model ->
            ModelRow(
                model = model,
                role = role,
                state = states[model.id] ?: TranslatorModelState.NotDownloaded,
                inUse = current?.id == model.id,
                onUse = { onChoose(model.id) },
                onDownload = {
                    // The model a person chooses to download becomes the one in use when none is installed yet.
                    if (current == null) onChoose(model.id)
                    appScope.launch { store.download(model.id) }
                },
                onCancel = { store.cancelDownload(model.id) },
                onDelete = { appScope.launch { store.delete(model.id) } },
            )
        }
    }
    Text(
        text = stringResource(Res.string.ai_models_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )

    Text(
        text = stringResource(Res.string.ai_privacy),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** The small red text under a switch: a function is on, but no model for it is installed yet. */
@Composable
private fun ChooseModelHint(downloading: Boolean, onClick: () -> Unit) {
    Text(
        text =
        stringResource(
            if (downloading) Res.string.ai_model_choose_downloading else Res.string.ai_model_choose_red,
        ),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 2.dp),
    )
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
private fun ModelRow(
    model: AiModelInfo,
    role: AiRole,
    state: TranslatorModelState,
    inUse: Boolean,
    onUse: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val polish = Locale.current.language == "pl"
    val description =
        (if (polish) model.descriptionPl.ifBlank { model.descriptionEn } else model.descriptionEn.ifBlank { model.descriptionPl })
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state == TranslatorModelState.Ready) RadioButton(selected = inUse, onClick = onUse)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = model.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text =
                    stringResource(
                        if (role in model.recommendedFor) Res.string.ai_model_recommended else Res.string.ai_model_alternative,
                    ) + if (model.hearing && role == AiRole.DICTATION) " · " + stringResource(Res.string.ai_model_hearing) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (inUse && state == TranslatorModelState.Ready) {
                Text(stringResource(Res.string.ai_model_in_use), style = MaterialTheme.typography.labelMedium)
            }
        }
        if (description.isNotBlank()) Text(text = description, style = MaterialTheme.typography.bodySmall)
        Text(
            text = stringResource(Res.string.ai_model_specs, model.sizeMb, model.ramMb, model.minDeviceRamGb),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.ai_model_license_line, model.license.label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (model.pageUrl.isNotBlank()) {
            TextButton(onClick = { uriHandler.openUri(model.pageUrl) }) { Text(stringResource(Res.string.ai_model_page)) }
        }
        when {
            !model.engineReady ->
                Text(stringResource(Res.string.ai_model_soon), style = MaterialTheme.typography.bodySmall)

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
