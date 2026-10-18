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
package org.meshtastic.feature.intro

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val MAX_WIDTH = 640.dp

/**
 * Desktop welcome screen shown on the first start: the same two cards as on Android (what the app does, then what this
 * fork adds), without the permission screens, because the desktop needs no Bluetooth, location or notification
 * onboarding. The content is kept to a readable width on large windows.
 */
@Composable
fun DesktopWelcomeScreen(onDone: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (page == 0) {
            WelcomeScreen(onGetStarted = { page = 1 }, maxContentWidth = MAX_WIDTH)
        } else {
            MtSwFeaturesScreen(onContinue = onDone, maxContentWidth = MAX_WIDTH)
        }
    }
}
