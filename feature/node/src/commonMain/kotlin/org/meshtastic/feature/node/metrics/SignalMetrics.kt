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
package org.meshtastic.feature.node.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.VicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.model.TelemetryType
import org.meshtastic.core.model.noiseFloorOrNull
import org.meshtastic.core.model.util.TimeConstants.MS_PER_SEC
import org.meshtastic.core.model.util.rxTimeOrNull
import org.meshtastic.core.model.util.snrOrNull
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.busy_noise_floor
import org.meshtastic.core.resources.clear
import org.meshtastic.core.resources.formatDuration
import org.meshtastic.core.resources.lna_gain_correction_description
import org.meshtastic.core.resources.lna_gain_db
import org.meshtastic.core.resources.lna_gain_node_description
import org.meshtastic.core.resources.local_stats_bad
import org.meshtastic.core.resources.local_stats_nodes
import org.meshtastic.core.resources.local_stats_noise
import org.meshtastic.core.resources.local_stats_relays
import org.meshtastic.core.resources.local_stats_rx_share
import org.meshtastic.core.resources.local_stats_traffic
import org.meshtastic.core.resources.local_stats_uptime
import org.meshtastic.core.resources.no_local_stats
import org.meshtastic.core.resources.noise_floor
import org.meshtastic.core.resources.noise_floor_definition
import org.meshtastic.core.resources.noise_floor_no_reading
import org.meshtastic.core.resources.request
import org.meshtastic.core.resources.rssi
import org.meshtastic.core.resources.rssi_definition
import org.meshtastic.core.resources.save
import org.meshtastic.core.resources.signal_quality
import org.meshtastic.core.resources.snr
import org.meshtastic.core.resources.snr_definition
import org.meshtastic.core.resources.wifi_disconnected_now
import org.meshtastic.core.resources.wifi_drop_reason_label
import org.meshtastic.core.resources.wifi_drops_label
import org.meshtastic.core.resources.wifi_export
import org.meshtastic.core.resources.wifi_rssi_definition
import org.meshtastic.core.resources.wifi_rssi_signal
import org.meshtastic.core.ui.component.LoraSignalIndicator
import org.meshtastic.core.ui.component.SignedIntegerEditTextPreference
import org.meshtastic.core.ui.component.determineRssiQuality
import org.meshtastic.core.ui.component.determineSignalQuality
import org.meshtastic.core.ui.icon.Delete
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Refresh
import org.meshtastic.core.ui.icon.Save
import org.meshtastic.core.ui.theme.GraphColors.Blue
import org.meshtastic.core.ui.theme.GraphColors.Gold
import org.meshtastic.core.ui.theme.GraphColors.Green
import org.meshtastic.core.ui.theme.GraphColors.Orange
import org.meshtastic.core.ui.theme.GraphColors.Red
import org.meshtastic.core.ui.theme.StatusColors.StatusConnecting
import org.meshtastic.core.ui.theme.StatusColors.StatusDisconnected
import org.meshtastic.core.ui.theme.StatusColors.StatusOnline
import org.meshtastic.core.ui.theme.StatusColors.StatusPurple
import org.meshtastic.core.ui.util.LocalLnaCorrection
import org.meshtastic.core.ui.util.LocalModemPreset
import org.meshtastic.core.ui.util.rememberSaveFileLauncher
import org.meshtastic.proto.MeshPacket
import org.meshtastic.proto.Telemetry

private const val QUIET_NOISE_FLOOR_DBM = -100
private const val BUSY_FLOOR_DBM = -90
private const val NOISY_FLOOR_DBM = -80
private const val MIN_DBM_AXIS = -120.0
private const val MAX_DBM_AXIS = 0.0

private enum class SignalMetric(val color: Color) {
    NOISE_FLOOR(Gold),
    BUSY_FLOOR(Red),
    SNR(Green),
    RSSI(Blue),
    WIFI_RSSI(Orange),
}

private val LEGEND_DATA =
    listOf(
        LegendData(nameRes = Res.string.noise_floor, color = SignalMetric.NOISE_FLOOR.color, isLine = true),
        LegendData(nameRes = Res.string.rssi, color = SignalMetric.RSSI.color),
        LegendData(nameRes = Res.string.wifi_rssi_signal, color = SignalMetric.WIFI_RSSI.color),
        LegendData(nameRes = Res.string.snr, color = SignalMetric.SNR.color),
    )

/** Builds the combined signal log, newest first. Kept out of the composable so the LazyColumn keys are testable. */
internal fun buildSignalLog(
    signalData: List<MeshPacket>,
    localStatsData: List<Telemetry>,
    wifiData: List<WifiSample> = emptyList(),
): List<SignalLogEntry> {
    val localStats = localStatsData.mapIndexed { index, telemetry -> SignalLogEntry.LocalStatsEntry(telemetry, index) }
    val packets = signalData.mapIndexed { index, packet -> SignalLogEntry.PacketEntry(packet, index) }
    val wifi = wifiData.mapIndexed { index, sample -> SignalLogEntry.WifiEntry(sample, index) }
    return (localStats + packets + wifi).sortedByDescending { it.timeSeconds }
}

internal sealed interface SignalLogEntry {
    val timeSeconds: Int

    /** Stable, collision-free identity for use as a LazyColumn item key across both entry types. */
    val key: Any

    /** Distinguishes the two card layouts so Compose can reuse compositions per type. */
    val contentType: Any

    data class LocalStatsEntry(val telemetry: Telemetry, val index: Int) : SignalLogEntry {
        override val timeSeconds: Int = telemetry.time

        // Local stats telemetry is an id-less proto; the source-list index disambiguates same-second samples.
        override val key: Any = "local_stats_${telemetry.time}_$index"
        override val contentType: Any = "local_stats"
    }

    data class WifiEntry(val sample: WifiSample, val index: Int) : SignalLogEntry {
        override val timeSeconds: Int = sample.time

        // Same-second samples are told apart by their position in the source list.
        override val key: Any = "wifi_${sample.time}_$index"
        override val contentType: Any = "wifi"
    }

    data class PacketEntry(val meshPacket: MeshPacket, val index: Int) : SignalLogEntry {
        override val timeSeconds: Int = meshPacket.rxTimeOrNull() ?: 0

        // MeshPacket.id repeats: it is unique only per originating node, and retransmissions are stored per reception.
        // The source-list index disambiguates, as it does for local stats.
        override val key: Any = "packet_${meshPacket.id}_$index"
        override val contentType: Any = "signal_packet"
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun SignalMetricsScreen(viewModel: MetricsViewModel, onNavigateUp: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val timeFrame by viewModel.timeFrame.collectAsStateWithLifecycle()
    val availableTimeFrames by viewModel.availableTimeFrames.collectAsStateWithLifecycle()
    val threshold = timeFrame.timeThreshold()
    // An unstamped packet has no place on a time axis. Dropping it here is the invariant every downstream
    // `rxTimeOrNull() ?: 0` in this file relies on, so none of them can render or plot 1970.
    val signalData =
        state.signalMetrics.filter { packet ->
            val rxTime = packet.rxTimeOrNull() ?: return@filter false
            rxTime.toLong() >= threshold
        }
    val localStatsData = state.localStats.filter { it.time.toLong() >= threshold && it.local_stats != null }
    // The device's own WiFi link (fw+ extended stats) arrives in the same telemetry as the host statistics.
    val wifiData = remember(state.hostMetrics, threshold) { state.hostMetrics.toWifiSamples(threshold) }
    val data = remember(signalData, localStatsData, wifiData) { buildSignalLog(signalData, localStatsData, wifiData) }
    val hasWifiRssi = remember(wifiData) { wifiData.any { it.isConnected } }
    val hasNoiseFloor = remember(localStatsData) { localStatsData.any { it.local_stats?.noiseFloorOrNull != null } }
    val hasRssi = remember(signalData) { signalData.any { it.rx_rssi != null } }
    val hasSnr = remember(signalData) { signalData.any { it.snrOrNull() != null } }
    val hasAnyLocalStats = state.localStats.isNotEmpty()
    val localStatsExportLauncher = rememberSaveFileLauncher { uri -> viewModel.saveLocalStatsCSV(uri, localStatsData) }
    val wifiExportLauncher = rememberSaveFileLauncher { uri -> viewModel.saveWifiSignalCSV(uri, wifiData) }
    val signalExportLauncher = rememberSaveFileLauncher { uri -> viewModel.saveSignalMetricsCSV(uri, signalData) }

    // The LNA gain and current-polarity fields sit under the info text, the same for every node (ours too; our own
    // values can also be set in the LoRa settings).
    val myNodeNum = LocalLnaCorrection.current.myNodeNum
    val lnaNodeNum = state.node?.num
    val isLocalNode = lnaNodeNum != null && lnaNodeNum == myNodeNum

    BaseMetricScreen(
        onNavigateUp = onNavigateUp,
        telemetryType = null,
        titleRes = Res.string.signal_quality,
        nodeName = state.node?.user?.long_name ?: "",
        data = data,
        timeProvider = { it.timeSeconds.toDouble() },
        modifier = modifier,
        onExportCsv =
        if (signalData.isNotEmpty()) {
            { signalExportLauncher("signal_metrics.csv", "text/csv") }
        } else {
            null
        },
        infoData =
        buildList {
            if (hasNoiseFloor) {
                add(
                    InfoDialogData(
                        Res.string.noise_floor,
                        Res.string.noise_floor_definition,
                        SignalMetric.NOISE_FLOOR.color,
                    ),
                )
            }
            if (hasSnr) add(InfoDialogData(Res.string.snr, Res.string.snr_definition, SignalMetric.SNR.color))
            if (hasRssi) add(InfoDialogData(Res.string.rssi, Res.string.rssi_definition, SignalMetric.RSSI.color))
            if (hasWifiRssi) {
                add(
                    InfoDialogData(
                        Res.string.wifi_rssi_signal,
                        Res.string.wifi_rssi_definition,
                        SignalMetric.WIFI_RSSI.color,
                    ),
                )
            }
        },
        infoExtraContent =
        lnaNodeNum?.let { num -> @Composable { LnaGainField(num, isLocalNode) } },
        controlPart = {
            TimeFrameSelector(
                selectedTimeFrame = timeFrame,
                availableTimeFrames = availableTimeFrames,
                onTimeFrameSelected = viewModel::setTimeFrame,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        },
        chartPart = { contentModifier, selectedX, vicoScrollState, onPointSelected ->
            SignalMetricsChart(
                modifier = contentModifier,
                nodeNum = state.node?.num,
                localStats = localStatsData.reversed(),
                meshPackets = signalData.reversed(),
                wifi = wifiData.reversed(),
                vicoScrollState = vicoScrollState,
                selectedX = selectedX,
                onPointSelected = onPointSelected,
            )
        },
        listPart = { contentModifier, selectedX, lazyListState, onCardClick ->
            if (data.isEmpty()) {
                Box(modifier = contentModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.no_local_stats),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = contentModifier.fillMaxSize(), state = lazyListState) {
                    itemsIndexed(
                        data,
                        key = { _, entry -> entry.key },
                        contentType = { _, entry -> entry.contentType },
                    ) { _, entry ->
                        when (entry) {
                            is SignalLogEntry.LocalStatsEntry ->
                                LocalStatsCard(
                                    nodeNum = state.node?.num,
                                    telemetry = entry.telemetry,
                                    isSelected = entry.timeSeconds.toDouble() == selectedX,
                                    onClick = { onCardClick(entry.timeSeconds.toDouble()) },
                                )

                            is SignalLogEntry.WifiEntry ->
                                WifiCard(
                                    sample = entry.sample,
                                    isSelected = entry.timeSeconds.toDouble() == selectedX,
                                    onClick = { onCardClick(entry.timeSeconds.toDouble()) },
                                )

                            is SignalLogEntry.PacketEntry ->
                                SignalMetricsCard(
                                    meshPacket = entry.meshPacket,
                                    isSelected = entry.timeSeconds.toDouble() == selectedX,
                                    onClick = { onCardClick(entry.timeSeconds.toDouble()) },
                                )
                        }
                    }
                }
            }
        },
        bottomContent = {
            LocalStatsActionButtons(
                hasLocalStats = hasAnyLocalStats,
                hasVisibleLocalStats = localStatsData.isNotEmpty(),
                onClear = viewModel::clearLocalStats,
                onRequest = { viewModel.requestTelemetry(TelemetryType.LOCAL_STATS) },
                onSave = { localStatsExportLauncher("local_stats.csv", "text/csv") },
                hasWifi = wifiData.isNotEmpty(),
                onSaveWifi = { wifiExportLauncher("wifi_signal.csv", "text/csv") },
            )
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocalStatsActionButtons(
    hasLocalStats: Boolean,
    hasVisibleLocalStats: Boolean,
    onClear: () -> Unit,
    onRequest: () -> Unit,
    onSave: () -> Unit,
    hasWifi: Boolean,
    onSaveWifi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (hasLocalStats) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onClear,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(imageVector = MeshtasticIcons.Delete, contentDescription = stringResource(Res.string.clear))
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(Res.string.clear), maxLines = 1)
            }
        }
        OutlinedButton(modifier = Modifier.weight(1f), onClick = onRequest) {
            Icon(imageVector = MeshtasticIcons.Refresh, contentDescription = stringResource(Res.string.request))
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(Res.string.request), maxLines = 1)
        }
        if (hasVisibleLocalStats) {
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onSave) {
                Icon(imageVector = MeshtasticIcons.Save, contentDescription = stringResource(Res.string.save))
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(Res.string.save), maxLines = 1)
            }
        }
        if (hasWifi) {
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onSaveWifi) {
                Icon(imageVector = MeshtasticIcons.Save, contentDescription = stringResource(Res.string.wifi_export))
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(Res.string.wifi_export), maxLines = 1)
            }
        }
    }
}

/** Per-node LNA gain (dB); display-only, stored on this phone (see LnaCorrection). */
@Composable
internal fun LnaGainField(nodeNum: Int, isLocal: Boolean = false) {
    val uiPrefs = koinInject<UiPrefs>()
    val gains by uiPrefs.lnaGains.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    SignedIntegerEditTextPreference(
        title = stringResource(Res.string.lna_gain_db),
        summary =
        stringResource(
            if (isLocal) Res.string.lna_gain_correction_description else Res.string.lna_gain_node_description,
        ),
        value = gains[nodeNum] ?: 0,
        enabled = true,
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        onValueChanged = { uiPrefs.setLnaGain(nodeNum, it.coerceIn(MIN_LNA_GAIN_DB, MAX_LNA_GAIN_DB)) },
    )
}

private const val MIN_LNA_GAIN_DB = -30
private const val MAX_LNA_GAIN_DB = 60

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun SignalMetricsChart(
    modifier: Modifier = Modifier,
    nodeNum: Int?,
    localStats: List<Telemetry>,
    meshPackets: List<MeshPacket>,
    wifi: List<WifiSample>,
    vicoScrollState: VicoScrollState,
    selectedX: Double?,
    onPointSelected: (Double) -> Unit,
) {
    // LNA gain correction: display-only. The viewed node's own gain shifts its noise floor; our gain shifts the RSSI of
    // packets we received.
    val lna = LocalLnaCorrection.current
    val noiseGain = nodeNum?.let { lna.gains[it] } ?: 0
    val rssiGain = lna.localGain
    val noiseFloorData = remember(localStats) { localStats.filter { it.local_stats?.noiseFloorOrNull != null } }
    val busyFloorData =
        remember(noiseFloorData) {
            if (noiseFloorData.size > 1) listOf(noiseFloorData.first(), noiseFloorData.last()) else emptyList()
        }
    val rssiData = remember(meshPackets) { meshPackets.filter { it.rx_rssi != null } }
    val snrData = remember(meshPackets) { meshPackets.filter { it.snrOrNull() != null } }
    val wifiRssiData = remember(wifi) { wifi.filter { it.isConnected } }
    val legendData =
        remember(noiseFloorData, rssiData, snrData, wifiRssiData) {
            LEGEND_DATA.filter { legend ->
                when (legend.nameRes) {
                    Res.string.noise_floor -> noiseFloorData.isNotEmpty()
                    Res.string.rssi -> rssiData.isNotEmpty()
                    Res.string.snr -> snrData.isNotEmpty()
                    Res.string.wifi_rssi_signal -> wifiRssiData.isNotEmpty()
                    else -> true
                }
            }
        }

    MetricChartScaffold(
        isEmpty = meshPackets.isEmpty() && localStats.isEmpty() && wifiRssiData.isEmpty(),
        legendData = legendData,
        modifier = modifier,
    ) { modelProducer, chartModifier ->
        val noiseFloorColor = SignalMetric.NOISE_FLOOR.color
        val busyFloorColor = SignalMetric.BUSY_FLOOR.color
        val rssiColor = SignalMetric.RSSI.color
        val snrColor = SignalMetric.SNR.color
        val wifiColor = SignalMetric.WIFI_RSSI.color
        val noiseFloorLabel = stringResource(Res.string.noise_floor)
        val busyFloorLabel = stringResource(Res.string.busy_noise_floor)
        val rssiLabel = stringResource(Res.string.rssi)
        val snrLabel = stringResource(Res.string.snr)
        val wifiLabel = stringResource(Res.string.wifi_rssi_signal)

        LaunchedEffect(noiseFloorData, busyFloorData, rssiData, wifiRssiData, snrData, noiseGain, rssiGain) {
            modelProducer.runTransaction {
                if (noiseFloorData.isNotEmpty()) {
                    lineModel {
                        series(
                            x = noiseFloorData.map { it.time },
                            y = noiseFloorData.map { (it.local_stats?.noise_floor ?: 0) - noiseGain },
                        )
                    }
                }
                if (busyFloorData.isNotEmpty()) {
                    lineModel { series(x = busyFloorData.map { it.time }, y = busyFloorData.map { BUSY_FLOOR_DBM - noiseGain }) }
                }
                if (rssiData.isNotEmpty()) {
                    lineModel {
                        series(x = rssiData.map { it.rxTimeOrNull() ?: 0 }, y = rssiData.mapNotNull { it.rx_rssi?.minus(rssiGain) })
                    }
                }
                if (wifiRssiData.isNotEmpty()) {
                    // The WiFi link is the device's own, so the LNA correction of the LoRa radio does not apply.
                    lineModel { series(x = wifiRssiData.map { it.time }, y = wifiRssiData.map { it.stats.wifiRssi }) }
                }
                if (snrData.isNotEmpty()) {
                    /* Use a separate lineModel call to associate SNR with the right axis. */
                    lineModel {
                        series(x = snrData.map { it.rxTimeOrNull() ?: 0 }, y = snrData.mapNotNull { it.snrOrNull() })
                    }
                }
            }
        }

        val marker =
            ChartStyling.rememberMarker(
                valueFormatter =
                ChartStyling.createColoredMarkerValueFormatter { value, color ->
                    when (color.copy(alpha = 1f)) {
                        noiseFloorColor -> "$noiseFloorLabel: ${MetricFormatter.rssi(value.toInt())}"
                        busyFloorColor -> "$busyFloorLabel: ${MetricFormatter.rssi(value.toInt())}"
                        rssiColor -> "$rssiLabel: ${MetricFormatter.rssi(value.toInt())}"
                        wifiColor -> "$wifiLabel: ${MetricFormatter.rssi(value.toInt())}"
                        snrColor -> "$snrLabel: ${MetricFormatter.snr(value.toFloat())}"
                        else -> value.toString()
                    }
                },
            )

        val axisMin = MIN_DBM_AXIS - maxOf(0, noiseGain, rssiGain)
        val dbmRangeProvider = remember(axisMin) { CartesianLayerRangeProvider.fixed(minY = axisMin, maxY = MAX_DBM_AXIS) }
        val noiseFloorLayer =
            rememberConditionalLayer(
                hasData = noiseFloorData.isNotEmpty(),
                lineProvider = LineCartesianLayer.LineProvider.series(ChartStyling.createBoldLine(noiseFloorColor)),
                verticalAxisPosition = Axis.Position.Vertical.Start,
                rangeProvider = dbmRangeProvider,
            )
        val busyFloorLayer =
            rememberConditionalLayer(
                hasData = busyFloorData.isNotEmpty(),
                lineProvider = LineCartesianLayer.LineProvider.series(ChartStyling.createDashedLine(busyFloorColor)),
                verticalAxisPosition = Axis.Position.Vertical.Start,
                rangeProvider = dbmRangeProvider,
            )
        val rssiLayer =
            rememberConditionalLayer(
                hasData = rssiData.isNotEmpty(),
                lineProvider = LineCartesianLayer.LineProvider.series(ChartStyling.createDashedLine(rssiColor)),
                verticalAxisPosition = Axis.Position.Vertical.Start,
                rangeProvider = dbmRangeProvider,
            )
        val wifiLayer =
            rememberConditionalLayer(
                hasData = wifiRssiData.isNotEmpty(),
                lineProvider = LineCartesianLayer.LineProvider.series(ChartStyling.createDashedLine(wifiColor)),
                verticalAxisPosition = Axis.Position.Vertical.Start,
                rangeProvider = dbmRangeProvider,
            )
        val snrLayer =
            rememberConditionalLayer(
                hasData = snrData.isNotEmpty(),
                lineProvider = LineCartesianLayer.LineProvider.series(ChartStyling.createDashedLine(snrColor)),
                verticalAxisPosition = Axis.Position.Vertical.End,
            )

        val layers =
            remember(noiseFloorLayer, busyFloorLayer, rssiLayer, wifiLayer, snrLayer) {
                listOfNotNull(noiseFloorLayer, busyFloorLayer, rssiLayer, wifiLayer, snrLayer)
            }

        if (layers.isNotEmpty()) {
            GenericMetricChart(
                modelProducer = modelProducer,
                modifier = chartModifier,
                layers = layers,
                startAxis =
                if (noiseFloorData.isNotEmpty() || rssiData.isNotEmpty() || wifiRssiData.isNotEmpty()) {
                    VerticalAxis.rememberStart(
                        label =
                        ChartStyling.rememberAxisLabel(
                            color =
                            when {
                                noiseFloorData.isNotEmpty() -> noiseFloorColor
                                rssiData.isNotEmpty() -> rssiColor
                                else -> wifiColor
                            },
                        ),
                        valueFormatter = { _, value, _ -> MetricFormatter.rssi(value.toInt()) },
                    )
                } else {
                    null
                },
                endAxis =
                if (snrData.isNotEmpty()) {
                    VerticalAxis.rememberEnd(
                        label = ChartStyling.rememberAxisLabel(color = snrColor),
                        valueFormatter = { _, value, _ -> MetricFormatter.snr(value.toFloat()) },
                    )
                } else {
                    null
                },
                bottomAxis = CommonCharts.rememberBottomTimeAxis(),
                marker = marker,
                selectedX = selectedX,
                onPointSelected = onPointSelected,
                vicoScrollState = vicoScrollState,
            )
        }
    }
}

@Composable
private fun noiseFloorTextColor(value: Int?): Color = when {
    value == null -> MaterialTheme.colorScheme.onSurfaceVariant
    // Palette matches the signal-quality / connection-status colours: gold = quiet, red = busy, purple = very noisy,
    // white = worst.
    value < QUIET_NOISE_FLOOR_DBM -> MaterialTheme.colorScheme.StatusOnline
    value < BUSY_FLOOR_DBM -> MaterialTheme.colorScheme.StatusDisconnected
    value < NOISY_FLOOR_DBM -> MaterialTheme.colorScheme.StatusPurple
    else -> MaterialTheme.colorScheme.StatusConnecting
}

@Suppress("LongMethod")
@Composable
private fun LocalStatsCard(nodeNum: Int?, telemetry: Telemetry, isSelected: Boolean, onClick: () -> Unit) {
    val localStats = telemetry.local_stats
    val time = telemetry.time.toLong() * MS_PER_SEC
    // Shown (and coloured) after the LNA gain correction of the node that reported it.
    val noiseFloor = localStats?.noiseFloorOrNull?.let { LocalLnaCorrection.current.noiseFloor(nodeNum, it) }

    SelectableMetricCard(isSelected = isSelected, onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = DateFormatter.formatDateTime(time),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text =
                    if (noiseFloor != null) {
                        stringResource(Res.string.local_stats_noise, noiseFloor)
                    } else {
                        stringResource(Res.string.noise_floor_no_reading)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = noiseFloorTextColor(noiseFloor),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // FlowRow(SpaceBetween): the relays stat wraps to its own line when the traffic label is too long,
            // instead of being crushed and wrapped one character per line.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                    stringResource(
                        Res.string.local_stats_traffic,
                        localStats?.num_packets_tx ?: 0,
                        localStats?.num_packets_rx ?: 0,
                        localStats?.num_rx_dupe ?: 0,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text =
                    stringResource(
                        Res.string.local_stats_relays,
                        localStats?.num_tx_relay ?: 0,
                        localStats?.num_tx_relay_canceled ?: 0,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            val rx = localStats?.num_packets_rx ?: 0
            if (rx > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text =
                    stringResource(
                        Res.string.local_stats_rx_share,
                        rxShare(localStats?.num_rx_dupe ?: 0, rx),
                        rxShare(localStats?.num_packets_rx_bad ?: 0, rx),
                        rxShare(localStats?.num_tx_relay ?: 0, rx),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // FlowRow(SpaceBetween): the uptime stat wraps to its own line when the nodes label is too long,
            // instead of being crushed and wrapped one character per line.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                    stringResource(
                        Res.string.local_stats_nodes,
                        localStats?.num_online_nodes ?: 0,
                        localStats?.num_total_nodes ?: 0,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text =
                    stringResource(
                        Res.string.local_stats_uptime,
                        formatDuration((localStats?.uptime_seconds ?: 0).toLong()),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            if ((localStats?.num_packets_rx_bad ?: 0) > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.local_stats_bad, localStats?.num_packets_rx_bad ?: 0),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** [count] as a percentage of all received packets [total], e.g. "2,5%". */
private fun rxShare(count: Int, total: Int): String = MetricFormatter.percent(count * 100f / total, decimalPlaces = 1)

/** One report of the device's own WiFi link: strength, how often it dropped and why the last drop happened. */
@Composable
private fun WifiCard(sample: WifiSample, isSelected: Boolean, onClick: () -> Unit) {
    val time = sample.time.toLong() * MS_PER_SEC
    SelectableMetricCard(isSelected = isSelected, onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = DateFormatter.formatDateTime(time),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text =
                    if (sample.isConnected) {
                        MetricFormatter.rssi(sample.stats.wifiRssi)
                    } else {
                        stringResource(Res.string.wifi_disconnected_now)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = wifiRssiTextColor(sample.stats.wifiRssi.takeIf { sample.isConnected }),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            MetricValueRow(
                color = SignalMetric.WIFI_RSSI.color,
                text = stringResource(Res.string.wifi_rssi_signal),
                textColor = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${stringResource(Res.string.wifi_drops_label)}: ${sample.stats.wifiDrops}",
                style = MaterialTheme.typography.labelLarge,
            )
            if (sample.stats.wifiDropReason != 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text =
                    "${stringResource(Res.string.wifi_drop_reason_label)}: " +
                        wifiDropReasonText(sample.stats.wifiDropReason),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// WiFi strength limits in dBm: above -70 the link is solid, below -85 it tends to drop.
private const val WIFI_GOOD_DBM = -70
private const val WIFI_WEAK_DBM = -85

@Composable
private fun wifiRssiTextColor(rssi: Int?): Color = when {
    rssi == null -> MaterialTheme.colorScheme.StatusDisconnected
    rssi >= WIFI_GOOD_DBM -> MaterialTheme.colorScheme.StatusOnline
    rssi >= WIFI_WEAK_DBM -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.StatusDisconnected
}

@Composable
private fun SignalMetricsCard(meshPacket: MeshPacket, isSelected: Boolean, onClick: () -> Unit) {
    val time = (meshPacket.rxTimeOrNull() ?: 0).toLong() * MS_PER_SEC
    SelectableMetricCard(isSelected = isSelected, onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            /* Data */
            Box(modifier = Modifier.weight(weight = 5f).height(IntrinsicSize.Min)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    /* Time */
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = DateFormatter.formatDateTime(time),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    /* SNR and RSSI */
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // The dot keeps the chart series colour; the value is coloured by its own quality band.
                        val shownRssi = meshPacket.rx_rssi?.let { LocalLnaCorrection.current.rssi(it) }
                        val cardSnr = meshPacket.snrOrNull()
                        MetricValueRow(
                            color = SignalMetric.RSSI.color,
                            text = MetricFormatter.rssi(shownRssi),
                            textColor =
                            shownRssi?.let { determineRssiQuality(it).color.invoke() }
                                ?: MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.width(12.dp))
                        MetricValueRow(
                            color = SignalMetric.SNR.color,
                            text = MetricFormatter.snr(cardSnr),
                            textColor =
                            cardSnr?.let { determineSignalQuality(it, LocalModemPreset.current).color.invoke() }
                                ?: MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            /* Signal Indicator */
            Box(modifier = Modifier.weight(weight = 3f).height(IntrinsicSize.Max)) {
                LoraSignalIndicator(snr = meshPacket.snrOrNull())
            }
        }
    }
}
