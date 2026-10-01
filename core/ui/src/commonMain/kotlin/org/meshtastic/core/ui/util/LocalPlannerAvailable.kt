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

import androidx.compose.runtime.compositionLocalOf

/**
 * Whether the native MT_SW Planner is available on this host. Gates the "open in Planner" action on the node detail
 * screen. Defaults to false so a host without a map never surfaces a dead action.
 *
 * Flavor-injected feature availability, mirroring the other Local*Provider seams in this package (all baselined for
 * this rule); a plain boolean local is the lightest way to gate a commonMain UI action on a flavor capability.
 */
@Suppress("CompositionLocalAllowlist")
val LocalPlannerAvailable = compositionLocalOf { false }
