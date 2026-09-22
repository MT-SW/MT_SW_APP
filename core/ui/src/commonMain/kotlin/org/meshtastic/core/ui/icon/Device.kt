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
package org.meshtastic.core.ui.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.vectorResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ic_android
import org.meshtastic.core.resources.ic_fingerprint
import org.meshtastic.core.resources.ic_fork_left
import org.meshtastic.core.resources.ic_foxhunt
import org.meshtastic.core.resources.ic_home
import org.meshtastic.core.resources.ic_icecream
import org.meshtastic.core.resources.ic_memory
import org.meshtastic.core.resources.ic_military_tech
import org.meshtastic.core.resources.ic_mountain_flag
import org.meshtastic.core.resources.ic_my_location
import org.meshtastic.core.resources.ic_numbers
import org.meshtastic.core.resources.ic_person
import org.meshtastic.core.resources.ic_person_off
import org.meshtastic.core.resources.ic_phone_android
import org.meshtastic.core.resources.ic_router
import org.meshtastic.core.resources.ic_search
import org.meshtastic.core.resources.ic_sensors
import org.meshtastic.core.resources.ic_visibility_off
import org.meshtastic.core.resources.ic_work
import org.meshtastic.core.resources.role_client
import org.meshtastic.core.resources.role_client_base
import org.meshtastic.core.resources.role_client_hidden
import org.meshtastic.core.resources.role_client_mute
import org.meshtastic.core.resources.role_lost_and_found
import org.meshtastic.core.resources.role_repeater
import org.meshtastic.core.resources.role_router
import org.meshtastic.core.resources.role_router_client
import org.meshtastic.core.resources.role_router_late
import org.meshtastic.core.resources.role_sensor
import org.meshtastic.core.resources.role_tak
import org.meshtastic.core.resources.role_tak_tracker
import org.meshtastic.core.resources.role_tracker
import org.meshtastic.proto.Config

val MeshtasticIcons.Role: ImageVector
    @Composable get() = vectorResource(Res.drawable.ic_work)
val MeshtasticIcons.NodeId: ImageVector
    @Composable get() = vectorResource(Res.drawable.ic_fingerprint)

/** Returns a specific icon for a given [Config.DeviceConfig.Role]. */
@Composable
fun MeshtasticIcons.role(role: Config.DeviceConfig.Role?): ImageVector = when (role) {
    Config.DeviceConfig.Role.CLIENT -> vectorResource(Res.drawable.ic_person)
    Config.DeviceConfig.Role.CLIENT_MUTE -> vectorResource(Res.drawable.ic_person_off)
    Config.DeviceConfig.Role.ROUTER -> vectorResource(Res.drawable.ic_mountain_flag)
    Config.DeviceConfig.Role.TRACKER -> vectorResource(Res.drawable.ic_my_location)
    Config.DeviceConfig.Role.SENSOR -> vectorResource(Res.drawable.ic_sensors)
    Config.DeviceConfig.Role.TAK -> vectorResource(Res.drawable.ic_military_tech)
    Config.DeviceConfig.Role.TAK_TRACKER -> vectorResource(Res.drawable.ic_my_location)
    Config.DeviceConfig.Role.CLIENT_HIDDEN -> vectorResource(Res.drawable.ic_visibility_off)
    Config.DeviceConfig.Role.LOST_AND_FOUND -> vectorResource(Res.drawable.ic_search)
    Config.DeviceConfig.Role.CLIENT_BASE -> vectorResource(Res.drawable.ic_home)
    Config.DeviceConfig.Role.ROUTER_LATE -> vectorResource(Res.drawable.ic_router)
    else -> vectorResource(Res.drawable.ic_work)
}

/** Zasób tekstowy z nazwą danej roli — użyj przez `stringResource(role.label)` w miejscu wywołania. */
val Config.DeviceConfig.Role.label: StringResource
    get() =
        when (this) {
            Config.DeviceConfig.Role.CLIENT -> Res.string.role_client
            Config.DeviceConfig.Role.CLIENT_BASE -> Res.string.role_client_base
            Config.DeviceConfig.Role.CLIENT_MUTE -> Res.string.role_client_mute
            Config.DeviceConfig.Role.ROUTER -> Res.string.role_router
            Config.DeviceConfig.Role.ROUTER_CLIENT -> Res.string.role_router_client
            Config.DeviceConfig.Role.REPEATER -> Res.string.role_repeater
            Config.DeviceConfig.Role.TRACKER -> Res.string.role_tracker
            Config.DeviceConfig.Role.SENSOR -> Res.string.role_sensor
            Config.DeviceConfig.Role.TAK -> Res.string.role_tak
            Config.DeviceConfig.Role.CLIENT_HIDDEN -> Res.string.role_client_hidden
            Config.DeviceConfig.Role.LOST_AND_FOUND -> Res.string.role_lost_and_found
            Config.DeviceConfig.Role.TAK_TRACKER -> Res.string.role_tak_tracker
            Config.DeviceConfig.Role.ROUTER_LATE -> Res.string.role_router_late
        }

val MeshtasticIcons.Device: ImageVector
    @Composable get() = vectorResource(Res.drawable.ic_router)

val MeshtasticIcons.PhoneAndroid: ImageVector
    @Composable get() = vectorResource(Res.drawable.ic_phone_android)
val MeshtasticIcons.ForkLeft: ImageVector
    @Composable get() = vectorResource(Res.drawable.ic_fork_left)
val MeshtasticIcons.Icecream: ImageVector
    @Composable get() = vectorResource(Res.drawable.ic_icecream)
val MeshtasticIcons.DeviceNumbers: ImageVector
    @Composable get() = vectorResource(Res.drawable.ic_numbers)
val MeshtasticIcons.Android: ImageVector
    @Composable get() = vectorResource(Res.drawable.ic_android)
val MeshtasticIcons.HardwareModel: ImageVector
    @Composable get() = vectorResource(Res.drawable.ic_memory)
val MeshtasticIcons.Foxhunt: ImageVector
    @Composable get() = vectorResource(Res.drawable.ic_foxhunt)
