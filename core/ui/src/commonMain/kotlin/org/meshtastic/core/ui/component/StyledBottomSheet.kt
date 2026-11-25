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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.ui.icon.MeshtasticIcons
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.meshtastic.core.model.UiOptions

private const val SHEET_CORNER_DP = 28
private const val WINDOW_MAX_WIDTH_DP = 560
private const val WINDOW_MARGIN_DP = 16
private const val WINDOW_MAX_HEIGHT_PERCENT = 0.85f
private const val CLOSE_BUTTON_DP = 40

/** How far (in dp) a downward swipe has to go to close the window. */
private const val SWIPE_CLOSE_DP = 72

/**
 * MT_SW: the same as the Material bottom sheet `ModalBottomSheet` (the parameters the app uses), but in the look of our
 * app: the shadow (a light glow on a dark theme) and the thin rim along the rounded top edge, like the other elements.
 * Use this instead of the Material one so that every sheet looks the same.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current.density
    // MT_SW: everywhere (phone and computer) the sheet is a pop-up window in the middle, in our look, not a panel
    // coming up from the bottom edge. The content scrolls by itself where it needs to; the window only limits
    // its height (an outer scroll would clash with the content's own one).
    val windowShape = RoundedCornerShape(SHEET_CORNER_DP.dp)
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier =
            modifier
                .padding(WINDOW_MARGIN_DP.dp)
                .widthIn(max = WINDOW_MAX_WIDTH_DP.dp)
                .fillMaxWidth()
                .heightIn(max = (containerSize.height / density * WINDOW_MAX_HEIGHT_PERCENT).dp)
                .background(MaterialTheme.colorScheme.surface, windowShape)
                .elementLook(UiOptions.GROUP_DIALOGS, windowShape)
                .popupRim(windowShape)
                // A swipe down closes the window (a swipe that the content uses for scrolling does not reach here).
                .pointerInput(onDismissRequest) {
                    var dragged = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = { dragged = 0f },
                        onDragCancel = { dragged = 0f },
                    ) { _, amount ->
                        dragged += amount
                        if (dragged > SWIPE_CLOSE_DP.dp.toPx()) {
                            dragged = Float.NEGATIVE_INFINITY
                            onDismissRequest()
                        }
                    }
                }
                // Like the sheet it replaces, the window can be dismissed through accessibility and tests.
                .semantics { dismiss { onDismissRequest().let { true } } },
            shape = windowShape,
            color = Color.Transparent,
        ) {
            // Rows such as ListItem paint the theme's surface colour by default, which would hide the window's
            // gradient behind a flat fill. Inside the window that colour is transparent, so the whole window has
            // one background with one gradient.
            MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = Color.Transparent)) {
                Column {
                    // A close cross at the top, so the window can always be closed with one touch.
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(onClick = onDismissRequest, modifier = Modifier.size(CLOSE_BUTTON_DP.dp)) {
                            Icon(
                                imageVector = MeshtasticIcons.Close,
                                contentDescription = stringResource(Res.string.close),
                            )
                        }
                    }
                    content()
                }
            }
        }
    }
}
