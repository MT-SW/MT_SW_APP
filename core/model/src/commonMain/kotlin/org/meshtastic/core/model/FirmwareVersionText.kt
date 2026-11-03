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
package org.meshtastic.core.model

private val SHORT_VERSION = Regex("^v?(\\d+\\.\\d+\\.\\d+)(?:[.+\\-_ ].*)?$")

/**
 * A firmware version as it is shown on the screens: only the number (2.8.2), without the build mark that follows it
 * (2.8.2.54e0d8d) or any other addition. A text that does not start with a three-part number stays as it is.
 */
fun String.shortFirmwareVersion(): String = SHORT_VERSION.matchEntire(trim())?.groupValues?.get(1) ?: this
