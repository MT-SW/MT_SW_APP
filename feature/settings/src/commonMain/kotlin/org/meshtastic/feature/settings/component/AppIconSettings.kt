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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.app_icon_auto
import org.meshtastic.core.resources.app_icon_clear
import org.meshtastic.core.resources.app_icon_dark
import org.meshtastic.core.resources.app_icon_flat
import org.meshtastic.core.resources.app_icon_hint
import org.meshtastic.core.resources.app_icon_standard
import org.meshtastic.core.resources.app_icon_tinted
import org.meshtastic.core.resources.app_icon_title
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.icon.FormatPaint
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.feature.settings.AppIconStyle

/** Icon styles; the key is what is saved ("" is automatic). The same on every platform. */
private val ICON_STYLES: List<Pair<String, StringResource>> =
    listOf(
        "" to Res.string.app_icon_auto,
        "standard" to Res.string.app_icon_standard,
        "dark" to Res.string.app_icon_dark,
        "clear" to Res.string.app_icon_clear,
        "tinted" to Res.string.app_icon_tinted,
    )

@Composable
private fun appIconLabel(key: String): String =
    stringResource((ICON_STYLES.firstOrNull { it.first == AppIconStyle.base(key) } ?: ICON_STYLES.first()).second) +
        if (AppIconStyle.isFlat(key)) " · " + stringResource(Res.string.app_icon_flat) else ""

/** One row "Ikona aplikacji" with a picker of the icon styles and one flat-icon tick for all of them (also found by the settings search). */
@Composable
fun AppIconSettingsItem(current: String, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    if (open) {
        MeshtasticDialog(
            title = stringResource(Res.string.app_icon_title),
            onDismiss = { open = false },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    val flat = AppIconStyle.isFlat(current)
                    // One tick for every style below: the same icon without the 3D shading.
                    Row(
                        modifier =
                        Modifier.fillMaxWidth()
                            .toggleable(
                                value = flat,
                                role = Role.Checkbox,
                                onValueChange = { onPick(AppIconStyle.withFlat(current, it)) },
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = flat, onCheckedChange = null)
                        Text(
                            text = stringResource(Res.string.app_icon_flat),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                    HorizontalDivider()
                    ICON_STYLES.forEach { (key, label) ->
                        ListItem(
                            text = stringResource(label) + if (key == AppIconStyle.base(current)) "  ✓" else "",
                            trailingIcon = null,
                        ) {
                            onPick(AppIconStyle.withFlat(key, flat))
                            open = false
                        }
                    }
                    Text(
                        text = stringResource(Res.string.app_icon_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }
    ListItem(
        modifier = modifier,
        text = stringResource(Res.string.app_icon_title),
        supportingText = appIconLabel(current),
        leadingIcon = MeshtasticIcons.FormatPaint,
    ) {
        open = true
    }
}
