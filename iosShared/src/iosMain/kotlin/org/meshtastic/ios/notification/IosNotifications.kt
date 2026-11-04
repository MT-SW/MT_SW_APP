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
package org.meshtastic.ios.notification

import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.FirmwareUpdateNotice
import org.meshtastic.core.model.MeshBeaconOffer
import org.meshtastic.core.model.Node
import org.meshtastic.core.repository.MeshNotificationManager
import org.meshtastic.core.repository.Notification
import org.meshtastic.core.repository.NotificationManager
import org.meshtastic.proto.ClientNotification
import org.meshtastic.proto.Telemetry

/**
 * Notifications on iPhone. For now nothing is shown outside the app (local notifications are the next step), so every
 * call is accepted and ignored; the in-app lists keep working.
 */
class IosNotificationManager : NotificationManager {
    override suspend fun dispatch(notification: Notification): Boolean = false

    override fun cancel(id: Int) {}

    override fun cancelAll() {}
}

@Suppress("TooManyFunctions", "EmptyFunctionBlock", "UnusedParameter")
class IosMeshNotificationManager : MeshNotificationManager {
    override fun clearNotifications() {}

    override fun initChannels() {}

    override fun updateServiceStateNotification(state: ConnectionState, telemetry: Telemetry?) {}

    override suspend fun updateMessageNotification(
        contactKey: String,
        name: String,
        message: String,
        isBroadcast: Boolean,
        channelName: String?,
        isSilent: Boolean,
    ) {}

    override suspend fun updateWaypointNotification(
        contactKey: String,
        name: String,
        message: String,
        waypointId: Int,
        isSilent: Boolean,
    ) {}

    override suspend fun updateReactionNotification(
        contactKey: String,
        name: String,
        emoji: String,
        isBroadcast: Boolean,
        channelName: String?,
        isSilent: Boolean,
    ) {}

    override suspend fun showAlertNotification(contactKey: String, name: String, alert: String) {}

    override suspend fun showMeshBeaconNotification(offer: MeshBeaconOffer) {}

    override suspend fun showNewNodeSeenNotification(node: Node, title: String) {}

    override fun cancelNewNodeNotification(nodeNum: Int) {}

    override suspend fun notifyLowBattery(node: Node, isRemote: Boolean) {}

    override fun cancelLowBatteryNotification(nodeNum: Int) {}

    override suspend fun showClientNotification(
        clientNotification: ClientNotification,
        title: String,
        severity: Notification.Type,
    ) {}

    override fun clearClientNotification(clientNotification: ClientNotification) {}

    override suspend fun showFirmwareUpdateNotification(notice: FirmwareUpdateNotice): Boolean = false

    override suspend fun showReconnectBlockedNotification(title: String, message: String): Boolean = false

    override suspend fun cancelMessageNotification(contactKey: String) {}
}
