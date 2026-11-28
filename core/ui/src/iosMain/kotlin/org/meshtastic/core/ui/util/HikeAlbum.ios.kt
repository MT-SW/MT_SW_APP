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
@file:Suppress("TooGenericExceptionCaught")

package org.meshtastic.core.ui.util

import co.touchlab.kermit.Logger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSMutableArray
import platform.Foundation.NSNumber
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.valueForKey
import platform.Photos.PHAsset
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAssetCollection
import platform.Photos.PHAssetCollectionChangeRequest
import platform.Photos.PHAssetResource
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
    Logger.i { "Album wędrówek: zapis zdjęcia, odczyt albumu" }
    val defaults = NSUserDefaults.standardUserDefaults
    val known = defaults.stringForKey(ALBUM_ID_KEY)
    val album = known?.let {
        PHAssetCollection.fetchAssetCollectionsWithLocalIdentifiers(listOf(it), null).firstObject as? PHAssetCollection
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

actual fun appPhotoFolder(): String? = null

actual fun currentPhotoFolder(): String? = null

actual fun changeAppPhotoFolder(): String? = null

actual fun defaultPhotoFolder(): String? = null

private const val BYTES_PER_MB = 1024L * 1024L

/** The room taken by the pictures in the album "MT_SW" and the free space of the phone's storage. */
actual fun photoFolderUsage(folder: String?): PhotoFolderUsage? = try {
    Logger.i { "Album zdjęć: odczyt albumu" }
    var used = 0L
    val known = NSUserDefaults.standardUserDefaults.stringForKey(ALBUM_ID_KEY)
    val album = known?.let {
        PHAssetCollection.fetchAssetCollectionsWithLocalIdentifiers(listOf(it), null).firstObject
            as? PHAssetCollection
    }
    if (album != null) {
        val assets = PHAsset.fetchAssetsInAssetCollection(album, null)
        for (index in 0 until assets.count.toInt()) {
            val asset = assets.objectAtIndex(index.toULong()) as? PHAsset ?: continue
            PHAssetResource.assetResourcesForAsset(asset).forEach { resource ->
                val size = (resource as? PHAssetResource)?.valueForKey("fileSize") as? NSNumber
                used += size?.longLongValue ?: 0L
            }
        }
    }
    val attributes = NSFileManager.defaultManager.attributesOfFileSystemForPath(NSHomeDirectory(), null)
    val free = (attributes?.get(NSFileSystemFreeSize) as? NSNumber)?.longLongValue ?: 0L
    Logger.i { "Album zdjęć: zajmuje ${used / BYTES_PER_MB} MB, wolne ${free / BYTES_PER_MB} MB" }
    PhotoFolderUsage(used / BYTES_PER_MB, free / BYTES_PER_MB)
} catch (e: Exception) {
    Logger.w { "Album zdjęć: nie udało się policzyć miejsca: ${e.message}" }
    null
}
