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
package org.meshtastic.core.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.net.toUri
import org.meshtastic.core.navigation.DEEP_LINK_BASE_URI

/**
 * Quick Settings tile that opens the SOS screen. It only opens it: the alarm itself is sent by holding the button
 * there, so a stray tap on the tile cannot raise a false alarm.
 */
class SosQuickSettingsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = SOS_LABEL
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (isSecure && isLocked) unlockAndRun(::openSos) else openSos()
    }

    private fun openSos() {
        val intent =
            Intent(Intent.ACTION_VIEW, "$DEEP_LINK_BASE_URI/sos".toUri())
                .setPackage(packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private companion object {
        /** The same in every language, so it needs no translation. */
        const val SOS_LABEL = "SOS"
    }
}
