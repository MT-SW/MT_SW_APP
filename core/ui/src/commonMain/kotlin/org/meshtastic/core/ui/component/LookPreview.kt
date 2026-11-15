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
package org.meshtastic.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.element_group_bubbles
import org.meshtastic.core.resources.element_group_cards
import org.meshtastic.core.resources.element_group_input
import org.meshtastic.core.resources.element_group_navbar
import org.meshtastic.core.ui.icon.Groups
import org.meshtastic.core.ui.icon.Icon
import org.meshtastic.core.ui.icon.Info
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Refresh

/**
 * A live preview of the chosen look: a card, a bubble, a field and a floating bar over a colourful background (so the
 * glass and the see-through look can be seen at once). It follows every change of the options as it is made.
 */
@Composable
fun LookPreview(modifier: Modifier = Modifier) {
    val hazeState = rememberHazeState()
    val scheme = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(PREVIEW_CORNER_DP.dp))) {
        Box(
            Modifier.matchParentSize()
                .hazeSource(hazeState)
                .background(Brush.linearGradient(listOf(scheme.primary, scheme.tertiary, scheme.secondary))),
        )
        Column(Modifier.padding(PREVIEW_PADDING_DP.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StyledSurface(
                group = UiOptions.GROUP_CARDS,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                hazeState = hazeState,
            ) {
                Text(stringResource(Res.string.element_group_cards), Modifier.padding(14.dp))
            }
            StyledSurface(
                group = UiOptions.GROUP_BUBBLES,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.align(Alignment.End),
                hazeState = hazeState,
            ) {
                Text(
                    stringResource(Res.string.element_group_bubbles),
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
            StyledSurface(
                group = UiOptions.GROUP_INPUT,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth(),
                hazeState = hazeState,
            ) {
                Text(
                    stringResource(Res.string.element_group_input),
                    Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
            StyledSurface(
                group = UiOptions.GROUP_NAV,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth(),
                hazeState = hazeState,
                floating = true,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(MeshtasticIcons.Groups, contentDescription = stringResource(Res.string.element_group_navbar))
                    Icon(MeshtasticIcons.Refresh, contentDescription = null)
                    Icon(MeshtasticIcons.Info, contentDescription = null)
                }
            }
        }
    }
}

private const val PREVIEW_CORNER_DP = 24
private const val PREVIEW_PADDING_DP = 16
