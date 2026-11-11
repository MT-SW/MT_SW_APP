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

/**
 * The saved icon style: "" (automatic), "standard", "dark", "clear" or "tinted", with [FLAT_PREFIX] in front for the
 * flat (not 3D) icon. One string, so nothing else has to be saved for it.
 */
object AppIconStyle {
    const val FLAT_PREFIX = "flat-"

    fun isFlat(style: String): Boolean = style.startsWith(FLAT_PREFIX)

    /** The style without the flat marker. */
    fun base(style: String): String = style.removePrefix(FLAT_PREFIX)

    /** The same style with the flat marker switched on or off. */
    fun withFlat(style: String, flat: Boolean): String = if (flat) FLAT_PREFIX + base(style) else base(style)
}
