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
package org.meshtastic.feature.connections.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import org.meshtastic.core.ble.BluetoothRepository
import org.meshtastic.core.ble.MeshtasticBleDevice
import org.meshtastic.core.common.database.DatabaseManager
import org.meshtastic.core.common.util.safeCatchingAll
import org.meshtastic.core.datastore.model.RecentAddress
import org.meshtastic.core.model.Node
import org.meshtastic.core.network.repository.DiscoveredService
import org.meshtastic.core.repository.NodeManager
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.resources.meshtastic
import org.meshtastic.feature.connections.BLE_DEVICE_PREFIX
import org.meshtastic.feature.connections.data.RecentAddressesSource
import org.meshtastic.feature.connections.model.DeviceListEntry
import org.meshtastic.feature.connections.model.DiscoveredDevices
import org.meshtastic.feature.connections.model.GetDiscoveredDevicesUseCase

/**
 * Platform-agnostic implementation of [GetDiscoveredDevicesUseCase].
 *
 * Intentionally NOT annotated `@Single` in common source: on Android, the richer
 * [org.meshtastic.feature.connections.domain.usecase.AndroidGetDiscoveredDevicesUseCase] is the canonical binding, and
 * a common `@Single` here would silently override it (last-write-wins), producing an empty USB list. Each non-Android
 * target registers its own `@Single` wrapper (see `JvmGetDiscoveredDevicesUseCase`).
 */
open class CommonGetDiscoveredDevicesUseCase(
    private val recentAddressesDataSource: RecentAddressesSource,
    private val nodeRepository: NodeRepository,
    private val databaseManager: DatabaseManager,
    private val nodeManager: NodeManager,
    private val usbScanner: UsbScanner? = null,
    // MT_SW: lets a previously connected BLE device (recent address) show on the Connect list right away, the way
    // Android always shows its system-bonded devices — null only in tests/targets with no BLE transport at all.
    private val bluetoothRepository: BluetoothRepository? = null,
) : GetDiscoveredDevicesUseCase {

    override fun invoke(
        showMock: Boolean,
        showReplay: Boolean,
        resolvedList: Flow<List<DiscoveredService>>,
    ): Flow<DiscoveredDevices> {
        val nodeDb = nodeRepository.nodeDBbyNum
        val usbFlow = usbScanner?.scanUsbDevices() ?: flowOf(emptyList())

        return combine(
            nodeDb,
            resolvedList,
            recentAddressesDataSource.recentAddresses,
            usbFlow,
            localNodeNums(databaseManager, nodeManager),
        ) { db, resolved, recentList, usbList, nums ->
            val defaultName = safeCatchingAll { getStringSuspend(Res.string.meshtastic) }.getOrDefault("Meshtastic")
            val processedTcp = processTcpServices(resolved, recentList, defaultName)
            val discoveredTcpAddresses = processedTcp.mapTo(mutableSetOf()) { it.fullAddress }

            val discoveredTcpForUi = matchDiscoveredTcpNodes(processedTcp, db, resolved, databaseManager, nums)
            val recentTcpForUi = buildRecentTcpEntries(recentList, discoveredTcpAddresses, db, nums)
            val recentBleForUi = buildRecentBleEntries(recentList, db, nums)

            DiscoveredDevices(
                bleDevices = recentBleForUi,
                discoveredTcpDevices = discoveredTcpForUi,
                recentTcpDevices = recentTcpForUi,
                usbDevices = usbList.withLocalNodes(db, nums),
                virtualDevices = virtualDeviceEntries(showMock, showReplay).withLocalNodes(db, nums),
            )
        }
    }

    /**
     * A previously connected BLE device (saved to [RecentAddressesSource] on connect) shown right away, before any
     * scan: the counterpart of Android's always-visible system-bonded list, for platforms (desktop, iPhone/iPad) that
     * have no such system list, or where it cannot be enumerated ahead of a scan. Deduplicated against a live scan by
     * the view model merging them, the same as Android's bonded entries.
     */
    private fun buildRecentBleEntries(
        recentList: List<RecentAddress>,
        nodeDb: Map<Int, Node>,
        nums: LocalNodeNums,
    ): List<DeviceListEntry> = recentList
        .filter { it.address.startsWith(BLE_DEVICE_PREFIX) }
        .map { recent ->
            val address = recent.address.removePrefix(BLE_DEVICE_PREFIX)
            DeviceListEntry.Ble(
                device =
                MeshtasticBleDevice(
                    address = address,
                    name = recent.name,
                    isBonded = bluetoothRepository?.isBonded(address) ?: false,
                ),
            )
        }
        .distinctBy { it.fullAddress }
        .withLocalNodes(nodeDb, nums)
        .sortedBy { it.name }
}
