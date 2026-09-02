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
package org.meshtastic.core.domain.usecase.settings

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.koin.core.annotation.Single
import org.meshtastic.core.model.nodeColorsFromNum
import org.meshtastic.core.model.util.crc32
import org.meshtastic.core.model.util.platformRandomBytes
import org.meshtastic.core.model.util.x25519PublicKey

/** Result of a successful grind: the private key found, and the actual node color it produces (may differ slightly
 * from the requested target — up to [tolerance] per channel). */
data class VanityKeyResult(val privateKey: ByteString, val nodeColor: Int)

/**
 * Grinds random X25519 private keys until the resulting node color is within [tolerance] per RGB channel of
 * [targetColor], or [timeoutMillis] elapses.
 *
 * "Node color" means what [nodeColorsFromNum] would paint the node once connected: the low 24 bits of
 * `crc32(public_key)`, matching both the colleague's mvgrind tool and firmware v2.8+'s nodeNum derivation. Each
 * candidate is an independent random key — a plain parallel random search, not mvgrind's optimized incremental
 * point-addition grinder — so a tight tolerance (or an exact match) can take a while; a tolerance of a few units
 * per channel keeps it fast, the same trade `--tol` makes in mvgrind itself.
 */
@Single
open class GenerateVanityKeyUseCase {

    open suspend operator fun invoke(
        targetColor: Int,
        tolerance: Int,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    ): VanityKeyResult? = withTimeoutOrNull(timeoutMillis) {
        val found = CompletableDeferred<VanityKeyResult>()
        coroutineScope {
            repeat(WORKER_COUNT) {
                launch(Dispatchers.Default) {
                    while (isActive && !found.isCompleted) {
                        val candidate = randomClampedPrivateKey()
                        val publicKey = x25519PublicKey(candidate)
                        val nodeNum = crc32(publicKey).toInt()
                        val (_, background) = nodeColorsFromNum(nodeNum)
                        if (colorWithinTolerance(background, targetColor, tolerance)) {
                            found.complete(VanityKeyResult(candidate.toByteString(), background))
                        }
                    }
                }
            }
            found.await()
        }
    }

    private fun randomClampedPrivateKey(): ByteArray {
        val key = platformRandomBytes(PRIVATE_KEY_SIZE)
        // Same RFC 7748 clamp as PrivateKeyRegenerateDialog in SecurityConfigScreen.kt.
        key[0] = (key[0].toInt() and 0xF8).toByte()
        key[31] = ((key[31].toInt() and 0x7F) or 0x40).toByte()
        return key
    }

    private fun colorWithinTolerance(a: Int, b: Int, tolerance: Int): Boolean =
        CHANNEL_SHIFTS.all { shift ->
            val ca = (a shr shift) and 0xFF
            val cb = (b shr shift) and 0xFF
            kotlin.math.abs(ca - cb) <= tolerance
        }

    companion object {
        private const val PRIVATE_KEY_SIZE = 32
        private const val WORKER_COUNT = 4
        private const val DEFAULT_TIMEOUT_MILLIS = 20_000L
        private val CHANNEL_SHIFTS = intArrayOf(16, 8, 0)
    }
}