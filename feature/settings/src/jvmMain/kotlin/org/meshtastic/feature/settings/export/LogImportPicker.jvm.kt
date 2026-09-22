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
package org.meshtastic.feature.settings.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser

@Composable
actual fun rememberLogImportPicker(onResult: (LogImportContent) -> Unit): LogImportPickerLauncher {
    val scope = rememberCoroutineScope()
    return LogImportPickerLauncher {
        scope.launch {
            val imported =
                withContext(Dispatchers.IO) {
                    @Suppress("TooGenericExceptionCaught")
                    try {
                        val chooser = JFileChooser().apply { dialogTitle = "Load Log" }
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            val file = chooser.selectedFile
                            LogImportContent(content = file.readBytes(), fileName = file.name)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Logger.e(throwable = e) { "Failed to read log import file on desktop" }
                        null
                    }
                }
            imported?.let(onResult)
        }
    }
}
