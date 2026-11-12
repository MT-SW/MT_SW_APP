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

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import co.touchlab.kermit.Logger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.launch
import okio.Buffer
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.repository.FileService
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.import_label
import org.meshtastic.core.resources.play
import org.meshtastic.core.resources.ringtone_file_empty
import org.meshtastic.core.resources.ringtone_import_error
import org.meshtastic.core.resources.ringtone_imported
import org.meshtastic.core.ui.icon.FolderOpen
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.PlayArrow
import org.meshtastic.core.ui.util.rememberOpenFileLauncher
import org.meshtastic.core.ui.util.rememberShowToast
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.create

private const val MAX_RINGTONE_SIZE = 230

// The player stops when it is released, so the one that is playing is kept here.
private var currentPlayer: AVAudioPlayer? = null

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun playWav(wav: ByteArray) {
    val data = wav.usePinned { NSData.create(bytes = it.addressOf(0), length = wav.size.toULong()) }
    val player = AVAudioPlayer(data = data, error = null)
    player.prepareToPlay()
    player.play()
    currentPlayer = player
}

@Suppress("TooGenericExceptionCaught")
@Composable
actual fun RingtoneTrailingIcon(ringtoneInput: String, onRingtoneImported: (String) -> Unit, enabled: Boolean) {
    val fileService = koinInject<FileService>()
    val showToast = rememberShowToast()
    val scope = rememberCoroutineScope()

    val launcher =
        rememberOpenFileLauncher { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        var text = ""
                        fileService.read(uri) { source ->
                            val buffer = Buffer()
                            source.read(buffer, MAX_RINGTONE_SIZE.toLong())
                            text = buffer.readUtf8()
                        }
                        if (text.isNotEmpty()) {
                            onRingtoneImported(text)
                            showToast(getString(Res.string.ringtone_imported))
                        } else {
                            showToast(getString(Res.string.ringtone_file_empty))
                        }
                    } catch (e: Exception) {
                        Logger.e(e) { "Error importing ringtone" }
                        showToast(getString(Res.string.ringtone_import_error, e.message ?: e.toString()))
                    }
                }
            }
        }

    Row {
        IconButton(onClick = { launcher("*/*") }, enabled = enabled) {
            Icon(MeshtasticIcons.FolderOpen, contentDescription = stringResource(Res.string.import_label))
        }

        IconButton(
            onClick = {
                try {
                    RtttlSynth.toWav(ringtoneInput)?.let { playWav(it) }
                } catch (e: Exception) {
                    Logger.e(e) { "Failed to play ringtone" }
                }
            },
            enabled = enabled,
        ) {
            Icon(MeshtasticIcons.PlayArrow, contentDescription = stringResource(Res.string.play))
        }
    }
}
