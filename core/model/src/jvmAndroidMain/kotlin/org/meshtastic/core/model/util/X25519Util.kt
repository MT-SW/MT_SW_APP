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
package org.meshtastic.core.model.util

import org.bouncycastle.math.ec.rfc7748.X25519

actual fun x25519PublicKey(privateKey: ByteArray): ByteArray {
    require(privateKey.size == 32) { "X25519 private key must be 32 bytes, got ${privateKey.size}" }
    val publicKey = ByteArray(32)
    X25519.scalarMultBase(privateKey, 0, publicKey, 0)
    return publicKey
}
