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
package org.meshtastic.core.takserver

/** Writes PKCS#12 containers in the legacy encoding (pbeWithSHAAnd3-KeyTripleDES-CBC, SHA-1 MAC) that ATAK reads. */
internal object TakPkcs12 {
    private const val ITERATIONS = 2048
    private const val OID_PBE_3DES = "1.2.840.113549.1.12.1.3"
    private const val OID_DATA = "1.2.840.113549.1.7.1"
    private const val OID_ENCRYPTED_DATA = "1.2.840.113549.1.7.6"
    private const val OID_CERT_BAG = "1.2.840.113549.1.12.10.1.3"
    private const val OID_SHROUDED_KEY_BAG = "1.2.840.113549.1.12.10.1.2"
    private const val OID_X509_CERT = "1.2.840.113549.1.9.22.1"
    private const val OID_FRIENDLY_NAME = "1.2.840.113549.1.9.20"
    private const val OID_LOCAL_KEY_ID = "1.2.840.113549.1.9.21"
    private const val OID_RSA = "1.2.840.113549.1.1.1"
    private const val OID_SHA1 = "1.3.14.3.2.26"

    class Cert(val der: ByteArray, val name: String, val keyId: ByteArray? = null)

    class Key(val pkcs1: ByteArray, val name: String, val keyId: ByteArray)

    /** A container with [certs] and, when given, one private [key] (whose certificate carries the same key id). */
    fun build(password: String, certs: List<Cert>, key: Key?): ByteArray {
        val pw = TakHash.bmpPassword(password)
        val contents = ArrayList<ByteArray>()

        val certBags = certs.map { certBag(it) }
        val certSafe = Der.seq(*certBags.toTypedArray())
        val certSalt = TakApple.randomBytes(8)
        contents +=
            Der.seq(
                Der.oid(OID_ENCRYPTED_DATA),
                Der.explicit(
                    0,
                    Der.seq(
                        Der.int(0),
                        Der.seq(
                            Der.oid(OID_DATA),
                            Der.seq(Der.oid(OID_PBE_3DES), params(certSalt)),
                            Der.implicit(0, encrypt(certSafe, pw, certSalt)),
                        ),
                    ),
                ),
            )

        if (key != null) {
            val pkcs8 =
                Der.seq(Der.int(0), Der.seq(Der.oid(OID_RSA), Der.nullValue()), Der.octet(key.pkcs1))
            val keySalt = TakApple.randomBytes(8)
            val keyBag =
                Der.seq(
                    Der.oid(OID_SHROUDED_KEY_BAG),
                    Der.explicit(
                        0,
                        Der.seq(Der.seq(Der.oid(OID_PBE_3DES), params(keySalt)), Der.octet(encrypt(pkcs8, pw, keySalt))),
                    ),
                    attributes(key.name, key.keyId),
                )
            contents += Der.seq(Der.oid(OID_DATA), Der.explicit(0, Der.octet(Der.seq(keyBag))))
        }

        val authSafe = Der.seq(*contents.toTypedArray())
        val macSalt = TakApple.randomBytes(8)
        val mac = TakHash.hmacSha1(TakHash.pkcs12Kdf(pw, macSalt, ITERATIONS, 3, 20), authSafe)
        return Der.seq(
            Der.int(3),
            Der.seq(Der.oid(OID_DATA), Der.explicit(0, Der.octet(authSafe))),
            Der.seq(
                Der.seq(Der.seq(Der.oid(OID_SHA1), Der.nullValue()), Der.octet(mac)),
                Der.octet(macSalt),
                Der.int(ITERATIONS),
            ),
        )
    }

    private fun certBag(cert: Cert): ByteArray =
        Der.seq(
            Der.oid(OID_CERT_BAG),
            Der.explicit(0, Der.seq(Der.oid(OID_X509_CERT), Der.explicit(0, Der.octet(cert.der)))),
            attributes(cert.name, cert.keyId),
        )

    private fun attributes(name: String, keyId: ByteArray?): ByteArray {
        val friendly = Der.seq(Der.oid(OID_FRIENDLY_NAME), Der.set(Der.bmp(name)))
        return if (keyId == null) {
            Der.set(friendly)
        } else {
            Der.set(friendly, Der.seq(Der.oid(OID_LOCAL_KEY_ID), Der.set(Der.octet(keyId))))
        }
    }

    private fun params(salt: ByteArray): ByteArray = Der.seq(Der.octet(salt), Der.int(ITERATIONS))

    private fun encrypt(plain: ByteArray, pw: ByteArray, salt: ByteArray): ByteArray {
        val key = TakHash.pkcs12Kdf(pw, salt, ITERATIONS, 1, 24)
        val iv = TakHash.pkcs12Kdf(pw, salt, ITERATIONS, 2, 8)
        return TakApple.tripleDes(true, key, iv, plain)
    }
}
