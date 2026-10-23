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
package org.meshtastic.feature.messaging.translation

/** Drops emoji and other characters the native part of the model library cannot take, and control characters. */
internal fun String.stripUnsafeChars(): String {
    val out = StringBuilder(length)
    for (c in this) {
        when {
            c.isSurrogate() -> Unit
            c.code < 0x20 && c != '\n' && c != '\t' -> Unit
            c.code == 0xFFFE || c.code == 0xFFFF -> Unit
            else -> out.append(c)
        }
    }
    return out.toString()
}
