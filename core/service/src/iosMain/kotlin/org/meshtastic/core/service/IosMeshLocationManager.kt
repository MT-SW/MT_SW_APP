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
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.meshtastic.core.repository.Location
import org.meshtastic.core.repository.MeshLocationManager
import org.meshtastic.core.repository.PhoneLocationProvider
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.proto.Position as ProtoPosition

/**
 * iPhone: sends the phone's GPS position to the radio while location sharing is on. Like on Android, once the phone
 * stays in the radio's stillness cell the phone's GPS rests (saving battery) and wakes when the accelerometer shows
 * movement or the cell changes; firmware keeps broadcasting the last position it was given.
 */
@Single
class IosMeshLocationManager(
    private val provider: PhoneLocationProvider,
    private val radioConfigRepository: RadioConfigRepository,
) : MeshLocationManager {
    private var scope: CoroutineScope? = null
    private var sendPositionFn: (suspend (ProtoPosition) -> Boolean)? = null
    private var job: Job? = null

    /** The bits of the radio's stillness cell, followed while the flow runs. */
    private val cellBits = MutableStateFlow(0)

    override fun start(scope: CoroutineScope, sendPositionFn: suspend (ProtoPosition) -> Boolean) {
        this.scope = scope
        this.sendPositionFn = sendPositionFn
        if (job?.isActive == true) return
        job =
            scope.launch {
                launch {
                    combine(radioConfigRepository.channelSetFlow, radioConfigRepository.localConfigFlow) {
                            channels,
                            config,
                        ->
                        stillnessCellBits(channels.settings, config.device?.role)
                    }
                        .collect { cellBits.value = it }
                }
                feedWhileMoving(
                    fixes = {
                        provider
                            .locationUpdates()
                            .mapNotNull { if (it.permissionGranted) it.location else null }
                            .distinctUntilChanged { a, b -> a.timeMillis == b.timeMillis }
                            .mapNotNull { loc ->
                                Location(
                                    latitude = loc.latitude,
                                    longitude = loc.longitude,
                                    altitudeMeters = loc.altitude,
                                    mslAltitudeMeters = loc.mslAltitude,
                                    accuracyMeters = loc.accuracyM,
                                    speedMetersPerSecond = loc.speedMps,
                                    timeMillis = loc.timeMillis,
                                )
                            }
                    },
                    cellBits = cellBits,
                    awaitMotion = { awaitSignificantMotion() },
                ) { loc ->
                    sendPositionFn(
                        phonePosition(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            timeMillis = loc.timeMillis,
                            mslAltitudeMeters = loc.mslAltitudeMeters,
                            haeAltitudeMeters = loc.altitudeMeters,
                            speedMetersPerSecond = loc.speedMetersPerSecond,
                            bearingDegrees = null,
                        ),
                    )
                }
                coroutineContext.cancelChildren()
            }
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
