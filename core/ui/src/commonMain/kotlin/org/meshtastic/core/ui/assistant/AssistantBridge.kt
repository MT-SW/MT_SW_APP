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
package org.meshtastic.core.ui.assistant

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lets the offline assistant open a screen of the app. The assistant only ever puts a link here when the user taps
 * the button under its answer; the app shell turns it into a normal deep link.
 */
object AssistantLinks {
    private val _links = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val links: SharedFlow<String> = _links.asSharedFlow()

    /** [uri] is a full deep link such as `meshtastic://meshtastic/settings/lora`. */
    fun open(uri: String) {
        _links.tryEmit(uri)
    }
}

/** The title of the setting the assistant wants to show; the matching row blinks and then the mark disappears. */
object SettingHighlight {
    private val _target = MutableStateFlow<String?>(null)
    val target: StateFlow<String?> = _target.asStateFlow()

    fun show(title: String?) {
        _target.value = title?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun clear() {
        _target.value = null
    }
}

private const val HIGHLIGHT_MS = 6000L
private const val BLINK_MS = 450

/**
 * Makes a setting row blink with a yellow outline in the colour of the app theme when the assistant points at it, and
 * scrolls it into view. Rows whose [title] is not the target are not touched at all.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.assistantHighlight(title: String): Modifier {
    val target by SettingHighlight.target.collectAsState()
    val active = title.isNotBlank() && target != null && title.trim().equals(target, ignoreCase = true)
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(active) {
        if (active) {
            runCatching { requester.bringIntoView() }
            delay(HIGHLIGHT_MS)
            if (SettingHighlight.target.value.equals(title.trim(), ignoreCase = true)) SettingHighlight.clear()
        }
    }
    if (!active) return this
    val transition = rememberInfiniteTransition(label = "assistantHighlight")
    val alpha by
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(BLINK_MS), RepeatMode.Reverse),
            label = "assistantHighlightAlpha",
        )
    return this.bringIntoViewRequester(requester)
        .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = alpha), RoundedCornerShape(8.dp))
}
