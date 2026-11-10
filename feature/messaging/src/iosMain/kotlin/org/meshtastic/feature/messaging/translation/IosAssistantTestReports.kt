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
package org.meshtastic.feature.messaging.translation

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToURL

/** iPhone and iPad: the result files lie in the app's documents folder (visible in the Files app). */
class IosAssistantTestReports : AssistantTestReports {
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun write(name: String, text: String): String? = runCatching {
        val dir =
            NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).firstOrNull() as? NSURL
                ?: return null
        val target = dir.URLByAppendingPathComponent(name) ?: return null
        val ok =
            NSString.create(string = text)
                .writeToURL(target, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        if (ok) target.path else null
    }
        .getOrNull()
}
