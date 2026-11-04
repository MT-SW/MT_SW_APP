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
package org.meshtastic.ios

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import io.ktor.client.HttpClient
import okio.Path.Companion.toPath
import org.koin.compose.koinInject
import org.meshtastic.core.database.iosDataDir

private const val MEMORY_CACHE_MAX_BYTES = 64L * 1024L * 1024L
private const val DISK_CACHE_MAX_BYTES = 32L * 1024L * 1024L

/** Image loading (maps of hardware pictures, avatars, links) with a memory and a disk cache. */
@Composable
fun IosCoilSetup() {
    val httpClient = koinInject<HttpClient>()
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(httpClient = httpClient))
                add(SvgDecoder.Factory(renderToBitmap = true))
            }
            .memoryCache { MemoryCache.Builder().maxSizeBytes(MEMORY_CACHE_MAX_BYTES).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory((iosDataDir() + "/image_cache_v3").toPath())
                    .maxSizeBytes(DISK_CACHE_MAX_BYTES)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
