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
package org.meshtastic.desktop.notification

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

private const val CLICK_BUFFER = 8

/**
 * Taps on desktop notifications. Each native sender (macOS, Linux, Windows) reports the link of the notification that
 * was clicked here; the main window opens it (a conversation, for message notifications).
 *
 * The last click is replayed so one that arrives while the window is still being built (the app was started by the
 * click) is not lost; the window clears it once it has opened the link.
 */
object NotificationClicks {
    private val _links = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = CLICK_BUFFER)

    val links: SharedFlow<String> = _links.asSharedFlow()

    fun clicked(link: String) {
        _links.tryEmit(link)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun consumed() {
        _links.resetReplayCache()
    }
}
