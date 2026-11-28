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
package org.meshtastic.desktop

import co.touchlab.kermit.Logger
import org.meshtastic.core.ui.component.WheelCloseTail
import java.awt.AWTEvent
import java.awt.EventQueue
import java.awt.Toolkit
import java.awt.event.MouseWheelEvent

/** A scroll that stayed silent this long has ended; what comes next is a new one. */
private const val TAIL_END_MS = 150L

/**
 * MT_SW: after a window is closed by scrolling, drops the rest of that scroll before it reaches the app, so it does not
 * zoom the map or move the list that was under the window.
 */
internal class DesktopWheelTail : EventQueue() {
    private var swallowing = false
    private var lastTime = 0L
    private var dropped = 0

    override fun dispatchEvent(event: AWTEvent) {
        if (swallowing && event is MouseWheelEvent) {
            // A scroll the other way is a new one, even without a break.
            if (event.`when` - lastTime < TAIL_END_MS && event.preciseWheelRotation <= 0.0) {
                lastTime = event.`when`
                dropped++
                return
            }
            swallowing = false
            Logger.i { "Okno: po zamknięciu pominięto $dropped ruchów przewijania" }
        }
        super.dispatchEvent(event)
    }

    private fun start() {
        swallowing = true
        dropped = 0
        lastTime = System.currentTimeMillis()
    }

    companion object {
        fun install() {
            val queue = DesktopWheelTail()
            Toolkit.getDefaultToolkit().systemEventQueue.push(queue)
            WheelCloseTail.onWindowClosed = queue::start
        }
    }
}
