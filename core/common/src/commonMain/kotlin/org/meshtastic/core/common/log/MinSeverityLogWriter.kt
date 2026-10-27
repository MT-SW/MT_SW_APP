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
package org.meshtastic.core.common.log

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

/**
 * Passes on only lines of [min] severity or higher. Kermit's global minimum severity is lowered while the file log is
 * on (to catch Debug and Verbose lines); this keeps the other writers (console, crash reports, analytics) seeing only
 * what they saw before.
 */
class MinSeverityLogWriter(private val delegate: LogWriter, private val min: Severity) : LogWriter() {
    override fun isLoggable(tag: String, severity: Severity): Boolean =
        severity >= min && delegate.isLoggable(tag, severity)

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) =
        delegate.log(severity, message, tag, throwable)
}
