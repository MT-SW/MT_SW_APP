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

        /** 1 = the floating bar casts a shadow (off by default). */
        const val NAV_SHADOW = "navbar.shadow"

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

        /** The shadow under glass cards and glass surfaces: a step of [GLASS_SHADOW_STEPS_DP] (0 = no shadow). */
        const val GLASS_SHADOW = "glass.shadow"
        val GLASS_SHADOW_STEPS_DP = floatArrayOf(0f, 4f, 8f, 14f, 22f)

        /** The strength of the soft gradient on the buttons: an index into [BUTTON_GRADIENT_SHARE] (0 = none). */
        const val BUTTON_GRADIENT = "buttons.gradient"
        val BUTTON_GRADIENT_SHARE = floatArrayOf(0f, 0.5f, 1f, 1.6f)

        /** Cards and bubbles give way a little under a finger, with a spring. */
        const val PRESS_SPRING = "motion.pressSpring"

        /** How the round progress rings look: the old thick line or the thin iPhone-style outline. */
        const val RING_STYLE = "rings.style"
        const val RING_STYLE_CLASSIC = 0
        const val RING_STYLE_THIN = 1

        /** How sliders, switches and check boxes look: the Material ones or the thin iPhone-style outlines. */
        const val CONTROL_STYLE = "controls.style"
        const val CONTROL_STYLE_CLASSIC = 0
        const val CONTROL_STYLE_THIN = 1

        /**
         * How the buttons look: [BUTTON_STYLE_CLASSIC] (the plain Material ones), [BUTTON_STYLE_GLASS] (filled glossy
         * glass) or [BUTTON_STYLE_OUTLINE_GLASS] (the default: a thin outline on a clear glass pane; a working button
         * has a soft glow in the colour of the theme, a switched-off one is grey).
         */
        const val BUTTON_STYLE = "buttons.style"
        const val BUTTON_STYLE_CLASSIC = 0
        const val BUTTON_STYLE_GLASS = 1
        const val BUTTON_STYLE_OUTLINE_GLASS = 2

        /**
         * The brightness (colour saturation) of the accent of the buttons and small elements: an index into
         * [BRIGHTNESS_SHARE]. One general setting; the main themes themselves are not changed by it.
         */
        const val BRIGHTNESS = "look.brightness"
        val BRIGHTNESS_SHARE = floatArrayOf(0.4f, 0.7f, 1f, 1.25f, 1.5f)

        /**
         * The brightness of the buttons alone: 0 = as the general setting, 1..5 = an own level of [BRIGHTNESS_SHARE].
         */
        const val BUTTON_BRIGHTNESS = "buttons.brightness"

        /** The saturation share of the small elements (sliders, switches, progress bars): the general level. */
        fun elementBrightness(options: UiOptions): Float =
            BRIGHTNESS_SHARE[options[BRIGHTNESS].coerceIn(0, BRIGHTNESS_SHARE.lastIndex)]

        /** The saturation share of the buttons: their own level, or the general one. */
        fun buttonBrightness(options: UiOptions): Float {
            val own = options[BUTTON_BRIGHTNESS]
            val index = if (own in 1..BRIGHTNESS_SHARE.size) own - 1 else options[BRIGHTNESS]
            return BRIGHTNESS_SHARE[index.coerceIn(0, BRIGHTNESS_SHARE.lastIndex)]
        }

        /** The strength of the glow of a working outline-glass button: an index into [BUTTON_GLOW_SHARE] (0 = none). */
        const val BUTTON_GLOW = "buttons.glow"
        val BUTTON_GLOW_SHARE = floatArrayOf(0f, 0.5f, 1f, 1.6f)

        /** The thickness of the outline of an outline-glass button: an index into [BUTTON_OUTLINE_DP]. */
        const val BUTTON_OUTLINE = "buttons.outline"
        val BUTTON_OUTLINE_DP = floatArrayOf(1f, 1.5f, 2f, 3f)

        /** How the radio chooser shows a dropped radio: [RADIO_ALERT_OFF], [RADIO_ALERT_DIGIT], [RADIO_ALERT_PULSE]. */
        const val RADIO_ALERT = "radios.alert"
        const val RADIO_ALERT_OFF = 0
        const val RADIO_ALERT_DIGIT = 1
        const val RADIO_ALERT_PULSE = 2

        /** The colour of the pulse: an index into [RADIO_ALERT_COLORS] (white, purple, theme colour, amber, red). */
        const val RADIO_ALERT_COLOR = "radios.alert.color"
        val RADIO_ALERT_COLORS = longArrayOf(0xFFFFFFFF, 0xFFB388FF, 0L, 0xFFFFB300, 0xFFE53935)

        /** The length of one pulse: an index into [RADIO_ALERT_PERIOD_MS]. */
        const val RADIO_ALERT_SPEED = "radios.alert.speed"
        val RADIO_ALERT_PERIOD_MS = intArrayOf(2400, 1600, 1000)

        /** 1 = the shadow of the plate of our node blinks blue and green together with the connection icon (0 = off). */
        const val NODE_CHIP_BLINK = "nodeChip.blink"


        /** Whether the radio chooser shows the number of connected radios (1 = yes). */
        const val RADIO_COUNT = "radios.count"

        /**
         * 1 = the saved radios that are not in use keep collecting packets in the background (0 = off; on by default).
         */
        const val RADIO_BACKGROUND = "radios.background"

        /**
         * How the data charts (bars, heat map, half-round gauge) look: [CHART_STYLE_FLAT] (the old plain colours),
         * [CHART_STYLE_GLASS] (glossy 3D glass with a gradient), [CHART_STYLE_RIBBON] (default: glass, with a glass
         * card and drops under line charts), [CHART_STYLE_GRADIENT], [CHART_STYLE_OUTLINE] or [CHART_STYLE_GLOW].
         */
        const val CHART_STYLE = "charts.style"
        const val CHART_STYLE_FLAT = 0
        const val CHART_STYLE_GLASS = 1
        const val CHART_STYLE_GRADIENT = 2
        const val CHART_STYLE_OUTLINE = 3
        const val CHART_STYLE_GLOW = 4

        /**
         * Like [CHART_STYLE_GLASS]; the line charts of the node statistics lie on a glass card and have glass drops.
         */
        const val CHART_STYLE_RIBBON = 5

        /**
         * Own chart style of "Network health" and of the node details; the default is [CHART_STYLE_RIBBON];
         * [CHART_STYLE_SAME] means the general [CHART_STYLE].
         */
        const val CHART_STYLE_HEALTH = "charts.style.health"
        const val CHART_STYLE_NODE = "charts.style.node"
        const val CHART_STYLE_SAME = -1

        /**
         * How the node chips (the coloured plates with a short name, in the lists, on the map and in the panorama)
         * look: [NODE_CHIP_FLAT] (the plain colour, as before), [NODE_CHIP_STANDARD] (the default: the node's colour
         * with a gradient, a little transparency, a thin outline and a soft shadow, in the style of the rest of the
         * app) or [NODE_CHIP_GLASS_3D] (a stronger glass gradient with sheen, an outline and a soft shadow).
         */
        const val NODE_CHIP_STYLE = "chips.style"
        const val NODE_CHIP_FLAT = 0
        const val NODE_CHIP_STANDARD = 1
        const val NODE_CHIP_GLASS_3D = 2

        /**
         * The look of the terrain panorama (ridges and buildings), the antenna pattern plot and the choice buttons:
         * [VIEW_STYLE_GLASS] (the default: gradient, sheen, outline), [VIEW_STYLE_STANDARD] (plain colours with a thin
         * outline) or [VIEW_STYLE_FLAT] (plain colours, no sheen or outline).
         */
        const val VIEW_STYLE = "views.style"
        const val VIEW_STYLE_GLASS = 0
        const val VIEW_STYLE_STANDARD = 1
        const val VIEW_STYLE_FLAT = 2

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
            put(NAV_SHADOW, 0)
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
            put(GLASS_SHADOW, 3)
            put(PRESS_SPRING, 1)
            put(RING_STYLE, RING_STYLE_THIN)
            put(CONTROL_STYLE, CONTROL_STYLE_THIN)
            put(BUTTON_STYLE, BUTTON_STYLE_OUTLINE_GLASS)
            put(BUTTON_GLOW, 2)
            put(BRIGHTNESS, 2)
            put(BUTTON_BRIGHTNESS, 0)
            put(BUTTON_OUTLINE, 1)
            put(RADIO_ALERT, RADIO_ALERT_PULSE)
            put(RADIO_ALERT_COLOR, 0)
            put(RADIO_ALERT_SPEED, 1)
            put(RADIO_COUNT, 1)
            put(NODE_CHIP_BLINK, 0)
            put(RADIO_BACKGROUND, 1)
            put(CHART_STYLE, CHART_STYLE_RIBBON)
            put(CHART_STYLE_HEALTH, CHART_STYLE_RIBBON)
            put(CHART_STYLE_NODE, CHART_STYLE_RIBBON)
            put(NODE_CHIP_STYLE, NODE_CHIP_STANDARD)
            put(VIEW_STYLE, VIEW_STYLE_GLASS)
            for (group in ALL_GROUPS) {
                // Glass where the element floats above the content (and can be blurred), see-through elsewhere.
                put("$group.look", if (group in GLASS_GROUPS) ElementLook.GLASS else ElementLook.TRANSPARENT)
                put("$group.strength", if (group in GLASS_GROUPS) GLASS_STRENGTH else DEFAULT_STRENGTH)
                put("$group.blur", DEFAULT_BLUR)
            }
            // Own defaults of some groups (the author's settings): strength and blur of the glass.
            put("$GROUP_TOPBAR.strength", 50)
            put("$GROUP_ASSISTANT.strength", 40)
            put("$GROUP_HEALTH.strength", 40)
            put("$GROUP_NAV.blur", 5)
            put("$GROUP_TOPBAR.blur", 15)
            put("$GROUP_INPUT.blur", 0)
            put("$GROUP_SEARCH.blur", 0)
            put("$GROUP_MAP.blur", 0)
        }
    }
}
