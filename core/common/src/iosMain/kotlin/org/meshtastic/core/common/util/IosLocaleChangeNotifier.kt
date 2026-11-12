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
package org.meshtastic.core.common.util

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.koin.core.annotation.Single
import platform.Foundation.NSCurrentLocaleDidChangeNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue

/** Reports every change of the system locale (language or region), so formatted values can be redrawn. */
@Single
class IosLocaleChangeNotifier : LocaleChangeNotifier {
    override val localeChanges: Flow<Unit> = callbackFlow {
        val center = NSNotificationCenter.defaultCenter
        val observer =
            center.addObserverForName(
                name = NSCurrentLocaleDidChangeNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue,
            ) { _ ->
                trySend(Unit)
            }
        awaitClose { center.removeObserver(observer) }
    }
}
