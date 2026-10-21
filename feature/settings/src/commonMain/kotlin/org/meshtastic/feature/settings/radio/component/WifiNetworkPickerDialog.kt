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
package org.meshtastic.feature.settings.radio.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.util.PhoneWifiNetwork
import org.meshtastic.core.ui.util.PhoneWifiScanner

/**
 * Lets the user pick the radio's Wi-Fi network name from the networks this phone or computer sees. Only 2.4 GHz
 * networks are offered (the radios cannot use 5 GHz); the network this device is on now is listed first. The password
 * is never read: it is typed by hand or scanned from a QR code.
 */
@Composable
fun WifiNetworkPickerDialog(scanner: PhoneWifiScanner, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var current by remember { mutableStateOf<String?>(null) }
    var networks by remember { mutableStateOf<List<PhoneWifiNetwork>>(emptyList()) }

    LaunchedEffect(scanner) {
        current = runCatching { scanner.currentSsid() }.getOrNull()
        networks = runCatching { scanner.scan() }.getOrDefault(emptyList()).filter { it.is24Ghz != false }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sieci Wi-Fi w pobliżu") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Pokazane są tylko sieci 2,4 GHz. Hasło wpisz ręcznie albo zeskanuj kod QR.",
                    style = MaterialTheme.typography.bodySmall,
                )
                val names = buildList {
                    current?.let { add(it to true) }
                    networks.filter { it.ssid != current }.forEach { add(it.ssid to false) }
                }
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                } else if (names.isEmpty()) {
                    Text(
                        text = "Nie znaleziono sieci. Sprawdź, czy Wi-Fi i lokalizacja są włączone, albo wpisz nazwę ręcznie.",
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                names.forEach { (name, isCurrent) ->
                    TextButton(onClick = { onPick(name) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (isCurrent) "$name (obecna sieć)" else name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zamknij") } },
    )
}
