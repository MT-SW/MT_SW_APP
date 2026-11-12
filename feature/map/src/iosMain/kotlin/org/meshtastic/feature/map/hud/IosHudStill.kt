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
@file:Suppress("MagicNumber", "DEPRECATION")

package org.meshtastic.feature.map.hud

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import co.touchlab.kermit.Logger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skia.Image
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeLeft
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeRight
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoOrientationPortraitUpsideDown
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.fileDataRepresentation
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.darwin.NSObject
import kotlin.coroutines.resume

private val log = Logger.withTag("HudStill")

/** Waits for one photo of the camera and hands its data (or null) to [onData]. */
private class PhotoDelegate(private val onData: (NSData?) -> Unit) :
    NSObject(),
    AVCapturePhotoCaptureDelegateProtocol {
    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?,
    ) {
        if (error != null) log.w { "The camera could not take the picture: ${error.localizedDescription}" }
        onData(if (error == null) didFinishProcessingPhoto.fileDataRepresentation() else null)
    }
}

/**
 * Takes a picture with the camera of the terrain view, draws the overlay on it where it showed on the screen and saves
 * the result to the Photos app.
 */
class IosHudStill(private val photoOutput: AVCapturePhotoOutput) : HudStill {
    // The system holds the delegate only weakly, so it is kept here until the photo arrives.
    private var delegate: PhotoDelegate? = null

    override suspend fun take(overlay: ImageBitmap): Boolean {
        val photo = takePhoto() ?: return false
        val overlayImage = overlay.toUiImage() ?: return false
        val composed = compose(photo, overlayImage)
        return savePhoto(composed)
    }

    private suspend fun takePhoto(): UIImage? = suspendCancellableCoroutine { continuation ->
        val connection: AVCaptureConnection? = photoOutput.connectionWithMediaType(AVMediaTypeVideo)
        if (connection != null && connection.isVideoOrientationSupported()) {
            connection.videoOrientation = currentVideoOrientation()
        }
        val photoDelegate = PhotoDelegate { data ->
            delegate = null
            if (continuation.isActive) continuation.resume(data?.let { UIImage.imageWithData(it) })
        }
        delegate = photoDelegate
        photoOutput.capturePhotoWithSettings(AVCapturePhotoSettings.photoSettings(), delegate = photoDelegate)
    }

    /** The photo turned the way the phone is held, so it matches what the preview showed. */
    private fun currentVideoOrientation(): Long = when (UIDevice.currentDevice.orientation) {
        UIDeviceOrientation.UIDeviceOrientationLandscapeLeft -> AVCaptureVideoOrientationLandscapeRight
        UIDeviceOrientation.UIDeviceOrientationLandscapeRight -> AVCaptureVideoOrientationLandscapeLeft
        UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown -> AVCaptureVideoOrientationPortraitUpsideDown
        else -> AVCaptureVideoOrientationPortrait
    }

    /**
     * The preview fills the view and crops the overflow about its center, so the overlay (the size of the view) belongs
     * to that center part of the photo.
     */
    private fun compose(photo: UIImage, overlay: UIImage): UIImage {
        val (photoWidth, photoHeight) = photo.size.useContents { width to height }
        val (overlayWidth, overlayHeight) = overlay.size.useContents { width to height }
        val overlayAspect = if (overlayHeight > 0.0) overlayWidth / overlayHeight else photoWidth / photoHeight
        val visibleWidth: Double
        val visibleHeight: Double
        if (photoWidth / photoHeight > overlayAspect) {
            visibleHeight = photoHeight
            visibleWidth = photoHeight * overlayAspect
        } else {
            visibleWidth = photoWidth
            visibleHeight = photoWidth / overlayAspect
        }
        val format = UIGraphicsImageRendererFormat.defaultFormat().apply { scale = 1.0 }
        val renderer = UIGraphicsImageRenderer(size = CGSizeMake(photoWidth, photoHeight), format = format)
        return renderer.imageWithActions { _ ->
            photo.drawInRect(CGRectMake(0.0, 0.0, photoWidth, photoHeight))
            overlay.drawInRect(
                CGRectMake(
                    (photoWidth - visibleWidth) / 2,
                    (photoHeight - visibleHeight) / 2,
                    visibleWidth,
                    visibleHeight,
                ),
            )
        }
    }

    private suspend fun savePhoto(image: UIImage): Boolean = suspendCancellableCoroutine { continuation ->
        PHPhotoLibrary.sharedPhotoLibrary()
            .performChanges(
                changeBlock = { PHAssetChangeRequest.creationRequestForAssetFromImage(image) },
                completionHandler = { success, error ->
                    if (!success) log.w { "The picture could not be saved: ${error?.localizedDescription}" }
                    if (continuation.isActive) continuation.resume(success)
                },
            )
    }
}

/** The overlay (a Compose image) as a UIImage, going through PNG. */
private fun ImageBitmap.toUiImage(): UIImage? {
    val bytes = Image.makeFromBitmap(asSkiaBitmap()).encodeToData()?.bytes ?: return null
    if (bytes.isEmpty()) return null
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    return UIImage.imageWithData(data)
}
