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

import org.jetbrains.compose.resources.StringResource
import org.meshtastic.core.resources.getStringSuspend

/**
 * The language the assistant answers in. A whole answer is in the language of the question: when someone asks in
 * English while the app is in Polish (or the other way round), the texts the app writes itself (counts, lists, where a
 * setting is, the button captions) come from [texts] instead of the app's own language. Null texts = the app's language.
 */
object AnswerLanguage {
    @Volatile var texts: Map<String, String>? = null
}

/** [res] in the language of the answer (see [AnswerLanguage]), with %1$s-style arguments filled in. */
suspend fun aiString(res: StringResource, vararg args: Any): String {
    val template = AnswerLanguage.texts?.get(res.key) ?: return getStringSuspend(res, *args)
    val values = args.map { if (it is StringResource) aiString(it) else it }
    return PLACEHOLDER.replace(template) { m ->
        values.getOrNull(m.groupValues[1].toInt() - 1)?.toString() ?: m.value
    }.replace("%%", "%")
}

private val PLACEHOLDER = Regex("%(\\d+)\\$[sd]")
