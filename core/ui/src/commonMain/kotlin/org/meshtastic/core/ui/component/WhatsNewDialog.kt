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
package org.meshtastic.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.meshtastic.core.resources.getStringSuspend
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.common.state.LaunchOptions
import org.meshtastic.core.model.MtswRelease
import org.meshtastic.core.model.WhatsNew
import org.meshtastic.core.model.WhatsNewRelease
import org.meshtastic.core.model.WhatsNewRequest
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.whats_new_all
import org.meshtastic.core.resources.whats_new_changelog
import org.meshtastic.core.resources.whats_new_empty
import org.meshtastic.core.resources.whats_new_intro_first
import org.meshtastic.core.resources.whats_new_intro_update
import org.meshtastic.core.resources.whats_new_language
import org.meshtastic.core.resources.whats_new_since_stable
import org.meshtastic.core.resources.whats_new_title
import org.meshtastic.core.ui.viewmodel.UIViewModel
import org.meshtastic.proto.Config

private const val CHANGELOG_URL = "https://github.com/MT-SW/MT_SW_APP/blob/main/CHANGELOG.md"
private const val CONTENT_MAX_WIDTH_DP = 720

/** Milliseconds to wait after an update before the list is shown, so it does not collide with the first screens. */
private const val UPDATE_WAIT_MS = 4_000L

/** After the first setup of a configured radio: a little more, so the person has seen the app first. */
private const val FIRST_RUN_SETTLE_MS = 5_000L

/** Without a configured radio (never connected): the list is shown anyway after this long in the app. */
private const val FIRST_RUN_FALLBACK_MS = 180_000L

private val BOLD = Regex("\\*\\*(.+?)\\*\\*")

/** Builds a line of text with the parts between two pairs of asterisks in bold. */
internal fun whatsNewText(text: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (match in BOLD.findAll(text)) {
        append(text.substring(last, match.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.groupValues[1]) }
        last = match.range.last + 1
    }
    append(text.substring(last))
}

/** The name of this build as the person knows it, e.g. `v5` or `v5.test.1`. */
private fun releaseLabel(): String =
    "v${MtswRelease.RELEASE}" + if (MtswRelease.TEST > 0) ".test.${MtswRelease.TEST}" else ""

/**
 * Decides when the "what's new" list is shown and shows it. A new installation gets the whole list, but only after the
 * first setup is done (the terms accepted, the introduction closed, and the radio configured — or, when there is no
 * radio, after a few minutes in the app). After an update the person gets what changed since the last stable release.
 */
@Composable
fun WhatsNewHost(uiViewModel: UIViewModel) {
    if (koinInject<LaunchOptions>().skipOnboarding) return
    val radioConfig = koinInject<RadioConfigRepository>()
    val configured by
        remember(radioConfig) {
            radioConfig.localConfigFlow
                .map { config ->
                    val region = config.lora?.region
                    region != null && region != Config.LoRaConfig.RegionCode.UNSET
                }
                .distinctUntilChanged()
        }
            .collectAsStateWithLifecycle(initialValue = false)
    val seenBuild by uiViewModel.whatsNewSeenBuild.collectAsStateWithLifecycle()
    val seenStable by uiViewModel.whatsNewSeenStable.collectAsStateWithLifecycle()
    val firstRun by uiViewModel.whatsNewFull.collectAsStateWithLifecycle()
    var ready by remember { mutableStateOf(false) }
    var closed by remember { mutableStateOf(false) }

    LaunchedEffect(firstRun, configured) {
        delay(
            when {
                !firstRun -> UPDATE_WAIT_MS
                configured -> FIRST_RUN_SETTLE_MS
                else -> FIRST_RUN_FALLBACK_MS
            },
        )
        ready = true
    }

    val request = WhatsNew.plan(firstRun, seenBuild, seenStable, MtswRelease.RELEASE, MtswRelease.TEST)
    if (ready && !closed && request != null) {
        WhatsNewDialog(
            initialRequest = request,
            onDismiss = {
                closed = true
                uiViewModel.onWhatsNewShown(
                    buildKey = WhatsNew.buildKey(MtswRelease.RELEASE, MtswRelease.TEST),
                    stableRelease = MtswRelease.RELEASE.takeIf { MtswRelease.TEST == 0 },
                )
            },
        )
    }
}

/**
 * The list of what the app has over the original, grouped by release and then by kind (new features, improvements,
 * fixed bugs, changes against the original). The person can switch between "since the last stable release" and
 * "everything from the beginning".
 */
@Suppress("LongMethod")
@Composable
fun WhatsNewDialog(initialRequest: WhatsNewRequest, onDismiss: () -> Unit) {
    var request by remember { mutableStateOf(initialRequest) }
    // "Since the last stable" keeps the range the person was offered (for an update that may span several releases).
    val sinceStable =
        if (initialRequest.full) {
            WhatsNewRequest(full = false, fromRelease = MtswRelease.RELEASE - 1)
        } else {
            initialRequest
        }
    val all by
        produceState<List<WhatsNewRelease>?>(initialValue = null) {
            value =
                try {
                    val language = getStringSuspend(Res.string.whats_new_language)
                    WhatsNew.parse(Res.readBytes("files/whats_new_$language.md").decodeToString())
                } catch (e: CancellationException) {
                    throw e
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    // Only in the log: the window then simply says there is nothing to show.
                    Logger.withTag("WhatsNew").e(e) { "The list of changes could not be read" }
                    emptyList()
                }
        }
    val releases = all?.let { WhatsNew.select(it, request, MtswRelease.RELEASE) }.orEmpty()
    val uriHandler = LocalUriHandler.current

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), shape = MaterialTheme.shapes.large) {
            Box(contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier.widthIn(max = CONTENT_MAX_WIDTH_DP.dp).fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        // The version belongs only to "what changed lately"; the whole history is tied to no version.
                        text =
                        if (request.full) {
                            stringResource(Res.string.whats_new_title)
                        } else {
                            stringResource(Res.string.whats_new_title) + "  " + releaseLabel()
                        },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text =
                        if (request.full) {
                            stringResource(Res.string.whats_new_intro_first)
                        } else {
                            stringResource(Res.string.whats_new_intro_update)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !request.full,
                            onClick = { request = sinceStable },
                            label = { Text(stringResource(Res.string.whats_new_since_stable)) },
                        )
                        FilterChip(
                            selected = request.full,
                            onClick = { request = WhatsNewRequest(full = true, fromRelease = 0) },
                            label = { Text(stringResource(Res.string.whats_new_all)) },
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (all != null && releases.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(Res.string.whats_new_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        for (release in releases) {
                            item(key = "release-${release.order}") {
                                Text(
                                    text = release.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                            for (category in release.categories) {
                                item(key = "category-${release.order}-${category.kind}") {
                                    Text(
                                        text = category.title + " (" + category.items.size + ")",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 8.dp),
                                    )
                                }
                                items(category.items) { item ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("•", style = MaterialTheme.typography.bodyMedium)
                                        Text(whatsNewText(item), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { uriHandler.openUri(CHANGELOG_URL) }) {
                            Text(stringResource(Res.string.whats_new_changelog))
                        }
                        Button(onClick = onDismiss) { Text(stringResource(Res.string.close)) }
                    }
                }
            }
        }
    }
}
