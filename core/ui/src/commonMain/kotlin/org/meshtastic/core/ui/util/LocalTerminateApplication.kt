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

package org.meshtastic.core.ui.util

import androidx.compose.runtime.compositionLocalOf

/**
 * Fully ends the app: stops the background connection service (so nothing keeps reconnecting to the radio) and kills
 * the process. Provided by each app entry point (Android activity, desktop main); null where the platform offers no
 * such action, which hides the button that uses it.
 */
val LocalTerminateApplication = compositionLocalOf<(() -> Unit)?> { null }
