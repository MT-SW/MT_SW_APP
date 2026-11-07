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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.font_default
import org.meshtastic.core.resources.font_family
import org.meshtastic.core.resources.font_mono
import org.meshtastic.core.resources.font_sans
import org.meshtastic.core.resources.font_serif
import org.meshtastic.core.resources.font_size
import org.meshtastic.core.resources.font_weight
import org.meshtastic.core.resources.font_weight_bolder
import org.meshtastic.core.resources.font_weight_designed
import org.meshtastic.core.resources.font_weight_lighter
import org.meshtastic.core.resources.font_weight_much_bolder
import org.meshtastic.core.resources.font_weight_much_lighter
import org.meshtastic.core.resources.ui_scale_follow_system
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.icon.FormatPaint
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.theme.FONT_FAMILY_OPTIONS
import org.meshtastic.core.ui.theme.FONT_SIZE_OPTIONS
import org.meshtastic.core.ui.theme.FONT_WEIGHT_SHIFT_OPTIONS
import org.meshtastic.feature.settings.SettingsViewModel

@Composable
private fun fontFamilyLabel(value: Int): String = stringResource(
    when (value) {
        1 -> Res.string.font_sans
        2 -> Res.string.font_serif
        3 -> Res.string.font_mono
        else -> Res.string.font_default
    },
)

@Composable
private fun fontWeightLabel(shift: Int): String = stringResource(
    when {
        shift <= -200 -> Res.string.font_weight_much_lighter
        shift < 0 -> Res.string.font_weight_lighter
        shift == 0 -> Res.string.font_weight_designed
        shift < 200 -> Res.string.font_weight_bolder
        else -> Res.string.font_weight_much_bolder
    },
)

@Composable
private fun fontSizeLabel(percent: Int): String =
    if (percent <= 0) stringResource(Res.string.ui_scale_follow_system) else "$percent%"

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    current: T,
    label: @Composable (T) -> String,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    MeshtasticDialog(
        title = title,
        onDismiss = onDismiss,
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    ListItem(text = label(option) + if (option == current) "  ✓" else "", trailingIcon = null) {
                        onPick(option)
                        onDismiss()
                    }
                }
            }
        },
    )
}

/** Three rows (typeface, weight, text size) with their pickers; they apply to the whole app on every platform. */
@Composable
fun FontSettingsItems(settingsViewModel: SettingsViewModel) {
    val family by settingsViewModel.fontFamily.collectAsStateWithLifecycle()
    val weight by settingsViewModel.fontWeightShift.collectAsStateWithLifecycle()
    val size by settingsViewModel.fontSizePercent.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(0) }

    when (open) {
        1 ->
            ChoiceDialog(
                stringResource(Res.string.font_family),
                FONT_FAMILY_OPTIONS,
                family,
                { fontFamilyLabel(it) },
                { settingsViewModel.setFontFamily(it) },
                { open = 0 },
            )
        2 ->
            ChoiceDialog(
                stringResource(Res.string.font_weight),
                FONT_WEIGHT_SHIFT_OPTIONS,
                weight,
                { fontWeightLabel(it) },
                { settingsViewModel.setFontWeightShift(it) },
                { open = 0 },
            )
        3 ->
            ChoiceDialog(
                stringResource(Res.string.font_size),
                FONT_SIZE_OPTIONS,
                size,
                { fontSizeLabel(it) },
                { settingsViewModel.setFontSizePercent(it) },
                { open = 0 },
            )
    }
    ListItem(
        text = stringResource(Res.string.font_family),
        supportingText = fontFamilyLabel(family),
        leadingIcon = MeshtasticIcons.FormatPaint,
    ) {
        open = 1
    }
    ListItem(
        text = stringResource(Res.string.font_weight),
        supportingText = fontWeightLabel(weight),
        leadingIcon = MeshtasticIcons.FormatPaint,
    ) {
        open = 2
    }
    ListItem(
        text = stringResource(Res.string.font_size),
        supportingText = fontSizeLabel(size),
        leadingIcon = MeshtasticIcons.FormatPaint,
    ) {
        open = 3
    }
}
