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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.app_update_check_summary
import org.meshtastic.core.resources.app_update_check_title
import org.meshtastic.core.resources.app_update_tests_summary
import org.meshtastic.core.resources.app_update_tests_title
import org.meshtastic.core.ui.component.SwitchPreference

/** The two switches of the MT_SW_APP update notifications: the check itself, and whether test versions count too. */
@Composable
internal fun AppUpdateSettings() {
    val uiPrefs = koinInject<UiPrefs>()
    val checkEnabled by uiPrefs.appUpdateCheckEnabled.collectAsStateWithLifecycle()
    val notifyTests by uiPrefs.appUpdateNotifyTests.collectAsStateWithLifecycle()

    SwitchPreference(
        title = stringResource(Res.string.app_update_check_title),
        summary = stringResource(Res.string.app_update_check_summary),
        checked = checkEnabled,
        enabled = true,
        onCheckedChange = uiPrefs::setAppUpdateCheckEnabled,
    )
    SwitchPreference(
        title = stringResource(Res.string.app_update_tests_title),
        summary = stringResource(Res.string.app_update_tests_summary),
        checked = notifyTests,
        enabled = checkEnabled,
        onCheckedChange = uiPrefs::setAppUpdateNotifyTests,
    )
}
