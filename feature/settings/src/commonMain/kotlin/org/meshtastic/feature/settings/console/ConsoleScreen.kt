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
package org.meshtastic.feature.settings.console

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.console_input
import org.meshtastic.core.resources.console_intro
import org.meshtastic.core.resources.console_next
import org.meshtastic.core.resources.console_prev
import org.meshtastic.core.resources.console_send
import org.meshtastic.core.resources.console_info_body
import org.meshtastic.core.resources.console_title
import org.meshtastic.core.ui.component.Button
import org.meshtastic.core.ui.component.FloatingBarUnderlap
import org.meshtastic.core.ui.component.InfoButton
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.component.SosBarScreen
import org.meshtastic.core.ui.component.floatingBarEndPadding
import org.meshtastic.core.ui.viewmodel.RadioChooserViewModel
import org.meshtastic.feature.settings.mtsw.RadioBridgeViewModel

private const val MAX_SUGGESTIONS = 8

/**
 * The console (MT_SW): typed commands in the language of the app, with choices offered while typing and the earlier
 * lines one tap away. Every command comes from the one registry (see [buildConsoleRegistry]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConsoleScreen(
    onBack: () -> Unit,
    viewModel: ConsoleViewModel = koinViewModel(),
    chooser: RadioChooserViewModel = koinViewModel(),
    bridge: RadioBridgeViewModel = koinViewModel(),
) {
    val lines by viewModel.lines.collectAsStateWithLifecycle()
    val input by viewModel.input.collectAsStateWithLifecycle()
    val labels = rememberConsoleLabels()
    val registry =
        remember(labels, viewModel, chooser, bridge) {
            buildConsoleRegistry(
                ConsoleEnvironment(
                    radios = { chooser.state.value.radios },
                    choose = chooser::choose,
                    disconnect = { id ->
                        (id ?: chooser.state.value.radios.firstOrNull { it.active }?.id)?.let(chooser::disconnect)
                    },
                    remove = chooser::remove,
                    connection = viewModel::connection,
                    nodes = viewModel::nodes,
                    bridge = { bridge.state.value },
                    updateBridge = bridge::update,
                ),
                labels,
            )
        }
    val listState = rememberLazyListState()
    LaunchedEffect(lines.size) { if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1) }
    val suggestions = registry.suggestions(input).take(MAX_SUGGESTIONS)

    FloatingBarUnderlap()
    Scaffold(
        topBar = {
            MainAppBar(
                sosScreen = SosBarScreen.SETTINGS,
                title = stringResource(Res.string.console_title),
                canNavigateUp = true,
                onNavigateUp = onBack,
                ourNode = null,
                showNodeChip = false,
                actions = { InfoButton(Res.string.console_title, Res.string.console_info_body) },
                onClickChip = {},
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp).floatingBarEndPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = stringResource(Res.string.console_intro), style = MaterialTheme.typography.bodySmall)
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(lines) { line ->
                    Text(text = line, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { suggestion ->
                    AssistChip(
                        onClick = { viewModel.setInput("$suggestion ") },
                        label = { Text(suggestion, fontFamily = FontFamily.Monospace) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = viewModel::setInput,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(Res.string.console_input)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.run(registry) }),
                )
                Button(onClick = { viewModel.run(registry) }) { Text(stringResource(Res.string.console_send)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = viewModel::previous) { Text(stringResource(Res.string.console_prev)) }
                TextButton(onClick = viewModel::next) { Text(stringResource(Res.string.console_next)) }
            }
        }
    }
}
