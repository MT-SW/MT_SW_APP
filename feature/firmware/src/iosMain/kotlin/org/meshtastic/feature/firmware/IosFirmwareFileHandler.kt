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
@file:Suppress("LoopWithTooManyJumpStatements")

package org.meshtastic.feature.firmware

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.model.DeviceHardware
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToURL
import platform.posix.memcpy

private const val MAX_ENTRIES = 512
private const val MAX_FILE_BYTES = 128L * 1024L * 1024L
private const val MAX_TOTAL_BYTES = 256L * 1024L * 1024L
private const val CHUNK_BYTES = 128 * 1024

/**
 * Firmware files on the iPhone: downloaded to the app's temporary folder, unpacked from the release zip with okio, or
 * taken from a file the user chose. Flashing itself is done by the shared update code.
 */
@Suppress("TooManyFunctions")
@Single(binds = [FirmwareFileHandler::class])
class IosFirmwareFileHandler(private val client: HttpClient) : FirmwareFileHandler {
    private val fs = FileSystem.SYSTEM
    private val tempDir: Path = (NSTemporaryDirectory().trimEnd('/') + "/firmware").toPath()

    private fun ensureTempDir() = fs.createDirectories(tempDir)

    private fun artifactFor(path: Path) = FirmwareArtifact(
        uri = CommonUri.parse(NSURL.fileURLWithPath(path.toString()).absoluteString.orEmpty()),
        fileName = path.name,
        isTemporary = true,
    )

    private fun pathOf(artifact: FirmwareArtifact): Path? = pathOf(artifact.uri)

    private fun pathOf(uri: CommonUri): Path? =
        NSURL.URLWithString(uri.toString())?.takeIf { it.fileURL }?.path?.toPath()

    override fun cleanupAllTemporaryFiles() {
        runCatching {
            if (fs.exists(tempDir)) fs.deleteRecursively(tempDir)
            ensureTempDir()
        }
            .onFailure { Logger.w(it) { "Failed to cleanup temp directory" } }
    }

    override suspend fun deleteFile(file: FirmwareArtifact) {
        if (!file.isTemporary) return
        pathOf(file)?.let { runCatching { fs.delete(it, mustExist = false) } }
    }

    override suspend fun checkUrlExists(url: String): Boolean = try {
        client.head(url).status.isSuccess()
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Logger.w(e) { "Failed to check URL existence: $url" }
        false
    }

    override suspend fun fetchText(url: String): String? = try {
        val response = client.get(url)
        if (response.status.isSuccess()) response.bodyAsText() else null
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Logger.w(e) { "Failed to fetch text from: $url" }
        null
    }

    override suspend fun downloadFile(url: String, fileName: String, onProgress: (Float) -> Unit): FirmwareArtifact? =
        withContext(Dispatchers.Default) {
            // The name can come from a remote list, so it must name a file directly inside the temp folder.
            if (fileName.isEmpty() || fileName.contains('/') || fileName.contains("..")) {
                Logger.w { "Refusing a download name outside the temp directory: $fileName" }
                return@withContext null
            }
            ensureTempDir()
            val target = tempDir / fileName
            try {
                var ok = false
                client.prepareGet(url).execute { response ->
                    if (!response.status.isSuccess()) {
                        Logger.w { "Download failed: ${response.status.value} for $url" }
                        return@execute
                    }
                    val total = response.contentLength() ?: -1L
                    val channel = response.bodyAsChannel()
                    val buffer = ByteArray(CHUNK_BYTES)
                    var written = 0L
                    val sink = fs.sink(target).buffer()
                    try {
                        while (true) {
                            val n = channel.readAvailable(buffer, 0, buffer.size)
                            if (n < 0) break
                            if (n == 0) continue
                            sink.write(buffer, 0, n)
                            written += n
                            if (total > 0) onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                        }
                    } finally {
                        sink.close()
                    }
                    ok = true
                }
                if (ok) artifactFor(target) else null
            } catch (e: CancellationException) {
                runCatching { fs.delete(target, mustExist = false) }
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w(e) { "Download failed for $url" }
                runCatching { fs.delete(target, mustExist = false) }
                null
            }
        }

    override suspend fun getFileSize(file: FirmwareArtifact): Long =
        pathOf(file)?.let { runCatching { fs.metadata(it).size }.getOrNull() } ?: 0L

    override suspend fun readBytes(artifact: FirmwareArtifact): ByteArray =
        pathOf(artifact)?.let { runCatching { fs.read(it) { readByteArray() } }.getOrNull() } ?: ByteArray(0)

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun importFromUri(uri: CommonUri): FirmwareArtifact? = withContext(Dispatchers.Default) {
        val url = NSURL.URLWithString(uri.toString()) ?: return@withContext null
        // A file picked by the user outside the app needs this permission while it is being read.
        val scoped = url.startAccessingSecurityScopedResource()
        try {
            val data = NSData.dataWithContentsOfURL(url) ?: return@withContext null
            ensureTempDir()
            val target = tempDir / (url.lastPathComponent ?: "firmware.bin")
            fs.write(target) { write(data.toByteArray()) }
            artifactFor(target)
        } finally {
            if (scoped) url.stopAccessingSecurityScopedResource()
        }
    }

    override suspend fun getDisplayName(uri: CommonUri): String? =
        NSURL.URLWithString(uri.toString())?.lastPathComponent

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun copyToUri(source: FirmwareArtifact, destinationUri: CommonUri): Long {
        val bytes = readBytes(source)
        val url = NSURL.URLWithString(destinationUri.toString()) ?: return 0L
        val scoped = url.startAccessingSecurityScopedResource()
        return try {
            if (bytes.toNSData().writeToURL(url, atomically = true)) bytes.size.toLong() else 0L
        } finally {
            if (scoped) url.stopAccessingSecurityScopedResource()
        }
    }

    override suspend fun isRemovableDestination(destinationUri: CommonUri): Boolean = false

    override suspend fun readSiblingText(treeUri: CommonUri, fileName: String): String? = null

    override suspend fun createDocumentInTree(treeUri: CommonUri, fileName: String, mimeType: String): CommonUri? = null

    override suspend fun isDestinationReadable(destinationUri: CommonUri): Boolean =
        pathOf(destinationUri)?.let { NSFileManager.defaultManager.isReadableFileAtPath(it.toString()) } ?: false

    override suspend fun extractFirmware(
        uri: CommonUri,
        hardware: DeviceHardware,
        fileExtension: String,
        preferredFilename: String?,
    ): FirmwareArtifact? {
        val zip = importFromUri(uri) ?: return null
        return try {
            extractFirmwareFromZip(zip, hardware, fileExtension, preferredFilename)
        } finally {
            deleteFile(zip)
        }
    }

    override suspend fun extractFirmwareFromZip(
        zipFile: FirmwareArtifact,
        hardware: DeviceHardware,
        fileExtension: String,
        preferredFilename: String?,
    ): FirmwareArtifact? = withContext(Dispatchers.Default) {
        val zipPath = pathOf(zipFile) ?: return@withContext null
        val target = hardware.effectiveTarget
        if (target.isEmpty() && preferredFilename == null) return@withContext null
        try {
            val zipFs = fs.openZip(zipPath)
            val files =
                zipFs
                    .listRecursively("/".toPath())
                    .filter { zipFs.metadataOrNull(it)?.isDirectory == false }
                    .toList()
            require(files.size <= MAX_ENTRIES) { "Firmware archive has more than $MAX_ENTRIES entries" }
            val matches = files.filter { entry ->
                val name = entry.toString().removePrefix("/").lowercase()
                if (preferredFilename != null) {
                    name.substringAfterLast('/') == preferredFilename.lowercase()
                } else {
                    isValidFirmwareFile(name, target.lowercase(), fileExtension)
                }
            }
            val chosen = matches.minByOrNull { it.toString().length } ?: return@withContext null
            require((zipFs.metadata(chosen).size ?: 0L) <= MAX_FILE_BYTES) { "Firmware entry is too large" }
            ensureTempDir()
            // Only the last path segment is kept, so an entry name cannot write outside the temp folder.
            val out = tempDir / chosen.name.lowercase()
            val source = zipFs.source(chosen).buffer()
            try {
                fs.write(out) { writeAll(source) }
            } finally {
                source.close()
            }
            artifactFor(out)
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(e) { "Failed to extract firmware" }
            null
        }
    }

    override suspend fun extractZipEntries(artifact: FirmwareArtifact): Map<String, ByteArray> =
        withContext(Dispatchers.Default) {
            val zipPath = pathOf(artifact) ?: return@withContext emptyMap()
            val zipFs = fs.openZip(zipPath)
            val files =
                zipFs.listRecursively("/".toPath()).filter { zipFs.metadataOrNull(it)?.isDirectory == false }.toList()
            require(files.size <= MAX_ENTRIES) { "Firmware archive has more than $MAX_ENTRIES entries" }
            var remaining = MAX_TOTAL_BYTES
            val entries = LinkedHashMap<String, ByteArray>()
            for (file in files) {
                val size = zipFs.metadata(file).size ?: 0L
                remaining -= size
                require(remaining >= 0) { "Firmware archive expands past the $MAX_TOTAL_BYTES-byte limit" }
                entries[file.toString().removePrefix("/")] = zipFs.read(file) { readByteArray() }
            }
            entries
        }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val n = length.toInt()
    val out = ByteArray(n)
    if (n > 0) out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
