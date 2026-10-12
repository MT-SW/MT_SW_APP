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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.network.update.AppUpdateService
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.app_update_dialog_message
import org.meshtastic.core.resources.app_update_dialog_test_message
import org.meshtastic.core.resources.app_update_open_releases
import org.meshtastic.core.resources.app_update_title
import org.meshtastic.core.resources.close
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.util.rememberOpenUrl

/**
 * The window behind the "new version" notification. A desktop notification cannot open a web page, so clicking it only
 * brings the app forward; this window then offers the button to the releases page.
 */
@Composable
internal fun AppUpdateDialog() {
    val service = koinInject<AppUpdateService>()
    val update by service.pendingUpdate.collectAsState()
    val openUrl = rememberOpenUrl()
    val pending = update ?: return
    MeshtasticDialog(
        title = stringResource(Res.string.app_update_title),
        message =
        stringResource(
            if (pending.isTest) Res.string.app_update_dialog_test_message else Res.string.app_update_dialog_message,
            pending.tag,
        ),
        confirmText = stringResource(Res.string.app_update_open_releases),
        onConfirm = {
            openUrl(pending.url)
            service.dismissPendingUpdate()
        },
        dismissText = stringResource(Res.string.close),
        onDismiss = { service.dismissPendingUpdate() },
    )
}
