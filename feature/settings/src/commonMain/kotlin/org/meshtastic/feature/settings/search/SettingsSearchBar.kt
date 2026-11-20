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
package org.meshtastic.feature.settings.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.navigation.Route
import org.meshtastic.core.navigation.TopLevelDestination
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.settings_search_no_results
import org.meshtastic.core.resources.settings_search_placeholder
import org.meshtastic.core.ui.component.MeshtasticSearchBar
import org.meshtastic.core.ui.util.TabRepress

/** Tag for the collapsed settings search field, so a test can target it rather than the expanded overlay's copy. */
const val SETTINGS_SEARCH_BAR_INPUT_FIELD_TAG = "SettingsSearchBarInputField"

/**
 * The search field at the top of Settings. Typing expands the bar over the screen and lists matching settings; choosing
 * one navigates to the screen that holds it and asks that screen to scroll to the control. A screen whose list items
 * carry no key for the control opens at the top.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSearchBar(
    viewModel: SettingsSearchViewModel,
    modifier: Modifier = Modifier,
    includeAppLocal: Boolean = true,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    var resetKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(includeAppLocal) { viewModel.setIncludeAppLocal(includeAppLocal) }

    // The bottom button of the Settings tab, pressed again, clears the search.
    LaunchedEffect(Unit) {
        TabRepress.events.collect { tab ->
            if (tab == TopLevelDestination.Settings) {
                viewModel.setQuery("")
                resetKey++
            }
        }
    }

    // MT_SW: the field is the same always-visible one as in the node list; the matches replace the settings below it
    // (see SettingsSearchResultsInline) instead of opening a small panel next to it.
    MeshtasticSearchBar(
        query = query,
        onQueryChange = viewModel::setQuery,
        placeholder = stringResource(Res.string.settings_search_placeholder),
        modifier = modifier,
        resetKey = resetKey,
        inputFieldTag = SETTINGS_SEARCH_BAR_INPUT_FIELD_TAG,
        inline = true,
    )
}

/** The matches of the settings search, drawn over the whole width in place of the settings list while typing. */
@Composable
fun SettingsSearchResultsInline(
    viewModel: SettingsSearchViewModel,
    onNavigate: (Route) -> Unit,
    modifier: Modifier = Modifier,
) {
    val results by viewModel.results.collectAsStateWithLifecycle()
    if (results.isEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.settings_search_no_results),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            results.forEach { entry ->
                SettingsSearchResult(entry = entry, onClick = { onNavigate(viewModel.select(entry)) })
                HorizontalDivider()
            }
        }
    }
}

/** The results shown under the expanded field: the matches, or a line saying there were none. */
@Composable
internal fun SettingsSearchResults(
    results: List<ResolvedSettingsEntry>,
    query: String,
    onSelect: (ResolvedSettingsEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (query.isNotBlank() && results.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.settings_search_no_results),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        // Keyed on the resource name, not the text: two entries can share a title and a destination (six controls are
        // called "Enabled"), and a duplicate key crashes the list.
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(results, key = { it.id }) { entry ->
                SettingsSearchResult(entry = entry, onClick = { onSelect(entry) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SettingsSearchResult(entry: ResolvedSettingsEntry, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(entry.title) },
        supportingContent =
        entry.description?.let { description ->
            { Text(text = description, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        },
        // The destination's name, so "Enabled" says which Enabled it is.
        overlineContent = { Text(entry.screenTitle) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
