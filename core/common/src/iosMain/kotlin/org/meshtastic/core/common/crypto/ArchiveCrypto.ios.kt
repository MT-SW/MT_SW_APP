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
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.UByteVar
import platform.CoreCrypto.CCKeyDerivationPBKDF
import platform.CoreCrypto.kCCPBKDF2
import platform.CoreCrypto.kCCPRFHmacAlgSHA256
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault

/** PBKDF2 from the system crypto library. */
@OptIn(ExperimentalForeignApi::class)
actual fun pbkdf2Sha256(password: ByteArray, salt: ByteArray, iterations: Int, keyBytes: Int): ByteArray {
    val out = ByteArray(keyBytes)
    // An empty array has no first element to point at, so a one-byte stand-in is passed with a length of zero.
    val pass = if (password.isEmpty()) ByteArray(1) else password
    val status =
        pass.usePinned { p ->
            salt.usePinned { s ->
                out.usePinned { o ->
                    CCKeyDerivationPBKDF(
                        kCCPBKDF2,
                        p.addressOf(0),
                        password.size.convert(),
                        s.addressOf(0).reinterpret<UByteVar>(),
                        salt.size.convert(),
                        kCCPRFHmacAlgSHA256,
                        iterations.convert(),
                        o.addressOf(0).reinterpret<UByteVar>(),
                        keyBytes.convert(),
                    )
                }
            }
        }
    check(status == 0) { "PBKDF2 failed: $status" }
    return out
}

@OptIn(ExperimentalForeignApi::class)
actual fun secureRandomBytes(size: Int): ByteArray {
    val out = ByteArray(size)
    val status = out.usePinned { SecRandomCopyBytes(kSecRandomDefault, size.convert(), it.addressOf(0)) }
    check(status == 0) { "Secure random failed: $status" }
    return out
}
