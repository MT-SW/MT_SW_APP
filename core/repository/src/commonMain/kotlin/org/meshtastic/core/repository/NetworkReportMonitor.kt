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
package org.meshtastic.core.repository

import kotlinx.coroutines.CoroutineScope

/**
 * MT_SW: posts a short report of the network once a week (nodes heard in the last 7 days and active now) when it is
 * switched on in "MT_SW features". Off by default.
 */
interface NetworkReportMonitor {
    /** Starts watching in [scope]; the watching ends when the scope is cancelled. */
    fun start(scope: CoroutineScope)
}
