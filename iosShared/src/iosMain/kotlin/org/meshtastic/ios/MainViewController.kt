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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import org.meshtastic.core.ui.theme.rememberScaledDensity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import co.touchlab.kermit.Logger
import org.koin.core.Koin
import org.koin.plugin.module.dsl.startKoin
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.model.DeviceAddress
import org.meshtastic.core.navigation.TopLevelDestination
import org.meshtastic.core.navigation.rememberMultiBackstack
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.service.MeshLogCleanup
import org.meshtastic.core.service.MeshServiceOrchestrator
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.util.LocalTerminateApplication
import platform.posix.exit
import org.meshtastic.core.ui.theme.MODE_MONO_DARK
import org.meshtastic.core.ui.theme.MODE_MONO_LIGHT
import org.meshtastic.core.ui.theme.isMonochromeMode
import org.meshtastic.core.ui.viewmodel.UIViewModel
import org.meshtastic.feature.intro.IosWelcomeScreen
import org.meshtastic.ios.di.IosKoinApp
import org.meshtastic.ios.map.ProvideIosMaps
import org.meshtastic.ios.ui.IosMainScreen
import org.meshtastic.ios.worker.IosNodeCleanupScheduler
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Starts the dependency graph once for the whole life of the app process. */
private val koin: Koin by lazy {
    IosFileLog.install()
    startKoin<IosKoinApp> {}.koin
}

/** One shared screen model for the whole app, also used by links opened from outside. */
private val sharedUiViewModel: UIViewModel by lazy { koin.get<UIViewModel>(UIViewModel::class) }

/** Called from the iPhone app when a link (e.g. a shared channel or contact) is opened. */
fun handleIosDeepLink(url: String) {
    sharedUiViewModel.handleDeepLink(CommonUri.parse(url)) { Logger.e { "Invalid link from iOS: $url" } }
}

/** The single entry the iOS app (iosApp) calls: the whole screen is drawn by Compose Multiplatform. */
fun MainViewController() =
    ComposeUIViewController(
        // The system must not shift the whole screen up when a text field is focused (that pushed the top bar out of
        // sight and left the field under the keyboard); the screen lifts itself above the keyboard instead.
        configure = { onFocusBehavior = OnFocusBehavior.DoNothing },
    ) {
    val uiViewModel = sharedUiViewModel
    org.koin.compose.KoinContext(koin) {
        MeshServiceLifecycle()
        BackgroundCleanup()
        ThemedApp(uiViewModel)
    }
}

/** Starts the radio service with the screen and stops it with it. */
@Composable
private fun MeshServiceLifecycle() {
    val orchestrator = remember { koin.get<MeshServiceOrchestrator>(MeshServiceOrchestrator::class) }
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
    val scheduler = remember { koin.get<IosNodeCleanupScheduler>(IosNodeCleanupScheduler::class) }
    val logCleanup = remember { koin.get<MeshLogCleanup>(MeshLogCleanup::class) }
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
    val uiPrefs = remember { koin.get<UiPrefs>(UiPrefs::class) }
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
    val uiScale by uiViewModel.uiScale.collectAsState()
    // "Force stop" like on Android/desktop: disconnect the radio and stop the service, then end the process.
    val terminate: () -> Unit = {
        runCatching { koin.get<MeshServiceOrchestrator>(MeshServiceOrchestrator::class).stop() }
        exit(0)
    }
    CompositionLocalProvider(
        LocalDensity provides rememberScaledDensity(percent = uiScale),
        LocalTerminateApplication provides terminate,
    ) {
    AppTheme(darkTheme = isDarkTheme, monochrome = isMonochromeMode(themePref)) {
        IosCoilSetup()
        val introCompleted by uiViewModel.appIntroCompleted.collectAsState()
        if (introCompleted) {
            ProvideIosMaps { IosMainScreen(uiViewModel, multiBackstack) }
        } else {
            IosWelcomeScreen(onDone = { uiViewModel.onAppIntroCompleted() })
        }
    }
    }
}
