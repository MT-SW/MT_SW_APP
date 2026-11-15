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

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.NodePresence
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.presence_ask_summary
import org.meshtastic.core.resources.presence_ask_title
import org.meshtastic.core.resources.presence_color_green
import org.meshtastic.core.resources.presence_color_theme
import org.meshtastic.core.resources.presence_color_title
import org.meshtastic.core.resources.presence_expiry_hours
import org.meshtastic.core.resources.presence_expiry_min
import org.meshtastic.core.resources.presence_expiry_summary
import org.meshtastic.core.resources.presence_expiry_title
import org.meshtastic.core.resources.presence_icon_dot
import org.meshtastic.core.resources.presence_icon_person
import org.meshtastic.core.resources.presence_icon_ring
import org.meshtastic.core.resources.presence_icon_title
import org.meshtastic.core.resources.presence_replace_favorite
import org.meshtastic.core.resources.presence_replace_muted
import org.meshtastic.core.resources.presence_replace_none
import org.meshtastic.core.resources.presence_replace_title
import org.meshtastic.core.resources.presence_replace_unmessageable
import org.meshtastic.core.resources.presence_show
import org.meshtastic.core.resources.presence_show_summary
import org.meshtastic.core.ui.component.DropDownPreference
import org.meshtastic.core.ui.component.SwitchListItem
import org.meshtastic.core.ui.icon.DisplaySettings
import org.meshtastic.core.ui.icon.MeshtasticIcons

private const val MINUTES_PER_HOUR = 60

/**
 * MT_SW: what the activity status icon of the nodes looks like and how it behaves (see `NodePresence`). Every choice is
 * one of the shared options, so it works the same on every platform; [onChange] saves one option.
 */
@Composable
fun PresenceSettings(options: UiOptions, onChange: (String, Int) -> Unit) {
    val shown = options.flag(UiOptions.PRESENCE_SHOW)
    SwitchListItem(
        text = stringResource(Res.string.presence_show),
        supportingText = stringResource(Res.string.presence_show_summary),
        leadingIcon = MeshtasticIcons.DisplaySettings,
        checked = shown,
        onClick = { onChange(UiOptions.PRESENCE_SHOW, if (shown) 0 else 1) },
    )
    DropDownPreference(
        title = stringResource(Res.string.presence_icon_title),
        enabled = shown,
        items =
        listOf(
            UiOptions.PRESENCE_ICON_DOT to stringResource(Res.string.presence_icon_dot),
            UiOptions.PRESENCE_ICON_PERSON to stringResource(Res.string.presence_icon_person),
            UiOptions.PRESENCE_ICON_RING to stringResource(Res.string.presence_icon_ring),
        ),
        selectedItem = options[UiOptions.PRESENCE_ICON],
        onItemSelected = { onChange(UiOptions.PRESENCE_ICON, it) },
    )
    DropDownPreference(
        title = stringResource(Res.string.presence_replace_title),
        enabled = shown,
        items =
        listOf(
            UiOptions.PRESENCE_REPLACE_NONE to stringResource(Res.string.presence_replace_none),
            UiOptions.PRESENCE_REPLACE_FAVORITE to stringResource(Res.string.presence_replace_favorite),
            UiOptions.PRESENCE_REPLACE_MUTED to stringResource(Res.string.presence_replace_muted),
            UiOptions.PRESENCE_REPLACE_UNMESSAGEABLE to stringResource(Res.string.presence_replace_unmessageable),
        ),
        selectedItem = options[UiOptions.PRESENCE_REPLACE],
        onItemSelected = { onChange(UiOptions.PRESENCE_REPLACE, it) },
    )
    DropDownPreference(
        title = stringResource(Res.string.presence_color_title),
        enabled = shown,
        items =
        listOf(
            0 to stringResource(Res.string.presence_color_green),
            1 to stringResource(Res.string.presence_color_theme),
        ),
        selectedItem = options[UiOptions.PRESENCE_COLOR],
        onItemSelected = { onChange(UiOptions.PRESENCE_COLOR, it) },
    )
    val hoursLabel = Res.string.presence_expiry_hours
    val minutesLabel = Res.string.presence_expiry_min
    DropDownPreference(
        title = stringResource(Res.string.presence_expiry_title),
        enabled = shown,
        items =
        NodePresence.EXPIRY_STEPS_MINUTES.mapIndexed { index, minutes ->
            index to
                if (minutes % MINUTES_PER_HOUR == 0) {
                    stringResource(hoursLabel, minutes / MINUTES_PER_HOUR)
                } else {
                    stringResource(minutesLabel, minutes)
                }
        },
        selectedItem = options[UiOptions.PRESENCE_EXPIRY],
        onItemSelected = { onChange(UiOptions.PRESENCE_EXPIRY, it) },
        summary = stringResource(Res.string.presence_expiry_summary),
    )
    SwitchListItem(
        text = stringResource(Res.string.presence_ask_title),
        supportingText = stringResource(Res.string.presence_ask_summary),
        leadingIcon = MeshtasticIcons.DisplaySettings,
        checked = options.flag(UiOptions.PRESENCE_ASK),
        enabled = shown,
        onClick = { onChange(UiOptions.PRESENCE_ASK, if (options.flag(UiOptions.PRESENCE_ASK)) 0 else 1) },
    )
}
