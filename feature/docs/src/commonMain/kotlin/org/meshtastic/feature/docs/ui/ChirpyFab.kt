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

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.chirpy_checking
import org.meshtastic.core.resources.chirpy_downloading_progress
import org.meshtastic.core.resources.chirpy_ready
import org.meshtastic.core.resources.img_chirpy
import org.meshtastic.core.ui.component.FloatingActionButton
import org.meshtastic.core.ui.component.ProgressRing
import org.meshtastic.core.ui.component.ProgressSpinner
import org.meshtastic.feature.docs.model.ModelReadiness
import org.meshtastic.core.resources.Res as CoreRes

private const val FAB_ICON_SIZE = 32
private const val FAB_PROGRESS_SIZE = 56
private const val PULSE_SCALE_MIN = 0.96f
private const val PULSE_SCALE_MAX = 1.04f
private const val PULSE_DURATION_MS = 1800
private const val PERCENT_MULTIPLIER = 100

/**
 * Expressive Chirpy FAB that communicates model state through motion.
 * - **Available**: Gentle breathing pulse — alive and ready.
 * - **Checking**: Spinning indeterminate ring with reduced opacity icon.
 * - **Downloading**: Determinate circular progress arc wrapping the FAB.
 * - **Unavailable**: Not rendered (caller should hide).
 */
@Composable
fun ChirpyFab(modelReadiness: ModelReadiness, onClick: () -> Unit, modifier: Modifier = Modifier) {
    when (modelReadiness) {
        is ModelReadiness.Unavailable -> return

        is ModelReadiness.Available -> AvailableFab(onClick = onClick, modifier = modifier)

        is ModelReadiness.Checking -> CheckingFab(onClick = onClick, modifier = modifier)

        is ModelReadiness.Downloading ->
            DownloadingFab(readiness = modelReadiness, onClick = onClick, modifier = modifier)
    }
}

@Composable
private fun AvailableFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "chirpyPulse")
    val scale by
        infiniteTransition.animateFloat(
            initialValue = PULSE_SCALE_MIN,
            targetValue = PULSE_SCALE_MAX,
            animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = PULSE_DURATION_MS, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseScale",
        )

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        elevation = FloatingActionButtonDefaults.elevation(),
    ) {
        Image(
            painter = painterResource(CoreRes.drawable.img_chirpy),
            contentDescription = stringResource(CoreRes.string.chirpy_ready),
            modifier = Modifier.size(FAB_ICON_SIZE.dp),
        )
    }
}

@Composable
private fun CheckingFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            ProgressSpinner(size = FAB_PROGRESS_SIZE.dp, color = MaterialTheme.colorScheme.tertiary)
            Image(
                painter = painterResource(CoreRes.drawable.img_chirpy),
                contentDescription = stringResource(CoreRes.string.chirpy_checking),
                modifier = Modifier.size(FAB_ICON_SIZE.dp),
                alpha = 0.6f,
            )
        }
    }
}

@Composable
private fun DownloadingFab(readiness: ModelReadiness.Downloading, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val targetProgress = readiness.progress ?: 0f
    val animatedProgress by
        animateFloatAsState(
            targetValue = targetProgress,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
            label = "downloadProgress",
        )

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (targetProgress > 0f) {
                ProgressRing(
                    progress = animatedProgress,
                    size = FAB_PROGRESS_SIZE.dp,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            } else {
                ProgressSpinner(size = FAB_PROGRESS_SIZE.dp, color = MaterialTheme.colorScheme.tertiary)
            }
            Image(
                painter = painterResource(CoreRes.drawable.img_chirpy),
                contentDescription =
                stringResource(
                    CoreRes.string.chirpy_downloading_progress,
                    (targetProgress * PERCENT_MULTIPLIER).toInt(),
                ),
                modifier = Modifier.size(FAB_ICON_SIZE.dp),
            )
        }
    }
}
