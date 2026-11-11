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
@file:Suppress("ContentSlotReused", "LongMethod")

package org.meshtastic.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.DeviceType
import org.meshtastic.core.model.MeshActivity
import org.meshtastic.core.navigation.MultiBackstack
import org.meshtastic.core.navigation.TopLevelDestination
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ai_tab_ai
import org.meshtastic.core.resources.ai_tab_assistant
import org.meshtastic.core.resources.connected
import org.meshtastic.core.resources.connecting
import org.meshtastic.core.resources.device_sleeping
import org.meshtastic.core.resources.disconnected
import org.meshtastic.core.resources.ic_auto_awesome
import org.meshtastic.core.resources.node_restarting
import org.meshtastic.core.resources.reconnecting
import org.meshtastic.core.ui.navigation.icon
import org.meshtastic.core.ui.theme.monochromeLayer
import org.meshtastic.core.ui.util.MapViewReset
import org.meshtastic.core.ui.util.hideKeyboardOnTapOrScroll
import org.meshtastic.core.ui.viewmodel.UIViewModel

/**
 * Shared adaptive navigation shell using [NavigationSuiteScaffold].
 *
 * This implementation uses the [MultiBackstack] state holder to manage independent histories for each tab, aligning
 * with Navigation 3 best practices for state preservation during tab switching.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshtasticNavigationSuite(
    multiBackstack: MultiBackstack,
    uiViewModel: UIViewModel,
    modifier: Modifier = Modifier,
    /** When set, the bottom bar has this height instead of the standard one (used on iPhone, where it is too tall). */
    barHeight: Dp? = null,
    /** Empty space under the bar (only with [barHeight]); it lifts the icons away from the rounded screen corners. */
    barBottomGap: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val connectionState by uiViewModel.connectionState.collectAsStateWithLifecycle()
    val nodeRestartExpected by uiViewModel.nodeRestartExpected.collectAsStateWithLifecycle()
    val watchdogReconnectInFlight by uiViewModel.watchdogReconnectInFlight.collectAsStateWithLifecycle()
    val unreadMessageCount by uiViewModel.unreadMessageCount.collectAsStateWithLifecycle()
    val selectedDevice by uiViewModel.currentDeviceAddressFlow.collectAsStateWithLifecycle()

    val translatorEnabled by uiViewModel.translatorEnabled.collectAsStateWithLifecycle()
    val assistantEnabled by uiViewModel.assistantEnabled.collectAsStateWithLifecycle()
    val showAiTab = translatorEnabled || assistantEnabled
    val visibleDestinations = TopLevelDestination.entries.filter { it != TopLevelDestination.Translator || showAiTab }

    val adaptiveInfo = currentWindowAdaptiveInfoSafe()

    val currentTabRoute = multiBackstack.currentTabRoute
    val topLevelDestination = TopLevelDestination.fromNavKey(currentTabRoute)

    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo).coerceNavigationType()
    val showLabels = layoutType == NavigationSuiteType.NavigationRail

    val entries = visibleDestinations.map { destination ->
        val isSelected = destination == topLevelDestination
        NavEntry(
            selected = isSelected,
            onClick = { handleNavigation(destination, topLevelDestination, multiBackstack, uiViewModel) },
            icon = {
                Box(modifier = Modifier.monochromeLayer()) {
                    NavigationIconContent(
                        destination = destination,
                        isSelected = isSelected,
                        connectionState = connectionState,
                        nodeRestartExpected = nodeRestartExpected,
                        watchdogReconnectInFlight = watchdogReconnectInFlight,
                        unreadMessageCount = unreadMessageCount,
                        selectedDevice = selectedDevice,
                        meshActivityFlow = uiViewModel.meshActivity,
                        useAiIcon = assistantEnabled,
                    )
                }
            },
            label =
            if (showLabels) {
                {
                    Text(
                        stringResource(
                            when {
                                destination != TopLevelDestination.Translator -> destination.label
                                translatorEnabled && assistantEnabled -> Res.string.ai_tab_ai
                                assistantEnabled -> Res.string.ai_tab_assistant
                                else -> destination.label
                            },
                        ),
                    )
                }
            } else {
                null
            },
        )
    }

    val items: NavigationSuiteScope.() -> Unit = {
        entries.forEach { entry ->
            item(selected = entry.selected, onClick = entry.onClick, icon = entry.icon, label = entry.label)
        }
    }

    if (barHeight != null && layoutType == NavigationSuiteType.NavigationBar) {
        Surface(
            modifier = modifier.hideKeyboardOnTapOrScroll(),
            color = NavigationSuiteScaffoldDefaults.containerColor,
            contentColor = NavigationSuiteScaffoldDefaults.contentColor,
        ) {
            NavigationSuiteScaffoldLayout(
                navigationSuite = {
                    // While the keyboard is up the bar gives way to it, so the message field is not squeezed.
                    val keyboardUp = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                    if (!keyboardUp) {
                        Column(Modifier.background(NavigationBarDefaults.containerColor)) {
                            NavigationSuite(
                                modifier = Modifier.height(barHeight),
                                layoutType = layoutType,
                                content = items,
                            )
                            Spacer(Modifier.height(barBottomGap))
                        }
                    }
                },
                layoutType = layoutType,
            ) {
                Row { content() }
            }
        }
    } else if (layoutType == NavigationSuiteType.NavigationRail) {
        // Tablet and desktop: the side bar keeps its buttons at the bottom of the screen, and a new function is added
        // above the others, so the buttons stay within the reach of the hand.
        Surface(
            modifier = modifier.hideKeyboardOnTapOrScroll(),
            color = NavigationSuiteScaffoldDefaults.containerColor,
            contentColor = NavigationSuiteScaffoldDefaults.contentColor,
        ) {
            NavigationSuiteScaffoldLayout(
                navigationSuite = {
                    NavigationRail {
                        Spacer(Modifier.weight(1f))
                        entries.forEach { entry ->
                            NavigationRailItem(
                                selected = entry.selected,
                                onClick = entry.onClick,
                                icon = entry.icon,
                                label = entry.label,
                                alwaysShowLabel = true,
                            )
                        }
                    }
                },
                layoutType = layoutType,
            ) {
                Row { content() }
            }
        }
    } else {
        NavigationSuiteScaffold(
            modifier = modifier.hideKeyboardOnTapOrScroll(),
            layoutType = layoutType,
            navigationSuiteItems = items,
        ) {
            Row { content() }
        }
    }
}

/** One button of the navigation bar (the same data feeds the bottom bar and the side bar). */
private class NavEntry(
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit,
    val label: (@Composable () -> Unit)?,
)

/**
 * Caps [NavigationSuiteType] so that expanded/extra-large widths still use a NavigationRail instead of promoting to a
 * permanent NavigationDrawer.
 */
private fun NavigationSuiteType.coerceNavigationType(): NavigationSuiteType = when (this) {
    NavigationSuiteType.NavigationDrawer -> NavigationSuiteType.NavigationRail
    else -> this
}

private fun handleNavigation(
    destination: TopLevelDestination,
    topLevelDestination: TopLevelDestination?,
    multiBackstack: MultiBackstack,
    uiViewModel: UIViewModel,
) {
    val isRepress = destination == topLevelDestination
    if (!isRepress) {
        // Another tab opens where it was left, with its own screens and state.
        multiBackstack.navigateTopLevel(destination.route)
        return
    }
    // Pressing the button of the tab already open brings it back to its first screen.
    val wasOnRoot = multiBackstack.activeBackStack.size <= 1
    multiBackstack.navigateTopLevel(destination.route)
    val event =
        when (destination) {
            TopLevelDestination.Nodes -> ScrollToTopEvent.NodesTabPressed
            TopLevelDestination.Messages -> ScrollToTopEvent.ConversationsTabPressed
            else -> null
        }
    if (destination == TopLevelDestination.Map) MapViewReset.events.tryEmit(Unit)
    if (event != null) {
        // On the list itself the screen is already there; after going back to it, it has to be built first.
        if (wasOnRoot) uiViewModel.emitScrollToTopEvent(event) else uiViewModel.emitScrollToTopEventDelayed(event)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationIconContent(
    destination: TopLevelDestination,
    isSelected: Boolean,
    connectionState: ConnectionState,
    unreadMessageCount: Int,
    selectedDevice: String?,
    meshActivityFlow: Flow<MeshActivity>,
    nodeRestartExpected: Boolean = false,
    watchdogReconnectInFlight: Boolean = false,
    useAiIcon: Boolean = false,
) {
    val isConnectionsRoute = destination == TopLevelDestination.Connect
    // An expected node restart (reboot-applying config save) presents as an in-progress state, not a scary
    // red disconnect: the icon borrows the Connecting treatment and the tooltip says "Restarting". A
    // watchdog-forced handshake recovery gets the same treatment with a "Reconnecting…" tooltip.
    val restarting = nodeRestartExpected && connectionState != ConnectionState.Connected
    val reconnecting = watchdogReconnectInFlight && !restarting
    val presentedState =
        if ((restarting || reconnecting) && connectionState == ConnectionState.Disconnected) {
            ConnectionState.Connecting
        } else {
            connectionState
        }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip {
                Text(
                    if (isConnectionsRoute) {
                        connectionTooltipLabel(restarting, reconnecting, connectionState)
                    } else {
                        stringResource(destination.label)
                    },
                )
            }
        },
        state = rememberTooltipState(),
    ) {
        if (isConnectionsRoute) {
            AnimatedConnectionsNavIcon(
                connectionState = presentedState,
                deviceType = DeviceType.fromAddress(selectedDevice ?: "NoDevice"),
                meshActivityFlow = meshActivityFlow,
            )
        } else {
            BadgedBox(
                badge = {
                    if (destination == TopLevelDestination.Messages) {
                        var lastNonZeroCount by remember { mutableIntStateOf(unreadMessageCount) }
                        if (unreadMessageCount > 0) {
                            lastNonZeroCount = unreadMessageCount
                        }
                        AnimatedVisibility(
                            visible = unreadMessageCount > 0,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut(),
                        ) {
                            Badge { Text(lastNonZeroCount.toString()) }
                        }
                    }
                },
            ) {
                Crossfade(isSelected, label = "BottomBarIcon") { isSelectedState ->
                    Icon(
                        imageVector =
                        vectorResource(
                            if (destination == TopLevelDestination.Translator && useAiIcon) {
                                Res.drawable.ic_auto_awesome
                            } else {
                                destination.icon
                            },
                        ),
                        contentDescription = stringResource(destination.label),
                        tint = if (isSelectedState) colorScheme.primary else LocalContentColor.current,
                    )
                }
            }
        }
    }
}

@Composable
private fun connectionTooltipLabel(
    restarting: Boolean,
    reconnecting: Boolean,
    connectionState: ConnectionState,
): String = when {
    restarting -> stringResource(Res.string.node_restarting)
    reconnecting -> stringResource(Res.string.reconnecting)
    connectionState == ConnectionState.Connected -> stringResource(Res.string.connected)
    connectionState == ConnectionState.Connecting -> stringResource(Res.string.connecting)
    connectionState == ConnectionState.DeviceSleep -> stringResource(Res.string.device_sleeping)
    else -> stringResource(Res.string.disconnected)
}
