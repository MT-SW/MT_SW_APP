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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ai_tab_ai
import org.meshtastic.core.resources.ai_tab_assistant
import org.meshtastic.core.resources.ai_tab_translator
import org.meshtastic.core.resources.assistant_history
import org.meshtastic.core.resources.assistant_autoscroll
import org.meshtastic.core.resources.assistant_autoscroll_hint
import org.meshtastic.core.resources.assistant_info_body
import org.meshtastic.core.resources.assistant_info_title
import org.meshtastic.core.resources.translator_open
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.icon.History
import org.meshtastic.core.ui.icon.Info
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.feature.messaging.translation.AssistantViewModel
import org.meshtastic.feature.messaging.translation.TranslatorViewModel

/**
 * The AI tab: the translator, the assistant, or both under two sub-tabs, depending on which modes are switched on.
 */
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
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(Res.string.assistant_info_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(Res.string.assistant_info_body))
                    val autoScroll by uiPrefs.assistantAutoScroll.collectAsStateWithLifecycle()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.assistant_autoscroll), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(Res.string.assistant_autoscroll_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = autoScroll, onCheckedChange = { uiPrefs.setAssistantAutoScroll(it) })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("OK") } },
        )
    }
    val savedChats by assistantViewModel.saved.collectAsStateWithLifecycle()
    if (showHistory) {
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
                    if (showAssistant && assistantOn) {
                        IconButton(onClick = { showInfo = true }) {
                            Icon(
                                MeshtasticIcons.Info,
                                contentDescription = stringResource(Res.string.assistant_info_title),
                            )
                        }
                        IconButton(
                            onClick = {
                                assistantViewModel.refreshSaved()
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
