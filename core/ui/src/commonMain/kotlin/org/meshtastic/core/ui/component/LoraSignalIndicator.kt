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
@file:Suppress("MagicNumber")

package org.meshtastic.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.bad
import org.meshtastic.core.resources.fair
import org.meshtastic.core.resources.good
import org.meshtastic.core.resources.ic_signal_cellular_4_bar
import org.meshtastic.core.resources.ic_signal_cellular_alt
import org.meshtastic.core.resources.ic_signal_cellular_alt_1_bar
import org.meshtastic.core.resources.ic_signal_cellular_alt_2_bar
import org.meshtastic.core.resources.none_quality
import org.meshtastic.core.resources.rssi
import org.meshtastic.core.resources.signal
import org.meshtastic.core.resources.signal_quality
import org.meshtastic.core.resources.snr
import org.meshtastic.core.resources.unknown
import org.meshtastic.core.ui.theme.StatusColors.StatusConnecting
import org.meshtastic.core.ui.theme.StatusColors.StatusDisconnected
import org.meshtastic.core.ui.theme.StatusColors.StatusOnline
import org.meshtastic.core.ui.theme.StatusColors.StatusPurple
import org.meshtastic.core.ui.util.LocalModemPreset
import org.meshtastic.proto.Config.LoRaConfig.ModemPreset

// RSSI display bands match Apple's getRssiColor; quality needs SNR and a preset instead.
const val RSSI_GOOD_THRESHOLD = -115
const val RSSI_FAIR_THRESHOLD = -120
const val RSSI_BAD_THRESHOLD = -126

// Absolute SNR thresholds (dB) for the quality bands: above -3 is GOOD, above -7 is FAIR (sufficient), above -12 is
// BAD (weak); anything at or below that is NONE.
private const val SNR_GOOD_THRESHOLD = -3f
private const val SNR_FAIR_THRESHOLD = -7f
private const val SNR_BAD_THRESHOLD = -12f

@Stable
enum class Quality(
    @Stable val nameRes: StringResource,
    @Stable val icon: DrawableResource,
    @Stable val color: @Composable () -> Color,
) {
    // Colours match the connection-status palette: gold (connected / key OK), red (disconnected), purple, white.
    NONE(Res.string.none_quality, Res.drawable.ic_signal_cellular_alt_1_bar, { colorScheme.StatusConnecting }),
    BAD(Res.string.bad, Res.drawable.ic_signal_cellular_alt_2_bar, { colorScheme.StatusPurple }),
    FAIR(Res.string.fair, Res.drawable.ic_signal_cellular_alt, { colorScheme.StatusDisconnected }),
    GOOD(Res.string.good, Res.drawable.ic_signal_cellular_4_bar, { colorScheme.StatusOnline }),
}

private const val SIZE_ICON_DP = 16

/**
 * Displays a human readable description and icon representing the signal quality.
 *
 * A null [snr] means the packet carried no measurement, which is rendered as "Unknown" in a neutral tint. It must not
 * fall through to [Quality.NONE] — that band means "measured, and too weak to demodulate", a different claim.
 */
@Composable
fun LoraSignalIndicator(
    snr: Float?,
    modifier: Modifier = Modifier,
    modemPreset: ModemPreset? = LocalModemPreset.current,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val quality = snr?.let { determineSignalQuality(it, modemPreset) }
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize().padding(8.dp),
    ) {
        Icon(
            modifier = Modifier.size(SIZE_ICON_DP.dp),
            imageVector = vectorResource(quality?.icon ?: Res.drawable.ic_signal_cellular_alt),
            contentDescription = stringResource(Res.string.signal_quality),
            tint = quality?.color?.invoke() ?: MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${stringResource(Res.string.signal)} " + stringResource(quality?.nameRes ?: Res.string.unknown),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

/** Renders nothing when [snr] is absent — 0 dB is a real reading, so it must not stand in for "no reading". */
@Composable
fun Snr(snr: Float?, modifier: Modifier = Modifier, modemPreset: ModemPreset? = LocalModemPreset.current) {
    if (snr == null) return
    val color: Color = determineSignalQuality(snr, modemPreset).color.invoke()

    Text(
        modifier = modifier,
        text = "${stringResource(Res.string.snr)} ${MetricFormatter.snr(snr, decimalPlaces = 2)}",
        color = color,
        style = MaterialTheme.typography.labelSmall,
    )
}

/**
 * [displayOffset] (LNA gain, dB) is subtracted from the shown text only; the colour is still rated from the raw [rssi].
 *
 * Renders nothing when [rssi] is absent — 0 dBm is a real reading, so it must not stand in for "no reading".
 */
@Composable
fun Rssi(
    rssi: Int?,
    modifier: Modifier = Modifier,
    label: String = stringResource(Res.string.rssi),
    displayOffset: Int = 0,
) {
    if (rssi == null) return
    val color: Color =
        when {
            rssi > RSSI_GOOD_THRESHOLD -> Quality.GOOD.color.invoke()
            rssi > RSSI_FAIR_THRESHOLD -> Quality.FAIR.color.invoke()
            rssi > RSSI_BAD_THRESHOLD -> Quality.BAD.color.invoke()
            else -> Quality.NONE.color.invoke()
        }
    Text(
        modifier = modifier,
        text = "$label ${MetricFormatter.rssi(rssi - displayOffset)}",
        color = color,
        style = MaterialTheme.typography.labelSmall,
    )
}

/**
 * Rates link quality from SNR alone, against fixed thresholds: above -3 dB is GOOD, above -7 dB is FAIR (sufficient),
 * above -12 dB is BAD (weak), and anything lower is NONE. RSSI does not affect the rating.
 *
 * [modemPreset], [rssi] and [noiseFloor] are kept so existing call sites stay unchanged; they are no longer used.
 */
@Suppress("UNUSED_PARAMETER")
fun determineSignalQuality(snr: Float, modemPreset: ModemPreset?, rssi: Int? = null, noiseFloor: Int? = null): Quality =
    when {
        snr > SNR_GOOD_THRESHOLD -> Quality.GOOD
        snr > SNR_FAIR_THRESHOLD -> Quality.FAIR
        snr > SNR_BAD_THRESHOLD -> Quality.BAD
        else -> Quality.NONE
    }
