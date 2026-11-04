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
package org.meshtastic.feature.firmware

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.model.DeviceHardware

/**
 * Firmware update is not ported to iPhone yet: the pages that only read the firmware list work (they need the network
 * only), while anything that would save, unpack or flash a file reports "not available".
 */
@Suppress("TooManyFunctions", "UnusedParameter")
@Single(binds = [FirmwareFileHandler::class])
class IosFirmwareFileHandler(private val client: HttpClient) : FirmwareFileHandler {
    override fun cleanupAllTemporaryFiles() {}

    override suspend fun deleteFile(file: FirmwareArtifact) {}

    override suspend fun checkUrlExists(url: String): Boolean =
        runCatching { client.get(url).status.isSuccess() }.getOrDefault(false)

    override suspend fun fetchText(url: String): String? =
        runCatching { client.get(url).takeIf { it.status.isSuccess() }?.bodyAsText() }.getOrNull()

    override suspend fun downloadFile(url: String, fileName: String, onProgress: (Float) -> Unit): FirmwareArtifact? =
        null

    override suspend fun getFileSize(file: FirmwareArtifact): Long = 0L

    override suspend fun readBytes(artifact: FirmwareArtifact): ByteArray = ByteArray(0)

    override suspend fun importFromUri(uri: CommonUri): FirmwareArtifact? = null

    override suspend fun getDisplayName(uri: CommonUri): String? = null

    override suspend fun copyToUri(source: FirmwareArtifact, destinationUri: CommonUri): Long = 0L

    override suspend fun isRemovableDestination(destinationUri: CommonUri): Boolean = false

    override suspend fun readSiblingText(treeUri: CommonUri, fileName: String): String? = null

    override suspend fun createDocumentInTree(treeUri: CommonUri, fileName: String, mimeType: String): CommonUri? = null

    override suspend fun isDestinationReadable(destinationUri: CommonUri): Boolean = false

    override suspend fun extractFirmware(
        uri: CommonUri,
        hardware: DeviceHardware,
        fileExtension: String,
        preferredFilename: String?,
    ): FirmwareArtifact? = null

    override suspend fun extractFirmwareFromZip(
        zipFile: FirmwareArtifact,
        hardware: DeviceHardware,
        fileExtension: String,
        preferredFilename: String?,
    ): FirmwareArtifact? = null

    override suspend fun extractZipEntries(artifact: FirmwareArtifact): Map<String, ByteArray> = emptyMap()
}
