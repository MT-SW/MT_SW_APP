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
package org.meshtastic.core.network.radio

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.meshtastic.core.common.util.nowMillis
import kotlin.time.Duration.Companion.minutes

/**
 * Tracks recent BLE reconnect failures per device address across [BleRadioTransport] instance boundaries.
 *
 * [BleReconnectPolicy] and [GattCacheInvalidationGate] both live inside a single [BleRadioTransport], which is
 * recreated whenever the user manually stops and restarts a connection ("Stop Connecting", or picking a different
 * device and back). That recreation resets every in-memory failure counter to zero — so a user who reacts to a
 * stuck "Not connected" screen by stopping and retrying every 20-30 seconds never lets a single streak reach
 * [GattCacheInvalidationGate.DEFAULT_FAILURE_THRESHOLD], even though the underlying stale-cache condition
 * (issue #6685) hasn't changed between those attempts.
 *
 * This object persists a small failure count per address for the lifetime of the process, independent of transport
 * recreation, so repeated manual retries accumulate towards the same threshold instead of resetting it. Entries
 * older than [STALE_AFTER] are dropped so a failure from a much earlier, unrelated session can't wrongly bank
 * itself into a later attempt.
 */
internal object PersistentReconnectFailures {

    private data class Entry(val count: Int, val lastFailureMillis: Long)

    private val mutex = Mutex()
    private val entries = mutableMapOf<String, Entry>()

    /** Records a failed attempt for [address] and returns the updated streak count. */
    suspend fun recordFailure(address: String): Int = mutex.withLock {
        val now = nowMillis
        val previous = entries[address]?.takeIf { now - it.lastFailureMillis <= STALE_AFTER.inWholeMilliseconds }
        val count = (previous?.count ?: 0) + 1
        entries[address] = Entry(count, now)
        count
    }

    /** Clears the streak for [address] after a stable or intentional disconnect. */
    suspend fun recordSuccess(address: String) {
        mutex.withLock { entries.remove(address) }
    }

    /** The current streak for [address], or 0 if none is recorded or it has expired. */
    suspend fun currentCount(address: String): Int = mutex.withLock {
        val entry = entries[address] ?: return@withLock 0
        if (nowMillis - entry.lastFailureMillis > STALE_AFTER.inWholeMilliseconds) {
            entries.remove(address)
            0
        } else {
            entry.count
        }
    }

    private val STALE_AFTER = 30.minutes
}