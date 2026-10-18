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
package org.meshtastic.feature.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ui_scale
import org.meshtastic.core.resources.ui_scale_follow_system
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.theme.UI_SCALE_OPTIONS

/** Label of one scale choice: "Follow the phone" for 0, otherwise the percent. */
@Composable
fun uiScaleLabel(percent: Int): String =
    if (percent <= 0) stringResource(Res.string.ui_scale_follow_system) else "$percent%"

/** Dialog for picking the in-app scale (independent of the phone's font and display size). */
@Composable
fun UiScalePickerDialog(current: Int, onClickScale: (Int) -> Unit, onDismiss: () -> Unit) {
    MeshtasticDialog(
        title = stringResource(Res.string.ui_scale),
        onDismiss = onDismiss,
        text = {
            Column {
                UI_SCALE_OPTIONS.forEach { percent ->
                    ListItem(
                        text = uiScaleLabel(percent) + if (percent == current) "  ✓" else "",
                        trailingIcon = null,
                    ) {
                        onClickScale(percent)
                        onDismiss()
                    }
                }
            }
        },
    )
}
