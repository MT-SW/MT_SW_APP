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

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking

internal actual fun platformWebTransport(): WebAccess.Transport = KtorWebTransport

/**
 * iPhone and iPad: Ktor on the system network stack, one request without redirects. The assistant calls this from a
 * background thread, so waiting for the answer here does not block the screen.
 */
private object KtorWebTransport : WebAccess.Transport {
    private val client by lazy {
        HttpClient(Darwin) {
            followRedirects = false
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = 8_000
                requestTimeoutMillis = 15_000
            }
        }
    }

    override fun get(url: String, accept: String, agent: String, maxBytes: Int): WebAccess.Response = runBlocking {
        val response =
            client.get(url) {
                header(HttpHeaders.UserAgent, agent)
                header(HttpHeaders.Accept, accept)
            }
        val code = response.status.value
        if (code in 300..399) {
            return@runBlocking WebAccess.Response(code, response.headers[HttpHeaders.Location], ByteArray(0))
        }
        if (code !in 200..299) return@runBlocking WebAccess.Response(code, null, ByteArray(0))
        WebAccess.Response(
            code,
            null,
            response.bodyAsBytes().let { if (it.size > maxBytes) it.copyOf(maxBytes) else it },
        )
    }
}
