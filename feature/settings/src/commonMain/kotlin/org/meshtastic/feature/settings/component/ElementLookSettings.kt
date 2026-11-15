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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.ElementLook
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.element_group_bubbles
import org.meshtastic.core.resources.element_group_cards
import org.meshtastic.core.resources.element_group_dialogs
import org.meshtastic.core.resources.element_group_input
import org.meshtastic.core.resources.element_group_navbar
import org.meshtastic.core.resources.element_group_search
import org.meshtastic.core.resources.element_look
import org.meshtastic.core.resources.element_look_blur
import org.meshtastic.core.resources.element_look_depth
import org.meshtastic.core.resources.element_look_flat
import org.meshtastic.core.resources.element_look_glass
import org.meshtastic.core.resources.element_look_glass_summary
import org.meshtastic.core.resources.element_look_strength
import org.meshtastic.core.resources.element_look_summary
import org.meshtastic.core.resources.element_look_transparent
import org.meshtastic.core.resources.icon_style
import org.meshtastic.core.resources.icon_style_bold
import org.meshtastic.core.resources.icon_style_outline
import org.meshtastic.core.resources.icon_style_standard
import org.meshtastic.core.resources.icon_style_summary
import org.meshtastic.core.resources.input_hold_buzz
import org.meshtastic.core.resources.look_preset_classic
import org.meshtastic.core.resources.look_preset_iphone
import org.meshtastic.core.resources.look_preset_glass
import org.meshtastic.core.resources.look_preset_depth
import org.meshtastic.core.resources.look_presets_title
import org.meshtastic.core.resources.look_presets_summary
import org.meshtastic.core.resources.glass_tint
import org.meshtastic.core.resources.glass_tint_theme
import org.meshtastic.core.resources.glass_tint_light
import org.meshtastic.core.resources.glass_tint_dark
import org.meshtastic.core.resources.glass_tint_accent
import org.meshtastic.core.resources.glass_rim
import org.meshtastic.core.resources.press_spring
import org.meshtastic.core.resources.nav_bar_haptics
import org.meshtastic.core.resources.nav_bar_hide
import org.meshtastic.core.resources.nav_bar_hide_far
import org.meshtastic.core.resources.nav_bar_hide_medium
import org.meshtastic.core.resources.nav_bar_hide_off
import org.meshtastic.core.resources.nav_bar_hide_small
import org.meshtastic.core.resources.nav_bar_new_icons
import org.meshtastic.core.resources.nav_bar_show
import org.meshtastic.core.resources.nav_bar_show_at_top
import org.meshtastic.core.resources.nav_bar_show_far
import org.meshtastic.core.resources.nav_bar_show_small
import org.meshtastic.core.resources.soft_light_theme
import org.meshtastic.core.ui.component.DropDownPreference
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.SliderPreference
import org.meshtastic.core.ui.component.SwitchListItem
import org.meshtastic.core.ui.icon.DisplaySettings
import org.meshtastic.core.ui.icon.MeshtasticIcons

private const val STRENGTH_STEP = 5
private const val STRENGTH_MAX = 100
private const val BLUR_MAX = 60

/**
 * MT_SW: the settings of the look of the elements: the soft grey of the light themes, the look of the floating bar,
 * the cards, the message box and the search (plain, with depth, see-through or glass, each with its strength and
 * blur), and how the bar hides, its icons and its vibration. [onChange] saves one option under its key.
 */
@Composable
fun ElementLookSettings(options: UiOptions, onChange: (String, Int) -> Unit) {
    SwitchListItem(
        text = stringResource(Res.string.soft_light_theme),
        leadingIcon = MeshtasticIcons.DisplaySettings,
        checked = options.flag(UiOptions.SOFT_LIGHT),
        onClick = { onChange(UiOptions.SOFT_LIGHT, if (options.flag(UiOptions.SOFT_LIGHT)) 0 else 1) },
    )
    Text(
        text = stringResource(Res.string.element_look_summary),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
    DropDownPreference(
        title = stringResource(Res.string.icon_style),
        enabled = true,
        items =
        listOf(
            UiOptions.ICON_STYLE_STANDARD to stringResource(Res.string.icon_style_standard),
            UiOptions.ICON_STYLE_BOLD to stringResource(Res.string.icon_style_bold),
            UiOptions.ICON_STYLE_OUTLINE to stringResource(Res.string.icon_style_outline),
        ),
        selectedItem = options[UiOptions.ICON_STYLE],
        onItemSelected = { onChange(UiOptions.ICON_STYLE, it) },
        summary = stringResource(Res.string.icon_style_summary),
    )
    ElementPresets(onChange)
    ElementGroupSettings(stringResource(Res.string.element_group_navbar), UiOptions.GROUP_NAV, options, onChange)
    ElementGroupSettings(stringResource(Res.string.element_group_cards), UiOptions.GROUP_CARDS, options, onChange)
    ElementGroupSettings(stringResource(Res.string.element_group_bubbles), UiOptions.GROUP_BUBBLES, options, onChange)
    ElementGroupSettings(stringResource(Res.string.element_group_input), UiOptions.GROUP_INPUT, options, onChange)
    ElementGroupSettings(stringResource(Res.string.element_group_search), UiOptions.GROUP_SEARCH, options, onChange)
    ElementGroupSettings(stringResource(Res.string.element_group_dialogs), UiOptions.GROUP_DIALOGS, options, onChange)
    GlassDetailSettings(options, onChange)
    NavBarBehaviourSettings(options, onChange)
}

/** The look, the strength and (for glass) the blur of one group of elements. */
@Composable
private fun ElementGroupSettings(title: String, group: String, options: UiOptions, onChange: (String, Int) -> Unit) {
    val style = options.element(group)
    val canBeGlass = group in UiOptions.GLASS_GROUPS
    val looks =
        buildList {
            add(ElementLook.FLAT to stringResource(Res.string.element_look_flat))
            add(ElementLook.DEPTH to stringResource(Res.string.element_look_depth))
            add(ElementLook.TRANSPARENT to stringResource(Res.string.element_look_transparent))
            if (canBeGlass) add(ElementLook.GLASS to stringResource(Res.string.element_look_glass))
        }
    Column(modifier = Modifier.fillMaxWidth()) {
        DropDownPreference(
            title = "$title: ${stringResource(Res.string.element_look)}",
            enabled = true,
            items = looks,
            selectedItem = style.look,
            onItemSelected = { onChange("$group.look", it) },
            summary = if (canBeGlass) stringResource(Res.string.element_look_glass_summary) else null,
        )
        if (style.look != ElementLook.FLAT) {
            SliderPreference(
                title = stringResource(Res.string.element_look_strength),
                enabled = true,
                items = (0..STRENGTH_MAX step STRENGTH_STEP).map { it to "$it%" },
                selectedValue = style.strength,
                onValueChange = { onChange("$group.strength", it) },
            )
        }
        if (style.look == ElementLook.GLASS) {
            SliderPreference(
                title = stringResource(Res.string.element_look_blur),
                enabled = true,
                items = (0..BLUR_MAX step STRENGTH_STEP).map { it to "$it" },
                selectedValue = style.blur,
                onValueChange = { onChange("$group.blur", it) },
            )
        }
    }
}

/** How the floating bar hides and comes back, its icons and the vibration when a tab is changed. */
@Composable
private fun NavBarBehaviourSettings(options: UiOptions, onChange: (String, Int) -> Unit) {
    DropDownPreference(
        title = stringResource(Res.string.nav_bar_hide),
        enabled = true,
        items =
        listOf(
            0 to stringResource(Res.string.nav_bar_hide_off),
            1 to stringResource(Res.string.nav_bar_hide_small),
            2 to stringResource(Res.string.nav_bar_hide_medium),
            3 to stringResource(Res.string.nav_bar_hide_far),
        ),
        selectedItem = options[UiOptions.NAV_HIDE],
        onItemSelected = { onChange(UiOptions.NAV_HIDE, it) },
    )
    if (options[UiOptions.NAV_HIDE] != 0) {
        DropDownPreference(
            title = stringResource(Res.string.nav_bar_show),
            enabled = true,
            items =
            listOf(
                0 to stringResource(Res.string.nav_bar_show_small),
                1 to stringResource(Res.string.nav_bar_show_far),
                UiOptions.SHOW_ONLY_AT_TOP to stringResource(Res.string.nav_bar_show_at_top),
            ),
            selectedItem = options[UiOptions.NAV_SHOW],
            onItemSelected = { onChange(UiOptions.NAV_SHOW, it) },
        )
    }
    SwitchListItem(
        text = stringResource(Res.string.nav_bar_new_icons),
        leadingIcon = MeshtasticIcons.DisplaySettings,
        checked = options.flag(UiOptions.NAV_ICONS),
        onClick = { onChange(UiOptions.NAV_ICONS, if (options.flag(UiOptions.NAV_ICONS)) 0 else 1) },
    )
    SwitchListItem(
        text = stringResource(Res.string.nav_bar_haptics),
        leadingIcon = MeshtasticIcons.DisplaySettings,
        checked = options.flag(UiOptions.NAV_HAPTICS),
        onClick = { onChange(UiOptions.NAV_HAPTICS, if (options.flag(UiOptions.NAV_HAPTICS)) 0 else 1) },
    )
    SwitchListItem(
        text = stringResource(Res.string.input_hold_buzz),
        leadingIcon = MeshtasticIcons.DisplaySettings,
        checked = options.flag(UiOptions.HOLD_BUZZ),
        onClick = { onChange(UiOptions.HOLD_BUZZ, if (options.flag(UiOptions.HOLD_BUZZ)) 0 else 1) },
    )
}

/** One ready-made set of looks: the options it sets, each with its value. */
private class LookPreset(val title: StringResource, val values: Map<String, Int>)

private fun look(group: String, look: Int, strength: Int, blur: Int = 20): Map<String, Int> =
    mapOf("$group.look" to look, "$group.strength" to strength, "$group.blur" to blur)

private const val PRESET_BLUR = 24

private val LOOK_PRESETS =
    listOf(
        LookPreset(
            Res.string.look_preset_classic,
            listOf(
                UiOptions.GROUP_NAV,
                UiOptions.GROUP_CARDS,
                UiOptions.GROUP_BUBBLES,
                UiOptions.GROUP_INPUT,
                UiOptions.GROUP_SEARCH,
                UiOptions.GROUP_DIALOGS,
            ).fold(
                mapOf(
                    UiOptions.ICON_STYLE to UiOptions.ICON_STYLE_STANDARD,
                    UiOptions.SOFT_LIGHT to 0,
                    UiOptions.PRESS_SPRING to 0,
                ),
            ) { acc, group -> acc + look(group, ElementLook.FLAT, 55) },
        ),
        LookPreset(
            Res.string.look_preset_iphone,
            mapOf(
                UiOptions.ICON_STYLE to UiOptions.ICON_STYLE_OUTLINE,
                UiOptions.SOFT_LIGHT to 1,
                UiOptions.PRESS_SPRING to 1,
            ) + look(UiOptions.GROUP_NAV, ElementLook.GLASS, 45, PRESET_BLUR) +
                look(UiOptions.GROUP_CARDS, ElementLook.DEPTH, 40) +
                look(UiOptions.GROUP_BUBBLES, ElementLook.DEPTH, 35) +
                look(UiOptions.GROUP_INPUT, ElementLook.GLASS, 50, PRESET_BLUR) +
                look(UiOptions.GROUP_SEARCH, ElementLook.GLASS, 50, PRESET_BLUR) +
                look(UiOptions.GROUP_DIALOGS, ElementLook.GLASS, 40, PRESET_BLUR),
        ),
        LookPreset(
            Res.string.look_preset_glass,
            mapOf(UiOptions.PRESS_SPRING to 1) +
                look(UiOptions.GROUP_NAV, ElementLook.GLASS, 55, PRESET_BLUR) +
                look(UiOptions.GROUP_CARDS, ElementLook.TRANSPARENT, 45) +
                look(UiOptions.GROUP_BUBBLES, ElementLook.TRANSPARENT, 35) +
                look(UiOptions.GROUP_INPUT, ElementLook.GLASS, 55, PRESET_BLUR) +
                look(UiOptions.GROUP_SEARCH, ElementLook.GLASS, 55, PRESET_BLUR) +
                look(UiOptions.GROUP_DIALOGS, ElementLook.GLASS, 45, PRESET_BLUR),
        ),
        LookPreset(
            Res.string.look_preset_depth,
            mapOf(UiOptions.PRESS_SPRING to 1) +
                look(UiOptions.GROUP_NAV, ElementLook.DEPTH, 55) +
                look(UiOptions.GROUP_CARDS, ElementLook.DEPTH, 55) +
                look(UiOptions.GROUP_BUBBLES, ElementLook.DEPTH, 55) +
                look(UiOptions.GROUP_INPUT, ElementLook.DEPTH, 55) +
                look(UiOptions.GROUP_SEARCH, ElementLook.DEPTH, 55) +
                look(UiOptions.GROUP_DIALOGS, ElementLook.DEPTH, 55),
        ),
    )

/** Ready-made sets of looks: one tap sets many options at once, and each of them can still be changed below. */
@Composable
private fun ElementPresets(onChange: (String, Int) -> Unit) {
    Text(
        text = stringResource(Res.string.look_presets_title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    Text(
        text = stringResource(Res.string.look_presets_summary),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    for (preset in LOOK_PRESETS) {
        ListItem(
            text = stringResource(preset.title),
            leadingIcon = MeshtasticIcons.DisplaySettings,
        ) {
            preset.values.forEach { (key, value) -> onChange(key, value) }
        }
    }
}

/** The colour of the glass, the thickness of its rim and the spring on touch. */
@Composable
private fun GlassDetailSettings(options: UiOptions, onChange: (String, Int) -> Unit) {
    DropDownPreference(
        title = stringResource(Res.string.glass_tint),
        enabled = true,
        items =
        listOf(
            0 to stringResource(Res.string.glass_tint_theme),
            1 to stringResource(Res.string.glass_tint_light),
            2 to stringResource(Res.string.glass_tint_dark),
            3 to stringResource(Res.string.glass_tint_accent),
        ),
        selectedItem = options[UiOptions.GLASS_TINT],
        onItemSelected = { onChange(UiOptions.GLASS_TINT, it) },
    )
    SliderPreference(
        title = stringResource(Res.string.glass_rim),
        enabled = true,
        items = UiOptions.RIM_STEPS_DP.indices.map { it to "${UiOptions.RIM_STEPS_DP[it]} dp" },
        selectedValue = options[UiOptions.RIM],
        onValueChange = { onChange(UiOptions.RIM, it) },
    )
    SwitchListItem(
        text = stringResource(Res.string.press_spring),
        leadingIcon = MeshtasticIcons.DisplaySettings,
        checked = options.flag(UiOptions.PRESS_SPRING),
        onClick = { onChange(UiOptions.PRESS_SPRING, if (options.flag(UiOptions.PRESS_SPRING)) 0 else 1) },
    )
}
