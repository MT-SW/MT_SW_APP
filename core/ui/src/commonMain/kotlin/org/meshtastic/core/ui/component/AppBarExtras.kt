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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Node
import org.meshtastic.core.repository.SosBarScreens
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.sos_button
import org.meshtastic.core.resources.sos_open

/**
 * What every top bar shows beside its own buttons, provided once at the app root: the plate of our own node (when the
 * radio is connected), and the red SOS button that opens the SOS screen. [sosScreens] are the keys of the cards (see
 * [SosBarScreens]) that show the SOS button; a null [onOpenSos] means there is nowhere to open it (previews, tests).
 */
data class AppBarExtras(
    val ourNode: Node? = null,
    val connected: Boolean = false,
    val sosScreens: Set<String> = emptySet(),
    val onOpenSos: (() -> Unit)? = null,
)

@Suppress("CompositionLocalAllowlist")
val LocalAppBarExtras = compositionLocalOf { AppBarExtras() }

/** The cards of the app that can show the SOS button; the key is what is stored in the settings. */
enum class SosBarScreen(val key: String) {
    CONNECTIONS(SosBarScreens.CONNECTIONS),
    MESSAGES(SosBarScreens.MESSAGES),
    NODES(SosBarScreens.NODES),
    MAP(SosBarScreens.MAP),
    SETTINGS(SosBarScreens.SETTINGS),
    AI(SosBarScreens.AI),
    HEALTH(SosBarScreens.HEALTH),
    OTHER(SosBarScreens.OTHER),
}

private val SosRed = Color(0xFFD32F2F)
private val SosGold = Color(0xFFF5C839)

/**
 * The SOS button of the top bars, made like the plate of a node (same shape and height) so the two sit together: red
 * inside with a gold border and bold white letters, to be seen at once and read at a glance.
 */
@Composable
fun SosChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(Res.string.sos_open)
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = SosRed, contentColor = Color.White),
        border = BorderStroke(2.dp, SosGold),
    ) {
        Box(
            modifier =
            Modifier.width(IntrinsicSize.Min)
                .defaultMinSize(minWidth = 48.dp, minHeight = 28.dp)
                .padding(horizontal = 8.dp)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.sos_button),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
        }
    }
}
