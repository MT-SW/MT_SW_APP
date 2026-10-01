/*
 * Copyright (c) 2025 MT_SW contributors
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
@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("TooManyFunctions")

package org.meshtastic.feature.map.planner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.planner_error_number
import org.meshtastic.core.resources.planner_error_range
import org.meshtastic.core.ui.icon.Info
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Warning

/** A titled card; [info] adds the "i" button of that topic next to the title. */
@Composable
internal fun PlannerCard(
    title: String,
    modifier: Modifier = Modifier,
    info: PlannerInfoTopic? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (info != null) PlannerInfoButton(info)
            }
            content()
        }
    }
}

/** Small sub-heading with an optional info button. */
@Composable
internal fun PlannerSubheading(text: String, info: PlannerInfoTopic? = null, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        if (info != null) PlannerInfoButton(info)
    }
}

/** "label ........ value" row. */
@Composable
internal fun PlannerValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
    bold: Boolean = false,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else null,
            color = valueColor,
            textAlign = TextAlign.End,
        )
    }
}

/** A coloured notice box: informational (default), warning or error. */
@Composable
internal fun PlannerNotice(text: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
        CardDefaults.cardColors(
            containerColor =
            if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
            contentColor =
            if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = if (isError) MeshtasticIcons.Warning else MeshtasticIcons.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(text = text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * A numeric text field. Keeps its own text (so the cursor never jumps and partial input like "12," survives) and
 * reports every valid value inside [[min], [max]] through [onValue]. When [value] changes from outside (the model
 * clamped it, another source was picked) the text follows. A blank text calls [onBlank] when given.
 */
@Composable
internal fun PlannerNumberField(
    label: String,
    value: Double?,
    onValue: (Double) -> Unit,
    modifier: Modifier = Modifier,
    decimals: Int = 2,
    suffix: String? = null,
    min: Double = -1.0e12,
    max: Double = 1.0e12,
    supporting: String? = null,
    onBlank: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    var text by remember { mutableStateOf(value?.let { PlannerInput.formatField(it, decimals) } ?: "") }
    LaunchedEffect(value) {
        if (value == null) {
            text = ""
        } else {
            val parsed = PlannerInput.parseNumber(text)
            if (parsed == null || !PlannerInput.sameValue(parsed, value)) text = PlannerInput.formatField(value, decimals)
        }
    }
    val parsedNow = PlannerInput.parseNumber(text)
    val invalid = text.isNotBlank() && (parsedNow == null || parsedNow < min || parsedNow > max)
    val numberError = stringResource(Res.string.planner_error_number)
    val rangeError =
        if (invalid && parsedNow != null) {
            stringResource(
                Res.string.planner_error_range,
                PlannerInput.formatField(min, decimals),
                PlannerInput.formatField(max, decimals),
            )
        } else {
            ""
        }
    val supportText: String? =
        when {
            invalid -> if (parsedNow == null) numberError else rangeError
            else -> supporting
        }
    OutlinedTextField(
        value = text,
        onValueChange = { t ->
            text = t
            val p = PlannerInput.parseNumber(t)
            if (p != null && p >= min && p <= max) {
                onValue(p)
            } else if (t.isBlank()) {
                onBlank?.invoke()
            }
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = invalid,
        suffix = suffix?.let { s -> { Text(s) } },
        supportingText = supportText?.let { st -> { Text(st) } },
        keyboardOptions =
        KeyboardOptions(keyboardType = if (min < 0.0) KeyboardType.Text else KeyboardType.Decimal),
    )
}

/** A plain text field with local text state. */
@Composable
internal fun PlannerTextField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(value) }
    LaunchedEffect(value) { if (value != text) text = value }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValue(it)
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
    )
}

/** Read-only dropdown field. */
@Composable
internal fun <T> PlannerDropdown(
    label: String,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    },
                )
            }
        }
    }
}
