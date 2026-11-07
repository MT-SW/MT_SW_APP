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
@file:OptIn(ExperimentalForeignApi::class)
@file:Suppress("MagicNumber")

package org.meshtastic.feature.map.hud

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.position
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMVideoFormatDescriptionGetDimensions
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import kotlin.math.tan

private const val SENSOR_LONG_MM = 10f

/** A view whose only content is the camera's live picture, kept the size of the view. */
private class PreviewView(session: AVCaptureSession) : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val previewLayer = AVCaptureVideoPreviewLayer(session = session)

    init {
        backgroundColor = UIColor.blackColor
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer.frame = bounds
    }
}

/**
 * The back camera of the iPhone as the live picture of the view. The system reports the horizontal field of view of the
 * active format, which is turned into the intrinsics the overlay needs (a sensor of a fixed size and the focal length
 * that gives the same angle).
 */
@Composable
actual fun HudCameraPreview(
    onIntrinsics: (CameraIntrinsics?) -> Unit,
    onAvailable: (Boolean) -> Unit,
    onStill: (HudStill?) -> Unit,
    modifier: Modifier,
) {
    val currentOnIntrinsics by rememberUpdatedState(onIntrinsics)
    val currentOnAvailable by rememberUpdatedState(onAvailable)
    val currentOnStill by rememberUpdatedState(onStill)
    val session = remember { AVCaptureSession() }

    DisposableEffect(session) {
        // There is no still of the camera on iPhone; the overlay is shown without the picture capture.
        currentOnStill(null)
        fun start() {
            val device =
                AVCaptureDevice.devicesWithMediaType(AVMediaTypeVideo).filterIsInstance<AVCaptureDevice>().firstOrNull {
                    it.position == AVCaptureDevicePositionBack
                } ?: AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
            val input = device?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, error = null) }
            if (device == null || input == null || !session.canAddInput(input)) {
                currentOnAvailable(false)
                return
            }
            session.beginConfiguration()
            if (session.canSetSessionPreset(AVCaptureSessionPresetPhoto)) {
                session.sessionPreset = AVCaptureSessionPresetPhoto
            }
            session.addInput(input)
            session.commitConfiguration()
            val format = device.activeFormat
            val fovRad = format.videoFieldOfView.toDouble() * kotlin.math.PI / 180.0
            val dims = CMVideoFormatDescriptionGetDimensions(format.formatDescription).useContents { width to height }
            val long = maxOf(dims.first, dims.second).toFloat()
            val short = minOf(dims.first, dims.second).toFloat()
            if (fovRad > 0 && long > 0f) {
                currentOnIntrinsics(
                    CameraIntrinsics(
                        focalLengthMm = (SENSOR_LONG_MM / 2 / tan(fovRad / 2)).toFloat(),
                        sensorLongMm = SENSOR_LONG_MM,
                        sensorShortMm = SENSOR_LONG_MM * short / long,
                    ),
                )
            }
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
                session.startRunning()
                dispatch_async(dispatch_get_main_queue()) { currentOnAvailable(session.running) }
            }
        }
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> start()

            AVAuthorizationStatusNotDetermined ->
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    dispatch_async(dispatch_get_main_queue()) { if (granted) start() else currentOnAvailable(false) }
                }

            else -> currentOnAvailable(false)
        }
        onDispose {
            if (session.running) session.stopRunning()
            currentOnIntrinsics(null)
        }
    }

    UIKitView(
        factory = { PreviewView(session) },
        modifier = modifier,
        properties = UIKitInteropProperties(isInteractive = false, isNativeAccessibilityEnabled = false),
    )
}
