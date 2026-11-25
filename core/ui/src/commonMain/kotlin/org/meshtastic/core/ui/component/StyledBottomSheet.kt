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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.ui.icon.Close
import org.meshtastic.core.ui.icon.MeshtasticIcons

private const val SHEET_CORNER_DP = 28
private const val WINDOW_MAX_WIDTH_DP = 560
private const val WINDOW_MARGIN_DP = 16
private const val WINDOW_MAX_HEIGHT_PERCENT = 0.85f
private const val CLOSE_BUTTON_DP = 40

/** How far (in dp) a downward swipe has to go to close the window. */
private const val SWIPE_CLOSE_DP = 40
private const val HANDLE_WIDTH_DP = 40
private const val HANDLE_HEIGHT_DP = 4

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
    closeCross: Boolean = true,
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
                // A swipe down anywhere on the window closes it.
                .windowSwipeDownToClose(onDismissRequest)
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
                    // A close cross at the top, so the window can always be closed with one touch. Windows that
                    // have their own original cross in their header pass closeCross = false.
                    if (closeCross) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.weight(1f).height(CLOSE_BUTTON_DP.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier =
                                    Modifier.width(HANDLE_WIDTH_DP.dp)
                                        .height(HANDLE_HEIGHT_DP.dp)
                                        .background(
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            RoundedCornerShape(HANDLE_HEIGHT_DP.dp),
                                        ),
                                )
                            }
                            IconButton(onClick = onDismissRequest, modifier = Modifier.size(CLOSE_BUTTON_DP.dp)) {
                                Icon(
                                    imageVector = MeshtasticIcons.Close,
                                    contentDescription = stringResource(Res.string.close),
                                )
                            }
                        }
                    }
                    content()
                }
            }
        }
    }
}

/**
 * MT_SW: a swipe down anywhere on the window closes it. Two cases are covered: content that scrolls (the swipe closes
 * the window only when the content is already at its top and has nothing left to scroll, reported by the nested
 * scroll), and content that does not scroll (a plain downward drag that nobody else used). A drag that scrolls the
 * content, or a tap on a button, never closes the window.
 */
@Composable
private fun Modifier.windowSwipeDownToClose(onClose: () -> Unit): Modifier {
    val threshold = with(LocalDensity.current) { SWIPE_CLOSE_DP.dp.toPx() }
    val close = rememberUpdatedState(onClose)
    val connection = remember {
        object : NestedScrollConnection {
            private var dragged = 0f

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f && consumed.y == 0f) {
                    dragged += available.y
                    if (dragged > threshold) {
                        dragged = Float.NEGATIVE_INFINITY
                        close.value()
                    }
                } else {
                    dragged = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                dragged = 0f
                return Velocity.Zero
            }
        }
    }
    return nestedScroll(connection).pointerInput(threshold) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            var dragged = 0f
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull() ?: break
                if (!change.pressed || change.isConsumed) break
                dragged += change.positionChange().y
                if (dragged > threshold) {
                    close.value()
                    break
                }
            }
        }
    }
}
