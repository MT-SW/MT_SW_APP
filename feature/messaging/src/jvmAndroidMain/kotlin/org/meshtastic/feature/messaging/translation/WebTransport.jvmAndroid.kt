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
@file:Suppress("MagicNumber")

package org.meshtastic.feature.messaging.translation

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI

private const val CONNECT_MS = 8_000
private const val READ_MS = 12_000

internal actual fun platformWebTransport(): WebAccess.Transport = JavaWebTransport

/** Android and computer: Java's own HTTP client, one request without redirects. */
private object JavaWebTransport : WebAccess.Transport {
    override fun get(url: String, accept: String, agent: String, maxBytes: Int): WebAccess.Response {
        @Suppress("DEPRECATION")
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = CONNECT_MS
            connection.readTimeout = READ_MS
            connection.setRequestProperty("User-Agent", agent)
            connection.setRequestProperty("Accept", accept)
            val code = connection.responseCode
            if (code in 300..399) return WebAccess.Response(code, connection.getHeaderField("Location"), ByteArray(0))
            if (code !in 200..299) return WebAccess.Response(code, null, ByteArray(0))
            val out = ByteArrayOutputStream()
            connection.inputStream.use { stream ->
                val buffer = ByteArray(8192)
                while (out.size() < maxBytes) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                }
            }
            return WebAccess.Response(code, null, out.toByteArray())
        } finally {
            connection.disconnect()
        }
    }

    override fun resolvesToLocalNetwork(host: String): Boolean = runCatching {
        InetAddress.getAllByName(host).any {
            it.isAnyLocalAddress || it.isLoopbackAddress || it.isLinkLocalAddress || it.isSiteLocalAddress ||
                it.isMulticastAddress
        }
    }.getOrDefault(false)
}
