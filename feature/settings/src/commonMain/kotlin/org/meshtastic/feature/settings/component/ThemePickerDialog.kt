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
@file:Suppress("MatchingDeclarationName")

package org.meshtastic.feature.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.choose_theme
import org.meshtastic.core.resources.dynamic
import org.meshtastic.core.resources.theme_dark
import org.meshtastic.core.resources.theme_group_dark
import org.meshtastic.core.resources.theme_group_light
import org.meshtastic.core.resources.theme_group_other
import org.meshtastic.core.resources.theme_group_system
import org.meshtastic.core.resources.theme_light
import org.meshtastic.core.resources.theme_mono_dark
import org.meshtastic.core.resources.theme_mono_light
import org.meshtastic.core.resources.theme_system
import org.meshtastic.core.resources.theme_very_vivid
import org.meshtastic.core.resources.theme_very_vivid_dark
import org.meshtastic.core.resources.theme_very_vivid_light
import org.meshtastic.core.resources.theme_vivid
import org.meshtastic.core.resources.theme_vivid_dark
import org.meshtastic.core.resources.theme_vivid_light
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.component.verticalScrollWithBar
import org.meshtastic.core.ui.icon.Check
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.theme.MODE_DARK
import org.meshtastic.core.ui.theme.MODE_DYNAMIC
import org.meshtastic.core.ui.theme.MODE_LIGHT
import org.meshtastic.core.ui.theme.MODE_MONO_DARK
import org.meshtastic.core.ui.theme.MODE_MONO_LIGHT
import org.meshtastic.core.ui.theme.MODE_VERY_VIVID
import org.meshtastic.core.ui.theme.MODE_VERY_VIVID_DARK
import org.meshtastic.core.ui.theme.MODE_VERY_VIVID_LIGHT
import org.meshtastic.core.ui.theme.MODE_VIVID
import org.meshtastic.core.ui.theme.MODE_VIVID_DARK
import org.meshtastic.core.ui.theme.MODE_VIVID_LIGHT
import org.meshtastic.core.ui.theme.themePreviewColors

/** Theme modes that match AppCompatDelegate constants for cross-platform use. */
enum class ThemeOption(val label: StringResource, val mode: Int) {
    DYNAMIC(label = Res.string.dynamic, mode = MODE_DYNAMIC),
    VIVID(label = Res.string.theme_vivid, mode = MODE_VIVID),
    VIVID_LIGHT(label = Res.string.theme_vivid_light, mode = MODE_VIVID_LIGHT),
    VIVID_DARK(label = Res.string.theme_vivid_dark, mode = MODE_VIVID_DARK),
    VERY_VIVID(label = Res.string.theme_very_vivid, mode = MODE_VERY_VIVID),
    VERY_VIVID_LIGHT(label = Res.string.theme_very_vivid_light, mode = MODE_VERY_VIVID_LIGHT),
    VERY_VIVID_DARK(label = Res.string.theme_very_vivid_dark, mode = MODE_VERY_VIVID_DARK),
    LIGHT(label = Res.string.theme_light, mode = MODE_LIGHT),
    DARK(label = Res.string.theme_dark, mode = MODE_DARK),
    SYSTEM(label = Res.string.theme_system, mode = -1), // AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    MONO_LIGHT(label = Res.string.theme_mono_light, mode = MODE_MONO_LIGHT),
    MONO_DARK(label = Res.string.theme_mono_dark, mode = MODE_MONO_DARK),
}

/** Picker categories, in the order they are shown: dark, light, like the system, other. */
private enum class ThemeGroup(val title: StringResource, val options: List<ThemeOption>) {
    DARK(
        title = Res.string.theme_group_dark,
        options = listOf(ThemeOption.DARK, ThemeOption.VIVID_DARK, ThemeOption.VERY_VIVID_DARK, ThemeOption.MONO_DARK),
    ),
    LIGHT(
        title = Res.string.theme_group_light,
        options =
        listOf(ThemeOption.LIGHT, ThemeOption.VIVID_LIGHT, ThemeOption.VERY_VIVID_LIGHT, ThemeOption.MONO_LIGHT),
    ),
    SYSTEM(
        title = Res.string.theme_group_system,
        options = listOf(ThemeOption.SYSTEM, ThemeOption.VIVID, ThemeOption.VERY_VIVID),
    ),
    OTHER(title = Res.string.theme_group_other, options = listOf(ThemeOption.DYNAMIC)),
}

/** The name of the theme with this [mode], for the line that opens the picker (null for an unknown value). */
@Composable
fun themeSummary(mode: Int): String? =
    ThemeOption.entries.firstOrNull { it.mode == mode }?.let { stringResource(it.label) }

/** Shared dialog for picking a theme option. Used by both Android and Desktop settings screens. */
@Composable
fun ThemePickerDialog(currentMode: Int, onClickTheme: (Int) -> Unit, onDismiss: () -> Unit) {
    MeshtasticDialog(
        title = stringResource(Res.string.choose_theme),
        onDismiss = onDismiss,
        text = {
            Column(modifier = Modifier.verticalScrollWithBar(rememberScrollState())) {
                ThemeGroup.entries.forEach { group ->
                    Text(
                        text = stringResource(group.title),
                        // Category headers: gold (the theme's primary colour) and larger than the options below.
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                    )
                    group.options.forEach { option ->
                        // The theme in use: a tick and the accent colour, so it can be seen when the list opens.
                        val selected = option.mode == currentMode
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ThemeSwatch(option.mode, Modifier.padding(start = 16.dp))
                            ListItem(
                                text = stringResource(option.label),
                                modifier = Modifier.weight(1f),
                                textColor = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                trailingIcon = if (selected) MeshtasticIcons.Check else null,
                                trailingIconTint =
                                if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                            ) {
                                onClickTheme(option.mode)
                                onDismiss()
                            }
                        }
                    }
                }
            }
        },
    )
}

/** A small circle with the theme's background and its accent colour, so the themes can be told apart at a glance. */
@Composable
private fun ThemeSwatch(mode: Int, modifier: Modifier = Modifier) {
    val colors = themePreviewColors(mode)
    // The wallpaper theme has no fixed colours: a ring in the colours of the theme in use stands for it.
    val background = colors?.first ?: MaterialTheme.colorScheme.surfaceVariant
    val accent = colors?.second ?: MaterialTheme.colorScheme.primary
    Box(
        modifier =
        modifier
            .size(SWATCH_SIZE_DP.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size((SWATCH_SIZE_DP / 2).dp).clip(CircleShape).background(accent))
    }
}

private const val SWATCH_SIZE_DP = 24
