/*
 * Copyright (c) 2025 MT_SW contributors
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
@file:OptIn(ExperimentalLayoutApi::class)

package org.meshtastic.feature.map.planner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.repository.FileService
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.planner_export_csv_profile
import org.meshtastic.core.resources.planner_export_csv_summary
import org.meshtastic.core.resources.planner_export_failed
import org.meshtastic.core.resources.planner_export_geojson
import org.meshtastic.core.resources.planner_export_kml
import org.meshtastic.core.resources.planner_export_nothing
import org.meshtastic.core.resources.planner_export_pdf
import org.meshtastic.core.resources.planner_export_png
import org.meshtastic.core.resources.planner_export_saved
import org.meshtastic.core.resources.planner_export_unavailable
import org.meshtastic.core.resources.planner_section_export
import org.meshtastic.core.ui.util.rememberSaveFileLauncher
import org.meshtastic.feature.map.planner.state.PlannerReportStrings
import org.meshtastic.feature.map.planner.state.PlannerUiState
import org.meshtastic.feature.map.planner.state.PlannerViewModel

private class PendingExport(val fileName: String, val mime: String, val bytes: ByteArray)

private enum class ExportOutcome {
    SAVED,
    FAILED,
    NOTHING,
}

private const val MIME_PDF = "application/pdf"
private const val MIME_CSV = "text/csv"
private const val MIME_KML = "application/vnd.google-earth.kml+xml"
private const val MIME_GEOJSON = "application/geo+json"
private const val MIME_PNG = "image/png"

/** UTF-8 byte order mark, so that spreadsheet programs show the Polish letters of CSV files correctly. */
private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

/** File exports (PDF, CSV, KML, GeoJSON, PNG) and "show on map". */
@Composable
internal fun ExportSection(
    state: PlannerUiState,
    vm: PlannerViewModel,
    bridge: PlannerMapBridge,
    onShownOnMap: () -> Unit,
) {
    val strings = rememberPlannerReportStrings()
    val prefix = plannerGeneratedPrefix()
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<PendingExport?>(null) }
    var outcome by remember { mutableStateOf<ExportOutcome?>(null) }

    val launcher =
        rememberSaveFileLauncher { uri ->
            val p = pending
            if (p != null) {
                scope.launch {
                    outcome = if (writeBytes(uri, p.bytes)) ExportOutcome.SAVED else ExportOutcome.FAILED
                    pending = null
                }
            }
        }

    fun export(extension: String, mime: String, produce: suspend (PlannerReportStrings) -> ByteArray?) {
        scope.launch {
            outcome = null
            val fresh = strings.copy(generatedAt = plannerGeneratedAt(prefix))
            val bytes =
                try {
                    produce(fresh)
                } catch (e: CancellationException) {
                    throw e
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    null
                }
            if (bytes == null) {
                outcome = ExportOutcome.NOTHING
                return@launch
            }
            val name = "mt_sw_planner_" + PlannerClock.fileStamp(nowMillis) + extension
            pending = PendingExport(name, mime, bytes)
            launcher(name, mime)
        }
    }

    val hasLink = state.isLinkReady && state.link != null
    val hasAnything = hasLink || state.coverage != null
    val hasProfile = hasLink && state.series != null

    PlannerCard(title = stringResource(Res.string.planner_section_export), info = PlannerInfoTopic.EXPORT) {
        if (hasAnything) ShowOnMapButton(vm = vm, bridge = bridge, onShownOnMap = onShownOnMap)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { export(".pdf", MIME_PDF) { vm.exportPdf(it) } }, enabled = hasAnything) {
                Text(stringResource(Res.string.planner_export_pdf))
            }
            OutlinedButton(
                onClick = { export("_summary.csv", MIME_CSV) { withBom(vm.exportCsvSummary(it)) } },
                enabled = hasAnything,
            ) {
                Text(stringResource(Res.string.planner_export_csv_summary))
            }
            OutlinedButton(
                onClick = { export("_profile.csv", MIME_CSV) { withBom(vm.exportCsvProfile(it)) } },
                enabled = hasProfile,
            ) {
                Text(stringResource(Res.string.planner_export_csv_profile))
            }
            OutlinedButton(
                onClick = { export(".kml", MIME_KML) { vm.exportKml(it).encodeToByteArray() } },
                enabled = hasAnything,
            ) {
                Text(stringResource(Res.string.planner_export_kml))
            }
            OutlinedButton(
                onClick = { export(".geojson", MIME_GEOJSON) { vm.exportGeoJson(it).encodeToByteArray() } },
                enabled = hasAnything,
            ) {
                Text(stringResource(Res.string.planner_export_geojson))
            }
            OutlinedButton(onClick = { export("_profile.png", MIME_PNG) { vm.exportPng(it) } }, enabled = hasProfile) {
                Text(stringResource(Res.string.planner_export_png))
            }
        }
        when (outcome) {
            ExportOutcome.SAVED -> Text(stringResource(Res.string.planner_export_saved), style = MaterialTheme.typography.bodySmall)
            ExportOutcome.FAILED ->
                Text(
                    stringResource(Res.string.planner_export_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            ExportOutcome.NOTHING ->
                Text(
                    stringResource(Res.string.planner_export_nothing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            null -> if (!hasAnything) {
                Text(
                    stringResource(Res.string.planner_export_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun withBom(text: String): ByteArray = UTF8_BOM + text.encodeToByteArray()

private suspend fun writeBytes(uri: CommonUri, bytes: ByteArray): Boolean = try {
    val fileService = KoinPlatform.getKoin().get<FileService>()
    withContext(Dispatchers.Default) { fileService.write(uri) { sink -> sink.write(bytes) } }
} catch (e: CancellationException) {
    throw e
} catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
    false
}
