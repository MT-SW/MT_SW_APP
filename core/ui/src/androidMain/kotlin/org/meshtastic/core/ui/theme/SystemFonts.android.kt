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
package org.meshtastic.core.ui.theme

import android.graphics.Typeface as AndroidTypeface
import android.os.Build
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import java.io.File

private const val BOLD_FROM = 600
private val FONT_FILES =
    listOf(
        "/system/etc/fonts.xml",
        "/product/etc/fonts_customization.xml",
        "/system/etc/fonts_customization.xml",
        "/vendor/etc/fonts_customization.xml",
    )
private val FAMILY_NAME = Regex("<family[^>]*\\sname=\"([^\"]+)\"")

/** Family names listed in the system's font configuration files (there is no public list on Android). */
actual fun systemFontNames(): List<String> = FONT_FILES
    .flatMap { path ->
        runCatching { FAMILY_NAME.findAll(File(path).readText()).map { it.groupValues[1] }.toList() }
            .getOrDefault(emptyList())
    }
    .filter { it.isNotBlank() }
    .distinct()
    .sorted()

actual fun systemFontFamily(name: String, weight: Int): FontFamily? {
    val base = AndroidTypeface.create(name, AndroidTypeface.NORMAL)
    val typeface =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            AndroidTypeface.create(base, weight, false)
        } else {
            AndroidTypeface.create(base, if (weight >= BOLD_FROM) AndroidTypeface.BOLD else AndroidTypeface.NORMAL)
        }
    return FontFamily(Typeface(typeface))
}
