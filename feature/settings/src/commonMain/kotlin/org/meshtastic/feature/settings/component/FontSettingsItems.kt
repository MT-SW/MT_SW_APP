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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.font_default
import org.meshtastic.core.resources.font_family
import org.meshtastic.core.resources.font_preview
import org.meshtastic.core.resources.font_search
import org.meshtastic.core.resources.font_size
import org.meshtastic.core.resources.font_weight
import org.meshtastic.core.resources.font_weight_designed
import org.meshtastic.core.resources.ui_scale_follow_system
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.icon.FormatPaint
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.theme.FONT_SIZE_OPTIONS
import org.meshtastic.core.ui.theme.FONT_WEIGHT_MAX
import org.meshtastic.core.ui.theme.FONT_WEIGHT_MIN
import org.meshtastic.core.ui.theme.FONT_WEIGHT_STEP
import org.meshtastic.core.ui.theme.systemFontFamily
import org.meshtastic.core.ui.theme.systemFontNames

private const val LIST_HEIGHT_DP = 360
private const val BASE_WEIGHT = 400

@Composable
private fun fontSizeLabel(percent: Int): String =
    if (percent <= 0) stringResource(Res.string.ui_scale_follow_system) else "$percent%"

@Composable
private fun fontWeightLabel(shift: Int): String =
    if (shift == 0) stringResource(Res.string.font_weight_designed) else (if (shift > 0) "+$shift" else "$shift")

/** Searchable list of every font installed on this device; each name is shown in its own font. */
@Composable
private fun FontFamilyDialog(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val all = remember { systemFontNames() }
    var query by remember { mutableStateOf("") }
    val shown = remember(query, all) { all.filter { it.contains(query.trim(), ignoreCase = true) } }
    val defaultName = stringResource(Res.string.font_default)
    MeshtasticDialog(
        title = stringResource(Res.string.font_family),
        onDismiss = onDismiss,
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(Res.string.font_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.height(LIST_HEIGHT_DP.dp)) {
                    if (query.isBlank()) {
                        item {
                            ListItem(text = defaultName + if (current.isEmpty()) "  ✓" else "", trailingIcon = null) {
                                onPick("")
                                onDismiss()
                            }
                        }
                    }
                    items(shown) { name ->
                        val family = remember(name) { systemFontFamily(name, BASE_WEIGHT) }
                        Text(
                            text = name + if (name == current) "  ✓" else "",
                            style = MaterialTheme.typography.bodyLarge.merge(TextStyle(fontFamily = family)),
                            modifier =
                            Modifier.fillMaxWidth()
                                .clickable {
                                    onPick(name)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
    )
}

/** Slider that changes the weight of all text live, with a sample line that shows the result. */
@Composable
private fun FontWeightDialog(current: Int, onChange: (Int) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableIntStateOf(current) }
    val steps = (FONT_WEIGHT_MAX - FONT_WEIGHT_MIN) / FONT_WEIGHT_STEP - 1
    MeshtasticDialog(
        title = stringResource(Res.string.font_weight),
        onDismiss = onDismiss,
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(fontWeightLabel(value), style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = value.toFloat(),
                    onValueChange = {
                        value = it.toInt()
                        onChange(value)
                    },
                    valueRange = FONT_WEIGHT_MIN.toFloat()..FONT_WEIGHT_MAX.toFloat(),
                    steps = steps,
                )
                Text(
                    stringResource(Res.string.font_preview),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        },
    )
}

@Composable
private fun FontSizeDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    MeshtasticDialog(
        title = stringResource(Res.string.font_size),
        onDismiss = onDismiss,
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FONT_SIZE_OPTIONS.forEach { percent ->
                    ListItem(
                        text = fontSizeLabel(percent) + if (percent == current) "  ✓" else "",
                        trailingIcon = null,
                    ) {
                        onPick(percent)
                        onDismiss()
                    }
                }
            }
        },
    )
}

private enum class FontDialogKind {
    None,
    Family,
    Weight,
    Size,
}

/** Three rows (font, weight, text size) with their pickers; they apply to the whole app on every platform. */
@Composable
fun FontSettingsItems(
    family: String,
    weight: Int,
    size: Int,
    onFamily: (String) -> Unit,
    onWeight: (Int) -> Unit,
    onSize: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(FontDialogKind.None) }
    val close = { open = FontDialogKind.None }

    Column(modifier) {
        when (open) {
            FontDialogKind.Family -> FontFamilyDialog(family, onFamily, close)
            FontDialogKind.Weight -> FontWeightDialog(weight, onWeight, close)
            FontDialogKind.Size -> FontSizeDialog(size, onSize, close)
            FontDialogKind.None -> Unit
        }
        ListItem(
            text = stringResource(Res.string.font_family),
            supportingText = family.ifEmpty { stringResource(Res.string.font_default) },
            leadingIcon = MeshtasticIcons.FormatPaint,
        ) {
            open = FontDialogKind.Family
        }
        ListItem(
            text = stringResource(Res.string.font_weight),
            supportingText = fontWeightLabel(weight),
            leadingIcon = MeshtasticIcons.FormatPaint,
        ) {
            open = FontDialogKind.Weight
        }
        ListItem(
            text = stringResource(Res.string.font_size),
            supportingText = fontSizeLabel(size),
            leadingIcon = MeshtasticIcons.FormatPaint,
        ) {
            open = FontDialogKind.Size
        }
    }
}
