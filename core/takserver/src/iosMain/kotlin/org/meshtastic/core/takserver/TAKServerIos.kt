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
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.repository.TakCertificateStore
import platform.CoreFoundation.CFArrayCreate
import platform.CoreFoundation.CFRelease
import platform.Network.nw_endpoint_create_host
import platform.Network.nw_listener_cancel
import platform.Network.nw_listener_create
import platform.Network.nw_listener_set_new_connection_handler
import platform.Network.nw_listener_set_queue
import platform.Network.nw_listener_set_state_changed_handler
import platform.Network.nw_listener_start
import platform.Network.nw_listener_state_failed
import platform.Network.nw_listener_state_ready
import platform.Network.nw_listener_t
import platform.Network.nw_parameters_create_secure_tcp
import platform.Network.nw_parameters_set_local_endpoint
import platform.Network.nw_parameters_set_reuse_local_address
import platform.Network.nw_tls_copy_sec_protocol_options
import platform.Security.SecPolicyCreateBasicX509
import platform.Security.SecTrustEvaluateWithError
import platform.Security.SecTrustGetCertificateAtIndex
import platform.Security.SecTrustSetAnchorCertificates
import platform.Security.SecTrustSetAnchorCertificatesOnly
import platform.Security.SecTrustSetPolicies
import platform.Security.sec_identity_create
import platform.Security.sec_protocol_options_set_local_identity
import platform.Security.sec_protocol_options_set_min_tls_protocol_version
import platform.Security.sec_protocol_options_set_peer_authentication_required
import platform.Security.sec_protocol_options_set_verify_block
import platform.Security.sec_trust_copy_ref
import platform.Security.sec_trust_t
import platform.Security.tls_protocol_version_TLSv12
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_create
import kotlin.concurrent.Volatile
import kotlin.random.Random

/** Pause between stopping a listener and binding the same port again (regenerating the certificates). */
private const val REBIND_DELAY_MS = 400L

/**
 * The local TAK server on iPhone and iPad, built on Apple's Network framework: a TLS listener bound to `127.0.0.1`
 * only, with mutual TLS. A client is accepted only when it presents exactly the one client certificate this install
 * issued (the same rule as the Android and computer server), so ATAK / iTAK on the same device import the exported data
 * package and connect to `127.0.0.1:8089`.
 *
 * All connection state lives on one serial dispatch queue; the state flows are the only thing read from other threads.
 */
internal class TAKServerIos(
    private val dispatchers: CoroutineDispatchers,
    certificateStore: TakCertificateStore,
    private val port: Int = DEFAULT_TAK_PORT,
) : TAKServer {
    private val pki = TakIosPki(certificateStore)
    private val queue = dispatch_queue_create("pl.sw.mt.tak", null)
    private val verifyQueue = dispatch_queue_create("pl.sw.mt.tak.verify", null)
    private val restartMutex = Mutex()
    private val connections = mutableMapOf<String, IosTakConnection>()

    private var listener: nw_listener_t = null
    private var serverScope: CoroutineScope? = null
    private var keepaliveJob: Job? = null

    @Volatile private var running = false

    override val isSupported = true

    private val _connectionCount = MutableStateFlow(0)
    override val connectionCount: StateFlow<Int> = _connectionCount.asStateFlow()

    private val _rejectedConnectionCount = MutableStateFlow(0)
    override val rejectedConnectionCount: StateFlow<Int> = _rejectedConnectionCount.asStateFlow()

    override var onMessage: ((CoTMessage, TAKClientInfo?) -> Unit)? = null
    override var onClientConnected: (() -> Unit)? = null

    @Suppress("ReturnCount")
    override suspend fun start(scope: CoroutineScope): Result<Unit> {
        if (running) return Result.success(Unit)
        return try {
            // Three RSA-2048 keys on first start: keep that off the main thread.
            val loaded = withContext(dispatchers.io) { pki.getOrCreate() }
            serverScope = scope
            val ready = CompletableDeferred<Result<Unit>>()
            val newListener = buildListener(loaded, ready)
            listener = newListener
            nw_listener_start(newListener)
            val result = ready.await()
            if (result.isFailure) {
                nw_listener_cancel(newListener)
                listener = null
                return result
            }
            running = true
            _rejectedConnectionCount.value = 0
            Logger.i { "TAK Server listening on 127.0.0.1:$port (TLS, mTLS enforced)" }
            keepaliveJob =
                scope.launch(dispatchers.io) {
                    while (isActive) {
                        delay(TAK_KEEPALIVE_INTERVAL_MS)
                        dispatch_async(queue) {
                            connections.values.filter { it.isReady }.forEach { it.sendKeepalive() }
                        }
                    }
                }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(e) { "Failed to start TAK Server on 127.0.0.1:$port" }
            running = false
            listener?.let { nw_listener_cancel(it) }
            listener = null
            Result.failure(e)
        }
    }

    private fun buildListener(loaded: TakIosPki.Loaded, ready: CompletableDeferred<Result<Unit>>): nw_listener_t {
        val parameters = nw_parameters_create_secure_tcp({ tlsOptions -> configureTls(tlsOptions, loaded) }, { _ -> })
        nw_parameters_set_local_endpoint(parameters, nw_endpoint_create_host("127.0.0.1", port.toString()))
        nw_parameters_set_reuse_local_address(parameters, true)
        val newListener = nw_listener_create(parameters) ?: error("Could not create the TAK listener")
        nw_listener_set_queue(newListener, queue)
        nw_listener_set_state_changed_handler(newListener) { state, _ ->
            when (state) {
                nw_listener_state_ready -> ready.complete(Result.success(Unit))

                nw_listener_state_failed ->
                    ready.complete(Result.failure(IllegalStateException("TAK listener failed on port $port")))

                else -> Unit
            }
        }
        nw_listener_set_new_connection_handler(newListener) { connection ->
            if (connection != null) accept(connection)
        }
        return newListener
    }

    private fun configureTls(tlsOptions: platform.Network.nw_protocol_options_t, loaded: TakIosPki.Loaded) {
        val options = nw_tls_copy_sec_protocol_options(tlsOptions)
        sec_protocol_options_set_local_identity(options, sec_identity_create(loaded.identity))
        sec_protocol_options_set_min_tls_protocol_version(options, tls_protocol_version_TLSv12)
        sec_protocol_options_set_peer_authentication_required(options, true)
        sec_protocol_options_set_verify_block(
            options,
            { _, trust, complete ->
                val accepted = verifyClient(trust, loaded)
                if (!accepted) {
                    Logger.w { "TAK server rejected a client: not the certificate issued by this install" }
                    if (running) _rejectedConnectionCount.value = _rejectedConnectionCount.value + 1
                }
                complete?.invoke(accepted)
            },
            verifyQueue,
        )
    }

    /** True when the chain leads to this install's CA and the leaf is exactly the client certificate it issued. */
    @Suppress("DEPRECATION")
    private fun verifyClient(trust: sec_trust_t, loaded: TakIosPki.Loaded): Boolean = memScoped {
        val secTrust = trust?.let { sec_trust_copy_ref(it) } ?: return@memScoped false
        val anchors = CFArrayCreate(null, allocArrayOf(loaded.ca).reinterpret(), 1, null)
        SecTrustSetAnchorCertificates(secTrust, anchors)
        SecTrustSetAnchorCertificatesOnly(secTrust, true)
        SecTrustSetPolicies(secTrust, SecPolicyCreateBasicX509())
        val chainOk = SecTrustEvaluateWithError(secTrust, null)
        val leaf = SecTrustGetCertificateAtIndex(secTrust, 0)
        val pinned = leaf != null && TakApple.certificateBytes(leaf).contentEquals(loaded.clientCertificate)
        CFRelease(anchors)
        CFRelease(secTrust)
        chainOk && pinned
    }

    private fun accept(connection: platform.Network.nw_connection_t) {
        val id = Random.nextInt().toString(TAK_HEX_RADIX)
        val clientInfo = TAKClientInfo(id = id, endpoint = "127.0.0.1")
        Logger.i { "TAK client connecting: id=$id" }
        val client = IosTakConnection(connection, clientInfo, queue) { event -> handleEvent(id, event) }
        connections[id] = client
        client.start()
    }

    private fun handleEvent(id: String, event: TAKConnectionEvent) {
        when (event) {
            is TAKConnectionEvent.Connected -> {
                _connectionCount.value = connections.values.count { it.isReady }
                // An issued client getting in means the current package is in use, so the re-import notice is stale.
                _rejectedConnectionCount.value = 0
                Logger.i { "TAK client accepted: id=$id, connection count now ${_connectionCount.value}" }
                onClientConnected?.invoke()
            }

            is TAKConnectionEvent.Message -> onMessage?.invoke(event.cotMessage, event.clientInfo)

            is TAKConnectionEvent.Disconnected,
            is TAKConnectionEvent.Error,
            -> {
                connections.remove(id)
                _connectionCount.value = connections.values.count { it.isReady }
                Logger.i { "TAK client disconnected: id=$id, connection count now ${_connectionCount.value}" }
            }

            is TAKConnectionEvent.ClientInfoUpdated -> Unit
        }
    }

    override val certificates: TakCertificateProvider =
        object : TakCertificateProvider {
            override suspend fun dataPackageCredentials(): TakDataPackageCredentials? = try {
                withContext(dispatchers.io) {
                    val material = pki.getOrCreate().material
                    TakDataPackageCredentials(
                        clientP12 = material.clientP12,
                        truststoreP12 = material.truststoreP12,
                        password = material.password,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e) { "TAK: could not load certificates for the data package" }
                null
            }

            override suspend fun regenerate(): Result<Unit> = restartMutex.withLock {
                try {
                    withContext(dispatchers.io) { pki.regenerate() }
                    val scope = serverScope
                    if (running && scope != null) {
                        stop()
                        delay(REBIND_DELAY_MS)
                        start(scope).getOrThrow()
                    }
                    Result.success(Unit)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e(e) { "TAK: certificate regeneration failed" }
                    Result.failure(e)
                }
            }
        }

    override fun stop() {
        running = false
        keepaliveJob?.cancel()
        keepaliveJob = null
        listener?.let { nw_listener_cancel(it) }
        listener = null
        serverScope = null
        dispatch_async(queue) {
            val open = connections.values.toList()
            connections.clear()
            _connectionCount.value = 0
            open.forEach { it.close() }
        }
        Logger.i { "TAK Server stopped" }
    }

    override suspend fun broadcast(cotMessage: CoTMessage) = broadcastRawXml(cotMessage.toXml())

    override suspend fun broadcastRawXml(xml: String) {
        if (_connectionCount.value == 0) return
        dispatch_async(queue) { connections.values.filter { it.isReady }.forEach { it.sendXml(xml) } }
    }

    override suspend fun hasConnections(): Boolean = _connectionCount.value > 0
}

actual fun createTAKServer(
    dispatchers: CoroutineDispatchers,
    certificateStore: TakCertificateStore,
    port: Int,
): TAKServer = TAKServerIos(dispatchers, certificateStore, port)
