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
@file:Suppress("MagicNumber", "ModifierMissing")

package org.meshtastic.feature.settings.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.node_chip_style
import org.meshtastic.core.resources.node_chip_style_flat
import org.meshtastic.core.resources.node_chip_style_glass_3d
import org.meshtastic.core.resources.node_chip_style_preview_hint
import org.meshtastic.core.resources.node_chip_style_standard
import org.meshtastic.core.resources.node_chip_style_summary
import org.meshtastic.core.resources.view_style
import org.meshtastic.core.resources.view_style_flat
import org.meshtastic.core.resources.view_style_glass
import org.meshtastic.core.resources.view_style_standard
import org.meshtastic.core.resources.view_style_summary
import org.meshtastic.core.ui.component.DropDownPreference
import org.meshtastic.core.ui.component.nodeChipLook

private val NODE_CHIP_STYLES =
    listOf(UiOptions.NODE_CHIP_STANDARD, UiOptions.NODE_CHIP_GLASS_3D, UiOptions.NODE_CHIP_FLAT)

private val SAMPLE_BACKGROUND = Color(0xFFF5C839)
private val SAMPLE_TEXT = Color(0xFF2C2D3C)

/**
 * MT_SW: the style of the node chips (the coloured plates with a short name). A preview of all styles (tap one to
 * choose it) and the same choice as a list. It applies to the lists, the top bar, the map and the terrain panorama.
 */
@Composable
fun NodeChipStyleSettings(options: UiOptions, onChange: (String, Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val current = options[UiOptions.NODE_CHIP_STYLE]
        Text(
            text = stringResource(Res.string.node_chip_style_preview_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (style in NODE_CHIP_STYLES) {
                NodeChipStyleSample(
                    label = nodeChipStyleName(style),
                    style = style,
                    selected = style == current,
                    onClick = { onChange(UiOptions.NODE_CHIP_STYLE, style) },
                )
            }
        }
        DropDownPreference(
            title = stringResource(Res.string.node_chip_style),
            enabled = true,
            items = NODE_CHIP_STYLES.map { it to nodeChipStyleName(it) },
            selectedItem = current,
            onItemSelected = { onChange(UiOptions.NODE_CHIP_STYLE, it) },
            summary = stringResource(Res.string.node_chip_style_summary),
        )
        DropDownPreference(
            title = stringResource(Res.string.view_style),
            enabled = true,
            items =
            listOf(
                UiOptions.VIEW_STYLE_GLASS to stringResource(Res.string.view_style_glass),
                UiOptions.VIEW_STYLE_STANDARD to stringResource(Res.string.view_style_standard),
                UiOptions.VIEW_STYLE_FLAT to stringResource(Res.string.view_style_flat),
            ),
            selectedItem = options[UiOptions.VIEW_STYLE],
            onItemSelected = { onChange(UiOptions.VIEW_STYLE, it) },
            summary = stringResource(Res.string.view_style_summary),
        )
    }
}

@Composable
private fun nodeChipStyleName(style: Int): String = when (style) {
    UiOptions.NODE_CHIP_GLASS_3D -> stringResource(Res.string.node_chip_style_glass_3d)
    UiOptions.NODE_CHIP_FLAT -> stringResource(Res.string.node_chip_style_flat)
    else -> stringResource(Res.string.node_chip_style_standard)
}

/** One row of the preview: the name of a style with a sample chip drawn in that style. */
@Composable
private fun NodeChipStyleSample(label: String, style: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    val border = if (selected) BorderStroke(2.dp, colors.primary) else BorderStroke(1.dp, colors.outlineVariant)
    Row(
        modifier =
        Modifier.fillMaxWidth().clip(shape).border(border, shape).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        val chipShape = MaterialTheme.shapes.small
        Box(
            modifier =
            Modifier.defaultMinSize(minWidth = 64.dp, minHeight = 28.dp)
                .then(nodeChipLook(SAMPLE_BACKGROUND, SAMPLE_TEXT, chipShape, style))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "MT_SW", style = MaterialTheme.typography.labelLarge, color = SAMPLE_TEXT)
        }
    }
}
