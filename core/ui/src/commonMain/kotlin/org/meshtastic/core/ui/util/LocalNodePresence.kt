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
package org.meshtastic.core.ui.util

import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import org.meshtastic.core.model.NodePresence

/**
 * MT_SW: the last activity status of the nodes (see `NodePresence`), provided once at the app root (see
 * MeshtasticAppShell). The provided value is the **State holder**, not the map: its reference is stable, so providing
 * it costs no recomposition, and only the small status badge of a node reads it. Null (previews, tests) shows nothing.
 */
@Suppress("CompositionLocalAllowlist")
val LocalNodePresence = compositionLocalOf<State<Map<Int, NodePresence>>?> { null }
