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

/** No-op stubs for core:model on iOS. */
actual fun getShortDateTime(time: Long): String = ""

// Deliberately not a no-op: this backs channel PSK and private-key generation, so an all-zeros stub must not be
// allowed to ship silently. Fail loudly until it is wired to SecRandomCopyBytes.
actual fun platformRandomBytes(size: Int): ByteArray = error("platformRandomBytes is not implemented on iOS")

// Same reasoning as platformRandomBytes above: silently returning garbage would let the vanity-color grinder run
// and hand back a key/color pair the device would never actually derive. Fail loudly until a Kotlin/Native X25519
// (BouncyCastle has no iOS target) is wired up here.
actual fun x25519PublicKey(privateKey: ByteArray): ByteArray = error("x25519PublicKey is not implemented on iOS")
