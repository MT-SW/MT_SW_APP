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

/**
 * Meshtastic channel-packet decryption (AES-CTR), shared by the Radio and MQTT Sniffer screens so both decode encrypted
 * traffic identically whenever the app already holds the channel's PSK.
 *
 * Meshtastic encrypts a packet's `Data` submessage with AES-CTR, keyed by the channel's PSK, using a 16-byte counter
 * block built from the packet id and sender node number (see [buildNonce]). This mirrors firmware's own CryptoEngine so
 * the app can decrypt anything it already has the key for -- exactly the same channels firmware itself would decrypt,
 * no more.
 *
 * Callers must pass an already-*expanded* key -- [org.meshtastic.core.model.Channel.psk], not the raw
 * `ChannelSettings.psk` bytes -- so Meshtastic's single-byte "default key" shorthand is resolved the same
 * already-verified way the rest of the app resolves it, rather than re-guessed here.
 */
object ChannelCrypto {

    /** AES key sizes this decryptor supports. */
    private val SUPPORTED_KEY_SIZES = setOf(16, 32)

    fun isSupportedKey(psk: ByteArray): Boolean = psk.size in SUPPORTED_KEY_SIZES

    /**
     * Builds the 16-byte AES-CTR initial counter block Meshtastic uses for a packet: little-endian [packetId] in bytes
     * 0-3, zero in bytes 4-7, little-endian [fromNode] in bytes 8-11, zero in bytes 12-15.
     */
    fun buildNonce(packetId: Int, fromNode: Int): ByteArray {
        val nonce = ByteArray(16)
        writeLeInt(nonce, 0, packetId)
        writeLeInt(nonce, 8, fromNode)
        return nonce
    }

    private fun writeLeInt(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        buffer[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        buffer[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    /**
     * Decrypts [data] with [key] (AES-CTR is its own inverse, so this equally "encrypts"). Returns null when [key]
     * isn't a supported length, this platform has no AES-CTR implementation wired up, or the transform fails.
     */
    fun decrypt(key: ByteArray, packetId: Int, fromNode: Int, data: ByteArray): ByteArray? {
        if (!isSupportedKey(key)) return null
        return runCatching { aesCtrTransform(key, buildNonce(packetId, fromNode), data) }.getOrNull()
    }
}

/** Platform AES-CTR transform (NoPadding). Throws if unavailable/unsupported on this platform. */
expect fun aesCtrTransform(key: ByteArray, counterBlock: ByteArray, data: ByteArray): ByteArray
