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
package org.meshtastic.ios.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.meshtastic.core.ble.BleConnectionFactory
import org.meshtastic.core.ble.BleScanner
import org.meshtastic.core.ble.BluetoothRepository
import org.meshtastic.core.data.datasource.BundledAssetReader
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.network.HttpClientDefaults
import org.meshtastic.core.network.configureDefaultRetry
import org.meshtastic.core.network.service.ApiService
import org.meshtastic.core.network.service.ApiServiceImpl
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.MeshNotificationManager
import org.meshtastic.core.repository.MessageQueue
import org.meshtastic.core.repository.NeighborInfoResponseProvider
import org.meshtastic.core.repository.NotificationManager
import org.meshtastic.core.repository.NotificationPrefs
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.RadioController
import org.meshtastic.core.repository.RadioTransportFactory
import org.meshtastic.core.repository.ServiceRepository
import org.meshtastic.core.repository.ServiceStateWriter
import org.meshtastic.core.repository.TracerouteResponseProvider
import org.meshtastic.core.service.ServiceRepositoryImpl
import org.meshtastic.ios.notification.IosMeshNotificationManager
import org.meshtastic.ios.notification.IosNotificationManager
import org.meshtastic.ios.radio.IosMessageQueue
import org.meshtastic.ios.radio.IosRadioTransportFactory

/** Radio, network, notifications and message queue for iPhone. */
@Module
class IosRuntimeModule {
    @Single(
        binds =
        [
            ServiceRepository::class,
            ConnectionStateProvider::class,
            TracerouteResponseProvider::class,
            NeighborInfoResponseProvider::class,
            ServiceStateWriter::class,
        ],
    )
    fun serviceRepository(): ServiceRepository = ServiceRepositoryImpl()

    @Single
    fun radioTransportFactory(
        dispatchers: CoroutineDispatchers,
        scanner: BleScanner,
        bluetoothRepository: BluetoothRepository,
        connectionFactory: BleConnectionFactory,
    ): RadioTransportFactory = IosRadioTransportFactory(
        dispatchers = dispatchers,
        scanner = scanner,
        bluetoothRepository = bluetoothRepository,
        connectionFactory = connectionFactory,
    )

    @Single fun notificationManager(prefs: NotificationPrefs): NotificationManager = IosNotificationManager(prefs)

    @Single
    fun meshNotificationManager(notificationManager: NotificationManager): MeshNotificationManager =
        IosMeshNotificationManager(notificationManager)

    @Single
    fun messageQueue(
        packetRepository: PacketRepository,
        radioController: RadioController,
        dispatchers: CoroutineDispatchers,
    ): MessageQueue = IosMessageQueue(
        packetRepository = packetRepository,
        radioController = radioController,
        dispatchers = dispatchers,
    )

    @Single fun apiService(httpClient: HttpClient): ApiService = ApiServiceImpl(httpClient)

    @Single
    fun httpClient(json: Json): HttpClient = HttpClient(Darwin) {
        install(ContentNegotiation) { json(json) }
        install(DefaultRequest) {
            url(HttpClientDefaults.API_BASE_URL)
            header(HttpHeaders.UserAgent, "MT_SW-iOS")
        }
        install(HttpTimeout) {
            requestTimeoutMillis = HttpClientDefaults.REQUEST_TIMEOUT_MS
            connectTimeoutMillis = HttpClientDefaults.TIMEOUT_MS
            socketTimeoutMillis = HttpClientDefaults.TIMEOUT_MS
        }
        install(HttpRequestRetry) { configureDefaultRetry() }
    }

    @Single fun bundledAssetReader(): BundledAssetReader = BundledAssetReader { null }
}
