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
@file:Suppress("MagicNumber")

package org.meshtastic.core.common.crypto

/**
 * Password encryption of the files of the message archive, in the format of `openssl enc -aes-256-ctr -pbkdf2`: the
 * bytes `Salted__`, an 8-byte salt, then the data encrypted with AES-256 in counter mode. Key and counter come from one
 * PBKDF2-HMAC-SHA256 run over the password and the salt ([ITERATIONS] rounds, 32 bytes of key then 16 of counter).
 *
 * Keeping to that format means a file can be opened on any computer with a standard tool, without this app: `openssl
 * enc -d -aes-256-ctr -pbkdf2 -md sha256 -iter 100000 -in file.enc -out file.txt`.
 *
 * Counter mode hides the content but does not detect a wrong password or a changed file: a wrong password gives
 * unreadable bytes.
 */
object ArchiveCrypto {
    const val ITERATIONS = 100_000
    private const val SALT_SIZE = 8
    private const val KEY_SIZE = 32
    private const val COUNTER_SIZE = 16
    private val MAGIC = "Salted__".encodeToByteArray()

    /** Encrypts [data] with [password], using a fresh random salt. */
    fun encrypt(password: String, data: ByteArray): ByteArray {
        val salt = secureRandomBytes(SALT_SIZE)
        return MAGIC + salt + transform(password, salt, data)
    }

    /** Reverses [encrypt]; null when [data] does not start like an encrypted file. */
    fun decrypt(password: String, data: ByteArray): ByteArray? {
        val header = MAGIC.size + SALT_SIZE
        if (data.size < header || !data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) return null
        val salt = data.copyOfRange(MAGIC.size, header)
        return transform(password, salt, data.copyOfRange(header, data.size))
    }

    private fun transform(password: String, salt: ByteArray, data: ByteArray): ByteArray {
        val derived = pbkdf2Sha256(password.encodeToByteArray(), salt, ITERATIONS, KEY_SIZE + COUNTER_SIZE)
        val key = derived.copyOfRange(0, KEY_SIZE)
        val counter = derived.copyOfRange(KEY_SIZE, KEY_SIZE + COUNTER_SIZE)
        return aesCtrTransform(key, counter, data)
    }
}

/** PBKDF2 with HMAC-SHA256: [keyBytes] bytes derived from [password] and [salt]. */
expect fun pbkdf2Sha256(password: ByteArray, salt: ByteArray, iterations: Int, keyBytes: Int): ByteArray

/** [size] bytes from the system's secure random source. */
expect fun secureRandomBytes(size: Int): ByteArray
