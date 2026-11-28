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
package org.meshtastic.core.ble

import com.juul.kable.Peripheral
import com.juul.kable.PeripheralBuilder
import com.juul.kable.ScannerBuilder
import com.juul.kable.toIdentifier

internal actual fun ScannerBuilder.platformScanConfig() {
    // No-op: the scan buffer setting is Android-only.
}

// Kable's btleplug backend evaluates scan filters with a hardcoded `address = null`, so an address filter matches
// nothing and the scan yields no advertisements at all.
internal actual val supportsNativeAddressScanFilter: Boolean = false

// Kable's JVM target scans through the Rust btleplug library. On Windows, btleplug's WinRT-backed scanner does not
// implement ScanFilter at all (github.com/deviceplug/btleplug issue #249): a service-UUID filter is accepted by the
// API but silently produces zero results instead of narrowing the scan, so Meshtastic devices were never discovered
// on Windows desktop -- the scan callback fires ("Starting scan") but the native layer discards every advertisement
// before Kable's Flow ever sees it. KableBleScanner.advertisements skips requesting this filter here and
// KableBleScanner.scan narrows client-side using Advertisement.uuids instead.
internal actual val supportsNativeServiceScanFilter: Boolean = false

// btleplug's CoreBluetooth backend (macOS) identifies a peripheral by an Apple UUID; its BlueZ backend (Linux) by a
// path that carries the MAC.
internal actual val bleAddressIsHardware: Boolean =
    !System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)

internal actual fun createPeripheral(address: String, builderAction: PeripheralBuilder.() -> Unit): Peripheral =
    com.juul.kable.Peripheral(address.toIdentifier(), builderAction)

// btleplug reports the negotiated MTU only through Kable's suspend maximumWriteValueLengthForType.
internal actual fun Peripheral.negotiatedMaxWriteLength(atConnect: Int?): Int? = atConnect
