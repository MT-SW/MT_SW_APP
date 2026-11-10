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
@file:OptIn(ExperimentalForeignApi::class)

@file:Suppress("MagicNumber")

package org.meshtastic.core.takserver

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreCrypto.CCCrypt
import platform.CoreFoundation.CFArrayGetCount
import platform.CoreFoundation.CFArrayGetValueAtIndex
import platform.CoreFoundation.CFArrayRef
import platform.CoreFoundation.CFArrayRefVar
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.kCFNumberSInt32Type
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Security.SecCertificateCopyData
import platform.Security.SecCertificateRef
import platform.Security.SecIdentityRef
import platform.Security.SecKeyCopyExternalRepresentation
import platform.Security.SecKeyCopyPublicKey
import platform.Security.SecKeyCreateRandomKey
import platform.Security.SecKeyCreateSignature
import platform.Security.SecKeyRef
import platform.Security.SecPKCS12Import
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeRSA
import platform.Security.kSecImportExportPassphrase
import platform.Security.kSecImportItemCertChain
import platform.Security.kSecImportItemIdentity
import platform.Security.kSecKeyAlgorithmRSASignatureMessagePKCS1v15SHA256
import platform.Security.kSecRandomDefault
import platform.posix.memcpy
import platform.posix.size_tVar

/** An RSA key pair that lives only in memory: [privateKey] signs, the two byte arrays are PKCS#1 DER. */
internal class TakRsaKey(val privateKey: SecKeyRef, val publicPkcs1: ByteArray, val privatePkcs1: ByteArray)

/** What [TakApple.importP12] returns: the identity (when the container holds a key) and its certificate chain. */
internal class TakImported(val identity: SecIdentityRef?, val chain: List<SecCertificateRef>)

/** The few Security / CommonCrypto calls the iOS TAK PKI needs. */
internal object TakApple {

    fun randomBytes(count: Int): ByteArray {
        val out = ByteArray(count)
        out.usePinned { SecRandomCopyBytes(kSecRandomDefault, count.convert(), it.addressOf(0)) }
        return out
    }

    fun newRsaKey(bits: Int): TakRsaKey = memScoped {
        val size = alloc<kotlinx.cinterop.IntVar>().also { it.value = bits }
        val sizeNumber = CFNumberCreate(null, kCFNumberSInt32Type, size.ptr)
        val attributes = dict(kSecAttrKeyType to kSecAttrKeyTypeRSA, kSecAttrKeySizeInBits to sizeNumber)
        val privateKey = SecKeyCreateRandomKey(attributes, null) ?: error("RSA key generation failed")
        val publicKey = SecKeyCopyPublicKey(privateKey) ?: error("RSA public key missing")
        val publicData = SecKeyCopyExternalRepresentation(publicKey, null) ?: error("RSA public key export failed")
        val privateData = SecKeyCopyExternalRepresentation(privateKey, null) ?: error("RSA private key export failed")
        TakRsaKey(privateKey, bytesOf(publicData), bytesOf(privateData))
    }

    /** RSA PKCS#1 v1.5 signature over SHA-256 of [data]. */
    fun sign(key: TakRsaKey, data: ByteArray): ByteArray {
        val signature =
            SecKeyCreateSignature(key.privateKey, kSecKeyAlgorithmRSASignatureMessagePKCS1v15SHA256, cfData(data), null)
                ?: error("RSA signing failed")
        return bytesOf(signature)
    }

    /** Triple-DES (three-key, CBC, PKCS#7 padding) for the legacy PKCS#12 encoding ATAK can still read. */
    fun tripleDes(encrypt: Boolean, key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray = memScoped {
        val out = ByteArray(data.size + 16)
        val moved = alloc<size_tVar>()
        val status = key.usePinned { k ->
            iv.usePinned { v ->
                data.usePinned { d ->
                    out.usePinned { o ->
                        CCCrypt(
                            (if (encrypt) 0 else 1).convert(),
                            2.convert(),
                            1.convert(),
                            k.addressOf(0),
                            key.size.convert(),
                            v.addressOf(0),
                            if (data.isEmpty()) null else d.addressOf(0),
                            data.size.convert(),
                            o.addressOf(0),
                            out.size.convert(),
                            moved.ptr,
                        )
                    }
                }
            }
        }
        check(status == 0) { "3DES failed: $status" }
        out.copyOf(moved.value.toInt())
    }

    /** Imports a PKCS#12 file. The identity is also added to the app's own keychain (SecPKCS12Import's behaviour). */
    fun importP12(p12: ByteArray, password: String): TakImported = memScoped {
        val passphrase = CFStringCreateWithCString(null, password, kCFStringEncodingUTF8)
        val options = dict(kSecImportExportPassphrase to passphrase)
        val items = alloc<CFArrayRefVar>()
        val status = SecPKCS12Import(cfData(p12), options, items.ptr)
        check(status == 0) { "PKCS#12 import failed: $status" }
        val first: CFDictionaryRef? = CFArrayGetValueAtIndex(items.value, 0)?.reinterpret()
        val identity: SecIdentityRef? = CFDictionaryGetValue(first, kSecImportItemIdentity)?.reinterpret()
        val chainArray: CFArrayRef? = CFDictionaryGetValue(first, kSecImportItemCertChain)?.reinterpret()
        val chain = ArrayList<SecCertificateRef>()
        if (chainArray != null) {
            for (i in 0 until CFArrayGetCount(chainArray)) {
                val cert: SecCertificateRef? = CFArrayGetValueAtIndex(chainArray, i)?.reinterpret()
                if (cert != null) chain += cert
            }
        }
        TakImported(identity, chain)
    }

    fun certificateBytes(cert: SecCertificateRef): ByteArray = bytesOf(SecCertificateCopyData(cert) ?: error("no data"))

    fun cfData(bytes: ByteArray): CFDataRef {
        require(bytes.isNotEmpty())
        return bytes.usePinned { CFDataCreate(null, it.addressOf(0).reinterpret(), bytes.size.convert()) }
            ?: error("CFData allocation failed")
    }

    fun bytesOf(data: CFDataRef): ByteArray {
        val length = CFDataGetLength(data).toInt()
        val out = ByteArray(length)
        if (length > 0) out.usePinned { memcpy(it.addressOf(0), CFDataGetBytePtr(data), length.convert()) }
        return out
    }

    private fun dict(vararg pairs: Pair<CFTypeRef?, CFTypeRef?>): CFDictionaryRef? = memScoped {
        val keys = allocArrayOf(*pairs.map { it.first }.toTypedArray())
        val values = allocArrayOf(*pairs.map { it.second }.toTypedArray())
        CFDictionaryCreate(null, keys.reinterpret(), values.reinterpret(), pairs.size.convert(), null, null)
    }
}
