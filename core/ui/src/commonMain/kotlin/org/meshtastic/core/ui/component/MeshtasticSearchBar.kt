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
@file:Suppress("LongMethod")

package org.meshtastic.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.ElementLook
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.clear
import org.meshtastic.core.resources.navigate_back
import org.meshtastic.core.ui.icon.ArrowBack
import org.meshtastic.core.ui.icon.Close
import org.meshtastic.core.ui.icon.Icon
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Search
import org.meshtastic.core.ui.theme.rememberUiOptions

/** Appended to the caller's tag for the field the expanded overlay shows, so a test can target one of the two. */
const val SEARCH_BAR_EXPANDED_TAG_SUFFIX = "Expanded"

/**
 * The Material 3 search bar every Meshtastic search field is built from: a collapsed [SearchBar] that expands into a
 * results surface over the screen.
 *
 * The expanded half follows Material's own rule rather than the caller's: full-screen on compact width, docked on
 * anything wider, because a full-screen dialog over a tablet or desktop window hides context the user was reading.
 *
 * @param query the text the field starts with, so a query that survived process death restores into the bar. Later
 *   changes to it are ignored; the field is the source of truth once it exists. See [resetKey].
 * @param onQueryChange called for every edit made through the field.
 * @param placeholder shown in the empty field, and the field's accessibility name.
 * @param resetKey change this to make the field take [query] again, for a caller that clears the search from outside.
 * @param inputFieldTag test tag for the collapsed field; the expanded one takes it plus
 *   [SEARCH_BAR_EXPANDED_TAG_SUFFIX].
 * @param scrollBehavior lets the bar react to the content scrolling under it; see
 *   [SearchBarDefaults.enterAlwaysSearchBarScrollBehavior].
 * @param trailingActions extra icons after the clear button, for controls that belong to the search itself (a sort
 *   menu, a filter menu). They render in the collapsed and expanded field alike.
 * @param inline when true the bar is a plain always-visible text field and never expands: whatever the caller filters
 *   (the list under it) is the result, so no second list is drawn over the first. [expandedContent] is ignored.
 * @param expandedContent the results shown under the field once expanded.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MeshtasticSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    clearDescription: String = stringResource(Res.string.clear),
    resetKey: Any? = Unit,
    inputFieldTag: String = SEARCH_BAR_INPUT_FIELD_TAG,
    scrollBehavior: SearchBarScrollBehavior? = null,
    inline: Boolean = false,
    trailingActions: @Composable RowScope.() -> Unit = {},
    expandedContent: @Composable ColumnScope.() -> Unit = {},
) {
    // The field owns the text; [query] only seeds it, and [resetKey] is how a caller replaces it.
    //
    // Feeding [query] back in on every change would lose keystrokes wherever it makes a round trip that lags the
    // typing - the node list's does, through SavedStateHandle and a combined flow - because a stale value arriving
    // while the field is further ahead overwrites what was typed.
    val textFieldState = rememberTextFieldState(query)
    val searchBarState = rememberSearchBarState()
    val latestOnQueryChange by rememberUpdatedState(onQueryChange)
    val latestQuery by rememberUpdatedState(query)

    LaunchedEffect(resetKey) {
        if (textFieldState.text.toString() != latestQuery) {
            textFieldState.edit {
                replace(0, length, latestQuery)
                placeCursorAtEnd()
            }
        }
    }
    // Edits made through the field are reported back to the caller.
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }.collect { latestOnQueryChange(it) }
    }

    if (inline) {
        InlineSearchField(
            textFieldState = textFieldState,
            placeholder = placeholder,
            clearDescription = clearDescription,
            testTag = inputFieldTag,
            trailingActions = trailingActions,
            modifier = modifier,
        )
        return
    }

    val inputField: @Composable (String) -> Unit = { tag ->
        SearchInputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            placeholder = placeholder,
            clearDescription = clearDescription,
            testTag = tag,
            trailingActions = trailingActions,
        )
    }

    val barModifier =
        modifier.fillMaxWidth().let { base ->
            scrollBehavior?.let { behavior -> with(behavior) { base.searchBarScrollBehavior() } } ?: base
        }
    // MT_SW: the look the person chose for the search (plain, with depth, see-through or glass).
    val searchLook = rememberUiOptions().element(UiOptions.GROUP_SEARCH)
    val searchFade =
        if (searchLook.look == ElementLook.TRANSPARENT || searchLook.look == ElementLook.GLASS) {
            (1f - searchLook.strength / SEARCH_PERCENT).coerceIn(SEARCH_MIN_ALPHA, 1f)
        } else {
            1f
        }
    SearchBar(
        state = searchBarState,
        inputField = { inputField(inputFieldTag) },
        modifier = barModifier.elementLook(UiOptions.GROUP_SEARCH, SearchBarDefaults.inputFieldShape),
        colors =
        SearchBarDefaults.colors(
            containerColor = SearchBarDefaults.colors().containerColor.copy(alpha = searchFade),
        ),
    )

    val expandedTag = inputFieldTag + SEARCH_BAR_EXPANDED_TAG_SUFFIX
    // Material docks the expanded bar on medium and larger windows and only goes full-screen on compact ones. The
    // pane directive is the width test the rest of the app already adapts on, so search agrees with the panes.
    val splitsHorizontally = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoSafe()).maxHorizontalPartitions > 1
    if (splitsHorizontally) {
        ExpandedDockedSearchBar(
            state = searchBarState,
            inputField = { inputField(expandedTag) },
            content = expandedContent,
        )
    } else {
        ExpandedFullScreenSearchBar(
            state = searchBarState,
            inputField = { inputField(expandedTag) },
            content = expandedContent,
        )
    }
}

/** A plain text field in the search bar's look, for lists that filter in place instead of opening a results surface. */
@Composable
private fun InlineSearchField(
    textFieldState: TextFieldState,
    placeholder: String,
    clearDescription: String,
    testTag: String,
    trailingActions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    // MT_SW: the field is a clear glass pane like the outlined buttons: a thin outline and a soft gradient in the
    // colour of the theme (grey while empty, the theme's gold as soon as something is typed, so it is visible that the
    // list below is being filtered). Nothing inside is filled solid. The field is drawn by hand, so its inside has
    // exactly the colour of the whole bar.
    val scheme = MaterialTheme.colorScheme
    val filtering = textFieldState.text.isNotEmpty()
    val base = scheme.surfaceContainerHigh
    val accent by animateColorAsState(targetValue = if (filtering) scheme.primary else scheme.outline)
    val shape = RoundedCornerShape(SEARCH_FIELD_CORNER_DP.dp)
    val contentColor = glassContentColor(UiOptions.GROUP_SEARCH, base)
    val fillTop = if (filtering) SEARCH_FILL_TOP else SEARCH_FILL_TOP_IDLE
    val fillBottom = if (filtering) SEARCH_FILL_BOTTOM else SEARCH_FILL_BOTTOM_IDLE
    StyledSurface(
        group = UiOptions.GROUP_SEARCH,
        shape = shape,
        // The same width as the cards of the list below.
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        baseColor = base,
    ) {
        BasicTextField(
            state = textFieldState,
            lineLimits = TextFieldLineLimits.SingleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = contentColor),
            cursorBrush = SolidColor(contentColor),
            modifier =
            Modifier.fillMaxWidth()
                .drawBehind {
                    val outline = shape.createOutline(size, layoutDirection, this)
                    drawOutline(
                        outline = outline,
                        brush =
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = fillTop), accent.copy(alpha = fillBottom)),
                        ),
                    )
                }
                .drawWithContent {
                    drawContent()
                    // A thin outline, strongest along the top, fading downwards: a pane of glass.
                    val outline = shape.createOutline(size, layoutDirection, this)
                    drawOutline(
                        outline = outline,
                        brush =
                        Brush.verticalGradient(
                            listOf(accent, accent.copy(alpha = SEARCH_EDGE_BOTTOM_ALPHA)),
                        ),
                        style = Stroke(width = SEARCH_RIM_DP.dp.toPx()),
                    )
                }
                .testTag(testTag)
                .semantics { contentDescription = placeholder },
            decorator = { innerTextField ->
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    Row(
                        modifier =
                        Modifier.fillMaxWidth().heightIn(min = SEARCH_FIELD_HEIGHT_DP.dp).padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = MeshtasticIcons.Search, contentDescription = null)
                        Box(
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (!filtering) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = contentColor.copy(alpha = SEARCH_HINT_ALPHA),
                                )
                            }
                            innerTextField()
                        }
                        if (filtering) {
                            IconButton(onClick = { textFieldState.clearText() }) {
                                Icon(imageVector = MeshtasticIcons.Close, contentDescription = clearDescription)
                            }
                        }
                        trailingActions()
                    }
                }
            },
        )
    }
}

private const val SEARCH_RIM_DP = 1.2f
private const val SEARCH_FILL_TOP = 0.26f
private const val SEARCH_FILL_BOTTOM = 0.08f
private const val SEARCH_FILL_TOP_IDLE = 0.12f
private const val SEARCH_FILL_BOTTOM_IDLE = 0.04f
private const val SEARCH_EDGE_BOTTOM_ALPHA = 0.55f
private const val SEARCH_HINT_ALPHA = 0.6f
private const val SEARCH_FIELD_HEIGHT_DP = 52

private const val SEARCH_FIELD_CORNER_DP = 12

/** Default tag for the collapsed field, for callers with only one search bar on screen. */
const val SEARCH_BAR_INPUT_FIELD_TAG = "SearchBarInputField"

/** The [SearchBarDefaults.InputField] shared by the collapsed bar and the expanded overlay. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchInputField(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    placeholder: String,
    clearDescription: String,
    testTag: String,
    trailingActions: @Composable RowScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val isExpanded = searchBarState.currentValue == SearchBarValue.Expanded
    val backDescription = stringResource(Res.string.navigate_back)

    SearchBarDefaults.InputField(
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
        placeholder = { Text(placeholder) },
        leadingIcon = {
            if (isExpanded) {
                IconButton(onClick = { scope.launch { searchBarState.animateToCollapsed() } }) {
                    Icon(imageVector = MeshtasticIcons.ArrowBack, contentDescription = backDescription)
                }
            } else {
                Icon(imageVector = MeshtasticIcons.Search, contentDescription = null)
            }
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (textFieldState.text.isNotEmpty()) {
                    IconButton(onClick = { textFieldState.clearText() }) {
                        Icon(imageVector = MeshtasticIcons.Close, contentDescription = clearDescription)
                    }
                }
                trailingActions()
            }
        },
        modifier = Modifier.testTag(testTag).semantics { contentDescription = placeholder },
    )
}

private const val SEARCH_PERCENT = 100f
private const val SEARCH_MIN_ALPHA = 0.12f
