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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val REGION_DIALOG_CLOSE_DELAY_SECONDS = 10

/**
 * Dialog shown when the phone is physically within the Świętokrzyskie region while the locally-connected radio's LoRa
 * channel is wide (>200 kHz). Same close-button-countdown mechanic as [NarrowBandWarningDialog], shorter text.
 */
@Composable
fun RegionWarningDialog(onClose: () -> Unit) {
    var secondsRemaining by remember { mutableIntStateOf(REGION_DIALOG_CLOSE_DELAY_SECONDS) }

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
        }
    }

    MeshtasticDialog(
        title = "Witamy w woj. Świętokrzyskim!",
        dismissable = false,
        text = {
            Column {
                Text(
                    text =
                    "Na naszym terenie pracujemy na nastawach Custom (62KHz, SF7, CR6) lub preset NarrowFast. " +
                        "Więcej szczegółów znajdziesz na https://mt-sw.pl",
                )
                TextButton(
                    onClick = onClose,
                    enabled = secondsRemaining <= 0,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Text(text = if (secondsRemaining > 0) "Zamknij (${secondsRemaining}s)" else "Zamknij")
                }
            }
        },
    )
}
