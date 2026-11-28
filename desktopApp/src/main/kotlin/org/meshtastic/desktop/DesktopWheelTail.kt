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
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Toolkit
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent

/** A scroll that stayed silent this long has ended; what comes next is a new one. */
private const val TAIL_END_MS = 40L

/** A smaller move of the pointer than this (in pixels) is noise, not the person doing something new. */
private const val POINTER_MOVE_PX = 3.0

/**
 * MT_SW: after a window is closed by scrolling, drops the rest of that scroll before it reaches the app, so it does not
 * zoom the map or move the list that was under the window.
 */
internal class DesktopWheelTail : EventQueue() {
    private var swallowing = false
    private var lastTime = 0L
    private var dropped = 0
    private var pointerAtStart: Point? = null

    override fun dispatchEvent(event: AWTEvent) {
        if (swallowing && event is MouseEvent && isNewAction(event)) stop("ruch kursora lub kliknięcie")
        if (swallowing && event is MouseWheelEvent) {
            // Only silence ends it: a trackpad mixes sideways and opposite jitter into the same scroll.
            val gap = event.`when` - lastTime
            if (gap < TAIL_END_MS) {
                lastTime = event.`when`
                dropped++
                return
            }
            stop("cisza ${gap}ms")
        }
        super.dispatchEvent(event)
    }

    private fun isNewAction(event: MouseEvent): Boolean = when (event.id) {
        MouseEvent.MOUSE_PRESSED -> true
        MouseEvent.MOUSE_MOVED ->
            pointerAtStart?.let { it.distance(event.locationOnScreen) >= POINTER_MOVE_PX } == true
        else -> false
    }

    private fun stop(reason: String) {
        swallowing = false
        Logger.i { "Okno: po zamknięciu pominięto $dropped ruchów przewijania (koniec: $reason)" }
    }

    private fun start() {
        swallowing = true
        dropped = 0
        pointerAtStart = MouseInfo.getPointerInfo()?.location
        lastTime = System.currentTimeMillis()
        Logger.i { "Okno: pomijam resztę przewijania po zamknięciu" }
    }

    companion object {
        fun install() {
            val queue = DesktopWheelTail()
            Toolkit.getDefaultToolkit().systemEventQueue.push(queue)
            WheelCloseTail.onWindowClosed = queue::start
        }
    }
}
