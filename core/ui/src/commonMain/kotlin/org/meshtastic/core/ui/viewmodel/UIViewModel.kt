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
package org.meshtastic.core.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import co.touchlab.kermit.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.database.entity.asDeviceVersion
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.EventFirmwareEdition
import org.meshtastic.core.model.MeshActivity
import org.meshtastic.core.model.MyNodeInfo
import org.meshtastic.core.model.RegionInfo
import org.meshtastic.core.model.TracerouteMapAvailability
import org.meshtastic.core.model.effectiveBandwidthKHz
import org.meshtastic.core.model.evaluateTracerouteMapAvailability
import org.meshtastic.core.model.geofence.GeofencePolygon
import org.meshtastic.core.model.service.TracerouteResponse
import org.meshtastic.core.model.util.dispatchMeshtasticUri
import org.meshtastic.core.model.util.isOtaStatusNotification
import org.meshtastic.core.navigation.DEEP_LINK_BASE_URI
import org.meshtastic.core.navigation.DeepLinkRouter
import org.meshtastic.core.repository.EventFirmwareRepository
import org.meshtastic.core.repository.FirmwareReleaseRepository
import org.meshtastic.core.repository.FirmwareUpdateStatusRepository
import org.meshtastic.core.repository.LocationService
import org.meshtastic.core.repository.LockdownCoordinator
import org.meshtastic.core.repository.LockdownPassphraseStore
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.NodeRestartTracker
import org.meshtastic.core.repository.NotificationManager
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.repository.RadioController
import org.meshtastic.core.repository.RadioInterfaceService
import org.meshtastic.core.repository.ServiceRepository
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.repository.notificationId
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.client_notification
import org.meshtastic.core.resources.compromised_keys
import org.meshtastic.core.ui.component.ScrollToTopEvent
import org.meshtastic.core.ui.util.AlertManager
import org.meshtastic.core.ui.util.ComposableContent
import org.meshtastic.core.ui.util.SnackbarManager
import org.meshtastic.proto.ChannelSet
import org.meshtastic.proto.ClientNotification
import org.meshtastic.proto.LocalConfig
import org.meshtastic.proto.SharedContact

/**
 * Shared base for the application-level ViewModel.
 *
 * Contains all platform-independent state and actions (themes, alerts, connection state, firmware checks, traceroute,
 * shared contacts, channel sets, unread counts, etc.).
 */
@KoinViewModel
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList", "TooManyFunctions")
class UIViewModel(
    private val nodeDB: NodeRepository,
    protected val serviceRepository: ServiceRepository,
    private val radioController: RadioController,
    private val lockdownCoordinator: LockdownCoordinator,
    radioInterfaceService: RadioInterfaceService,
    meshLogRepository: MeshLogRepository,
    firmwareReleaseRepository: FirmwareReleaseRepository,
    private val eventFirmwareRepository: EventFirmwareRepository,
    private val firmwareUpdateStatusRepository: FirmwareUpdateStatusRepository,
    private val uiPrefs: UiPrefs,
    private val notificationManager: NotificationManager,
    private val radioConfigRepository: RadioConfigRepository,
    private val locationService: LocationService,
    packetRepository: PacketRepository,
    val alertManager: AlertManager,
    val snackbarManager: SnackbarManager,
    nodeRestartTracker: NodeRestartTracker,
) : ViewModel() {

    /** True while the connected node is expected to be mid-restart (reboot-applying config save or reboot command). */
    val nodeRestartExpected: StateFlow<Boolean> = nodeRestartTracker.restartExpected

    /**
     * True while the handshake-stall watchdog is force-reconnecting the transport, so the UI can present the transient
     * Disconnected window as an in-progress recovery rather than a user-visible disconnect. Same signal contract as
     * [ConnectionsViewModel.connectionStatus]'s RECONNECTING case.
     */
    val watchdogReconnectInFlight: StateFlow<Boolean> =
        combine(serviceRepository.connectionState, serviceRepository.connectionProgress) { state, progress ->
            state is ConnectionState.Disconnected && progress == ServiceRepository.RECONNECTING_PROGRESS_TEXT
        }
            .distinctUntilChanged()
            .stateInWhileSubscribed(initialValue = false)

    private val _navigationDeepLink = MutableSharedFlow<List<NavKey>>(replay = 1)
    val navigationDeepLink = _navigationDeepLink.asSharedFlow()

    /**
     * Unified handler for all Meshtastic deep links and OS intents.
     *
     * This method orchestrates two distinct types of URI handling:
     * 1. **Navigation:** First attempts to parse the URI into a typed [NavKey] backstack via [DeepLinkRouter]. If
     *    successful, navigates the user to the target screen.
     * 2. **Data Import:** If navigation fails, falls back to legacy contact/channel parsing via
     *    [dispatchMeshtasticUri]. This triggers import dialogs for shared nodes or channel configurations.
     */
    fun handleDeepLink(uri: CommonUri, onInvalid: () -> Unit = {}) {
        // Try navigation routing first
        val navKeys = DeepLinkRouter.route(uri)
        if (navKeys != null) {
            _navigationDeepLink.tryEmit(navKeys)
            return
        }

        // Fallback to channel/contact importing
        uri.dispatchMeshtasticUri(
            onContact = { setSharedContactRequested(it) },
            onChannel = { setRequestChannelSet(it) },
            onInvalid = {
                Logger.w { "Import URI rejected: ${uri.toSanitizedImportSummary()}" }
                onInvalid()
            },
        )
    }

    val theme: StateFlow<Int> = uiPrefs.theme

    /** Opt-out for applying an event edition's ambient theme (accent + typeface) app-wide. */
    val eventThemeEnabled: StateFlow<Boolean> = uiPrefs.eventThemeEnabled

    fun setEventThemeEnabled(enabled: Boolean) = uiPrefs.setEventThemeEnabled(enabled)

    val firmwareEdition = meshLogRepository.getMyNodeInfo().map { nodeInfo -> nodeInfo?.firmware_edition }

    val eventEdition: StateFlow<EventFirmwareEdition?> =
        combine(firmwareEdition, connectionState) { edition, state ->
            edition?.name?.takeIf { state is ConnectionState.Connected }
        }
            .distinctUntilChanged()
            // Observe rather than read once, so a manifest refresh that lands after connecting reaches the branding
            // already on screen instead of waiting for a reconnect.
            .flatMapLatest { editionName ->
                editionName?.let { eventFirmwareRepository.observeEdition(it) } ?: flowOf(null)
            }
            .stateInWhileSubscribed(initialValue = null)

    val clientNotification: StateFlow<ClientNotification?> = serviceRepository.clientNotification

    fun clearClientNotification(notification: ClientNotification) {
        serviceRepository.clearClientNotification()
        notificationManager.cancel(notification.notificationId())
    }

    val lockdownState = serviceRepository.lockdownState
    val lockdownTokenInfo = serviceRepository.lockdownTokenInfo

    fun sendLockdownUnlock(
        passphrase: String,
        bootTtl: Int = DEFAULT_BOOT_TTL,
        hourTtl: Int = 0,
        maxSessionSeconds: Int = 0,
        disable: Boolean = false,
    ) {
        lockdownCoordinator.submitPassphrase(passphrase, bootTtl, hourTtl, maxSessionSeconds, disable)
    }

    fun sendLockNow() {
        lockdownCoordinator.lockNow()
    }

    fun clearLockdownState() {
        serviceRepository.clearLockdownState()
    }

    /** Emits events for mesh network send/receive activity. */
    val meshActivity: Flow<MeshActivity> = radioInterfaceService.meshActivity

    val currentDeviceAddressFlow: StateFlow<String?> = radioInterfaceService.currentDeviceAddressFlow

    private val _scrollToTopEventFlow =
        MutableSharedFlow<ScrollToTopEvent>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val scrollToTopEventFlow: Flow<ScrollToTopEvent> = _scrollToTopEventFlow.asFlow()

    fun emitScrollToTopEvent(event: ScrollToTopEvent) {
        _scrollToTopEventFlow.tryEmit(event)
    }

    fun tracerouteMapAvailability(forwardRoute: List<Int>, returnRoute: List<Int>): TracerouteMapAvailability =
        evaluateTracerouteMapAvailability(
            forwardRoute = forwardRoute,
            returnRoute = returnRoute,
            positionedNodeNums =
            nodeDB.nodeDBbyNum.value.values.filter { it.validPosition != null }.map { it.num }.toSet(),
        )

    fun showAlert(
        title: String? = null,
        titleRes: StringResource? = null,
        message: String? = null,
        messageRes: StringResource? = null,
        composableMessage: ComposableContent? = null,
        html: String? = null,
        onConfirm: (() -> Unit)? = {},
        onDismiss: (() -> Unit)? = null,
        confirmText: String? = null,
        confirmTextRes: StringResource? = null,
        dismissText: String? = null,
        dismissTextRes: StringResource? = null,
        choices: Map<String, () -> Unit> = emptyMap(),
    ) {
        alertManager.showAlert(
            title = title,
            titleRes = titleRes,
            message = message,
            messageRes = messageRes,
            composableMessage = composableMessage,
            html = html,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            confirmText = confirmText,
            confirmTextRes = confirmTextRes,
            dismissText = dismissText,
            dismissTextRes = dismissTextRes,
            choices = choices,
        )
    }

    fun dismissAlert() {
        alertManager.dismissAlert()
    }

    fun showSnackbar(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        snackbarManager.showSnackbar(message = message, actionLabel = actionLabel, onAction = onAction)
    }

    fun setDeviceAddress(address: String) {
        safeLaunch(tag = "setDeviceAddress") { radioController.setDeviceAddress(address) }
    }

    val unreadMessageCount =
        packetRepository.getUnreadCountTotal().map { it.coerceAtLeast(0) }.stateInWhileSubscribed(initialValue = 0)

    // hardware info about our local device (can be null)
    val myNodeInfo: StateFlow<MyNodeInfo?>
        get() = nodeDB.myNodeInfo

    init {
        serviceRepository.errorMessage
            .filterNotNull()
            .onEach {
                showAlert(
                    titleRes = Res.string.client_notification,
                    message = it,
                    onConfirm = { serviceRepository.clearErrorMessage() },
                )
            }
            .launchIn(viewModelScope)

        serviceRepository.clientNotification
            .filterNotNull()
            .onEach { notification ->
                val firmwareUpdateStatus = firmwareUpdateStatusRepository.status.value
                if (notification.isOtaStatusNotification() && firmwareUpdateStatus.isOtaUpdateActive) {
                    Logger.i { "Suppressing OTA status ClientNotification generic alert during firmware update" }
                    if (!firmwareUpdateStatus.isAwaitingOtaStatus) {
                        clearClientNotification(notification)
                    }
                    return@onEach
                }
                val isCompromised = notification.low_entropy_key != null || notification.duplicated_public_key != null
                showAlert(
                    titleRes = Res.string.client_notification,
                    message = if (isCompromised) getString(Res.string.compromised_keys) else notification.message,
                    onConfirm = {
                        // Action for compromised keys should be handled via a callback or event
                        clearClientNotification(notification)
                    },
                    onDismiss = { clearClientNotification(notification) },
                )
            }
            .launchIn(viewModelScope)

        Logger.d { "UIViewModel created" }
    }

    private val _sharedContactRequested: MutableStateFlow<SharedContact?> = MutableStateFlow(null)
    val sharedContactRequested: StateFlow<SharedContact?>
        get() = _sharedContactRequested.asStateFlow()

    fun setSharedContactRequested(contact: SharedContact?) {
        _sharedContactRequested.value = contact
    }

    /** Clears the pending shared contact request. */
    fun clearSharedContactRequested() {
        _sharedContactRequested.value = null
    }

    /** Canonical app-level connection state, sourced from [ServiceRepository.connectionState]. */
    val connectionState
        get() = serviceRepository.connectionState

    private val _requestChannelSet = MutableStateFlow<ChannelSet?>(null)
    val requestChannelSet: StateFlow<ChannelSet?>
        get() = _requestChannelSet

    fun setRequestChannelSet(channelSet: ChannelSet?) {
        _requestChannelSet.value = channelSet
    }

    val latestStableFirmwareRelease = firmwareReleaseRepository.stableRelease.mapNotNull { it?.asDeviceVersion() }

    /** Clears the pending channel set import request. */
    fun clearRequestChannelUrl() {
        _requestChannelSet.value = null
    }

    override fun onCleared() {
        super.onCleared()
        Logger.d { "UIViewModel cleared" }
    }

    val tracerouteResponse: Flow<TracerouteResponse?>
        get() = serviceRepository.tracerouteResponse

    fun clearTracerouteResponse() {
        serviceRepository.clearTracerouteResponse()
    }

    val neighborInfoResponse: StateFlow<String?> = serviceRepository.neighborInfoResponse

    fun clearNeighborInfoResponse() {
        serviceRepository.clearNeighborInfoResponse()
    }

    val appIntroCompleted: StateFlow<Boolean> = uiPrefs.appIntroCompleted

    fun onAppIntroCompleted() {
        uiPrefs.setAppIntroCompleted(true)
    }

    /**
     * Snapshot of the locally-connected radio's config, used to check its LoRa channel bandwidth. Started eagerly (not
     * `stateInWhileSubscribed`) since [checkNarrowBandWarningThrottled] only ever reads [StateFlow.value] directly and
     * never collects it — a lazily-started, subscriber-gated flow would never actually receive an update.
     */
    private val localConfig: StateFlow<LocalConfig> =
        radioConfigRepository.localConfigFlow.stateIn(viewModelScope, SharingStarted.Eagerly, LocalConfig())

    private val narrowBandWarningFlow = MutableStateFlow(false)

    /** True when the narrow-band LoRa channel warning dialog should be shown. */
    val showNarrowBandWarning: StateFlow<Boolean> = narrowBandWarningFlow.asStateFlow()

    /**
     * Checks the locally-connected radio's LoRa channel bandwidth on a foreground app-open/resume, throttled to at most
     * once per [NARROW_BAND_WARNING_THROTTLE_MILLIS] so it surfaces roughly twice a day. Foreground-only by design — no
     * background/WorkManager check. Unrelated to the unthrottled check shown every time remote admin is opened from
     * Node Details.
     */
    fun checkNarrowBandWarningThrottled() {
        val lora = localConfig.value.lora ?: return
        val regionInfo = RegionInfo.fromRegionCode(lora.region)
        if (lora.effectiveBandwidthKHz(regionInfo) <= NARROW_BAND_WARNING_THRESHOLD_KHZ) return

        val now = nowMillis
        if (now - uiPrefs.lastNarrowBandWarningShownMillis.value < NARROW_BAND_WARNING_THROTTLE_MILLIS) return

        uiPrefs.setLastNarrowBandWarningShownMillis(now)
        narrowBandWarningFlow.value = true
    }

    /** Dismisses the narrow-band warning dialog after its close button becomes enabled. */
    fun dismissNarrowBandWarning() {
        narrowBandWarningFlow.value = false
    }

    private val regionWarningFlow = MutableStateFlow(false)

    /** True when the Świętokrzyskie region warning dialog should be shown. */
    val showRegionWarning: StateFlow<Boolean> = regionWarningFlow.asStateFlow()

    /**
     * Checks (on a foreground app-open/resume) whether the phone is physically within the Świętokrzyskie region while
     * the locally-connected radio's LoRa channel is wide (>200 kHz). Fires immediately, unthrottled, the moment a
     * border crossing is detected (was outside, now inside); otherwise repeats at most ~2x/day, offset by
     * [REGION_WARNING_OFFSET_FROM_NARROW_BAND_MILLIS] from the NarrowFast dialog's own throttle so the two don't show
     * back-to-back. On trigger, navigates to the LoRa settings screen first so the preset picker is already on screen
     * when the dialog appears.
     */
    fun checkRegionWarningThrottled() {
        viewModelScope.launch { performRegionCheck() }
    }

    private suspend fun performRegionCheck() {
        val lora = localConfig.value.lora ?: return
        val regionInfo = RegionInfo.fromRegionCode(lora.region)
        if (lora.effectiveBandwidthKHz(regionInfo) <= NARROW_BAND_WARNING_THRESHOLD_KHZ) return

        val location = locationService.getCurrentLocation() ?: return
        val isInside = SWIETOKRZYSKIE_REGION.contains(location.latitude, location.longitude)
        val wasInside = uiPrefs.wasInsideSwietokrzyskieRegion.value
        if (isInside != wasInside) uiPrefs.setWasInsideSwietokrzyskieRegion(isInside)
        if (!isInside) return

        val now = nowMillis
        val justCrossed = !wasInside
        val periodicDue = now - uiPrefs.lastRegionWarningShownMillis.value >= REGION_WARNING_THROTTLE_MILLIS
        val clearOfNarrowBand =
            now - uiPrefs.lastNarrowBandWarningShownMillis.value >= REGION_WARNING_OFFSET_FROM_NARROW_BAND_MILLIS

        if (justCrossed || (periodicDue && clearOfNarrowBand)) {
            uiPrefs.setLastRegionWarningShownMillis(now)
            handleDeepLink(CommonUri.parse("$DEEP_LINK_BASE_URI/lora"))
            regionWarningFlow.value = true
        }
    }

    /** Dismisses the region warning dialog after its close button becomes enabled. */
    fun dismissRegionWarning() {
        regionWarningFlow.value = false
    }

    private var periodicRegionCheckJob: Job? = null

    /**
     * Starts re-checking the region every [REGION_CHECK_INTERVAL_MILLIS] while the app is in the foreground, so a
     * border crossing is caught even if the user never leaves/reopens the app (i.e. no `onResume` ever fires again on
     * its own). Call from `onResume`/`onStart`; pair with [stopPeriodicRegionCheck] on pause/stop so this never runs
     * while backgrounded.
     */
    fun startPeriodicRegionCheck() {
        if (periodicRegionCheckJob?.isActive == true) return
        periodicRegionCheckJob =
            viewModelScope.launch {
                while (isActive) {
                    performRegionCheck()
                    delay(REGION_CHECK_INTERVAL_MILLIS)
                }
            }
    }

    /** Stops the periodic foreground re-check; call when the app leaves the foreground. */
    fun stopPeriodicRegionCheck() {
        periodicRegionCheckJob?.cancel()
        periodicRegionCheckJob = null
    }

    companion object {
        private const val DEFAULT_BOOT_TTL = LockdownPassphraseStore.DEFAULT_BOOTS
        private const val NARROW_BAND_WARNING_THRESHOLD_KHZ = 200f

        /** ~12h throttle → the foreground check can fire at most twice in a 24h period. */
        private const val NARROW_BAND_WARNING_THROTTLE_MILLIS = 12 * 60 * 60 * 1000L

        /**
         * Simplified border polygon for Świętokrzyskie voivodeship (52 vertices, Douglas-Peucker simplified from the
         * ppatrzyk/polska-geojson dataset) — (lat, lon) pairs, connects back to the first vertex.
         */
        @Suppress("MagicNumber")
        private val SWIETOKRZYSKIE_REGION =
            GeofencePolygon(
                listOf(
                    50.8660 to 19.7471,
                    50.9357 to 19.8488,
                    51.0469 to 19.8752,
                    50.9726 to 20.0356,
                    51.0577 to 20.0562,
                    51.0731 to 19.9818,
                    51.1647 to 20.0258,
                    51.1840 to 19.9941,
                    51.2013 to 20.2324,
                    51.2588 to 20.2605,
                    51.2366 to 20.3711,
                    51.3105 to 20.3644,
                    51.3394 to 20.4328,
                    51.3314 to 20.5072,
                    51.2304 to 20.5460,
                    51.1960 to 20.7003,
                    51.1490 to 20.6974,
                    51.1544 to 20.8798,
                    51.1953 to 20.9207,
                    51.1445 to 20.9985,
                    51.1571 to 21.0566,
                    51.2114 to 21.0700,
                    51.1887 to 21.1199,
                    51.1551 to 21.0912,
                    51.0803 to 21.1528,
                    51.0853 to 21.3555,
                    51.0131 to 21.4636,
                    51.0590 to 21.5291,
                    51.0780 to 21.6763,
                    51.0374 to 21.7521,
                    51.0721 to 21.8030,
                    50.8136 to 21.8690,
                    50.6451 to 21.7801,
                    50.6454 to 21.7226,
                    50.5212 to 21.6040,
                    50.4941 to 21.4541,
                    50.3298 to 21.1766,
                    50.2896 to 20.7899,
                    50.1866 to 20.5724,
                    50.2160 to 20.3746,
                    50.2415 to 20.3975,
                    50.3280 to 20.2925,
                    50.3623 to 20.3372,
                    50.4894 to 20.2205,
                    50.5545 to 19.7887,
                    50.6394 to 19.9222,
                    50.6514 to 19.8352,
                    50.6930 to 19.8717,
                    50.7254 to 19.8108,
                    50.7292 to 19.7129,
                    50.8250 to 19.8257,
                    50.8660 to 19.7471,
                ),
            )

        /** ~12h throttle → the periodic region reminder can also fire at most twice in a 24h period. */
        private const val REGION_WARNING_THROTTLE_MILLIS = 12 * 60 * 60 * 1000L

        /** Minimum gap kept between the NarrowFast dialog and the periodic region reminder (not the crossing one). */
        private const val REGION_WARNING_OFFSET_FROM_NARROW_BAND_MILLIS = 90 * 60 * 1000L // 1.5h

        /** How often the region is re-checked while the app stays in the foreground without a resume/pause cycle. */
        private const val REGION_CHECK_INTERVAL_MILLIS = 5 * 60 * 1000L // 5 minutes
    }
}

internal fun CommonUri.toSanitizedImportSummary(): String {
    val fragmentLength = fragment?.length ?: 0
    val hasFragment = !fragment.isNullOrBlank()
    val queryParameterCount = getQueryParameterNames().size
    // pathSegments values are not logged: a malformed channel URL can still leak structure (e.g.
    // a malformed "/e/" path). pathSegmentCount is enough to diagnose routing without exposing it.
    return "rawLength=${toString().length} scheme=$scheme host=$host " +
        "pathSegmentCount=${pathSegments.size} hasFragment=$hasFragment " +
        "fragmentLength=$fragmentLength queryParameterCount=$queryParameterCount"
}
