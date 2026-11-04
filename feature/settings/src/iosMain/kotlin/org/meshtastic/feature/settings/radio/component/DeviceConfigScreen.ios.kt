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
package org.meshtastic.feature.settings.radio.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.abs
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateByAddingTimeInterval
import platform.Foundation.systemTimeZone

@Composable actual fun rememberSystemTimeZonePosixString(): String = remember { systemPosixTimeZone() }

private fun offsetText(secondsEast: Int): String {
    // POSIX counts the offset the other way round: hours west of Greenwich.
    val west = -secondsEast
    val sign = if (west < 0) "-" else ""
    val total = abs(west) / 60
    val h = total / 60
    val m = total % 60
    return if (m == 0) "$sign$h" else "$sign$h:${m.toString().padStart(2, '0')}"
}

private fun cleanName(name: String): String = if (name.length >= 3 && name.all { it.isLetter() }) name else "GMT"

/** The phone's time zone as a POSIX string (like "CET-1CEST,M3.5.0,M10.5.0/3"), the form the radio expects. */
@OptIn(ExperimentalForeignApi::class)
private fun systemPosixTimeZone(): String {
    val tz = NSTimeZone.systemTimeZone
    val now = NSDate()
    val dstShift = tz.daylightSavingTimeOffsetForDate(now)
    val std = tz.secondsFromGMTForDate(now).toInt() - dstShift.toInt()
    val stdName = cleanName(tz.abbreviationForDate(now.dateByAddingTimeInterval(-dstShift)) ?: "GMT")
    val first = tz.nextDaylightSavingTimeTransitionAfterDate(now) ?: return stdName + offsetText(std)
    val second = tz.nextDaylightSavingTimeTransitionAfterDate(first.dateByAddingTimeInterval(1.0))
        ?: return stdName + offsetText(std)

    val calendar = NSCalendar.currentCalendar.also { it.timeZone = tz }
    val units = NSCalendarUnitMonth or NSCalendarUnitDay or NSCalendarUnitWeekday or NSCalendarUnitHour or NSCalendarUnitMinute

    fun rule(transition: NSDate): String {
        // The wall clock just before the change is the time the rule names.
        val before = transition.dateByAddingTimeInterval(-1.0)
        val c = calendar.components(units, fromDate = before)
        val daysInMonth = calendar.rangeOfUnit(NSCalendarUnitDay, inUnit = NSCalendarUnitMonth, forDate = before).useContents { length.toInt() }
        val day = c.day.toInt()
        val week = if (day + 7 > daysInMonth) 5 else (day - 1) / 7 + 1
        val weekday = (c.weekday.toInt() - 1) % 7
        var hour = c.hour.toInt()
        if (c.minute.toInt() >= 59) hour += 1
        return "M${c.month}.$week.$weekday" + if (hour != 2) "/$hour" else ""
    }

    val firstIsStart = tz.daylightSavingTimeOffsetForDate(first.dateByAddingTimeInterval(1.0)) > 0.0
    val startRule = rule(if (firstIsStart) first else second)
    val endRule = rule(if (firstIsStart) second else first)
    val dstSeconds = tz.daylightSavingTimeOffsetForDate((if (firstIsStart) first else second).dateByAddingTimeInterval(1.0)).toInt()
    val dstName = cleanName(tz.abbreviationForDate((if (firstIsStart) first else second).dateByAddingTimeInterval(1.0)) ?: "GMT")
    val dstOffset = if (dstSeconds == 3600) "" else offsetText(std + dstSeconds)
    return stdName + offsetText(std) + dstName + dstOffset + ",$startRule,$endRule"
}
