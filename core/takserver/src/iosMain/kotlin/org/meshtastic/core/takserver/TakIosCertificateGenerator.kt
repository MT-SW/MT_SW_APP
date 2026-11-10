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

package org.meshtastic.core.takserver

import org.meshtastic.core.repository.TakCertificateMaterial
import kotlin.time.Clock

/**
 * Issues the same per-install PKI as the Android / computer generator (private CA, server identity for 127.0.0.1, one
 * client identity), written with Apple's Security framework for the RSA keys and signatures and with [Der] /
 * [TakPkcs12] for the certificates and legacy PKCS#12 containers. The CA key stays on the stack and is dropped.
 */
internal object TakIosCertificateGenerator {
    private const val PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789"
    private const val OID_SHA256_RSA = "1.2.840.113549.1.1.11"
    private const val OID_RSA = "1.2.840.113549.1.1.1"
    private const val SECONDS_PER_DAY = 86_400L

    private class Issued(val key: TakRsaKey, val der: ByteArray, val ski: ByteArray)

    fun generate(): TakCertificateMaterial {
        val installId = TakApple.randomBytes(4).joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
        val password =
            TakApple.randomBytes(TAK_CERT_PASSWORD_LENGTH)
                .map { PASSWORD_ALPHABET[(it.toInt() and 0xFF) % PASSWORD_ALPHABET.length] }
                .joinToString("")

        val caName = name("CN" to "Meshtastic TAK CA $installId", "O" to "Meshtastic")
        val ca =
            issue(caName, caName, null) { ski, _ ->
                listOf(
                    extension("2.5.29.19", true, Der.seq(Der.bool(true), Der.int(0))),
                    extension("2.5.29.15", true, keyUsage(0x06, 1)),
                    extension("2.5.29.14", false, Der.octet(ski)),
                )
            }
        val server =
            issue(caName, name("CN" to "localhost", "OU" to "TAK Server", "O" to "Meshtastic"), ca) { ski, caSki ->
                listOf(
                    extension("2.5.29.19", true, Der.seq()),
                    extension("2.5.29.15", true, keyUsage(0xA0, 5)),
                    extension("2.5.29.37", false, Der.seq(Der.oid("1.3.6.1.5.5.7.3.1"))),
                    extension(
                        "2.5.29.17",
                        false,
                        Der.seq(
                            Der.implicit(2, "localhost".encodeToByteArray()),
                            Der.implicit(7, byteArrayOf(127, 0, 0, 1)),
                        ),
                    ),
                    extension("2.5.29.14", false, Der.octet(ski)),
                    extension("2.5.29.35", false, Der.seq(Der.implicit(0, caSki!!))),
                )
            }
        val client =
            issue(
                caName,
                name("CN" to "Meshtastic TAK Client $installId", "OU" to "TAK Client", "O" to "Meshtastic"),
                ca,
            ) { ski, caSki ->
                listOf(
                    extension("2.5.29.19", true, Der.seq()),
                    extension("2.5.29.15", true, keyUsage(0x80, 7)),
                    extension("2.5.29.37", false, Der.seq(Der.oid("1.3.6.1.5.5.7.3.2"))),
                    extension("2.5.29.14", false, Der.octet(ski)),
                    extension("2.5.29.35", false, Der.seq(Der.implicit(0, caSki!!))),
                )
            }

        val caEntry = TakPkcs12.Cert(ca.der, "meshtastic-tak-ca")
        return TakCertificateMaterial(
            serverP12 =
            TakPkcs12.build(
                password,
                listOf(TakPkcs12.Cert(server.der, "meshtastic-tak-server", server.ski), caEntry),
                TakPkcs12.Key(server.key.privatePkcs1, "meshtastic-tak-server", server.ski),
            ),
            clientP12 =
            TakPkcs12.build(
                password,
                listOf(TakPkcs12.Cert(client.der, "meshtastic-tak-client", client.ski), caEntry),
                TakPkcs12.Key(client.key.privatePkcs1, "meshtastic-tak-client", client.ski),
            ),
            truststoreP12 = TakPkcs12.build(password, listOf(caEntry), null),
            password = password,
        )
    }

    private fun issue(
        issuerName: ByteArray,
        subjectName: ByteArray,
        issuer: Issued?,
        extensions: (ski: ByteArray, caSki: ByteArray?) -> List<ByteArray>,
    ): Issued {
        val key = TakApple.newRsaKey(TAK_CERT_KEY_BITS)
        val ski = TakHash.sha1(key.publicPkcs1)
        val spki = Der.seq(Der.seq(Der.oid(OID_RSA), Der.nullValue()), Der.bitString(key.publicPkcs1))
        val now = Clock.System.now().epochSeconds
        val notBefore = now - SECONDS_PER_DAY
        val notAfter = now + TAK_CERT_VALIDITY_DAYS * SECONDS_PER_DAY
        val algorithm = Der.seq(Der.oid(OID_SHA256_RSA), Der.nullValue())
        val tbs =
            Der.seq(
                Der.explicit(0, Der.int(2)),
                Der.unsigned(TakApple.randomBytes(16).also { it[0] = (it[0].toInt() and 0x7F).toByte() }),
                algorithm,
                issuerName,
                Der.seq(Der.time(notBefore), Der.time(notAfter)),
                subjectName,
                spki,
                Der.explicit(3, Der.seq(*extensions(ski, issuer?.ski).toTypedArray())),
            )
        val signer = issuer?.key ?: key
        val signature = TakApple.sign(signer, tbs)
        return Issued(key, Der.seq(tbs, algorithm, Der.bitString(signature)), ski)
    }

    private fun name(vararg parts: Pair<String, String>): ByteArray {
        val oids = mapOf("CN" to "2.5.4.3", "OU" to "2.5.4.11", "O" to "2.5.4.10")
        return Der.seq(*parts.map { (k, v) -> Der.set(Der.seq(Der.oid(oids.getValue(k)), Der.utf8(v))) }.toTypedArray())
    }

    private fun extension(oid: String, critical: Boolean, value: ByteArray): ByteArray = if (critical) {
        Der.seq(Der.oid(oid), Der.bool(true), Der.octet(value))
    } else {
        Der.seq(Der.oid(oid), Der.octet(value))
    }

    private fun keyUsage(bits: Int, unused: Int): ByteArray = Der.bitString(byteArrayOf(bits.toByte()), unused)
}
