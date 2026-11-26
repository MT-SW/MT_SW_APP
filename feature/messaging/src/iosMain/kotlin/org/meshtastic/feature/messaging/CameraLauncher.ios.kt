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
@file:Suppress("ParameterNaming")

package org.meshtastic.feature.messaging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import co.touchlab.kermit.Logger
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
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
}

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
            topViewController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}
