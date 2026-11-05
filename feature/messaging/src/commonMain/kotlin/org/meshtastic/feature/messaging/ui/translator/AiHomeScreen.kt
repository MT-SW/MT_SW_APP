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
@file:Suppress("LongMethod", "ModifierMissing", "ViewModelForwarding", "UnnecessaryLaunchedEffect")

package org.meshtastic.feature.messaging.ui.translator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ai_tab_ai
import org.meshtastic.core.resources.ai_tab_assistant
import org.meshtastic.core.resources.ai_tab_translator
import org.meshtastic.core.resources.assistant_autoscroll
import org.meshtastic.core.resources.assistant_autoscroll_hint
import org.meshtastic.core.resources.assistant_header_collapse
import org.meshtastic.core.resources.assistant_header_collapse_hint
import org.meshtastic.core.resources.assistant_history
import org.meshtastic.core.resources.assistant_info_body
import org.meshtastic.core.resources.assistant_info_title
import org.meshtastic.core.resources.compat_run_again
import org.meshtastic.core.resources.translator_info_body
import org.meshtastic.core.resources.translator_info_title
import org.meshtastic.core.resources.translator_open
import org.meshtastic.core.resources.translator_sessions_empty
import org.meshtastic.core.resources.translator_sessions_new
import org.meshtastic.core.resources.translator_sessions_title
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.icon.History
import org.meshtastic.core.ui.icon.Info
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.feature.messaging.translation.AssistantViewModel
import org.meshtastic.feature.messaging.translation.TranslatorViewModel

/** The AI tab: the translator, the assistant, or both under two sub-tabs, depending on which modes are switched on. */
@Composable
fun AiHomeScreen(
    translatorViewModel: TranslatorViewModel,
    assistantViewModel: AssistantViewModel,
    onBack: () -> Unit,
    canNavigateUp: Boolean,
) {
    val uiPrefs = koinInject<UiPrefs>()
    val translatorOn by uiPrefs.translatorEnabled.collectAsStateWithLifecycle()
    val assistantOn by uiPrefs.assistantEnabled.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val both = translatorOn && assistantOn
    // With a single mode on there are no sub-tabs; the one that is on is shown.
    val showAssistant = if (both) selected == 1 else assistantOn

    var showHistory by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    val modelReady by assistantViewModel.modelReady.collectAsStateWithLifecycle()
    val compat by assistantViewModel.compat.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(modelReady, showAssistant) {
        if (modelReady && showAssistant) assistantViewModel.offerCompat()
    }
    CompatTestDialogs(compat = compat, viewModel = assistantViewModel)
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = {
                Text(
                    stringResource(
                        if (showAssistant) Res.string.assistant_info_title else Res.string.translator_info_title,
                    ),
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(
                            if (showAssistant) Res.string.assistant_info_body else Res.string.translator_info_body,
                        ),
                    )
                    val autoScroll by uiPrefs.assistantAutoScroll.collectAsStateWithLifecycle()
                    if (showAssistant) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(Res.string.assistant_autoscroll),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    stringResource(Res.string.assistant_autoscroll_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = autoScroll, onCheckedChange = { uiPrefs.setAssistantAutoScroll(it) })
                        }
                        val headerCollapse by uiPrefs.assistantHeaderCollapse.collectAsStateWithLifecycle()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(Res.string.assistant_header_collapse),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    stringResource(Res.string.assistant_header_collapse_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = headerCollapse,
                                onCheckedChange = { uiPrefs.setAssistantHeaderCollapse(it) },
                            )
                        }
                        if (modelReady) {
                            TextButton(
                                onClick = {
                                    showInfo = false
                                    assistantViewModel.requestCompat()
                                },
                            ) {
                                Text(stringResource(Res.string.compat_run_again))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("OK") } },
        )
    }
    val savedChats by assistantViewModel.saved.collectAsStateWithLifecycle()
    val savedSessions by translatorViewModel.saved.collectAsStateWithLifecycle()
    if (showHistory && showAssistant) {
        AssistantHistoryDialog(
            chats = savedChats,
            onOpen = {
                assistantViewModel.open(it)
                selected = 1
                showHistory = false
            },
            onDelete = { assistantViewModel.deleteSaved(it.id) },
            onNew = {
                assistantViewModel.startNew()
                showHistory = false
            },
            onDismiss = { showHistory = false },
        )
    } else if (showHistory) {
        // The translator's own history: saved sessions with their translations and the results of its commands.
        AssistantHistoryDialog(
            chats = savedSessions,
            onOpen = {
                translatorViewModel.open(it)
                selected = 0
                showHistory = false
            },
            onDelete = { translatorViewModel.deleteSaved(it.id) },
            onNew = {
                translatorViewModel.startNew()
                showHistory = false
            },
            onDismiss = { showHistory = false },
            title = Res.string.translator_sessions_title,
            empty = Res.string.translator_sessions_empty,
            newLabel = Res.string.translator_sessions_new,
        )
    }

    Scaffold(
        topBar = {
            MainAppBar(
                title =
                stringResource(
                    when {
                        both -> Res.string.ai_tab_ai
                        assistantOn -> Res.string.ai_tab_assistant
                        else -> Res.string.translator_open
                    },
                ),
                canNavigateUp = canNavigateUp,
                onNavigateUp = onBack,
                ourNode = null,
                showNodeChip = false,
                actions = {
                    if ((showAssistant && assistantOn) || (!showAssistant && translatorOn)) {
                        IconButton(onClick = { showInfo = true }) {
                            Icon(
                                MeshtasticIcons.Info,
                                contentDescription = stringResource(Res.string.assistant_info_title),
                            )
                        }
                        IconButton(
                            onClick = {
                                if (showAssistant) {
                                    assistantViewModel.refreshSaved()
                                } else {
                                    translatorViewModel.refreshSaved()
                                }
                                showHistory = true
                            },
                        ) {
                            Icon(
                                MeshtasticIcons.History,
                                contentDescription = stringResource(Res.string.assistant_history),
                            )
                        }
                    }
                },
                onClickChip = {},
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (both) {
                PrimaryTabRow(selectedTabIndex = selected) {
                    Tab(
                        selected = selected == 0,
                        onClick = { selected = 0 },
                        text = { Text(stringResource(Res.string.ai_tab_translator)) },
                    )
                    Tab(
                        selected = selected == 1,
                        onClick = { selected = 1 },
                        text = { Text(stringResource(Res.string.ai_tab_assistant)) },
                    )
                }
            }
            if (showAssistant) {
                AssistantContent(viewModel = assistantViewModel, modifier = Modifier.weight(1f))
            } else {
                TranslatorContent(viewModel = translatorViewModel, modifier = Modifier.weight(1f))
            }
        }
    }
}
