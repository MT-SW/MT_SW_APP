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
@file:Suppress("MatchingDeclarationName", "MagicNumber")

package org.meshtastic.core.common.util

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.languageCode
import platform.Foundation.localeIdentifier
import kotlin.math.abs

private const val MINUTE_MILLIS = 60_000L
private const val HOUR_MILLIS = 3_600_000L
private const val DAY_MILLIS = 86_400_000L

private fun format(millis: Long, dateStyle: ULong, timeStyle: ULong): String {
    val formatter = NSDateFormatter()
    formatter.locale = NSLocale.currentLocale
    formatter.dateStyle = dateStyle
    formatter.timeStyle = timeStyle
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis / 1000.0))
}

/** Dates and times on the iPhone, in the format and language set in the phone. */
actual object DateFormatter {
    actual fun formatRelativeTime(timestampMillis: Long): String {
        val delta = nowMillis - timestampMillis
        val abs = abs(delta)
        val suffix = if (delta >= 0) "ago" else "from now"
        return when {
            abs < MINUTE_MILLIS -> if (delta >= 0) "just now" else "in a moment"
            abs < HOUR_MILLIS -> "${abs / MINUTE_MILLIS}m $suffix"
            abs < DAY_MILLIS -> "${abs / HOUR_MILLIS}h $suffix"
            else -> "${abs / DAY_MILLIS}d $suffix"
        }
    }

    actual fun formatDayMonth(timestampMillis: Long, withYear: Boolean): String {
        val formatter = NSDateFormatter()
        formatter.locale = NSLocale.currentLocale
        formatter.setLocalizedDateFormatFromTemplate(if (withYear) "dMMMMy" else "dMMMM")
        return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(timestampMillis / 1000.0))
    }

    actual fun formatDateTime(timestampMillis: Long): String =
        format(timestampMillis, NSDateFormatterShortStyle, NSDateFormatterShortStyle)

    actual fun formatShortDate(timestampMillis: Long): String = if (nowMillis - timestampMillis <= DAY_MILLIS) {
        formatTime(timestampMillis)
    } else {
        format(timestampMillis, NSDateFormatterShortStyle, NSDateFormatterNoStyle)
    }

    actual fun formatTime(timestampMillis: Long): String =
        format(timestampMillis, NSDateFormatterNoStyle, NSDateFormatterShortStyle)

    actual fun formatTimeWithSeconds(timestampMillis: Long): String =
        format(timestampMillis, NSDateFormatterNoStyle, NSDateFormatterMediumStyle)

    actual fun formatDate(timestampMillis: Long): String =
        format(timestampMillis, NSDateFormatterShortStyle, NSDateFormatterNoStyle)

    actual fun formatDateTimeShort(timestampMillis: Long): String =
        format(timestampMillis, NSDateFormatterShortStyle, NSDateFormatterMediumStyle)
}

private val IMPERIAL_REGIONS = setOf("US", "LR", "MM")
private val FAHRENHEIT_REGIONS = setOf("US", "BS", "BZ", "KY", "PR", "PW")

actual fun getSystemMeasurementSystem(): MeasurementSystem =
    if (currentRegionCode().uppercase() in IMPERIAL_REGIONS) MeasurementSystem.IMPERIAL else MeasurementSystem.METRIC

actual fun getSystemTemperatureUnit(): TemperatureUnit =
    if (currentRegionCode().uppercase() in FAHRENHEIT_REGIONS) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS

actual fun currentLocaleCode(): String = NSLocale.currentLocale.languageCode

actual fun currentRegionCode(): String = NSLocale.currentLocale.countryCode.orEmpty()

actual fun currentLocaleTag(): String = NSLocale.currentLocale.localeIdentifier.replace('_', '-')

private val IPV4 = Regex("^(?:\\d{1,3}\\.){3}\\d{1,3}$")
private val IPV6 = Regex("^[0-9A-Fa-f:.]{2,45}$")
private val LABEL = Regex("^[A-Za-z0-9-]{1,63}$")

actual fun String?.isValidAddress(): Boolean {
    val value = this?.trim()
    return when {
        value.isNullOrEmpty() -> false

        value == "localhost" -> true

        IPV4.matches(value) -> value.split('.').all { it.toIntOrNull() in 0..255 }

        value.contains(':') -> IPV6.matches(value) && value.count { it == ':' } >= 2

        else -> {
            val labels = value.split('.')
            value.length <= 253 &&
                labels.size >= 2 &&
                labels.all { LABEL.matches(it) && !it.startsWith('-') && !it.endsWith('-') } &&
                labels.last().length >= 2 &&
                labels.last().all { it.isLetter() }
        }
    }
}
