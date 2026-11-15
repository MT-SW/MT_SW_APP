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
package org.meshtastic.core.ui.navigation

import org.jetbrains.compose.resources.DrawableResource
import org.meshtastic.core.navigation.TopLevelDestination
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ic_forum
import org.meshtastic.core.resources.ic_map
import org.meshtastic.core.resources.ic_nav_health
import org.meshtastic.core.resources.ic_nav_health_sel
import org.meshtastic.core.resources.ic_nav_map
import org.meshtastic.core.resources.ic_nav_map_sel
import org.meshtastic.core.resources.ic_nav_messages
import org.meshtastic.core.resources.ic_nav_messages_sel
import org.meshtastic.core.resources.ic_nav_nodes
import org.meshtastic.core.resources.ic_nav_nodes_sel
import org.meshtastic.core.resources.ic_nav_settings
import org.meshtastic.core.resources.ic_nav_settings_sel
import org.meshtastic.core.resources.ic_nav_translate
import org.meshtastic.core.resources.ic_nav_translate_sel
import org.meshtastic.core.resources.ic_nodes
import org.meshtastic.core.resources.ic_settings
import org.meshtastic.core.resources.ic_ssid_chart
import org.meshtastic.core.resources.ic_translate
import org.meshtastic.core.resources.ic_wifi

/** Maps a shared [TopLevelDestination] to its corresponding icon [DrawableResource]. */
val TopLevelDestination.icon: DrawableResource
    get() =
        when (this) {
            TopLevelDestination.Messages -> Res.drawable.ic_forum
            TopLevelDestination.Nodes -> Res.drawable.ic_nodes
            TopLevelDestination.NetworkHealth -> Res.drawable.ic_ssid_chart
            TopLevelDestination.Translator -> Res.drawable.ic_translate
            TopLevelDestination.Map -> Res.drawable.ic_map
            TopLevelDestination.Settings -> Res.drawable.ic_settings
            TopLevelDestination.Connect -> Res.drawable.ic_wifi
        }

/**
 * MT_SW: the newer tab icons, drawn as an outline when the tab is not open and with a soft fill and a heavier line when
 * it is. The connection tab has no such pair: its icon is animated and shows the state of the connection.
 */
fun TopLevelDestination.newIcon(selected: Boolean): DrawableResource? =
    when (this) {
        TopLevelDestination.Messages -> if (selected) Res.drawable.ic_nav_messages_sel else Res.drawable.ic_nav_messages
        TopLevelDestination.Nodes -> if (selected) Res.drawable.ic_nav_nodes_sel else Res.drawable.ic_nav_nodes
        TopLevelDestination.NetworkHealth ->
            if (selected) Res.drawable.ic_nav_health_sel else Res.drawable.ic_nav_health
        TopLevelDestination.Translator ->
            if (selected) Res.drawable.ic_nav_translate_sel else Res.drawable.ic_nav_translate
        TopLevelDestination.Map -> if (selected) Res.drawable.ic_nav_map_sel else Res.drawable.ic_nav_map
        TopLevelDestination.Settings ->
            if (selected) Res.drawable.ic_nav_settings_sel else Res.drawable.ic_nav_settings
        TopLevelDestination.Connect -> null
    }
