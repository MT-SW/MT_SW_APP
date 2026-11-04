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
package org.meshtastic.ios.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import org.meshtastic.core.navigation.MultiBackstack
import org.meshtastic.core.ui.component.MeshtasticAppShell
import org.meshtastic.core.ui.component.MeshtasticNavDisplay
import org.meshtastic.core.ui.component.MeshtasticNavigationSuite
import org.meshtastic.core.ui.viewmodel.UIViewModel
import org.meshtastic.ios.navigation.iosNavGraph
import org.meshtastic.feature.settings.navigation.rememberSettingsRadioConfigViewModelProvider

private val BAR_LOWER_BY = 20.dp

/**
 * iPhone main screen — assembles the shared [MeshtasticAppShell], [MeshtasticNavigationSuite], and
 * [MeshtasticNavDisplay] with the iPhone-specific [iosNavGraph] entry provider.
 */
@Suppress("ViewModelForwarding")
@Composable
fun IosMainScreen(uiViewModel: UIViewModel, multiBackstack: MultiBackstack, modifier: Modifier = Modifier) {
    val backStack = multiBackstack.activeBackStack
    val settingsRadioConfigViewModelProvider = rememberSettingsRadioConfigViewModelProvider(backStack)

    Surface(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
        MeshtasticAppShell(
            multiBackstack = multiBackstack,
            uiViewModel = uiViewModel,
            hostModifier = Modifier,
        ) {
            MeshtasticNavigationSuite(
                multiBackstack = multiBackstack,
                uiViewModel = uiViewModel,
                // The bar keeps only part of the space reserved for the home indicator, so it sits lower on the screen.
                modifier = Modifier.fillMaxSize().consumeWindowInsets(PaddingValues(bottom = BAR_LOWER_BY)),
            ) {
                val provider =
                    entryProvider<NavKey> {
                        iosNavGraph(
                            backStack = backStack,
                            uiViewModel = uiViewModel,
                            multiBackstack = multiBackstack,
                            settingsRadioConfigViewModel = settingsRadioConfigViewModelProvider,
                        )
                    }
                MeshtasticNavDisplay(
                    multiBackstack = multiBackstack,
                    entryProvider = provider,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
