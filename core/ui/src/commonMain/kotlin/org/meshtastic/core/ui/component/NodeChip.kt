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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.meshtastic.core.model.Node
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.proto.EnvironmentMetrics
import org.meshtastic.proto.Paxcount
import org.meshtastic.proto.User

@Composable
fun NodeChip(modifier: Modifier = Modifier, node: Node, onClick: ((Node) -> Unit)? = null) {
    val (textColor, nodeColor) = node.colors
    // MT_SW: the look of the chip follows the style chosen in "Look" (see GlassChip.kt).
    val colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = Color(textColor))
    val glass = nodeChipLook(Color(nodeColor), Color(textColor), MaterialTheme.shapes.small)

    val content: @Composable () -> Unit = {
        Box(
            modifier =
            Modifier.width(IntrinsicSize.Min)
                .defaultMinSize(minWidth = 64.dp, minHeight = 28.dp)
                .padding(horizontal = 8.dp)
                .semantics { contentDescription = node.user.short_name.ifEmpty { "Node" } },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = node.user.short_name.ifEmpty { "???" },
                style = MaterialTheme.typography.labelLarge,
                textDecoration = TextDecoration.LineThrough.takeIf { node.isIgnored },
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }

    // A Card with onClick pads itself out to the 48dp touch size, and the glass drawn on its modifier would cover that
    // whole area (big squares); so the card is not clickable itself and the click is added after the glass.
    val clickable =
        if (onClick == null) Modifier else Modifier.clip(MaterialTheme.shapes.small).clickable { onClick(node) }
    Card(modifier = modifier.then(glass).then(clickable), shape = MaterialTheme.shapes.small, colors = colors) {
        content()
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
fun NodeChipPreview() {
    val user =
        User.Builder()
            .also { wb ->
                wb.short_name = "JD"
                wb.long_name = "John Doe"
            }
            .build()
    val node =
        Node(
            num = 13444,
            user = user,
            isIgnored = false,
            paxcounter =
            Paxcount.Builder()
                .also { wb ->
                    wb.ble = 10
                    wb.wifi = 5
                }
                .build(),
            environmentMetrics =
            EnvironmentMetrics.Builder()
                .also { wb ->
                    wb.temperature = 25f
                    wb.relative_humidity = 60f
                }
                .build(),
        )
    AppTheme { NodeChip(node = node) }
}
