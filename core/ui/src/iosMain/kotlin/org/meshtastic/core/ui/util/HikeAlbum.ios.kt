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
@file:Suppress("TooGenericExceptionCaught")

package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSMutableArray
import platform.Foundation.NSData
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAssetCollection
import platform.Photos.PHAssetCollectionChangeRequest
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIImage

private const val ALBUM_ID_KEY = "mtsw_hike_album_id"

/** The picture goes to the album "MT_SW" (made on the first picture); the album's id is remembered. */
actual fun saveToHikeAlbum(bytes: ByteArray, fileName: String, folder: String?): Boolean {
    val data: NSData =
        if (bytes.isEmpty()) {
            NSData()
        } else {
            bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        }
    val image = UIImage.imageWithData(data)
    if (image == null) {
        Logger.w { "Album wędrówek: obrazu nie da się odczytać" }
        return false
    }
    val defaults = NSUserDefaults.standardUserDefaults
    val known = defaults.stringForKey(ALBUM_ID_KEY)
    val album =
        known?.let {
            PHAssetCollection.fetchAssetCollectionsWithLocalIdentifiers(listOf(it), null).firstObject
                as? PHAssetCollection
        }
    PHPhotoLibrary.sharedPhotoLibrary()
        .performChanges(
            {
                val asset = PHAssetChangeRequest.creationRequestForAssetFromImage(image)
                val placeholder = asset.placeholderForCreatedAsset
                if (album != null) {
                    PHAssetCollectionChangeRequest.changeRequestForAssetCollection(album)
                        ?.addAssets(NSMutableArray().apply { addObject(placeholder) })
                } else {
                    val create =
                        PHAssetCollectionChangeRequest.creationRequestForAssetCollectionWithTitle(HIKE_ALBUM_NAME)
                    val newId = create.placeholderForCreatedAssetCollection.localIdentifier
                    defaults.setObject(newId, ALBUM_ID_KEY)
                    create.addAssets(NSMutableArray().apply { addObject(placeholder) })
                }
            },
            completionHandler = { ok, error ->
                if (ok) {
                    Logger.i { "Album wędrówek: zapisano w albumie" }
                } else {
                    Logger.w { "Album wędrówek: zapis nie udał się: ${error?.localizedDescription}" }
                }
            },
        )
    return true
}

@Composable actual fun rememberAlbumFolderPicker(onPicked: (String) -> Unit): (() -> Unit)? = null

actual fun defaultAlbumFolder(): String? = null
