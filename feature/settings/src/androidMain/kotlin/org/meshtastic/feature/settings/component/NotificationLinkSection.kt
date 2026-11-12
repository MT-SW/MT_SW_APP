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

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.app_notifications
import org.meshtastic.core.resources.notification_settings_android
import org.meshtastic.core.resources.notification_settings_android_hint
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Notifications

/**
 * Link to the Android notification settings of the app. The system lists every notification category of the app there
 * (messages, alarms, new nodes, battery and others), and for each one the sound, vibration, importance and the lock
 * screen can be set.
 */
@Suppress("ModifierMissing")
@Composable
fun NotificationLinkSection() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {}

    ExpressiveSection(title = stringResource(Res.string.app_notifications)) {
        ListItem(
            text = stringResource(Res.string.notification_settings_android),
            supportingText = stringResource(Res.string.notification_settings_android_hint),
            leadingIcon = MeshtasticIcons.Notifications,
            trailingIcon = null,
        ) {
            val intent =
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            launcher.launch(intent)
        }
    }
}
