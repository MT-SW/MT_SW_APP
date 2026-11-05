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
package org.meshtastic.feature.messaging.ui.contact

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.navigation.ChannelsRoute
import org.meshtastic.core.navigation.ContactsRoute
import org.meshtastic.core.navigation.NodesRoute
import org.meshtastic.core.navigation.SettingsRoute
import org.meshtastic.core.navigation.replaceLast
import org.meshtastic.core.ui.component.ScrollToTopEvent

@Composable
fun AdaptiveContactsScreen(
    backStack: NavBackStack<NavKey>,
    scrollToTopEvents: Flow<ScrollToTopEvent>,
    onHandleDeepLink: (CommonUri, onInvalid: () -> Unit) -> Unit,
) {
    val contactsViewModel = koinViewModel<ContactsViewModel>()

    ContactsScreen(
        onNavigateToShare = { backStack.add(ChannelsRoute.Channels) },
        onHandleDeepLink = onHandleDeepLink,
        viewModel = contactsViewModel,
        onClickNodeChip = { backStack.add(NodesRoute.NodeDetail(it)) },
        // With the list and the messages side by side, the list stays visible while a conversation is open: a new
        // choice replaces the open conversation instead of stacking on it, and the open one is not added again.
        onNavigateToMessages = { contactKey ->
            val open = backStack.lastOrNull() as? ContactsRoute.Messages
            when {
                open == null -> backStack.add(ContactsRoute.Messages(contactKey))
                open.contactKey != contactKey -> backStack.replaceLast(ContactsRoute.Messages(contactKey))
            }
        },
        onNavigateToNodeDetails = { backStack.add(NodesRoute.NodeDetail(it)) },
        onNavigateToFilterSettings = { backStack.add(SettingsRoute.FilterSettings) },
        scrollToTopEvents = scrollToTopEvents,
        activeContactKey = (backStack.lastOrNull() as? ContactsRoute.Messages)?.contactKey,
    )
}
