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
@file:Suppress("ModifierMissing", "LongMethod")

package org.meshtastic.feature.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.log.LogInsights
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.link_quality_item
import org.meshtastic.core.resources.link_quality_none
import org.meshtastic.core.resources.link_quality_title
import org.meshtastic.core.resources.log_insights_ago
import org.meshtastic.core.resources.log_insights_clear
import org.meshtastic.core.resources.log_insights_counts
import org.meshtastic.core.resources.log_insights_map
import org.meshtastic.core.resources.log_insights_none
import org.meshtastic.core.resources.log_insights_since
import org.meshtastic.core.resources.log_insights_title
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.ModalBottomSheet
import org.meshtastic.core.ui.icon.List
import org.meshtastic.core.ui.icon.MeshHub
import org.meshtastic.core.ui.icon.MeshtasticIcons

/**
 * MT_SW: two entries in the log settings, each opening a window. One sums up what went wrong since the app started
 * (warnings and errors grouped by what they say, and how smooth the map was); the other shows how the link to each
 * device behaved (timeouts, closed links, errors). Both read the app's own log as it is written.
 */
@Composable
fun LogInsightsEntries() {
    var showSummary by remember { mutableStateOf(false) }
    var showLinks by remember { mutableStateOf(false) }
    ListItem(text = stringResource(Res.string.log_insights_title), leadingIcon = MeshtasticIcons.List) {
        Logger.i { "Podsumowanie logów: okno otwarte" }
        showSummary = true
    }
    ListItem(text = stringResource(Res.string.link_quality_title), leadingIcon = MeshtasticIcons.MeshHub) {
        Logger.i { "Jakość łącza: okno otwarte" }
        showLinks = true
    }
    if (showSummary) LogSummaryWindow(onDismiss = { showSummary = false })
    if (showLinks) LinkQualityWindow(onDismiss = { showLinks = false })
}

private const val MILLIS_IN_MINUTE = 60_000L
private const val MAX_SHOWN_GROUPS = 30

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogSummaryWindow(onDismiss: () -> Unit) {
    val summary by LogInsights.summary.collectAsStateWithLifecycle()
    val now = nowMillis
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(Res.string.log_insights_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(Res.string.log_insights_since, minutesAgo(now, summary.sinceMillis)),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(Res.string.log_insights_counts, summary.errors, summary.warnings),
                style = MaterialTheme.typography.titleSmall,
            )
            if (summary.map.windows > 0) {
                Text(
                    text =
                    stringResource(
                        Res.string.log_insights_map,
                        summary.map.slowFrames,
                        summary.map.windows,
                        summary.map.longestMs,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            val groups = summary.groups.values.sortedByDescending { it.count }.take(MAX_SHOWN_GROUPS)
            if (groups.isEmpty()) {
                Text(stringResource(Res.string.log_insights_none), style = MaterialTheme.typography.bodyMedium)
            }
            groups.forEach { group ->
                val mark = if (group.severity >= Severity.Error) "E" else "W"
                Text(
                    text = "$mark  ${group.count}×  ${group.text}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(Res.string.log_insights_ago, minutesAgo(now, group.lastMillis)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(
                onClick = {
                    Logger.i { "Podsumowanie logów: liczenie wyczyszczone" }
                    LogInsights.clear()
                },
            ) {
                Text(stringResource(Res.string.log_insights_clear))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkQualityWindow(onDismiss: () -> Unit) {
    val summary by LogInsights.summary.collectAsStateWithLifecycle()
    val now = nowMillis
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(Res.string.link_quality_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(Res.string.log_insights_since, minutesAgo(now, summary.sinceMillis)),
                style = MaterialTheme.typography.bodySmall,
            )
            val links = summary.links.values.sortedByDescending { it.timeouts + it.closed + it.errors }
            if (links.isEmpty()) {
                Text(stringResource(Res.string.link_quality_none), style = MaterialTheme.typography.bodyMedium)
            }
            links.forEach { link ->
                Text(link.id, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(Res.string.link_quality_item, link.timeouts, link.closed, link.errors),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(Res.string.log_insights_ago, minutesAgo(now, link.lastMillis)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun minutesAgo(now: Long, then: Long): Int = ((now - then).coerceAtLeast(0L) / MILLIS_IN_MINUTE).toInt()
