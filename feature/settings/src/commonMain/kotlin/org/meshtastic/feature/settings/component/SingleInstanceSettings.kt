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
@file:Suppress("ModifierMissing")

package org.meshtastic.feature.settings.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.background.SingleInstanceControls
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.single_instance_summary
import org.meshtastic.core.resources.single_instance_switch
import org.meshtastic.core.ui.component.SwitchPreference

/** The computer app's "allow a second copy" switch; shown only where the platform installs it (the desktop app). */
@Composable
fun SingleInstanceSettings() {
    val control = SingleInstanceControls.current ?: return
    val allowSeveral by control.allowSeveral.collectAsStateWithLifecycle()
    SwitchPreference(
        title = stringResource(Res.string.single_instance_switch),
        summary = stringResource(Res.string.single_instance_summary),
        checked = allowSeveral,
        enabled = true,
        onCheckedChange = { control.setAllowSeveral(it) },
    )
}
