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
package org.meshtastic.core.network.repository

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Single
import platform.Foundation.NSData
import platform.Foundation.NSNetService
import platform.Foundation.NSNetServiceBrowser
import platform.Foundation.NSNetServiceBrowserDelegateProtocol
import platform.Foundation.NSNetServiceDelegateProtocol
import platform.posix.memcpy
import platform.darwin.NSObject

private const val SERVICE_TYPE = "_meshtastic._tcp."
private const val RESOLVE_TIMEOUT_SECONDS = 10.0

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toBytes(): ByteArray {
    val out = ByteArray(length.toInt())
    if (out.isNotEmpty()) out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
}

/** iPhone: finds radios on the Wi-Fi (Bonjour / mDNS). */
@OptIn(ExperimentalForeignApi::class)
@Single(binds = [ServiceDiscovery::class])
class IosServiceDiscovery : ServiceDiscovery {
    override val resolvedServices: Flow<List<DiscoveredService>> = callbackFlow {
        val found = LinkedHashMap<String, NSNetService>()
        val resolved = LinkedHashMap<String, DiscoveredService>()

        fun push() {
            trySend(resolved.values.toList())
        }

        val serviceDelegate =
            object : NSObject(), NSNetServiceDelegateProtocol {
                override fun netServiceDidResolveAddress(sender: NSNetService) {
                    val host = sender.hostName?.trimEnd('.') ?: return
                    val txt =
                        sender.TXTRecordData()?.let { data ->
                            NSNetService.dictionaryFromTXTRecordData(data)
                                .entries
                                .mapNotNull { (k, v) -> (k as? String)?.let { key -> (v as? NSData)?.let { key to it.toBytes() } } }
                                .toMap()
                        } ?: emptyMap()
                    resolved[sender.name] = DiscoveredService(sender.name, host, sender.port.toInt(), txt)
                    push()
                }

                override fun netService(sender: NSNetService, didNotResolve: Map<Any?, *>) {
                    found.remove(sender.name)
                }
            }

        val browserDelegate =
            object : NSObject(), NSNetServiceBrowserDelegateProtocol {
                @ObjCSignatureOverride
                override fun netServiceBrowser(
                    browser: NSNetServiceBrowser,
                    didFindService: NSNetService,
                    moreComing: Boolean,
                ) {
                    found[didFindService.name] = didFindService
                    didFindService.delegate = serviceDelegate
                    didFindService.resolveWithTimeout(RESOLVE_TIMEOUT_SECONDS)
                }

                @ObjCSignatureOverride
                override fun netServiceBrowser(
                    browser: NSNetServiceBrowser,
                    didRemoveService: NSNetService,
                    moreComing: Boolean,
                ) {
                    found.remove(didRemoveService.name)
                    resolved.remove(didRemoveService.name)
                    push()
                }
            }

        val browser = NSNetServiceBrowser()
        browser.delegate = browserDelegate
        browser.searchForServicesOfType(SERVICE_TYPE, inDomain = "local.")
        push()
        awaitClose {
            browser.stop()
            found.values.forEach { it.stop() }
        }
    }.flowOn(Dispatchers.Main)
}
