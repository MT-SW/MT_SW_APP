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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.meshtastic.core.model.UiOptions

private const val SHEET_CORNER_DP = 28
private const val WIDE_WINDOW_DP = 600
private const val WINDOW_MAX_WIDTH_DP = 560
private const val WINDOW_MAX_HEIGHT_PERCENT = 0.85f

/**
 * MT_SW: the same as the Material bottom sheet `ModalBottomSheet` (the parameters the app uses), but in the look of
 * our app: the shadow (a light glow on a dark theme) and the thin rim along the rounded top edge, like the other
 * elements. Use this instead of the Material one so that every sheet looks the same.
 */
// The sheet state is optional, so the content is composed in one of two branches, never both at once.
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ContentSlotReused")
@Composable
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState? = null,
    windowOnWide: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current.density
    if (windowOnWide && containerSize.width / density >= WIDE_WINDOW_DP) {
        // MT_SW: on a computer (a wide window) the sheet is a scrollable window in the middle, in our look, not a
        // panel coming up from the bottom edge.
        val windowShape = RoundedCornerShape(SHEET_CORNER_DP.dp)
        Dialog(onDismissRequest = onDismissRequest) {
            Surface(
                modifier =
                modifier
                    .widthIn(max = WINDOW_MAX_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .heightIn(max = (containerSize.height / density * WINDOW_MAX_HEIGHT_PERCENT).dp)
                    .elementLook(UiOptions.GROUP_DIALOGS, windowShape)
                    .popupRim(windowShape),
                shape = windowShape,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) { content() }
            }
        }
        return
    }
    val shape = RoundedCornerShape(topStart = SHEET_CORNER_DP.dp, topEnd = SHEET_CORNER_DP.dp)
    val look = modifier.elementLook(UiOptions.GROUP_DIALOGS, shape).popupRim(shape)
    if (sheetState != null) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = look,
            sheetState = sheetState,
            shape = shape,
            content = content,
        )
    } else {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = look,
            shape = shape,
            content = content,
        )
    }
}
