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
package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.last_heard_ago_d
import org.meshtastic.core.resources.last_heard_ago_day_one
import org.meshtastic.core.resources.last_heard_ago_h
import org.meshtastic.core.resources.last_heard_ago_min
import org.meshtastic.core.resources.now
import org.meshtastic.core.resources.unknown
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val HOURS_PER_DAY = 24
private const val MAX_RELATIVE_DAYS = 7
private const val DAYS_BEFORE_YEAR_SHOWN = 330

/**
 * Texts for [formatAgo], resolved from the app's language (not the system's) on every platform. The three unit
 * templates carry one `%1$d`; they are abbreviations, so they need no plural forms.
 */
@Immutable
class AgoLabels(
    val unknown: String,
    val now: String,
    val minutes: String,
    val hours: String,
    val days: String,
    val oneDay: String = days.replace("%1\$d", "1"),
)

/** Resolves [AgoLabels] from the string resources in the language set in the app. */
@Composable
fun rememberAgoLabels(): AgoLabels = AgoLabels(
    unknown = stringResource(Res.string.unknown),
    now = stringResource(Res.string.now),
    minutes = stringResource(Res.string.last_heard_ago_min),
    hours = stringResource(Res.string.last_heard_ago_h),
    days = stringResource(Res.string.last_heard_ago_d),
    oneDay = stringResource(Res.string.last_heard_ago_day_one),
)

private fun String.withNumber(n: Long): String = replace("%1\$d", n.toString())

/**
 * Formats a given Unix timestamp (in seconds) into a relative "time ago" string in the app's language, e.g. "5 min
 * temu", "2 godz. temu", "3 dni temu". Under a minute it returns [AgoLabels.now]; older than 7 days it is a date such
 * as "7 listopada".
 *
 * @param lastSeenUnixSeconds The Unix timestamp in seconds to be formatted.
 * @param labels Localized texts, see [rememberAgoLabels].
 */
fun formatAgo(lastSeenUnixSeconds: Int, labels: AgoLabels): String {
    if (lastSeenUnixSeconds <= 0) return labels.unknown

    val lastSeenDuration = lastSeenUnixSeconds.seconds
    val diff = (nowMillis.milliseconds - lastSeenDuration).absoluteValue

    return when {
        diff < 1.minutes -> labels.now
        diff < 1.hours -> labels.minutes.withNumber(diff.inWholeMinutes)
        diff < HOURS_PER_DAY.hours -> labels.hours.withNumber(diff.inWholeHours)
        diff < 2.days -> labels.oneDay
        diff <= MAX_RELATIVE_DAYS.days -> labels.days.withNumber(diff.inWholeDays)
        else ->
            DateFormatter.formatDayMonth(
                timestampMillis = lastSeenDuration.inWholeMilliseconds,
                withYear = diff.inWholeDays >= DAYS_BEFORE_YEAR_SHOWN,
            )
    }
}

/** Composable convenience overload that resolves string resources automatically. */
@Composable
fun formatAgo(lastSeenUnixSeconds: Int): String = formatAgo(lastSeenUnixSeconds, rememberAgoLabels())
