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
import org.meshtastic.core.repository.SosNotificationActions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.resources.sos_alarm_confirm
import org.meshtastic.core.resources.sos_reply_button_calling_help
import org.meshtastic.core.resources.sos_reply_button_coming
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
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptionNone
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptionNone
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.RadioBackgroundMonitor

private const val BADGE_START_DELAY_MS = 3_000L
private const val USER_INFO_DEEP_LINK = "deepLink"

/** The category of the SOS alarm: its buttons confirm the alarm and answer the sender without opening the app. */
private const val SOS_CATEGORY = "mtsw_sos_alarm"
private const val SOS_ACTION_PREFIX = "mtsw_sos_"

/** The most notifications one alarm posts (the system keeps at most 64 waiting). */
private const val MAX_REPEATS = 60

private val STACKING_CATEGORIES =
    setOf(
        Notification.Category.Message,
        Notification.Category.Reaction,
        Notification.Category.Waypoint,
        Notification.Category.NodeEvent,
        Notification.Category.Silence,
        Notification.Category.Active,
        Notification.Category.MeshBeacon,
        Notification.Category.Alert,
        Notification.Category.Battery,
    )

/**
 * Shows the banner also while the app is open (iOS hides it by default), and opens the conversation (or other screen) a
 * notification points to when the user taps it.
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
        val action = didReceiveNotificationResponse.actionIdentifier
        if (action.startsWith(SOS_ACTION_PREFIX)) {
            // A button of the SOS alarm: confirm it and send the answer; the app is not opened.
            SosNotificationActions.reply(action.removePrefix(SOS_ACTION_PREFIX))
            withCompletionHandler()
            return
        }
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
    private var sosCategoryRegistered = false

    init {
        // On iPhone the process lifecycle is always "resumed" (the app keeps its Bluetooth link), so it cannot tell a
        // locked or backgrounded app from an open one. Without this, every message was posted as "app is open" and
        // therefore without sound. The real screen state decides here.
        observe(UIApplicationDidBecomeActiveNotification) { conversationTracker.setAppForeground(true) }
        observe(UIApplicationWillResignActiveNotification) { conversationTracker.setAppForeground(false) }
        center.delegate = delegate
        keepIconBadge()
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { granted, error ->
            Logger.i { "Notification permission: granted=$granted error=${error?.localizedDescription}" }
        }
    }

    /**
     * The red number on the icon of the app (MT_SW): all unread messages, of the device shown and of the devices kept in
     * the background. It is kept here, not in a screen, so it also changes while the app is not on the screen. The
     * sources are taken later from Koin (they need this manager themselves).
     */
    private fun keepIconBadge() {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            delay(BADGE_START_DELAY_MS)
            val koin = KoinPlatform.getKoin()
            val packets = koin.get<PacketRepository>()
            val background = koin.get<RadioBackgroundMonitor>()
            combine(packets.getUnreadCountTotal(), background.unreadByAddress) { shown, quiet ->
                (shown.coerceAtLeast(0) + quiet.values.sum()).toLong()
            }
                .distinctUntilChanged()
                .collect { total -> center.setBadgeCount(total) { _ -> } }
        }
    }

    /** The buttons of the SOS alarm (confirm, "I am coming", "calling for help"); registered once, with their texts. */
    private suspend fun registerSosCategory() {
        if (sosCategoryRegistered) return
        sosCategoryRegistered = true
        val actions =
            listOf(
                SosNotificationActions.SEEN to getStringSuspend(Res.string.sos_alarm_confirm),
                SosNotificationActions.COMING to getStringSuspend(Res.string.sos_reply_button_coming),
                SosNotificationActions.CALLING_HELP to getStringSuspend(Res.string.sos_reply_button_calling_help),
            )
                .map { (id, title) ->
                    UNNotificationAction.actionWithIdentifier(
                        identifier = SOS_ACTION_PREFIX + id,
                        title = title,
                        options = UNNotificationActionOptionNone,
                    )
                }
        val category =
            UNNotificationCategory.categoryWithIdentifier(
                identifier = SOS_CATEGORY,
                actions = actions,
                intentIdentifiers = emptyList<String>(),
                options = UNNotificationCategoryOptionNone,
            )
        center.setNotificationCategories(setOf(category))
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

    @Suppress("ReturnCount")
    override suspend fun dispatch(notification: Notification): Boolean {
        val categoryEnabled =
            when (notification.category) {
                Notification.Category.Message -> prefs.messagesEnabled.value
                Notification.Category.Reaction -> prefs.loadReactionsEnabled()
                Notification.Category.Waypoint -> prefs.loadNewWaypointsEnabled()
                Notification.Category.NodeEvent -> prefs.nodeEventsEnabled.value
                Notification.Category.Silence -> prefs.nodeSilenceEnabled.value
                Notification.Category.Active -> prefs.nodeActiveEnabled.value
                Notification.Category.MeshBeacon -> prefs.nodeEventsEnabled.value
                Notification.Category.Battery -> prefs.lowBatteryEnabled.value
                Notification.Category.Alert -> prefs.alertsEnabled.value
                Notification.Category.Client -> prefs.radioNoticesEnabled.value
                Notification.Category.Service -> prefs.updatesEnabled.value
            }
        // An alarm (the SOS) is shown whatever the switch of its category says.
        val enabled = notification.alarm || categoryEnabled
        if (!enabled) return false
        // The buttons answer an SOS we received; the notice that someone called for help has nothing to answer.
        val sosButtons = notification.alarm && SosNotificationActions.alarmShowing()
        if (sosButtons) registerSosCategory()
        val content =
            UNMutableNotificationContent().apply {
                setTitle(notification.title)
                setBody(notification.message)
                notification.deepLink?.let { setUserInfo(mapOf<Any?, Any?>(USER_INFO_DEEP_LINK to it)) }
                if (sosButtons) setCategoryIdentifier(SOS_CATEGORY)
                // Notifications of one conversation (or kind) are stacked together in the list.
                notification.id?.let { setThreadIdentifier(it.toString()) }
                // A quiet notification (app open on another screen) gets a sound only if the user switched that on.
                if (!notification.isSilent || prefs.soundWhileOpen.value) {
                    setSound(
                        notification.soundName?.let { UNNotificationSound.soundNamed(it) }
                            ?: UNNotificationSound.defaultSound,
                    )
                }
            }
        // A notification posted under an identifier that is already shown only replaces it, silently: no sound and no
        // vibration on the watch. So every message, reaction, waypoint and alert gets its own identifier (they stack
        // by thread); only the service notices, which are meant to be updated in place, keep a fixed one.
        val id =
            if (notification.id == null || notification.category in STACKING_CATEGORIES) {
                NSUUID().UUIDString
            } else {
                notification.id.toString()
            }
        if (notification.repeatTimes > 1 && notification.id != null) return postRepeated(notification, content)
        val request = UNNotificationRequest.requestWithIdentifier(id, content, trigger = null)
        return suspendCancellableCoroutine { cont ->
            center.addNotificationRequest(request) { error ->
                if (error != null) Logger.w { "Notification failed: ${error.localizedDescription}" }
                cont.resume(error == null)
            }
        }
    }

    /**
     * An alarm that goes on ringing: the same notification posted again every few seconds (the system keeps up to 64
     * waiting), so it sounds again and again even when the app is not running. [cancel] removes the ones that did not
     * fire yet.
     */
    private suspend fun postRepeated(notification: Notification, content: UNMutableNotificationContent): Boolean {
        val id = notification.id ?: return false
        var accepted = true
        for (n in 0 until notification.repeatTimes.coerceAtMost(MAX_REPEATS)) {
            val seconds = (n * notification.repeatEverySeconds).coerceAtLeast(1).toDouble()
            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(seconds, repeats = false)
            val request = UNNotificationRequest.requestWithIdentifier(repeatIdentifier(id, n), content, trigger)
            accepted =
                accepted &&
                suspendCancellableCoroutine { cont ->
                    center.addNotificationRequest(request) { error ->
                        if (error != null) Logger.w { "Notification failed: ${error.localizedDescription}" }
                        cont.resume(error == null)
                    }
                }
        }
        return accepted
    }

    private fun repeatIdentifier(id: Int, n: Int) = "$id-r$n"

    override fun cancel(id: Int) {
        center.removeDeliveredNotificationsWithIdentifiers(listOf(id.toString()))
        center.removePendingNotificationRequestsWithIdentifiers(
            listOf(id.toString()) + (0 until MAX_REPEATS).map { repeatIdentifier(id, it) },
        )
        // The stacked notifications of this conversation carry their own identifiers; find them by thread.
        center.getDeliveredNotificationsWithCompletionHandler { delivered ->
            val ids =
                delivered
                    ?.mapNotNull { it as? UNNotification }
                    ?.filter { it.request.content.threadIdentifier == id.toString() }
                    ?.map { it.request.identifier }
                    .orEmpty()
            if (ids.isNotEmpty()) center.removeDeliveredNotificationsWithIdentifiers(ids)
        }
    }

    override fun cancelAll() {
        center.removeAllDeliveredNotifications()
        center.removeAllPendingNotificationRequests()
    }
}
