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
package org.meshtastic.feature.node.health

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformLatest

/** How long a burst of database changes is allowed to settle before the heavy calculations run again. */
private const val CALM_MILLIS = 750L

/**
 * Lets the first value through at once, then waits for a burst of further values to settle and passes on only the
 * newest. Every packet the radio receives changes the log table, and the health and detail screens re-read thousands of
 * rows each time, so without this a busy mesh keeps them recalculating (and allocating) continuously -- while the first
 * screen still opens without waiting.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Flow<T>.calmed(): Flow<T> = flow {
    var first = true
    emitAll(
        transformLatest { value ->
            if (first) first = false else delay(CALM_MILLIS)
            emit(value)
        },
    )
}
