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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.repository.AssistantMemory
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ai_memory_clear
import org.meshtastic.core.resources.ai_memory_close
import org.meshtastic.core.resources.ai_memory_delete
import org.meshtastic.core.resources.ai_memory_instructions_hint
import org.meshtastic.core.resources.ai_memory_instructions_label
import org.meshtastic.core.resources.ai_memory_learned_title
import org.meshtastic.core.resources.ai_memory_none
import org.meshtastic.core.resources.ai_memory_open
import org.meshtastic.core.resources.ai_memory_save
import org.meshtastic.core.resources.ai_memory_summary
import org.meshtastic.core.resources.ai_memory_title
import org.meshtastic.core.ui.component.SwitchPreference

/** The switch of the assistant's memory and the dialog with the user's instructions and the assistant's notes. */
@Composable
internal fun AssistantMemorySettings() {
    val uiPrefs = koinInject<UiPrefs>()
    val memory = koinInject<AssistantMemory>()
    val on by uiPrefs.assistantMemoryEnabled.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }

    SwitchPreference(
        title = stringResource(Res.string.ai_memory_title),
        summary = stringResource(Res.string.ai_memory_summary),
        checked = on,
        enabled = true,
        onCheckedChange = { uiPrefs.setAssistantMemoryEnabled(it) },
    )
    TextButton(onClick = { open = true }, modifier = Modifier.padding(horizontal = 8.dp)) {
        Text(stringResource(Res.string.ai_memory_open))
    }
    if (open) MemoryDialog(memory) { open = false }
}

@Composable
private fun MemoryDialog(memory: AssistantMemory, onClose: () -> Unit) {
    val notes by memory.notes.collectAsStateWithLifecycle()
    var instructions by remember { mutableStateOf(notes.firstOrNull { it.byUser }?.text.orEmpty()) }
    val edits = remember { mutableStateMapOf<String, String>() }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(Res.string.ai_memory_title)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it.take(INSTRUCTIONS_MAX) },
                    label = { Text(stringResource(Res.string.ai_memory_instructions_label)) },
                    placeholder = { Text(stringResource(Res.string.ai_memory_instructions_hint)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(Res.string.ai_memory_learned_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                val learned = notes.filter { !it.byUser }
                if (learned.isEmpty()) {
                    Text(stringResource(Res.string.ai_memory_none), style = MaterialTheme.typography.bodyMedium)
                }
                learned.forEach { note ->
                    OutlinedTextField(
                        value = edits[note.name] ?: note.text,
                        onValueChange = { edits[note.name] = it.take(NOTE_MAX) },
                        label = { Text(note.name) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = { memory.delete(note.name) }) { Text(stringResource(Res.string.ai_memory_delete)) }
                }
                if (learned.isNotEmpty()) {
                    TextButton(onClick = { memory.clearLearned() }) { Text(stringResource(Res.string.ai_memory_clear)) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    memory.save(AssistantMemory.USER_NOTE, instructions)
                    edits.forEach { (name, text) -> memory.save(name, text) }
                    onClose()
                },
            ) {
                Text(stringResource(Res.string.ai_memory_save))
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(Res.string.ai_memory_close)) } },
    )
}

private const val INSTRUCTIONS_MAX = 1500
private const val NOTE_MAX = 700
