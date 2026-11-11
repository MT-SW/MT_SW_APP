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
package org.meshtastic.feature.settings.component

import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform
import org.meshtastic.core.common.di.GOOGLE_SERVICES_AVAILABLE

/**
 * False in the Google flavor (the Google Play build): Google Play's payments policy does not allow a link to a donation
 * page that is not a tax-exempt organization's, so the row is not offered there. The F-Droid flavor, the desktop app
 * and the plain files from GitHub keep it. Where the flavor flag is not bound (desktop) the row is shown.
 */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
actual fun isSupportLinkAllowed(): Boolean = try {
    KoinPlatform.getKoin().getOrNull<Boolean>(named(GOOGLE_SERVICES_AVAILABLE)) != true
} catch (e: Exception) {
    true
}
