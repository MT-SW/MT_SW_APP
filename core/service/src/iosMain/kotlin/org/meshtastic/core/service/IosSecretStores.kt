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
@file:Suppress("MagicNumber", "ReturnCount")

package org.meshtastic.core.service

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Single
import org.meshtastic.core.database.iosDataDir
import org.meshtastic.core.repository.LockdownPassphraseStore
import org.meshtastic.core.repository.SecurityKeyBackupStore
import org.meshtastic.core.repository.StoredPassphrase
import org.meshtastic.core.repository.StoredSecurityKeys
import org.meshtastic.core.repository.TakCertificateMaterial
import org.meshtastic.core.repository.TakCertificateStore

private fun secretDir(name: String): Path =
    (iosDataDir() + "/" + name).toPath().also { FileSystem.SYSTEM.createDirectories(it) }

private fun readText(path: Path): String? =
    if (FileSystem.SYSTEM.exists(path)) FileSystem.SYSTEM.read(path) { readUtf8() } else null

private fun writeText(path: Path, text: String) = FileSystem.SYSTEM.write(path) { writeUtf8(text) }

/** Stored in the app's private folder, which iOS keeps encrypted while the phone is locked. */
@Single(binds = [LockdownPassphraseStore::class])
class IosLockdownPassphraseStore : LockdownPassphraseStore {
    private val dir by lazy { secretDir("lockdown") }

    private fun file(address: String) = dir / (address.replace(Regex("[^a-zA-Z0-9_-]"), "_") + ".txt")

    override fun getPassphrase(deviceAddress: String): StoredPassphrase? {
        val parts = readText(file(deviceAddress))?.split("\n", limit = 4) ?: return null
        if (parts.size < 4) return null
        val boots = parts[0].toIntOrNull() ?: return null
        val hours = parts[1].toIntOrNull() ?: return null
        val maxSession = parts[2].toIntOrNull() ?: return null
        if (parts[3].isEmpty()) return null
        return StoredPassphrase(parts[3], boots, hours, maxSession)
    }

    override fun savePassphrase(
        deviceAddress: String,
        passphrase: String,
        boots: Int,
        hours: Int,
        maxSessionSeconds: Int,
    ) {
        writeText(file(deviceAddress), "$boots\n$hours\n$maxSessionSeconds\n$passphrase")
    }

    override fun clearPassphrase(deviceAddress: String) {
        FileSystem.SYSTEM.delete(file(deviceAddress), mustExist = false)
    }
}

@Single(binds = [SecurityKeyBackupStore::class])
class IosSecurityKeyBackupStore : SecurityKeyBackupStore {
    private val dir by lazy { secretDir("security_keys") }

    private fun file(nodeNum: Int) = dir / "$nodeNum.txt"

    private fun parse(text: String): StoredSecurityKeys? {
        val p = text.split("\n", limit = 4)
        if (p.size < 3) return null
        val time = p[2].toLongOrNull() ?: return null
        return StoredSecurityKeys(p[0], p[1], time, p.getOrNull(3)?.takeIf { it.isNotEmpty() })
    }

    override fun get(nodeNum: Int): StoredSecurityKeys? = readText(file(nodeNum))?.let(::parse)

    override fun all(): Map<Int, StoredSecurityKeys> = FileSystem.SYSTEM.list(dir)
        .mapNotNull { path ->
            val num = path.name.removeSuffix(".txt").toIntOrNull() ?: return@mapNotNull null
            val keys = readText(path)?.let(::parse) ?: return@mapNotNull null
            num to keys
        }
        .toMap()

    override fun save(
        nodeNum: Int,
        publicKeyBase64: String,
        privateKeyBase64: String,
        timestamp: Long,
        longName: String?,
    ) {
        writeText(
            file(nodeNum),
            "$publicKeyBase64\n$privateKeyBase64\n$timestamp\n${longName?.replace("\n", " ").orEmpty()}",
        )
    }

    override fun delete(nodeNum: Int) {
        FileSystem.SYSTEM.delete(file(nodeNum), mustExist = false)
    }
}

@Single(binds = [TakCertificateStore::class])
class IosTakCertificateStore : TakCertificateStore {
    private val dir by lazy { secretDir("tak") }

    override fun load(): TakCertificateMaterial? {
        val fs = FileSystem.SYSTEM
        val password = readText(dir / "password.txt")?.takeIf { it.isNotEmpty() } ?: return null
        fun bytes(name: String) = (dir / name).takeIf { fs.exists(it) }?.let { fs.read(it) { readByteArray() } }
        return TakCertificateMaterial(
            serverP12 = bytes("server.p12") ?: return null,
            clientP12 = bytes("client.p12") ?: return null,
            truststoreP12 = bytes("truststore.p12") ?: return null,
            password = password,
        )
    }

    override fun save(material: TakCertificateMaterial) {
        FileSystem.SYSTEM.write(dir / "server.p12") { write(material.serverP12) }
        FileSystem.SYSTEM.write(dir / "client.p12") { write(material.clientP12) }
        FileSystem.SYSTEM.write(dir / "truststore.p12") { write(material.truststoreP12) }
        writeText(dir / "password.txt", material.password)
    }

    override fun clear() {
        listOf("server.p12", "client.p12", "truststore.p12", "password.txt").forEach {
            FileSystem.SYSTEM.delete(dir / it, mustExist = false)
        }
    }
}
