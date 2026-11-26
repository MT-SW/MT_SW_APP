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
package org.meshtastic.core.prefs.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import org.meshtastic.core.common.state.HiddenFeaturesStore
import org.meshtastic.core.repository.UiPrefs

@Single(binds = [HiddenFeaturesStore::class])
class HiddenFeaturesStoreImpl(private val uiPrefs: UiPrefs) : HiddenFeaturesStore {
    override val kept: Flow<Boolean> = uiPrefs.uiOptions.map { it[KEY] == 1 }

    override fun keep() = uiPrefs.setUiOption(KEY, 1)

    override fun forget() = uiPrefs.setUiOption(KEY, 0)

    private companion object {
        const val KEY = "app.mode.k"
    }
}
