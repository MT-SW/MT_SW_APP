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
package org.meshtastic.feature.settings.liveupdate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import org.meshtastic.core.model.effectiveLongName
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.widget_content_expand_info
import org.meshtastic.core.resources.widget_content_favorites
import org.meshtastic.core.resources.widget_content_info
import org.meshtastic.core.resources.widget_content_network
import org.meshtastic.core.resources.widget_content_own
import org.meshtastic.core.resources.widget_content_second_title
import org.meshtastic.core.resources.widget_content_small_title
import org.meshtastic.core.resources.widget_others_all
import org.meshtastic.core.resources.widget_others_clear
import org.meshtastic.core.resources.widget_others_info
import org.meshtastic.core.resources.widget_others_latest
import org.meshtastic.core.resources.widget_others_pick
import org.meshtastic.core.resources.widget_others_pick_title
import org.meshtastic.core.resources.widget_others_picked_summary
import org.meshtastic.core.resources.widget_others_search
import org.meshtastic.core.resources.widget_others_title
import org.meshtastic.core.ui.component.BasicListItem
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.component.icon
import org.meshtastic.core.ui.icon.ChevronRight
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Notifications
import org.meshtastic.core.ui.util.rememberWidgetSizes

/**
 * MT_SW: what the Android widget shows in its sizes, like the iPhone widget: the smallest one (our node, favorites or
 * the whole network in turn) and the second area of the bigger ones (favorites or the whole network in turn).
 */
@Suppress("MultipleEmitters")
@Composable
fun AndroidWidgetContentItems(modifier: Modifier = Modifier) {
    val uiPrefs: UiPrefs = koinInject()
    val small by uiPrefs.androidWidgetSmall.collectAsStateWithLifecycle()
    val second by uiPrefs.androidWidgetSecond.collectAsStateWithLifecycle()
    var chooser by rememberSaveable { mutableIntStateOf(0) }
    val own = stringResource(Res.string.widget_content_own)
    val favorites = stringResource(Res.string.widget_content_favorites)
    val network = stringResource(Res.string.widget_content_network)
    val smallNames = listOf(own, favorites, network)
    val secondNames = listOf(favorites, network)
    // Only the options of the sizes the widget really has on the home screen; with nothing placed yet, the basic one.
    val sizes = rememberWidgetSizes()
    val showSmall = !sizes.known || sizes.medium || !(sizes.large || sizes.extraLarge)
    val showSecond = !sizes.known || sizes.large
    Text(
        text = stringResource(Res.string.widget_content_expand_info),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    if (showSmall) {
        BasicListItem(
            text = stringResource(Res.string.widget_content_small_title),
            supportingText = smallNames[small.coerceIn(0, 2)],
            leadingIcon = MeshtasticIcons.Notifications,
            trailingContent = MeshtasticIcons.ChevronRight.icon(),
            onClick = { chooser = 1 },
            modifier = modifier,
        )
    }
    if (showSecond) {
        BasicListItem(
            text = stringResource(Res.string.widget_content_second_title),
            supportingText = secondNames[second.coerceIn(0, 1)],
            leadingIcon = MeshtasticIcons.Notifications,
            trailingContent = MeshtasticIcons.ChevronRight.icon(),
            onClick = { chooser = 2 },
        )
    }
    if (chooser != 0) {
        val isSmall = chooser == 1
        MeshtasticDialog(
            title =
            stringResource(
                if (isSmall) Res.string.widget_content_small_title else Res.string.widget_content_second_title,
            ),
            message = stringResource(Res.string.widget_content_info),
            choices =
            (if (isSmall) smallNames else secondNames)
                .mapIndexed { index, name ->
                    name to
                        {
                            if (isSmall) {
                                uiPrefs.setAndroidWidgetSmall(index)
                            } else {
                                uiPrefs.setAndroidWidgetSecond(index)
                            }
                        }
                }
                .toMap(),
            onDismiss = { chooser = 0 },
        )
    }
    WidgetOthersItem()
}

private val OTHERS_LIMITS = listOf(10, 20, 50, 100)

/** MT_SW: how many other nodes the widget rotates through: all, the latest N, or nodes picked by hand. */
@Composable
private fun WidgetOthersItem() {
    val uiPrefs: UiPrefs = koinInject()
    val limit by uiPrefs.widgetOthersLimit.collectAsStateWithLifecycle()
    val picked by uiPrefs.widgetOthersPicked.collectAsStateWithLifecycle()
    var dialog by rememberSaveable { mutableIntStateOf(0) }
    val summary =
        when {
            picked.isNotEmpty() -> stringResource(Res.string.widget_others_picked_summary, picked.size)
            limit > 0 -> stringResource(Res.string.widget_others_latest, limit)
            else -> stringResource(Res.string.widget_others_all)
        }
    BasicListItem(
        text = stringResource(Res.string.widget_others_title),
        supportingText = summary,
        leadingIcon = MeshtasticIcons.Notifications,
        trailingContent = MeshtasticIcons.ChevronRight.icon(),
        onClick = { dialog = 1 },
    )
    if (dialog == 1) {
        val choices = linkedMapOf<String, () -> Unit>()
        choices[stringResource(Res.string.widget_others_all)] = {
            uiPrefs.setWidgetOthersLimit(0)
            uiPrefs.setWidgetOthersPicked(emptySet())
        }
        OTHERS_LIMITS.forEach { n ->
            choices[stringResource(Res.string.widget_others_latest, n)] = {
                uiPrefs.setWidgetOthersLimit(n)
                uiPrefs.setWidgetOthersPicked(emptySet())
            }
        }
        choices[stringResource(Res.string.widget_others_pick)] = { dialog = 2 }
        MeshtasticDialog(
            title = stringResource(Res.string.widget_others_title),
            message = stringResource(Res.string.widget_others_info),
            choices = choices,
            onDismiss = { if (dialog == 1) dialog = 0 },
        )
    }
    if (dialog == 2) WidgetOthersPicker(picked = picked, onDone = { dialog = 0 })
}

@Composable
private fun WidgetOthersPicker(picked: Set<Int>, onDone: () -> Unit) {
    val uiPrefs: UiPrefs = koinInject()
    val nodes: NodeRepository = koinInject()
    val all by nodes.nodeDBbyNum.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val list =
        remember(all, query) {
            all.values
                .filter { !it.isFavorite && it.effectiveLongName.contains(query.trim(), ignoreCase = true) }
                .sortedBy { it.effectiveLongName.lowercase() }
        }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(stringResource(Res.string.widget_others_pick_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text(stringResource(Res.string.widget_others_search)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn {
                    items(list, key = { it.num }) { node ->
                        val on = node.num in picked
                        Row(
                            modifier =
                            Modifier.fillMaxWidth()
                                .clickable {
                                    uiPrefs.setWidgetOthersPicked(if (on) picked - node.num else picked + node.num)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = on, onCheckedChange = null)
                            Text(node.effectiveLongName, modifier = Modifier.padding(start = 12.dp), maxLines = 1)
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { uiPrefs.setWidgetOthersPicked(emptySet()) }) {
                Text(stringResource(Res.string.widget_others_clear))
            }
        },
        confirmButton = { TextButton(onClick = onDone) { Text(stringResource(Res.string.close)) } },
    )
}
