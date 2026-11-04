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
package org.meshtastic.ios

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.core.Koin
import org.koin.plugin.module.dsl.startKoin
import org.meshtastic.core.model.DeviceAddress
import org.meshtastic.core.navigation.TopLevelDestination
import org.meshtastic.core.navigation.rememberMultiBackstack
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.service.MeshLogCleanup
import org.meshtastic.core.service.MeshServiceOrchestrator
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.theme.MODE_MONO_DARK
import org.meshtastic.core.ui.theme.MODE_MONO_LIGHT
import org.meshtastic.core.ui.theme.isMonochromeMode
import org.meshtastic.core.ui.viewmodel.UIViewModel
import org.meshtastic.ios.di.IosKoinApp
import org.meshtastic.ios.ui.IosMainScreen
import org.meshtastic.ios.worker.IosNodeCleanupScheduler
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Starts the dependency graph once for the whole life of the app process. */
private val koin: Koin by lazy { startKoin<IosKoinApp> {}.koin }

/** The single entry the iOS app (iosApp) calls: the whole screen is drawn by Compose Multiplatform. */
fun MainViewController() = ComposeUIViewController {
    val uiViewModel = remember { koin.get<UIViewModel>() }
    org.koin.compose.KoinContext(koin) {
        MeshServiceLifecycle()
        BackgroundCleanup()
        ThemedApp(uiViewModel)
    }
}

/** Starts the radio service with the screen and stops it with it. */
@Composable
private fun MeshServiceLifecycle() {
    val orchestrator = koinInject<MeshServiceOrchestrator>()
    DisposableEffect(Unit) {
        orchestrator.start()
        onDispose { orchestrator.stop() }
    }
}

private val NODE_CLEANUP_CHECK_INTERVAL = 1.hours
private val NODE_CLEANUP_START_DELAY = 1.minutes

/** Hourly clean-up of old logs and (when enabled) inactive nodes, while the app is open. */
@Composable
private fun BackgroundCleanup() {
    val scheduler = koinInject<IosNodeCleanupScheduler>()
    val logCleanup = koinInject<MeshLogCleanup>()
    LaunchedEffect(Unit) { withContext(Dispatchers.Default) { logCleanup.runHourly() } }
    LaunchedEffect(Unit) {
        delay(NODE_CLEANUP_START_DELAY)
        while (isActive) {
            scheduler.runIfDue()
            delay(NODE_CLEANUP_CHECK_INTERVAL)
        }
    }
}

/** Applies the theme chosen in the settings and shows the app. */
@Suppress("ViewModelForwarding")
@Composable
private fun ThemedApp(uiViewModel: UIViewModel) {
    val uiPrefs = koinInject<UiPrefs>()
    val themePref by uiPrefs.theme.collectAsState(initial = -1)
    val isDarkTheme =
        when (themePref) {
            1, MODE_MONO_LIGHT -> false
            2, MODE_MONO_DARK -> true
            else -> isSystemInDarkTheme()
        }
    val multiBackstack =
        rememberMultiBackstack(
            // First start or no radio chosen: connections; otherwise the node list.
            if (DeviceAddress.parse(uiViewModel.currentDeviceAddressFlow.value) == null) {
                TopLevelDestination.Connect.route
            } else {
                TopLevelDestination.Nodes.route
            },
        )
    AppTheme(darkTheme = isDarkTheme, monochrome = isMonochromeMode(themePref)) {
        IosMainScreen(uiViewModel, multiBackstack)
    }
}
