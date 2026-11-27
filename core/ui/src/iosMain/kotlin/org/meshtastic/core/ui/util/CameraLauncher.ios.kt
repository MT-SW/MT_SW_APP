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
@file:Suppress("ParameterNaming", "MagicNumber")
@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import co.touchlab.kermit.Logger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSLocale
import platform.Foundation.NSSelectorFromString
import platform.Foundation.preferredLanguages
import platform.UIKit.UIButton
import platform.UIKit.UIButtonTypeSystem
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerCameraDevice
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import platform.darwin.NSObject

private const val JPEG_QUALITY = 0.85

private class CameraDelegate(var onPhoto: (ByteArray, String) -> Unit) :
    NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val data = image?.let { UIImageJPEGRepresentation(it, JPEG_QUALITY) }
        if (data == null) {
            Logger.w { "Aparat: brak zdjęcia w odpowiedzi aparatu" }
            return
        }
        Logger.i { "Aparat: zdjęcie zrobione, ${data.length} bajtów" }
        onPhoto(data.toByteArray(), "image/jpeg")
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        Logger.i { "Aparat: zdjęcie nie zostało zrobione" }
        picker.dismissViewControllerAnimated(true, completion = null)
    }

    /** Keeps the buttons of the camera screen alive: a button only holds a weak reference to its target. */
    var controls: CameraControls? = null
}

/**
 * MT_SW: our own buttons on the camera screen (shutter, flip, cancel). The system screen shown after the shot only
 * offers "retake" and "use", so there was no way out; with our buttons the photo goes straight on to the question
 * whether to send it, and "cancel" leaves the camera at any moment.
 */
private class CameraControls(private val picker: UIImagePickerController, private val delegate: CameraDelegate) :
    NSObject() {
    @ObjCAction
    fun shoot() {
        Logger.i { "Aparat: przycisk zrobienia zdjęcia" }
        picker.takePicture()
    }

    @ObjCAction
    fun flip() {
        val toFront = picker.cameraDevice == UIImagePickerControllerCameraDevice.UIImagePickerControllerCameraDeviceRear
        Logger.i { "Aparat: przełączenie kamery na ${if (toFront) "przednią" else "tylną"}" }
        picker.cameraDevice =
            if (toFront) {
                UIImagePickerControllerCameraDevice.UIImagePickerControllerCameraDeviceFront
            } else {
                UIImagePickerControllerCameraDevice.UIImagePickerControllerCameraDeviceRear
            }
    }

    @ObjCAction
    fun cancel() {
        Logger.i { "Aparat: przycisk anulowania" }
        delegate.imagePickerControllerDidCancel(picker)
    }

    /** The overlay with the three buttons, laid over the live view of the camera. */
    fun overlay(): UIView {
        val polish = NSLocale.preferredLanguages.firstOrNull()?.toString()?.startsWith("pl") == true
        val width = UIScreen.mainScreen.bounds.useContents { size.width }
        val height = UIScreen.mainScreen.bounds.useContents { size.height }
        val view = UIView(frame = CGRectMake(0.0, 0.0, width, height))
        view.userInteractionEnabled = true
        val top = height - BOTTOM_ROW_FROM_BOTTOM
        view.addSubview(button(if (polish) "Anuluj" else "Cancel", "cancel", 16.0, top, SIDE_BUTTON_WIDTH))
        val flipX = width - SIDE_BUTTON_WIDTH - 16.0
        view.addSubview(button(if (polish) "Obróć" else "Flip", "flip", flipX, top, SIDE_BUTTON_WIDTH))
        // The shutter: a large round white button in the middle.
        val shutter = button("", "shoot", (width - SHUTTER_SIZE) / 2.0, top - 12.0, SHUTTER_SIZE)
        shutter.backgroundColor = UIColor.whiteColor
        shutter.layer.cornerRadius = SHUTTER_SIZE / 2.0
        shutter.layer.borderWidth = 4.0
        shutter.layer.borderColor = UIColor.grayColor.CGColor
        shutter.setFrame(CGRectMake(shutter.frame.useContents { origin.x }, top - 12.0, SHUTTER_SIZE, SHUTTER_SIZE))
        view.addSubview(shutter)
        return view
    }

    private fun button(title: String, action: String, x: Double, y: Double, width: Double): UIButton {
        val button = UIButton.buttonWithType(UIButtonTypeSystem)
        button.setFrame(CGRectMake(x, y, width, BUTTON_HEIGHT))
        button.setTitle(title, forState = UIControlStateNormal)
        button.setTitleColor(UIColor.whiteColor, forState = UIControlStateNormal)
        button.addTarget(this, action = NSSelectorFromString(action), forControlEvents = UIControlEventTouchUpInside)
        return button
    }
}

private const val BOTTOM_ROW_FROM_BOTTOM = 110.0
private const val SIDE_BUTTON_WIDTH = 90.0
private const val SHUTTER_SIZE = 72.0
private const val BUTTON_HEIGHT = 48.0

@Composable
actual fun rememberCameraLauncher(onPhotoTaken: (bytes: ByteArray, mimeType: String) -> Unit): (() -> Unit)? {
    val camera = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
    if (!UIImagePickerController.isSourceTypeAvailable(camera)) return null
    val current = rememberUpdatedState(onPhotoTaken)
    val delegate = remember { CameraDelegate { b, m -> current.value(b, m) } }
    return remember {
        {
            Logger.i { "Aparat: przycisk dotknięty" }
            val picker = UIImagePickerController()
            picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
            picker.delegate = delegate
            picker.showsCameraControls = false
            val controls = CameraControls(picker, delegate)
            delegate.controls = controls
            picker.cameraOverlayView = controls.overlay()
            topViewController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}
