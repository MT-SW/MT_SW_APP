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
package org.meshtastic.feature.messaging

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

actual suspend fun downloadBytes(url: String, maxBytes: Long): ByteArray {
    val client =
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = DOWNLOAD_CONNECT_TIMEOUT_MS
                socketTimeoutMillis = DOWNLOAD_SOCKET_TIMEOUT_MS
                requestTimeoutMillis = DOWNLOAD_REQUEST_TIMEOUT_MS
            }
        }
    try {
        val response = client.get(url) { header(HttpHeaders.UserAgent, "MT_SW_APP") }
        check(response.status.isSuccess()) { "Serwer odpowiedział: ${response.status.value}" }
        val bytes = response.readRawBytes()
        check(bytes.size <= maxBytes) { "Plik jest za duży: ${bytes.size} bajtów" }
        return bytes
    } finally {
        client.close()
    }
}

private const val DOWNLOAD_CONNECT_TIMEOUT_MS = 20_000L
private const val DOWNLOAD_SOCKET_TIMEOUT_MS = 90_000L
private const val DOWNLOAD_REQUEST_TIMEOUT_MS = 300_000L
