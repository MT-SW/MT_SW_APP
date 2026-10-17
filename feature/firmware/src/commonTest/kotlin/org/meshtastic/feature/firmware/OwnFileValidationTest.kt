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
package org.meshtastic.feature.firmware

import org.meshtastic.core.model.DeviceHardware
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OwnFileValidationTest {
    private val unknown = DeviceHardware(displayName = "Unknown device", hwModel = 255)
    private val nrf = DeviceHardware(architecture = "nrf52840", platformioTarget = "rak4631")

    @Test
    fun `unidentified device is recognised by its blank architecture`() {
        assertTrue(unknown.isUnidentified)
        assertEquals(false, nrf.isUnidentified)
    }

    @Test
    fun `own file is accepted under any name when only the type must fit`() {
        val ble = FirmwareUpdateMethod.Ble
        assertEquals(
            LocalFirmwareFileValidation.Valid,
            validateLocalFirmwareFileName("my-build.zip", nrf, ble, lenient = true),
        )
        assertEquals(
            LocalFirmwareFileValidation.Valid,
            validateLocalFirmwareFileName("my-build.zip", unknown, ble, lenient = true),
        )
        assertEquals(
            LocalFirmwareFileValidation.Valid,
            validateLocalFirmwareFileName("my-build.bin", unknown, ble, lenient = true),
        )
    }

    @Test
    fun `own file of the wrong type is still refused`() {
        assertEquals(
            LocalFirmwareFileValidation.Invalid(LocalFirmwareFileValidationReason.RequiresOtaZip),
            validateLocalFirmwareFileName("my-build.bin", nrf, FirmwareUpdateMethod.Ble, lenient = true),
        )
    }

    @Test
    fun `strict check still demands the model name`() {
        assertEquals(
            LocalFirmwareFileValidation.Invalid(LocalFirmwareFileValidationReason.TargetMismatch),
            validateLocalFirmwareFileName("my-build-ota.zip", nrf, FirmwareUpdateMethod.Ble),
        )
    }
}
