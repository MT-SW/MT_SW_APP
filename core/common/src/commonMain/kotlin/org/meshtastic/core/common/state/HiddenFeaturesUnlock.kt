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
package org.meshtastic.core.common.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.meshtastic.core.common.di.ServiceScope

/** Storage of the state between launches. */
interface HiddenFeaturesStore {
    val kept: Flow<Boolean>

    fun keep()

    fun forget()
}

/**
 * Process-scoped easter-egg unlock (tapping the app version row in Settings five times). A normal unlock re-locks on
 * the next launch, like the web flasher's konami code. Gates the firmware-excluded module configuration screens and the
 * nightly firmware preview channel.
 */
@Single
class HiddenFeaturesUnlock(private val store: HiddenFeaturesStore? = null, scope: ServiceScope? = null) {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    init {
        if (store != null && scope != null) {
            scope.launch { store.kept.collect { if (it) _unlocked.value = true } }
        }
    }

    fun unlock() {
        _unlocked.value = true
    }

    /** Locks again and clears the stored state. */
    fun lock() {
        _unlocked.value = false
        store?.forget()
    }

    /** Unlocks and stores the state. */
    fun keep() {
        _unlocked.value = true
        store?.keep()
    }
}
