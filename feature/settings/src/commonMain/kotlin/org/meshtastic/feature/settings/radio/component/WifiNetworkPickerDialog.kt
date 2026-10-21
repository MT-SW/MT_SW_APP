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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.SignalCellular1Bar
import org.meshtastic.core.ui.icon.SignalCellular2Bar
import org.meshtastic.core.ui.icon.SignalCellular3Bar
import org.meshtastic.core.ui.icon.SignalCellular4Bar
import org.meshtastic.core.ui.icon.Wifi
import org.meshtastic.core.ui.util.PhoneWifiNetwork
import org.meshtastic.core.ui.util.PhoneWifiScanner

private const val GOOD_DBM = -60
private const val OK_DBM = -70
private const val FAIR_DBM = -80

@Composable
private fun strengthIcon(rssi: Int?): ImageVector = when {
    rssi == null -> MeshtasticIcons.Wifi
    rssi >= GOOD_DBM -> MeshtasticIcons.SignalCellular4Bar
    rssi >= OK_DBM -> MeshtasticIcons.SignalCellular3Bar
    rssi >= FAIR_DBM -> MeshtasticIcons.SignalCellular2Bar
    else -> MeshtasticIcons.SignalCellular1Bar
}

private fun strengthText(rssi: Int?): String? = when {
    rssi == null -> null
    rssi >= GOOD_DBM -> "Sygnał bardzo dobry"
    rssi >= OK_DBM -> "Sygnał dobry"
    rssi >= FAIR_DBM -> "Sygnał średni"
    else -> "Sygnał słaby"
}

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
    var round by remember { mutableIntStateOf(0) }

    LaunchedEffect(scanner, round) {
        loading = true
        current = runCatching { scanner.currentSsid() }.getOrNull()
        networks = runCatching { scanner.scan() }.getOrDefault(emptyList()).filter { it.is24Ghz != false }
        loading = false
    }

    // The network we are on now first, then the rest strongest first.
    val rows =
        buildList {
            current?.let { name -> add(PhoneWifiNetwork(name, networks.firstOrNull { it.ssid == name }?.rssi, null) to true) }
            networks.filter { it.ssid != current }.forEach { add(it to false) }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wybierz sieć Wi-Fi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Radio łączy się tylko z sieciami 2,4 GHz. Hasło wpisz ręcznie albo zeskanuj kod QR.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Szukam sieci…", style = MaterialTheme.typography.bodyMedium)
                } else if (rows.isEmpty()) {
                    Text(
                        text = "Nie znaleziono sieci. Sprawdź, czy Wi-Fi i lokalizacja są włączone, albo wpisz nazwę ręcznie.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    rows.forEachIndexed { index, (network, isCurrent) ->
                        if (index > 0) HorizontalDivider()
                        ListItem(
                            modifier = Modifier.clickable { onPick(network.ssid) },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            leadingContent = {
                                Icon(
                                    imageVector = strengthIcon(network.rssi),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            headlineContent = { Text(network.ssid, maxLines = 1) },
                            supportingContent = {
                                val text = listOfNotNull(if (isCurrent) "Obecna sieć" else null, strengthText(network.rssi))
                                if (text.isNotEmpty()) Text(text.joinToString(" • "))
                            },
                        )
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = { round++ }, enabled = !loading) { Text("Szukaj ponownie") } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zamknij") } },
    )
}
