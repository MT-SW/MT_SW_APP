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

package org.meshtastic.feature.settings.radio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.repository.AppSettingsBackupOperations
import org.meshtastic.core.repository.SettingsBackupScope
import org.meshtastic.core.ui.util.rememberOpenFileLauncher
import org.meshtastic.core.ui.util.rememberSaveFileLauncher

/** Picker-backed entry points for saving and restoring the app settings, and the look of the app on its own. */
class SettingsBackupActions(
    val onBackupAll: () -> Unit,
    val onRestoreAll: () -> Unit,
    val onBackupLook: () -> Unit,
    val onRestoreLook: () -> Unit,
)

@Composable
fun rememberSettingsBackupActions(): SettingsBackupActions {
    val viewModel = koinViewModel<SettingsBackupViewModel>()
    val saveAll = rememberSaveFileLauncher { uri -> viewModel.backup(SettingsBackupScope.All, uri) }
    val saveLook = rememberSaveFileLauncher { uri -> viewModel.backup(SettingsBackupScope.Look, uri) }
    val openAll = rememberOpenFileLauncher { uri -> if (uri != null) viewModel.restore(SettingsBackupScope.All, uri) }
    val openLook = rememberOpenFileLauncher { uri -> if (uri != null) viewModel.restore(SettingsBackupScope.Look, uri) }
    val mime = AppSettingsBackupOperations.MIME_TYPE
    return remember(viewModel, saveAll, saveLook, openAll, openLook) {
        SettingsBackupActions(
            onBackupAll = { saveAll(viewModel.suggestedFileName(SettingsBackupScope.All), mime) },
            onRestoreAll = { viewModel.confirmRestore(SettingsBackupScope.All) { openAll(mime) } },
            onBackupLook = { saveLook(viewModel.suggestedFileName(SettingsBackupScope.Look), mime) },
            onRestoreLook = { viewModel.confirmRestore(SettingsBackupScope.Look) { openLook(mime) } },
        )
    }
}
