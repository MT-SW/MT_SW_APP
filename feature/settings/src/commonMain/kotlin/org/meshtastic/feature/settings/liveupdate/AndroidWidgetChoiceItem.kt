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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.effectiveLongName
import org.meshtastic.core.repository.LiveUpdateMode
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.widget_adaptive_mesh_status_shown
import org.meshtastic.core.resources.widget_adaptive_title
import org.meshtastic.core.resources.widget_adaptive_tracking_shown
import org.meshtastic.core.ui.component.BasicListItem
import org.meshtastic.core.ui.component.icon
import org.meshtastic.core.ui.icon.ChevronRight
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Notifications

/**
 * MT_SW: what the Android widget shows (the whole mesh or one node), chosen here and apart from the adaptive
 * notification, so the widget has its own settings.
 */
@Composable
fun AndroidWidgetChoiceItem(modifier: Modifier = Modifier) {
    val uiPrefs: UiPrefs = koinInject()
    val viewModel: LiveUpdateViewModel = koinViewModel()
    val nodeNum by uiPrefs.androidWidgetNode.collectAsStateWithLifecycle()
    val candidates by viewModel.candidates.collectAsStateWithLifecycle()
    var showChooser by rememberSaveable { mutableStateOf(false) }
    val mode = if (nodeNum == 0) LiveUpdateMode.MeshStatus else LiveUpdateMode.TrackNode(nodeNum)
    val summary =
        if (nodeNum == 0) {
            stringResource(Res.string.widget_adaptive_mesh_status_shown)
        } else {
            val name =
                candidates.firstOrNull { it.num == nodeNum }?.effectiveLongName
                    ?: NodeAddress.numToDefaultId(nodeNum)
            stringResource(Res.string.widget_adaptive_tracking_shown, name)
        }
    BasicListItem(
        text = stringResource(Res.string.widget_adaptive_title),
        supportingText = summary,
        leadingIcon = MeshtasticIcons.Notifications,
        trailingContent = MeshtasticIcons.ChevronRight.icon(),
        onClick = { showChooser = true },
        modifier = modifier,
    )
    if (showChooser) {
        LiveUpdateChooserSheet(
            mode = mode,
            candidates = candidates,
            onStart = {
                uiPrefs.setAndroidWidgetNode((it as? LiveUpdateMode.TrackNode)?.nodeNum ?: 0)
                showChooser = false
            },
            onStop = {
                uiPrefs.setAndroidWidgetNode(0)
                showChooser = false
            },
            onDismiss = { showChooser = false },
            widget = true,
        )
    }
}
