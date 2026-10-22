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
package org.meshtastic.core.repository

import kotlinx.coroutines.flow.StateFlow

/** State of the offline translator model files stored on this device. */
sealed interface TranslatorModelState {
    /** Nothing downloaded yet. */
    data object NotDownloaded : TranslatorModelState

    /** Download in progress; [progress] is 0..1. */
    data class Downloading(val progress: Float) : TranslatorModelState

    /** All files are on the device and the translator can run. */
    data object Ready : TranslatorModelState

    /** The last download failed (or was cancelled); [reason] is for logs, not for display. */
    data class Failed(val reason: String) : TranslatorModelState
}

/** Downloads, tracks and removes the offline translator model (one multilingual model for every language pair). */
interface TranslatorModelManager {
    val state: StateFlow<TranslatorModelState>

    /** Rough download size, shown to the user before downloading. */
    val approximateSizeMb: Int

    /** Rough RAM needed while translating, shown in the warning when the translator is switched on. */
    val approximateRamMb: Int

    suspend fun download()

    fun cancelDownload()

    suspend fun deleteModel()
}
