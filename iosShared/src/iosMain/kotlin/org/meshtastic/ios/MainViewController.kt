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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.koin.core.Koin
import org.koin.plugin.module.dsl.startKoin
import org.meshtastic.core.ble.configureIosBleBackground
import org.meshtastic.core.common.background.AppBackgroundControls
import org.meshtastic.core.common.log.AppLogControls
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.model.DeviceAddress
import org.meshtastic.core.navigation.TopLevelDestination
import org.meshtastic.core.navigation.rememberMultiBackstack
import org.meshtastic.core.prefs.ui.UiPrefsDefaults
import org.meshtastic.core.repository.NotificationPrefs
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.service.MeshLogCleanup
import org.meshtastic.core.service.MeshServiceOrchestrator
import org.meshtastic.core.service.stats.MeshStatsFeed
import org.meshtastic.core.ui.component.DataNoticeScreen
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.theme.EventTheme
import org.meshtastic.core.ui.theme.EventThemeToggle
import org.meshtastic.core.ui.theme.LocalEventTheme
import org.meshtastic.core.ui.theme.LocalEventThemeToggle
import org.meshtastic.core.ui.theme.LocalSystemDensity
import org.meshtastic.core.ui.theme.MODE_MONO_DARK
import org.meshtastic.core.ui.theme.MODE_MONO_LIGHT
import org.meshtastic.core.ui.theme.MODE_VIVID
import org.meshtastic.core.ui.theme.isMonochromeMode
import org.meshtastic.core.ui.theme.isVividDark
import org.meshtastic.core.ui.theme.isVividLight
import org.meshtastic.core.ui.theme.rememberScaledDensity
import org.meshtastic.core.ui.theme.saturationFor
import org.meshtastic.core.ui.util.LocalBarcodeScannerProvider
import org.meshtastic.core.ui.util.LocalBarcodeScannerSupported
import org.meshtastic.core.ui.util.LocalEventBranding
import org.meshtastic.core.ui.util.LocalTerminateApplication
import org.meshtastic.core.ui.util.LocalWipeApplicationData
import org.meshtastic.core.ui.util.accentColorOrNull
import org.meshtastic.core.ui.util.brandHighlightOrNull
import org.meshtastic.core.ui.util.brandPalette
import org.meshtastic.core.ui.util.rememberIosBarcodeScanner
import org.meshtastic.core.ui.viewmodel.UIViewModel
import org.meshtastic.feature.intro.IosWelcomeScreen
import org.meshtastic.ios.di.IosKoinApp
import org.meshtastic.ios.map.ProvideIosMaps
import org.meshtastic.ios.notification.IosNotificationSettingsSync
import org.meshtastic.ios.ui.IosMainScreen
import org.meshtastic.ios.widget.IosWidgetPublisher
import org.meshtastic.ios.worker.IosNodeCleanupScheduler
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.posix.exit
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Starts the dependency graph once for the whole life of the app process. */
private val koin: Koin by lazy {
    // The system gives no wallpaper colours on an iPhone: the saturated theme is the default look.
    UiPrefsDefaults.theme = MODE_VIVID
    IosFileLog.install()
    AppLogControls.current = IosLogControl
    IosLogControl.start()
    configureIosBleBackground()
    AppBackgroundControls.current = IosBackgroundKeepAlive
    IosBackgroundKeepAlive.restore()
    startKoin<IosKoinApp> {}.koin
}

/** One shared screen model for the whole app, also used by links opened from outside. */
private val sharedUiViewModel: UIViewModel by lazy { koin.get<UIViewModel>(UIViewModel::class) }

/** Called from the iPhone app when a link (e.g. a shared channel or contact) is opened. */
fun handleIosDeepLink(url: String) {
    sharedUiViewModel.handleDeepLink(CommonUri.parse(url)) { Logger.e { "Invalid link from iOS: $url" } }
}

/** The single entry the iOS app (iosApp) calls: the whole screen is drawn by Compose Multiplatform. */
// The name is called from Swift (iosApp), so it has to start with a capital letter.
@Suppress("FunctionName", "ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController(
    // The system must not shift the whole screen up when a text field is focused (that pushed the top bar out of
    // sight and left the field under the keyboard); the screen lifts itself above the keyboard instead.
    configure = { onFocusBehavior = OnFocusBehavior.DoNothing },
) {
    val uiViewModel = sharedUiViewModel
    MeshServiceLifecycle()
    BackgroundCleanup()
    WidgetPublishing()
    NotificationSettingsSync()
    AppUpdateLifecycle()
    ThemedApp(uiViewModel)
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

/** Keeps the notification switches of the iPhone settings (Settings → MT_SW) and of the app the same. */
@Composable
private fun NotificationSettingsSync() {
    val sync = remember { IosNotificationSettingsSync(koin.get<NotificationPrefs>(NotificationPrefs::class)) }
    LaunchedEffect(Unit) { sync.run() }
}

/** Keeps the statistics for the widget up to date while the app runs (the widget reads them from the App Group). */
@Composable
private fun WidgetPublishing() {
    @Suppress("LongMethod")
    val publisher = remember {
        IosWidgetPublisher(koin.get<MeshStatsFeed>(MeshStatsFeed::class), koin.get<UiPrefs>(UiPrefs::class))
    }
    LaunchedEffect(Unit) { withContext(Dispatchers.Default) { publisher.run() } }
}

/** Applies the theme chosen in the settings and shows the app. */
@Suppress("ViewModelForwarding", "LongMethod")
@Composable
private fun ThemedApp(uiViewModel: UIViewModel) {
    val uiPrefs = remember { koin.get<UiPrefs>(UiPrefs::class) }
    val themePref by uiPrefs.theme.collectAsState(initial = UiPrefsDefaults.theme)
    val isDarkTheme =
        when (themePref) {
            1,
            MODE_MONO_LIGHT,
            -> false

            2,
            MODE_MONO_DARK,
            -> true

            else -> isVividDark(themePref) || (!isVividLight(themePref) && isSystemInDarkTheme())
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
    // The map is a native view drawn in real screen points; it needs the density from before the in-app scale.
    val systemDensity = LocalDensity.current
    // Event firmware (an edition with its own colours) brands the app like on Android; the sheet can opt out.
    val eventEdition by uiViewModel.eventEdition.collectAsState()
    val eventThemeEnabled by uiViewModel.eventThemeEnabled.collectAsState()
    val eventTheme =
        remember(eventEdition, eventThemeEnabled) {
            eventEdition
                ?.takeIf { eventThemeEnabled }
                ?.let {
                    EventTheme(
                        accent = it.accentColorOrNull(),
                        highlight = it.brandHighlightOrNull(),
                        palette = it.brandPalette(),
                    )
                }
        }
    CompositionLocalProvider(
        LocalEventBranding provides eventEdition,
        LocalEventTheme provides eventTheme,
        LocalEventThemeToggle provides
            EventThemeToggle(enabled = eventThemeEnabled, onChange = uiViewModel::setEventThemeEnabled),
        LocalDensity provides rememberScaledDensity(percent = uiScale),
        LocalSystemDensity provides systemDensity,
        LocalTerminateApplication provides terminate,
        LocalWipeApplicationData provides { wipeIosData() },
        LocalBarcodeScannerProvider provides { onResult -> rememberIosBarcodeScanner(onResult) },
        LocalBarcodeScannerSupported provides true,
    ) {
        AppTheme(
            darkTheme = isDarkTheme,
            monochrome = isMonochromeMode(themePref),
            saturation = saturationFor(themePref),
        ) {
            IosCoilSetup()
            val introCompleted by uiViewModel.appIntroCompleted.collectAsState()
            val dataNoticeAcceptedAt by uiViewModel.dataNoticeAcceptedAt.collectAsState()
            if (dataNoticeAcceptedAt == null) {
                // Still reading the stored consent: show nothing rather than flash the notice.
            } else if (dataNoticeAcceptedAt == 0L) {
                DataNoticeScreen(onAccept = { uiViewModel.onDataNoticeAccepted() })
            } else if (introCompleted) {
                ProvideIosMaps { IosMainScreen(uiViewModel, multiBackstack) }
            } else {
                IosWelcomeScreen(onDone = { uiViewModel.onAppIntroCompleted() })
            }
        }
    }
}

/** Deletes everything the app stored (documents, support files, caches, settings) and ends the process. */
@OptIn(ExperimentalForeignApi::class)
private fun wipeIosData() {
    val fileManager = NSFileManager.defaultManager
    listOf(NSDocumentDirectory, NSApplicationSupportDirectory, NSCachesDirectory).forEach { directory ->
        val roots = NSSearchPathForDirectoriesInDomains(directory, NSUserDomainMask, true)
        roots.filterIsInstance<String>().forEach { root ->
            fileManager.contentsOfDirectoryAtPath(root, null)?.filterIsInstance<String>()?.forEach { name ->
                fileManager.removeItemAtPath("$root/$name", null)
            }
        }
    }
    NSBundle.mainBundle.bundleIdentifier?.let { NSUserDefaults.standardUserDefaults.removePersistentDomainForName(it) }
    exit(0)
}
