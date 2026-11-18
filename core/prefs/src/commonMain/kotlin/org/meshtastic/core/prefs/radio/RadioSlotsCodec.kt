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
package org.meshtastic.core.prefs.radio

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.meshtastic.core.common.util.isValidDeviceAddress
import org.meshtastic.core.repository.RadioSlot
import org.meshtastic.core.repository.RadioSlotsPrefs

/** Text form of the saved radios: a JSON list of `{id, address, name}`. A damaged text reads as an empty list. */
internal object RadioSlotsCodec {
    fun encode(slots: List<RadioSlot>): String = JsonArray(
        slots.map { slot ->
            JsonObject(
                buildMap {
                    put("id", JsonPrimitive(slot.id))
                    put("address", JsonPrimitive(slot.address))
                    slot.name?.let { put("name", JsonPrimitive(it)) }
                },
            )
        },
    )
        .toString()

    fun decode(text: String?): List<RadioSlot> {
        if (text.isNullOrBlank()) return emptyList()
        return runCatching {
            Json.parseToJsonElement(text).jsonArray.mapNotNull { element ->
                val obj = element.jsonObject
                val id = obj["id"]?.jsonPrimitive?.int ?: return@mapNotNull null
                val address = obj["address"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                if (address == null || id !in 1..RadioSlotsPrefs.MAX_SLOTS) return@mapNotNull null
                RadioSlot(id, address, obj["name"]?.jsonPrimitive?.contentOrNull)
            }
        }
            .getOrDefault(emptyList())
            .distinctBy { it.id }
            .sortedBy { it.id }
            .take(RadioSlotsPrefs.MAX_SLOTS)
    }

    /**
     * The radios to use given what is stored. When nothing is stored yet, the single address saved before the
     * connection center existed becomes radio 1 (the migration), so an update never loses the connection.
     */
    fun effective(stored: List<RadioSlot>, legacyAddress: String?, legacyName: String?): List<RadioSlot> =
        stored.ifEmpty {
            listOfNotNull(legacyAddress?.takeIf { isValidDeviceAddress(it) }?.let { RadioSlot(1, it, legacyName) })
        }
}
