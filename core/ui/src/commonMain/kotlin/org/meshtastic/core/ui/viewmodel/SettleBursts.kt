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
package org.meshtastic.core.ui.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformLatest

/**
 * Lets the first value through at once, then waits [quietMillis] for a burst of further values to settle and passes on
 * only the newest. For flows that change on every received packet (the node list) feeding something expensive to
 * rebuild (map layers): the screen still opens immediately, but a busy mesh no longer rebuilds it continuously.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.settleBursts(quietMillis: Long): Flow<T> = flow {
    var first = true
    emitAll(
        transformLatest { value ->
            if (first) first = false else delay(quietMillis)
            emit(value)
        },
    )
}
