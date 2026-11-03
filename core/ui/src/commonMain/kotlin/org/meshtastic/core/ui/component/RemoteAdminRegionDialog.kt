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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val REMOTE_ADMIN_DIALOG_CLOSE_DELAY_SECONDS = 10

/**
 * Notice shown when remote admin is opened on a node while the connected radio has a wide channel (>200 kHz) and either
 * this phone or the node being configured is inside the Świętokrzyskie region. It names the preset the regional network
 * runs on, so it is worded around the settings being changed rather than around the app. Same close-button countdown as
 * [NarrowBandWarningDialog].
 */
@Composable
fun RemoteAdminRegionDialog(onClose: () -> Unit) {
    var secondsRemaining by remember { mutableIntStateOf(REMOTE_ADMIN_DIALOG_CLOSE_DELAY_SECONDS) }

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
        }
    }

    val isEnglish = Locale.current.language == "en"

    MeshtasticDialog(
        title =
        if (isEnglish) {
            "You are changing settings in the Świętokrzyskie region"
        } else {
            "Zmieniasz ustawienia na terenie woj. świętokrzyskiego"
        },
        dismissable = false,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text =
                    if (isEnglish) {
                        "You are either in the Świętokrzyskie voivodeship or the device you are configuring is. " +
                            "The local network works on the Narrow Fast preset (a 62.5 kHz channel), and this " +
                            "radio is still on a wide one.\n\nBefore you save anything, consider switching it to " +
                            "Narrow Fast: it copes better with interference and reflections, and it lets you " +
                            "hear the whole local network and be heard by it.\n\nDetails: https://mt-sw.pl"
                    } else {
                        "Jesteś na terenie woj. świętokrzyskiego albo urządzenie, które konfigurujesz, jest na " +
                            "jego terenie. Nasza sieć pracuje na presecie Wąski szybki (Narrow Fast), czyli na " +
                            "wąskim kanale 62,5 kHz, a to radio nadal ma szeroki kanał.\n\nZanim zapiszesz " +
                            "zmiany, rozważ przełączenie go na Wąski szybki (Narrow Fast): lepiej radzi sobie z " +
                            "zakłóceniami i odbiciami sygnału, a Ty usłyszysz całą lokalną sieć i będziesz przez " +
                            "nią słyszany.\n\nSzczegóły: https://mt-sw.pl"
                    },
                )
                TextButton(
                    onClick = onClose,
                    enabled = secondsRemaining <= 0,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Text(
                        text =
                        when {
                            secondsRemaining > 0 && isEnglish -> "Close (${secondsRemaining}s)"
                            secondsRemaining > 0 -> "Zamknij (${secondsRemaining}s)"
                            isEnglish -> "Close"
                            else -> "Zamknij"
                        },
                    )
                }
            }
        },
    )
}
