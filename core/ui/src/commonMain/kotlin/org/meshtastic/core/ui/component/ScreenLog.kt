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
package org.meshtastic.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.distinctUntilChanged
import org.meshtastic.core.navigation.MultiBackstack

/**
 * Writes every change of the visible screen to the log: the tab, the depth of the stack and the names of the screen
 * types only. Nothing from inside a screen (node numbers, names, texts) is written, so the log holds no personal data.
 */
@Composable
fun LogScreenChanges(multiBackstack: MultiBackstack) {
    LaunchedEffect(multiBackstack) {
        snapshotFlow {
            val tab = multiBackstack.currentTabRoute::class.simpleName
            val stack = multiBackstack.backStacks[multiBackstack.currentTabRoute].orEmpty()
            "tab=$tab depth=${stack.size} top=${stack.lastOrNull()?.let { it::class.simpleName }}"
        }
            .distinctUntilChanged()
            .collect { Logger.i(tag = "Screen") { "Screen: $it" } }
    }
}
