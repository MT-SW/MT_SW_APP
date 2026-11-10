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
package org.meshtastic.ios.notification

import co.touchlab.kermit.Logger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.meshtastic.core.repository.ActiveConversationTracker
import org.meshtastic.core.repository.Notification
import org.meshtastic.core.repository.NotificationManager
import org.meshtastic.core.repository.NotificationPrefs
import org.meshtastic.ios.handleIosDeepLink
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSUUID
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject
import kotlin.coroutines.resume

private const val USER_INFO_DEEP_LINK = "deepLink"

/**
 * Shows the banner also while the app is open (iOS hides it by default), and opens the conversation (or other screen)
 * a notification points to when the user taps it.
 */
private class ForegroundBanner :
    NSObject(),
    UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        withCompletionHandler(
            UNNotificationPresentationOptionBanner or
                UNNotificationPresentationOptionList or
                UNNotificationPresentationOptionSound,
        )
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit,
    ) {
        val link = didReceiveNotificationResponse.notification.request.content.userInfo[USER_INFO_DEEP_LINK] as? String
        if (link != null) handleIosDeepLink(link)
        withCompletionHandler()
    }
}

/** Local notifications on iPhone: gated by the user's notification settings, shown by the system. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosNotificationManager(
    private val prefs: NotificationPrefs,
    private val conversationTracker: ActiveConversationTracker,
) : NotificationManager {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private val delegate = ForegroundBanner()

    init {
        // On iPhone the process lifecycle is always "resumed" (the app keeps its Bluetooth link), so it cannot tell a
        // locked or backgrounded app from an open one. Without this, every message was posted as "app is open" and
        // therefore without sound. The real screen state decides here.
        observe(UIApplicationDidBecomeActiveNotification) { conversationTracker.setAppForeground(true) }
        observe(UIApplicationWillResignActiveNotification) { conversationTracker.setAppForeground(false) }
        center.delegate = delegate
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { granted, error ->
            Logger.i { "Notification permission: granted=$granted error=${error?.localizedDescription}" }
        }
    }

    private fun observe(name: String?, block: () -> Unit) {
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = name,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            block()
        }
    }

    override suspend fun dispatch(notification: Notification): Boolean {
        val enabled =
            when (notification.category) {
                Notification.Category.Message -> prefs.messagesEnabled.value
                Notification.Category.Reaction -> prefs.loadReactionsEnabled()
                Notification.Category.Waypoint -> prefs.loadNewWaypointsEnabled()
                Notification.Category.NodeEvent -> prefs.nodeEventsEnabled.value
                Notification.Category.MeshBeacon -> prefs.nodeEventsEnabled.value
                Notification.Category.Battery -> prefs.lowBatteryEnabled.value
                Notification.Category.Alert -> true
                Notification.Category.Client -> true
                Notification.Category.Service -> true
            }
        if (!enabled) return false
        val content =
            UNMutableNotificationContent().apply {
                setTitle(notification.title)
                setBody(notification.message)
                notification.deepLink?.let { setUserInfo(mapOf<Any?, Any?>(USER_INFO_DEEP_LINK to it)) }
                // A quiet notification (app open on another screen) gets a sound only if the user switched that on.
                if (!notification.isSilent || prefs.soundWhileOpen.value) setSound(UNNotificationSound.defaultSound)
            }
        val id = notification.id?.toString() ?: NSUUID().UUIDString
        val request = UNNotificationRequest.requestWithIdentifier(id, content, trigger = null)
        return suspendCancellableCoroutine { cont ->
            center.addNotificationRequest(request) { error ->
                if (error != null) Logger.w { "Notification failed: ${error.localizedDescription}" }
                cont.resume(error == null)
            }
        }
    }

    override fun cancel(id: Int) {
        center.removeDeliveredNotificationsWithIdentifiers(listOf(id.toString()))
        center.removePendingNotificationRequestsWithIdentifiers(listOf(id.toString()))
    }

    override fun cancelAll() {
        center.removeAllDeliveredNotifications()
        center.removeAllPendingNotificationRequests()
    }
}
