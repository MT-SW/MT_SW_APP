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
package org.meshtastic.core.common.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreCrypto.CCCryptorCreateWithMode
import platform.CoreCrypto.CCCryptorRefVar
import platform.CoreCrypto.CCCryptorRelease
import platform.CoreCrypto.CCCryptorUpdate
import platform.CoreCrypto.ccNoPadding
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCModeCTR
import platform.CoreCrypto.kCCModeOptionCTR_BE

/** AES in counter mode with the system crypto library (the same operation both encrypts and decrypts). */
@OptIn(ExperimentalForeignApi::class)
actual fun aesCtrTransform(key: ByteArray, counterBlock: ByteArray, data: ByteArray): ByteArray {
    if (data.isEmpty()) return ByteArray(0)
    val out = ByteArray(data.size)
    memScoped {
        val cryptor = alloc<CCCryptorRefVar>()
        val created = key.usePinned { k ->
            counterBlock.usePinned { iv ->
                CCCryptorCreateWithMode(
                    kCCEncrypt,
                    kCCModeCTR,
                    kCCAlgorithmAES,
                    ccNoPadding,
                    iv.addressOf(0),
                    k.addressOf(0),
                    key.size.convert(),
                    null,
                    0u,
                    0,
                    kCCModeOptionCTR_BE,
                    cryptor.ptr,
                )
            }
        }
        check(created == 0) { "AES-CTR could not start: $created" }
        val moved = alloc<ULongVar>()
        val status = data.usePinned { input ->
            out.usePinned { output ->
                CCCryptorUpdate(
                    cryptor.value,
                    input.addressOf(0),
                    data.size.convert(),
                    output.addressOf(0),
                    out.size.convert(),
                    moved.ptr,
                )
            }
        }
        CCCryptorRelease(cryptor.value)
        check(status == 0) { "AES-CTR failed: $status" }
    }
    return out
}
