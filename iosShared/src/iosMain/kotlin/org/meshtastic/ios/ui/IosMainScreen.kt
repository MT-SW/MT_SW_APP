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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
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

private val TOP_TRIM = 14.dp

/**
 * iPhone main screen — assembles the shared [MeshtasticAppShell], [MeshtasticNavigationSuite], and
 * [MeshtasticNavDisplay] with the iPhone-specific [iosNavGraph] entry provider.
 */
@Suppress("ViewModelForwarding")
@Composable
fun IosMainScreen(uiViewModel: UIViewModel, multiBackstack: MultiBackstack, modifier: Modifier = Modifier) {
    val backStack = multiBackstack.activeBackStack
    val settingsRadioConfigViewModelProvider = rememberSettingsRadioConfigViewModelProvider(backStack)

    // The background is painted over the whole screen, including the notch area; only the content stays inside it.
    Surface(modifier = modifier.fillMaxSize()) {
      // The top gap is a bit smaller than the system's safe area (the content still stays clear of the notch).
      val topGap = (WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() - TOP_TRIM).coerceAtLeast(0.dp)
      Box(
          Modifier.fillMaxSize()
              .padding(top = topGap)
              .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
              .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
      ) {
        MeshtasticAppShell(
            multiBackstack = multiBackstack,
            uiViewModel = uiViewModel,
            hostModifier = Modifier,
        ) {
            MeshtasticNavigationSuite(
                multiBackstack = multiBackstack,
                uiViewModel = uiViewModel,
                // The bar does not add space for the home indicator, so it sits at the bottom of the screen.
                modifier = Modifier.fillMaxSize().consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
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
}
