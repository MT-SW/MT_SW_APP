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
package org.meshtastic.feature.settings.radio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Node
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.auto_clean_nodes_check_interval_days
import org.meshtastic.core.resources.auto_clean_nodes_description
import org.meshtastic.core.resources.auto_clean_nodes_enabled
import org.meshtastic.core.resources.auto_clean_nodes_inactivity_days
import org.meshtastic.core.resources.auto_clean_nodes_title
import org.meshtastic.core.resources.clean_app_node_db_button
import org.meshtastic.core.resources.clean_app_node_db_description
import org.meshtastic.core.resources.clean_app_node_db_keep_favorites
import org.meshtastic.core.resources.clean_mismatched_key_nodes
import org.meshtastic.core.resources.clean_node_database_description
import org.meshtastic.core.resources.clean_node_database_title
import org.meshtastic.core.resources.clean_nodes_older_than
import org.meshtastic.core.resources.clean_now
import org.meshtastic.core.resources.clean_unknown_nodes
import org.meshtastic.core.resources.clean_unknown_nodes_any_date
import org.meshtastic.core.resources.nodes_queued_for_deletion
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.component.NodeChip

/**
 * Composable screen for cleaning the node database. Allows users to specify criteria for deleting nodes. The list of
 * nodes to be deleted updates automatically as filter criteria change.
 */
@Composable
fun CleanNodeDatabaseScreen(viewModel: CleanNodeDatabaseViewModel, onBack: () -> Unit) {
    val olderThanDays by viewModel.olderThanDays.collectAsStateWithLifecycle()
    val onlyUnknownNodes by viewModel.onlyUnknownNodes.collectAsStateWithLifecycle()
    val ignoreDate by viewModel.ignoreDate.collectAsStateWithLifecycle()
    val onlyMismatchedKeys by viewModel.onlyMismatchedKeys.collectAsStateWithLifecycle()
    val keepFavorites by viewModel.keepFavorites.collectAsStateWithLifecycle()
    val nodesToDelete by viewModel.nodesToDelete.collectAsStateWithLifecycle()
    val autoCleanEnabled by viewModel.autoCleanEnabled.collectAsStateWithLifecycle()
    val autoCleanInactivityDays by viewModel.autoCleanInactivityDays.collectAsStateWithLifecycle()
    val autoCleanCheckIntervalDays by viewModel.autoCleanCheckIntervalDays.collectAsStateWithLifecycle()

    SideEffect(olderThanDays, onlyUnknownNodes, ignoreDate, onlyMismatchedKeys) { viewModel.getNodesToDelete() }

    Scaffold(
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.clean_node_database_title),
                ourNode = null,
                showNodeChip = false,
                canNavigateUp = true,
                onNavigateUp = onBack,
                actions = {},
                onClickChip = {},
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState())) {
            AutoCleanNodesSection(
                autoCleanEnabled = autoCleanEnabled,
                autoCleanInactivityDays = autoCleanInactivityDays,
                autoCleanCheckIntervalDays = autoCleanCheckIntervalDays,
                onAutoCleanEnabledChanged = viewModel::onAutoCleanEnabledChanged,
                onAutoCleanInactivityDaysChanged = viewModel::onAutoCleanInactivityDaysChanged,
                onAutoCleanCheckIntervalDaysChanged = viewModel::onAutoCleanCheckIntervalDaysChanged,
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(Res.string.clean_node_database_description), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.clean_mismatched_key_nodes),
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = onlyMismatchedKeys, onCheckedChange = viewModel::onOnlyMismatchedKeysChanged)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!onlyMismatchedKeys) {
                if (!(onlyUnknownNodes && ignoreDate)) {
                    DaysThresholdFilter(
                        olderThanDays = olderThanDays,
                        onlyUnknownNodes = onlyUnknownNodes,
                        onDaysChanged = viewModel::onOlderThanDaysChanged,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                UnknownNodesFilter(
                    onlyUnknownNodes = onlyUnknownNodes,
                    onCheckedChanged = viewModel::onOnlyUnknownNodesChanged,
                )

                if (onlyUnknownNodes) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(Res.string.clean_unknown_nodes_any_date),
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = ignoreDate, onCheckedChange = viewModel::onIgnoreDateChanged)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            NodesDeletionPreview(nodesToDelete = nodesToDelete)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { if (nodesToDelete.isNotEmpty()) viewModel.requestCleanNodes() },
                modifier = Modifier.fillMaxWidth(),
                enabled = nodesToDelete.isNotEmpty(),
            ) {
                Text(stringResource(Res.string.clean_now))
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(Res.string.clean_app_node_db_description), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(Res.string.clean_app_node_db_keep_favorites), modifier = Modifier.weight(1f))
                Switch(checked = keepFavorites, onCheckedChange = viewModel::onKeepFavoritesChanged)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = viewModel::requestClearAppNodeDatabase, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.clean_app_node_db_button))
            }
        }
    }
}

private const val MIN_UNKNOWN_DAYS_THRESHOLD = 0f
private const val MIN_KNOWN_DAYS_THRESHOLD = 7f
private const val MAX_DAYS_THRESHOLD = 365f

private const val MIN_AUTO_CLEAN_INACTIVITY_DAYS = 1f
private const val MAX_AUTO_CLEAN_INACTIVITY_DAYS = 90f
private const val MIN_AUTO_CLEAN_CHECK_INTERVAL_DAYS = 1f
private const val MAX_AUTO_CLEAN_CHECK_INTERVAL_DAYS = 30f

/**
 * Composable for the automatic cleanup section: a switch to enable/disable it, and — when enabled — sliders for the
 * inactivity threshold (in days) after which a node is removed, and for how often the check itself runs.
 *
 * @param autoCleanEnabled Whether automatic cleanup is enabled.
 * @param autoCleanInactivityDays The inactivity threshold, in days.
 * @param autoCleanCheckIntervalDays How often, in days, the background check runs.
 * @param onAutoCleanEnabledChanged Callback for when the enabled state changes.
 * @param onAutoCleanInactivityDaysChanged Callback for when the inactivity threshold changes.
 * @param onAutoCleanCheckIntervalDaysChanged Callback for when the check interval changes.
 */
@Composable
private fun AutoCleanNodesSection(
    autoCleanEnabled: Boolean,
    autoCleanInactivityDays: Int,
    autoCleanCheckIntervalDays: Int,
    onAutoCleanEnabledChanged: (Boolean) -> Unit,
    onAutoCleanInactivityDaysChanged: (Int) -> Unit,
    onAutoCleanCheckIntervalDaysChanged: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.auto_clean_nodes_title), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(stringResource(Res.string.auto_clean_nodes_description), style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.auto_clean_nodes_enabled))
            Spacer(Modifier.weight(1f))
            Switch(checked = autoCleanEnabled, onCheckedChange = onAutoCleanEnabledChanged)
        }

        if (autoCleanEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = stringResource(Res.string.auto_clean_nodes_inactivity_days, autoCleanInactivityDays),
            )
            Slider(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                value = autoCleanInactivityDays.toFloat(),
                onValueChange = { onAutoCleanInactivityDaysChanged(it.toInt()) },
                valueRange = MIN_AUTO_CLEAN_INACTIVITY_DAYS..MAX_AUTO_CLEAN_INACTIVITY_DAYS,
                steps = (MAX_AUTO_CLEAN_INACTIVITY_DAYS - MIN_AUTO_CLEAN_INACTIVITY_DAYS - 1).toInt(),
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = stringResource(Res.string.auto_clean_nodes_check_interval_days, autoCleanCheckIntervalDays),
            )
            Slider(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                value = autoCleanCheckIntervalDays.toFloat(),
                onValueChange = { onAutoCleanCheckIntervalDaysChanged(it.toInt()) },
                valueRange = MIN_AUTO_CLEAN_CHECK_INTERVAL_DAYS..MAX_AUTO_CLEAN_CHECK_INTERVAL_DAYS,
                steps = (MAX_AUTO_CLEAN_CHECK_INTERVAL_DAYS - MIN_AUTO_CLEAN_CHECK_INTERVAL_DAYS - 1).toInt(),
            )
        }
    }
}

/**
 * Composable for the "older than X days" filter. This filter is always active.
 *
 * @param olderThanDays The number of days for the filter.
 * @param onlyUnknownNodes Whether the "only unknown nodes" filter is enabled.
 * @param onDaysChanged Callback for when the number of days changes.
 */
@Composable
private fun DaysThresholdFilter(olderThanDays: Float, onlyUnknownNodes: Boolean, onDaysChanged: (Float) -> Unit) {
    val valueRange =
        if (onlyUnknownNodes) {
            MIN_UNKNOWN_DAYS_THRESHOLD..MAX_DAYS_THRESHOLD
        } else {
            MIN_KNOWN_DAYS_THRESHOLD..MAX_DAYS_THRESHOLD
        }
    val steps = (valueRange.endInclusive - valueRange.start - 1).toInt().coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(bottom = 8.dp),
            text = stringResource(Res.string.clean_nodes_older_than, olderThanDays.toInt()),
        )
        Slider(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            value = olderThanDays,
            onValueChange = onDaysChanged,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

/**
 * Composable for the "only unknown nodes" filter.
 *
 * @param onlyUnknownNodes Whether the filter is enabled.
 * @param onCheckedChanged Callback for when the checked state changes.
 */
@Composable
private fun UnknownNodesFilter(onlyUnknownNodes: Boolean, onCheckedChanged: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(Res.string.clean_unknown_nodes))
        Spacer(Modifier.weight(1f))
        Switch(checked = onlyUnknownNodes, onCheckedChange = onCheckedChanged)
    }
}

/**
 * Composable for displaying the list of nodes queued for deletion.
 *
 * @param nodesToDelete The list of nodes to be deleted.
 */
@Composable
private fun NodesDeletionPreview(nodesToDelete: List<Node>) {
    Text(
        stringResource(Res.string.nodes_queued_for_deletion, nodesToDelete.size),
        modifier = Modifier.padding(bottom = 16.dp),
    )
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center,
    ) {
        nodesToDelete.forEach { node -> NodeChip(node = node, modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)) }
    }
}
