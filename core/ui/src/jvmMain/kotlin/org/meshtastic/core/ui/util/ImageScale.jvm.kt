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
@file:Suppress("TooGenericExceptionCaught")

package org.meshtastic.core.ui.util

import co.touchlab.kermit.Logger
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

actual fun scaleJpeg(bytes: ByteArray, maxSide: Int, quality: Int): ByteArray? = try {
    val source = ImageIO.read(ByteArrayInputStream(bytes))
    if (source == null) {
        null
    } else {
        val factor = minOf(1.0, maxSide.toDouble() / maxOf(source.width, source.height))
        val w = (source.width * factor).toInt().coerceAtLeast(1)
        val h = (source.height * factor).toInt().coerceAtLeast(1)
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(source, 0, 0, w, h, null)
        g.dispose()
        val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
        val params =
            writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = quality / PERCENT
            }
        val buffer = ByteArrayOutputStream()
        writer.output = ImageIO.createImageOutputStream(buffer)
        writer.write(null, IIOImage(out, null, null), params)
        writer.dispose()
        buffer.toByteArray()
    }
} catch (e: Exception) {
    Logger.w { "Zdjęcie: zmniejszenie nie udało się: ${e.message}" }
    null
}

private const val PERCENT = 100f
