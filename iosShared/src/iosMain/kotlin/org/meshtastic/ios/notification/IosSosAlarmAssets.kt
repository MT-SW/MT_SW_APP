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
@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package org.meshtastic.ios.notification

import co.touchlab.kermit.Logger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.meshtastic.core.repository.SosAlarmStyle
import org.meshtastic.core.repository.SosSiren
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSLibraryDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile

/** The name of the siren file the alarm notifications use as their sound (it lives in the app's Library/Sounds). */
internal const val SOS_SOUND_FILE = "mtsw_sos_alarm.wav"

/** The same for the bell. */
internal const val SOS_BELL_FILE = "mtsw_sos_bell.wav"

private const val NOTIFICATION_SIREN_SECONDS = 10.0

/** Bytes as [NSData], copied. */
internal fun ByteArray.toNSData(): NSData = usePinned {
    NSData.create(bytes = it.addressOf(0), length = size.toULong())
}

/**
 * Writes the siren into the app's Library/Sounds folder (once per start), where the system looks for the custom sounds
 * of notifications, and returns its name; null when it could not be written (the default sound is used then). The sound
 * is ten seconds long, so notifications posted ten seconds apart sound as one siren without a break.
 */
@Suppress("ReturnCount")
internal fun ensureSosSoundFile(style: SosAlarmStyle = SosAlarmStyle.SIREN): String? {
    val bell = style == SosAlarmStyle.BELL
    val name = if (bell) SOS_BELL_FILE else SOS_SOUND_FILE
    val library =
        NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, true).firstOrNull() as? String
            ?: return null
    val folder = "$library/Sounds"
    val path = "$folder/$name"
    val files = NSFileManager.defaultManager
    if (files.fileExistsAtPath(path)) return name
    files.createDirectoryAtPath(folder, withIntermediateDirectories = true, attributes = null, error = null)
    val sound = if (bell) SosSiren.bellWav(NOTIFICATION_SIREN_SECONDS) else SosSiren.wav(NOTIFICATION_SIREN_SECONDS)
    val written = sound.toNSData().writeToFile(path, atomically = true)
    if (!written) Logger.w { "SOS alarm: the siren file could not be written" }
    return if (written) name else null
}
