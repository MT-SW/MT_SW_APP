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
package org.meshtastic.core.model

/** How an element of the app is drawn. The numbers are what is saved, so they never change. */
object ElementLook {
    /** The plain look the app always had. */
    const val FLAT = 0

    /** A little depth: a soft shadow, light along the upper edge and a thin bright rim. */
    const val DEPTH = 1

    /** The element's colour is partly see-through. */
    const val TRANSPARENT = 2

    /** Frosted glass: what is behind the element is blurred (only for elements that float above the content). */
    const val GLASS = 3
}

/** The look chosen for one group of elements: [look] (see [ElementLook]), its [strength] 0-100 and the glass [blur]. */
data class ElementStyle(val look: Int, val strength: Int, val blur: Int)

/**
 * MT_SW: the "look of the elements" options. Each is a small whole number saved under its key; a missing key means the
 * default below. Keeping them in one map means a new option needs no new plumbing.
 */
class UiOptions(private val stored: Map<String, Int> = emptyMap()) {
    operator fun get(key: String): Int = stored[key] ?: DEFAULTS[key] ?: 0

    fun flag(key: String): Boolean = get(key) != 0

    /** The look of the group of elements named [group] (one of the `GROUP_` names). */
    fun element(group: String): ElementStyle =
        ElementStyle(get("$group.look"), get("$group.strength"), get("$group.blur"))

    companion object {
        const val GROUP_NAV = "navbar"
        const val GROUP_CARDS = "cards"
        const val GROUP_INPUT = "input"
        const val GROUP_SEARCH = "search"

        /** The groups that float above the content and so can be glass. */
        val GLASS_GROUPS = setOf(GROUP_NAV, GROUP_INPUT, GROUP_SEARCH)

        const val SOFT_LIGHT = "theme.softLight"
        const val NAV_HIDE = "navbar.hide"
        const val NAV_SHOW = "navbar.show"
        const val NAV_ICONS = "navbar.icons"
        const val NAV_HAPTICS = "navbar.haptics"

        /** The send button buzzes while it is held. */
        const val HOLD_BUZZ = "input.holdBuzz"

        /** Scroll steps (in dp) of the choices of [NAV_HIDE] (1..3) and [NAV_SHOW] (0..1; 2 = only at the top). */
        val HIDE_STEPS_DP = intArrayOf(0, 24, 96, 240)
        val SHOW_STEPS_DP = intArrayOf(16, 72)
        const val SHOW_ONLY_AT_TOP = 2

        private const val DEFAULT_STRENGTH = 55
        private const val DEFAULT_BLUR = 20

        val DEFAULTS: Map<String, Int> =
            mapOf(
                SOFT_LIGHT to 0,
                NAV_HIDE to 0,
                NAV_SHOW to 0,
                NAV_ICONS to 1,
                NAV_HAPTICS to 1,
                HOLD_BUZZ to 1,
                "$GROUP_NAV.look" to ElementLook.FLAT,
                "$GROUP_NAV.strength" to DEFAULT_STRENGTH,
                "$GROUP_NAV.blur" to DEFAULT_BLUR,
                "$GROUP_CARDS.look" to ElementLook.FLAT,
                "$GROUP_CARDS.strength" to DEFAULT_STRENGTH,
                "$GROUP_CARDS.blur" to DEFAULT_BLUR,
                "$GROUP_INPUT.look" to ElementLook.FLAT,
                "$GROUP_INPUT.strength" to DEFAULT_STRENGTH,
                "$GROUP_INPUT.blur" to DEFAULT_BLUR,
                "$GROUP_SEARCH.look" to ElementLook.FLAT,
                "$GROUP_SEARCH.strength" to DEFAULT_STRENGTH,
                "$GROUP_SEARCH.blur" to DEFAULT_BLUR,
            )
    }
}
