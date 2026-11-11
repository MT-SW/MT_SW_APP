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

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSSelectorFromString
import platform.Foundation.valueForKey
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
actual fun applyAppIcon(style: String) {
    // "" is the main icon, which the system shows in the style (default, dark, tinted) chosen in iOS.
    val name =
        when (AppIconStyle.base(style)) {
            "standard" -> if (AppIconStyle.isFlat(style)) "AppIcon-StandardFlat" else "AppIcon-Standard"

            "dark" -> if (AppIconStyle.isFlat(style)) "AppIcon-DarkFlat" else "AppIcon-Dark"

            "clear" -> "AppIcon-Clear"

            "tinted" -> "AppIcon-Tinted"

            // A flat icon needs a named set: "automatic" has none, so it becomes the standard one.
            else -> if (AppIconStyle.isFlat(style)) "AppIcon-StandardFlat" else null
        }
    // Kotlin/Native does not expose the alternate-icon calls of UIApplication, so they go through the selector.
    val application = UIApplication.sharedApplication
    if (application.valueForKey("alternateIconName") as? String == name) return
    application.performSelector(NSSelectorFromString("setAlternateIconName:completionHandler:"), name, null)
}
