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
@file:Suppress("ModifierMissing", "MultipleEmitters")

package org.meshtastic.feature.settings.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.background.AppBackgroundControls
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.background_keepalive_permission
import org.meshtastic.core.resources.background_keepalive_summary
import org.meshtastic.core.resources.background_keepalive_title
import org.meshtastic.core.ui.component.SwitchPreference

/**
 * The fork's "keep running in the background" switch for iPhone and iPad: off by default. Shown only where the platform
 * has installed its [org.meshtastic.core.common.background.AppBackgroundControl] (iOS suspends a background app and
 * with it the Wi-Fi/TCP link to the radio).
 */
@Composable
fun AppBackgroundSettings() {
    val control = AppBackgroundControls.current ?: return
    val enabled by control.enabled.collectAsStateWithLifecycle()
    val granted by control.permissionGranted.collectAsStateWithLifecycle()

    SwitchPreference(
        title = stringResource(Res.string.background_keepalive_title),
        summary = stringResource(Res.string.background_keepalive_summary),
        checked = enabled,
        enabled = true,
        onCheckedChange = { control.setEnabled(it) },
    )
    if (enabled && !granted) {
        Text(
            text = stringResource(Res.string.background_keepalive_permission),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}
