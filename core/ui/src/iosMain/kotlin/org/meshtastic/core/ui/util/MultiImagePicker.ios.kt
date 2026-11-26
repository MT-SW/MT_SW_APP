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

package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import co.touchlab.kermit.Logger
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private class MultiPickerDelegate(var onPicked: (List<PickedImage>) -> Unit) :
    NSObject(),
    PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val results = didFinishPicking.filterIsInstance<PHPickerResult>()
        if (results.isEmpty()) {
            Logger.i { "Galeria: nic nie wybrano" }
            return
        }
        val slots = arrayOfNulls<PickedImage>(results.size)
        var finished = 0
        results.forEachIndexed { index, result ->
            result.itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data: NSData?, _: NSError? ->
                dispatch_async(dispatch_get_main_queue()) {
                    if (data != null) slots[index] = PickedImage(data.toByteArray(), "image/jpeg")
                    finished += 1
                    if (finished == results.size) {
                        val images = slots.filterNotNull()
                        Logger.i { "Galeria: wybrano ${results.size}, odczytano ${images.size}" }
                        onPicked(images)
                    }
                }
            }
        }
    }
}

@Composable
actual fun rememberMultiImagePicker(onPicked: (List<PickedImage>) -> Unit): (() -> Unit)? {
    val current = rememberUpdatedState(onPicked)
    val delegate = remember { MultiPickerDelegate { images -> current.value(images) } }
    return remember {
        {
            val config = PHPickerConfiguration()
            config.selectionLimit = MAX_PICKED_IMAGES.toLong()
            config.filter = PHPickerFilter.imagesFilter
            val picker = PHPickerViewController(configuration = config)
            picker.delegate = delegate
            topViewController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}
