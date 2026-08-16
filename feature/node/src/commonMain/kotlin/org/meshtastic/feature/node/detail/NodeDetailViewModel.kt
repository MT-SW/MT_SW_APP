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
package org.meshtastic.feature.node.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.domain.usecase.session.EnsureRemoteAdminSessionUseCase
import org.meshtastic.core.domain.usecase.session.EnsureSessionResult
import org.meshtastic.core.domain.usecase.session.ObserveRemoteAdminSessionStatusUseCase
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.RegionInfo
import org.meshtastic.core.model.SessionStatus
import org.meshtastic.core.model.effectiveBandwidthKHz
import org.meshtastic.core.navigation.Route
import org.meshtastic.core.navigation.SettingsRoute
import org.meshtastic.core.repository.LocalNodeUnavailableException
import org.meshtastic.core.repository.PacketQueueRejectedException
import org.meshtastic.core.repository.QueryController
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.repository.RadioController
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.UiText
import org.meshtastic.core.resources.connect_radio_for_remote_admin
import org.meshtastic.core.resources.delivery_confirmed
import org.meshtastic.core.resources.gpio_off
import org.meshtastic.core.resources.gpio_on
import org.meshtastic.core.resources.gpio_read_result
import org.meshtastic.core.resources.remote_admin_unreachable
import org.meshtastic.core.resources.remote_command_no_response
import org.meshtastic.core.ui.util.SnackbarManager
import org.meshtastic.core.ui.viewmodel.safeLaunch
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.feature.node.component.NodeMenuAction
import org.meshtastic.feature.node.domain.usecase.GetNodeDetailsUseCase
import org.meshtastic.feature.node.metrics.EnvironmentMetricsState
import org.meshtastic.feature.node.model.LogsType
import org.meshtastic.feature.node.model.MetricsState
import org.meshtastic.proto.LocalConfig

private const val QUEUE_REJECTION_LOG_MESSAGE = "Node-detail request rejected by outbound packet queue"
private const val LOCAL_NODE_UNAVAILABLE_LOG_MESSAGE =
    "Node-detail request deferred until local node identity is available"

/** UI state for the Node Details screen. */
@androidx.compose.runtime.Stable
data class NodeDetailUiState(
    val node: Node? = null,
    val nodeName: UiText = UiText.DynamicString(""),
    val ourNode: Node? = null,
    val metricsState: MetricsState = MetricsState.Empty,
    val environmentState: EnvironmentMetricsState = EnvironmentMetricsState(),
    val availableLogs: Set<LogsType> = emptySet(),
    val lastTracerouteTime: Long? = null,
    val lastRequestNeighborsTime: Long? = null,
    val relayNodeName: String? = null,
    val sessionStatus: SessionStatus = SessionStatus.NoSession,
    val isEnsuringSession: Boolean = false,
    val showNarrowBandWarning: Boolean = false,
)

/**
 * ViewModel for the Node Details screen, coordinating data from the node database, mesh logs, and radio configuration.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
@Suppress("LongParameterList")
class NodeDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val nodeManagementActions: NodeManagementActions,
    private val nodeRequestActions: NodeRequestActions,
    private val queryController: QueryController,
    private val radioController: RadioController,
    private val getNodeDetailsUseCase: GetNodeDetailsUseCase,
    private val ensureRemoteAdminSession: EnsureRemoteAdminSessionUseCase,
    private val observeRemoteAdminSessionStatus: ObserveRemoteAdminSessionStatusUseCase,
    private val snackbarManager: SnackbarManager,
    private val radioConfigRepository: RadioConfigRepository,
    private val resolveUiText: suspend (UiText) -> String = { it.resolve() },
) : ViewModel() {

    private val nodeIdFromRoute: Int? = savedStateHandle.get<Int>("destNum")

    private val manualNodeId = MutableStateFlow<Int?>(null)
    private val activeNodeId =
        combine(MutableStateFlow(nodeIdFromRoute), manualNodeId) { fromRoute, manual -> manual ?: fromRoute }
            .distinctUntilChanged()

    private val isEnsuringSession = MutableStateFlow(false)

    /**
     * Snapshot of the locally-connected radio's config, used to check its LoRa channel bandwidth. Started eagerly (not
     * `stateInWhileSubscribed`) since [checkNarrowBandWarning] only ever reads [StateFlow.value] directly and never
     * collects it — a lazily-started, subscriber-gated flow would never actually receive an update.
     */
    private val localConfig: StateFlow<LocalConfig> =
        radioConfigRepository.localConfigFlow.stateIn(viewModelScope, SharingStarted.Eagerly, LocalConfig())

    private val narrowBandWarningFlow = MutableStateFlow(false)

    private val sessionStatusFlow =
        activeNodeId.flatMapLatest { nodeId ->
            if (nodeId == null) flowOf(SessionStatus.NoSession) else observeRemoteAdminSessionStatus(nodeId)
        }

    /** One-shot navigation events from session-bearing actions (e.g. successful remote-admin opens). */
    private val _navigationEvents = Channel<Route>(capacity = Channel.BUFFERED)
    val navigationEvents: Flow<Route> = _navigationEvents.receiveAsFlow()

    /** Primary UI state stream, combining identity, metrics, and global device metadata. */
    val uiState: StateFlow<NodeDetailUiState> =
        activeNodeId
            .flatMapLatest { nodeId ->
                if (nodeId == null) {
                    flowOf(NodeDetailUiState())
                } else {
                    combine(
                        getNodeDetailsUseCase(nodeId),
                        sessionStatusFlow,
                        isEnsuringSession,
                        narrowBandWarningFlow,
                    ) { base, sessionStatus, ensuring, showNarrowBandWarning ->
                        base.copy(
                            sessionStatus = sessionStatus,
                            isEnsuringSession = ensuring,
                            showNarrowBandWarning = showNarrowBandWarning,
                        )
                    }
                }
            }
            .stateInWhileSubscribed(initialValue = NodeDetailUiState())

    fun start(nodeId: Int) {
        if (manualNodeId.value != nodeId) {
            manualNodeId.value = nodeId
        }
    }

    /**
     * Sets favorite status for [targetNodeNum] on the remote radio [destNum] over the admin session already established
     * for this screen — not the locally-connected radio, and regardless of whether [targetNodeNum] is in the local node
     * DB.
     */
    fun setRemoteFavorite(destNum: Int, targetNodeNum: Int, favorite: Boolean) {
        viewModelScope.launch {
            val delivered = nodeManagementActions.setFavorite(targetNodeNum, favorite, destNum)
            showRemoteCommandResult(delivered)
        }
    }

    /**
     * Sets ignore status for [targetNodeNum] on the remote radio [destNum] over the admin session already established
     * for this screen — not the locally-connected radio.
     */
    fun setRemoteIgnored(destNum: Int, targetNodeNum: Int, ignored: Boolean) {
        viewModelScope.launch {
            val delivered = nodeManagementActions.setIgnored(targetNodeNum, ignored, destNum)
            showRemoteCommandResult(delivered)
        }
    }

    /**
     * Manually adds a contact ([targetNodeNum] with [longName]/[shortName]) to the remote radio [destNum]'s node DB
     * over the admin session already established for this screen — not the locally-connected radio.
     */
    fun addRemoteContact(destNum: Int, targetNodeNum: Int, longName: String, shortName: String) {
        viewModelScope.launch {
            val delivered = radioController.addManualContact(targetNodeNum, longName, shortName, destNum)
            showRemoteCommandResult(delivered)
        }
    }

    /** Sets the GPIO pins in [gpioMask] on [destNum] to [gpioValue] via the Remote Hardware module. */
    fun writeGpio(destNum: Int, gpioMask: Long, gpioValue: Long) {
        viewModelScope.launch { radioController.writeGpio(destNum, gpioMask, gpioValue) }
    }

    /** Reads the GPIO pins in [gpioMask] on [destNum] via the Remote Hardware module and shows the result. */
    fun readGpio(destNum: Int, gpioMask: Long) {
        viewModelScope.launch {
            val value = radioController.readGpio(destNum, gpioMask)
            val resultText =
                when {
                    value == null -> getString(Res.string.remote_command_no_response)
                    (value and gpioMask) == gpioMask -> getString(Res.string.gpio_on)
                    else -> getString(Res.string.gpio_off)
                }
            snackbarManager.showSnackbar(getString(Res.string.gpio_read_result, resultText))
        }
    }

    /**
     * Sends a preset quick command (e.g. "/ping") as a private message to [node]. Prefers the PKI-encrypted channel
     * when we've exchanged a valid public key with [node] ([Node.hasPKC] && ![Node.mismatchKey]); otherwise falls back
     * to [node]'s regular channel.
     */
    fun sendQuickMessage(node: Node, text: String) {
        viewModelScope.launch {
            val channel = if (node.hasPKC && !node.mismatchKey) NodeAddress.PKC_CHANNEL_INDEX else node.channel
            radioController.sendMessage(DataPacket(to = node.user.id, channel = channel, text = text))
        }
    }

    /** Shows a snackbar confirming (or not) mesh delivery for a remote favorite/ignore command. */
    private suspend fun showRemoteCommandResult(delivered: Boolean) {
        val messageRes = if (delivered) Res.string.delivery_confirmed else Res.string.remote_command_no_response
        snackbarManager.showSnackbar(getString(messageRes))
    }

    /** Dispatches high-level node management actions like removal, muting, or favoriting. */
    fun handleNodeMenuAction(action: NodeMenuAction, onAfterRemove: () -> Unit = {}) {
        when (action) {
            is NodeMenuAction.Remove ->
                nodeManagementActions.requestRemoveNode(viewModelScope, action.node, onAfterRemove)

            is NodeMenuAction.Ignore -> nodeManagementActions.requestIgnoreNode(viewModelScope, action.node)

            is NodeMenuAction.Mute -> nodeManagementActions.requestMuteNode(viewModelScope, action.node)

            is NodeMenuAction.Favorite -> nodeManagementActions.requestFavoriteNode(viewModelScope, action.node)

            is NodeMenuAction.RequestUserInfo ->
                safeLaunch(tag = "requestUserInfo") {
                    nodeRequestActions.requestUserInfo(action.node.num, action.node.user.long_name)
                }

            is NodeMenuAction.RequestNeighborInfo ->
                safeLaunch(tag = "requestNeighborInfo") {
                    nodeRequestActions.requestNeighborInfo(action.node.num, action.node.user.long_name)
                }

            is NodeMenuAction.RequestPosition ->
                safeLaunch(tag = "requestPosition") {
                    nodeRequestActions.requestPosition(action.node.num, action.node.user.long_name)
                }

            is NodeMenuAction.RequestTelemetry ->
                safeLaunch(tag = "requestTelemetry") {
                    nodeRequestActions.requestTelemetry(action.node.num, action.node.user.long_name, action.type)
                }

            is NodeMenuAction.TraceRoute ->
                safeLaunch(tag = "requestTraceroute") {
                    nodeRequestActions.requestTraceroute(action.node.num, action.node.user.long_name)
                }

            else -> {}
        }
    }

    /**
     * Re-fetch device metadata (firmware/edition/role) for [destNum]. Refreshes the session passkey as a side effect.
     */
    fun refreshMetadata(destNum: Int) = safeLaunch(tag = "refreshMetadata") {
        try {
            queryController.refreshMetadata(destNum)
        } catch (e: PacketQueueRejectedException) {
            showNodeRequestFailure(e, QUEUE_REJECTION_LOG_MESSAGE, snackbarManager, resolveUiText)
        } catch (e: LocalNodeUnavailableException) {
            showNodeRequestFailure(e, LOCAL_NODE_UNAVAILABLE_LOG_MESSAGE, snackbarManager, resolveUiText)
        }
    }

    /**
     * Ensure a remote-admin session passkey is fresh, then request navigation to the remote-admin screen. Surfaces a
     * snackbar with the appropriate guidance on [EnsureSessionResult.Disconnected] or [EnsureSessionResult.Timeout].
     */
    fun openRemoteAdmin(destNum: Int) {
        checkNarrowBandWarning()
        // Atomic check-and-flip prevents a double-tap from queuing two passkey exchanges + two navigation events.
        if (!isEnsuringSession.compareAndSet(expect = false, update = true)) return
        safeLaunch(tag = "openRemoteAdmin") {
            try {
                when (ensureRemoteAdminSession(destNum)) {
                    EnsureSessionResult.AlreadyActive,
                    EnsureSessionResult.Refreshed,
                    -> _navigationEvents.trySend(SettingsRoute.Settings(destNum))

                    EnsureSessionResult.Disconnected -> {
                        val text = Res.string.connect_radio_for_remote_admin
                        snackbarManager.showSnackbar(resolveUiText(UiText.Resource(text)))
                    }

                    EnsureSessionResult.Timeout ->
                        snackbarManager.showSnackbar(
                            resolveUiText(UiText.Resource(Res.string.remote_admin_unreachable)),
                        )
                }
            } catch (e: PacketQueueRejectedException) {
                showNodeRequestFailure(e, QUEUE_REJECTION_LOG_MESSAGE, snackbarManager, resolveUiText)
            } catch (e: LocalNodeUnavailableException) {
                showNodeRequestFailure(e, LOCAL_NODE_UNAVAILABLE_LOG_MESSAGE, snackbarManager, resolveUiText)
            } finally {
                isEnsuringSession.value = false
            }
        }
    }

    /**
     * Shows [NodeDetailUiState.showNarrowBandWarning] if the locally-connected radio's current LoRa channel bandwidth
     * exceeds [NARROW_BAND_WARNING_THRESHOLD_KHZ] — unthrottled, shown every time remote admin is opened.
     */
    private fun checkNarrowBandWarning() {
        val lora = localConfig.value.lora ?: return
        val regionInfo = RegionInfo.fromRegionCode(lora.region)
        if (lora.effectiveBandwidthKHz(regionInfo) > NARROW_BAND_WARNING_THRESHOLD_KHZ) {
            narrowBandWarningFlow.value = true
        }
    }

    /** Dismisses the narrow-band warning dialog after its close button becomes enabled. */
    fun dismissNarrowBandWarning() {
        narrowBandWarningFlow.value = false
    }

    fun setNodeNotes(nodeNum: Int, notes: String) {
        safeLaunch(tag = "setNodeNotes") { nodeManagementActions.setNodeNotes(nodeNum, notes) }
    }

    /** Returns the type-safe navigation route for a direct message to this node. */
    fun getDirectMessageRoute(node: Node, ourNode: Node?): String {
        val hasPKC = ourNode?.hasPKC == true && node.hasPKC
        val channel = if (hasPKC) NodeAddress.PKC_CHANNEL_INDEX else node.channel
        return "${channel}${node.user.id}"
    }

    private companion object {
        /** LoRa channel bandwidths above this are considered "wide" (the 250 kHz Long/Medium/Short presets). */
        const val NARROW_BAND_WARNING_THRESHOLD_KHZ = 200f
    }
}
