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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.conflate
import org.meshtastic.core.model.MeshActivity
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.theme.StatusColors.StatusReceive
import org.meshtastic.core.ui.theme.StatusColors.StatusTransmit
import org.meshtastic.core.ui.theme.rememberUiOptions
import org.meshtastic.core.ui.util.LocalMeshActivity
import org.meshtastic.proto.EnvironmentMetrics
import org.meshtastic.proto.Paxcount
import org.meshtastic.proto.User

@Composable
fun NodeChip(
    modifier: Modifier = Modifier,
    node: Node,
    onClick: ((Node) -> Unit)? = null,
    /** MT_SW: the shadow blinks with the connection icon when the option in "Look" is on (only our node on the bar). */
    blinkWithActivity: Boolean = false,
) {
    val (textColor, nodeColor) = node.colors
    // MT_SW: the look of the chip follows the style chosen in "Look" (see GlassChip.kt).
    val labelColor = nodeChipTextColor(rememberNodeChipStyle(), Color(textColor))
    val colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = labelColor)
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
    val blink = if (blinkWithActivity) Modifier.activityBlink(MaterialTheme.shapes.small) else Modifier
    Card(
        modifier = modifier.then(blink).then(glass).then(clickable),
        shape = MaterialTheme.shapes.small,
        colors = colors,
    ) {
        content()
    }
}

private const val BLINK_MILLIS = 1000
private const val BLINK_LAYERS = 6
private const val BLINK_REACH_DP = 12f
private const val BLINK_ALPHA = 0.55f
private const val BLINK_DEFAULT_RADIUS_DP = 8f

/**
 * MT_SW: a coloured glow behind the chip that blinks like the connection icon (blue when the app sends, green when it
 * receives); off unless the option in "Look" is on. It is drawn in the draw phase from the activity flow, so a packet
 * never recomposes the bar.
 */
@Composable
internal fun Modifier.activityBlink(shape: Shape, outsideOnly: Boolean = false): Modifier {
    val options = rememberUiOptions()
    val flow = LocalMeshActivity.current
    if (flow == null || options[UiOptions.NODE_CHIP_BLINK] != 1) return this
    val colors = MaterialTheme.colorScheme
    val sendColor = colors.StatusTransmit
    val receiveColor = colors.StatusReceive
    var glow by remember { mutableStateOf(Color.Transparent) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(flow, sendColor, receiveColor) {
        flow.conflate().collect { activity ->
            glow = if (activity is MeshActivity.Send) sendColor else receiveColor
            alpha.stop()
            alpha.snapTo(1f)
            alpha.animateTo(0f, tween(durationMillis = BLINK_MILLIS, easing = LinearEasing))
        }
    }
    return drawBehind { drawBlinkGlow(shape, glow, alpha.value, outsideOnly = outsideOnly) }
}

/** MT_SW: the layered soft glow around a rounded shape used by every blinking shadow (top-bar chip and node list). */
internal fun DrawScope.drawBlinkGlow(shape: Shape, glow: Color, a: Float, outsideOnly: Boolean = false) {
    if (a <= 0f) return
    if (outsideOnly) {
        // Only the shadow around the card blinks: the card itself (which is translucent) must not be tinted.
        val hole = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawBlinkGlow)) }
        clipPath(hole, ClipOp.Difference) { drawBlinkGlow(shape, glow, a, outsideOnly = false) }
        return
    }
    val corner =
        (shape.createOutline(size, layoutDirection, this) as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x
            ?: BLINK_DEFAULT_RADIUS_DP.dp.toPx()
    val reach = BLINK_REACH_DP.dp.toPx()
    for (i in BLINK_LAYERS downTo 1) {
        val grow = reach * i / BLINK_LAYERS
        val inner = 1f - (i - 1f) / BLINK_LAYERS
        val outer = 1f - i.toFloat() / BLINK_LAYERS
        drawRoundRect(
            color = glow.copy(alpha = BLINK_ALPHA * a * (inner * inner - outer * outer)),
            topLeft = Offset(-grow, -grow),
            size = Size(size.width + 2 * grow, size.height + 2 * grow),
            cornerRadius = CornerRadius(corner + grow),
        )
    }
}

/** MT_SW: blink length shared with the node list. */
internal const val NODE_BLINK_MILLIS = BLINK_MILLIS

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
