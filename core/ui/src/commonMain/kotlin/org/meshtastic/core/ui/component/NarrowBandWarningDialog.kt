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

    MeshtasticDialog(
        title = "Czym jest NarrowFast, i dlaczego warto go przetestować?",
        dismissable = false,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text =
                    "Do tej pory, sieć Meshtastic bazowała na standardowych presetach LongFast, MediumFast czy " +
                        "ShortFast, któe łączy jedna wspólna cecha - ich emisja zajmuje dokładnie 250 KHz pasma na " +
                        "częstotliwości 869,525 MHz. Pozawalają one na komunikację z dużą prędkością, jednak nie są " +
                        "odporne na lokalne zakłócenia, czy odbicia sygnału od przeszkód terenowych.\n\n" +
                        "W najnowszej wersji firmware Meshtastic, wprowadzone zostały nowe presety, pracujące na " +
                        "wąskim kanale transmisji danych. Presety te, nazwane NarrowFast, oraz NarrowSlow, " +
                        "zajmujące dokładnie 62,5 KHz i pozwalają na użycie jednego z 4 kanałów od 869.43125 do " +
                        "869.61875 MHz. Węższy kanał transmisji danych w presetach Narrow charakteryzuje się " +
                        "większą odpornością na zakłócenia, oraz odbicia sygnału, dodatkowo pozwala na dotychczas " +
                        "jednej częstotliwości dostępnej dla sieci MT czyli 869,525 MHz pracę 4 niezależnych sieci " +
                        "na 4 osobnych kanałach. Jeśli masz kłopoty z zasięgiem, słabym sygnałem, sprawdź już dziś " +
                        "nowy preset NarrowFast, lub jeśli nie chcesz jeszcze wgrywać nie stabilnego firmware, " +
                        "skorzystaj z ustawień Custom (przykład takiej konfiguracji znajdziesz na stronie " +
                        "https://mt-sw.pl ) Jeśli w Twojej okolicy pracują opróćz Meshtastica " +
                        "inne sieci, jak Meshcore czy Reticulum, przetestuj preset NarrowFast, unikniesz wtedy " +
                        "zakłóceń ze strony tych sieci, i zarazem sam nie będziesz ich powodował innym użytkownikom " +
                        "pasma.",
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
