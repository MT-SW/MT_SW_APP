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
@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package org.meshtastic.ios

import co.touchlab.kermit.Logger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.meshtastic.core.repository.SosAlarmSound
import org.meshtastic.core.repository.SosAlarmStyle
import org.meshtastic.core.repository.SosSiren
import org.meshtastic.ios.notification.toNSData
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.hasTorch
import platform.AVFoundation.torchMode
import platform.AudioToolbox.AudioServicesPlaySystemSound

/**
 * The alarm of a received SOS while the app is running (open, or kept alive in the background by "Praca w tle"): the
 * siren in a loop at full volume on the playback channel (it sounds on a locked phone and with the silent switch on),
 * the vibration, and the torch blinking, all until [stop]. When the app is not running, the notifications posted by
 * the alarm (a new one every ten seconds, with the same siren) keep ringing instead.
 */
internal object IosSosAlarmSound : SosAlarmSound {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sirenPlayer: AVAudioPlayer? = null
    private var vibrationJob: Job? = null
    private var torchJob: Job? = null

    override fun start(flash: Boolean, style: SosAlarmStyle) {
        if (vibrationJob?.isActive == true) return
        startSiren(style)
        vibrationJob =
            scope.launch {
                while (isActive) {
                    AudioServicesPlaySystemSound(VIBRATE_ID)
                    delay(VIBRATE_MS)
                }
            }
        if (flash) startTorch()
    }

    override fun stop() {
        vibrationJob?.cancel()
        vibrationJob = null
        torchJob?.cancel()
        torchJob = null
        setTorch(false)
        sirenPlayer?.stop()
        sirenPlayer = null
    }

    private fun startSiren(style: SosAlarmStyle) {
        runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, error = null)
            session.setActive(true, error = null)
            val sound =
                if (style == SosAlarmStyle.BELL) SosSiren.bellWav(BELL_SECONDS) else SosSiren.wav(SIREN_SECONDS)
            val player = AVAudioPlayer(data = sound.toNSData(), error = null)
            player.numberOfLoops = -1
            player.volume = 1f
            player.prepareToPlay()
            player.play()
            sirenPlayer = player
        }
            .onFailure { Logger.w(it) { "SOS alarm: the siren could not be started" } }
    }

    private fun startTorch() {
        torchJob =
            scope.launch {
                try {
                    while (isActive) {
                        setTorch(true)
                        delay(BLINK_MS)
                        setTorch(false)
                        delay(BLINK_MS)
                    }
                } finally {
                    setTorch(false)
                }
            }
    }

    private fun setTorch(on: Boolean) {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return
        if (!device.hasTorch) return
        if (device.lockForConfiguration(null)) {
            device.torchMode = if (on) AVCaptureTorchModeOn else AVCaptureTorchModeOff
            device.unlockForConfiguration()
        }
    }

    private const val VIBRATE_ID = 4095u
    private const val VIBRATE_MS = 1_500L
    private const val BLINK_MS = 400L
    private const val SIREN_SECONDS = 1.4
    private const val BELL_SECONDS = 2.0
}
