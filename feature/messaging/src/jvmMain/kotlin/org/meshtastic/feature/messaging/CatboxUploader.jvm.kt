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
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

actual suspend fun uploadToCatbox(
    bytes: ByteArray,
    mimeType: String,
    fileName: String,
    host: ImageHost,
    onProgress: (Float) -> Unit,
): String {
    // Without limits of its own a slow connection hit the engine's short defaults and the picture stopped part way.
    val client =
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                socketTimeoutMillis = SOCKET_TIMEOUT_MS
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
            }
        }
    try {
        val response =
            client.post(host.url) {
                header(HttpHeaders.UserAgent, "MT_SW_APP")
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            host.extraFields.forEach { (key, value) -> append(key, value) }
                            append(
                                host.fileField,
                                bytes,
                                Headers.build {
                                    append(HttpHeaders.ContentType, mimeType)
                                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                                },
                            )
                        },
                    ),
                )
                onUpload { sent, total ->
                    val whole = (total ?: bytes.size.toLong()).coerceAtLeast(1L)
                    onProgress((sent.toDouble() / whole).toFloat().coerceIn(0f, 1f))
                }
            }
        val answer = response.bodyAsText().trim()
        // The host answers an error with a text, which must never be sent on as if it were a link.
        check(response.status.isSuccess() && answer.startsWith("https://")) {
            "Serwer zdjęć (${host.label}) odpowiedział: ${response.status.value} ${answer.take(MAX_ERROR_CHARS)}"
        }
        onProgress(1f)
        return answer
    } finally {
        client.close()
    }
}

private const val CONNECT_TIMEOUT_MS = 20_000L
private const val SOCKET_TIMEOUT_MS = 90_000L
private const val REQUEST_TIMEOUT_MS = 300_000L
private const val MAX_ERROR_CHARS = 80
