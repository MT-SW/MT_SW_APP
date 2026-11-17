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
package org.meshtastic.feature.settings.radio

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.common.util.nowInstant
import org.meshtastic.core.repository.AppSettingsBackupOperations
import org.meshtastic.core.repository.FileService
import org.meshtastic.core.repository.SettingsBackupScope
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.app_settings_backup_saved
import org.meshtastic.core.resources.app_settings_restore_confirmation
import org.meshtastic.core.resources.app_settings_restore_done
import org.meshtastic.core.resources.app_settings_restore_not_backup
import org.meshtastic.core.resources.are_you_sure
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.resources.look_restore_confirmation
import org.meshtastic.core.resources.look_restore_done
import org.meshtastic.core.resources.look_saved
import org.meshtastic.core.resources.restore_app_settings
import org.meshtastic.core.resources.restore_look
import org.meshtastic.core.ui.util.AlertManager
import org.meshtastic.core.ui.viewmodel.safeLaunch

/** Saves the app settings, or only the look of the app, to a file and restores them from one. Same on all platforms. */
@KoinViewModel
class SettingsBackupViewModel(
    private val backupService: AppSettingsBackupOperations,
    private val fileService: FileService,
    private val alertManager: AlertManager,
) : ViewModel() {

    /** File name offered by the save picker: what is saved and today's date. */
    fun suggestedFileName(scope: SettingsBackupScope): String {
        val date = nowInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val what = if (scope == SettingsBackupScope.Look) "wyglad" else "ustawienia"
        return "mt_sw_${what}_$date.json"
    }

    fun backup(scope: SettingsBackupScope, uri: CommonUri) {
        safeLaunch(tag = "settingsBackup") {
            val written = fileService.write(uri) { sink -> backupService.export(scope, sink) }
            if (written) {
                val look = scope == SettingsBackupScope.Look
                val saved = if (look) Res.string.look_saved else Res.string.app_settings_backup_saved
                alertManager.showAlert(message = getStringSuspend(saved))
            } else {
                Logger.w { "Settings backup ($scope) could not be written" }
            }
        }
    }

    /** Asks for confirmation, then calls [onConfirmed] so the caller can open its file picker. */
    fun confirmRestore(scope: SettingsBackupScope, onConfirmed: () -> Unit) {
        safeLaunch(tag = "settingsRestoreConfirm") {
            val look = scope == SettingsBackupScope.Look
            alertManager.showAlert(
                titleRes = Res.string.are_you_sure,
                message =
                getStringSuspend(
                    if (look) Res.string.look_restore_confirmation else Res.string.app_settings_restore_confirmation,
                ),
                confirmTextRes = if (look) Res.string.restore_look else Res.string.restore_app_settings,
                onConfirm = onConfirmed,
            )
        }
    }

    fun restore(scope: SettingsBackupScope, uri: CommonUri) {
        safeLaunch(tag = "settingsRestore") {
            var restored: Int? = null
            val read = fileService.read(uri) { source -> restored = backupService.import(scope, source) }
            val count = restored
            val message =
                when {
                    !read || count == null -> getStringSuspend(Res.string.app_settings_restore_not_backup)
                    scope == SettingsBackupScope.Look -> getStringSuspend(Res.string.look_restore_done)
                    else -> getStringSuspend(Res.string.app_settings_restore_done, count)
                }
            if (!read) Logger.w { "Settings restore ($scope): the file could not be read" }
            alertManager.showAlert(message = message)
        }
    }
}
