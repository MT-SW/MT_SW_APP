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
package org.meshtastic.core.network.update

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.common.util.safeCatching
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.AppUpdatePolicy
import org.meshtastic.core.model.MtswRelease
import org.meshtastic.core.model.ReleaseCandidate
import org.meshtastic.core.model.ReleaseVersion
import org.meshtastic.core.repository.MeshNotificationManager
import org.meshtastic.core.repository.UiPrefs

/**
 * Checks the GitHub releases of MT_SW_APP for a newer version than the running build and, when there is one, tells the
 * user (see [AppUpdatePolicy] for the rules). Runs from a twice-a-day background job on Android and from a timer on
 * desktop; the notification itself is limited by [AppUpdatePolicy.REMINDER_INTERVAL_MILLIS].
 */
@Single
class AppUpdateService(
    private val httpClient: HttpClient,
    private val uiPrefs: UiPrefs,
    private val notificationManager: MeshNotificationManager,
    private val dispatchers: CoroutineDispatchers,
) {
    /** One check; does nothing when the user switched the update check off. Never throws (offline is normal). */
    suspend fun checkAndNotify() {
        withContext(dispatchers.io) {
            val prefs = uiPrefs.loadAppUpdatePrefs()
            Logger.i { "Update check: enabled=${prefs.checkEnabled}, includeTests=${prefs.notifyTests}" }
            if (!prefs.checkEnabled) return@withContext
            safeCatching {
                val releases = fetchReleases()
                val current = ReleaseVersion(MtswRelease.RELEASE, MtswRelease.TEST)
                Logger.i { "Update check: this build is $current, GitHub lists ${releases.map { it.tag }}" }
                val update =
                    AppUpdatePolicy.newestUpdate(current, releases, includeTests = prefs.notifyTests)
                        ?: run {
                            Logger.i { "Update check: nothing newer than this build" }
                            return@safeCatching
                        }
                val now = nowMillis
                if (
                    !AppUpdatePolicy.shouldNotify(
                        update = update,
                        lastNotifiedTag = prefs.lastNotifiedTag,
                        lastNotifiedMillis = prefs.lastNotifiedMillis,
                        nowMillis = now,
                    )
                ) {
                    Logger.i { "Update check: ${update.tag} was announced recently, no reminder yet" }
                    return@safeCatching
                }
                val version = AppUpdatePolicy.parse(update.tag, update.prerelease)
                val shown =
                    notificationManager.showAppUpdateNotification(
                        tag = update.tag,
                        isTest = version?.isTest == true,
                        url = RELEASES_PAGE_URL,
                    )
                Logger.i { "Update check: notification for ${update.tag} shown=$shown" }
                if (shown) uiPrefs.setAppUpdateLastNotified(update.tag, now)
            }
                .onFailure { Logger.w(it) { "Update check failed: offline, rate-limited, or malformed response" } }
        }
    }

    private suspend fun fetchReleases(): List<ReleaseCandidate> {
        val body =
            httpClient
                .get(RELEASES_API_URL) {
                    header("Accept", "application/vnd.github+json")
                    header("User-Agent", "MT_SW_APP")
                }
                .bodyAsText()
        val array: JsonArray = Json.parseToJsonElement(body).jsonArray
        return array.mapNotNull { element ->
            val release = element.jsonObject
            val tag = release["tag_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            ReleaseCandidate(
                tag = tag,
                prerelease = release["prerelease"]?.jsonPrimitive?.booleanOrNull ?: false,
                draft = release["draft"]?.jsonPrimitive?.booleanOrNull ?: false,
                url = release["html_url"]?.jsonPrimitive?.content ?: RELEASES_PAGE_URL,
            )
        }
    }

    private companion object {
        const val RELEASES_API_URL = "https://api.github.com/repos/MT-SW/MT_SW_APP/releases?per_page=30"
        const val RELEASES_PAGE_URL = "https://github.com/MT-SW/MT_SW_APP/releases"
    }
}
