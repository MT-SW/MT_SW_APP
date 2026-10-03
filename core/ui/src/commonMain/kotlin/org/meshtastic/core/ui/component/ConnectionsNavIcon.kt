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
package org.meshtastic.core.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.DeviceType
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.connected
import org.meshtastic.core.resources.connected_sleeping
import org.meshtastic.core.resources.connecting
import org.meshtastic.core.resources.disconnected
import org.meshtastic.core.ui.icon.Bluetooth
import org.meshtastic.core.ui.icon.Device
import org.meshtastic.core.ui.icon.DeviceSleep
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.NoDevice
import org.meshtastic.core.ui.icon.Reconnecting
import org.meshtastic.core.ui.icon.Usb
import org.meshtastic.core.ui.icon.Wifi
import org.meshtastic.core.ui.theme.StatusColors.StatusConnecting
import org.meshtastic.core.ui.theme.StatusColors.StatusDisconnected
import org.meshtastic.core.ui.theme.StatusColors.StatusOnline
import org.meshtastic.core.ui.theme.StatusColors.StatusPurple

@Composable
fun ConnectionsNavIcon(
    modifier: Modifier = Modifier,
    connectionState: ConnectionState,
    deviceType: DeviceType?,
    contentDescription: String? = null,
    tintOverride: Color? = null,
) {
    val tint = tintOverride ?: getTint(connectionState)

    val (backgroundIcon, connectionTypeIcon) = getIconPair(deviceType = deviceType, connectionState = connectionState)

    val foregroundPainter = connectionTypeIcon?.let { rememberVectorPainter(it) }

    Crossfade(targetState = backgroundIcon, label = "ConnectionIcon") {
        Icon(
            imageVector = it,
            contentDescription = contentDescription,
            tint = tint,
            modifier =
            modifier.drawWithContent {
                drawContent()
                foregroundPainter?.let {
                    @Suppress("MagicNumber")
                    val badgeSize = size.width * .45f
                    with(it) { draw(Size(badgeSize, badgeSize), colorFilter = ColorFilter.tint(tint)) }
                }
            },
        )
    }
}

@Composable
fun getTint(connectionState: ConnectionState): Color = when (connectionState) {
    ConnectionState.Connecting -> colorScheme.StatusConnecting
    ConnectionState.Disconnected -> colorScheme.StatusDisconnected
    ConnectionState.DeviceSleep -> colorScheme.StatusPurple
    else -> colorScheme.StatusOnline
}

@Composable
fun getIconPair(connectionState: ConnectionState, deviceType: DeviceType? = null): Pair<ImageVector, ImageVector?> =
    when (connectionState) {
        ConnectionState.Disconnected -> MeshtasticIcons.NoDevice to null

        ConnectionState.DeviceSleep -> MeshtasticIcons.Device to MeshtasticIcons.DeviceSleep

        ConnectionState.Connecting -> MeshtasticIcons.Device to MeshtasticIcons.Reconnecting

        else ->
            MeshtasticIcons.Device to
                when (deviceType) {
                    DeviceType.BLE -> MeshtasticIcons.Bluetooth
                    DeviceType.TCP -> MeshtasticIcons.Wifi
                    DeviceType.USB -> MeshtasticIcons.Usb
                    else -> null
                }
    }

/** The connection states the icon can show, compact and description-free like [SecurityLegendItems]. */
private val CONNECTION_STATUS_ORDER =
    listOf(
        ConnectionState.Connected to Res.string.connected,
        ConnectionState.Connecting to Res.string.connecting,
        ConnectionState.DeviceSleep to Res.string.connected_sleeping,
        ConnectionState.Disconnected to Res.string.disconnected,
    )

/** Every state [ConnectionsNavIcon] can show, one compact row per state — for the node list help sheet. */
@Composable
fun ConnectionStatusLegendItems() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CONNECTION_STATUS_ORDER.forEach { (state, labelRes) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = androidx.compose.ui.Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    ConnectionsNavIcon(
                        connectionState = state,
                        deviceType = null,
                        modifier = androidx.compose.ui.Modifier.size(24.dp),
                    )
                }
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = androidx.compose.ui.Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}
