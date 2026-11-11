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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.widget_content_favorites
import org.meshtastic.core.resources.widget_content_info
import org.meshtastic.core.resources.widget_content_network
import org.meshtastic.core.resources.widget_content_own
import org.meshtastic.core.resources.widget_content_second_title
import org.meshtastic.core.resources.widget_content_small_title
import org.meshtastic.core.ui.component.BasicListItem
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.component.icon
import org.meshtastic.core.ui.icon.ChevronRight
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Notifications

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
    BasicListItem(
        text = stringResource(Res.string.widget_content_small_title),
        supportingText = smallNames[small.coerceIn(0, 2)],
        leadingIcon = MeshtasticIcons.Notifications,
        trailingContent = MeshtasticIcons.ChevronRight.icon(),
        onClick = { chooser = 1 },
        modifier = modifier,
    )
    BasicListItem(
        text = stringResource(Res.string.widget_content_second_title),
        supportingText = secondNames[second.coerceIn(0, 1)],
        leadingIcon = MeshtasticIcons.Notifications,
        trailingContent = MeshtasticIcons.ChevronRight.icon(),
        onClick = { chooser = 2 },
    )
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
}
