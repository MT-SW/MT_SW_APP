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
package org.meshtastic.core.service

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.meshtastic.core.repository.SosAlarmSound
import org.meshtastic.core.repository.SosAlarmStyle

/**
 * What the alarm of a received SOS adds on a phone besides the sound of its notification (which rings again and again
 * by itself): the torch flashes until [stop]. Needs no permission. A phone without a torch does nothing.
 */
class AndroidSosAlarmFeedback(context: Context) : SosAlarmSound {
    private val cameras = context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    @Synchronized
    override fun start(flash: Boolean, style: SosAlarmStyle) {
        if (!flash || job?.isActive == true) return
        val manager = cameras ?: return
        val id =
            runCatching {
                manager.cameraIdList.firstOrNull {
                    manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                }
            }
                .getOrNull() ?: return
        job =
            scope.launch {
                try {
                    while (isActive) {
                        setTorch(manager, id, true)
                        delay(BLINK_MS)
                        setTorch(manager, id, false)
                        delay(BLINK_MS)
                    }
                } finally {
                    setTorch(manager, id, false)
                }
            }
    }

    @Synchronized
    override fun stop() {
        job?.cancel()
        job = null
    }

    private fun setTorch(manager: CameraManager, id: String, on: Boolean) {
        runCatching { manager.setTorchMode(id, on) }.onFailure { Logger.d { "SOS alarm: torch not available" } }
    }

    private companion object {
        const val BLINK_MS = 400L
    }
}
