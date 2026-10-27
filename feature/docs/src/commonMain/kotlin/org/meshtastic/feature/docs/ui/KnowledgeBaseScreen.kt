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
package org.meshtastic.feature.docs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.kb_all
import org.meshtastic.core.resources.kb_count
import org.meshtastic.core.resources.kb_empty
import org.meshtastic.core.resources.kb_intro
import org.meshtastic.core.resources.kb_loading
import org.meshtastic.core.resources.kb_search
import org.meshtastic.core.resources.kb_source
import org.meshtastic.core.resources.kb_tier_main
import org.meshtastic.core.resources.kb_tier_offtop
import org.meshtastic.core.resources.kb_tier_related
import org.meshtastic.core.resources.kb_title
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.feature.docs.data.KbEntry
import org.meshtastic.feature.docs.data.KbTier
import org.meshtastic.feature.docs.data.KnowledgeBaseLoader
import org.meshtastic.feature.docs.data.KnowledgeBaseParser

/** The offline knowledge base for people: search, three tiers, tap an entry to read it. The assistant uses the same files. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KnowledgeBaseScreen(onBack: () -> Unit) {
    var entries by remember { mutableStateOf<List<KbEntry>?>(null) }
    LaunchedEffect(Unit) { entries = KnowledgeBaseLoader.load() }
    var query by rememberSaveable { mutableStateOf("") }
    var tierIndex by rememberSaveable { mutableStateOf(-1) }
    var openId by rememberSaveable { mutableStateOf(-1) }
    val tier = KbTier.entries.getOrNull(tierIndex)
    val all = entries
    val shown = remember(all, query, tier) { all?.let { KnowledgeBaseParser.filter(it, query, tier) }.orEmpty() }

    Scaffold(
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.kb_title),
                canNavigateUp = true,
                onNavigateUp = onBack,
                ourNode = null,
                showNodeChip = false,
                actions = {},
                onClickChip = {},
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.kb_intro), style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(Res.string.kb_search)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = tier == null,
                            onClick = { tierIndex = -1 },
                            label = { Text(stringResource(Res.string.kb_all)) },
                        )
                        KbTier.entries.forEach { t ->
                            FilterChip(
                                selected = tier == t,
                                onClick = { tierIndex = t.ordinal },
                                label = {
                                    Text(
                                        stringResource(
                                            when (t) {
                                                KbTier.MAIN -> Res.string.kb_tier_main
                                                KbTier.RELATED -> Res.string.kb_tier_related
                                                KbTier.OFFTOP -> Res.string.kb_tier_offtop
                                            },
                                        ),
                                    )
                                },
                            )
                        }
                    }
                    Text(
                        when {
                            all == null -> stringResource(Res.string.kb_loading)
                            shown.isEmpty() -> stringResource(Res.string.kb_empty)
                            else -> stringResource(Res.string.kb_count, shown.size)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(shown, key = { it.id }) { e ->
                val open = openId == e.id
                Card(modifier = Modifier.fillMaxWidth().clickable { openId = if (open) -1 else e.id }) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(e.question, style = MaterialTheme.typography.titleSmall)
                        Text(
                            e.topic,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (open) {
                            Text(e.body, style = MaterialTheme.typography.bodyMedium)
                            if (e.source.isNotBlank()) {
                                Text(
                                    stringResource(Res.string.kb_source, e.source),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
