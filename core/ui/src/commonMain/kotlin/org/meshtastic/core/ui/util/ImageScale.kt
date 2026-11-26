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
package org.meshtastic.core.ui.util

/**
 * MT_SW: makes a smaller JPEG out of a picture: the longer side is at most [maxSide] pixels, the turn stored in the
 * picture is applied. Null when the picture cannot be read or the platform cannot do it (the caller keeps the
 * original).
 */
expect fun scaleJpeg(bytes: ByteArray, maxSide: Int, quality: Int): ByteArray?
