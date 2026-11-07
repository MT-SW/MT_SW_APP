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
@file:Suppress("MagicNumber")

package org.meshtastic.feature.map.hud

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import platform.CoreMotion.CMAttitudeReferenceFrameXMagneticNorthZVertical
import platform.CoreMotion.CMAttitudeReferenceFrameXTrueNorthZVertical
import platform.CoreMotion.CMDeviceMotion
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSError
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSProcessInfoThermalStateCritical
import platform.Foundation.NSProcessInfoThermalStateSerious
import platform.UIKit.UIApplication
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown
import platform.UIKit.UIWindowScene

private const val NORMAL_INTERVAL_S = 1.0 / 30
private const val HOT_INTERVAL_S = 1.0 / 15

/**
 * Orientation of the iPhone for the view with the camera: the motion sensors fused by the system, referenced to true
 * north when location is available (the system then applies the declination itself), otherwise to magnetic north.
 */
@OptIn(ExperimentalForeignApi::class)
class IosCameraPoseProvider : CameraPoseProvider {
    override fun poseUpdates(): Flow<CameraPoseState> {
        val frames = CMMotionManager.availableAttitudeReferenceFrames()
        val trueNorth = frames and CMAttitudeReferenceFrameXTrueNorthZVertical != 0uL
        val magnetic = frames and CMAttitudeReferenceFrameXMagneticNorthZVertical != 0uL
        if (!trueNorth && !magnetic) return flowOf(CameraPoseState(hasSensor = false))
        return callbackFlow {
            val manager = CMMotionManager()
            manager.deviceMotionUpdateInterval = NORMAL_INTERVAL_S
            val frame =
                if (trueNorth) {
                    CMAttitudeReferenceFrameXTrueNorthZVertical
                } else {
                    CMAttitudeReferenceFrameXMagneticNorthZVertical
                }
            manager.startDeviceMotionUpdatesUsingReferenceFrame(frame, NSOperationQueue.mainQueue) {
                    motion: CMDeviceMotion?,
                    _: NSError?,
                ->
                val thermal = currentThermal()
                manager.deviceMotionUpdateInterval =
                    if (thermal == HudThermal.NORMAL) NORMAL_INTERVAL_S else HOT_INTERVAL_S
                motion?.let {
                    trySend(
                        CameraPoseState(
                            pose = it.toCameraPose(interfaceOrientation()),
                            calibration = it.calibrationLevel(),
                            trueNorth = trueNorth,
                            thermal = thermal,
                        ),
                    )
                }
            }
            awaitClose { manager.stopDeviceMotionUpdates() }
        }
    }
}

private fun currentThermal(): HudThermal = when (NSProcessInfo.processInfo.thermalState) {
    NSProcessInfoThermalStateCritical -> HudThermal.CRITICAL
    NSProcessInfoThermalStateSerious -> HudThermal.HOT
    else -> HudThermal.NORMAL
}

private fun interfaceOrientation(): Long =
    (UIApplication.sharedApplication.connectedScenes.firstOrNull() as? UIWindowScene)?.interfaceOrientation ?: 1L

/** The sensor's own status mapped onto 3 high .. 0 unreliable; -1 while the system gives none. */
private fun CMDeviceMotion.calibrationLevel(): Int = when (magneticField.useContents { accuracy }) {
    -1 -> 0
    0 -> 1
    1 -> 2
    2 -> 3
    else -> CALIBRATION_UNKNOWN
}

private operator fun Vec3.unaryMinus() = Vec3(-x, -y, -z)

/**
 * The rows of the rotation matrix are the device axes in the reference frame (north, west, up); the world of the view is
 * (east, north, up). The back camera looks down device -Z; which device axis is the screen's right depends on how the
 * interface is rotated (the same mapping as on Android).
 */
private fun CMDeviceMotion.toCameraPose(orientation: Long): CameraPose = attitude.rotationMatrix.useContents {
    fun axis(north: Double, west: Double, up: Double) = Vec3(-west, north, up)
    val deviceX = axis(m11, m12, m13)
    val deviceY = axis(m21, m22, m23)
    val forward = -axis(m31, m32, m33)
    val (right, up) =
        when (orientation) {
            UIInterfaceOrientationLandscapeRight -> -deviceY to deviceX
            UIInterfaceOrientationLandscapeLeft -> deviceY to -deviceX
            UIInterfaceOrientationPortraitUpsideDown -> -deviceX to -deviceY
            else -> deviceX to deviceY
        }
    CameraPose(right = right, up = up, forward = forward)
}
