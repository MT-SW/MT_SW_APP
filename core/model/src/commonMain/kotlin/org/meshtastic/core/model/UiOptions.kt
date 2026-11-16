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
        const val GROUP_BUBBLES = "bubbles"
        const val GROUP_DIALOGS = "dialogs"
        const val GROUP_INPUT = "input"
        const val GROUP_SEARCH = "search"
        const val GROUP_TOPBAR = "topbar"
        const val GROUP_MAP = "map"
        const val GROUP_DETAILS = "details"
        const val GROUP_ASSISTANT = "assistant"
        const val GROUP_HEALTH = "health"
        const val GROUP_SETTINGS = "settingscards"

        /** Every group of elements whose look can be chosen. */
        val ALL_GROUPS =
            listOf(
                GROUP_NAV,
                GROUP_TOPBAR,
                GROUP_CARDS,
                GROUP_DETAILS,
                GROUP_BUBBLES,
                GROUP_INPUT,
                GROUP_SEARCH,
                GROUP_DIALOGS,
                GROUP_MAP,
                GROUP_ASSISTANT,
                GROUP_HEALTH,
                GROUP_SETTINGS,
            )

        /** The groups that float above the content and so can be glass. */
        val GLASS_GROUPS = setOf(GROUP_NAV, GROUP_INPUT, GROUP_SEARCH, GROUP_TOPBAR, GROUP_MAP)

        const val NAV_HIDE = "navbar.hide"
        const val NAV_SHOW = "navbar.show"
        const val NAV_ICONS = "navbar.icons"
        const val NAV_HAPTICS = "navbar.haptics"

        /** The thickness of the edge line of the floating bar alone: a step of [RIM_STEPS_DP] (0 = no line). */
        const val NAV_RIM = "navbar.rim"

        /** How the open card is marked on the bar: [NAV_SELECT_BUBBLE], [NAV_SELECT_LINE] or [NAV_SELECT_BOTH]. */
        const val NAV_SELECT = "navbar.select"
        const val NAV_SELECT_BUBBLE = 0
        const val NAV_SELECT_LINE = 1
        const val NAV_SELECT_BOTH = 2

        /** The colour of the glass: 0 = from the theme, 1 = light, 2 = dark, 3 = the accent colour. */
        const val GLASS_TINT = "glass.tint"

        /** The thickness of the bright rim of glass and depth: a step of [RIM_STEPS_DP]. */
        const val RIM = "glass.rim"
        val RIM_STEPS_DP = floatArrayOf(0f, 0.5f, 1f, 1.5f, 2f)

        /** The strength of the soft gradient on the buttons: an index into [BUTTON_GRADIENT_SHARE] (0 = none). */
        const val BUTTON_GRADIENT = "buttons.gradient"
        val BUTTON_GRADIENT_SHARE = floatArrayOf(0f, 0.5f, 1f, 1.6f)

        /** Cards and bubbles give way a little under a finger, with a spring. */
        const val PRESS_SPRING = "motion.pressSpring"

        /** How the round progress rings look: the old thick line or the thin iPhone-style outline. */
        const val RING_STYLE = "rings.style"
        const val RING_STYLE_CLASSIC = 0
        const val RING_STYLE_THIN = 1

        /**
         * How the data charts (bars, heat map, half-round gauge) look: [CHART_STYLE_FLAT] (the old plain colours),
         * [CHART_STYLE_GLASS] (default: glossy 3D glass with a gradient), [CHART_STYLE_GRADIENT], [CHART_STYLE_OUTLINE] or
         * [CHART_STYLE_GLOW].
         */
        const val CHART_STYLE = "charts.style"
        const val CHART_STYLE_FLAT = 0
        const val CHART_STYLE_GLASS = 1
        const val CHART_STYLE_GRADIENT = 2
        const val CHART_STYLE_OUTLINE = 3
        const val CHART_STYLE_GLOW = 4

        /** The style of all icons: [ICON_STYLE_STANDARD], [ICON_STYLE_BOLD] or [ICON_STYLE_OUTLINE]. */
        const val ICON_STYLE = "icons.style"
        const val ICON_STYLE_STANDARD = 0
        const val ICON_STYLE_BOLD = 1
        const val ICON_STYLE_OUTLINE = 2

        /** Activity status: show the icon on the node list (1) or not (0). */
        const val PRESENCE_SHOW = "presence.show"

        /** Look of the activity status icon: [PRESENCE_ICON_DOT], [PRESENCE_ICON_PERSON] or [PRESENCE_ICON_RING]. */
        const val PRESENCE_ICON = "presence.icon"
        const val PRESENCE_ICON_DOT = 0
        const val PRESENCE_ICON_PERSON = 1
        const val PRESENCE_ICON_RING = 2

        /** Which other icon the activity status replaces while it is known: none, favourite, muted or unmessageable. */
        const val PRESENCE_REPLACE = "presence.replace"
        const val PRESENCE_REPLACE_NONE = 0
        const val PRESENCE_REPLACE_FAVORITE = 1
        const val PRESENCE_REPLACE_MUTED = 2
        const val PRESENCE_REPLACE_UNMESSAGEABLE = 3

        /**
         * How long an "active" status holds without hearing the node: an index into
         * `NodePresence.EXPIRY_STEPS_MINUTES`.
         */
        const val PRESENCE_EXPIRY = "presence.expiry"

        /** Colour of the active icon: green (0) or the colour of the theme / grey (1). */
        const val PRESENCE_COLOR = "presence.color"

        /**
         * Ask a node for its status by itself when its conversation is opened (1) or only listen to its broadcasts and
         * ask by hand in the on-demand diagnostics (0, the default), even when this app is MT_SW_FIRMWARE v3 itself.
         */
        const val PRESENCE_ASK = "presence.ask"

        /** The send button buzzes while it is held. */
        const val HOLD_BUZZ = "input.holdBuzz"

        /** Scroll steps (in dp) of the choices of [NAV_HIDE] (1..3) and [NAV_SHOW] (0..1; 2 = only at the top). */
        val HIDE_STEPS_DP = intArrayOf(0, 24, 96, 240)
        val SHOW_STEPS_DP = intArrayOf(16, 72)
        const val SHOW_ONLY_AT_TOP = 2

        private const val DEFAULT_STRENGTH = 45
        private const val GLASS_STRENGTH = 55
        private const val DEFAULT_BLUR = 28

        val DEFAULTS: Map<String, Int> = buildMap {
            put(NAV_HIDE, 0)
            put(NAV_SHOW, 0)
            put(NAV_ICONS, 1)
            put(NAV_HAPTICS, 1)
            put(NAV_RIM, 0)
            put(BUTTON_GRADIENT, 2)
            put(NAV_SELECT, NAV_SELECT_BUBBLE)
            put(PRESENCE_SHOW, 1)
            put(PRESENCE_ICON, PRESENCE_ICON_DOT)
            put(PRESENCE_REPLACE, PRESENCE_REPLACE_NONE)
            put(PRESENCE_EXPIRY, 1)
            put(PRESENCE_COLOR, 0)
            put(PRESENCE_ASK, 0)
            put(HOLD_BUZZ, 1)
            // The app starts with the glass look and the thin line icons like on the iPhone; the person can go back to
            // the classic look in "Look" (every choice they make is saved and stays).
            put(ICON_STYLE, ICON_STYLE_OUTLINE)
            put(GLASS_TINT, 0)
            put(RIM, 0)
            put(PRESS_SPRING, 1)
            put(RING_STYLE, RING_STYLE_THIN)
            put(CHART_STYLE, CHART_STYLE_GLASS)
            for (group in ALL_GROUPS) {
                // Glass where the element floats above the content (and can be blurred), see-through elsewhere.
                put("$group.look", if (group in GLASS_GROUPS) ElementLook.GLASS else ElementLook.TRANSPARENT)
                put("$group.strength", if (group in GLASS_GROUPS) GLASS_STRENGTH else DEFAULT_STRENGTH)
                put("$group.blur", DEFAULT_BLUR)
            }
        }
    }
}
