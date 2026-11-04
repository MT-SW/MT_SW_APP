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
package org.meshtastic.feature.map.terrain

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

/** iPhone/iPad: the same Skia WebP decode as on desktop, with straight (not premultiplied) alpha. */
actual fun decodeTerrariumTile(webpBytes: ByteArray): ElevationTile {
    val image = Image.makeFromEncoded(webpBytes)
    val width = image.width
    val height = image.height
    val bitmap = Bitmap()
    try {
        check(bitmap.allocPixels(ImageInfo(width, height, ColorType.N32, ColorAlphaType.UNPREMUL))) {
            "Could not allocate a $width×$height bitmap for a decoded Terrarium tile"
        }
        check(image.readPixels(bitmap)) { "Could not read pixels from a decoded Terrarium tile" }
        val elevations = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = bitmap.getColor(x, y)
                val red = (color ushr RED_SHIFT) and COLOR_MASK
                val green = (color ushr GREEN_SHIFT) and COLOR_MASK
                val blue = color and COLOR_MASK
                elevations[y * width + x] = terrariumElevationMeters(red, green, blue)
            }
        }
        return ElevationTile(width, height, elevations)
    } finally {
        bitmap.close()
        image.close()
    }
}

private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val COLOR_MASK = 0xFF
