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
@file:Suppress("TooGenericExceptionCaught")
@file:OptIn(ExperimentalForeignApi::class)

package org.meshtastic.core.takserver

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.meshtastic.core.repository.TakCertificateMaterial
import org.meshtastic.core.repository.TakCertificateStore
import platform.Security.SecCertificateRef
import platform.Security.SecIdentityRef

/**
 * Owns the per-install TAK PKI on iOS: loads it from the [TakCertificateStore], issues and saves a new set when none
 * exists or the stored one is unusable. [Loaded] holds what the listener needs: the server identity, the CA that
 * anchors client certificates and the one issued client certificate (client certificates are pinned to it).
 */
internal class TakIosPki(private val store: TakCertificateStore) {

    class Loaded(
        val material: TakCertificateMaterial,
        val identity: SecIdentityRef,
        val ca: SecCertificateRef,
        val clientCertificate: ByteArray,
    )

    private val mutex = Mutex()
    private var cached: Loaded? = null

    suspend fun getOrCreate(): Loaded = mutex.withLock {
        cached
            ?: run {
                val stored =
                    try {
                        store.load()
                    } catch (e: Exception) {
                        Logger.w(e) { "TAK: could not read stored certificates, issuing a new set" }
                        null
                    }
                val loaded =
                    stored?.let {
                        try {
                            load(it)
                        } catch (e: Exception) {
                            Logger.w(e) { "TAK: stored certificates are unusable, issuing a new set" }
                            null
                        }
                    }
                (loaded ?: issue()).also { cached = it }
            }
    }

    suspend fun regenerate(): Loaded = mutex.withLock { issue().also { cached = it } }

    private fun issue(): Loaded {
        Logger.i { "TAK: generating per-install server, client and CA certificates" }
        val material = TakIosCertificateGenerator.generate()
        val loaded = load(material)
        store.save(material)
        return loaded
    }

    private fun load(material: TakCertificateMaterial): Loaded {
        val server = TakApple.importP12(material.serverP12, material.password)
        val identity = server.identity ?: error("TAK server.p12 holds no identity")
        val ca = server.chain.lastOrNull() ?: error("TAK server.p12 holds no CA certificate")
        val client = TakApple.importP12(material.clientP12, material.password)
        val clientLeaf = client.chain.firstOrNull() ?: error("TAK client.p12 holds no certificate")
        return Loaded(material, identity, ca, TakApple.certificateBytes(clientLeaf))
    }
}
