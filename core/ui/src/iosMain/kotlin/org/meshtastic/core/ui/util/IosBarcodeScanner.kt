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

package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.camera_permission_rationale
import org.meshtastic.core.resources.cancel
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIButton
import platform.UIKit.UIButtonTypeSystem
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

private const val BUTTON_WIDTH = 160.0
private const val BUTTON_HEIGHT = 48.0
private const val BUTTON_BOTTOM_MARGIN = 48.0

/** The live camera picture with a close button at the bottom. */
private class ScannerView(session: AVCaptureSession, cancelTitle: String) :
    UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private val previewLayer = AVCaptureVideoPreviewLayer(session = session)
    val cancelButton: UIButton = UIButton.buttonWithType(UIButtonTypeSystem)

    init {
        backgroundColor = UIColor.blackColor
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        layer.addSublayer(previewLayer)
        cancelButton.setTitle(cancelTitle, forState = UIControlStateNormal)
        cancelButton.setTitleColor(UIColor.whiteColor, forState = UIControlStateNormal)
        cancelButton.backgroundColor = UIColor.blackColor.colorWithAlphaComponent(0.6)
        cancelButton.layer.cornerRadius = BUTTON_HEIGHT / 2
        addSubview(cancelButton)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer.frame = bounds
        val (width, height) = bounds.useContents { size.width to size.height }
        val bottomInset = safeAreaInsets.useContents { bottom }
        cancelButton.setFrame(
            CGRectMake(
                (width - BUTTON_WIDTH) / 2,
                height - bottomInset - BUTTON_BOTTOM_MARGIN - BUTTON_HEIGHT,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
            ),
        )
    }
}

/** Reports the first QR code the camera reads. */
private class CodeDelegate(private val onCode: (String) -> Unit) :
    NSObject(),
    AVCaptureMetadataOutputObjectsDelegateProtocol {
    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        val code =
            didOutputMetadataObjects.filterIsInstance<AVMetadataMachineReadableCodeObject>().firstNotNullOfOrNull {
                it.stringValue
            }
        if (code != null) onCode(code)
    }
}

private class CancelTarget(private val onCancel: () -> Unit) : NSObject() {
    @ObjCAction
    fun cancel() {
        onCancel()
    }
}

/** What has to stay alive while the scanner is on the screen: the system holds delegates and targets only weakly. */
private class ActiveScan(val delegate: CodeDelegate, val target: CancelTarget)

private var activeScan: ActiveScan? = null

private fun presentScanner(cancelTitle: String, onResult: (String?) -> Unit) {
    val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
    val input = device?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, error = null) }
    val session = AVCaptureSession()
    val output = AVCaptureMetadataOutput()
    val host = topViewController()
    if (input == null || host == null || !session.canAddInput(input) || !session.canAddOutput(output)) {
        onResult(null)
        return
    }
    session.addInput(input)
    session.addOutput(output)

    val controller = UIViewController(nibName = null, bundle = null)
    val scannerView = ScannerView(session, cancelTitle)
    controller.view = scannerView
    controller.modalPresentationStyle = UIModalPresentationFullScreen

    var finished = false
    fun finish(result: String?) {
        if (finished) return
        finished = true
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) { session.stopRunning() }
        controller.dismissViewControllerAnimated(true) {
            activeScan = null
            onResult(result)
        }
    }

    val delegate = CodeDelegate { finish(it) }
    val target = CancelTarget { finish(null) }
    activeScan = ActiveScan(delegate, target)
    output.setMetadataObjectsDelegate(delegate, queue = dispatch_get_main_queue())
    output.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
    scannerView.cancelButton.addTarget(target, NSSelectorFromString("cancel"), UIControlEventTouchUpInside)

    host.presentViewController(controller, animated = true) {
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) { session.startRunning() }
    }
}

/**
 * Reads QR codes with the iPhone camera: a full-screen camera view that closes by itself on the first code (or with the
 * close button). Without the permission it asks for it, and when it was refused it opens the app settings.
 */
@Composable
fun rememberIosBarcodeScanner(onResult: (String?) -> Unit): BarcodeScanner {
    val currentOnResult = rememberUpdatedState(onResult)
    val cancelTitle = stringResource(Res.string.cancel)
    val rationale = stringResource(Res.string.camera_permission_rationale)
    return remember(cancelTitle, rationale) {
        object : BarcodeScanner {
            override fun startScan() {
                when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
                    AVAuthorizationStatusAuthorized -> presentScanner(cancelTitle) { currentOnResult.value(it) }

                    AVAuthorizationStatusNotDetermined ->
                        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                            dispatch_async(dispatch_get_main_queue()) {
                                if (granted) {
                                    presentScanner(cancelTitle) { currentOnResult.value(it) }
                                } else {
                                    currentOnResult.value(null)
                                }
                            }
                        }

                    else -> {
                        showToastNow(rationale)
                        openAppSettings()
                        currentOnResult.value(null)
                    }
                }
            }
        }
    }
}
