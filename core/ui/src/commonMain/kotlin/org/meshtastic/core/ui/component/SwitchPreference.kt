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
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package org.meshtastic.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.assistant.assistantHighlight
import org.meshtastic.core.ui.theme.AppTheme

@Composable
fun SwitchPreference(
    modifier: Modifier = Modifier,
    title: String,
    summary: String = "",
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    padding: PaddingValues? = null,
    containerColor: Color? = null,
    loading: Boolean = false,
) {
    // MT_SW: a switched-on row no longer gets Material's flat blue-grey fill. It gets a soft glass tint in the colour of
    // the theme (lighter at the top) and a thin outline on the left, which fits the dark and the light look.
    val currentColors =
        ListItemDefaults.colors(
            containerColor = containerColor ?: Color.Unspecified,
            selectedContainerColor = Color.Transparent,
        )
    val tinted = Modifier.switchOnGlass(checked && enabled && containerColor == null)

    ListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = tinted.then(padding?.let { Modifier.padding(it) } ?: modifier).assistantHighlight(title),
        enabled = enabled,
        shapes = ListItemDefaults.shapes(shape = RectangleShape),
        colors = currentColors,
        trailingContent = {
            AnimatedContent(targetState = loading) { loading ->
                if (loading) {
                    ProgressSpinner(size = 24.dp)
                } else {
                    Switch(enabled = enabled, checked = checked, onCheckedChange = null)
                }
            }
        },
        supportingContent = {
            if (summary.isNotEmpty()) {
                Text(text = summary)
            }
        },
        content = { Text(text = title) },
    )
}

@Preview(showBackground = true)
@Composable
fun SwitchPreferencePreview() {
    AppTheme { SwitchPreference(title = "Setting", checked = true, enabled = true, onCheckedChange = {}) }
}

/** MT_SW: the glass tint and the accent bar of a row whose switch is on; shared by every row with a switch. */
@Composable
internal fun Modifier.switchOnGlass(on: Boolean): Modifier {
    val accent = MaterialTheme.colorScheme.primary
    val tintAlpha by animateFloatAsState(if (on) 1f else 0f)
    return this.drawBehind {
        if (tintAlpha > 0f) {
            // The whole row is one glass pane: a soft tint inside and a thin outline all the way around it.
            val inset = SWITCH_INSET_DP.dp.toPx()
            val topLeft = Offset(inset, inset)
            val paneSize = Size(size.width - 2 * inset, size.height - 2 * inset)
            val corner = CornerRadius(SWITCH_CORNER_DP.dp.toPx())
            drawRoundRect(
                brush =
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = SWITCH_TINT_TOP * tintAlpha),
                        accent.copy(alpha = SWITCH_TINT_BOTTOM * tintAlpha),
                    ),
                ),
                topLeft = topLeft,
                size = paneSize,
                cornerRadius = corner,
            )
            drawRoundRect(
                brush =
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = SWITCH_EDGE_TOP * tintAlpha),
                        accent.copy(alpha = SWITCH_EDGE_BOTTOM * tintAlpha),
                    ),
                ),
                topLeft = topLeft,
                size = paneSize,
                cornerRadius = corner,
                style = Stroke(width = SWITCH_EDGE_DP.dp.toPx()),
            )
        }
    }
}

private const val SWITCH_TINT_TOP = 0.16f
private const val SWITCH_TINT_BOTTOM = 0.04f
private const val SWITCH_EDGE_TOP = 0.95f
private const val SWITCH_EDGE_BOTTOM = 0.55f
private const val SWITCH_EDGE_DP = 1.2f
private const val SWITCH_INSET_DP = 2f
private const val SWITCH_CORNER_DP = 12f
