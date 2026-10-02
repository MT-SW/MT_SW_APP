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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.okay
import org.meshtastic.core.resources.traceroute
import org.meshtastic.core.resources.view_on_map
import org.meshtastic.core.ui.theme.StatusColors.StatusDisconnected
import org.meshtastic.core.ui.theme.StatusColors.StatusOnline
import org.meshtastic.core.ui.theme.StatusColors.StatusPurple
import org.meshtastic.core.ui.util.LocalModemPreset
import org.meshtastic.core.ui.util.annotateTraceroute
import org.meshtastic.core.ui.util.toMessageRes
import org.meshtastic.core.ui.viewmodel.UIViewModel

/**
 * Handles the display of the traceroute alert when a response is received. Consolidates the side effect logic from the
 * main application screens into common code.
 */
@Composable
fun TracerouteAlertHandler(
    uiViewModel: UIViewModel,
    onNavigateToMap: (destinationNodeNum: Int, requestId: Int, logUuid: String?) -> Unit,
) {
    val traceRouteResponse by uiViewModel.tracerouteResponse.collectAsStateWithLifecycle(null)
    var dismissedTracerouteRequestId by remember { mutableStateOf<Int?>(null) }
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(traceRouteResponse, dismissedTracerouteRequestId) {
        val response = traceRouteResponse
        if (response != null && response.requestId != dismissedTracerouteRequestId) {
            // The map button exists only when every node on the route has a position; otherwise the reason is shown.
            val unavailableRes =
                uiViewModel
                    .tracerouteMapAvailability(
                        forwardRoute = response.forwardRoute,
                        returnRoute = response.returnRoute,
                    )
                    .toMessageRes()
            uiViewModel.showAlert(
                titleRes = Res.string.traceroute,
                composableMessage = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text =
                            annotateTraceroute(
                                response.message,
                                // Parameters are named by SNR tier; the palette matches the signal-quality colours.
                                statusGreen = colorScheme.StatusOnline,
                                statusYellow = colorScheme.StatusDisconnected,
                                statusOrange = colorScheme.StatusPurple,
                                statusRed = Quality.NONE.color.invoke(),
                                modemPreset = LocalModemPreset.current,
                            ),
                        )
                        if (unavailableRes != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = stringResource(unavailableRes), color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmTextRes = if (unavailableRes == null) Res.string.view_on_map else null,
                onConfirm =
                if (unavailableRes != null) {
                    null
                } else {
                    {
                        dismissedTracerouteRequestId = response.requestId
                        onNavigateToMap(response.destinationNodeNum, response.requestId, response.logUuid)
                    }
                },
                dismissTextRes = Res.string.okay,
                onDismiss = {
                    uiViewModel.clearTracerouteResponse()
                    dismissedTracerouteRequestId = null
                },
            )
        }
    }
}
