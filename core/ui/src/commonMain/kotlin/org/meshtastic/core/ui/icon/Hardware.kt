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
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ic_auto_awesome
import org.meshtastic.core.resources.ic_bluetooth
import org.meshtastic.core.resources.ic_bluetooth_connected
import org.meshtastic.core.resources.ic_bluetooth_searching
import org.meshtastic.core.resources.ic_build
import org.meshtastic.core.resources.ic_cached
import org.meshtastic.core.resources.ic_center_focus_strong
import org.meshtastic.core.resources.ic_display_settings
import org.meshtastic.core.resources.ic_gpio_pins
import org.meshtastic.core.resources.ic_gpio_read
import org.meshtastic.core.resources.ic_memory
import org.meshtastic.core.resources.ic_mesh_radio
import org.meshtastic.core.resources.ic_nfc
import org.meshtastic.core.resources.ic_photo_camera
import org.meshtastic.core.resources.ic_photo_save
import org.meshtastic.core.resources.ic_settings_input_antenna
import org.meshtastic.core.resources.ic_speaker_phone
import org.meshtastic.core.resources.ic_terminal
import org.meshtastic.core.resources.ic_usb
import org.meshtastic.core.resources.ic_usb_off
import org.meshtastic.core.resources.ic_view_in_ar
import org.meshtastic.core.resources.ic_wifi

/** The maker hardware mark from meshtastic/design#160: a wrench, Material Symbols "build". */
val MeshtasticIcons.Wrench: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_build)

/** Material Symbols "view_in_ar": the entry to a device's 3D model. */
val MeshtasticIcons.ViewInAr: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_view_in_ar)

/** MT_SW: a chip with pins under a magnifier, for asking a device which GPIO pins it lets us control. */
val MeshtasticIcons.GpioPins: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_gpio_pins)

/** MT_SW: a square wave under a magnifier, for reading the state of a GPIO pin on a device. */
val MeshtasticIcons.GpioRead: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_gpio_read)

/** MT_SW: a camera with a "save" arrow, for the folder the pictures taken in the app are saved to. */
val MeshtasticIcons.PhotoSave: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_photo_save)

/** Material Symbols "photo_camera": capture the AR view. */
val MeshtasticIcons.PhotoCamera: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_photo_camera)

/** Material Symbols "center_focus_strong", a viewfinder: the camera mesh view. */
val MeshtasticIcons.CenterFocusStrong: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_center_focus_strong)

val MeshtasticIcons.BluetoothConnected: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_bluetooth_connected)
val MeshtasticIcons.BluetoothSearching: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_bluetooth_searching)
val MeshtasticIcons.UsbOff: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_usb_off)
val MeshtasticIcons.Antenna: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_settings_input_antenna)
val MeshtasticIcons.Speaker: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_speaker_phone)
val MeshtasticIcons.Reconnecting: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_cached)
val MeshtasticIcons.Nfc: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_nfc)
val MeshtasticIcons.Bluetooth: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_bluetooth)
val MeshtasticIcons.Wifi: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_wifi)
val MeshtasticIcons.Usb: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_usb)
val MeshtasticIcons.Serial: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_terminal)
val MeshtasticIcons.Memory: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_memory)
val MeshtasticIcons.DisplaySettings: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_display_settings)
val MeshtasticIcons.MeshRadio: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_mesh_radio)
val MeshtasticIcons.AutoAwesome: ImageVector
    @Composable get() = styledVectorResource(Res.drawable.ic_auto_awesome)
