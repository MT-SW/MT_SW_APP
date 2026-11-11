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

import android.content.ComponentName
import android.content.pm.PackageManager
import org.meshtastic.core.common.ContextServices

private const val ALIAS_PREFIX = "org.meshtastic.app.Icon"
private val ALIASES = listOf("Standard", "StandardFlat", "Dark", "Clear", "Tinted")

actual fun applyAppIcon(style: String) {
    val context = ContextServices.app
    val packageManager = context.packageManager
    // Only the standard icon has a flat variant on Android: the others are flat already.
    val chosen =
        when (AppIconStyle.base(style)) {
            "dark" -> "Dark"
            "clear" -> "Clear"
            "tinted" -> "Tinted"
            else -> if (AppIconStyle.isFlat(style)) "StandardFlat" else "Standard"
        }
    // The new icon is switched on first, so the app never has no icon on the home screen.
    ALIASES.sortedBy { it != chosen }
        .forEach { name ->
            packageManager.setComponentEnabledSetting(
                ComponentName(context.packageName, ALIAS_PREFIX + name),
                if (name == chosen) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                },
                PackageManager.DONT_KILL_APP,
            )
        }
}
