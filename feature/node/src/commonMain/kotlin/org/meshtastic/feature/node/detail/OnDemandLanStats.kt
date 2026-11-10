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
package org.meshtastic.feature.node.detail

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.on_demand_lan_ip
import org.meshtastic.core.resources.on_demand_lan_link
import org.meshtastic.core.resources.on_demand_lan_link_ethernet
import org.meshtastic.core.resources.on_demand_lan_link_none
import org.meshtastic.core.resources.on_demand_lan_link_wifi
import org.meshtastic.core.resources.on_demand_lan_mqtt
import org.meshtastic.core.resources.on_demand_lan_mqtt_connected
import org.meshtastic.core.resources.on_demand_lan_no
import org.meshtastic.core.resources.on_demand_lan_no_ip
import org.meshtastic.core.resources.on_demand_lan_udp
import org.meshtastic.core.resources.on_demand_lan_yes
import org.meshtastic.proto.LanStats

private const val OCTET_MASK = 0xFF
private const val BITS_PER_OCTET = 8

/** The IPv4 address of [LanStats.ip_address], whose first octet sits in the lowest byte; null when there is none. */
internal fun LanStats.ipText(): String? = ip_address?.let { value ->
    (0 until 4).joinToString(".") { index -> ((value ushr (index * BITS_PER_OCTET)) and OCTET_MASK).toString() }
}

/** One label and value per line, in the order the card and the report show them. */
@Composable
internal fun LanStats.lines(): List<Pair<String, String>> {
    val yes = stringResource(Res.string.on_demand_lan_yes)
    val no = stringResource(Res.string.on_demand_lan_no)
    val link =
        stringResource(
            when (this.link) {
                LanStats.Link.WIFI -> Res.string.on_demand_lan_link_wifi
                LanStats.Link.ETHERNET -> Res.string.on_demand_lan_link_ethernet
                else -> Res.string.on_demand_lan_link_none
            },
        )
    return listOf(
        stringResource(Res.string.on_demand_lan_ip) to (ipText() ?: stringResource(Res.string.on_demand_lan_no_ip)),
        stringResource(Res.string.on_demand_lan_link) to link,
        stringResource(Res.string.on_demand_lan_udp) to if (udp_enabled) yes else no,
        stringResource(Res.string.on_demand_lan_mqtt) to if (mqtt_enabled) yes else no,
        stringResource(Res.string.on_demand_lan_mqtt_connected) to if (mqtt_connected) yes else no,
    )
}
