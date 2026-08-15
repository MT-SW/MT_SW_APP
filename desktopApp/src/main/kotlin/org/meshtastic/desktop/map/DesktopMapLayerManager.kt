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
package org.meshtastic.desktop.map

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.database.desktopDataDir
import org.meshtastic.core.di.CoroutineDispatchers
import java.io.File

private const val TAG = "DesktopMapLayerManager"
private const val LAYERS_DIR = "map_layers"

/**
 * Owner of the imported map-layer list for desktop: persistence (files under [desktopDataDir]/map_layers) and the
 * in-memory layer list. Visibility toggles are in-memory only for now (not persisted across restart) — the Android
 * fork persists this via `MapPrefs`, which has no desktop equivalent yet.
 */
@Single
class DesktopMapLayerManager(private val dispatchers: CoroutineDispatchers) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    private val _layers = MutableStateFlow<List<DesktopMapLayer>>(emptyList())
    val layers: StateFlow<List<DesktopMapLayer>> = _layers.asStateFlow()

    private val layersDir = File(desktopDataDir(), LAYERS_DIR)

    init {
        loadPersistedLayers()
    }

    private fun loadPersistedLayers() {
        scope.launch(dispatchers.io) {
            try {
                if (!layersDir.exists() || !layersDir.isDirectory) return@launch
                val loaded =
                    layersDir.listFiles().orEmpty().mapNotNull { file ->
                        if (!file.isFile) return@mapNotNull null
                        resolveDesktopLayerType(file.extension)?.let { type ->
                            DesktopMapLayer(
                                name = file.nameWithoutExtension,
                                filePath = file.absolutePath,
                                layerType = type,
                                createdAt = file.lastModified().takeIf { it > 0 },
                            )
                        }
                    }
                _layers.value = loaded
                if (loaded.isNotEmpty()) Logger.withTag(TAG).i { "Loaded ${loaded.size} persisted map layers" }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.withTag(TAG).e(e) { "Error loading persisted map layers" }
            }
        }
    }

    /** Copies [sourceFile] into the layers directory and adds it to the visible layer list. */
    fun addLayer(sourceFile: File) {
        scope.launch(dispatchers.io) {
            val type = resolveDesktopLayerType(sourceFile.extension)
            if (type == null) {
                Logger.withTag(TAG).e { "Unsupported map layer file type: ${sourceFile.extension}" }
                return@launch
            }
            try {
                if (!layersDir.exists()) layersDir.mkdirs()
                val destFile = File(layersDir, "${sourceFile.nameWithoutExtension}_${nowMillis}.${sourceFile.extension}")
                sourceFile.copyTo(destFile, overwrite = false)
                _layers.update {
                    it +
                        DesktopMapLayer(
                            name = sourceFile.nameWithoutExtension,
                            filePath = destFile.absolutePath,
                            layerType = type,
                            createdAt = nowMillis,
                        )
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.withTag(TAG).e(e) { "Failed to import map layer file" }
            }
        }
    }

    /** Writes a GeoJSON coverage estimate string directly to the layers directory and adds it, visible by default —
     * the Site Planner import path (no source file to copy, unlike [addLayer]). */
    fun addGeoJsonLayer(name: String, geoJson: String) {
        scope.launch(dispatchers.io) {
            try {
                if (!layersDir.exists()) layersDir.mkdirs()
                val destFile = File(layersDir, "${name}_$nowMillis.geojson")
                destFile.writeText(geoJson)
                _layers.update {
                    it +
                        DesktopMapLayer(
                            name = name,
                            filePath = destFile.absolutePath,
                            layerType = DesktopLayerType.GEOJSON,
                            createdAt = nowMillis,
                        )
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.withTag(TAG).e(e) { "Failed to write GeoJSON coverage layer" }
            }
        }
    }

    fun toggleVisibility(layerId: String) {
        _layers.update { layers -> layers.map { if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it } }
    }

    fun removeLayer(layerId: String) {
        scope.launch(dispatchers.io) {
            val target = _layers.value.find { it.id == layerId } ?: return@launch
            runCatching { File(target.filePath).delete() }
                .onFailure { Logger.withTag(TAG).e(it) { "Failed to delete layer file" } }
            withContext(dispatchers.default) { _layers.update { layers -> layers.filterNot { it.id == layerId } } }
        }
    }
}
