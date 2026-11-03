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

private const val NARROW_BAND_DIALOG_CLOSE_DELAY_SECONDS = 10

/**
 * Educational dialog nudging users whose LoRa channel bandwidth exceeds 200 kHz (the Long/Medium/Short presets, or an
 * equivalent custom config) toward the narrower NarrowFast/NarrowSlow presets.
 *
 * The close button stays disabled — showing a countdown — for [NARROW_BAND_DIALOG_CLOSE_DELAY_SECONDS] seconds, and the
 * dialog cannot be dismissed by tapping outside or pressing back before then, so the message is actually read.
 */
@Composable
fun NarrowBandWarningDialog(onClose: () -> Unit) {
    var secondsRemaining by remember { mutableIntStateOf(NARROW_BAND_DIALOG_CLOSE_DELAY_SECONDS) }

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
            "Narrow channel: what it is and what it gives you"
        } else {
            "Wąski kanał: co to jest i co daje"
        },
        dismissable = false,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text =
                    if (isEnglish) {
                        "Until now, the mesh network has mostly run on the Long Fast, Medium Fast and " +
                            "Short Fast presets. Each of them occupies 250 kHz of bandwidth on the 869.525 " +
                            "MHz frequency. They give fast communication, but everyone works on one shared " +
                            "frequency, and the signal is easily disturbed by other devices and reflections " +
                            "from terrain.\n\n" +
                            "The new Narrow Fast and Narrow Slow presets occupy only 62.5 kHz. Three such " +
                            "channels fit in the same band, so several networks can work side by side, each " +
                            "on its own channel. A narrow channel is also less sensitive to interference " +
                            "and signal reflections.\n\n" +
                            "If you have a weak signal or range problems, you can try the Narrow Fast " +
                            "preset. If you do not want to flash new firmware yet, use Custom settings. You " +
                            "can find an example configuration at https://mt-sw.pl.\n\n" +
                            "If other networks also operate in your area, e.g. MeshCore or Reticulum, a " +
                            "narrow channel helps avoid mutual interference.\n\n" +
                            "This is for information only. The decision to change is yours."
                    } else {
                        "Do tej pory sieć mesh pracowała głównie na presetach Szeroki daleki szybki " +
                            "(Long Fast), Szeroki średni szybki (Medium Fast) i Szeroki krótki szybki " +
                            "(Short Fast). Każdy z nich zajmuje 250 kHz pasma na częstotliwości 869,525 " +
                            "MHz. Dają szybką łączność, ale wszyscy pracują na jednej częstotliwości, a " +
                            "sygnał łatwo zakłócają inne urządzenia i odbicia od terenu.\n\n" +
                            "Nowe presety Wąski szybki (Narrow Fast) i Wąski wolny (Narrow Slow) zajmują " +
                            "tylko 62,5 kHz. W tym samym paśmie mieszczą się 3 takie kanały, więc kilka " +
                            "sieci może pracować obok siebie, każda na swoim kanale. Wąski kanał jest też " +
                            "mniej wrażliwy na zakłócenia i odbicia sygnału.\n\n" +
                            "Jeśli masz słaby sygnał albo kłopoty z zasięgiem, możesz sprawdzić preset " +
                            "Wąski szybki (Narrow Fast). Jeśli nie chcesz jeszcze wgrywać nowego firmware, " +
                            "użyj ustawień Custom. Przykładową konfigurację znajdziesz na https://mt-sw.pl.\n\n" +
                            "Jeśli w okolicy działają też inne sieci, np. MeshCore albo Reticulum, wąski " +
                            "kanał pomaga uniknąć wzajemnych zakłóceń.\n\n" +
                            "To tylko informacja. Decyzja o zmianie należy do Ciebie."
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
