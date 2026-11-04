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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Single
import org.meshtastic.core.repository.MeshLocationManager
import org.meshtastic.core.repository.PhoneLocationProvider
import org.meshtastic.proto.Position as ProtoPosition

/** iPhone: sends the phone's GPS position to the radio while location sharing is on. */
@Single
class IosMeshLocationManager(private val provider: PhoneLocationProvider) : MeshLocationManager {
    private var scope: CoroutineScope? = null
    private var sendPositionFn: (suspend (ProtoPosition) -> Unit)? = null
    private var job: Job? = null

    override fun start(scope: CoroutineScope, sendPositionFn: suspend (ProtoPosition) -> Unit) {
        this.scope = scope
        this.sendPositionFn = sendPositionFn
        if (job?.isActive == true) return
        job =
            provider
                .locationUpdates()
                .mapNotNull { if (it.permissionGranted) it.location else null }
                .onEach { loc ->
                    sendPositionFn(
                        phonePosition(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            timeMillis = loc.timeMillis,
                            mslAltitudeMeters = loc.mslAltitude,
                            haeAltitudeMeters = loc.altitude,
                            speedMetersPerSecond = loc.speedMps,
                            bearingDegrees = null,
                        ),
                    )
                }
                .launchIn(scope)
    }

    override fun restart() {
        val s = scope
        val fn = sendPositionFn
        if (s != null && fn != null) start(s, fn)
    }

    override fun stop() {
        job?.cancel()
        job = null
    }
}
