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

package org.meshtastic.core.model.util

import org.meshtastic.proto.Channel
import org.meshtastic.proto.ChannelSettings
import org.meshtastic.proto.ModuleSettings

/** Największa dokładność pozycji dozwolona na kanale z publicznym kluczem (tak jak w aplikacji na iPhone'a). */
const val PUBLIC_CHANNEL_MAX_POSITION_PRECISION = 15

private const val DEFAULT_KEY_LENGTH = 16
private val defaultKeyPrefix =
    intArrayOf(0xd4, 0xf1, 0xbb, 0x3a, 0x20, 0x29, 0x07, 0x59, 0xf0, 0xbc, 0xff, 0xab, 0xcf, 0x4e, 0x69)

/**
 * Czy klucz kanału jest publiczny: pusty, jednobajtowy (skrót do klucza domyślnego) albo z rodziny domyślnego klucza
 * (16 bajtów, różniący się tylko ostatnim bajtem).
 */
val ChannelSettings.hasPublicKey: Boolean
    get() {
        val key = psk
        if (key.size <= 1) return true
        if (key.size != DEFAULT_KEY_LENGTH) return false
        return defaultKeyPrefix.indices.all { (key[it].toInt() and 0xff) == defaultKeyPrefix[it] }
    }

/** Zwraca ustawienia z dokładnością pozycji ograniczoną do [PUBLIC_CHANNEL_MAX_POSITION_PRECISION], gdy klucz jest publiczny. */
fun ChannelSettings.withSafePositionPrecision(): ChannelSettings {
    val precision = module_settings?.position_precision ?: 0
    if (!hasPublicKey || precision <= PUBLIC_CHANNEL_MAX_POSITION_PRECISION) return this
    return newBuilder()
        .also { wb ->
            wb.module_settings =
                (module_settings ?: ModuleSettings.Builder().build())
                    .newBuilder()
                    .also { mb -> mb.position_precision = PUBLIC_CHANNEL_MAX_POSITION_PRECISION }
                    .build()
        }
        .build()
}

/** Kanał gotowy do wysłania do radia: bez dokładnej pozycji na kanale z publicznym kluczem. */
fun Channel.withSafePositionPrecision(): Channel {
    val current = settings ?: return this
    val safe = current.withSafePositionPrecision()
    return if (safe === current) this else newBuilder().also { wb -> wb.settings = safe }.build()
}
