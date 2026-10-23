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
package org.meshtastic.feature.settings.component

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.repository.MeshLogRetention
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.log_retention_days
import org.meshtastic.core.resources.log_retention_days_quantity
import org.meshtastic.core.resources.log_retention_days_summary
import org.meshtastic.core.resources.log_retention_hours
import org.meshtastic.core.resources.log_retention_never
import org.meshtastic.core.ui.component.DropDownPreference

private val RETENTION_DAYS_CHOICES = listOf(1, 2, 3, 7, 14, 30, 60)

/**
 * How long received packets (the logs that statistics and charts are built from) are kept: from one day up to two
 * months. A value chosen in an earlier version (an hour, "never", more than 60 days) is still shown and kept until the
 * user picks one of the choices.
 */
@Composable
fun LogRetentionPreference(retentionDays: Int, enabled: Boolean = true, onSelected: (Int) -> Unit) {
    val choices =
        RETENTION_DAYS_CHOICES.map { days ->
            days.toLong() to pluralStringResource(Res.plurals.log_retention_days_quantity, days, days)
        }
    val extra =
        when {
            retentionDays in RETENTION_DAYS_CHOICES -> emptyList()
            retentionDays == MeshLogRetention.ONE_HOUR ->
                listOf(retentionDays.toLong() to pluralStringResource(Res.plurals.log_retention_hours, 1, 1))
            retentionDays == MeshLogRetention.KEEP_FOREVER ->
                listOf(retentionDays.toLong() to stringResource(Res.string.log_retention_never))
            else ->
                listOf(
                    retentionDays.toLong() to
                        pluralStringResource(Res.plurals.log_retention_days_quantity, retentionDays, retentionDays),
                )
        }
    DropDownPreference(
        title = stringResource(Res.string.log_retention_days),
        enabled = enabled,
        items = choices + extra,
        selectedItem = retentionDays.toLong(),
        onItemSelected = { selected: Long -> onSelected(selected.toInt()) },
        summary = stringResource(Res.string.log_retention_days_summary),
    )
}
