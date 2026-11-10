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
@file:Suppress("MatchingDeclarationName")

package org.meshtastic.feature.messaging.translation

/** The model thought quietly for too long without starting its answer. */
class ThinkingTooLongException : IllegalStateException("The model kept reasoning without answering")

/** A safe file/note name for an assistant memory entry: letters and digits only, at most 40 characters. */
internal fun safeAssistantMemoryName(name: String): String = name
    .trim()
    .lowercase()
    .map { if (it.isLetterOrDigit()) it else '-' }
    .joinToString("")
    .trim('-')
    .take(MEMORY_NAME_MAX)

private const val MEMORY_NAME_MAX = 40
