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

import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.repository.UiPrefs
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

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
     * [ASK_INTERVAL_MILLIS] per node, and only while the asking is switched on in the settings (off by default: the
     * status is then asked for by hand).
     */
    open suspend fun askOnOpen(nodeNum: Int) {
        val options = UiOptions(uiPrefs.uiOptions.value)
        if (!options.flag(UiOptions.PRESENCE_ASK)) return
        val now = nowMillis
        val allowed = askLock.withLock {
            val last = askedAt[nodeNum]
            if (last != null && now - last < ASK_INTERVAL_MILLIS) {
                false
            } else {
                askedAt[nodeNum] = now
                true
            }
        }
        // Asks for the firmware version first (when it is not known yet): an older node is not sent the status query.
        if (allowed) askQuietly(nodeNum, forceVersionCheck = false)
    }

    /** One status query whose failure is not worth more than a log line; a cancelled caller still stops the work. */
    private suspend fun askQuietly(nodeNum: Int, forceVersionCheck: Boolean) {
        @Suppress("TooGenericExceptionCaught")
        try {
            snifferControlUseCase.requestPresenceChecked(nodeNum, forceVersionCheck)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.d(e) { "Status query for node $nodeNum failed" }
        }
    }

    /**
     * Asks the given nodes for their status by hand, one after another with a pause between them (a query is a radio
     * transmission, so many at once would clog the mesh). It works whether or not the automatic asking is switched on.
     * [forceVersionCheck] asks about the firmware version again even when an older one was learned less than a week ago
     * (for a single node asked for by hand). [onProgress] gets how many nodes have been asked so far; the work ends at
     * once when the caller's job is cancelled.
     */
    open suspend fun askNodes(nodeNums: List<Int>, forceVersionCheck: Boolean = false, onProgress: (Int) -> Unit = {}) {
        nodeNums.forEachIndexed { index, nodeNum ->
            askLock.withLock { askedAt[nodeNum] = nowMillis }
            askQuietly(nodeNum, forceVersionCheck)
            onProgress(index + 1)
            if (index < nodeNums.lastIndex) delay(MANUAL_ASK_SPACING)
        }
    }

    private companion object {
        val MANUAL_ASK_SPACING = 5.seconds
        const val MILLIS_PER_SECOND = 1000L
        const val ASK_INTERVAL_MILLIS = 10 * 60 * MILLIS_PER_SECOND
    }
}
