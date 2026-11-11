/*
 * Copyright (c) 2025-2026 Meshtastic LLC
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
@file:Suppress("MagicNumber", "TooGenericExceptionCaught", "SwallowedException")

package org.meshtastic.feature.map.planner.state

import okio.FileSystem
import okio.Path
import org.meshtastic.feature.map.layers.mapLayerFileSystem
import org.meshtastic.feature.map.layers.mapLayersDirectory

/** Keeps the remembered link results ([PlanSnapshot]) in a small text file next to the other map data. */
internal class PlanSnapshotStore {
    private val fileSystem: FileSystem = mapLayerFileSystem()
    private val file: Path = (mapLayersDirectory().parent ?: mapLayersDirectory()) / "planner" / "snapshots.txt"

    fun read(): List<PlanSnapshot> = try {
        if (!fileSystem.exists(file)) {
            emptyList()
        } else {
            fileSystem
                .read(file) { readUtf8() }
                .lines()
                .mapNotNull { line ->
                    val p = line.split('\t')
                    if (p.size != FIELDS) return@mapNotNull null
                    PlanSnapshot(
                        label = p[0],
                        distanceM = p[1].toDouble(),
                        marginAToBDb = p[2].toDouble(),
                        marginBToADb = p[3].toDouble(),
                        spreadingFactor = p[4].toInt(),
                        frequencyMHz = p[5].toDouble(),
                        heightAM = p[6].toDouble(),
                        heightBM = p[7].toDouble(),
                    )
                }
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun write(list: List<PlanSnapshot>) {
        try {
            file.parent?.let { fileSystem.createDirectories(it) }
            val text =
                list.joinToString("\n") {
                    listOf(
                        it.label,
                        it.distanceM,
                        it.marginAToBDb,
                        it.marginBToADb,
                        it.spreadingFactor,
                        it.frequencyMHz,
                        it.heightAM,
                        it.heightBM,
                    )
                        .joinToString("\t")
                }
            fileSystem.write(file) { writeUtf8(text) }
        } catch (e: Exception) {
            // Not being able to keep the list only means it is gone after the app is closed.
        }
    }

    private companion object {
        const val FIELDS = 8
    }
}
