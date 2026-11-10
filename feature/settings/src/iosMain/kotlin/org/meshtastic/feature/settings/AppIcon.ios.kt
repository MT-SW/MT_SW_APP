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
package org.meshtastic.feature.settings

import co.touchlab.kermit.Logger
import platform.UIKit.UIApplication

actual fun applyAppIcon(style: String) {
    // "" is the main icon, which the system shows in the style (default, dark, tinted) chosen in iOS.
    val name =
        when (style) {
            "standard" -> "AppIcon-Standard"
            "dark" -> "AppIcon-Dark"
            "clear" -> "AppIcon-Clear"
            "tinted" -> "AppIcon-Tinted"
            else -> null
        }
    val application = UIApplication.sharedApplication
    if (application.alternateIconName == name) return
    application.setAlternateIconName(name) { error ->
        if (error != null) Logger.w { "Changing the app icon failed: ${error.localizedDescription}" }
    }
}
