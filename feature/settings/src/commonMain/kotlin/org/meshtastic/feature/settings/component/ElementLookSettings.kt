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
@file:Suppress("LongMethod", "MagicNumber", "ModifierMissing", "MultipleEmitters", "TooManyFunctions")

package org.meshtastic.feature.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.ElementLook
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.backup_look
import org.meshtastic.core.resources.brightness_1
import org.meshtastic.core.resources.brightness_2
import org.meshtastic.core.resources.brightness_3
import org.meshtastic.core.resources.brightness_4
import org.meshtastic.core.resources.brightness_5
import org.meshtastic.core.resources.brightness_as_general
import org.meshtastic.core.resources.brightness_buttons
import org.meshtastic.core.resources.brightness_general
import org.meshtastic.core.resources.button_glow
import org.meshtastic.core.resources.button_gradient
import org.meshtastic.core.resources.button_gradient_normal
import org.meshtastic.core.resources.button_gradient_off
import org.meshtastic.core.resources.button_gradient_strong
import org.meshtastic.core.resources.button_gradient_weak
import org.meshtastic.core.resources.button_outline
import org.meshtastic.core.resources.button_style
import org.meshtastic.core.resources.button_style_classic
import org.meshtastic.core.resources.button_style_glass
import org.meshtastic.core.resources.button_style_outline_glass
import org.meshtastic.core.resources.button_style_summary
import org.meshtastic.core.resources.control_style_thin
import org.meshtastic.core.resources.control_style_thin_summary
import org.meshtastic.core.resources.dialog_see_through
import org.meshtastic.core.resources.dialog_see_through_summary
import org.meshtastic.core.resources.element_group_assistant
import org.meshtastic.core.resources.element_group_bubbles
import org.meshtastic.core.resources.element_group_cards
import org.meshtastic.core.resources.element_group_details
import org.meshtastic.core.resources.element_group_dialogs
import org.meshtastic.core.resources.element_group_health
import org.meshtastic.core.resources.element_group_input
import org.meshtastic.core.resources.element_group_map
import org.meshtastic.core.resources.element_group_navbar
import org.meshtastic.core.resources.element_group_search
import org.meshtastic.core.resources.element_group_settings
import org.meshtastic.core.resources.element_group_topbar
import org.meshtastic.core.resources.element_look
import org.meshtastic.core.resources.element_look_blur
import org.meshtastic.core.resources.element_look_depth
import org.meshtastic.core.resources.element_look_flat
import org.meshtastic.core.resources.element_look_glass
import org.meshtastic.core.resources.element_look_glass_summary
import org.meshtastic.core.resources.element_look_strength
import org.meshtastic.core.resources.element_look_summary
import org.meshtastic.core.resources.element_look_transparent
import org.meshtastic.core.resources.glass_rim
import org.meshtastic.core.resources.glass_shadow
import org.meshtastic.core.resources.glass_tint
import org.meshtastic.core.resources.glass_tint_accent
import org.meshtastic.core.resources.glass_tint_dark
import org.meshtastic.core.resources.glass_tint_light
import org.meshtastic.core.resources.glass_tint_theme
import org.meshtastic.core.resources.icon_style
import org.meshtastic.core.resources.icon_style_bold
import org.meshtastic.core.resources.icon_style_outline
import org.meshtastic.core.resources.icon_style_standard
import org.meshtastic.core.resources.icon_style_summary
import org.meshtastic.core.resources.input_hold_buzz
import org.meshtastic.core.resources.look_advanced
import org.meshtastic.core.resources.look_advanced_summary
import org.meshtastic.core.resources.look_all
import org.meshtastic.core.resources.look_area_ai
import org.meshtastic.core.resources.look_area_bars
import org.meshtastic.core.resources.look_area_cards
import org.meshtastic.core.resources.look_area_dialogs
import org.meshtastic.core.resources.look_area_fields
import org.meshtastic.core.resources.look_area_map
import org.meshtastic.core.resources.look_area_messages
import org.meshtastic.core.resources.look_backup_summary
import org.meshtastic.core.resources.look_backup_title
import org.meshtastic.core.resources.look_basic_summary
import org.meshtastic.core.resources.look_mixed
import org.meshtastic.core.resources.look_preset_classic
import org.meshtastic.core.resources.look_preset_depth
import org.meshtastic.core.resources.look_preset_glass
import org.meshtastic.core.resources.look_preset_iphone
import org.meshtastic.core.resources.look_presets_summary
import org.meshtastic.core.resources.look_presets_title
import org.meshtastic.core.resources.nav_bar_haptics
import org.meshtastic.core.resources.nav_bar_hide
import org.meshtastic.core.resources.nav_bar_hide_far
import org.meshtastic.core.resources.nav_bar_hide_medium
import org.meshtastic.core.resources.nav_bar_hide_off
import org.meshtastic.core.resources.nav_bar_hide_small
import org.meshtastic.core.resources.nav_bar_new_icons
import org.meshtastic.core.resources.nav_bar_rim
import org.meshtastic.core.resources.nav_bar_select
import org.meshtastic.core.resources.nav_bar_shadow
import org.meshtastic.core.resources.nav_bar_show
import org.meshtastic.core.resources.nav_bar_show_at_top
import org.meshtastic.core.resources.nav_bar_show_far
import org.meshtastic.core.resources.nav_bar_show_small
import org.meshtastic.core.resources.nav_select_both
import org.meshtastic.core.resources.nav_select_bubble
import org.meshtastic.core.resources.nav_select_line
import org.meshtastic.core.resources.press_spring
import org.meshtastic.core.resources.radio_alert
import org.meshtastic.core.resources.radio_alert_color
import org.meshtastic.core.resources.radio_alert_color_amber
import org.meshtastic.core.resources.radio_alert_color_purple
import org.meshtastic.core.resources.radio_alert_color_red
import org.meshtastic.core.resources.radio_alert_color_theme
import org.meshtastic.core.resources.radio_alert_color_white
import org.meshtastic.core.resources.radio_alert_digit
import org.meshtastic.core.resources.radio_alert_off
import org.meshtastic.core.resources.radio_alert_pulse
import org.meshtastic.core.resources.radio_alert_speed
import org.meshtastic.core.resources.radio_alert_speed_fast
import org.meshtastic.core.resources.radio_alert_speed_medium
import org.meshtastic.core.resources.radio_alert_speed_slow
import org.meshtastic.core.resources.radio_background
import org.meshtastic.core.resources.radio_background_off
import org.meshtastic.core.resources.radio_background_on
import org.meshtastic.core.resources.radio_count
import org.meshtastic.core.resources.radio_count_hide
import org.meshtastic.core.resources.radio_count_show
import org.meshtastic.core.resources.restore_look
import org.meshtastic.core.resources.ring_style_thin
import org.meshtastic.core.resources.ring_style_thin_summary
import org.meshtastic.core.ui.component.DropDownPreference
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.LookPreview
import org.meshtastic.core.ui.component.SliderPreference
import org.meshtastic.core.ui.component.SwitchListItem
import org.meshtastic.core.ui.icon.DisplaySettings
import org.meshtastic.core.ui.icon.Download
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Upload
import org.meshtastic.feature.settings.radio.rememberSettingsBackupActions

private const val STRENGTH_STEP = 5
private const val STRENGTH_MAX = 100
private const val BLUR_MAX = 60
private const val MIXED = -1

/** One part of the app that has a simple look choice: the groups of elements it covers. */
private class LookArea(val title: StringResource, val groups: List<String>)

private val LOOK_AREAS =
    listOf(
        LookArea(Res.string.look_area_bars, listOf(UiOptions.GROUP_NAV, UiOptions.GROUP_TOPBAR)),
        LookArea(
            Res.string.look_area_cards,
            listOf(UiOptions.GROUP_CARDS, UiOptions.GROUP_DETAILS, UiOptions.GROUP_SETTINGS),
        ),
        LookArea(Res.string.look_area_messages, listOf(UiOptions.GROUP_BUBBLES)),
        LookArea(Res.string.look_area_fields, listOf(UiOptions.GROUP_INPUT, UiOptions.GROUP_SEARCH)),
        LookArea(Res.string.look_area_dialogs, listOf(UiOptions.GROUP_DIALOGS)),
        LookArea(Res.string.look_area_map, listOf(UiOptions.GROUP_MAP)),
        LookArea(Res.string.look_area_ai, listOf(UiOptions.GROUP_ASSISTANT, UiOptions.GROUP_HEALTH)),
    )

/** The look that [group] really gets when the person asks for [look]: only floating groups can be glass. */
private fun lookFor(group: String, look: Int): Int =
    if (look == ElementLook.GLASS && group !in UiOptions.GLASS_GROUPS) ElementLook.TRANSPARENT else look

/** The one look all of [groups] have, or [MIXED] when they differ. */
private fun UiOptions.commonLook(groups: List<String>): Int {
    val candidates = listOf(ElementLook.FLAT, ElementLook.DEPTH, ElementLook.TRANSPARENT, ElementLook.GLASS)
    return candidates.firstOrNull { c -> groups.all { lookFor(it, c) == element(it).look } } ?: MIXED
}

/**
 * MT_SW: the simple choices of the look of the app: ready-made looks, the style of all icons, one look for the whole
 * app and one look for each part of it (bars, cards and lists, messages, text fields and search, windows, the map, the
 * assistant and network health). The finer settings are in [ElementAdvancedSettings]. [onChange] saves one option under
 * its key.
 */
@Composable
fun ElementBasicSettings(options: UiOptions, onChange: (String, Int) -> Unit) {
    Text(
        text = stringResource(Res.string.element_look_summary),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
    LookPreview(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    ElementPresets(onChange)
    LookBackup()
    Text(
        text = stringResource(Res.string.look_basic_summary),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    AreaLookPicker(stringResource(Res.string.look_all), UiOptions.ALL_GROUPS, options, onChange)
    for (area in LOOK_AREAS) AreaLookPicker(stringResource(area.title), area.groups, options, onChange)
    if (options.element(UiOptions.GROUP_DIALOGS).look != ElementLook.FLAT) {
        SliderPreference(
            title = stringResource(Res.string.dialog_see_through),
            enabled = true,
            items = (0..STRENGTH_MAX step STRENGTH_STEP).map { it to "$it%" },
            selectedValue = options.element(UiOptions.GROUP_DIALOGS).strength,
            onValueChange = { onChange("${UiOptions.GROUP_DIALOGS}.strength", it) },
            summary = stringResource(Res.string.dialog_see_through_summary),
        )
    }
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
    SwitchListItem(
        text = stringResource(Res.string.ring_style_thin),
        supportingText = stringResource(Res.string.ring_style_thin_summary),
        leadingIcon = MeshtasticIcons.DisplaySettings,
        checked = options[UiOptions.RING_STYLE] == UiOptions.RING_STYLE_THIN,
        onClick = {
            val thin = options[UiOptions.RING_STYLE] == UiOptions.RING_STYLE_THIN
            onChange(UiOptions.RING_STYLE, if (thin) UiOptions.RING_STYLE_CLASSIC else UiOptions.RING_STYLE_THIN)
        },
    )
    SwitchListItem(
        text = stringResource(Res.string.control_style_thin),
        supportingText = stringResource(Res.string.control_style_thin_summary),
        leadingIcon = MeshtasticIcons.DisplaySettings,
        checked = options[UiOptions.CONTROL_STYLE] == UiOptions.CONTROL_STYLE_THIN,
        onClick = {
            val thin = options[UiOptions.CONTROL_STYLE] == UiOptions.CONTROL_STYLE_THIN
            onChange(
                UiOptions.CONTROL_STYLE,
                if (thin) UiOptions.CONTROL_STYLE_CLASSIC else UiOptions.CONTROL_STYLE_THIN,
            )
        },
    )
    DropDownPreference(
        title = stringResource(Res.string.button_style),
        enabled = true,
        items =
        listOf(
            UiOptions.BUTTON_STYLE_OUTLINE_GLASS to stringResource(Res.string.button_style_outline_glass),
            UiOptions.BUTTON_STYLE_GLASS to stringResource(Res.string.button_style_glass),
            UiOptions.BUTTON_STYLE_CLASSIC to stringResource(Res.string.button_style_classic),
        ),
        selectedItem = options[UiOptions.BUTTON_STYLE],
        onItemSelected = { onChange(UiOptions.BUTTON_STYLE, it) },
        summary = stringResource(Res.string.button_style_summary),
    )
    ChartStyleSettings(options, onChange)
    NodeChipStyleSettings(options, onChange)
}

/** One look for all the groups of a part of the app. */
@Composable
private fun AreaLookPicker(title: String, groups: List<String>, options: UiOptions, onChange: (String, Int) -> Unit) {
    val current = options.commonLook(groups)
    val canBeGlass = groups.any { it in UiOptions.GLASS_GROUPS }
    val looks = buildList {
        add(ElementLook.FLAT to stringResource(Res.string.element_look_flat))
        add(ElementLook.DEPTH to stringResource(Res.string.element_look_depth))
        add(ElementLook.TRANSPARENT to stringResource(Res.string.element_look_transparent))
        if (canBeGlass) add(ElementLook.GLASS to stringResource(Res.string.element_look_glass))
        if (current == MIXED) add(MIXED to stringResource(Res.string.look_mixed))
    }
    DropDownPreference(
        title = title,
        enabled = true,
        items = looks,
        selectedItem = current,
        onItemSelected = { picked ->
            if (picked != MIXED) groups.forEach { onChange("$it.look", lookFor(it, picked)) }
        },
    )
}

/**
 * MT_SW: the finer settings of the look, folded away until the person opens them: the look, strength and blur of every
 * group of elements separately, and the colour and edge of the glass.
 */
@Composable
fun ElementAdvancedSettings(options: UiOptions, onChange: (String, Int) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ListItem(
        text = stringResource(Res.string.look_advanced),
        supportingText = stringResource(Res.string.look_advanced_summary),
        leadingIcon = MeshtasticIcons.DisplaySettings,
    ) {
        expanded = !expanded
    }
    if (expanded) {
        ElementGroupSettings(stringResource(Res.string.element_group_navbar), UiOptions.GROUP_NAV, options, onChange)
        ElementGroupSettings(stringResource(Res.string.element_group_topbar), UiOptions.GROUP_TOPBAR, options, onChange)
        ElementGroupSettings(stringResource(Res.string.element_group_cards), UiOptions.GROUP_CARDS, options, onChange)
        ElementGroupSettings(
            stringResource(Res.string.element_group_details),
            UiOptions.GROUP_DETAILS,
            options,
            onChange,
        )
        ElementGroupSettings(
            stringResource(Res.string.element_group_bubbles),
            UiOptions.GROUP_BUBBLES,
            options,
            onChange,
        )
        ElementGroupSettings(stringResource(Res.string.element_group_input), UiOptions.GROUP_INPUT, options, onChange)
        ElementGroupSettings(stringResource(Res.string.element_group_search), UiOptions.GROUP_SEARCH, options, onChange)
        ElementGroupSettings(
            stringResource(Res.string.element_group_dialogs),
            UiOptions.GROUP_DIALOGS,
            options,
            onChange,
        )
        ElementGroupSettings(stringResource(Res.string.element_group_map), UiOptions.GROUP_MAP, options, onChange)
        ElementGroupSettings(
            stringResource(Res.string.element_group_assistant),
            UiOptions.GROUP_ASSISTANT,
            options,
            onChange,
        )
        ElementGroupSettings(
            stringResource(Res.string.element_group_health),
            UiOptions.GROUP_HEALTH,
            options,
            onChange,
        )
        ElementGroupSettings(
            stringResource(Res.string.element_group_settings),
            UiOptions.GROUP_SETTINGS,
            options,
            onChange,
        )
        GlassDetailSettings(options, onChange)
    }
}

/** How the radio chooser of the top bar tells that a radio dropped: the way, the colour, the speed, the number. */
@Composable
fun RadioChooserSettings(options: UiOptions, onChange: (String, Int) -> Unit) {
    DropDownPreference(
        title = stringResource(Res.string.radio_alert),
        enabled = true,
        items =
        listOf(
            UiOptions.RADIO_ALERT_OFF to stringResource(Res.string.radio_alert_off),
            UiOptions.RADIO_ALERT_DIGIT to stringResource(Res.string.radio_alert_digit),
            UiOptions.RADIO_ALERT_PULSE to stringResource(Res.string.radio_alert_pulse),
        ),
        selectedItem = options[UiOptions.RADIO_ALERT],
        onItemSelected = { onChange(UiOptions.RADIO_ALERT, it) },
    )
    if (options[UiOptions.RADIO_ALERT] == UiOptions.RADIO_ALERT_PULSE) {
        DropDownPreference(
            title = stringResource(Res.string.radio_alert_color),
            enabled = true,
            items =
            listOf(
                0 to stringResource(Res.string.radio_alert_color_white),
                1 to stringResource(Res.string.radio_alert_color_purple),
                2 to stringResource(Res.string.radio_alert_color_theme),
                3 to stringResource(Res.string.radio_alert_color_amber),
                4 to stringResource(Res.string.radio_alert_color_red),
            ),
            selectedItem = options[UiOptions.RADIO_ALERT_COLOR],
            onItemSelected = { onChange(UiOptions.RADIO_ALERT_COLOR, it) },
        )
        DropDownPreference(
            title = stringResource(Res.string.radio_alert_speed),
            enabled = true,
            items =
            listOf(
                0 to stringResource(Res.string.radio_alert_speed_slow),
                1 to stringResource(Res.string.radio_alert_speed_medium),
                2 to stringResource(Res.string.radio_alert_speed_fast),
            ),
            selectedItem = options[UiOptions.RADIO_ALERT_SPEED],
            onItemSelected = { onChange(UiOptions.RADIO_ALERT_SPEED, it) },
        )
    }
    DropDownPreference(
        title = stringResource(Res.string.radio_count),
        enabled = true,
        items =
        listOf(
            1 to stringResource(Res.string.radio_count_show),
            0 to stringResource(Res.string.radio_count_hide),
        ),
        selectedItem = options[UiOptions.RADIO_COUNT],
        onItemSelected = { onChange(UiOptions.RADIO_COUNT, it) },
    )
    DropDownPreference(
        title = stringResource(Res.string.radio_background),
        enabled = true,
        items =
        listOf(
            0 to stringResource(Res.string.radio_background_off),
            1 to stringResource(Res.string.radio_background_on),
        ),
        selectedItem = options[UiOptions.RADIO_BACKGROUND],
        onItemSelected = { onChange(UiOptions.RADIO_BACKGROUND, it) },
    )
}

/** The look, the strength and (for glass) the blur of one group of elements. */
@Composable
private fun ElementGroupSettings(title: String, group: String, options: UiOptions, onChange: (String, Int) -> Unit) {
    val style = options.element(group)
    val canBeGlass = group in UiOptions.GLASS_GROUPS
    val looks = buildList {
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

/** How the floating bar hides and comes back, its icons, the vibration of a tab change and of the held send button. */
@Composable
fun NavBarBehaviourSettings(options: UiOptions, onChange: (String, Int) -> Unit) {
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
    DropDownPreference(
        title = stringResource(Res.string.nav_bar_select),
        enabled = true,
        items =
        listOf(
            UiOptions.NAV_SELECT_BUBBLE to stringResource(Res.string.nav_select_bubble),
            UiOptions.NAV_SELECT_LINE to stringResource(Res.string.nav_select_line),
            UiOptions.NAV_SELECT_BOTH to stringResource(Res.string.nav_select_both),
        ),
        selectedItem = options[UiOptions.NAV_SELECT],
        onItemSelected = { onChange(UiOptions.NAV_SELECT, it) },
    )
    SliderPreference(
        title = stringResource(Res.string.nav_bar_rim),
        enabled = true,
        items = UiOptions.RIM_STEPS_DP.indices.map { it to "${UiOptions.RIM_STEPS_DP[it]} dp" },
        selectedValue = options[UiOptions.NAV_RIM],
        onValueChange = { onChange(UiOptions.NAV_RIM, it) },
    )
    SwitchListItem(
        text = stringResource(Res.string.nav_bar_shadow),
        leadingIcon = MeshtasticIcons.DisplaySettings,
        checked = options.flag(UiOptions.NAV_SHADOW),
        onClick = { onChange(UiOptions.NAV_SHADOW, if (options.flag(UiOptions.NAV_SHADOW)) 0 else 1) },
    )
    SwitchListItem(
        text = stringResource(Res.string.input_hold_buzz),
        leadingIcon = MeshtasticIcons.DisplaySettings,
        checked = options.flag(UiOptions.HOLD_BUZZ),
        onClick = { onChange(UiOptions.HOLD_BUZZ, if (options.flag(UiOptions.HOLD_BUZZ)) 0 else 1) },
    )
    SwitchListItem(
        text = stringResource(Res.string.press_spring),
        leadingIcon = MeshtasticIcons.DisplaySettings,
        checked = options.flag(UiOptions.PRESS_SPRING),
        onClick = { onChange(UiOptions.PRESS_SPRING, if (options.flag(UiOptions.PRESS_SPRING)) 0 else 1) },
    )
}

/** One ready-made set of looks: the options it sets, each with its value. */
private class LookPreset(val title: StringResource, val values: Map<String, Int>)

private fun look(groups: List<String>, look: Int, strength: Int, blur: Int = 20): Map<String, Int> =
    groups.fold(emptyMap()) { acc, group ->
        acc + mapOf("$group.look" to lookFor(group, look), "$group.strength" to strength, "$group.blur" to blur)
    }

private const val PRESET_BLUR = 28

private val BARS = listOf(UiOptions.GROUP_NAV, UiOptions.GROUP_TOPBAR)
private val CARDS = listOf(UiOptions.GROUP_CARDS, UiOptions.GROUP_DETAILS, UiOptions.GROUP_SETTINGS)
private val FIELDS = listOf(UiOptions.GROUP_INPUT, UiOptions.GROUP_SEARCH)
private val AI = listOf(UiOptions.GROUP_ASSISTANT, UiOptions.GROUP_HEALTH)
private val BUBBLES = listOf(UiOptions.GROUP_BUBBLES)
private val DIALOGS = listOf(UiOptions.GROUP_DIALOGS)
private val MAP = listOf(UiOptions.GROUP_MAP)

private val LOOK_PRESETS =
    listOf(
        LookPreset(
            Res.string.look_preset_classic,
            mapOf(
                UiOptions.ICON_STYLE to UiOptions.ICON_STYLE_STANDARD,
                UiOptions.RING_STYLE to UiOptions.RING_STYLE_CLASSIC,
                UiOptions.CONTROL_STYLE to UiOptions.CONTROL_STYLE_CLASSIC,
                UiOptions.BUTTON_STYLE to UiOptions.BUTTON_STYLE_CLASSIC,
                UiOptions.CHART_STYLE to UiOptions.CHART_STYLE_FLAT,
                UiOptions.PRESS_SPRING to 0,
            ) + look(UiOptions.ALL_GROUPS, ElementLook.FLAT, 55),
        ),
        LookPreset(
            Res.string.look_preset_iphone,
            mapOf(
                UiOptions.ICON_STYLE to UiOptions.ICON_STYLE_OUTLINE,
                UiOptions.RING_STYLE to UiOptions.RING_STYLE_THIN,
                UiOptions.CONTROL_STYLE to UiOptions.CONTROL_STYLE_THIN,
                UiOptions.BUTTON_STYLE to UiOptions.BUTTON_STYLE_OUTLINE_GLASS,
                UiOptions.CHART_STYLE to UiOptions.CHART_STYLE_GLASS,
                UiOptions.PRESS_SPRING to 1,
            ) +
                look(BARS, ElementLook.GLASS, 45, PRESET_BLUR) +
                look(CARDS, ElementLook.DEPTH, 45) +
                look(BUBBLES, ElementLook.DEPTH, 40) +
                look(FIELDS, ElementLook.GLASS, 50, PRESET_BLUR) +
                look(DIALOGS, ElementLook.GLASS, 40, PRESET_BLUR) +
                look(MAP, ElementLook.GLASS, 45, PRESET_BLUR) +
                look(AI, ElementLook.DEPTH, 40),
        ),
        LookPreset(
            Res.string.look_preset_glass,
            mapOf(UiOptions.PRESS_SPRING to 1, UiOptions.CHART_STYLE to UiOptions.CHART_STYLE_GLASS) +
                look(BARS, ElementLook.GLASS, 55, PRESET_BLUR) +
                look(CARDS, ElementLook.TRANSPARENT, 45) +
                look(BUBBLES, ElementLook.TRANSPARENT, 35) +
                look(FIELDS, ElementLook.GLASS, 55, PRESET_BLUR) +
                look(DIALOGS, ElementLook.GLASS, 45, PRESET_BLUR) +
                look(MAP, ElementLook.GLASS, 55, PRESET_BLUR) +
                look(AI, ElementLook.TRANSPARENT, 40),
        ),
        LookPreset(
            Res.string.look_preset_depth,
            mapOf(UiOptions.PRESS_SPRING to 1, UiOptions.CHART_STYLE to UiOptions.CHART_STYLE_GRADIENT) +
                look(UiOptions.ALL_GROUPS, ElementLook.DEPTH, 60),
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
        ListItem(text = stringResource(preset.title), leadingIcon = MeshtasticIcons.DisplaySettings) {
            preset.values.forEach { (key, value) -> onChange(key, value) }
        }
    }
}

/**
 * Saves the whole look to a file and brings it back, so a look can always be returned to or moved to another device.
 */
@Composable
private fun LookBackup() {
    val backup = rememberSettingsBackupActions()
    Text(
        text = stringResource(Res.string.look_backup_title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    Text(
        text = stringResource(Res.string.look_backup_summary),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    ListItem(
        text = stringResource(Res.string.backup_look),
        leadingIcon = MeshtasticIcons.Upload,
        onClick = backup.onBackupLook,
    )
    ListItem(
        text = stringResource(Res.string.restore_look),
        leadingIcon = MeshtasticIcons.Download,
        onClick = backup.onRestoreLook,
    )
}

/** The colour of the glass and the thickness of its edge. */
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
    SliderPreference(
        title = stringResource(Res.string.glass_shadow),
        enabled = true,
        items =
        UiOptions.GLASS_SHADOW_STEPS_DP.indices.map { it to "${UiOptions.GLASS_SHADOW_STEPS_DP[it].toInt()} dp" },
        selectedValue = options[UiOptions.GLASS_SHADOW],
        onValueChange = { onChange(UiOptions.GLASS_SHADOW, it) },
    )
    val levels =
        listOf(
            stringResource(Res.string.brightness_1),
            stringResource(Res.string.brightness_2),
            stringResource(Res.string.brightness_3),
            stringResource(Res.string.brightness_4),
            stringResource(Res.string.brightness_5),
        )
    SliderPreference(
        title = stringResource(Res.string.brightness_general),
        enabled = true,
        items = levels.mapIndexed { index, label -> index to label },
        selectedValue = options[UiOptions.BRIGHTNESS],
        onValueChange = { onChange(UiOptions.BRIGHTNESS, it) },
    )
    SliderPreference(
        title = stringResource(Res.string.brightness_buttons),
        enabled = true,
        items =
        listOf(0 to stringResource(Res.string.brightness_as_general)) +
            levels.mapIndexed { index, label -> index + 1 to label },
        selectedValue = options[UiOptions.BUTTON_BRIGHTNESS],
        onValueChange = { onChange(UiOptions.BUTTON_BRIGHTNESS, it) },
    )
    SliderPreference(
        title = stringResource(Res.string.button_glow),
        enabled = true,
        items =
        listOf(
            0 to stringResource(Res.string.button_gradient_off),
            1 to stringResource(Res.string.button_gradient_weak),
            2 to stringResource(Res.string.button_gradient_normal),
            3 to stringResource(Res.string.button_gradient_strong),
        ),
        selectedValue = options[UiOptions.BUTTON_GLOW],
        onValueChange = { onChange(UiOptions.BUTTON_GLOW, it) },
    )
    SliderPreference(
        title = stringResource(Res.string.button_outline),
        enabled = true,
        items = UiOptions.BUTTON_OUTLINE_DP.indices.map { it to "${UiOptions.BUTTON_OUTLINE_DP[it]} dp" },
        selectedValue = options[UiOptions.BUTTON_OUTLINE],
        onValueChange = { onChange(UiOptions.BUTTON_OUTLINE, it) },
    )
    SliderPreference(
        title = stringResource(Res.string.button_gradient),
        enabled = true,
        items =
        listOf(
            0 to stringResource(Res.string.button_gradient_off),
            1 to stringResource(Res.string.button_gradient_weak),
            2 to stringResource(Res.string.button_gradient_normal),
            3 to stringResource(Res.string.button_gradient_strong),
        ),
        selectedValue = options[UiOptions.BUTTON_GRADIENT],
        onValueChange = { onChange(UiOptions.BUTTON_GRADIENT, it) },
    )
}
