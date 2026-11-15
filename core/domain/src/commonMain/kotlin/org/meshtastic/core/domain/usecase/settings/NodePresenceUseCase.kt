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
package org.meshtastic.core.domain.usecase.settings

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.repository.UiPrefs

/**
 * MT_SW: the activity status of the nodes' users (whether someone is at the node, see `Presence` in ondemand.proto).
 *
 * The status arrives as RESPONSE_PRESENCE on port 354, either broadcast by the node itself or as the answer to our
 * REQUEST_PRESENCE. Like all on-demand answers it is logged and read back from the log ([NodePresenceLogs]); this use
 * case only asks a node for its status.
 */
@Single
open class NodePresenceUseCase(
    private val snifferControlUseCase: SnifferControlUseCase,
    private val uiPrefs: UiPrefs,
) {
    private val askedAt = mutableMapOf<Int, Long>()
    private val askLock = Mutex()

    /**
     * Asks [nodeNum] for its status when a conversation with it is opened (never in a loop): not more often than every
     * [ASK_INTERVAL_MILLIS] per node, and only while both the icon and the asking are switched on in the settings.
     */
    open suspend fun askOnOpen(nodeNum: Int) {
        val options = UiOptions(uiPrefs.uiOptions.value)
        if (!options.flag(UiOptions.PRESENCE_SHOW) || !options.flag(UiOptions.PRESENCE_ASK)) return
        val now = nowMillis
        val allowed =
            askLock.withLock {
                val last = askedAt[nodeNum]
                if (last != null && now - last < ASK_INTERVAL_MILLIS) {
                    false
                } else {
                    askedAt[nodeNum] = now
                    true
                }
            }
        // Asks for the firmware version first (when it is not known yet): an older node is not sent the status query.
        if (allowed) runCatching { snifferControlUseCase.requestPresenceChecked(nodeNum) }
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
        const val ASK_INTERVAL_MILLIS = 10 * 60 * MILLIS_PER_SECOND
    }
}
