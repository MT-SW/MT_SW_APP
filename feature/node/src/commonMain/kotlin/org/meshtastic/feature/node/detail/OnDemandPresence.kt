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
package org.meshtastic.feature.node.detail

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.on_demand_presence_since
import org.meshtastic.core.resources.on_demand_presence_state
import org.meshtastic.core.resources.on_demand_presence_unknown
import org.meshtastic.core.resources.presence_active
import org.meshtastic.core.resources.presence_inactive
import org.meshtastic.proto.Presence

private const val SECONDS_PER_MINUTE = 60

/** One label and value per line, in the order the card and the report show them. */
@Composable
internal fun Presence.lines(): List<Pair<String, String>> {
    val state =
        stringResource(
            when (this.state) {
                Presence.State.ONLINE -> Res.string.presence_active
                Presence.State.OFFLINE -> Res.string.presence_inactive
                else -> Res.string.on_demand_presence_unknown
            },
        )
    val lines = mutableListOf(stringResource(Res.string.on_demand_presence_state) to state)
    secs_since_change?.let { secs ->
        lines += stringResource(Res.string.on_demand_presence_since) to "${secs / SECONDS_PER_MINUTE} min"
    }
    return lines
}
