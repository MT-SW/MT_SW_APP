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
@file:Suppress("SpreadOperator", "UnsafeCallOnNullableType", "UseOrEmpty")

package org.meshtastic.feature.messaging.translation

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** Marks a function of an [AiToolSet] as a tool the model may call; [description] tells the model when to use it. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AiTool(val description: String)

/** Describes one text parameter of an [AiTool] function; [name] is the name the model writes in its call. */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class AiToolParam(val name: String, val description: String)

/** A class whose [AiTool] functions can be offered to a model (they all take text and return text). */
interface AiToolSet {
    /**
     * The tools of this set as ready-to-offer specs (written out by hand: no reflection, it must work on every
     * platform).
     */
    fun specs(): List<AiToolSpec>
}

/** One tool as the model sees it, plus the code that runs when the model calls it. */
class AiToolSpec(
    val name: String,
    val description: String,
    val params: List<Param>,
    val call: (Map<String, String>) -> String,
) {
    class Param(val name: String, val description: String)

    /** The OpenAI style tool entry (`{"type":"function","function":{...}}`) that the model server understands. */
    fun toJson(): JsonObject = buildJsonObject {
        put("type", "function")
        putJsonObject("function") {
            put("name", name)
            put("description", description)
            putJsonObject("parameters") {
                put("type", "object")
                putJsonObject("properties") {
                    params.forEach { p ->
                        putJsonObject(p.name) {
                            put("type", "string")
                            put("description", p.description)
                        }
                    }
                }
                put("required", buildJsonArray { params.forEach { add(JsonPrimitive(it.name)) } })
            }
        }
    }
}

/** All tools of this set, in name order. */
fun AiToolSet.toSpecs(): List<AiToolSpec> = specs().sortedBy { it.name }
